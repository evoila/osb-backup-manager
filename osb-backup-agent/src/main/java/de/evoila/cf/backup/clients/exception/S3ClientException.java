/**
 *
 */
package de.evoila.cf.backup.clients.exception;

/**
 * @author Johannes Hiemer.
 */
public class S3ClientException extends Exception {

    public S3ClientException(Exception e) {
        super(e);
    }

    public S3ClientException(String s) {
        super(s);
    }

    /**
     *
     */
    private static final long serialVersionUID = 1L;

}
