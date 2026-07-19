package org.kimwanyi.sacco.repository;

import java.util.List;
import java.util.Optional;

public interface GenericRepository<T, ID> {
    T save(T entity);
    T update(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void delete(T entity);
    long count();
    boolean existsById(ID id);
}
