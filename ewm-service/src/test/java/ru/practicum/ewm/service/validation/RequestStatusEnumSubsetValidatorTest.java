package ru.practicum.ewm.service.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.practicum.ewm.service.request.model.RequestStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RequestStatusEnumSubsetValidatorTest {
    private RequestStatusEnumSubsetValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        context = mock(ConstraintValidatorContext.class);
        validator = new RequestStatusEnumSubsetValidator();

        RequestStatusEnumSubset annotation = mock(RequestStatusEnumSubset.class);
        Mockito.when(annotation.anyOf())
                .thenReturn(new RequestStatus[]{RequestStatus.CONFIRMED, RequestStatus.REJECTED});
        validator.initialize(annotation);
    }

    @Test
    void testNullValueIsValid() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    void testAllowedValueIsValid() {
        assertThat(validator.isValid(RequestStatus.CONFIRMED, context)).isTrue();
        assertThat(validator.isValid(RequestStatus.REJECTED, context)).isTrue();
    }

    @Test
    void testDisallowedValueIsInvalid() {
        assertThat(validator.isValid(RequestStatus.PENDING, context)).isFalse();
        assertThat(validator.isValid(RequestStatus.CANCELED, context)).isFalse();
    }
}