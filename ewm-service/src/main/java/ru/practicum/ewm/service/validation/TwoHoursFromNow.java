package ru.practicum.ewm.service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TwoHoursFromNowValidator.class)
public @interface TwoHoursFromNow {
    String message() default "Дата и время на которые намечено событие не может быть раньше, " +
            "чем через два часа от текущего момента";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
