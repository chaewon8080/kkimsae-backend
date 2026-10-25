package com.example.kkimsae_backend.service;

import com.example.kkimsae_backend.dto.AdminPublicationDetailResponse;
import com.example.kkimsae_backend.dto.AdminPublicationSummaryResponse;
import com.example.kkimsae_backend.dto.PublicationApproveRequest;
import com.example.kkimsae_backend.dto.PublicationRejectRequest;

import com.example.kkimsae_backend.entity.CasePublication;
import com.example.kkimsae_backend.entity.PublicationStatus;

import com.example.kkimsae_backend.repository.CasePublicationRepository;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminPublicationService {

    private final CasePublicationRepository publicationRepository;
    private final Set<Long> adminMemberIds;

    public AdminPublicationService(
            CasePublicationRepository publicationRepository,
            @Value("${app.admin-member-ids:}") String adminMemberIds
    ) {
        this.publicationRepository = publicationRepository;

        this.adminMemberIds = Arrays.stream(
                        adminMemberIds.split(",")
                )
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public Page<AdminPublicationSummaryResponse> list(
            Long adminMemberId,
            PublicationStatus status,
            int page,
            int size
    ) {
        requireAdmin(adminMemberId);

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page는 0 이상, size는 1~100 사이여야 합니다."
            );
        }

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("requestedAt"),
                        Sort.Order.asc("id")
                )
        );

        return publicationRepository
                .findByStatus(status, pageable)
                .map(AdminPublicationSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminPublicationDetailResponse detail(
            Long adminMemberId,
            Long publicationId
    ) {
        requireAdmin(adminMemberId);

        CasePublication publication = publicationRepository
                .findById(publicationId)
                .orElseThrow(this::publicationNotFound);

        return AdminPublicationDetailResponse.from(publication);
    }

    @Transactional
    public AdminPublicationDetailResponse approve(
            Long adminMemberId,
            Long publicationId,
            PublicationApproveRequest request
    ) {
        requireAdmin(adminMemberId);

        CasePublication publication = findPendingForUpdate(
                publicationId
        );

        String title = singleLine(request.title(), "제목");
        String channel = singleLine(request.channel(), "접근 경로");
        String demand = singleLine(request.demand(), "요구 명목");

        String summary = normalizeSummary(request.summary());

        publication.approve(
                title,
                channel,
                demand,
                summary,
                adminMemberId
        );

        publicationRepository.flush();

        return AdminPublicationDetailResponse.from(publication);
    }

    @Transactional
    public AdminPublicationDetailResponse reject(
            Long adminMemberId,
            Long publicationId,
            PublicationRejectRequest request
    ) {
        requireAdmin(adminMemberId);

        CasePublication publication = findPendingForUpdate(
                publicationId
        );

        String reason = request.reason().trim();

        if (reason.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "반려 사유를 입력해주세요."
            );
        }

        publication.reject(reason, adminMemberId);

        publicationRepository.flush();

        return AdminPublicationDetailResponse.from(publication);
    }

    private CasePublication findPendingForUpdate(
            Long publicationId
    ) {
        CasePublication publication = publicationRepository
                .findForUpdateById(publicationId)
                .orElseThrow(this::publicationNotFound);

        if (publication.getStatus()
                != PublicationStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "검수 대기 상태의 사례만 처리할 수 있습니다."
            );
        }

        return publication;
    }

    private void requireAdmin(Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        if (!adminMemberIds.contains(memberId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "관리자 권한이 필요합니다."
            );
        }
    }
    private String singleLine(
            String value,
            String label
    ) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    label + "을 입력해주세요."
            );
        }

        String result = value.trim();

        if (result.contains("\n") || result.contains("\r")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    label + "에는 줄바꿈을 넣을 수 없습니다."
            );
        }

        return result;
    }
    private String normalizeSummary(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "진행 과정 요약을 입력해주세요."
            );
        }

        String normalized = value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();

        String[] lines = normalized.split("\n", -1);

        if (lines.length > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "진행 과정 요약은 1~5줄로 작성해주세요."
            );
        }

        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].trim();

            if (lines[i].isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "진행 과정 요약에는 빈 줄을 넣을 수 없습니다."
                );
            }
        }

        return String.join("\n", lines);
    }
    private ResponseStatusException publicationNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "공개 신청 내역을 찾을 수 없습니다."
        );
    }
}