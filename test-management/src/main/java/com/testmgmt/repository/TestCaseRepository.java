package com.testmgmt.repository;

import com.testmgmt.entity.TestCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// ─── TestCaseRepository ──────────────────────────────────────
@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {
    Optional<TestCase> findByCode(String code);

    @Query("""
                SELECT tc FROM TestCase tc
                WHERE tc.project.id = :projectId
                  AND (:priority IS NULL OR tc.priority = :priority)
                  AND (:status   IS NULL OR tc.status   = :status)
                  AND (:moduleId IS NULL OR tc.module.id = :moduleId)
            """)
    Page<TestCase> findByFilters(
            @Param("projectId") UUID projectId,
            @Param("priority") TestCase.Priority priority,
            @Param("status") TestCase.TestStatus status,
            @Param("moduleId") UUID moduleId,
            Pageable pageable
    );

    long countByProject_IdAndStatus(UUID projectId, TestCase.TestStatus status);
}
