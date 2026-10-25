package com.example.kkimsae_backend.service;


import com.example.kkimsae_backend.dto.CasePublicationResponse;
import com.example.kkimsae_backend.entity.CasePublication;
import com.example.kkimsae_backend.entity.Incident;
import com.example.kkimsae_backend.entity.PublicationStatus;

import com.example.kkimsae_backend.repository.CasePublicationRepository;
import com.example.kkimsae_backend.repository.IncidentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicationSummaryStateService {

    private final IncidentRepository incidentRepository;
    private final CasePublicationRepository publicationRepository;

    @Transactional
    public SummaryJob begin(
            Long memberId,
            Long incidentId
    ) {
        requireMyIncident(memberId, incidentId);

        CasePublication publication = publicationRepository
                .findForUpdateByIncidentId(incidentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "먼저 공개 신청을 해주세요."
                ));

        PublicationStatus status = publication.getStatus();

        boolean retryExpiredJob =
                status == PublicationStatus.GENERATING_SUMMARY
                        && publication.getSummaryStartedAt() != null
                        && publication.getSummaryStartedAt()
                        .isBefore(LocalDateTime.now().minusMinutes(2));

        if (status == PublicationStatus.GENERATING_SUMMARY
                && !retryExpiredJob) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 AI 요약을 생성하고 있습니다."
            );
        }

        if (status != PublicationStatus.PENDING_SUMMARY
                && status != PublicationStatus.SUMMARY_FAILED
                && !retryExpiredJob) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "현재 상태에서는 요약을 생성할 수 없습니다."
            );
        }

        String jobId = UUID.randomUUID().toString();

        publication.startSummary(jobId);

        return new SummaryJob(
                publication.getId(),
                jobId,
                publication.getSourceTitle(),
                publication.getSourceDescription(),
                publication.getSourceChannel(),
                publication.getSourceDemand(),
                publication.getSourceTimeline()
        );
    }

    @Transactional
    public CasePublicationResponse complete(
            SummaryJob job,
            PublicationSummaryAiService.SummaryDraft draft
    ) {
        CasePublication publication = publicationRepository
                .findForUpdateById(job.publicationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "공개 신청 내역이 삭제되었습니다."
                ));

        if (!publication.isCurrentSummaryJob(job.jobId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "공개 신청이 철회되거나 변경되어 요약 결과를 반영하지 않았습니다."
            );
        }

        publication.completeSummary(
                draft.title(),
                draft.channel(),
                draft.demand(),
                draft.summary()
        );

        publicationRepository.flush();

        return CasePublicationResponse.from(publication);
    }

    @Transactional
    public void fail(SummaryJob job) {
        publicationRepository
                .findForUpdateById(job.publicationId())
                .ifPresent(publication -> {
                    if (publication.isCurrentSummaryJob(job.jobId())) {
                        publication.failSummary();
                    }
                });
    }

    private Incident requireMyIncident(
            Long memberId,
            Long incidentId
    ) {
        if (memberId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        return incidentRepository
                .findByIdAndMember_Id(incidentId, memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사건을 찾을 수 없습니다."
                ));
    }

    public record SummaryJob(
            Long publicationId,
            String jobId,
            String title,
            String description,
            String channel,
            String demand,
            String timeline
    ) {
    }
}