package com.example.kkimsae_backend.repository;

import com.example.kkimsae_backend.entity.Incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository
        extends JpaRepository<Incident, Long> {

    List<Incident> findByMember_IdOrderByCreatedAtDescIdDesc(
            Long memberId
    );

    Optional<Incident> findByIdAndMember_Id(
            Long id,
            Long memberId
    );
}