package ru.practicum.ewm.service.request.command;

public interface RequestCommandService {
    RequestStatusUpdateResult updateRequestsStatuses(RequestStatusUpdateCommand command);
}
