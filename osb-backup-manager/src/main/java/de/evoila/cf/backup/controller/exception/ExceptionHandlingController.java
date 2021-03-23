/**
 * 
 */
package de.evoila.cf.backup.controller.exception;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import de.evoila.cf.broker.model.ResponseMessage;

/**
 * 
 * @author Johannes Hiemer, Maximilian Büttner
 *
 */
@Controller
public class ExceptionHandlingController {

    protected ResponseEntity processErrorResponse(String message, HttpStatus status) {
        return new ResponseEntity<>(new ResponseMessage<>(message), status);
    }
    
    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleIOException(IOException ex) {
        return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BackupException.class)
    public ResponseEntity<String> handleBackupException(BackupException ex) {
        return processErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
}