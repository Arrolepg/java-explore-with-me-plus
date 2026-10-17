package ru.practicum.ewm.service.event.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.exception.NotFoundException;

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
}
