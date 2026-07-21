package org.kimwanyi.sacco.audit;


import jakarta.persistence.*;
import lombok.Data;
import org.kimwanyi.sacco.entity.BaseEntity;
import org.kimwanyi.sacco.enums.AuditStatus;

@Data
@Entity
@Table(name = "audit_logs",
        indexes = {

                @Index(
                        name = "idx_audit_user",
                        columnList = "user_id"
                ),

                @Index(
                        name = "idx_audit_action",
                        columnList = "action"
                ),

                @Index(
                        name = "idx_audit_date",
                        columnList = "created_at"
                )

        }
)
public class AuditLog extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false,length = 100)
    private String action;

    @Column(length = 100)
    private String entityName;

    @Column
    private Long entityId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 45)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private AuditStatus status;

    public AuditLog(){

    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public AuditStatus getStatus() {
        return status;
    }

    public void setStatus(AuditStatus status) {
        this.status = status;
    }
}