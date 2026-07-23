package org.kimwanyi.sacco.repository;

import org.hibernate.Session;
import org.kimwanyi.sacco.audit.AuditLog;

import java.util.List;

public interface AuditRepository extends GenericRepository<AuditLog, Long> {

    List<AuditLog> findAllOrderedByDateDesc(Session session);
}