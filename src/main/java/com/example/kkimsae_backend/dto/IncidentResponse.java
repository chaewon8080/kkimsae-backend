package com.example.kkimsae_backend.dto;

import com.example.kkimsae_backend.entity.ClueType;
import com.example.kkimsae_backend.entity.Incident;
import com.example.kkimsae_backend.entity.IncidentClue;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class IncidentResponse {

    private Long id;
    private String title;
    private String description;
    private String channel;
    private String demand;
    private String company;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ClueResponse> clues;

    public static IncidentResponse from(
            Incident incident,
            List<IncidentClue> clues
    ) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getChannel(),
                incident.getDemand(),
                incident.getCompany(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                clues.stream()
                        .map(ClueResponse::from)
                        .toList()
        );
    }

    @Getter
    @AllArgsConstructor
    public static class ClueResponse {

        private Long id;
        private ClueType type;
        private String value;
        private String bankName;

        public static ClueResponse from(IncidentClue clue) {
            return new ClueResponse(
                    clue.getId(),
                    clue.getType(),
                    clue.getValue(),
                    clue.getBankName()
            );
        }
    }
}