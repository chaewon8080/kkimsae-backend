package com.example.kkimsae_backend.service;

import com.example.kkimsae_backend.dto.IncidentImageResponse;
import com.example.kkimsae_backend.entity.Incident;
import com.example.kkimsae_backend.entity.IncidentImage;
import com.example.kkimsae_backend.repository.IncidentImageRepository;
import com.example.kkimsae_backend.repository.IncidentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
public class IncidentImageService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final IncidentImageRepository incidentImageRepository;
    private final IncidentRepository incidentRepository;
    private final EntityManager entityManager;
    private final S3Client s3Client;
    private final String bucket;

    public IncidentImageService(
            IncidentImageRepository incidentImageRepository,
            IncidentRepository incidentRepository,
            EntityManager entityManager,
            S3Client s3Client,
            @Value("${app.s3.bucket}") String bucket
    ) {
        this.incidentImageRepository = incidentImageRepository;
        this.incidentRepository = incidentRepository;
        this.entityManager = entityManager;
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Transactional
    public IncidentImageResponse upload(
            Long memberId,
            Long incidentId,
            MultipartFile file
    ) {
        if (memberId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "다시 로그인해주세요."
            );
        }

        Incident incident = incidentRepository
                .findByIdAndMember_Id(incidentId, memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사건을 찾을 수 없습니다."
                ));

        // 사건 삭제와 사진 업로드를 동시에 처리하지 않도록 잠금
        entityManager.lock(
                incident,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "업로드할 사진을 선택해주세요."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "사진은 10MB 이하만 업로드할 수 있습니다."
            );
        }

        String originalName = cleanOriginalName(
                file.getOriginalFilename()
        );

        ImageType imageType = detectImageType(file);

        validateExtension(originalName, imageType);

        String storedName = "incidents/"
                + incidentId
                + "/"
                + UUID.randomUUID()
                + "."
                + imageType.extension();

// DB 저장 또는 최종 커밋 실패 시 S3 이미지 정리
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            deleteQuietly(storedName);
                        }
                    }
                }
        );

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storedName)
                .contentType(imageType.contentType())
                .build();

        try (InputStream input = file.getInputStream()) {
            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(input, file.getSize())
            );
        } catch (IOException | SdkException e) {
            log.error("사건 사진 S3 업로드 실패: {}", storedName, e);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "사진 저장에 실패했습니다.",
                    e
            );
        }

        IncidentImage image = new IncidentImage(
                incident,
                originalName,
                storedName,
                imageType.contentType(),
                file.getSize()
        );

        incidentImageRepository.saveAndFlush(image);

        return IncidentImageResponse.from(image);
    }

    // IncidentService.delete() 내부에서 호출
    // 사건 삭제가 커밋된 다음 실제 사진도 삭제
    @Transactional
    public void deleteForIncident(Long incidentId) {
        List<IncidentImage> images = incidentImageRepository
                .findByIncident_IdOrderByUploadedAtDescIdDesc(incidentId);

        List<String> keys = images.stream()
                .map(IncidentImage::getStoredName)
                .toList();

        incidentImageRepository.deleteAll(images);
        incidentImageRepository.flush();

        // 사건 삭제가 최종 커밋된 후 S3 이미지 삭제
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        keys.forEach(
                                IncidentImageService.this::deleteQuietly
                        );
                    }
                }
        );
    }

    private String cleanOriginalName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "파일명이 없습니다."
            );
        }

        String normalized = name.replace('\\', '/');

        String cleaned = normalized.substring(
                normalized.lastIndexOf('/') + 1
        ).replaceAll("\\p{Cntrl}", "").trim();

        if (cleaned.isBlank() || cleaned.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "파일명은 1~255자로 입력해주세요."
            );
        }

        return cleaned;
    }

    // 확장자와 별도로 파일 시작 바이트 확인
    private ImageType detectImageType(MultipartFile file) {
        byte[] header;

        try (InputStream input = file.getInputStream()) {
            header = input.readNBytes(8);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "사진을 읽을 수 없습니다.",
                    e
            );
        }

        if (startsWith(
                header,
                0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        )) {
            return new ImageType("png", "image/png");
        }

        if (startsWith(header, 0xFF, 0xD8, 0xFF)) {
            return new ImageType("jpg", "image/jpeg");
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "JPG, JPEG, PNG 사진만 업로드할 수 있습니다."
        );
    }

    private boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) {
                return false;
            }
        }

        return true;
    }

    private void validateExtension(
            String originalName,
            ImageType imageType
    ) {
        String lowerName = originalName.toLowerCase(Locale.ROOT);

        boolean valid = switch (imageType.extension()) {
            case "jpg" ->
                    lowerName.endsWith(".jpg")
                            || lowerName.endsWith(".jpeg");
            case "png" -> lowerName.endsWith(".png");
            default -> false;
        };

        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "파일 확장자와 실제 사진 형식이 일치하지 않습니다."
            );
        }
    }

    private void deleteQuietly(String key) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );
        } catch (SdkException e) {
            log.error("사건 사진 S3 정리 실패: {}", key, e);
        }
    }

    @Transactional
    public void deleteImage(
            Long memberId,
            Long incidentId,
            Long imageId
    ) {
        if (memberId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "다시 로그인해주세요."
            );
        }

        Incident incident = incidentRepository
                .findByIdAndMember_Id(incidentId, memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사건을 찾을 수 없습니다."
                ));

        entityManager.lock(
                incident,
                LockModeType.PESSIMISTIC_WRITE
        );

        IncidentImage image = incidentImageRepository
                .findByIdAndIncident_Id(imageId, incidentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사진을 찾을 수 없습니다."
                ));

        String key = image.getStoredName();

        incidentImageRepository.delete(image);
        incidentImageRepository.flush();

        // DB 삭제가 커밋된 후 S3 파일 삭제
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (key.startsWith("incidents/")) {
                            deleteQuietly(key);
                        }
                    }
                }
        );
    }

    private record ImageType(
            String extension,
            String contentType
    ) {
    }


}