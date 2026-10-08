package ru.practicum.ewm.service.request.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.repository.RequestRepository;
import ru.practicum.ewm.service.request.repository.projection.EventRequestsCount;
import ru.practicum.ewm.service.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RequestQueryImplUnitTest {
    private static final Long EVENT_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final Long REQUEST_ID = 10L;

    @Mock
    private RequestRepository requestRepository;

    @InjectMocks
    private RequestQueryImpl requestQuery;

    @Test
    void testFindRequests() {
        Request request = createRequest(REQUEST_ID, EVENT_ID, USER_ID, RequestStatus.PENDING);
        when(requestRepository.findRequestsByEventId(EVENT_ID)).thenReturn(List.of(request));

        List<ParticipationRequestDto> result = requestQuery.findRequests(EVENT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(REQUEST_ID);
        assertThat(result.getFirst().getEventId()).isEqualTo(EVENT_ID);
        assertThat(result.getFirst().getRequesterId()).isEqualTo(USER_ID);

        verify(requestRepository, times(1)).findRequestsByEventId(EVENT_ID);
        verifyNoMoreInteractions(requestRepository);
    }

    @Test
    void testFindRequestsEmpty() {
        when(requestRepository.findRequestsByEventId(EVENT_ID)).thenReturn(List.of());

        List<ParticipationRequestDto> result = requestQuery.findRequests(EVENT_ID);

        assertThat(result).isEmpty();

        verify(requestRepository, times(1)).findRequestsByEventId(EVENT_ID);
        verifyNoMoreInteractions(requestRepository);
    }

    @Test
    void testFindRequestsByIdsAndEventId() {
        Request request = createRequest(REQUEST_ID, EVENT_ID, USER_ID, RequestStatus.PENDING);
        when(requestRepository.findRequestsByIdInAndEventId(List.of(REQUEST_ID), EVENT_ID))
                .thenReturn(List.of(request));

        List<Request> result = requestQuery.findRequestsByIdsAndEventId(List.of(REQUEST_ID), EVENT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(REQUEST_ID);

        verify(requestRepository, times(1)).findRequestsByIdInAndEventId(List.of(REQUEST_ID), EVENT_ID);
        verifyNoMoreInteractions(requestRepository);
    }

    @Test
    void testFindRequestsByIdsAndEventIdNotFound() {
        when(requestRepository.findRequestsByIdInAndEventId(List.of(REQUEST_ID), EVENT_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> requestQuery.findRequestsByIdsAndEventId(List.of(REQUEST_ID), EVENT_ID))
                .isInstanceOf(NotFoundException.class);

        verify(requestRepository, times(1)).findRequestsByIdInAndEventId(List.of(REQUEST_ID), EVENT_ID);
        verifyNoMoreInteractions(requestRepository);
    }

    @Test
    void testCountRequestsByEventIdAndStatus() {
        when(requestRepository.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED))
                .thenReturn(5L);

        long result = requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED);

        assertThat(result).isEqualTo(5L);

        verify(requestRepository, times(1))
                .countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED);
        verifyNoMoreInteractions(requestRepository);
    }

    @Test
    void testFindRequestsByEventIdAndStatus() {
        Request request = createRequest(REQUEST_ID, EVENT_ID, USER_ID, RequestStatus.PENDING);
        when(requestRepository.findRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.PENDING))
                .thenReturn(List.of(request));

        List<Request> result = requestQuery.findRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.PENDING);

        assertThat(result).hasSize(1);

        verify(requestRepository, times(1))
                .findRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.PENDING);
        verifyNoMoreInteractions(requestRepository);
    }

    @Test
    void testCountRequestsByEventIdsAndStatus() {
        List<EventRequestsCount> projections = List.of(
                new EventRequestsCount(1L, 3L),
                new EventRequestsCount(2L, 5L)
        );
        when(requestRepository.countRequestsByEventIdsAndStatus(
                List.of(1L, 2L), RequestStatus.CONFIRMED)).thenReturn(projections);

        Map<Long, Long> result = requestQuery.countRequestsByEventIdsAndStatus(
                List.of(1L, 2L), RequestStatus.CONFIRMED);

        assertThat(result)
                .hasSize(2)
                .containsEntry(1L, 3L)
                .containsEntry(2L, 5L);

        verify(requestRepository, times(1))
                .countRequestsByEventIdsAndStatus(List.of(1L, 2L), RequestStatus.CONFIRMED);
        verifyNoMoreInteractions(requestRepository);
    }

    @Test
    void testCountRequestsByEventIdsAndStatusEmpty() {
        when(requestRepository.countRequestsByEventIdsAndStatus(
                List.of(999L), RequestStatus.CONFIRMED)).thenReturn(List.of());

        Map<Long, Long> result = requestQuery.countRequestsByEventIdsAndStatus(
                List.of(999L), RequestStatus.CONFIRMED);

        assertThat(result).isEmpty();

        verify(requestRepository, times(1))
                .countRequestsByEventIdsAndStatus(List.of(999L), RequestStatus.CONFIRMED);
        verifyNoMoreInteractions(requestRepository);
    }

    private Request createRequest(Long id, Long eventId, Long requesterId, RequestStatus status) {
        User requester = new User();
        requester.setId(requesterId);

        Event event = new Event();
        event.setId(eventId);

        Request request = new Request();
        request.setId(id);
        request.setEvent(event);
        request.setRequester(requester);
        request.setStatus(status);
        request.setCreated(LocalDateTime.now());
        return request;
    }
}