package de.evoila.cf.backup.controller;

import de.evoila.cf.model.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import javax.servlet.http.HttpServletResponse;

public abstract class BaseController {

    private final Logger log = LoggerFactory.getLogger(BaseController.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessage> handleException(HttpMessageNotReadableException ex, HttpServletResponse response) {
        return processErrorResponse(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)

    public ResponseEntity<ErrorMessage> handleException(MethodArgumentNotValidException ex,
                                                        HttpServletResponse response) {
        BindingResult result = ex.getBindingResult();
        String message = "Missing required fields:";
        for (FieldError error: result.getFieldErrors()) {
            message += " " + error.getField();
        }
        return processErrorResponse(message, HttpStatus.BAD_REQUEST);
    }

    protected ResponseEntity<ErrorMessage> processErrorResponse(String message, HttpStatus status) {
        return new ResponseEntity<>(new ErrorMessage(message), status);
    }
}
