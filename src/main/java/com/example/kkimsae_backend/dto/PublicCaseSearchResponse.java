package com.example.kkimsae_backend.dto;

import java.util.List;

public record PublicCaseSearchResponse(
        String query,
        List<PublicCaseResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}