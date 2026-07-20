package org.kimwanyi.sacco.audit;

import jakarta.persistence.*;
import org.kimwanyi.sacco.entity.BaseEntity;
import org.kimwanyi.sacco.enums.AuditAction;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(nullable = false)
    private String entityName;

    private Long entityId;

    @Column(nullable = false)
    private String description;

    private String username;

    private String ipAddress;


}
