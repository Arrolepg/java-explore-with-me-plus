package ru.practicum.stats.server.service;

import ru.practicum.stats.dto.parameter.ParamDto;
import ru.practicum.stats.dto.request.EndpointHitDto;
import ru.practicum.stats.dto.response.ViewStatsDto;

import java.util.List;

public interface StatsService {

    EndpointHitDto saveHit(EndpointHitDto dto);
    List<ViewStatsDto> getStats(ParamDto params);
}
