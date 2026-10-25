package com.example.kkimsae_backend.dto;

import com.example.kkimsae_backend.entity.CasePublication;
import com.example.kkimsae_backend.entity.PublicationStatus;

import java.time.LocalDateTime;

public record CasePublicationResponse(
        Long id,
        Long incidentId,
        PublicationStatus status,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt,
        LocalDateTime updatedAt
) {

    public static CasePublicationResponse from(
            CasePublication publication
    ) {
        return new CasePublicationResponse(
                publication.getId(),
                publication.getIncidentId(),
                publication.getStatus(),
                publication.getRequestedAt(),
                publication.getReviewedAt(),
                publication.getUpdatedAt()
        );
    }
}