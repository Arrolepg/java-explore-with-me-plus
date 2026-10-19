package ru.practicum.ewm.service.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class TwoHoursFromNowValidatorTest {
    private final TwoHoursFromNowValidator validator = new TwoHoursFromNowValidator();
    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

    @Test
    void testNullValueIsValid() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    void testPastDateIsInvalid() {
        assertThat(validator.isValid(LocalDateTime.now().minusHours(1), context)).isFalse();
    }

    @Test
    void testNowIsInvalid() {
        assertThat(validator.isValid(LocalDateTime.now(), context)).isFalse();
    }

    @Test
    void testOneHourFromNowIsInvalid() {
        assertThat(validator.isValid(LocalDateTime.now().plusHours(1), context)).isFalse();
    }

    @Test
    void testThreeHoursFromNowIsValid() {
        assertThat(validator.isValid(LocalDateTime.now().plusHours(3), context)).isTrue();
    }

    @Test
    void testOneDayFromNowIsValid() {
        assertThat(validator.isValid(LocalDateTime.now().plusDays(1), context)).isTrue();
    }
}