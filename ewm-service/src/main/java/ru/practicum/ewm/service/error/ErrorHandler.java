package ru.practicum.ewm.service.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.ewm.service.exception.BadRequestException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.stats.dto.error.ApiError;

import java.io.PrintWriter;
import java.io.StringWriter;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler({
            BadRequestException.class,
            MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(final Exception e) {
        log.warn("400 {}", e.getMessage());
        return new ApiError(HttpStatus.BAD_REQUEST.name(), "Incorrectly made request.", e.getMessage(), getStackTrace(e));
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(final Exception e) {
        log.error("500 {}", e.getMessage(), e);
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.name(), "Internal server error.", e.getMessage(), getStackTrace(e));
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(final NotFoundException e) {
        log.error("404 {}", e.getMessage(), e);
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.name(), "Объект не найден", e.getMessage(), getStackTrace(e));
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
