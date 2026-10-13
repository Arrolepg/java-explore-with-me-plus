package ru.practicum.ewm.service.event.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.service.event.model.EventSearchSort;
import ru.practicum.ewm.service.event.service.EventService;
import ru.practicum.ewm.service.event.utility.EventStatsHitParams;
import ru.practicum.ewm.service.common.PaginationParams;
import ru.practicum.ewm.service.event.utility.PublicEventSearchFilter;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicEventController.class)
public class PublicEventControllerTest {
    private static final String API_PREFIX = "/events";
    private static final Long EVENT_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Test
    void testFindAllWithAllParams() throws Exception {
        when(eventService.findAllPublic(any(PublicEventSearchFilter.class),
                any(PaginationParams.class),
                any(EventStatsHitParams.class)))
                .thenReturn(List.of());

        mockMvc.perform(get(API_PREFIX)
                        .param("text", "first")
                        .param("categories", "1")
                        .param("paid", "false")
                        .param("rangeStart", "2030-01-01 00:00:00")
                        .param("rangeEnd", "2031-01-01 00:00:00")
                        .param("sort", "VIEWS")
                        .param("onlyAvailable", "true")
                        .param("from", "5")
                        .param("size", "5")
                )
                .andExpect(
                        status().isOk()
                );

        verify(eventService).findAllPublic(
                argThat(filter -> {
                    assertAll(
                            () -> assertThat(filter.text()).isEqualTo("first"),
                            () -> assertThat(filter.categories()).containsExactly(1L),
                            () -> assertThat(filter.paid()).isEqualTo(false),
                            () -> assertThat(filter.rangeStart()).isEqualTo(LocalDateTime.of(2030, 1, 1, 0, 0)),
                            () -> assertThat(filter.rangeEnd()).isEqualTo(LocalDateTime.of(2031, 1, 1, 0, 0)),
                            () -> assertThat(filter.onlyAvailable()).isEqualTo(true)
                    );
                    return true;
                }),
                argThat(paginationParams -> {
                    assertAll(
                            () -> assertThat(paginationParams.from()).isEqualTo(5),
                            () -> assertThat(paginationParams.size()).isEqualTo(5),
                            () -> assertThat(paginationParams.sort()).isEqualTo(EventSearchSort.VIEWS)
                    );
                    return true;
                }),
                argThat(hitParams -> {
                    assertAll(
                            () -> assertThat(hitParams.uri()).isEqualTo(API_PREFIX),
                            () -> assertThat(hitParams.ip()).isEqualTo("127.0.0.1")
                    );
                    return true;
                })
        );
    }

    @Test
    void testFindAllWithDefaultsValues() throws Exception {
        when(eventService.findAllPublic(any(PublicEventSearchFilter.class),
                any(PaginationParams.class),
                any(EventStatsHitParams.class)))
                .thenReturn(List.of());

        mockMvc.perform(get(API_PREFIX)
                        .param("text", "first")
                        .param("categories", "1")
                        .param("paid", "false")
                        .param("rangeStart", "2030-01-01 00:00:00")
                        .param("rangeEnd", "2031-01-01 00:00:00")
                )
                .andExpect(
                        status().isOk()
                );

        verify(eventService).findAllPublic(
                argThat(filter -> {
                    assertAll(
                            () -> assertThat(filter.text()).isEqualTo("first"),
                            () -> assertThat(filter.categories()).containsExactly(1L),
                            () -> assertThat(filter.paid()).isEqualTo(false),
                            () -> assertThat(filter.rangeStart()).isEqualTo(LocalDateTime.of(2030, 1, 1, 0, 0)),
                            () -> assertThat(filter.rangeEnd()).isEqualTo(LocalDateTime.of(2031, 1, 1, 0, 0)),
                            () -> assertThat(filter.onlyAvailable()).isEqualTo(false)
                    );
                    return true;
                }),
                argThat(paginationParams -> {
                    assertAll(
                            () -> assertThat(paginationParams.from()).isEqualTo(0),
                            () -> assertThat(paginationParams.size()).isEqualTo(10),
                            () -> assertNull(paginationParams.sort())
                    );
                    return true;
                }),
                argThat(hitParams -> {
                    assertAll(
                            () -> assertThat(hitParams.uri()).isEqualTo(API_PREFIX),
                            () -> assertThat(hitParams.ip()).isEqualTo("127.0.0.1")
                    );
                    return true;
                })
        );
    }

    @Test
    void testFindAllWithWrongDateFormatForRangeStartReturnsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("rangeStart", "not-a-date"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testFindAllWithWrongDateFormatForRangeEndReturnsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("rangeEnd", "not-a-date"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testFindAllWithUnknownSortReturnsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("sort", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testFindAllWithNegativeFromThrowsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("from", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testFindAllWithZeroSizeThrowsBadRequest() throws Exception {
        mockMvc.perform(get(API_PREFIX).param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void testFindByIdPublic() throws Exception {
        when(eventService.findByIdPublic(eq(EVENT_ID),
                any(EventStatsHitParams.class)))
                .thenReturn(null);

        mockMvc.perform(get(API_PREFIX + "/" + EVENT_ID))
                .andExpect(
                        status().isOk()
                );

        verify(eventService).findByIdPublic(
                argThat(eventId -> {
                    assertThat(eventId).isEqualTo(EVENT_ID);
                    return true;
                }),
                argThat(hitParams -> {
                    assertAll(
                            () -> assertThat(hitParams.uri()).isEqualTo(API_PREFIX + "/" + EVENT_ID),
                            () -> assertThat(hitParams.ip()).isEqualTo("127.0.0.1")
                    );
                    return true;
                })
        );
    }

    @Test
    void testFindByIdPublicWithIncorrectId() throws Exception {
        mockMvc.perform(get(API_PREFIX + "/incorretId"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }
}
