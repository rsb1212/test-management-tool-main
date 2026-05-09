package com.testmgmt.controller;

import com.testmgmt.entity.*;
import com.testmgmt.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

// ══════════════════════════════════════════════════════════════
//  User Controller
// ══════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users")
class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<Map<String, Object>> getProfile(
            @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getProfile(principal.getUsername());
        return ResponseEntity.ok(Map.of(
            "userId",   user.getId(),
            "username", user.getUsername(),
            "email",    user.getEmail(),
            "fullName", user.getFullName() != null ? user.getFullName() : "",
            "role",     user.getRole(),
            "team",     user.getTeam() != null ? user.getTeam() : ""
        ));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails principal) {
        User user = userService.updateProfile(
            principal.getUsername(), body.get("fullName"), body.get("team"));
        return ResponseEntity.ok(Map.of(
            "userId",   user.getId(),
            "fullName", user.getFullName() != null ? user.getFullName() : "",
            "team",     user.getTeam() != null ? user.getTeam() : "",
            "updatedAt",user.getUpdatedAt()
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "List all active users (Admin/Manager only)")
    public ResponseEntity<List<User>> listAll() {
        return ResponseEntity.ok(userService.findAllActive());
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user role (Admin only)")
    public ResponseEntity<Map<String, Object>> updateRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        User.UserRole newRole = User.UserRole.valueOf(body.get("role").toUpperCase());
        User user = userService.updateRole(id, newRole);
        return ResponseEntity.ok(Map.of(
            "userId", user.getId(),
            "role",   user.getRole(),
            "message","Role updated successfully"
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a user (Admin only)")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}

// ══════════════════════════════════════════════════════════════
//  Project Controller
// ══════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Projects")
class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Create a project")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails principal) {
        Project p = projectService.create(
            body.get("code"), body.get("name"), body.get("description"),
            principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "projectId", p.getId(),
            "code",      p.getCode(),
            "name",      p.getName()
        ));
    }

    @GetMapping
    @Operation(summary = "List all active projects")
    public ResponseEntity<List<Project>> list() {
        return ResponseEntity.ok(projectService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<Project> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Update a project")
    public ResponseEntity<Project> update(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(projectService.update(id, body.get("name"), body.get("description")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a project (Admin only)")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        projectService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}

// ══════════════════════════════════════════════════════════════
//  TestPlan Controller
// ══════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/v1/testplans")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Test Plans")
class TestPlanController {

    private final TestPlanService testPlanService;

    @PostMapping
    @Operation(summary = "Create a test plan")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {

        UUID projectId  = UUID.fromString((String) body.get("projectId"));
        LocalDate start = body.get("startDate") != null ? LocalDate.parse((String) body.get("startDate")) : null;
        LocalDate end   = body.get("endDate")   != null ? LocalDate.parse((String) body.get("endDate"))   : null;

        @SuppressWarnings("unchecked")
        List<String> tcIds = (List<String>) body.getOrDefault("testCaseIds", List.of());
        List<UUID> testCaseIds = tcIds.stream().map(UUID::fromString).toList();

        TestPlan plan = testPlanService.create(
            (String) body.get("name"), projectId, (String) body.get("description"),
            (String) body.get("sprintId"), start, end, testCaseIds, principal.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "planId",    plan.getId(),
            "code",      plan.getCode(),
            "name",      plan.getName(),
            "createdAt", plan.getCreatedAt()
        ));
    }

    @GetMapping
    @Operation(summary = "List test plans for a project")
    public ResponseEntity<Page<TestPlan>> list(
            @RequestParam UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(testPlanService.findByProject(projectId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get test plan by ID")
    public ResponseEntity<TestPlan> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(testPlanService.findById(id));
    }

    @PostMapping("/{id}/testcases")
    @Operation(summary = "Add test cases to a plan")
    public ResponseEntity<Map<String, Object>> addTestCases(
            @PathVariable UUID id,
            @RequestBody Map<String, List<String>> body) {
        List<UUID> ids = body.get("testCaseIds").stream().map(UUID::fromString).toList();
        TestPlan plan  = testPlanService.addTestCases(id, ids);
        return ResponseEntity.ok(Map.of(
            "planId",        plan.getId(),
            "totalTestCases",plan.getTestCases().size()
        ));
    }

    @PostMapping("/{id}/cycles")
    @Operation(summary = "Add a test cycle to a plan")
    public ResponseEntity<Map<String, Object>> addCycle(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        TestCycle cycle = testPlanService.addCycle(id, body.get("name"), body.get("environment"));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "cycleId",     cycle.getId(),
            "code",        cycle.getCode(),
            "name",        cycle.getName(),
            "environment", cycle.getEnvironment() != null ? cycle.getEnvironment() : ""
        ));
    }

    @GetMapping("/{id}/cycles")
    @Operation(summary = "Get all cycles for a test plan")
    public ResponseEntity<List<TestCycle>> getCycles(@PathVariable UUID id) {
        return ResponseEntity.ok(testPlanService.getCycles(id));
    }
}
