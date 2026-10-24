package com.example.kkimsae_backend.dto;

import com.example.kkimsae_backend.entity.TimelineEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TimelineEventResponse {

    private Long id;
    private Long incidentId;
    private LocalDateTime occurredAt;
    private String content;
    private String tag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TimelineEventResponse from(
            TimelineEvent event
    ) {
        return new TimelineEventResponse(
                event.getId(),
                event.getIncident().getId(),
                event.getOccurredAt(),
                event.getContent(),
                event.getTag(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}