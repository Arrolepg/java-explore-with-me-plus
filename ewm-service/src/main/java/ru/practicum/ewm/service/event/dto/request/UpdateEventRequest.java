package ru.practicum.ewm.service.event.dto.request;

import java.time.LocalDateTime;

/**
 * Общие поля запросов на изменение события пользователем и администратором.
 * Если поле равно null, изменение этих данных не требуется.
 */
public interface UpdateEventRequest {
    String getAnnotation();

    Long getCategoryId();

    String getDescription();

    LocalDateTime getEventDate();

    EventLocationUpdateDto getLocation();

    Boolean getPaid();

    Integer getParticipantLimit();

    Boolean getRequestModeration();

    String getTitle();
}
