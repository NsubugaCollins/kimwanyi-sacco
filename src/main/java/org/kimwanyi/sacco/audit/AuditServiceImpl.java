package org.kimwanyi.sacco.audit;

import org.kimwanyi.sacco.enums.AuditAction;
import org.kimwanyi.sacco.enums.AuditStatus;
import org.kimwanyi.sacco.util.TransactionManager;

public class AuditServiceImpl implements AuditService {
    private final AuditRepository auditRepository;

    public AuditServiceImpl(AuditRepository auditRepository){
        this.auditRepository = auditRepository;
    }

    @Override
    public void logSuccess(Long userId, AuditAction action, String entityName, Long entityId, String description){
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action != null ? action.name() : null);
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setDescription(description);
        log.setStatus(AuditStatus.SUCCESS);
        TransactionManager.execute(session -> auditRepository.save(session, log));
    }

    @Override
    public void logFailure(Long userId, AuditAction action, String description){
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action != null ? action.name() : null);
        log.setDescription(description);
        log.setStatus(AuditStatus.FAILED);
        TransactionManager.execute(session -> auditRepository.save(session, log));
    }
}