package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.kimwanyi.sacco.repository.GenericRepository;

import java.util.List;
import java.util.Optional;

public abstract class GenericRepositoryImpl<T, ID> implements GenericRepository<T, ID> {

    protected final Class<T> entityClass;

    public GenericRepositoryImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public T save(Session session, T entity) {
        session.persist(entity);
        return entity;
    }

    @Override
    public T update(Session session, T entity) {
        return session.merge(entity);
    }

    @Override
    public Optional<T> findById(Session session, ID id) {
        return Optional.ofNullable(session.find(entityClass, id));
    }

    @Override
    public List<T> findAll(Session session) {
        String sql = "FROM " + entityClass.getSimpleName();
        return session.createQuery(sql, entityClass).getResultList();
    }

    @Override
    public void delete(Session session, T entity) {
        session.remove(entity);
    }

    @Override
    public long count(Session session) {
        String sql = "SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e";
        return session.createQuery(sql, Long.class).getSingleResult();
    }

    @Override
    public boolean existsById(Session session, ID id) {
        return findById(session, id).isPresent();
    }
}
