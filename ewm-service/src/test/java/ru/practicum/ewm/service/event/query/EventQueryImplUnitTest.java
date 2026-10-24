package ru.practicum.ewm.service.event.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.service.adapter.EventStatsAdapter;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.comment.model.CommentState;
import ru.practicum.ewm.service.comment.query.CommentQuery;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.query.RequestQuery;
import ru.practicum.ewm.service.user.model.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventQueryImplUnitTest {
    private static final Long EVENT_ID = 1L;
    private static final Long CATEGORY_ID = 2L;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventQueryImpl eventQuery;

    @Mock
    private RequestQuery requestQuery;

    @Mock
    private CommentQuery commentQuery;

    @Mock
    private EventStatsAdapter eventStatsAdapter;

    @Test
    void testFindEvent() {
        Event event = Event.builder().id(EVENT_ID).build();
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        Event result = eventQuery.findEvent(EVENT_ID);

        assertThat(result).isEqualTo(event);
        verify(eventRepository, times(1)).findById(EVENT_ID);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void testFindEventNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventQuery.findEvent(99L))
                .isInstanceOf(NotFoundException.class);

        verify(eventRepository, times(1)).findById(99L);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void testExistsByCategoryIdTrue() {
        when(eventRepository.existsEventByCategoryId(CATEGORY_ID)).thenReturn(true);

        assertThat(eventQuery.existsByCategoryId(CATEGORY_ID)).isTrue();

        verify(eventRepository, times(1)).existsEventByCategoryId(CATEGORY_ID);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void testExistsByCategoryIdFalse() {
        when(eventRepository.existsEventByCategoryId(CATEGORY_ID)).thenReturn(false);

        assertThat(eventQuery.existsByCategoryId(CATEGORY_ID)).isFalse();

        verify(eventRepository, times(1)).existsEventByCategoryId(CATEGORY_ID);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void getEventsDtoReturnsEmptyListWhenEventsAreEmpty() {
        List<EventShortDto> result = eventQuery.getEventsDto(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(requestQuery, eventStatsAdapter);
    }

    @Test
    void getEventsDtoReturnsMappedEventsWithStatistics() {
        Event event1 = createEventForDto(1L, "Concert", "Music", "Igor");
        Event event2 = createEventForDto(2L, "Play", "Theatre", "Ivan");
        List<Long> ids = List.of(1L, 2L);

        when(requestQuery.countRequestsByEventIdsAndStatus(
                ids, RequestStatus.CONFIRMED))
                .thenReturn(Map.of(1L, 3L, 2L, 7L));

        when(eventStatsAdapter.getViews(ids))
                .thenReturn(Map.of(1L, 10L, 2L, 20L));
        when(commentQuery.countCommentsByEventIdsAndState(ids, CommentState.PUBLISHED))
                .thenReturn(Map.of(1L, 3L, 2L, 5L));

        List<EventShortDto> result =
                eventQuery.getEventsDto(List.of(event1, event2));

        assertThat(result).hasSize(2);

        EventShortDto dto1 = result.getFirst();
        assertThat(dto1.getId()).isEqualTo(1L);
        assertThat(dto1.getTitle()).isEqualTo("Concert");
        assertThat(dto1.getCategory().getId()).isEqualTo(101L);
        assertThat(dto1.getCategory().getName()).isEqualTo("Music");
        assertThat(dto1.getInitiator().getId()).isEqualTo(201L);
        assertThat(dto1.getInitiator().getName()).isEqualTo("Igor");
        assertThat(dto1.getConfirmedRequests()).isEqualTo(3L);
        assertThat(dto1.getViews()).isEqualTo(10L);
        assertThat(dto1.getCommentsCount()).isEqualTo(3L);

        EventShortDto dto2 = result.getLast();
        assertThat(dto2.getId()).isEqualTo(2L);
        assertThat(dto2.getTitle()).isEqualTo("Play");
        assertThat(dto2.getCategory().getId()).isEqualTo(102L);
        assertThat(dto2.getCategory().getName()).isEqualTo("Theatre");
        assertThat(dto2.getInitiator().getId()).isEqualTo(202L);
        assertThat(dto2.getInitiator().getName()).isEqualTo("Ivan");
        assertThat(dto2.getConfirmedRequests()).isEqualTo(7L);
        assertThat(dto2.getViews()).isEqualTo(20L);
        assertThat(dto2.getCommentsCount()).isEqualTo(5L);

        verify(requestQuery).countRequestsByEventIdsAndStatus(
                ids, RequestStatus.CONFIRMED);
        verify(eventStatsAdapter).getViews(ids);
        verify(commentQuery).countCommentsByEventIdsAndState(
                ids, CommentState.PUBLISHED
        );
    }

    @Test
    void getEventsDtoUsesZerosWhenStatisticsAreMissing() {
        Event event = createEventForDto(1L, "Concert", "Music", "Igor");
        List<Long> ids = List.of(1L);

        when(requestQuery.countRequestsByEventIdsAndStatus(
                ids, RequestStatus.CONFIRMED))
                .thenReturn(Map.of());

        when(eventStatsAdapter.getViews(ids))
                .thenReturn(Map.of());
        when(commentQuery.countCommentsByEventIdsAndState(ids, CommentState.PUBLISHED))
                .thenReturn(Map.of());

        List<EventShortDto> result = eventQuery.getEventsDto(List.of(event));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
        assertThat(result.getFirst().getConfirmedRequests()).isZero();
        assertThat(result.getFirst().getViews()).isZero();
        assertThat(result.getFirst().getCommentsCount()).isZero();
    }

    private Event createEventForDto(
            Long id, String title, String categoryName, String userName) {

        Category category = new Category();
        category.setId(100L + id);
        category.setName(categoryName);

        User user = new User();
        user.setId(200L + id);
        user.setName(userName);

        return Event.builder()
                .id(id)
                .title(title)
                .category(category)
                .initiator(user)
                .build();
    }
}
