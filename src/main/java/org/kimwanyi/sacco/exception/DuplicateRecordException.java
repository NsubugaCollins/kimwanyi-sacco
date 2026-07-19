package org.kimwanyi.sacco.exception;


public class DuplicateRecordException extends BusinessException {
    public DuplicateRecordException(String message){
        super(message);
    }
}