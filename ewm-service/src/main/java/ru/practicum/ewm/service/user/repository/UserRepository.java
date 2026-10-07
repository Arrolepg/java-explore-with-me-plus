package ru.practicum.ewm.service.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.service.user.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
