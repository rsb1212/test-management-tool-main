package com.testmgmt.service;

import com.testmgmt.entity.*;
import com.testmgmt.exception.GlobalExceptionHandler.*;
import com.testmgmt.repository.*;
import com.testmgmt.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefectService {

    private final DefectRepository defectRepo;
    private final ProjectRepository projectRepo;
    private final TestRunRepository testRunRepo;
    private final TestCaseRepository testCaseRepo;
    private final ExecutionRepository executionRepo;
    private final UserRepository    userRepo;
    private final JiraService       jiraService;
    private final CodeSequenceService codeSeq;

    // Valid status transitions
    private static final Map<Defect.DefectStatus, Set<Defect.DefectStatus>> VALID_TRANSITIONS = Map.of(
        Defect.DefectStatus.NEW,         Set.of(Defect.DefectStatus.OPEN, Defect.DefectStatus.REJECTED),
        Defect.DefectStatus.OPEN,        Set.of(Defect.DefectStatus.IN_PROGRESS, Defect.DefectStatus.REJECTED),
        Defect.DefectStatus.IN_PROGRESS, Set.of(Defect.DefectStatus.FIXED, Defect.DefectStatus.REJECTED),
        Defect.DefectStatus.FIXED,       Set.of(Defect.DefectStatus.RETEST, Defect.DefectStatus.CLOSED),
        Defect.DefectStatus.RETEST,      Set.of(Defect.DefectStatus.CLOSED, Defect.DefectStatus.IN_PROGRESS),
        Defect.DefectStatus.CLOSED,      Set.of(),
        Defect.DefectStatus.REJECTED,    Set.of()
    );

    @Transactional
    public Defect create(UUID projectId, String title, String description,
                         Defect.Severity severity, Defect.DefectPriority priority,
                         UUID linkedRunId, UUID linkedTestCaseId, UUID linkedExecutionId,
                         String environment, String buildVersion, String module,
                         String reporterEmail) {

        Project project = projectRepo.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        User reporter = userRepo.findByEmail(reporterEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TestRun linkedRun = linkedRunId != null
            ? testRunRepo.findById(linkedRunId).orElse(null) : null;
        TestCase linkedTc = linkedTestCaseId != null
            ? testCaseRepo.findById(linkedTestCaseId).orElse(null) : null;
        Execution linkedExec = linkedExecutionId != null
            ? executionRepo.findById(linkedExecutionId).orElse(null) : null;

        Defect defect = Defect.builder()
            .code(codeSeq.next("DEF"))
            .title(title)
            .description(description)
            .severity(severity)
            .priority(priority)
            .status(Defect.DefectStatus.NEW)
            .project(project)
            .linkedTestRun(linkedRun)
            .linkedTestCase(linkedTc)
            .linkedExecution(linkedExec)
            .environment(environment)
            .buildVersion(buildVersion)
            .module(module)
            .reportedBy(reporter)
            .build();

        return defectRepo.save(defect);
    }

    @Transactional
    public Defect updateStatus(UUID defectId, Defect.DefectStatus newStatus, String comment, String updaterEmail) {
        Defect defect = findById(defectId);

        Set<Defect.DefectStatus> allowed = VALID_TRANSITIONS.get(defect.getStatus());
        if (!allowed.contains(newStatus)) {
            throw new InvalidStatusTransitionException(
                "Cannot transition from " + defect.getStatus() + " to " + newStatus);
        }

        defect.setStatus(newStatus);
        if (newStatus == Defect.DefectStatus.FIXED || newStatus == Defect.DefectStatus.CLOSED) {
            defect.setResolvedAt(Instant.now());
        }

        if (comment != null && !comment.isBlank()) {
            User updater = userRepo.findByEmail(updaterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            DefectComment dc = new DefectComment();
            dc.setDefect(defect);
            dc.setComment(comment);
            dc.setCreatedBy(updater);
            defect.getComments().add(dc);
        }

        return defectRepo.save(defect);
    }

    @Transactional
    public Map<String, String> syncToJira(UUID defectId) {
        Defect defect = findById(defectId);

        if (defect.getJiraIssueKey() != null) {
            throw new ConflictException("Defect already synced to Jira: " + defect.getJiraIssueKey());
        }

        JiraService.JiraIssueResult result = jiraService.createIssue(defect);
        defect.setJiraIssueKey(result.issueKey());
        defect.setJiraUrl(result.issueUrl());
        defectRepo.save(defect);

        return Map.of("defectId", defect.getId().toString(),
                      "jiraIssueKey", result.issueKey(),
                      "jiraUrl", result.issueUrl());
    }

    @Transactional
    public void handleJiraWebhook(String jiraKey, String jiraStatus) {
        defectRepo.findAll().stream()
            .filter(d -> jiraKey.equals(d.getJiraIssueKey()))
            .findFirst()
            .ifPresent(d -> {
                Defect.DefectStatus mapped = mapJiraStatus(jiraStatus);
                if (mapped != null) {
                    d.setStatus(mapped);
                    defectRepo.save(d);
                    log.info("Jira webhook: defect {} status updated to {}", d.getCode(), mapped);
                }
            });
    }

    @Transactional(readOnly = true)
    public Page<Defect> findByFilters(UUID projectId, String severity, List<String> statuses,
                                      String buildVersion, int page, int size) {
        Defect.Severity sev = severity != null ? Defect.Severity.valueOf(severity.toUpperCase()) : null;
        List<Defect.DefectStatus> statusList = statuses != null
            ? statuses.stream().map(s -> Defect.DefectStatus.valueOf(s.toUpperCase())).toList()
            : null;
        return defectRepo.findByFilters(projectId, sev, statusList, buildVersion,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public Defect findById(UUID id) {
        return defectRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Defect not found: " + id));
    }

    private Defect.DefectStatus mapJiraStatus(String jiraStatus) {
        return switch (jiraStatus.toLowerCase()) {
            case "in progress", "in development" -> Defect.DefectStatus.IN_PROGRESS;
            case "done", "resolved", "fixed"      -> Defect.DefectStatus.FIXED;
            case "closed"                          -> Defect.DefectStatus.CLOSED;
            default -> null;
        };
    }
}
