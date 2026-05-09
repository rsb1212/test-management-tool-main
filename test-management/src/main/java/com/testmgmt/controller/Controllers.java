package com.testmgmt.controller;

import com.testmgmt.dto.request.AuthDTOs.*;
import com.testmgmt.entity.*;
import com.testmgmt.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

// ══════════════════════════════════════════════════════════════
//  Auth Controller
// ══════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT token")
    public ResponseEntity<AuthService.AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        User user = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "userId",   user.getId(),
            "username", user.getUsername(),
            "email",    user.getEmail(),
            "role",     user.getRole()
        ));
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        authService.changePassword(principal.getUsername(), req);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}

// ══════════════════════════════════════════════════════════════
//  TestCase Controller
// ══════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/v1/testcases")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Test Cases")
class TestCaseController {

    private final TestCaseService testCaseService;

    @PostMapping
    @Operation(summary = "Create a test case")
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody com.testmgmt.dto.request.TestCaseDTOs.CreateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        TestCase tc = testCaseService.create(req, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "testCaseId", tc.getId(),
            "code",       tc.getCode(),
            "status",     tc.getStatus(),
            "createdAt",  tc.getCreatedAt()
        ));
    }

    @GetMapping
    @Operation(summary = "List test cases with filters")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam UUID projectId,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID moduleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TestCase> result = testCaseService.findByFilters(projectId, priority, status, moduleId, page, size);
        return ResponseEntity.ok(Map.of(
            "total",     result.getTotalElements(),
            "page",      result.getNumber(),
            "totalPages",result.getTotalPages(),
            "testCases", result.getContent()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get test case by ID")
    public ResponseEntity<TestCase> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(testCaseService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a test case")
    public ResponseEntity<TestCase> update(
            @PathVariable UUID id,
            @Valid @RequestBody com.testmgmt.dto.request.TestCaseDTOs.UpdateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(testCaseService.update(id, req, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a test case")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        testCaseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

// ══════════════════════════════════════════════════════════════
//  TestRun Controller
// ══════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/v1/testruns")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Test Runs & Execution")
class TestRunController {

    private final TestRunService testRunService;

    @PostMapping
    @Operation(summary = "Create a test run")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {
        UUID planId     = UUID.fromString((String) body.get("testPlanId"));
        UUID cycleId    = body.get("cycleId") != null ? UUID.fromString((String) body.get("cycleId")) : null;
        UUID assignedTo = body.get("assignedTo") != null ? UUID.fromString((String) body.get("assignedTo")) : null;

        TestRun run = testRunService.createRun(planId, cycleId,
                (String) body.get("environment"), (String) body.get("buildVersion"),
                assignedTo, principal.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "runId",      run.getId(),
            "code",       run.getCode(),
            "status",     run.getStatus(),
            "totalTests", run.getTotalTests(),
            "startedAt",  run.getStartedAt()
        ));
    }

    @PutMapping("/{runId}/execute")
    @Operation(summary = "Execute a test case within a run")
    public ResponseEntity<Map<String, Object>> execute(
            @PathVariable UUID runId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {
        UUID tcId       = UUID.fromString((String) body.get("testCaseId"));
        String status   = (String) body.get("status");
        String actual   = (String) body.get("actualResult");
        String comment  = (String) body.get("comment");
        Integer dur     = body.get("durationSecs") != null ? (Integer) body.get("durationSecs") : null;

        Execution exec  = testRunService.executeTestCase(runId, tcId, status, actual, comment, dur,
                                                          principal.getUsername());
        return ResponseEntity.ok(Map.of(
            "executionId",  exec.getId(),
            "code",         exec.getCode(),
            "testCaseId",   exec.getTestCase().getId(),
            "status",       exec.getResult(),
            "executedAt",   exec.getExecutedAt(),
            "raiseDefect",  exec.getResult() == Execution.ExecResult.FAILED
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get test run by ID")
    public ResponseEntity<TestRun> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(testRunService.findById(id));
    }
}

// ══════════════════════════════════════════════════════════════
//  Defect Controller
// ══════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/v1/defects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Defects")
class DefectController {

    private final DefectService defectService;

    @PostMapping
    @Operation(summary = "Report a defect")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {
        UUID projectId  = UUID.fromString((String) body.get("projectId"));
        Defect defect   = defectService.create(
            projectId,
            (String) body.get("title"),
            (String) body.get("description"),
            Defect.Severity.valueOf((String) body.get("severity")),
            Defect.DefectPriority.valueOf((String) body.get("priority")),
            body.get("linkedTestRun")      != null ? UUID.fromString((String) body.get("linkedTestRun"))      : null,
            body.get("linkedTestCase")     != null ? UUID.fromString((String) body.get("linkedTestCase"))     : null,
            body.get("linkedExecutionId")  != null ? UUID.fromString((String) body.get("linkedExecutionId"))  : null,
            (String) body.get("environment"),
            (String) body.get("buildVersion"),
            (String) body.get("module"),
            principal.getUsername()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "defectId",  defect.getId(),
            "code",      defect.getCode(),
            "status",    defect.getStatus(),
            "createdAt", defect.getCreatedAt()
        ));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update defect status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails principal) {
        Defect.DefectStatus newStatus = Defect.DefectStatus.valueOf(body.get("status"));
        Defect updated = defectService.updateStatus(id, newStatus, body.get("comment"), principal.getUsername());
        return ResponseEntity.ok(Map.of(
            "defectId",   updated.getId(),
            "code",       updated.getCode(),
            "status",     updated.getStatus(),
            "resolvedAt", updated.getResolvedAt() != null ? updated.getResolvedAt() : "N/A"
        ));
    }

    @PostMapping("/{id}/jira-sync")
    @Operation(summary = "Sync defect to Jira")
    public ResponseEntity<Map<String, String>> jiraSync(@PathVariable UUID id) {
        return ResponseEntity.ok(defectService.syncToJira(id));
    }

    @PostMapping("/jira-webhook")
    @Operation(summary = "Receive Jira webhook for status sync")
    public ResponseEntity<Void> jiraWebhook(@RequestBody Map<String, Object> payload) {
        String issueKey = (String) ((Map<?, ?>) payload.get("issue")).get("key");
        String status   = (String) ((Map<?, ?>) ((Map<?, ?>) payload.get("issue")).get("fields")).get("status");
        defectService.handleJiraWebhook(issueKey, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(summary = "List defects with filters")
    public ResponseEntity<Page<Defect>> list(
            @RequestParam UUID projectId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String buildVersion,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(defectService.findByFilters(projectId, severity, status, buildVersion, page, size));
    }
}

// ══════════════════════════════════════════════════════════════
//  Reporting Controller
// ══════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reports")
class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/execution-summary")
    @Operation(summary = "Get execution summary report")
    public ResponseEntity<ReportingService.ExecutionSummaryReport> executionSummary(
            @RequestParam UUID projectId,
            @RequestParam(required = false) String sprintId,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo) {

        Instant from = dateFrom != null ? dateFrom : Instant.now().minusSeconds(30L * 86400);
        Instant to   = dateTo   != null ? dateTo   : Instant.now();
        return ResponseEntity.ok(reportingService.executionSummary(projectId, sprintId, environment, from, to));
    }

    @GetMapping("/defect-summary")
    @Operation(summary = "Get defect summary report")
    public ResponseEntity<ReportingService.DefectSummaryReport> defectSummary(
            @RequestParam UUID projectId,
            @RequestParam(required = false) String buildVersion) {
        return ResponseEntity.ok(reportingService.defectSummary(projectId, buildVersion));
    }

    @GetMapping("/trends")
    @Operation(summary = "Get trend data (defects or executions over time)")
    public ResponseEntity<ReportingService.TrendReport> trends(
            @RequestParam UUID projectId,
            @RequestParam(defaultValue = "defects") String metricType,
            @RequestParam(defaultValue = "week") String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo) {

        Instant from = dateFrom != null ? dateFrom : Instant.now().minusSeconds(90L * 86400);
        Instant to   = dateTo   != null ? dateTo   : Instant.now();
        return ResponseEntity.ok(reportingService.trends(projectId, metricType, groupBy, from, to));
    }
}
