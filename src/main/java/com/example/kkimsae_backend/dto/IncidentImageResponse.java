package com.example.kkimsae_backend.dto;

import com.example.kkimsae_backend.entity.IncidentImage;

import java.time.LocalDateTime;

public record IncidentImageResponse(
        Long id,
        Long incidentId,
        String originalName,
        String contentType,
        long size,
        LocalDateTime uploadedAt,
        String url
) {

    public static IncidentImageResponse from(IncidentImage image) {
        return from(image, null);
    }

    public static IncidentImageResponse from(
            IncidentImage image,
            String url
    ) {
        return new IncidentImageResponse(
                image.getId(),
                image.getIncident().getId(),
                image.getOriginalName(),
                image.getContentType(),
                image.getSize(),
                image.getUploadedAt(),
                url
        );
    }
}