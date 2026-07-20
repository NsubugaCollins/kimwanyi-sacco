package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.SessionFactory;
import org.kimwanyi.sacco.entity.Role;
import org.kimwanyi.sacco.repository.RoleRepository;

public class RoleRepositoryImpl extends GenericRepositoryImpl<Role, Long> implements RoleRepository {
    public RoleRepositoryImpl(SessionFactory sessionFactory){
        super(Role.class, sessionFactory);
    }

    public Role findByName(String name){
        return sessionFactory.getCurrentSession().createQuery(
                "FROM Role r WHERE r.name = :name", Role.class
        ).setParameter("name", name).uniqueResult();
    }

    public boolean existsByName(String name){
        Long count = sessionFactory.getCurrentSession().createQuery(
                "SELECT COUNT(r) FROM Role r WHERE r.name= :name", Long.class
        ).setParameter("name", name).uniqueResult();

        return count > 0;
    }
}
