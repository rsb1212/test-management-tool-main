package com.testmgmt.dto.request;

import com.testmgmt.entity.TestCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

// ─── TestCase DTOs ────────────────────────────────────────────
public class TestCaseDTOs {

    public record StepRequest(
        @Min(1) int stepNumber,
        @NotBlank String action,
        @NotBlank String expectedResult
    ) {}

    public record CreateRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        String preconditions,
        @NotNull UUID projectId,
        UUID moduleId,
        TestCase.Priority priority,
        @Valid @NotEmpty List<StepRequest> steps
    ) {}

    public record UpdateRequest(
        @Size(max = 255) String title,
        String description,
        String preconditions,
        UUID moduleId,
        TestCase.Priority priority,
        TestCase.TestStatus status,
        @Valid List<StepRequest> steps
    ) {}
}
