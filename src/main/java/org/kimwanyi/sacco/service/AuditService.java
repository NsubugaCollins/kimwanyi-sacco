package org.kimwanyi.sacco.service;

import org.kimwanyi.sacco.enums.AuditAction;

public interface AuditService {
    void log(AuditAction action, String entityName, Long entityId, String description);
}
