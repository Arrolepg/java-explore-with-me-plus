package ru.practicum.ewm.service.event.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.ewm.service.adapter.EventStatsAdapter;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.model.*;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.event.utility.EventStatsHitParams;
import ru.practicum.ewm.service.event.utility.PaginationParams;
import ru.practicum.ewm.service.event.utility.PublicEventSearchFilter;
import ru.practicum.ewm.service.exception.BadRequestException;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.query.RequestQuery;
import ru.practicum.ewm.service.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceImplPublicUnitTest {
    private static final Long EVENT_ID_1 = 1L;
    private static final Long EVENT_ID_2 = 2L;
    private static final String URI = "/events";
    private static final String IP = "127.0.0.1";

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RequestQuery requestQuery;

    @Mock
    private EventStatsAdapter eventStatsAdapter;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void testFindAllPublicWithCorrectSearch() {
        Event event = createEvent(EVENT_ID_1, LocalDateTime.now().plusDays(5));

        when(eventRepository.findAllWithFetch(any(), any())).thenReturn(List.of(event));
        when(requestQuery.countRequestsByEventIdsAndStatus(
                List.of(EVENT_ID_1), RequestStatus.CONFIRMED
        ))
                .thenReturn(Map.of(EVENT_ID_1, 3L));
        when(eventStatsAdapter.getViews(List.of(EVENT_ID_1)))
                .thenReturn(Map.of(EVENT_ID_1, 3L));

        List<EventShortDto> events = eventService.findAllPublic(
                filterDefault(),
                params(0, 10, null),
                hitParams()
        );

        assertThat(events).hasSize(1);
        assertAll(
                () -> assertThat(events.getFirst().getId()).isEqualTo(EVENT_ID_1),
                () -> assertThat(events.getFirst().getConfirmedRequests()).isEqualTo(3L),
                () -> assertThat(events.getFirst().getConfirmedRequests()).isEqualTo(3L),
                () -> assertThat(events.getFirst().getViews()).isEqualTo(3L)
        );

        verify(eventStatsAdapter).createHit(URI, IP);
    }

    @Test
    void testFindAllPublicWithNoEvents() {
        when(eventRepository.findAllWithFetch(any(), any())).thenReturn(List.of());

        List<EventShortDto> events = eventService.findAllPublic(
                filterDefault(),
                params(0, 10, null),
                hitParams()
        );

        assertThat(events).isEmpty();

        verify(eventStatsAdapter).createHit(URI, IP);
        verifyNoMoreInteractions(requestQuery);
        verify(eventStatsAdapter, never()).getViews(anyList());
    }

    @Test
    void testFindAllPublicSortByViews() {
        Event event = createEvent(EVENT_ID_1, LocalDateTime.now().plusDays(5));
        Event event2 = createEvent(EVENT_ID_2, LocalDateTime.now().plusDays(5));

        when(eventRepository.findAllWithFetch(any(), any())).thenReturn(List.of(event, event2));
        when(requestQuery.countRequestsByEventIdsAndStatus(
                anyList(), eq(RequestStatus.CONFIRMED)
        ))
                .thenReturn(Map.of());
        when(eventStatsAdapter.getViews(List.of(EVENT_ID_1, EVENT_ID_2)))
                .thenReturn(Map.of(EVENT_ID_1, 3L, EVENT_ID_2, 10L));

        List<EventShortDto> events = eventService.findAllPublic(
                filterDefault(),
                params(0, 10, EventSearchSort.VIEWS),
                hitParams()
        );

        assertThat(events).hasSize(2);
        assertThat(events.getFirst().getId()).isEqualTo(EVENT_ID_2);
    }

    @Test
    void testFindAllPublicSortByViewsInMemorySkipAndLimit() {
        Event event = createEvent(EVENT_ID_1, LocalDateTime.now().plusDays(5));
        Event event2 = createEvent(EVENT_ID_2, LocalDateTime.now().plusDays(5));

        when(eventRepository.findAllWithFetch(any(), any())).thenReturn(List.of(event, event2));
        when(requestQuery.countRequestsByEventIdsAndStatus(
                anyList(), eq(RequestStatus.CONFIRMED)
        ))
                .thenReturn(Map.of());
        when(eventStatsAdapter.getViews(List.of(EVENT_ID_1, EVENT_ID_2)))
                .thenReturn(Map.of(EVENT_ID_1, 3L, EVENT_ID_2, 10L));

        List<EventShortDto> events = eventService.findAllPublic(
                filterDefault(),
                params(1, 1, EventSearchSort.VIEWS),
                hitParams()
        );

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getId()).isEqualTo(EVENT_ID_1);
    }

    @Test
    void testFindAllPublicSortByViewsUseUnpagedResult() {
        when(eventRepository.findAllWithFetch(any(), any())).thenReturn(List.of());

        eventService.findAllPublic(
                filterDefault(),
                params(0, 10, EventSearchSort.VIEWS),
                hitParams()
        );

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAllWithFetch(any(), captor.capture());

        assertThat(captor.getValue().isPaged()).isFalse();
    }

    @Test
    void testFindAllPublicSortByEventDateUsePageRequestWithDesc() {
        when(eventRepository.findAllWithFetch(any(), any())).thenReturn(List.of());

        eventService.findAllPublic(
                filterDefault(),
                params(0, 10, EventSearchSort.EVENT_DATE),
                hitParams()
        );

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAllWithFetch(any(), captor.capture());

        Pageable captured = captor.getValue();
        Sort.Order order = captured.getSort().getOrderFor(Event_.EVENT_DATE);

        assertAll(
                () -> assertThat(captured.isPaged()).isTrue(),
                () -> assertThat(captured.getPageSize()).isEqualTo(10),
                () -> assertThat(order).isNotNull(),
                () -> {
                    Assertions.assertNotNull(order);
                    assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
                }
        );
    }

    @Test
    void testFindAllPublicWithNoSortUseDefault() {
        when(eventRepository.findAllWithFetch(any(), any())).thenReturn(List.of());

        eventService.findAllPublic(
                filterDefault(),
                params(0, 10, null),
                hitParams()
        );

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAllWithFetch(any(), captor.capture());

        Pageable captured = captor.getValue();
        Sort.Order order = captured.getSort().getOrderFor(Event_.ID);

        assertAll(
                () -> assertThat(captured.isPaged()).isTrue(),
                () -> assertThat(captured.getPageSize()).isEqualTo(10),
                () -> assertThat(order).isNotNull(),
                () -> {
                    Assertions.assertNotNull(order);
                    assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
                }
        );
    }

    @Test
    void testFindAllPublicWithIncorrectRangeDates() {
        PublicEventSearchFilter filter = filter(
                LocalDateTime.of(2031, 1, 1, 0, 0),
                LocalDateTime.of(2030, 1, 1, 0, 0)
        );

        assertThatThrownBy(
                () -> eventService.findAllPublic(
                        filter,
                        params(0, 10, null),
                        hitParams()
                )
        )
                .isInstanceOf(BadRequestException.class);

        verifyNoMoreInteractions(eventRepository);
        verifyNoMoreInteractions(eventStatsAdapter);
    }

    private Event createEvent(Long id, LocalDateTime eventDate) {
        User initiator = new User();
        initiator.setId(1L);
        initiator.setEmail("user@mail.com");
        initiator.setName("User");
        Category category = createCategory();
        return Event.builder()
                .id(id)
                .annotation("Some annotation text here for event")
                .category(category)
                .description("Some description text here for event")
                .eventDate(eventDate)
                .eventLocation(EventLocation.builder().lat(55.75f).lon(37.62f).build())
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .title("Title")
                .state(EventState.PUBLISHED)
                .createdOn(LocalDateTime.now())
                .initiator(initiator)
                .build();
    }

    private Category createCategory() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Category");
        return category;
    }

    private PublicEventSearchFilter filter(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return new PublicEventSearchFilter(null, null, null, rangeStart, rangeEnd, false);
    }

    private PublicEventSearchFilter filterDefault() {
        return new PublicEventSearchFilter(null, null, null, null, null, null);
    }

    private PaginationParams params(int from, int size, EventSearchSort sort) {
        return new PaginationParams(from, size, sort);
    }

    private EventStatsHitParams hitParams() {
        return new EventStatsHitParams(URI, IP);
    }
}
