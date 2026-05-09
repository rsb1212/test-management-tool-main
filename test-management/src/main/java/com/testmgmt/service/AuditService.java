package com.testmgmt.service;

import com.testmgmt.entity.AuditLog;
import com.testmgmt.entity.User;
import com.testmgmt.repository.AuditLogRepository;
import com.testmgmt.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepo;
    private final UserRepository userRepo;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String entityType, UUID entityId, String action,
                    Map<String, Object> oldValue, Map<String, Object> newValue,
                    String performerEmail) {
        try {
            User performer = performerEmail != null
                ? userRepo.findByEmail(performerEmail).orElse(null)
                : null;

            AuditLog entry = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .performedBy(performer)
                .performedAt(Instant.now())
                .build();

            auditLogRepo.save(entry);
        } catch (Exception e) {
            log.warn("Audit log failed for {}/{}: {}", entityType, entityId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getHistory(String entityType, UUID entityId) {
        return auditLogRepo.findByEntityTypeAndEntityIdOrderByPerformedAtDesc(entityType, entityId);
    }
}
