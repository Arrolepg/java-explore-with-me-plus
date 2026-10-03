package ru.practicum.stats.server.util;

import lombok.experimental.UtilityClass;
import ru.practicum.stats.dto.request.EndpointHitDto;
import ru.practicum.stats.server.model.Stats;

@UtilityClass
public class StatsMapper {

    public Stats toStats(EndpointHitDto dto) {
        return Stats.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
    }

    public EndpointHitDto toEndpointHitDto(Stats stats) {
        return EndpointHitDto.builder()
                .id(stats.getId())
                .app(stats.getApp())
                .uri(stats.getUri())
                .ip(stats.getIp())
                .timestamp(stats.getTimestamp())
                .build();
    }
}
