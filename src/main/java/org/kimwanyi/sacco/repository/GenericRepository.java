package org.kimwanyi.sacco.repository;

import org.hibernate.Session;

import java.util.List;
import java.util.Optional;

public interface GenericRepository<T, ID> {

    T save(
            Session session,
            T entity
    );

    T update(
            Session session,
            T entity
    );

    Optional<T> findById(
            Session session,
            ID id
    );

    List<T> findAll(
            Session session
    );

    void delete(
            Session session,
            T entity
    );

    long count(
            Session session
    );

    boolean existsById(
            Session session,
            ID id
    );
}
