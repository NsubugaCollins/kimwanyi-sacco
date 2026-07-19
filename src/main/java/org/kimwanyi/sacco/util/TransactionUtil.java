package org.kimwanyi.sacco.util;

public class TransactionUtil {
    public static void execute(HibernateTransactionManager manager, TransactionExecutor executor){
        try{
            manager.begin();
            executor.execute();
            manager.commit();
        }catch(Exception e){
            manager.rollBack();
            throw e;
        }
    }
}
