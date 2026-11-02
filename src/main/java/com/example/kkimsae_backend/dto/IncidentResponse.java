package com.example.kkimsae_backend.dto;

import com.example.kkimsae_backend.entity.ClueType;
import com.example.kkimsae_backend.entity.Incident;
import com.example.kkimsae_backend.entity.IncidentClue;
import com.example.kkimsae_backend.entity.IncidentImage;
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
    private List<IncidentImageResponse> images;

    // 새 사건 생성 시 사용
    public static IncidentResponse from(
            Incident incident,
            List<IncidentClue> clues
    ) {
        return from(incident, clues, List.of());
    }

    // 사건 상세 조회·수정 시 사용
    public static IncidentResponse from(
            Incident incident,
            List<IncidentClue> clues,
            List<IncidentImage> images
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
                        .toList(),
                images.stream()
                        .map(IncidentImageResponse::from)
                        .toList()
        );
    }

    public IncidentResponse withImages(
            List<IncidentImageResponse> images
    ) {
        this.images = images;
        return this;
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