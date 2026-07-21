package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.Permission;
import org.kimwanyi.sacco.repository.PermissionRepository;

public class PermissionRepositoryImpl extends GenericRepositoryImpl<Permission, Long> implements PermissionRepository {

    public PermissionRepositoryImpl() {
        super(Permission.class);
    }

    @Override
    public Permission findByName(Session session, String name) {
        return session.createQuery(
                "FROM Permission p WHERE p.name = :name", Permission.class
        ).setParameter("name", name).uniqueResult();
    }
}
