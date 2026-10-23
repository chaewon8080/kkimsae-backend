package com.example.kkimsae_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "incidents",
        indexes = {
                @Index(
                        name = "idx_incident_member",
                        columnList = "member_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String channel;

    @Column(length = 200)
    private String demand;

    @Column(length = 200)
    private String company;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Incident(
            Member member,
            String title,
            String description,
            String channel,
            String demand,
            String company
    ) {
        this.member = member;
        this.title = title;
        this.description = description;
        this.channel = channel;
        this.demand = demand;
        this.company = company;
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

    public void update(
            String title,
            String description,
            String channel,
            String demand,
            String company
    ) {
        this.title = title;
        this.description = description;
        this.channel = channel;
        this.demand = demand;
        this.company = company;

        // 단서만 변경된 경우에도 사건 수정 시각 갱신
        this.updatedAt = LocalDateTime.now();
    }
}