package de.evoila.cf.service.exception;

/**
 * Created by yremmet on 06.07.17.
 */
public class BackupCreateException extends ProcessException {
  public BackupCreateException () {
    super("Could not create Backup");
  }
}
