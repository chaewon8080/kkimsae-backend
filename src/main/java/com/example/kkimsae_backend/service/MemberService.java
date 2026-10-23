package com.example.kkimsae_backend.service;

import com.example.kkimsae_backend.dto.*;
import com.example.kkimsae_backend.entity.Member;
import com.example.kkimsae_backend.repository.MemberRepository;

import com.example.kkimsae_backend.repository.RefreshTokenRepository;
import com.example.kkimsae_backend.security.JwtProvider;
import com.example.kkimsae_backend.security.RefreshTokenProvider;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.example.kkimsae_backend.dto.MemberResponse;
import com.example.kkimsae_backend.security.JwtProvider;

import com.example.kkimsae_backend.entity.RefreshToken;
import com.example.kkimsae_backend.repository.RefreshTokenRepository;
import com.example.kkimsae_backend.security.RefreshTokenProvider;
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProvider refreshTokenProvider;

    @Transactional
    public Long signup(SignupRequest request) {
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        String nickname = request.getNickname().trim();

        if (nickname.length() < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "닉네임은 공백을 제외하고 2자 이상 입력해주세요."
            );
        }

        // BCrypt는 비밀번호를 UTF-8 기준 최대 72바이트까지 처리
        if (request.getPassword()
                .getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다."
            );
        }

        if (memberRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 가입한 이메일입니다."
            );
        }

        String passwordHash =
                passwordEncoder.encode(request.getPassword());

        Member member = new Member(
                email,
                passwordHash,
                nickname
        );

        try {
            // 동시에 같은 이메일로 가입하는 경우도 DB에서 확인
            return memberRepository.saveAndFlush(member).getId();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "회원 정보가 기존 데이터와 중복되어 저장하지 못했습니다."
            );
        }
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(this::invalidCredentials);

        if (request.getPassword()
                .getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalidCredentials();
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                member.getPasswordHash()
        )) {
            throw invalidCredentials();
        }

        return issueTokens(member);
    }

    private TokenResponse issueTokens(Member member) {
        String accessToken =
                jwtProvider.createAccessToken(member.getId());

        String refreshToken =
                refreshTokenProvider.createToken();

        RefreshToken savedToken = new RefreshToken(
                member,
                refreshTokenProvider.hash(refreshToken),
                refreshTokenProvider.createExpiresAt()
        );

        refreshTokenRepository.save(savedToken);

        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProvider.getAccessTokenExpiresIn()
        );
    }


    @Transactional
    public TokenResponse reissue(RefreshTokenRequest request) {
        String tokenHash = refreshTokenProvider.hash(
                request.getRefreshToken()
        );

        RefreshToken refreshToken =
                refreshTokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "유효하지 않은 Refresh Token입니다."
                                )
                        );

        if (refreshToken.isExpired()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh Token이 만료되었습니다. 다시 로그인해주세요."
            );
        }

        Member member = refreshToken.getMember();

        // 사용한 Refresh Token 폐기
        refreshTokenRepository.delete(refreshToken);
        refreshTokenRepository.flush();

        // 새 Access Token + Refresh Token 발급
        return issueTokens(member);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String tokenHash = refreshTokenProvider.hash(
                request.getRefreshToken()
        );

        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "존재하지 않는 회원입니다."
                ));

        return MemberResponse.from(member);
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "이메일 또는 비밀번호가 일치하지 않습니다."
        );
    }
}