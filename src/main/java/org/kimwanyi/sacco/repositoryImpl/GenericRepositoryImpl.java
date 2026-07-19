package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.kimwanyi.sacco.repository.GenericRepository;

import java.util.List;
import java.util.Optional;

public abstract class GenericRepositoryImpl<T, ID> implements GenericRepository<T, ID> {
    private final Class<T> entityClass;
    protected SessionFactory sessionFactory;

    public GenericRepositoryImpl(Class<T> entityClass, SessionFactory sessionFactory) {
        this.entityClass = entityClass;
        this.sessionFactory = sessionFactory;
    }

    protected Session getSession(){
        return sessionFactory.getCurrentSession();
    }

    public T save(T entity){
        getSession().persist(entity);
        return entity;
    }

    public T update(T entity){
        return getSession().merge(entity);
    }

    public Optional<T> findById(ID id){
        return Optional.ofNullable(getSession().find(entityClass, id));
    }

    public List<T> findAll(){
        String sql = "FROM" + entityClass.getSimpleName();
        return getSession().createQuery(sql, entityClass).getResultList();
    }

    public void delete(T entity){
        getSession().remove(entity);
    }

    public long count(){
        String sql = "SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e";
        return getSession().createQuery(sql, Long.class).getSingleResult();
    }

    public boolean existsById(ID id){
        return  findById(id).isPresent();
    }
}
