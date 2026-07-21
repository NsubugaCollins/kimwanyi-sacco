package org.kimwanyi.sacco.audit;

import org.kimwanyi.sacco.enums.AuditAction;

public interface AuditService {
    void logSuccess(Long userId, AuditAction action, String entityName, Long entityId, String description);



    void logFailure(Long userId, AuditAction action, String description);

}
