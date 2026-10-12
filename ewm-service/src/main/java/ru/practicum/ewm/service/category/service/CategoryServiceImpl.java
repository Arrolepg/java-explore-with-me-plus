package ru.practicum.ewm.service.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.category.dto.request.CategoryCreateDto;
import ru.practicum.ewm.service.category.dto.response.CategoryResponseDto;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.repository.CategoryRepository;
import ru.practicum.ewm.service.category.utility.CategoryMapper;
import ru.practicum.ewm.service.category.utility.PublicCategorySearchRequest;
import ru.practicum.ewm.service.event.model.Event_;
import ru.practicum.ewm.service.event.repository.EventRepository;
import ru.practicum.ewm.service.exception.BadRequestException;
import ru.practicum.ewm.service.exception.ConflictException;
import ru.practicum.ewm.service.exception.NotFoundException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private static final Sort SORT_BY_ID_ASC = Sort.by(Sort.Direction.ASC, Event_.ID);


    @Override
    public CategoryResponseDto get(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Категория с id = %d не найдена", id)));
        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    @Transactional
    public CategoryResponseDto create(CategoryCreateDto categoryCreateDto) {
        checkForUpdate(categoryCreateDto);
        Category newCategory = CategoryMapper.toCategory(categoryCreateDto);

        try {
            Category savedCategory = categoryRepository.saveAndFlush(newCategory);
            return CategoryMapper.toCategoryDto(savedCategory);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ConflictException(String.format("Категория с названием '%s' уже существует", categoryCreateDto.getName()));
        }
    }

    @Override
    @Transactional
    public CategoryResponseDto update(CategoryCreateDto categoryCreateDto, Long id) {
        checkForUpdate(categoryCreateDto);

        Category updatedCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Категория с id = %d не найдена", id)));

        if (categoryCreateDto.getName() != null) {
            updatedCategory.setName(categoryCreateDto.getName());
        }
        try {
            Category savedCategory = categoryRepository.saveAndFlush(updatedCategory);
            return CategoryMapper.toCategoryDto(savedCategory);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ConflictException(String.format("Категория с названием '%s' уже существует", categoryCreateDto.getName()));
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Категория с id = %d не найдена", id)));

        if (eventRepository.existsByCategoryId(id)) {
            throw new ConflictException(String.format("Категория с id = %d не может быть удалена, так как с ней связаны события", id));
        }

        categoryRepository.deleteById(id);
    }

    @Override
    public List<CategoryResponseDto> getAll(PublicCategorySearchRequest publicCategorySearchRequest) {
        Pageable pageable = toPageable(publicCategorySearchRequest);
        return categoryRepository.findAll(pageable).stream()
                .map(CategoryMapper::toCategoryDto)
                .toList();
    }

    private void checkForUpdate(CategoryCreateDto categoryCreateDto) {

        if (categoryCreateDto.getName() == null) {
            throw new BadRequestException("Наименование категории не указано");
        }


        if (categoryCreateDto.getName().length() > 50) {
            throw new BadRequestException("Наименование категории не может превышать 50 символов");
        }

        if (categoryCreateDto.getName().isBlank()) {
            throw new BadRequestException("Наименование категории не может состоять из пробелов");
        }

    }

    private Pageable toPageable(PublicCategorySearchRequest publicCategorySearchRequest) {
        return toPageable(publicCategorySearchRequest.from(), publicCategorySearchRequest.size());
    }

    private Pageable toPageable(int from, int size) {
        return PageRequest.of(from / size, size, SORT_BY_ID_ASC);
    }

}
