package org.kimwanyi.sacco.serviceImpl;

import org.kimwanyi.sacco.audit.AuditLog;
import org.kimwanyi.sacco.audit.AuditService;
import org.kimwanyi.sacco.enums.AuditAction;
import org.kimwanyi.sacco.enums.AuditStatus;
import org.kimwanyi.sacco.repository.AuditRepository;
import org.kimwanyi.sacco.util.TransactionManager;

public class AuditServiceImpl implements AuditService, org.kimwanyi.sacco.service.AuditService {

    private final AuditRepository repository;

    public AuditServiceImpl(AuditRepository repository) {
        this.repository = repository;
    }

    @Override
    public void log(AuditAction action, String entityName, Long entityId, String description) {
        logSuccess(null, action, entityName, entityId, description);
    }

    @Override
    public void logSuccess(Long userId, AuditAction action, String entityName, Long entityId, String description) {
        AuditLog audit = new AuditLog();
        audit.setUserId(userId);
        audit.setAction(action != null ? action.name() : null);
        audit.setEntityName(entityName);
        audit.setEntityId(entityId);
        audit.setDescription(description);
        audit.setStatus(AuditStatus.SUCCESS);

        TransactionManager.execute(session -> repository.save(session, audit));
    }

    @Override
    public void logFailure(Long userId, AuditAction action, String description) {
        AuditLog audit = new AuditLog();
        audit.setUserId(userId);
        audit.setAction(action != null ? action.name() : null);
        audit.setDescription(description);
        audit.setStatus(AuditStatus.FAILED);

        TransactionManager.execute(session -> repository.save(session, audit));
    }
}
