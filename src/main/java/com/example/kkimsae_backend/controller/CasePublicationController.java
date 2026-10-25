package com.example.kkimsae_backend.controller;

import com.example.kkimsae_backend.dto.CasePublicationResponse;

import com.example.kkimsae_backend.service.CasePublicationService;
import com.example.kkimsae_backend.service.PublicationSummaryService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents/{incidentId}/publication")
@RequiredArgsConstructor
public class CasePublicationController {

    private final CasePublicationService publicationService;
    private final PublicationSummaryService summaryService;

    @PostMapping
    public ResponseEntity<CasePublicationResponse> request(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("incidentId") Long incidentId
    ) {
        return ResponseEntity.ok(
                publicationService.request(memberId, incidentId)
        );
    }

    @GetMapping
    public ResponseEntity<CasePublicationResponse> detail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("incidentId") Long incidentId
    ) {
        return ResponseEntity.ok(
                publicationService.getMyPublication(
                        memberId,
                        incidentId
                )
        );
    }

    @PostMapping("/summary")
    public ResponseEntity<CasePublicationResponse> generateSummary(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("incidentId") Long incidentId
    ) {
        return ResponseEntity.ok(
                summaryService.generate(memberId, incidentId)
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("incidentId") Long incidentId
    ) {
        publicationService.withdraw(memberId, incidentId);

        return ResponseEntity.noContent().build();
    }
}