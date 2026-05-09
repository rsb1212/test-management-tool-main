package com.testmgmt.repository;

import com.testmgmt.entity.TestCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

// ─── TestCycleRepository ─────────────────────────────────────
@Repository
public interface TestCycleRepository extends JpaRepository<TestCycle, UUID> {
    List<TestCycle> findByTestPlan_Id(UUID testPlanId);
}
