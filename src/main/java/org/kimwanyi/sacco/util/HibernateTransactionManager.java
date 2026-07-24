package org.kimwanyi.sacco.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public class HibernateTransactionManager {
    private final SessionFactory sessionFactory;

    private static final ThreadLocal<Transaction> transaction = new ThreadLocal<>();

    public HibernateTransactionManager(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void begin(){
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        transaction.set(tx);
    }

    public void commit(){
        Transaction tx = transaction.get();
        if(tx != null && tx.isActive()){
            tx.commit();
        }

        transaction.remove();
    }

    public  void rollBack(){
        Transaction tx = transaction.get();
        if(tx != null && tx.isActive()){
            tx.rollback();
        }
        transaction.remove();
    }
}
