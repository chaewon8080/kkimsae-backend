package com.example.kkimsae_backend.repository;

import com.example.kkimsae_backend.entity.IncidentImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
public interface IncidentImageRepository
        extends JpaRepository<IncidentImage, Long> {

    List<IncidentImage> findByIncident_IdOrderByUploadedAtDescIdDesc(
            Long incidentId
    );

    Optional<IncidentImage> findByIdAndIncident_Id(
            Long imageId,
            Long incidentId
    );
}