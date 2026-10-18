package ru.practicum.ewm.service.compilation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.service.compilation.model.Compilation;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long>, CompilationSearchRepository {
    @Query("""
    select distinct c
    from Compilation c
    left join fetch c.events e
    left join fetch e.category
    left join fetch e.initiator
    where c.id = :id
    """)
    Optional<Compilation> findByIdWithEvents(@Param("id") Long id);

    @Query("""
        select distinct c
        from Compilation c
        left join fetch c.events e
        left join fetch e.category
        left join fetch e.initiator
        where c.id in :ids
        order by c.id
        """)
    List<Compilation> findAllWithEventsByIdIn(@Param("ids") List<Long> ids);
}
