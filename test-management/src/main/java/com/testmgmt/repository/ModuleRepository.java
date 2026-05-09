package com.testmgmt.repository;

import com.testmgmt.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

// ─── ModuleRepository ────────────────────────────────────────
@Repository
public interface ModuleRepository extends JpaRepository<com.testmgmt.entity.Module, UUID> {
    List<Module> findByProject_Id(UUID projectId);
}
