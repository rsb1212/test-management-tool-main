package com.testmgmt.service;

import com.testmgmt.entity.*;
import com.testmgmt.exception.GlobalExceptionHandler.*;
import com.testmgmt.repository.*;
import com.testmgmt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TestPlanService {

    private final TestPlanRepository testPlanRepo;
    private final TestCycleRepository testCycleRepo;
    private final ProjectRepository projectRepo;
    private final TestCaseRepository testCaseRepo;
    private final UserRepository userRepo;
    private final CodeSequenceService codeSeq;

    @Transactional
    public TestPlan create(String name, UUID projectId, String description,
                           String sprintId, LocalDate startDate, LocalDate endDate,
                           List<UUID> testCaseIds, String creatorEmail) {

        Project project = projectRepo.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        User creator = userRepo.findByEmail(creatorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TestPlan plan = TestPlan.builder()
            .code(codeSeq.next("TP"))
            .name(name)
            .project(project)
            .description(description)
            .sprintId(sprintId)
            .startDate(startDate)
            .endDate(endDate)
            .createdBy(creator)
            .build();

        if (testCaseIds != null && !testCaseIds.isEmpty()) {
            testCaseIds.forEach(tcId -> {
                TestCase tc = testCaseRepo.findById(tcId)
                    .orElseThrow(() -> new ResourceNotFoundException("TestCase not found: " + tcId));
                plan.getTestCases().add(tc);
            });
        }

        return testPlanRepo.save(plan);
    }

    @Transactional
    public TestCycle addCycle(UUID planId, String name, String environment) {
        TestPlan plan = findById(planId);
        TestCycle cycle = new TestCycle();
        cycle.setCode(codeSeq.next("CYC"));
        cycle.setName(name);
        cycle.setEnvironment(environment);
        cycle.setTestPlan(plan);
        return testCycleRepo.save(cycle);
    }

    @Transactional
    public TestPlan addTestCases(UUID planId, List<UUID> testCaseIds) {
        TestPlan plan = findById(planId);
        testCaseIds.forEach(tcId -> {
            TestCase tc = testCaseRepo.findById(tcId)
                .orElseThrow(() -> new ResourceNotFoundException("TestCase not found: " + tcId));
            plan.getTestCases().add(tc);
        });
        return testPlanRepo.save(plan);
    }

    @Transactional(readOnly = true)
    public TestPlan findById(UUID id) {
        return testPlanRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TestPlan not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<TestPlan> findByProject(UUID projectId, int page, int size) {
        return testPlanRepo.findByProject_Id(projectId,
            PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public List<TestCycle> getCycles(UUID planId) {
        findById(planId); // validate exists
        return testCycleRepo.findByTestPlan_Id(planId);
    }
}
