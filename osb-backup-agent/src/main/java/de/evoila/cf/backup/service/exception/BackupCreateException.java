package de.evoila.cf.backup.service.exception;

/**
 * Created by yremmet on 06.07.17.
 */
public class BackupCreateException extends Throwable {
  public BackupCreateException () {
    super("Could not create Backup");
  }
}
