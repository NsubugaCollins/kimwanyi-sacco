package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.Role;
import org.kimwanyi.sacco.repository.RoleRepository;

public class RoleRepositoryImpl extends GenericRepositoryImpl<Role, Long> implements RoleRepository {

    public RoleRepositoryImpl() {
        super(Role.class);
    }

    @Override
    public Role findByName(Session session, String name) {
        return session.createQuery(
                "FROM Role r WHERE r.name = :name", Role.class
        ).setParameter("name", name).uniqueResult();
    }

    @Override
    public boolean existsByName(Session session, String name) {
        Long count = session.createQuery(
                "SELECT COUNT(r) FROM Role r WHERE r.name = :name", Long.class
        ).setParameter("name", name).uniqueResult();

        return count != null && count > 0;
    }
}
