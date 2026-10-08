package ru.practicum.ewm.service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import ru.practicum.ewm.service.request.model.RequestStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequestStatusEnumSubsetValidator.class)
public @interface RequestStatusEnumSubset {
    String message() default "Недопустимое значение поля для данного запроса";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    RequestStatus[] anyOf();
}
