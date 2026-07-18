package org.kimwanyi.sacco.config;

public class Test {
    public static void main(String[] args){


        HibernateUtil
                .getSessionFactory();


        System.out.println(
                "Database connected successfully"
        );


    }
}
