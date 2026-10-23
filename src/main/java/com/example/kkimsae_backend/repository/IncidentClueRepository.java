package com.example.kkimsae_backend.repository;

import com.example.kkimsae_backend.entity.IncidentClue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentClueRepository
        extends JpaRepository<IncidentClue, Long> {

    List<IncidentClue> findByIncident_IdOrderByIdAsc(
            Long incidentId
    );

    void deleteByIncident_Id(Long incidentId);
}