package org.kimwanyi.sacco.exception;

public class InsufficientBalanceException extends BusinessException{
    public InsufficientBalanceException(String message){
        super(message);
    }
}
