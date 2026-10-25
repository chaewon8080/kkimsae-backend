package com.example.kkimsae_backend.controller;

import com.example.kkimsae_backend.dto.AdminPublicationDetailResponse;
import com.example.kkimsae_backend.dto.AdminPublicationSummaryResponse;
import com.example.kkimsae_backend.dto.PublicationApproveRequest;
import com.example.kkimsae_backend.dto.PublicationRejectRequest;

import com.example.kkimsae_backend.entity.PublicationStatus;
import com.example.kkimsae_backend.service.AdminPublicationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/publications")
@RequiredArgsConstructor
public class AdminPublicationController {

    private final AdminPublicationService adminPublicationService;

    @GetMapping
    public ResponseEntity<Page<AdminPublicationSummaryResponse>> list(
            @AuthenticationPrincipal Long memberId,

            @RequestParam(
                    name = "status",
                    defaultValue = "PENDING_REVIEW"
            )
            PublicationStatus status,

            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    name = "size",
                    defaultValue = "20"
            )
            int size
    ) {
        return ResponseEntity.ok(
                adminPublicationService.list(
                        memberId,
                        status,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{publicationId}")
    public ResponseEntity<AdminPublicationDetailResponse> detail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("publicationId") Long publicationId
    ) {
        return ResponseEntity.ok(
                adminPublicationService.detail(
                        memberId,
                        publicationId
                )
        );
    }

    @PostMapping("/{publicationId}/approve")
    public ResponseEntity<AdminPublicationDetailResponse> approve(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("publicationId") Long publicationId,
            @Valid @RequestBody PublicationApproveRequest request
    ) {
        return ResponseEntity.ok(
                adminPublicationService.approve(
                        memberId,
                        publicationId,
                        request
                )
        );
    }

    @PostMapping("/{publicationId}/reject")
    public ResponseEntity<AdminPublicationDetailResponse> reject(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("publicationId") Long publicationId,
            @Valid @RequestBody PublicationRejectRequest request
    ) {
        return ResponseEntity.ok(
                adminPublicationService.reject(
                        memberId,
                        publicationId,
                        request
                )
        );
    }
}