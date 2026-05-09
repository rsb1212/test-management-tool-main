package com.testmgmt.repository;

import com.testmgmt.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// ─── ProjectRepository ───────────────────────────────────────
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findByCode(String code);

    List<Project> findByActiveTrue();

    Page<Project> findByOwner_Id(UUID ownerId, Pageable pageable);
}
