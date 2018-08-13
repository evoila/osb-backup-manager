package de.evoila.cf.controller.exception;

public class BackupException extends Exception {

    public BackupException(String msg){
        super(msg);
    }

    public BackupException(String msg, Exception ex) {
        super(msg, ex);
    }
}
