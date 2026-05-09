package com.testmgmt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "defects")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Defect extends BaseEntity {

    @Column(unique = true, nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DefectPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DefectStatus status = DefectStatus.NEW;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_test_run_id")
    private TestRun linkedTestRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_test_case_id")
    private TestCase linkedTestCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_execution_id")
    private Execution linkedExecution;

    @Column(length = 60)
    private String environment;

    @Column(name = "build_version", length = 40)
    private String buildVersion;

    @Column(length = 100)
    private String module;

    @Column(name = "jira_issue_key", length = 50)
    private String jiraIssueKey;

    @Column(name = "jira_url", columnDefinition = "TEXT")
    private String jiraUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @OneToMany(mappedBy = "defect", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DefectComment> comments = new ArrayList<>();

    public enum Severity       { CRITICAL, HIGH, MEDIUM, LOW }
    public enum DefectPriority { P1, P2, P3, P4 }
    public enum DefectStatus   { NEW, OPEN, IN_PROGRESS, FIXED, RETEST, CLOSED, REJECTED }
}
