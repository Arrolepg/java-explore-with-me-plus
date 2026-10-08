package ru.practicum.ewm.service.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "\"user\"")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String name;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;

        return id != null && id.equals(((User) o).getId());
    }

    @Override
    public final int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }
}
