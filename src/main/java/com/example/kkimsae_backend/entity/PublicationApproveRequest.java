package com.example.kkimsae_backend.dto;

import jakarta.validation.constraints.NotBlank;

public record PublicationApproveRequest(

        @NotBlank
        String title,

        @NotBlank
        String channel,

        @NotBlank
        String demand,

        @NotBlank
        String summary

) {
}