package org.kimwanyi.sacco.util;

import org.hibernate.SessionFactory;

public class HibernateSessionManager {
    private final SessionFactory sessionFactory;

    public HibernateSessionManager(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void openSession(){
        if(!sessionFactory.getCurrentSession().isOpen()){
            sessionFactory.openSession();
        }
    }

    public void CloseSession(){
        if(sessionFactory.getCurrentSession().isOpen()){
            sessionFactory.getCurrentSession().close();
        }
    }
}
