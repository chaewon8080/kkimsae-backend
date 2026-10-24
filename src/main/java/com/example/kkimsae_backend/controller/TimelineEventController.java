package com.example.kkimsae_backend.controller;

import com.example.kkimsae_backend.dto.TimelineEventRequest;
import com.example.kkimsae_backend.dto.TimelineEventResponse;
import com.example.kkimsae_backend.service.TimelineEventService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/incidents/{incidentId}/timeline")
@RequiredArgsConstructor
public class TimelineEventController {

    private final TimelineEventService timelineEventService;

    @PostMapping
    public ResponseEntity<TimelineEventResponse> create(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("incidentId") Long incidentId,
            @Valid @RequestBody TimelineEventRequest request
    ) {
        TimelineEventResponse response =
                timelineEventService.create(
                        memberId,
                        incidentId,
                        request
                );

        URI location = URI.create(
                "/api/incidents/" + incidentId
                        + "/timeline/" + response.getId()
        );

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TimelineEventResponse>> list(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("incidentId") Long incidentId
    ) {
        return ResponseEntity.ok(
                timelineEventService.list(memberId, incidentId)
        );
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<TimelineEventResponse> update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("incidentId") Long incidentId,
            @PathVariable("eventId") Long eventId,
            @Valid @RequestBody TimelineEventRequest request
    ) {
        return ResponseEntity.ok(
                timelineEventService.update(
                        memberId,
                        incidentId,
                        eventId,
                        request
                )
        );
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("incidentId") Long incidentId,
            @PathVariable("eventId") Long eventId
    ) {
        timelineEventService.delete(
                memberId,
                incidentId,
                eventId
        );

        return ResponseEntity.noContent().build();
    }
}