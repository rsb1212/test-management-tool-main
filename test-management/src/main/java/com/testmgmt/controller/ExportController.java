package com.testmgmt.controller;

import com.testmgmt.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Exports")
class ExportController {

    private final ExportService exportService;

    @GetMapping("/execution-summary/export")
    @Operation(summary = "Export execution report as PDF or Excel")
    public ResponseEntity<byte[]> exportExecutionSummary(
            @RequestParam UUID runId,
            @RequestParam(defaultValue = "pdf") String format) {

        return switch (format.toLowerCase()) {
            case "pdf" -> {
                byte[] bytes = exportService.exportExecutionPdf(runId);
                yield ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"execution-report-" + runId + ".pdf\"")
                    .body(bytes);
            }
            case "xlsx", "excel" -> {
                byte[] bytes = exportService.exportDefectsExcel(runId);
                yield ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"execution-report-" + runId + ".xlsx\"")
                    .body(bytes);
            }
            default -> ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        };
    }

    @GetMapping("/defect-summary/export")
    @Operation(summary = "Export defect report as Excel")
    public ResponseEntity<byte[]> exportDefectSummary(@RequestParam UUID projectId) {
        byte[] bytes = exportService.exportDefectsExcel(projectId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"defect-report-" + projectId + ".xlsx\"")
            .body(bytes);
    }
}
