package com.testmgmt.service;

import com.testmgmt.entity.Execution;
import com.testmgmt.repository.DefectRepository;
import com.testmgmt.repository.ExecutionRepository;
import com.testmgmt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final ExecutionRepository executionRepo;
    private final DefectRepository defectRepo;

    public record ModuleStat(String module, int passed, int failed, int blocked, int skipped) {}

    public record ExecutionSummaryReport(
        int totalTests, int passed, int failed, int blocked, int skipped,
        String passRate, List<ModuleStat> byModule
    ) {}

    public record DefectSummaryReport(
        int totalDefects, int open, int inProgress, int fixed,
        Map<String, Long> bySeverity, List<Map<String, Object>> byModule
    ) {}

    public record TrendEntry(String period, long raised, long closed) {}
    public record TrendReport(String groupBy, List<TrendEntry> data) {}

    // ── Execution Summary ─────────────────────────────────────
    @Transactional(readOnly = true)
    @Cacheable(value = "executionSummary",
               key = "#projectId + '_' + #sprintId + '_' + #environment + '_' + #dateFrom + '_' + #dateTo")
    public ExecutionSummaryReport executionSummary(UUID projectId, String sprintId,
                                                    String environment, Instant dateFrom, Instant dateTo) {
        List<Object[]> raw = executionRepo.executionSummary(projectId, sprintId, environment, dateFrom, dateTo);

        int passed = 0, failed = 0, blocked = 0, skipped = 0;
        for (Object[] row : raw) {
            Execution.ExecResult result = (Execution.ExecResult) row[0];
            long count = ((Number) row[1]).longValue();
            switch (result) {
                case PASSED  -> passed  += count;
                case FAILED  -> failed  += count;
                case BLOCKED -> blocked += count;
                case SKIPPED -> skipped += count;
            }
        }
        int total = passed + failed + blocked + skipped;
        String passRate = total > 0
            ? String.format("%.1f%%", (passed * 100.0 / total))
            : "0.0%";

        List<ModuleStat> byModule = buildModuleStats(projectId, dateFrom, dateTo);

        return new ExecutionSummaryReport(total, passed, failed, blocked, skipped, passRate, byModule);
    }

    // ── Defect Summary ────────────────────────────────────────
    @Transactional(readOnly = true)
    @Cacheable(value = "defectSummary", key = "#projectId + '_' + #buildVersion")
    public DefectSummaryReport defectSummary(UUID projectId, String buildVersion) {
        List<Object[]> bySev  = defectRepo.countBySeverity(projectId);
        List<Object[]> byMod  = defectRepo.countByModule(projectId);

        Map<String, Long> severityMap = new LinkedHashMap<>();
        for (Object[] row : bySev) {
            severityMap.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        List<Map<String, Object>> moduleList = new ArrayList<>();
        for (Object[] row : byMod) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("module",  row[0]);
            entry.put("defects", ((Number) row[1]).longValue());
            moduleList.add(entry);
        }

        long totalOpen       = severityMap.values().stream().mapToLong(Long::longValue).sum();
        // Simplified — real impl would join with status
        return new DefectSummaryReport((int) totalOpen, (int) totalOpen, 0, 0, severityMap, moduleList);
    }

    // ── Trend Report ─────────────────────────────────────────
    @Transactional(readOnly = true)
    @Cacheable(value = "trendReports", key = "#projectId + '_' + #metricType + '_' + #groupBy")
    public TrendReport trends(UUID projectId, String metricType, String groupBy,
                               Instant dateFrom, Instant dateTo) {

        if (!Set.of("day", "week", "month").contains(groupBy.toLowerCase())) {
            throw new IllegalArgumentException("Invalid groupBy: " + groupBy + ". Use day, week, or month.");
        }

        List<Object[]> raw = defectRepo.defectTrends(projectId, groupBy, dateFrom, dateTo);
        List<TrendEntry> entries = new ArrayList<>();
        for (Object[] row : raw) {
            String period = row[0].toString();
            long raised   = ((Number) row[1]).longValue();
            long closed   = ((Number) row[2]).longValue();
            entries.add(new TrendEntry(period, raised, closed));
        }

        return new TrendReport(groupBy, entries);
    }

    // ── Private helpers ───────────────────────────────────────
    private List<ModuleStat> buildModuleStats(UUID projectId, Instant from, Instant to) {
        List<Object[]> raw = executionRepo.executionByModule(projectId, from, to);
        Map<String, int[]> map = new LinkedHashMap<>();
        for (Object[] row : raw) {
            String module = row[0] != null ? row[0].toString() : "Unassigned";
            Execution.ExecResult result = (Execution.ExecResult) row[1];
            long count = ((Number) row[2]).longValue();
            map.computeIfAbsent(module, k -> new int[4]);
            switch (result) {
                case PASSED  -> map.get(module)[0] += count;
                case FAILED  -> map.get(module)[1] += count;
                case BLOCKED -> map.get(module)[2] += count;
                case SKIPPED -> map.get(module)[3] += count;
            }
        }
        return map.entrySet().stream()
            .map(e -> new ModuleStat(e.getKey(), e.getValue()[0], e.getValue()[1],
                                     e.getValue()[2], e.getValue()[3]))
            .toList();
    }
}
