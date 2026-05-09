package com.testmgmt.repository;

import com.testmgmt.entity.TestRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// ─── TestRunRepository ───────────────────────────────────────
@Repository
public interface TestRunRepository extends JpaRepository<TestRun, UUID> {
    Optional<TestRun> findByCode(String code);

    Page<TestRun> findByTestPlan_Id(UUID testPlanId, Pageable pageable);

    Page<TestRun> findByAssignedTo_Id(UUID userId, Pageable pageable);
}
