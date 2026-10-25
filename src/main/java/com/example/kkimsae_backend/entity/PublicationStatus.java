package com.example.kkimsae_backend.entity;

public enum PublicationStatus {

    PENDING_SUMMARY,

    // AI 요약 생성 중
    GENERATING_SUMMARY,

    SUMMARY_FAILED,

    PENDING_REVIEW,

    APPROVED,

    REJECTED,

    WITHDRAWN
}