package com.testmgmt.service;

import com.testmgmt.entity.*;
import com.testmgmt.exception.GlobalExceptionHandler.*;
import com.testmgmt.repository.ProjectRepository;
import com.testmgmt.repository.*;
import com.testmgmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// ─── ProjectService ───────────────────────────────────────────
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;

    @Transactional
    public Project create(String code, String name, String description, String ownerEmail) {
        if (projectRepo.findByCode(code).isPresent()) {
            throw new ConflictException("Project code already exists: " + code);
        }
        User owner = userRepo.findByEmail(ownerEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Project project = Project.builder()
            .code(code.toUpperCase())
            .name(name)
            .description(description)
            .owner(owner)
            .active(true)
            .build();

        return projectRepo.save(project);
    }

    @Transactional(readOnly = true)
    public List<Project> findAll() {
        return projectRepo.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Project findById(UUID id) {
        return projectRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }

    @Transactional
    public Project update(UUID id, String name, String description) {
        Project project = findById(id);
        if (name        != null) project.setName(name);
        if (description != null) project.setDescription(description);
        return projectRepo.save(project);
    }

    @Transactional
    public void deactivate(UUID id) {
        Project project = findById(id);
        project.setActive(false);
        projectRepo.save(project);
    }
}
