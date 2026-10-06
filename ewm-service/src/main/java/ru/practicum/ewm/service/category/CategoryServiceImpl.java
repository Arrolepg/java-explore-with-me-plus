package ru.practicum.ewm.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.service.exception.NotFoundException;

import java.util.Collection;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;


    @Override
    public CategoryDto get(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Категория с id = %d не найдена", id)));
        return CategoryMapper.categoryToCategoryDto(category);
    }

    @Override
    @Transactional
    public CategoryDto create(CategoryDto categoryDto) {
        Category newCategory = CategoryMapper.categoryDtoToCategory(categoryDto);
        return CategoryMapper.categoryToCategoryDto(categoryRepository.save(newCategory));
    }

    @Override
    @Transactional
    public CategoryDto update(CategoryDto categoryDto, Long id) {
        Category updatedCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Категория с id = %d не найдена", id)));
        if (categoryDto.getName() != null) {
            updatedCategory.setName(categoryDto.getName());
        }
        return CategoryMapper.categoryToCategoryDto(categoryRepository.save(updatedCategory));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }

    @Override
    public Collection<CategoryDto> getAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryMapper::categoryToCategoryDto)
                .toList();
    }
}
