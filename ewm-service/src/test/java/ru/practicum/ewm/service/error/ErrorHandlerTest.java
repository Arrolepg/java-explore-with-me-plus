package ru.practicum.ewm.service.error;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.service.exception.BadRequestException;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErrorHandlerTest {
    private static final String NOT_FOUND_MSG = "Event with id=1 was not found";
    private static final String CONFLICT_MSG = "Only pending or canceled events can be changed";
    private static final String BAD_REQUEST_MSG = "Event must be published";
    private static final String SORT_MSG = "Unknown sort: FOO";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ErrorHandler errorHandler = new ErrorHandler();

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            mockMvc = MockMvcBuilders
                    .standaloneSetup(new ThrowingController())
                    .setControllerAdvice(errorHandler)
                    .setValidator(new SpringValidatorAdapter(validator))
                    .build();
        }
    }

    @Test
    void testValidationBlankField() throws Exception {
        String body = "{\"name\":\"\"}";

        mockMvc.perform(post("/test/valid-body")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message", containsString("Field: name")))
                .andExpect(jsonPath("$.message", containsString("Имя не может быть пустым")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testValidationMultipleErrors() throws Exception {
        String body = "{\"name\":\"\",\"description\":\"\"}";

        mockMvc.perform(post("/test/valid-body")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Field: name")))
                .andExpect(jsonPath("$.message", containsString("Field: description")))
                .andExpect(jsonPath("$.message", containsString("; ")));
    }

    @Test
    void testValidationTooLong() throws Exception {
        String longName = "a".repeat(3000);
        String body = "{\"name\":\"" + longName + "\"}";

        mockMvc.perform(post("/test/valid-body")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Field: name")))
                .andExpect(jsonPath("$.message", containsString("Максимальная длина")));
    }

    @Test
    void testHttpMessageNotReadable() throws Exception {
        String badBody = "{not a valid json";

        mockMvc.perform(post("/test/valid-body")
                        .contentType("application/json")
                        .content(badBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."));
    }

    @Test
    void testMissingRequestParam() throws Exception {
        mockMvc.perform(get("/test/required-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("requiredParam")));
    }

    @Test
    void testMethodArgumentTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/path/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("abc")));
    }

    @Test
    void testBadRequestException() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message").value(BAD_REQUEST_MSG));
    }

    @Test
    void testNotFoundException() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").value("The required object was not found"))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MSG));
    }

    @Test
    void testConflictException() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.reason")
                        .value("For the requested operation the conditions are not met."))
                .andExpect(jsonPath("$.message").value(CONFLICT_MSG));
    }

    @Test
    void testGenericException() throws Exception {
        mockMvc.perform(get("/test/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.reason").value("Internal server error."))
                .andExpect(jsonPath("$.message").value("Unexpected failure"));
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/test/path/{id}")
        Long withPath(@PathVariable Long id) {
            return id;
        }

        @GetMapping("/test/required-param")
        Long withRequiredParam(@RequestParam String requiredParam) {
            return 1L;
        }

        @PostMapping("/test/valid-body")
        TestDto withValidBody(@Valid @RequestBody TestDto dto) {
            return dto;
        }

        @GetMapping("/test/bad-request")
        void throwBadRequest() {
            throw new BadRequestException(BAD_REQUEST_MSG);
        }

        @GetMapping("/test/not-found")
        void throwNotFound() {
            throw new NotFoundException(NOT_FOUND_MSG);
        }

        @GetMapping("/test/conflict")
        void throwConflict() {
            throw new ConflictException(CONFLICT_MSG);
        }

        @GetMapping("/test/error")
        void throwGeneric() {
            throw new RuntimeException("Unexpected failure");
        }
    }

    @Data
    static class TestDto {
        @NotBlank(message = "Имя не может быть пустым")
        @Size(max = 2000, message = "Максимальная длина имени - 2000 символов")
        private String name;

        @NotBlank(message = "Описание не может быть пустым")
        @Size(max = 7000, message = "Максимальная длина описания - 7000 символов")
        private String description;
    }
}