package ru.practicum.ewm.service.event.utility;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.utility.CategoryMapper;
import ru.practicum.ewm.service.event.dto.request.EventLocationCreateDto;
import ru.practicum.ewm.service.event.dto.request.NewEventDto;
import ru.practicum.ewm.service.event.dto.response.EventFullDto;
import ru.practicum.ewm.service.event.dto.response.EventLocationDto;
import ru.practicum.ewm.service.event.dto.response.EventRequestStatusUpdateResult;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.request.model.Request;
import ru.practicum.ewm.service.request.utility.RequestMapper;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.utility.UserMapper;

import java.util.List;
import java.util.Map;

@UtilityClass
public class EventMapper {
    public Event toEvent(Category category, User initiator, NewEventDto eventDto) {
        return Event.builder()
                .annotation(eventDto.getAnnotation())
                .category(category)
                .description(eventDto.getDescription())
                .eventDate(eventDto.getEventDate())
                .eventLocation(toEventLocation(eventDto.getLocation()))
                .paid(eventDto.getPaid())
                .participantLimit(eventDto.getParticipantLimit())
                .requestModeration(eventDto.getRequestModeration())
                .title(eventDto.getTitle())
                .initiator(initiator)
                .build();
    }

    public EventFullDto toEventFullDto(Event event) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                .location(toEventLocationDto(event.getEventLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .build();
    }

    public EventFullDto toEventFullDto(Event event, Long confirmedRequests, Long views) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                .location(toEventLocationDto(event.getEventLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public EventRequestStatusUpdateResult toEventRequestStatusUpdateResult(
            List<Request> confirmedRequests, List<Request> rejectedRequests) {
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(RequestMapper.toListParticipationRequestDto(confirmedRequests))
                .rejectedRequests(RequestMapper.toListParticipationRequestDto(rejectedRequests))
                .build();
    }

    public EventShortDto toEventShortDto(Event event, Long confirmedRequests, Long views) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public List<EventShortDto> toListEventShortDto(List<Event> events, Map<Long, Long> confirmedRequestsMap,
                                                   Map<Long, Long> viewsMap) {
        return events.stream()
                .map(event -> toEventShortDto(
                        event,
                        confirmedRequestsMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L)
                ))
                .toList();
    }

    private EventLocation toEventLocation(EventLocationCreateDto locationCreateDto) {
        return EventLocation.builder()
                .lat(locationCreateDto.getLat())
                .lon(locationCreateDto.getLon())
                .build();
    }

    private EventLocationDto toEventLocationDto(EventLocation eventLocation) {
        return EventLocationDto.builder()
                .lat(eventLocation.getLat())
                .lon(eventLocation.getLon())
                .build();
    }
}
