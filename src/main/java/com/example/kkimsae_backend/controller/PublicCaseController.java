package com.example.kkimsae_backend.controller;

import com.example.kkimsae_backend.dto.PublicCaseResponse;
import com.example.kkimsae_backend.dto.PublicCaseSearchResponse;

import com.example.kkimsae_backend.service.PublicCaseService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public-cases")
@RequiredArgsConstructor
public class PublicCaseController {

    private final PublicCaseService publicCaseService;

    @GetMapping("/search")
    public ResponseEntity<PublicCaseSearchResponse> search(
            @RequestParam(name="query", defaultValue = "") String query,

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
                publicCaseService.search(
                        query,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{publicationId}")
    public ResponseEntity<PublicCaseResponse> detail(
            @PathVariable("publicationId") Long publicationId
    ) {
        return ResponseEntity.ok(
                publicCaseService.detail(publicationId)
        );
    }
}