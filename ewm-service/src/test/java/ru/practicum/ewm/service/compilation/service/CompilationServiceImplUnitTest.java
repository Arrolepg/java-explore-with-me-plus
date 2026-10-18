package ru.practicum.ewm.service.compilation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.compilation.dto.CompilationDto;
import ru.practicum.ewm.service.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.service.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.service.compilation.model.Compilation;
import ru.practicum.ewm.service.compilation.repository.CompilationRepository;
import ru.practicum.ewm.service.compilation.utility.CompilationPaginationParams;
import ru.practicum.ewm.service.event.dto.response.EventShortDto;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.query.EventQuery;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.request.query.RequestQuery;
import ru.practicum.ewm.service.user.model.User;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompilationServiceImplUnitTest {

    @InjectMocks
    CompilationServiceImpl compService;

    @Mock
    private CompilationRepository compRepository;

    @Mock
    private RequestQuery requestQuery;

    @Mock
    private EventQuery eventQuery;

    @Test
    void findAllReturnsEmptyListWhenNoCompilations() {
        CompilationPaginationParams params = getParams(3, 5);

        when(compRepository.findCompilations(null, params))
                .thenReturn(List.of());

        assertThat(compService.findAll(null, params))
                .isEmpty();

        verifyNoInteractions(requestQuery);
    }

    @Test
    void findAllReturnsCompilationWithEmptyListEvents() {
        Compilation compilation = new Compilation(1L, false, "Летние концерты", List.of());
        CompilationPaginationParams params = getParams(0, 10);

        when(compRepository.findCompilations(false, params))
                .thenReturn(List.of(compilation));

        when(compRepository.findAllWithEventsByIdIn(List.of(1L)))
                .thenReturn(List.of(compilation));

        List<CompilationDto> compilationsDto = compService.findAll(false, params);

        assertThat(compilationsDto)
                .hasSize(1);
        assertThat(compilationsDto.getFirst().getEvents())
                .isEmpty();

        verifyNoInteractions(requestQuery);
    }

    @Test
    void findAllReturnsCompilationWithListEvents() {
        Event event1 = getEvent(1L);
        Event event2 = getEvent(2L);
        Event event3 = getEvent(3L);
        Compilation compilation1 = new Compilation(1L, false, "Спектакли", List.of(event2, event3));
        Compilation compilation2 = new Compilation(2L, true, "Летние концерты", List.of(event1, event3));
        CompilationPaginationParams params = getParams(0, 10);


        when(compRepository.findCompilations(null, params))
                .thenReturn(List.of(compilation1, compilation2));

        when(compRepository.findAllWithEventsByIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(compilation1, compilation2));

        when(eventQuery.getEventsDto(List.of(event2, event3, event1)))
                .thenReturn(List.of(
                        EventShortDto.builder().id(1L).build(),
                        EventShortDto.builder().id(2L).build(),
                        EventShortDto.builder().id(3L).build()
                ));

        List<CompilationDto> compilationsDto = compService.findAll(null, params);
        assertThat(compilationsDto).hasSize(2);

        CompilationDto dto1 = compilationsDto.getFirst();
        CompilationDto dto2 = compilationsDto.getLast();

        assertAll(
                () -> assertThat(dto1.getId()).isEqualTo(1L),
                () -> assertThat(dto1.getEvents()).hasSize(2),
                () -> assertThat(dto1.getEvents())
                        .extracting(EventShortDto::getId)
                        .containsExactlyInAnyOrder(2L, 3L)
        );

        assertAll(
                () -> assertThat(dto2.getId()).isEqualTo(2L),
                () -> assertThat(dto2.getEvents()).hasSize(2),
                () -> assertThat(dto2.getEvents())
                        .extracting(EventShortDto::getId)
                        .containsExactlyInAnyOrder(1L, 3L)
        );

    }

    @Test
    void findByIdThrowsNotFound() {
        when(compRepository.findByIdWithEvents(3L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> compService.findById(3L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateCompilationPinnedShouldBeTrue() {
        Event event2 = getEvent(2L);
        Event event3 = getEvent(3L);
        Compilation compilation1 = new Compilation(1L, false, "Спектакли", List.of(event2, event3));
        UpdateCompilationRequest dto = new UpdateCompilationRequest();
        dto.setPinned(true);

        when(compRepository.findByIdWithEvents(1L))
                .thenReturn(Optional.of(compilation1));


        CompilationDto compDto = compService.update(1L, dto);

        assertAll(
                () -> assertThat(compDto.getId()).isEqualTo(1L),
                () -> assertThat(compDto.getPinned()).isTrue(),
                () -> assertThat(compDto.getTitle()).isEqualTo("Спектакли")
        );

    }

    @Test
    void createReturnsCompilationWithoutEventsAndWithPinnedFalse() {
        NewCompilationDto dto = new NewCompilationDto();
        dto.setTitle("Летние концерты");

        when(compRepository.save(any(Compilation.class)))
                .thenAnswer(invocation -> {
                    Compilation compilation = invocation.getArgument(0);
                    compilation.setId(1L);
                    return compilation;
                });

        CompilationDto result = compService.create(dto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Летние концерты");
        assertThat(result.getPinned()).isFalse();
        assertThat(result.getEvents()).isEmpty();

        verify(compRepository).save(any(Compilation.class));
        verifyNoInteractions(eventQuery, requestQuery);
    }

    @Test
    void createThrowsNotFoundWhenSomeEventsAreMissing() {
        NewCompilationDto dto = new NewCompilationDto();
        dto.setTitle("Спектакли");
        dto.setEvents(List.of(1L, 2L));

        Event event = Event.builder()
                .id(1L)
                .build();

        when(eventQuery.findAllByIdWithFetch(List.of(1L, 2L)))
                .thenReturn(List.of(event));

        assertThatThrownBy(() -> compService.create(dto))
                .isInstanceOf(NotFoundException.class);

        verify(compRepository, never()).save(any(Compilation.class));
        verifyNoInteractions(requestQuery);
    }

    private Event getEvent(Long id) {
        Category category = new Category();
        category.setName("name");

        User user = new User();
        user.setName("user");
        user.setEmail("user@user.user");

        Event event = new Event();
        event.setId(id);
        event.setInitiator(user);
        event.setCategory(category);
        return event;
    }

    private CompilationPaginationParams getParams(Integer from, Integer size) {
        return CompilationPaginationParams.builder()
                .from(from)
                .size(size)
                .build();
    }

}
