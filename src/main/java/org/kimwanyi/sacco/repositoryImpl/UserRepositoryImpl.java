package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.kimwanyi.sacco.entity.User;
import org.kimwanyi.sacco.repository.UserRepository;

public class UserRepositoryImpl extends GenericRepositoryImpl<User, Long> implements UserRepository {
    public  UserRepositoryImpl(SessionFactory sessionFactory){
        super(User.class, sessionFactory);
    }

    public User findByUserName(String username){
        Session session = sessionFactory.getCurrentSession();
        return session.createQuery("FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .uniqueResult();
    }

    public User findByEmail(String email){
        Session session = sessionFactory.getCurrentSession();
        return session.createQuery("FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .uniqueResult();
    }

    @Override
    public boolean existsByUserName(String username) {
        Long count = sessionFactory.getCurrentSession().createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class
        ).setParameter("username", username).uniqueResult();
        return  count > 0;
    }

    public boolean existsByEmail(String email){
        Long count = sessionFactory.getCurrentSession().createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.email=:email", Long.class
                ).setParameter("email", email).uniqueResult();



        return count > 0;

    }
}
