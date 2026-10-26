package com.example.kkimsae_backend.repository;

import com.example.kkimsae_backend.entity.CasePublication;
import com.example.kkimsae_backend.entity.PublicationStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CasePublicationRepository
        extends JpaRepository<CasePublication, Long>,
        JpaSpecificationExecutor<CasePublication> {

    Optional<CasePublication> findByIncidentId(Long incidentId);

    void deleteByIncidentId(Long incidentId);

    Page<CasePublication> findByStatus(
            PublicationStatus status,
            Pageable pageable
    );

    Optional<CasePublication> findByIdAndStatus(
            Long id,
            PublicationStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from CasePublication p
            where p.incidentId = :incidentId
            """)
    Optional<CasePublication> findForUpdateByIncidentId(
            @Param("incidentId") Long incidentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from CasePublication p
            where p.id = :id
            """)
    Optional<CasePublication> findForUpdateById(
            @Param("id") Long id
    );
}