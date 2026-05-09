package com.testmgmt.dto.request;

import com.testmgmt.entity.Defect;
import com.testmgmt.entity.TestCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// ─── TestPlan DTOs ────────────────────────────────────────────
class TestPlanDTOs {

    public record CreateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull UUID projectId,
        String description,
        String sprintId,
        LocalDate startDate,
        LocalDate endDate,
        List<UUID> testCaseIds
    ) {}
}

// ─── TestRun DTOs ─────────────────────────────────────────────
class TestRunDTOs {

    public record CreateRequest(
        @NotNull UUID testPlanId,
        UUID cycleId,
        String environment,
        String buildVersion,
        UUID assignedTo
    ) {}

    public record ExecuteRequest(
        @NotNull UUID testCaseId,
        @NotNull String status,
        String actualResult,
        String comment,
        Integer durationSecs
    ) {}
}

// ─── Defect DTOs ──────────────────────────────────────────────
class DefectDTOs {

    public record CreateRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull Defect.Severity severity,
        @NotNull Defect.DefectPriority priority,
        @NotNull UUID projectId,
        UUID linkedTestRunId,
        UUID linkedTestCaseId,
        UUID linkedExecutionId,
        String environment,
        String buildVersion,
        String module
    ) {}

    public record UpdateStatusRequest(
        @NotNull Defect.DefectStatus status,
        String comment
    ) {}
}

// ─── Project DTOs ─────────────────────────────────────────────
class ProjectDTOs {

    public record CreateRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 120) String name,
        String description
    ) {}
}
