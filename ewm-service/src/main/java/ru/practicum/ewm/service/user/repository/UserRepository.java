package ru.practicum.ewm.service.user.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.service.user.model.User;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAllBy(Pageable pageable);

    List<User> findAllByIdIn(List<Long> ids, Pageable pageable);

    boolean existsByEmail(String email);
}
