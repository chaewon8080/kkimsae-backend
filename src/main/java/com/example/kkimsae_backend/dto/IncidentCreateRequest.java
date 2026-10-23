package com.example.kkimsae_backend.dto;

import com.example.kkimsae_backend.entity.ClueType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class IncidentCreateRequest {

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    @Size(max = 10000)
    private String description;

    @Size(max = 100)
    private String channel;

    @Size(max = 200)
    private String demand;

    @Size(max = 200)
    private String company;

    @Valid
    @NotNull
    @Size(max = 30)
    private List<@NotNull ClueRequest> clues = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ClueRequest {

        @NotNull
        private ClueType type;

        @NotBlank
        @Size(max = 2048)
        private String value;

        @Size(max = 100)
        private String bankName;
    }
}