package ru.practicum.ewm.service.category.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;

        return id != null && id.equals(((Category) o).getId());
    }

    @Override
    public final int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }
}
