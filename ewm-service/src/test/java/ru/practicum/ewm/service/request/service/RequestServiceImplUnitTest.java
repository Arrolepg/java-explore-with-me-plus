package ru.practicum.ewm.service.request.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.query.EventQuery;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.repository.RequestRepository;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.query.UserQuery;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RequestServiceImplUnitTest {

    @InjectMocks
    private RequestServiceImpl requestService;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private UserQuery userQuery;

    @Mock
    private EventQuery eventQuery;

    @Test
    void findAllByRequesterReturnsEmptyList() {
        User user = getUser(1L);

        when(userQuery.findUser(1L))
                .thenReturn(user);
        when(requestRepository.findByRequester_Id(1L))
                .thenReturn(List.of());

        assertThat(requestService.findAllByRequester(1L))
                .isEmpty();
    }

    @Test
    void shouldBeCanceledRequest() {
        User user = getUser(1L);

        Event event = getEvent(user, EventState.PUBLISHED);

        Request request = new Request();
        request.setId(3L);
        request.setEvent(event);
        request.setRequester(user);
        request.setStatus(RequestStatus.PENDING);

        when(userQuery.findUser(1L))
                .thenReturn(user);
        when(requestRepository.findByIdAndRequester_Id(3L, 1L))
                .thenReturn(Optional.of(request));
        when(requestRepository.save(request))
                .thenReturn(request);

        assertThat(requestService.cancelRequest(1L, 3L).getStatus())
                .isEqualTo(RequestStatus.CANCELED);
    }

    @Test
    void cancelRequestThrowsNotFoundWhenRequestDoesNotExist() {
        User user = getUser(1L);

        when(userQuery.findUser(1L))
                .thenReturn(user);
        when(requestRepository.findByIdAndRequester_Id(3L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.cancelRequest(1L, 3L))
                .isInstanceOf(NotFoundException.class);

        verify(requestRepository, never()).save(any(Request.class));
    }

    @Test
    void createRequestThrowsConflictWhenUserInitiatorEvent() {
        User user = getUser(1L);

        Event event = getEvent(user, EventState.PUBLISHED);

        when(userQuery.findUser(1L))
                .thenReturn(user);
        when(eventQuery.findEvent(2L))
                .thenReturn(event);

        assertThatThrownBy(() -> requestService.create(1L, 2L))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createRequestThrowsConflictWhenEventStateIsNotPublished() {
        User user = getUser(1L);
        User user2 = getUser(3L);

        Event event = getEvent(user2, EventState.PENDING);

        when(userQuery.findUser(1L))
                .thenReturn(user);
        when(eventQuery.findEvent(2L))
                .thenReturn(event);

        assertThatThrownBy(() -> requestService.create(1L, 2L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Событие не опубликовано");
    }

    @Test
    void createRequestThrowsConflictWhenNewRequestIsCopy() {
        User user = getUser(1L);
        User user2 = getUser(2L);

        Event event = getEvent(user2, EventState.PUBLISHED);

        when(userQuery.findUser(1L))
                .thenReturn(user);
        when(eventQuery.findEvent(2L))
                .thenReturn(event);
        when(requestRepository.existsByRequester_IdAndEvent_Id(1L, 2L))
                .thenReturn(true);

        assertThatThrownBy(() -> requestService.create(1L, 2L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Запрос был создан ранее");
    }

    @Test
    void createRequestThrowsConflictWhenLimitReached() {
        User user = getUser(1L);
        User user2 = getUser(2L);

        Event event = getEvent(user2, EventState.PUBLISHED);

        when(userQuery.findUser(1L))
                .thenReturn(user);
        when(eventQuery.findEvent(2L))
                .thenReturn(event);
        when(requestRepository.countRequestsByEventIdAndStatus(2L, RequestStatus.CONFIRMED))
                .thenReturn(2L);

        assertThatThrownBy(() -> requestService.create(1L, 2L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Достигнут лимит запросов на участие");

    }

    @Test
    void testCreateRequest() {
        User user = getUser(1L);
        User user2 = getUser(2L);

        Event event = getEvent(user2, EventState.PUBLISHED);
        event.setRequestModeration(false);

        when(userQuery.findUser(1L))
                .thenReturn(user);
        when(eventQuery.findEvent(2L))
                .thenReturn(event);
        when(requestRepository.countRequestsByEventIdAndStatus(2L, RequestStatus.CONFIRMED))
                .thenReturn(1L);

        assertThat(requestService.create(1L, 2L).getStatus())
                .isEqualTo(RequestStatus.CONFIRMED);

    }

    private User getUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Event getEvent(User user, EventState state) {
        Event event = new Event();
        event.setId(2L);
        event.setInitiator(user);
        event.setParticipantLimit(2);
        event.setState(state);
        return event;
    }

}
