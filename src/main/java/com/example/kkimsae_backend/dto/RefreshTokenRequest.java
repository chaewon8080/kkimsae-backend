package com.example.kkimsae_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh Token을 입력해주세요.")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{43}$",
            message = "Refresh Token 형식이 올바르지 않습니다."
    )
    private String refreshToken;
}