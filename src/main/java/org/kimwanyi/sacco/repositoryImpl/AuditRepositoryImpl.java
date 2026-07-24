package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.kimwanyi.sacco.audit.AuditLog;
import org.kimwanyi.sacco.repository.AuditRepository;

import java.util.List;

public class AuditRepositoryImpl extends GenericRepositoryImpl<AuditLog, Long> implements AuditRepository {

    public AuditRepositoryImpl() {
        super(AuditLog.class);
    }

    @Override
    public List<AuditLog> findAllOrderedByDateDesc(Session session) {
        return session.createQuery(
                "FROM AuditLog a ORDER BY a.createdAt DESC", AuditLog.class)
                .setMaxResults(500)
                .getResultList();
    }
}