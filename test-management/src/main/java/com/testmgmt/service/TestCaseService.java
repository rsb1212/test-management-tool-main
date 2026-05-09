package com.testmgmt.service;

import com.testmgmt.dto.request.TestCaseDTOs.*;
import com.testmgmt.entity.*;
import com.testmgmt.entity.Module;
import com.testmgmt.exception.GlobalExceptionHandler.*;
import com.testmgmt.repository.ModuleRepository;
import com.testmgmt.repository.ProjectRepository;
import com.testmgmt.repository.*;
import com.testmgmt.repository.TestCaseRepository;
import com.testmgmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class TestCaseService {

    private final TestCaseRepository testCaseRepo;
    private final ProjectRepository projectRepo;
    private final ModuleRepository moduleRepo;
    private final UserRepository userRepo;
    private final CodeSequenceService codeSeq;

    @Transactional
    @CacheEvict(cacheNames = "testCaseList", allEntries = true)
    public TestCase create(CreateRequest req, String creatorEmail) {
        Project project = projectRepo.findById(req.projectId())
            .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + req.projectId()));

        Module module = null;
        if (req.moduleId() != null) {
            module = moduleRepo.findById(req.moduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + req.moduleId()));
        }

        User creator = userRepo.findByEmail(creatorEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String code = codeSeq.next("TC");

        TestCase tc = TestCase.builder()
            .code(code)
            .title(req.title())
            .description(req.description())
            .preconditions(req.preconditions())
            .project(project)
            .module(module)
            .priority(req.priority() != null ? req.priority() : TestCase.Priority.MEDIUM)
            .status(TestCase.TestStatus.DRAFT)
            .createdBy(creator)
            .updatedBy(creator)
            .build();

        // Add steps
        if (req.steps() != null) {
            req.steps().forEach(s -> {
                TestStep step = new TestStep();
                step.setTestCase(tc);
                step.setStepNumber(s.stepNumber());
                step.setAction(s.action());
                step.setExpectedResult(s.expectedResult());
                tc.getSteps().add(step);
            });
        }

        return testCaseRepo.save(tc);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "testCaseList", key = "#projectId + '_' + #priority + '_' + #status + '_' + #page")
    public Page<TestCase> findByFilters(UUID projectId, String priority, String status,
                                        UUID moduleId, int page, int size) {
        TestCase.Priority  p = priority != null ? TestCase.Priority.valueOf(priority.toUpperCase())   : null;
        TestCase.TestStatus s = status  != null ? TestCase.TestStatus.valueOf(status.toUpperCase())   : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return testCaseRepo.findByFilters(projectId, p, s, moduleId, pageable);
    }

    @Transactional(readOnly = true)
    public TestCase findById(UUID id) {
        return testCaseRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Test case not found: " + id));
    }

    @Transactional
    @CacheEvict(cacheNames = "testCaseList", allEntries = true)
    public TestCase update(UUID id, UpdateRequest req, String updaterEmail) {
        TestCase tc = findById(id);
        User updater = userRepo.findByEmail(updaterEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (req.title()        != null) tc.setTitle(req.title());
        if (req.description()  != null) tc.setDescription(req.description());
        if (req.preconditions()!= null) tc.setPreconditions(req.preconditions());
        if (req.priority()     != null) tc.setPriority(req.priority());
        if (req.status()       != null) tc.setStatus(req.status());
        if (req.moduleId()     != null) {
            Module m = moduleRepo.findById(req.moduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));
            tc.setModule(m);
        }
        if (req.steps() != null && !req.steps().isEmpty()) {
            tc.getSteps().clear();
            // Flush orphan removals first to avoid unique(step_number) conflicts on reinsert.
            testCaseRepo.saveAndFlush(tc);
            req.steps().forEach(s -> {
                TestStep step = new TestStep();
                step.setTestCase(tc);
                step.setStepNumber(s.stepNumber());
                step.setAction(s.action());
                step.setExpectedResult(s.expectedResult());
                tc.getSteps().add(step);
            });
        }
        tc.setUpdatedBy(updater);
        return testCaseRepo.save(tc);
    }

    @Transactional
    @CacheEvict(cacheNames = "testCaseList", allEntries = true)
    public void delete(UUID id) {
        TestCase tc = findById(id);
        testCaseRepo.delete(tc);
    }
}
