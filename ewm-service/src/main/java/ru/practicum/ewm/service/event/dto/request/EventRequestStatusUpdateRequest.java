package ru.practicum.ewm.service.event.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.UniqueElements;
import ru.practicum.ewm.service.request.model.RequestStatus;
import ru.practicum.ewm.service.validation.RequestStatusEnumSubset;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {
    @NotEmpty(message = "Хотя бы одно Id запроса должно быть указано")
    @UniqueElements(message = "Id запросов должны быть уникальными")
    private List<Long> requestIds;

    @NotNull(message = "Требуемый статус должен быть указан")
    @RequestStatusEnumSubset(anyOf = {RequestStatus.CONFIRMED, RequestStatus.REJECTED})
    private RequestStatus status;
}
