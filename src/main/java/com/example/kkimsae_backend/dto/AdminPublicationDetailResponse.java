package com.example.kkimsae_backend.dto;

import com.example.kkimsae_backend.entity.CasePublication;
import com.example.kkimsae_backend.entity.PublicationStatus;

import java.time.LocalDateTime;

public record AdminPublicationDetailResponse(
        Long id,
        Long incidentId,

        String sourceTitle,
        String sourceDescription,
        String sourceChannel,
        String sourceDemand,
        String sourceTimeline,

        String publicTitle,
        String publicChannel,
        String publicDemand,
        String publicSummary,

        PublicationStatus status,

        String reviewNote,
        Long reviewedBy,
        LocalDateTime reviewedAt,

        LocalDateTime requestedAt,
        LocalDateTime updatedAt
) {

    public static AdminPublicationDetailResponse from(
            CasePublication publication
    ) {
        return new AdminPublicationDetailResponse(
                publication.getId(),
                publication.getIncidentId(),

                publication.getSourceTitle(),
                publication.getSourceDescription(),
                publication.getSourceChannel(),
                publication.getSourceDemand(),
                publication.getSourceTimeline(),

                publication.getPublicTitle(),
                publication.getPublicChannel(),
                publication.getPublicDemand(),
                publication.getPublicSummary(),

                publication.getStatus(),

                publication.getReviewNote(),
                publication.getReviewedBy(),
                publication.getReviewedAt(),

                publication.getRequestedAt(),
                publication.getUpdatedAt()
        );
    }
}