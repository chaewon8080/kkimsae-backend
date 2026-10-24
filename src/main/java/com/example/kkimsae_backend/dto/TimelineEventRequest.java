package com.example.kkimsae_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class TimelineEventRequest {

    @NotNull(message = "발생 일시를 입력해주세요.")
    private LocalDateTime occurredAt;

    @NotBlank(message = "기록 내용을 입력해주세요.")
    @Size(max = 10000)
    private String content;

    @Size(max = 100)
    private String tag;
}