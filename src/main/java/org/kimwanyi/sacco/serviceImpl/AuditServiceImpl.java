package org.kimwanyi.sacco.serviceImpl;

import org.kimwanyi.sacco.audit.AuditLog;
import org.kimwanyi.sacco.enums.AuditAction;
import org.kimwanyi.sacco.repository.AuditRepository;
import org.kimwanyi.sacco.service.AuditService;
import org.kimwanyi.sacco.util.TransactionManager;

public class AuditServiceImpl implements AuditService {
    private final AuditRepository repository;

    public AuditServiceImpl(AuditRepository repository) {
        this.repository = repository;
    }

    @Override
    public void log(AuditAction action, String entityName, Long entityId, String description){
        AuditLog audit = new AuditLog();
        audit.setAction(action != null ? action.name() : null);
        audit.setEntityName(entityName);
        audit.setEntityId(entityId);
        audit.setDescription(description);

        TransactionManager.execute(session -> repository.save(session, audit));
    }
}
