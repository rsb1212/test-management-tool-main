package com.testmgmt.repository;

import com.testmgmt.entity.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// ─── ExecutionRepository ─────────────────────────────────────
@Repository
public interface ExecutionRepository extends JpaRepository<Execution, UUID> {
    Optional<Execution> findByTestRun_IdAndTestCase_Id(UUID runId, UUID testCaseId);

    List<Execution> findByTestRun_Id(UUID runId);

    @Query("""
        SELECT e.result AS result, COUNT(e) AS count
        FROM Execution e
        WHERE e.testRun.id = :runId
        GROUP BY e.result
    """)
    List<Object[]> countByResultForRun(@Param("runId") UUID runId);

    @Query("""
        SELECT e.result AS result, COUNT(e) AS count
        FROM Execution e
        WHERE e.testRun.testPlan.project.id = :projectId
          AND (:sprintId IS NULL OR e.testRun.testPlan.sprintId = :sprintId)
          AND (:env      IS NULL OR e.testRun.environment = :env)
          AND e.executedAt BETWEEN :from AND :to
        GROUP BY e.result
    """)
    List<Object[]> executionSummary(
        @Param("projectId") UUID projectId,
        @Param("sprintId")  String sprintId,
        @Param("env")       String env,
        @Param("from") Instant from,
        @Param("to")        Instant to
    );

    @Query("""
        SELECT tc.module.name AS module, e.result AS result, COUNT(e) AS count
        FROM Execution e
        JOIN e.testCase tc
        WHERE e.testRun.testPlan.project.id = :projectId
          AND e.executedAt BETWEEN :from AND :to
        GROUP BY tc.module.name, e.result
    """)
    List<Object[]> executionByModule(
        @Param("projectId") UUID projectId,
        @Param("from")      Instant from,
        @Param("to")        Instant to
    );
}
