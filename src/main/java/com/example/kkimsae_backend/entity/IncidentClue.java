package com.example.kkimsae_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "incident_clues",
        indexes = {
                @Index(
                        name = "idx_incident_clue_incident",
                        columnList = "incident_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IncidentClue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClueType type;

    @Column(
            name = "clue_value",
            nullable = false,
            length = 2048
    )
    private String value;

    // 계좌번호일 때 사용. 다른 종류는 null.
    @Column(name = "bank_name", length = 100)
    private String bankName;

    public IncidentClue(
            Incident incident,
            ClueType type,
            String value,
            String bankName
    ) {
        this.incident = incident;
        this.type = type;
        this.value = value;
        this.bankName = bankName;
    }
}