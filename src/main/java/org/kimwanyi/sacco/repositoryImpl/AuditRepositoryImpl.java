package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.SessionFactory;
import org.kimwanyi.sacco.audit.AuditLog;
import org.kimwanyi.sacco.repository.AuditRepository;

public class AuditRepositoryImpl extends GenericRepositoryImpl<AuditLog, Long> implements AuditRepository {
    public AuditRepositoryImpl(SessionFactory sessionFactory){
        super(AuditLog.class, sessionFactory);
    }
}
