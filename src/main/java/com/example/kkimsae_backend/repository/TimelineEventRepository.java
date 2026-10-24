package com.example.kkimsae_backend.repository;

import com.example.kkimsae_backend.entity.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimelineEventRepository
        extends JpaRepository<TimelineEvent, Long> {

    List<TimelineEvent> findByIncident_IdOrderByOccurredAtAscIdAsc(
            Long incidentId
    );

    Optional<TimelineEvent> findByIdAndIncident_Id(
            Long eventId,
            Long incidentId
    );

    void deleteByIncident_Id(Long incidentId);
}