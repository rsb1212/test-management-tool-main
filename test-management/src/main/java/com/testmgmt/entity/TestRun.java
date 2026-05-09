package com.testmgmt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "test_runs")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class TestRun extends BaseEntity {

    @Column(unique = true, nullable = false, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_plan_id", nullable = false)
    private TestPlan testPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_cycle_id")
    private TestCycle testCycle;

    @Column(length = 60)
    private String environment;

    @Column(name = "build_version", length = 40)
    private String buildVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status = RunStatus.NOT_STARTED;

    @Column(name = "total_tests")
    private int totalTests = 0;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public enum RunStatus { NOT_STARTED, IN_PROGRESS, COMPLETED, ABORTED }
}
