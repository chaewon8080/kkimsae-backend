package com.example.kkimsae_backend.service;

import com.example.kkimsae_backend.dto.IncidentCreateRequest;
import com.example.kkimsae_backend.dto.IncidentResponse;
import com.example.kkimsae_backend.dto.IncidentSummaryResponse;

import com.example.kkimsae_backend.entity.ClueType;
import com.example.kkimsae_backend.entity.Incident;
import com.example.kkimsae_backend.entity.IncidentClue;
import com.example.kkimsae_backend.entity.Member;

import com.example.kkimsae_backend.repository.IncidentClueRepository;
import com.example.kkimsae_backend.repository.IncidentRepository;
import com.example.kkimsae_backend.repository.MemberRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.kkimsae_backend.repository.TimelineEventRepository;
import java.util.List;
import com.example.kkimsae_backend.repository.CasePublicationRepository;
import com.example.kkimsae_backend.entity.IncidentImage;
import com.example.kkimsae_backend.repository.IncidentImageRepository;

import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import com.example.kkimsae_backend.dto.IncidentImageResponse;
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final S3Presigner s3Presigner;

    @Value("${app.s3.bucket}")
    private String imageBucket;

    private final IncidentRepository incidentRepository;
    private final IncidentClueRepository incidentClueRepository;
    private final MemberRepository memberRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final CasePublicationRepository casePublicationRepository;
    private final EntityManager entityManager;
    private final IncidentImageRepository incidentImageRepository;
    private final IncidentImageService incidentImageService;
    @Transactional
    public IncidentResponse create(
            Long memberId,
            IncidentCreateRequest request
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "다시 로그인해주세요."
                ));

        Incident incident = new Incident(
                member,
                request.getTitle().trim(),
                request.getDescription().trim(),
                trimToNull(request.getChannel()),
                trimToNull(request.getDemand()),
                trimToNull(request.getCompany())
        );

        incidentRepository.save(incident);

        List<IncidentClue> clues = request.getClues()
                .stream()
                .map(clue -> createClue(incident, clue))
                .toList();

        List<IncidentClue> savedClues =
                incidentClueRepository.saveAll(clues);

        return IncidentResponse.from(incident, savedClues);
    }

    @Transactional(readOnly = true)
    public List<IncidentSummaryResponse> getMyIncidents(
            Long memberId
    ) {
        return incidentRepository
                .findByMember_IdOrderByCreatedAtDescIdDesc(memberId)
                .stream()
                .map(IncidentSummaryResponse::from)
                .toList();
    }
    @Transactional(readOnly = true)
    public IncidentResponse getMyIncident(
            Long memberId,
            Long incidentId
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

        List<IncidentClue> clues = incidentClueRepository
                .findByIncident_IdOrderByIdAsc(incidentId);

        List<IncidentImage> images = incidentImageRepository
                .findByIncident_IdOrderByUploadedAtDescIdDesc(incidentId);

        List<IncidentImageResponse> imageResponses = images.stream()
                .map(image -> {
                    // 기존 로컬 저장 파일에는 S3 조회 URL을 만들지 않음
                    String url = image.getStoredName().startsWith("incidents/")
                            ? createImageUrl(image.getStoredName())
                            : null;

                    return IncidentImageResponse.from(image, url);
                })
                .toList();

        return IncidentResponse.from(incident, clues)
                .withImages(imageResponses);
    }

    private String createImageUrl(String key) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(imageBucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .getObjectRequest(objectRequest)
                        .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    private IncidentClue createClue(
            Incident incident,
            IncidentCreateRequest.ClueRequest request
    ) {
        String bankName = null;

        if (request.getType() == ClueType.ACCOUNT) {
            bankName = trimToNull(request.getBankName());

            if (bankName == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "계좌번호에는 은행명을 입력해주세요."
                );
            }
        }

        return new IncidentClue(
                incident,
                request.getType(),
                request.getValue().trim(),
                bankName
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }


    @Transactional
    public IncidentResponse update(
            Long memberId,
            Long incidentId,
            IncidentCreateRequest request
    ) {
        Incident incident = incidentRepository
                .findByIdAndMember_Id(incidentId, memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사건을 찾을 수 없습니다."
                ));

        // 새 단서의 유효성을 먼저 확인
        List<IncidentClue> newClues = request.getClues()
                .stream()
                .map(clue -> createClue(incident, clue))
                .toList();

        incident.update(
                request.getTitle().trim(),
                request.getDescription().trim(),
                trimToNull(request.getChannel()),
                trimToNull(request.getDemand()),
                trimToNull(request.getCompany())
        );

        // 기존 단서를 삭제하고 새 목록으로 교체
        incidentClueRepository.deleteByIncident_Id(incidentId);
        incidentClueRepository.flush();

        List<IncidentClue> savedClues =
                incidentClueRepository.saveAll(newClues);

        List<IncidentImage> images = incidentImageRepository
                .findByIncident_IdOrderByUploadedAtDescIdDesc(incidentId);

        return IncidentResponse.from(
                incident,
                savedClues,
                images
        );

    }

    @Transactional
    public void delete(
            Long memberId,
            Long incidentId
    ) {
        Incident incident = incidentRepository
                .findByIdAndMember_Id(incidentId, memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사건을 찾을 수 없습니다."
                ));

        entityManager.lock(
                incident,
                jakarta.persistence.LockModeType.PESSIMISTIC_WRITE
        );

        // 사진 DB 정보 삭제 + 커밋 후 실제 사진 삭제
        incidentImageService.deleteForIncident(incidentId);

        // 공개 사례 삭제
        casePublicationRepository.deleteByIncidentId(incidentId);
        casePublicationRepository.flush();

        // 타임라인 삭제
        timelineEventRepository.deleteByIncident_Id(incidentId);
        timelineEventRepository.flush();

        // 단서 삭제
        incidentClueRepository.deleteByIncident_Id(incidentId);
        incidentClueRepository.flush();

        // 사건 삭제
        incidentRepository.delete(incident);
    }

}