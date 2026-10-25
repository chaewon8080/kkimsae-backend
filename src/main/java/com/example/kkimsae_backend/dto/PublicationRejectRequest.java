package com.example.kkimsae_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicationRejectRequest(

        @NotBlank
        @Size(max = 1000)
        String reason

) {
}