package ru.practicum.ewm.service.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.category.dto.request.CategoryCreateDto;
import ru.practicum.ewm.service.category.dto.request.CategoryUpdateDto;
import ru.practicum.ewm.service.category.dto.response.CategoryResponseDto;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.category.query.CategoryQuery;
import ru.practicum.ewm.service.category.repository.CategoryRepository;
import ru.practicum.ewm.service.category.utility.CategoryMapper;
import ru.practicum.ewm.service.category.utility.CategoryPaginationParams;
import ru.practicum.ewm.service.event.model.Event_;
import ru.practicum.ewm.service.event.query.EventQuery;
import ru.practicum.ewm.service.exception.ConflictException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryQuery categoryQuery;
    private final EventQuery eventQuery;
    private static final Sort SORT_BY_ID_ASC = Sort.by(Sort.Direction.ASC, Event_.ID);


    @Override
    public CategoryResponseDto getById(Long id) {
        Category category = categoryQuery.findCategory(id);

        return CategoryMapper.toCategoryResponseDto(category);
    }

    @Override
    @Transactional
    public CategoryResponseDto create(CategoryCreateDto createDto) {
        checkForCreate(createDto);
        Category category = CategoryMapper.toCategory(createDto);

        return CategoryMapper.toCategoryResponseDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponseDto update(Long id, CategoryUpdateDto updateDto) {
        Category category = categoryQuery.findCategory(id);
        applyUpdates(category, updateDto);

        return CategoryMapper.toCategoryResponseDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        checkForDelete(id);

        categoryRepository.deleteById(id);
    }

    @Override
    public List<CategoryResponseDto> getAll(CategoryPaginationParams paginationParams) {
        Pageable pageable = toPageable(paginationParams);
        List<Category> categories = categoryRepository.findAllList(pageable);

        return CategoryMapper.toListCategoryResponseDto(categories);
    }

    private void checkForCreate(CategoryCreateDto createDto) {
        if (categoryRepository.existsCategoryByName(createDto.getName())) {
            throw new ConflictException("Категория с названием " + createDto.getName() + " уже существует");
        }
    }

    private void checkForUpdate(String name, Long id) {
        if (categoryRepository.existsCategoryByNameAndIdNot(name, id)) {
            throw new ConflictException("Категория с названием " + name + " уже существует");
        }
    }

    private void applyUpdates(Category category, CategoryUpdateDto updateDto) {
        if (updateDto.getName() != null) {
            checkForUpdate(updateDto.getName(), category.getId());
            category.setName(updateDto.getName());
        }
    }

    private void checkForDelete(Long id) {
        categoryQuery.findCategory(id);

        if (eventQuery.existsByCategoryId(id)) {
            throw new ConflictException("Категория не может быть удалена, так как с ней связано событие");
        }
    }

    private Pageable toPageable(CategoryPaginationParams paginationParams) {
        return PageRequest.of(
                paginationParams.from() / paginationParams.size(),
                paginationParams.size(),
                SORT_BY_ID_ASC
        );
    }
}
