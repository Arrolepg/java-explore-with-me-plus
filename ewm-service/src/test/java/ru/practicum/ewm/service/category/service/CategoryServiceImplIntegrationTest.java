package ru.practicum.ewm.service.category.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.category.dto.request.CategoryCreateDto;
import ru.practicum.ewm.service.category.dto.request.CategoryUpdateDto;
import ru.practicum.ewm.service.category.dto.response.CategoryResponseDto;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.repository.CategoryRepository;
import ru.practicum.ewm.service.category.utility.CategoryPaginationParams;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.event.model.EventLocation;
import ru.practicum.ewm.service.event.model.EventState;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;
import ru.practicum.ewm.service.user.model.User;
import ru.practicum.ewm.service.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CategoryServiceImplIntegrationTest {
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private CategoryResponseDto first;
    private CategoryResponseDto second;
    private CategoryResponseDto third;

    @BeforeEach
    void setUp() {
        first = categoryService.create(createCreateDto("Concerts"));
        second = categoryService.create(createCreateDto("Exhibitions"));
        third = categoryService.create(createCreateDto("Theatre"));
    }

    @Test
    void testGetById() {
        CategoryResponseDto result = categoryService.getById(second.getId());

        assertThat(result.getId()).isEqualTo(second.getId());
        assertThat(result.getName()).isEqualTo("Exhibitions");
    }

    @Test
    void testGetByIdNotFound() {
        assertThatThrownBy(() -> categoryService.getById(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testCreate() {
        CategoryResponseDto result = categoryService.create(createCreateDto("Cinema"));

        assertThat(result.getId()).isNotNull();
        assertThat(categoryRepository.findById(result.getId()))
                .get()
                .extracting(Category::getName)
                .isEqualTo("Cinema");
    }

    @Test
    void testCreateWithDuplicateNameThrowsConflict() {
        assertThatThrownBy(() -> categoryService.create(createCreateDto("Concerts")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testUpdate() {
        CategoryResponseDto result = categoryService.update(first.getId(), createUpdateDto("Festivals"));

        assertThat(result.getId()).isEqualTo(first.getId());
        assertThat(result.getName()).isEqualTo("Festivals");
        assertThat(categoryRepository.findById(first.getId()))
                .get()
                .extracting(Category::getName)
                .isEqualTo("Festivals");
    }

    @Test
    void testUpdateWithSameName() {
        CategoryResponseDto result = categoryService.update(first.getId(), createUpdateDto("Concerts"));

        assertThat(result.getName()).isEqualTo("Concerts");
    }

    @Test
    void testUpdateWithDuplicateNameThrowsConflict() {
        assertThatThrownBy(() -> categoryService.update(first.getId(), createUpdateDto("Theatre")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testUpdateNotFound() {
        assertThatThrownBy(() -> categoryService.update(999L, createUpdateDto("Festivals")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testDelete() {
        categoryService.delete(third.getId());

        assertThat(categoryRepository.existsById(third.getId())).isFalse();
    }

    @Test
    void testDeleteNotFound() {
        assertThatThrownBy(() -> categoryService.delete(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testDeleteWithEventsThrowsConflict() {
        createEvent(categoryRepository.findById(first.getId()).orElseThrow());

        assertThatThrownBy(() -> categoryService.delete(first.getId()))
                .isInstanceOf(ConflictException.class);
        assertThat(categoryRepository.existsById(first.getId())).isTrue();
    }

    @Test
    void testGetAll() {
        List<CategoryResponseDto> result = categoryService.getAll(new CategoryPaginationParams(0, 10));

        assertThat(result).extracting(CategoryResponseDto::getId)
                .containsExactly(first.getId(), second.getId(), third.getId());
    }

    @Test
    void testGetAllWithPagination() {
        List<CategoryResponseDto> result = categoryService.getAll(new CategoryPaginationParams(2, 2));

        assertThat(result).extracting(CategoryResponseDto::getId).containsExactly(third.getId());
    }

    private CategoryCreateDto createCreateDto(String name) {
        CategoryCreateDto createDto = new CategoryCreateDto();
        createDto.setName(name);
        return createDto;
    }

    private CategoryUpdateDto createUpdateDto(String name) {
        CategoryUpdateDto updateDto = new CategoryUpdateDto();
        updateDto.setName(name);
        return updateDto;
    }

    private void createEvent(Category category) {
        User initiator = new User();
        initiator.setName("Owner");
        initiator.setEmail("owner@mail.com");
        initiator = userRepository.save(initiator);

        Event event = Event.builder()
                .annotation("Some annotation text here for event")
                .category(category)
                .description("Some description text here for event")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventLocation(EventLocation.builder().lat(55.75f).lon(37.62f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title("Title")
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .initiator(initiator)
                .build();
        eventRepository.save(event);
    }
}
