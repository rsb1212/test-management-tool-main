package com.testmgmt.service;

import com.testmgmt.entity.*;
import com.testmgmt.exception.GlobalExceptionHandler.*;
import com.testmgmt.repository.*;
import com.testmgmt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TestRunService {

    private final TestRunRepository testRunRepo;
    private final TestPlanRepository testPlanRepo;
    private final TestCycleRepository testCycleRepo;
    private final UserRepository userRepo;
    private final ExecutionRepository executionRepo;
    private final TestCaseRepository   testCaseRepo;
    private final CodeSequenceService codeSeq;

    // ── Create Test Run ───────────────────────────────────────
    @Transactional
    public TestRun createRun(UUID testPlanId, UUID cycleId, String environment,
                             String buildVersion, UUID assignedToId, String creatorEmail) {

        TestPlan plan = testPlanRepo.findById(testPlanId)
            .orElseThrow(() -> new ResourceNotFoundException("TestPlan not found: " + testPlanId));

        TestCycle cycle = null;
        if (cycleId != null) {
            cycle = testCycleRepo.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("TestCycle not found: " + cycleId));
        }

        User assignee = null;
        if (assignedToId != null) {
            assignee = userRepo.findById(assignedToId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + assignedToId));
        }

        User creator = userRepo.findByEmail(creatorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TestRun run = TestRun.builder()
            .code(codeSeq.next("TR"))
            .testPlan(plan)
            .testCycle(cycle)
            .environment(environment)
            .buildVersion(buildVersion)
            .assignedTo(assignee)
            .status(TestRun.RunStatus.IN_PROGRESS)
            .totalTests(plan.getTestCases().size())
            .startedAt(Instant.now())
            .createdBy(creator)
            .build();

        return testRunRepo.save(run);
    }

    // ── Execute a test case inside a run ──────────────────────
    @Transactional
    public Execution executeTestCase(UUID runId, UUID testCaseId, String resultStr,
                                     String actualResult, String comment,
                                     Integer durationSecs, String executorEmail) {

        TestRun run = testRunRepo.findById(runId)
            .orElseThrow(() -> new ResourceNotFoundException("TestRun not found: " + runId));

        TestCase testCase = testCaseRepo.findById(testCaseId)
            .orElseThrow(() -> new ResourceNotFoundException("TestCase not found: " + testCaseId));

        // Check for duplicate execution
        if (executionRepo.findByTestRun_IdAndTestCase_Id(runId, testCaseId).isPresent()) {
            throw new ConflictException("Test case " + testCaseId + " already executed in run " + runId);
        }

        User executor = userRepo.findByEmail(executorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Execution.ExecResult result = Execution.ExecResult.valueOf(resultStr.toUpperCase());

        Execution execution = Execution.builder()
            .code(codeSeq.next("EX"))
            .testRun(run)
            .testCase(testCase)
            .result(result)
            .actualResult(actualResult)
            .comment(comment)
            .executedBy(executor)
            .executedAt(Instant.now())
            .durationSecs(durationSecs)
            .build();

        Execution saved = executionRepo.save(execution);

        // Check if all test cases have been executed → auto-complete the run
        long executed = executionRepo.findByTestRun_Id(runId).size();
        if (executed >= run.getTotalTests()) {
            run.setStatus(TestRun.RunStatus.COMPLETED);
            run.setCompletedAt(Instant.now());
            testRunRepo.save(run);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public TestRun findById(UUID id) {
        return testRunRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TestRun not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<TestRun> findByPlan(UUID planId, int page, int size) {
        return testRunRepo.findByTestPlan_Id(planId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }
}
