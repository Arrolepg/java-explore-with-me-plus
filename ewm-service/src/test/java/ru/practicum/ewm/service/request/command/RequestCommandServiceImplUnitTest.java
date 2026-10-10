package ru.practicum.ewm.service.request.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.request.query.RequestQuery;
import ru.practicum.ewm.service.request.repository.RequestRepository;
import ru.practicum.ewm.service.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RequestCommandServiceImplUnitTest {
    private static final Long EVENT_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final Long REQUEST_ID = 10L;
    private static final Long REQUEST_ID_2 = 20L;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private RequestQuery requestQuery;

    @InjectMocks
    private RequestCommandServiceImpl requestCommandServiceImpl;

    @Test
    void testUpdateRequestsStatusesConfirmed() {
        Request request = createRequest(REQUEST_ID, RequestStatus.PENDING);
        when(requestRepository.findRequestsByIdInAndEventId(List.of(REQUEST_ID), EVENT_ID))
                .thenReturn(List.of(request));
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED))
                .thenReturn(0L);

        RequestStatusUpdateCommand command = new RequestStatusUpdateCommand(
                EVENT_ID, List.of(REQUEST_ID), RequestStatus.CONFIRMED, 10);

        RequestStatusUpdateResult result = requestCommandServiceImpl.updateRequestsStatuses(command);

        assertThat(result.confirmedRequests()).hasSize(1);
        assertThat(result.rejectedRequests()).isEmpty();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CONFIRMED);

        verify(requestRepository, times(1)).findRequestsByIdInAndEventId(List.of(REQUEST_ID),
                EVENT_ID);
        verify(requestQuery, times(1)).countRequestsByEventIdAndStatus(EVENT_ID,
                RequestStatus.CONFIRMED);
        verify(requestRepository, never()).findRequestsByEventIdAndStatus(any(), any());
    }

    @Test
    void testUpdateRequestsStatusesRejected() {
        Request request = createRequest(REQUEST_ID, RequestStatus.PENDING);
        when(requestRepository.findRequestsByIdInAndEventId(List.of(REQUEST_ID), EVENT_ID))
                .thenReturn(List.of(request));

        RequestStatusUpdateCommand command = new RequestStatusUpdateCommand(
                EVENT_ID, List.of(REQUEST_ID), RequestStatus.REJECTED, 10);

        RequestStatusUpdateResult result = requestCommandServiceImpl.updateRequestsStatuses(command);

        assertThat(result.rejectedRequests()).hasSize(1);
        assertThat(result.confirmedRequests()).isEmpty();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);

        verify(requestRepository, times(1)).findRequestsByIdInAndEventId(List.of(REQUEST_ID),
                EVENT_ID);
        verify(requestQuery, never()).countRequestsByEventIdAndStatus(any(), any());
    }

    @Test
    void testUpdateRequestsStatusesRequestNotFound() {
        when(requestRepository.findRequestsByIdInAndEventId(List.of(REQUEST_ID), EVENT_ID))
                .thenReturn(List.of());

        RequestStatusUpdateCommand command = new RequestStatusUpdateCommand(
                EVENT_ID, List.of(REQUEST_ID), RequestStatus.CONFIRMED, 10);

        assertThatThrownBy(() -> requestCommandServiceImpl.updateRequestsStatuses(command))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testUpdateRequestsStatusesNotPending() {
        Request request = createRequest(REQUEST_ID, RequestStatus.CONFIRMED);
        when(requestRepository.findRequestsByIdInAndEventId(List.of(REQUEST_ID), EVENT_ID))
                .thenReturn(List.of(request));

        RequestStatusUpdateCommand command = new RequestStatusUpdateCommand(
                EVENT_ID, List.of(REQUEST_ID), RequestStatus.CONFIRMED, 10);

        assertThatThrownBy(() -> requestCommandServiceImpl.updateRequestsStatuses(command))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testUpdateRequestsStatusesLimitReached() {
        Request request = createRequest(REQUEST_ID, RequestStatus.PENDING);
        when(requestRepository.findRequestsByIdInAndEventId(List.of(REQUEST_ID), EVENT_ID))
                .thenReturn(List.of(request));
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED))
                .thenReturn(5L);

        RequestStatusUpdateCommand command = new RequestStatusUpdateCommand(
                EVENT_ID, List.of(REQUEST_ID), RequestStatus.CONFIRMED, 5);

        assertThatThrownBy(() -> requestCommandServiceImpl.updateRequestsStatuses(command))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testUpdateRequestsStatusesRejectsRemainingWhenLimitReached() {
        Request confirmedReq = createRequest(REQUEST_ID, RequestStatus.PENDING);
        Request pendingReq = createRequest(REQUEST_ID_2, RequestStatus.PENDING);

        when(requestRepository.findRequestsByIdInAndEventId(List.of(REQUEST_ID), EVENT_ID))
                .thenReturn(List.of(confirmedReq));
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED))
                .thenReturn(4L);
        when(requestRepository.findRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.PENDING))
                .thenReturn(List.of(pendingReq));

        RequestStatusUpdateCommand command = new RequestStatusUpdateCommand(
                EVENT_ID, List.of(REQUEST_ID), RequestStatus.CONFIRMED, 5);

        RequestStatusUpdateResult result = requestCommandServiceImpl.updateRequestsStatuses(command);

        assertThat(confirmedReq.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
        assertThat(pendingReq.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(result.confirmedRequests()).hasSize(1);
        assertThat(result.rejectedRequests()).hasSize(1);
    }

    @Test
    void testUpdateRequestsStatusesNoLimit() {
        Request request = createRequest(REQUEST_ID, RequestStatus.PENDING);
        when(requestRepository.findRequestsByIdInAndEventId(List.of(REQUEST_ID), EVENT_ID))
                .thenReturn(List.of(request));

        RequestStatusUpdateCommand command = new RequestStatusUpdateCommand(
                EVENT_ID, List.of(REQUEST_ID), RequestStatus.CONFIRMED, 0);

        RequestStatusUpdateResult result = requestCommandServiceImpl.updateRequestsStatuses(command);

        assertThat(result.confirmedRequests()).hasSize(1);
        assertThat(result.rejectedRequests()).isEmpty();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CONFIRMED);

        verify(requestRepository, never()).findRequestsByEventIdAndStatus(any(), any());
    }

    @Test
    void testUpdateRequestsStatusesRejectsExcessWhenLimitReachedMidProcess() {
        Request first = createRequest(REQUEST_ID, RequestStatus.PENDING);
        Request second = createRequest(REQUEST_ID_2, RequestStatus.PENDING);

        when(requestRepository.findRequestsByIdInAndEventId(
                List.of(REQUEST_ID, REQUEST_ID_2), EVENT_ID))
                .thenReturn(List.of(first, second));
        when(requestQuery.countRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED))
                .thenReturn(1L);
        when(requestRepository.findRequestsByEventIdAndStatus(EVENT_ID, RequestStatus.PENDING))
                .thenReturn(List.of());

        RequestStatusUpdateCommand command = new RequestStatusUpdateCommand(
                EVENT_ID,
                List.of(REQUEST_ID, REQUEST_ID_2),
                RequestStatus.CONFIRMED,
                2);

        RequestStatusUpdateResult result = requestCommandServiceImpl.updateRequestsStatuses(command);

        assertThat(first.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
        assertThat(second.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(result.confirmedRequests()).hasSize(1);
        assertThat(result.rejectedRequests()).hasSize(1);
        assertThat(result.rejectedRequests().getFirst().getId()).isEqualTo(REQUEST_ID_2);
    }

    private Request createRequest(Long id, RequestStatus status) {
        User requester = new User();
        requester.setId(USER_ID);

        Event event = new Event();
        event.setId(EVENT_ID);

        Request request = new Request();
        request.setId(id);
        request.setEvent(event);
        request.setRequester(requester);
        request.setStatus(status);
        request.setCreated(LocalDateTime.now());
        return request;
    }
}
