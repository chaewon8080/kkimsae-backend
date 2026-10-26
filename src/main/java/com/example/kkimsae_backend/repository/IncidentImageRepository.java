package com.example.kkimsae_backend.repository;

import com.example.kkimsae_backend.entity.IncidentImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentImageRepository
        extends JpaRepository<IncidentImage, Long> {

    List<IncidentImage> findByIncident_IdOrderByUploadedAtDescIdDesc(
            Long incidentId
    );
}