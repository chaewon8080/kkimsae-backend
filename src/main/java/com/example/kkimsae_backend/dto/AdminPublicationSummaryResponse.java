package com.example.kkimsae_backend.dto;

import com.example.kkimsae_backend.entity.CasePublication;
import com.example.kkimsae_backend.entity.PublicationStatus;

import java.time.LocalDateTime;

public record AdminPublicationSummaryResponse(
        Long id,
        Long incidentId,
        String publicTitle,
        PublicationStatus status,
        LocalDateTime requestedAt
) {

    public static AdminPublicationSummaryResponse from(
            CasePublication publication
    ) {
        return new AdminPublicationSummaryResponse(
                publication.getId(),
                publication.getIncidentId(),
                publication.getPublicTitle(),
                publication.getStatus(),
                publication.getRequestedAt()
        );
    }
}