package com.example.kkimsae_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "case_publications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_case_publication_incident",
                        columnNames = "incident_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_case_publication_status",
                        columnList = "status"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CasePublication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 기존 사건 삭제 로직과 충돌하지 않도록
     * 이번에는 연관관계 대신 사건 ID를 저장합니다.
     *
     * 사건 삭제 시 공개 신청 데이터도 직접 삭제합니다.
     */
    @Column(name = "incident_id", nullable = false, updatable = false)
    private Long incidentId;

    // 공개 신청 당시의 비공개 원문
    @Column(
            name = "source_title",
            nullable = false,
            length = 150
    )
    private String sourceTitle;

    @Column(
            name = "source_description",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String sourceDescription;

    @Column(name = "source_channel", length = 100)
    private String sourceChannel;

    @Column(name = "source_demand", length = 200)
    private String sourceDemand;

    @Column(
            name = "source_timeline",
            nullable = false,
            columnDefinition = "LONGTEXT"
    )
    private String sourceTimeline;

    // 현재 실행 중인 요약 작업을 구분합니다.
    @Column(name = "summary_job_id", length = 36)
    private String summaryJobId;

    @Column(name = "summary_started_at")
    private LocalDateTime summaryStartedAt;

    /*
     * AI 생성 후 관리자가 확인할 공개용 필드입니다.
     * 원본 제목·접근 경로·요구 명목도 개인정보가 들어갈 수 있으므로
     * 원문을 공개용 필드에 그대로 복사하지 않습니다.
     */
    @Column(name = "public_title", columnDefinition = "LONGTEXT")
    private String publicTitle;

    @Column(name = "public_channel", columnDefinition = "LONGTEXT")
    private String publicChannel;

    @Column(name = "public_demand", columnDefinition = "LONGTEXT")
    private String publicDemand;

    @Column(name = "public_summary", columnDefinition = "LONGTEXT")
    private String publicSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PublicationStatus status;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public CasePublication(
            Long incidentId,
            String sourceTitle,
            String sourceDescription,
            String sourceChannel,
            String sourceDemand,
            String sourceTimeline
    ) {
        this.incidentId = incidentId;

        requestAgain(
                sourceTitle,
                sourceDescription,
                sourceChannel,
                sourceDemand,
                sourceTimeline
        );
    }

    public void requestAgain(
            String sourceTitle,
            String sourceDescription,
            String sourceChannel,
            String sourceDemand,
            String sourceTimeline
    ) {

          //기존 summary 상태 초기화
        this.summaryJobId = null;
        this.summaryStartedAt = null;

        this.sourceTitle = sourceTitle;
        this.sourceDescription = sourceDescription;
        this.sourceChannel = sourceChannel;
        this.sourceDemand = sourceDemand;
        this.sourceTimeline = sourceTimeline;

        // 재신청한 내용은 다시 요약·검수해야 합니다.
        this.publicTitle = null;
        this.publicChannel = null;
        this.publicDemand = null;
        this.publicSummary = null;

        this.reviewNote = null;
        this.reviewedBy = null;
        this.reviewedAt = null;

        this.status = PublicationStatus.PENDING_SUMMARY;
        this.requestedAt = LocalDateTime.now();
    }

    public void withdraw() {
        this.status = PublicationStatus.WITHDRAWN;
        this.summaryJobId = null;
        this.summaryStartedAt = null;
    }
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void startSummary(String jobId) {
        this.summaryJobId = jobId;
        this.summaryStartedAt = LocalDateTime.now();
        this.status = PublicationStatus.GENERATING_SUMMARY;
    }

    public boolean isCurrentSummaryJob(String jobId) {
        return this.status == PublicationStatus.GENERATING_SUMMARY
                && java.util.Objects.equals(this.summaryJobId, jobId);
    }

    public void completeSummary(
            String title,
            String channel,
            String demand,
            String summary
    ) {
        this.publicTitle = title;
        this.publicChannel = channel;
        this.publicDemand = demand;
        this.publicSummary = summary;

        // AI가 만들었다고 바로 공개하지 않습니다.
        this.status = PublicationStatus.PENDING_REVIEW;

        this.summaryJobId = null;
        this.summaryStartedAt = null;
    }

    public void failSummary() {
        this.status = PublicationStatus.SUMMARY_FAILED;
        this.summaryJobId = null;
        this.summaryStartedAt = null;
    }

    public void approve(
            String title,
            String channel,
            String demand,
            String summary,
            Long adminMemberId
    ) {
        this.publicTitle = title;
        this.publicChannel = channel;
        this.publicDemand = demand;
        this.publicSummary = summary;

        this.status = PublicationStatus.APPROVED;

        this.reviewNote = null;
        this.reviewedBy = adminMemberId;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(
            String reason,
            Long adminMemberId
    ) {
        this.status = PublicationStatus.REJECTED;

        this.reviewNote = reason;
        this.reviewedBy = adminMemberId;
        this.reviewedAt = LocalDateTime.now();
    }
}