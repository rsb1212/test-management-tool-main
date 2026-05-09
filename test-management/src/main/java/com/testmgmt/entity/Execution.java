package com.testmgmt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "executions")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Execution extends BaseEntity {

    @Column(unique = true, nullable = false, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_run_id", nullable = false)
    private TestRun testRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecResult result = ExecResult.IN_PROGRESS;

    @Column(name = "actual_result", columnDefinition = "TEXT")
    private String actualResult;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_by")
    private User executedBy;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "duration_secs")
    private Integer durationSecs;

    public enum ExecResult { PASSED, FAILED, BLOCKED, SKIPPED, IN_PROGRESS }
}
