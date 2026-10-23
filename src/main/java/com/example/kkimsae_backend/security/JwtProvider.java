package com.example.kkimsae_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtProvider {

    private static final String ISSUER = "kkimsae";

    private final SecretKey key;
    private final Duration accessTokenExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}")
            Duration accessTokenExpiration
    ) {
        this.key = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secret)
        );
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String createAccessToken(Long memberId) {
        Instant now = Instant.now();

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(memberId.toString())
                .claim("tokenType", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plus(accessTokenExpiration)
                ))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Long getMemberId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .require("tokenType", "access")
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (claims.getExpiration() == null) {
            throw new JwtException("만료 시간이 없는 토큰입니다.");
        }

        try {
            Long memberId = Long.valueOf(claims.getSubject());

            if (memberId <= 0) {
                throw new IllegalArgumentException();
            }

            return memberId;
        } catch (IllegalArgumentException exception) {
            throw new JwtException("회원 정보가 올바르지 않습니다.");
        }
    }

    public long getAccessTokenExpiresIn() {
        return accessTokenExpiration.toSeconds();
    }
}