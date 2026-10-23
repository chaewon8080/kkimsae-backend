package com.example.kkimsae_backend.controller;

import com.example.kkimsae_backend.dto.IncidentCreateRequest;
import com.example.kkimsae_backend.dto.IncidentResponse;
import com.example.kkimsae_backend.dto.IncidentSummaryResponse;
import com.example.kkimsae_backend.service.IncidentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    public ResponseEntity<IncidentResponse> create(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody IncidentCreateRequest request
    ) {
        IncidentResponse response =
                incidentService.create(memberId, request);

        return ResponseEntity
                .created(URI.create(
                        "/api/incidents/" + response.getId()
                ))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<IncidentSummaryResponse>> list(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(
                incidentService.getMyIncidents(memberId)
        );
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentResponse> detail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("incidentId") Long incidentId
    ) {
        return ResponseEntity.ok(
                incidentService.getMyIncident(
                        memberId,
                        incidentId
                )
        );
    }

    @PutMapping("/{incidentId}")
    public ResponseEntity<IncidentResponse> update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("incidentId") Long incidentId,
            @Valid @RequestBody IncidentCreateRequest request
    ) {
        return ResponseEntity.ok(
                incidentService.update(
                        memberId,
                        incidentId,
                        request
                )
        );
    }

    @DeleteMapping("/{incidentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("incidentId") Long incidentId
    ) {
        incidentService.delete(memberId, incidentId);

        return ResponseEntity.noContent().build();
    }
}
