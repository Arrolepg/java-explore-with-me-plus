package ru.practicum.ewm.service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

public class TwoHoursFromNowValidator implements ConstraintValidator<TwoHoursFromNow, LocalDateTime> {
    private static final long MIN_HOURS = 2;

    @Override
    public boolean isValid(LocalDateTime dateTime, ConstraintValidatorContext constraintValidatorContext) {
        if (dateTime == null) {
            return true;
        }

        return dateTime.isAfter(LocalDateTime.now().plusHours(MIN_HOURS));
    }
}
