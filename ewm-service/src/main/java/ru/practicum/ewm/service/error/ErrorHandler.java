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
import ru.practicum.common.error.ApiError;
import ru.practicum.ewm.service.exception.BadRequestException;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.exception.SortException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler({
            BadRequestException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            SortException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(final Exception e) {
        log.warn("400 {}", e.getMessage());
        return new ApiError(HttpStatus.BAD_REQUEST.name(),
                "Incorrectly made request.",
                e.getMessage(),
                LocalDateTime.now());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(final MethodArgumentNotValidException e) {
        log.warn("400 {}", e.getMessage());

        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> String.format("Field: %s. Error: %s. Value: %s",
                        fieldError.getField(),
                        fieldError.getDefaultMessage(),
                        fieldError.getRejectedValue())
                )
                .collect(Collectors.joining("; "));

        return new ApiError(
                HttpStatus.BAD_REQUEST.name(),
                "Incorrectly made request.",
                message,
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(final Exception e) {
        log.error("500 {}", e.getMessage(), e);
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.name(),
                "Internal server error.",
                e.getMessage(),
                LocalDateTime.now());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(final NotFoundException e) {
        log.warn("404 {}", e.getMessage());
        return new ApiError(HttpStatus.NOT_FOUND.name(),
                "The required object was not found",
                e.getMessage(),
                LocalDateTime.now());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(final ConflictException e) {
        log.warn("409 {}", e.getMessage());
        return new ApiError(
                HttpStatus.CONFLICT.name(),
                "For the requested operation the conditions are not met.",
                e.getMessage(),
                LocalDateTime.now()
        );
    }
}