package org.kimwanyi.sacco.audit;


import org.kimwanyi.sacco.enums.AuditAction;
import org.kimwanyi.sacco.enums.AuditStatus;



public class AuditServiceImpl implements AuditService {
    private final AuditRepository auditRepository;

    public AuditServiceImpl(AuditRepository auditRepository){
        this.auditRepository = auditRepository;
    }


    public void logSuccess(Long userId, AuditAction action, String entityName, Long entityId, String description){
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action.name());
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setDescription(description);
        log.setStatus(AuditStatus.SUCCESS);
        auditRepository.save(log);
    }

    public void logFailure(Long userId, AuditAction action, String description){
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action.name());
        log.setDescription(description);
        log.setStatus(AuditStatus.FAILED);
        auditRepository.save(log);
    }

}