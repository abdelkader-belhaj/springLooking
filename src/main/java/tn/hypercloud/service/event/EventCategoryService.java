package tn.hypercloud.service.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.dto.event.EventCategoryRequest;
import tn.hypercloud.dto.event.EventCategoryResponse;
import tn.hypercloud.entity.event.EventCategory;
import tn.hypercloud.repository.event.EventCategoryRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventCategoryService {

    private final EventCategoryRepository repository;

    public List<EventCategoryResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public EventCategoryResponse getById(Integer id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Category not found : " + id)));
    }

    public List<EventCategoryResponse> getByType(String type) {
        return repository
                .findByType(EventCategory.CategoryType.valueOf(type))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public EventCategoryResponse create(
            EventCategoryRequest request) {
        if (repository.existsByName(request.getName())) {
            throw new RuntimeException(
                    "Category already exists : " + request.getName());
        }
        return toResponse(repository.save(toEntity(request)));
    }

    public EventCategoryResponse update(
            Integer id, EventCategoryRequest request) {
        EventCategory category = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Category not found : " + id));

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setType(EventCategory.CategoryType
                .valueOf(request.getType()));

        return toResponse(repository.save(category));
    }

    public void delete(Integer id) {
        repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Category not found : " + id));
        repository.deleteById(id);
    }

    private EventCategory toEntity(EventCategoryRequest request) {
        return EventCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(EventCategory.CategoryType
                        .valueOf(request.getType()))
                .build();
    }

    private EventCategoryResponse toResponse(EventCategory c) {
        return EventCategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .type(c.getType() != null ?
                        c.getType().name() : null)
                .build();
    }
}