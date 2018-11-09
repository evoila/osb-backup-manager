package de.evoila.cf.backup.service.exception;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
public class BackupRequestException extends Throwable {

    public BackupRequestException(String s) {
        super(s);
    }

    public  BackupRequestException(String s, Throwable t) {
        super(s, t);
    }
}
