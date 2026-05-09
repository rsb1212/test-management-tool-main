package com.testmgmt.repository;

import com.testmgmt.entity.TestPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// ─── TestPlanRepository ──────────────────────────────────────
@Repository
public interface TestPlanRepository extends JpaRepository<TestPlan, UUID> {
    Optional<TestPlan> findByCode(String code);

    Page<TestPlan> findByProject_Id(UUID projectId, Pageable pageable);
}
