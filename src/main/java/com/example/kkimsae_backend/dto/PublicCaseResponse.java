package com.example.kkimsae_backend.dto;

import com.example.kkimsae_backend.entity.CasePublication;

import java.time.LocalDateTime;

public record PublicCaseResponse(
        Long id,
        String title,
        String channel,
        String demand,
        String summary,
        LocalDateTime publishedAt
) {

    public static PublicCaseResponse from(
            CasePublication publication
    ) {
        return new PublicCaseResponse(
                publication.getId(),
                publication.getPublicTitle(),
                publication.getPublicChannel(),
                publication.getPublicDemand(),
                publication.getPublicSummary(),
                publication.getReviewedAt()
        );
    }
}