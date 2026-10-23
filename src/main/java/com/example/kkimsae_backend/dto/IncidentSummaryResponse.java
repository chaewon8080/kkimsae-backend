package com.example.kkimsae_backend.dto;

import com.example.kkimsae_backend.entity.Incident;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class IncidentSummaryResponse {

    private Long id;
    private String title;
    private String channel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static IncidentSummaryResponse from(
            Incident incident
    ) {
        return new IncidentSummaryResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getChannel(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }
}