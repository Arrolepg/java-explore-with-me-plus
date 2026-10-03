package ru.practicum.stats.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.stats.dto.response.ViewStatsDto;
import ru.practicum.stats.server.model.Stats;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatsRepository extends JpaRepository<Stats, Long> {

    @Query("""
            SELECT new ru.practicum.stats.dto.response.ViewStatsDto(s.app, s.uri, COUNT(s.ip))
            FROM Stats s
            WHERE s.timestamp BETWEEN :start AND :end
            GROUP BY s.app, s.uri
            ORDER BY COUNT(s.ip) DESC
            """)
    List<ViewStatsDto> findAllStats(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    @Query("""
            SELECT new ru.practicum.stats.dto.response.ViewStatsDto(s.app, s.uri, COUNT(s.ip))
            FROM Stats s
            WHERE s.timestamp BETWEEN :start AND :end
              AND s.uri IN :uris
            GROUP BY s.app, s.uri
            ORDER BY COUNT(s.ip) DESC
            """)
    List<ViewStatsDto> findAllStatsByUris(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("uris") List<String> uris);

    @Query("""
            SELECT new ru.practicum.stats.dto.response.ViewStatsDto(s.app, s.uri, COUNT(DISTINCT s.ip))
            FROM Stats s
            WHERE s.timestamp BETWEEN :start AND :end
            GROUP BY s.app, s.uri
            ORDER BY COUNT(DISTINCT s.ip) DESC
            """)
    List<ViewStatsDto> findUniqueStats(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    @Query("""
            SELECT new ru.practicum.stats.dto.response.ViewStatsDto(s.app, s.uri, COUNT(DISTINCT s.ip))
            FROM Stats s
            WHERE s.timestamp BETWEEN :start AND :end
              AND s.uri IN :uris
            GROUP BY s.app, s.uri
            ORDER BY COUNT(DISTINCT s.ip) DESC
            """)
    List<ViewStatsDto> findUniqueStatsByUris(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end,
                                             @Param("uris") List<String> uris);
}
