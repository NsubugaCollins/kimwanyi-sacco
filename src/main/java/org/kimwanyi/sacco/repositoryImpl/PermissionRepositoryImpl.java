package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.SessionFactory;
import org.kimwanyi.sacco.entity.Permission;
import org.kimwanyi.sacco.repository.PermissionRepository;

public class PermissionRepositoryImpl extends GenericRepositoryImpl<Permission, Long> implements PermissionRepository {
    public PermissionRepositoryImpl(SessionFactory sessionFactory){
        super(Permission.class, sessionFactory);
    }

    public Permission findByName(String name){
        return sessionFactory.getCurrentSession().createQuery(
                "FROM Permission p WHERE p.name = :name", Permission.class
        ).setParameter("name", name).uniqueResult();
    }
}
