package ru.practicum.ewm.service.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.compilation.dto.CompilationDto;
import ru.practicum.ewm.service.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.service.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.service.compilation.model.Compilation;
import ru.practicum.ewm.service.compilation.repository.CompilationRepository;
import ru.practicum.ewm.service.compilation.utility.CompilationMapper;
import ru.practicum.ewm.service.compilation.utility.CompilationPaginationParams;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.query.EventQuery;
import ru.practicum.ewm.service.exception.NotFoundException;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventQuery eventQuery;

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto dto) {
        List<Event> events = new ArrayList<>();
        List<EventShortDto> eventsDto = new ArrayList<>();

        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            events = eventQuery.findAllByIdWithFetch(dto.getEvents());
            if (!new HashSet<>(getEventsIds(events)).equals(new HashSet<>(dto.getEvents()))) {
                throw new NotFoundException("Одно или несколько событий не найдены");
            }
            eventsDto = eventQuery.getEventsDto(events);
        }

        Compilation compilation = compilationRepository.save(CompilationMapper.toCompilation(dto, events));
        CompilationDto compDto = CompilationMapper.toCompilationDto(compilation,eventsDto);
        log.info("Создана подборка с id = {}", compilation.getId());
        return compDto;
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest dto) {
        Compilation compilation = findCompilationWithEvents(compId);

        if (dto.getEvents() != null) {
            List<Event> eventsNew = eventQuery.findAllByIdWithFetch(dto.getEvents());
            if (!new HashSet<>(getEventsIds(eventsNew)).equals(new HashSet<>(dto.getEvents()))) {
                throw new NotFoundException("Одно или несколько событий не найдены");
            }
            compilation.setEvents(eventsNew);
        }
        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }
        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }
        compilationRepository.save(compilation);

        List<EventShortDto> eventsDto = eventQuery.getEventsDto(compilation.getEvents());
        CompilationDto compDto = CompilationMapper.toCompilationDto(compilation, eventsDto);

        log.info("Обновлена подборка с id = {}", compId);
        return compDto;
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        compilationRepository.delete(findCompilation(compId));
        log.info("Удалена подборка с id = {}", compId);
    }

    @Override
    public CompilationDto findById(Long compId) {
        Compilation compilation = findCompilationWithEvents(compId);
        List<EventShortDto> eventsDto = eventQuery.getEventsDto(compilation.getEvents());

        return CompilationMapper.toCompilationDto(compilation, eventsDto);
    }

    @Override
    public List<CompilationDto> findAll(Boolean pinned, CompilationPaginationParams params) {
        List<Compilation> compilations = compilationRepository.findCompilations(pinned, params);
        if (compilations.isEmpty()) {
            return List.of();
        }
        List<Long> compIds = compilations.stream()
                .map(Compilation::getId)
                .toList();
        compilations = compilationRepository.findAllWithEventsByIdIn(compIds);

        List<Event> events = compilations.stream()
                .flatMap(comp -> comp.getEvents().stream())
                .distinct()
                .toList();

        List<EventShortDto> eventsDto = eventQuery.getEventsDto(events);

        Map<Long, EventShortDto> eventsDtoMap = new HashMap<>();
        for (EventShortDto event : eventsDto) {
            eventsDtoMap.put(event.getId(), event);
        }

        List<CompilationDto> compDtoList = new ArrayList<>();
        for (Compilation comp : compilations) {
            List<EventShortDto> compEventsDto = comp.getEvents().stream()
                    .map(event -> eventsDtoMap.get(event.getId()))
                    .toList();

            compDtoList.add(CompilationMapper.toCompilationDto(comp, compEventsDto));
        }
        return compDtoList;
    }

    private Compilation findCompilationWithEvents(Long compId) {
        return compilationRepository.findByIdWithEvents(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
    }

    private Compilation findCompilation(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
    }

    private List<Long> getEventsIds(List<Event> events) {
        return events.stream()
                .map(Event::getId)
                .toList();
    }

}
