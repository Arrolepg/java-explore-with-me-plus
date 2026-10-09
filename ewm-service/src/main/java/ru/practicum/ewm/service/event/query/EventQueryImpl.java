package ru.practicum.ewm.service.event.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.exception.NotFoundException;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryImpl implements EventQuery {
    private final EventRepository eventRepository;

    @Override
    public Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(
                        () -> new NotFoundException("Событие с id = " + eventId + " не найдено")
                );
    }
}
