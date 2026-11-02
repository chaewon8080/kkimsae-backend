package com.example.kkimsae_backend.controller;

import com.example.kkimsae_backend.dto.IncidentImageResponse;
import com.example.kkimsae_backend.service.IncidentImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/incidents/{incidentId}/images")
@RequiredArgsConstructor
public class IncidentImageController {

    private final IncidentImageService incidentImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IncidentImageResponse> upload(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("incidentId") Long incidentId,
            @RequestParam("file") MultipartFile file
    ) {
        IncidentImageResponse response = incidentImageService.upload(
                memberId,
                incidentId,
                file
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("incidentId") Long incidentId,
            @PathVariable("imageId") Long imageId
    ) {
        incidentImageService.deleteImage(
                memberId,
                incidentId,
                imageId
        );

        return ResponseEntity.noContent().build();
    }
}