package ru.practicum.ewm.service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.practicum.ewm.service.request.model.RequestStatus;

import java.util.Arrays;
import java.util.List;

public class RequestStatusEnumSubsetValidator implements ConstraintValidator<RequestStatusEnumSubset, RequestStatus> {
    private List<RequestStatus> allowedValues;

    @Override
    public void initialize(RequestStatusEnumSubset constraintAnnotation) {
        this.allowedValues = Arrays.asList(constraintAnnotation.anyOf());
    }

    @Override
    public boolean isValid(RequestStatus value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null) {
            return true;
        }

        return allowedValues.contains(value);
    }
}
