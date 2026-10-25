package com.example.kkimsae_backend.service;

import com.example.kkimsae_backend.dto.CasePublicationResponse;
import com.example.kkimsae_backend.entity.CasePublication;
import com.example.kkimsae_backend.entity.Incident;
import com.example.kkimsae_backend.entity.PublicationStatus;
import com.example.kkimsae_backend.repository.CasePublicationRepository;
import com.example.kkimsae_backend.repository.IncidentRepository;
import com.example.kkimsae_backend.repository.TimelineEventRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CasePublicationService {

    private final IncidentRepository incidentRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final CasePublicationRepository publicationRepository;
    private final EntityManager entityManager;

    @Transactional
    public CasePublicationResponse request(
            Long memberId,
            Long incidentId
    ) {
        Incident incident = lockMyIncident(memberId, incidentId);

        CasePublication publication = publicationRepository
                .findByIncidentId(incidentId)
                .orElse(null);

        if (publication != null && isProcessing(publication.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 공개 신청을 처리하고 있습니다."
            );
        }

        if (publication != null
                && publication.getStatus() == PublicationStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 승인된 사례입니다. 내용을 갱신하려면 공개 철회 후 다시 신청해주세요."
            );
        }

        String timeline = createTimelineSnapshot(incidentId);

        if (publication == null) {
            publication = new CasePublication(
                    incidentId,
                    incident.getTitle(),
                    incident.getDescription(),
                    incident.getChannel(),
                    incident.getDemand(),
                    timeline
            );
        } else {
            // 실패·반려·철회된 신청은 최신 사건 내용으로 다시 신청
            publication.requestAgain(
                    incident.getTitle(),
                    incident.getDescription(),
                    incident.getChannel(),
                    incident.getDemand(),
                    timeline
            );
        }

        publicationRepository.saveAndFlush(publication);

        return CasePublicationResponse.from(publication);
    }

    @Transactional(readOnly = true)
    public CasePublicationResponse getMyPublication(
            Long memberId,
            Long incidentId
    ) {
        findMyIncident(memberId, incidentId);

        CasePublication publication = publicationRepository
                .findByIncidentId(incidentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "공개 신청 내역이 없습니다."
                ));

        return CasePublicationResponse.from(publication);
    }

    @Transactional
    public void withdraw(
            Long memberId,
            Long incidentId
    ) {
        lockMyIncident(memberId, incidentId);

        publicationRepository.findByIncidentId(incidentId)
                .ifPresent(CasePublication::withdraw);
    }

    private boolean isProcessing(PublicationStatus status) {
        return status == PublicationStatus.PENDING_SUMMARY
                || status == PublicationStatus.GENERATING_SUMMARY
                || status == PublicationStatus.PENDING_REVIEW;
    }

    private String createTimelineSnapshot(Long incidentId) {
        return timelineEventRepository
                .findByIncident_IdOrderByOccurredAtAscIdAsc(incidentId)
                .stream()
                .map(event -> {
                    String tag = event.getTag() == null
                            ? ""
                            : event.getTag();

                    return """
                            [타임라인 기록]
                            발생 일시: %s
                            기록 내용:
                            %s
                            단계 요약: %s
                            """.formatted(
                            event.getOccurredAt(),
                            event.getContent(),
                            tag
                    );
                })
                .collect(Collectors.joining("\n"));
    }

    private Incident findMyIncident(
            Long memberId,
            Long incidentId
    ) {
        requireLogin(memberId);

        return incidentRepository
                .findByIdAndMember_Id(incidentId, memberId)
                .orElseThrow(this::incidentNotFound);
    }

    private Incident lockMyIncident(
            Long memberId,
            Long incidentId
    ) {
        requireLogin(memberId);

        Incident incident = entityManager.find(
                Incident.class,
                incidentId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (incident == null
                || !memberId.equals(incident.getMember().getId())) {
            throw incidentNotFound();
        }

        return incident;
    }

    private void requireLogin(Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }
    }

    private ResponseStatusException incidentNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "사건을 찾을 수 없습니다."
        );
    }
}