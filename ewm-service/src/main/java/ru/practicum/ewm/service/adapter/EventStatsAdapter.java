package ru.practicum.ewm.service.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.client.exception.StatsClientException;
import ru.practicum.stats.dto.parameter.ParamDto;
import ru.practicum.stats.dto.request.EndpointHitDto;
import ru.practicum.stats.dto.response.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventStatsAdapter {
    private static final String SERVICE_NAME = "ewm-service";
    private static final String EVENT_URI_PREFIX = "/events/";
    private static final LocalDateTime MIN_DATE = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final boolean DEFAULT_UNIQUE_VIEWS = true;

    private final StatsClient statsClient;

    public void createHit(String uri, String ip) {
        try {
            statsClient.createHit(EndpointHitDto.builder()
                    .app(SERVICE_NAME)
                    .uri(uri)
                    .ip(ip)
                    .timestamp(LocalDateTime.now())
                    .build()
            );
        } catch (StatsClientException e) {
            log.warn("Не удалось осуществить запись hit {}: status = {}, message = {}",
                    uri, e.getStatus(), e.getMessage());
        }
    }

    public Map<Long, Long> getViews(List<Long> eventsIds) {
        return getViews(eventsIds, DEFAULT_UNIQUE_VIEWS);
    }

    public Map<Long, Long> getViews(List<Long> eventsIds, boolean unique) {
        if (eventsIds == null || eventsIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> uris = eventsIds.stream()
                .map(id -> EVENT_URI_PREFIX + id)
                .toList();

        try {
            ParamDto paramDto = ParamDto.builder()
                    .start(MIN_DATE)
                    .end(LocalDateTime.now())
                    .uris(uris)
                    .unique(unique)
                    .build();

            List<ViewStatsDto> stats = statsClient.get(paramDto).getBody();
            if (stats == null || stats.isEmpty()) {
                return Collections.emptyMap();
            }

            return stats.stream()
                    .filter(stat -> stat.getUri().startsWith(EVENT_URI_PREFIX))
                    .collect(Collectors.toMap(
                            stat -> Long.parseLong(stat.getUri().substring(EVENT_URI_PREFIX.length())),
                            ViewStatsDto::getHits,
                            Long::sum
                    ));
        } catch (StatsClientException e) {
            log.warn("Не удалось получить статистику: status = {}, message = {}",
                    e.getStatus(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    public Long getViews(Long eventId) {
        return getViews(eventId, DEFAULT_UNIQUE_VIEWS);
    }

    public Long getViews(Long eventId, boolean unique) {
        if (eventId == null) {
            return 0L;
        }
        return getViews(List.of(eventId), unique).getOrDefault(eventId, 0L);
    }
}
