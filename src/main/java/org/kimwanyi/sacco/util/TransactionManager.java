package org.kimwanyi.sacco.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.kimwanyi.sacco.config.HibernateUtil;

public class TransactionManager {

    public interface TransactionCallback<T>{
        T execute(Session session);
    }

    public static <T> T execute(
            TransactionCallback<T> callback
    ){
        Transaction transaction = null;
        Session session = null;
        try{
            SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
            if (sessionFactory != null) {
                session = sessionFactory.openSession();
                transaction = session.beginTransaction();
            }

            T result = callback.execute(session);

            if (transaction != null && transaction.isActive()) {
                transaction.commit();
            }
            if (session != null && session.isOpen()) {
                session.close();
            }

            return result;
        }
        catch(Exception e){
            if(transaction != null && transaction.isActive()){
                try {
                    transaction.rollback();
                } catch (Exception ignored) {}
            }
            if(session != null && session.isOpen()){
                try {
                    session.close();
                } catch (Exception ignored) {}
            }
            throw e;
        }
    }
}
