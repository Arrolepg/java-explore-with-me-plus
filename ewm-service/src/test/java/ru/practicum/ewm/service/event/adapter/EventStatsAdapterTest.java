package ru.practicum.ewm.service.event.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import ru.practicum.ewm.service.adapter.EventStatsAdapter;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.client.exception.StatsClientException;
import ru.practicum.stats.dto.parameter.ParamDto;
import ru.practicum.stats.dto.request.EndpointHitDto;
import ru.practicum.stats.dto.response.ViewStatsDto;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventStatsAdapterTest {
    private static final Long EVENT_ID_1 = 1L;
    private static final Long EVENT_ID_2 = 2L;
    private static final String IP = "192.168.1.1";
    private static final String URI = "/events/1";

    @Mock
    private StatsClient statsClient;

    @InjectMocks
    private EventStatsAdapter eventStatsAdapter;

    @Test
    void testCreateHit() {
        when(statsClient.createHit(any(EndpointHitDto.class))).thenReturn(ResponseEntity.ok().build());

        eventStatsAdapter.createHit(URI, IP);

        verify(statsClient, times(1)).createHit(any(EndpointHitDto.class));
        verifyNoMoreInteractions(statsClient);
    }

    @Test
    void testCreateHitStatsClientException() {
        when(statsClient.createHit(any(EndpointHitDto.class)))
                .thenThrow(new StatsClientException("error", 500, "body"));

        eventStatsAdapter.createHit(URI, IP);

        verify(statsClient, times(1)).createHit(any(EndpointHitDto.class));
    }

    @Test
    void testGetViewsList() {
        Event event1 = createEvent(EVENT_ID_1);
        Event event2 = createEvent(EVENT_ID_2);
        List<ViewStatsDto> stats = List.of(
                createViewStats("/events/1", 5L),
                createViewStats("/events/2", 3L)
        );
        when(statsClient.get(any(ParamDto.class)))
                .thenReturn(ResponseEntity.ok(stats));

        Map<Long, Long> result = eventStatsAdapter.getViews(List.of(event1, event2));

        assertThat(result)
                .hasSize(2)
                .containsEntry(EVENT_ID_1, 5L)
                .containsEntry(EVENT_ID_2, 3L);

        verify(statsClient, times(1)).get(any(ParamDto.class));
        verifyNoMoreInteractions(statsClient);
    }

    @Test
    void testGetViewsListEmpty() {
        Map<Long, Long> result = eventStatsAdapter.getViews(List.of());

        assertThat(result).isEmpty();

        verifyNoInteractions(statsClient);
    }

    @Test
    void testGetViewsListNull() {
        Map<Long, Long> result = eventStatsAdapter.getViews((List<Event>) null);

        assertThat(result).isEmpty();

        verifyNoInteractions(statsClient);
    }

    @Test
    void testGetViewsListStatsBodyNull() {
        Event event = createEvent(EVENT_ID_1);
        when(statsClient.get(any(ParamDto.class)))
                .thenReturn(ResponseEntity.ok(null));

        Map<Long, Long> result = eventStatsAdapter.getViews(List.of(event));

        assertThat(result).isEmpty();

        verify(statsClient, times(1)).get(any(ParamDto.class));
    }

    @Test
    void testGetViewsListStatsBodyEmpty() {
        Event event = createEvent(EVENT_ID_1);
        when(statsClient.get(any(ParamDto.class)))
                .thenReturn(ResponseEntity.ok(List.of()));

        Map<Long, Long> result = eventStatsAdapter.getViews(List.of(event));

        assertThat(result).isEmpty();

        verify(statsClient, times(1)).get(any(ParamDto.class));
    }

    @Test
    void testGetViewsListFiltersForeignUris() {
        Event event = createEvent(EVENT_ID_1);
        List<ViewStatsDto> stats = List.of(
                createViewStats("/events/1", 5L),
                createViewStats("/other/2", 10L)
        );
        when(statsClient.get(any(ParamDto.class)))
                .thenReturn(ResponseEntity.ok(stats));

        Map<Long, Long> result = eventStatsAdapter.getViews(List.of(event));

        assertThat(result)
                .hasSize(1)
                .containsEntry(EVENT_ID_1, 5L);
    }

    @Test
    void testGetViewsListSumDuplicateUris() {
        Event event = createEvent(EVENT_ID_1);
        List<ViewStatsDto> stats = List.of(
                createViewStats("/events/1", 5L),
                createViewStats("/events/1", 3L)
        );
        when(statsClient.get(any(ParamDto.class)))
                .thenReturn(ResponseEntity.ok(stats));

        Map<Long, Long> result = eventStatsAdapter.getViews(List.of(event));

        assertThat(result)
                .hasSize(1)
                .containsEntry(EVENT_ID_1, 8L);
    }

    @Test
    void testGetViewsListStatsClientException() {
        Event event = createEvent(EVENT_ID_1);
        when(statsClient.get(any(ParamDto.class)))
                .thenThrow(new StatsClientException("error", 500, "body"));

        Map<Long, Long> result = eventStatsAdapter.getViews(List.of(event));

        assertThat(result).isEmpty();

        verify(statsClient, times(1)).get(any(ParamDto.class));
    }

    @Test
    void testGetViewsEvent() {
        Event event = createEvent(EVENT_ID_1);
        List<ViewStatsDto> stats = List.of(createViewStats("/events/1", 5L));
        when(statsClient.get(any(ParamDto.class)))
                .thenReturn(ResponseEntity.ok(stats));

        Long result = eventStatsAdapter.getViews(event);

        assertThat(result).isEqualTo(5L);

        verify(statsClient, times(1)).get(any(ParamDto.class));
    }

    @Test
    void testGetViewsEventNoStats() {
        Event event = createEvent(EVENT_ID_1);
        when(statsClient.get(any(ParamDto.class)))
                .thenReturn(ResponseEntity.ok(List.of()));

        Long result = eventStatsAdapter.getViews(event);

        assertThat(result).isZero();
    }

    @Test
    void testGetViewsEventNull() {
        Long result = eventStatsAdapter.getViews((Event) null);

        assertThat(result).isZero();

        verifyNoInteractions(statsClient);
    }

    private Event createEvent(Long id) {
        Event event = new Event();
        event.setId(id);
        return event;
    }

    private ViewStatsDto createViewStats(String uri, Long hits) {
        return ViewStatsDto.builder()
                .app("ewm-service")
                .uri(uri)
                .hits(hits)
                .build();
    }
}