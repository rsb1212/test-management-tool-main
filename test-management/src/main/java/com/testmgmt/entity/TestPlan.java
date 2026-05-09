package com.testmgmt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "test_plans")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class TestPlan extends BaseEntity {

    @Column(unique = true, nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sprint_id", length = 50)
    private String sprintId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "test_plan_cases",
        joinColumns = @JoinColumn(name = "test_plan_id"),
        inverseJoinColumns = @JoinColumn(name = "test_case_id")
    )
    @Builder.Default
    private Set<TestCase> testCases = new HashSet<>();
}
