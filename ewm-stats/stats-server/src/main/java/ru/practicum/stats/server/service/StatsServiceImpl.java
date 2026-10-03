package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.parameter.ParamDto;
import ru.practicum.stats.dto.request.EndpointHitDto;
import ru.practicum.stats.dto.response.ViewStatsDto;
import ru.practicum.stats.server.exception.BadRequestException;
import ru.practicum.stats.server.model.Stats;
import ru.practicum.stats.server.repository.StatsRepository;
import ru.practicum.stats.server.util.StatsMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public EndpointHitDto saveHit(EndpointHitDto dto) {
        Stats saved = statsRepository.save(StatsMapper.toStats(dto));
        log.info("Сохранена информация о запросе: id={}, app={}, uri={}", saved.getId(), saved.getApp(), saved.getUri());
        return StatsMapper.toEndpointHitDto(saved);
    }

    @Override
    public List<ViewStatsDto> getStats(ParamDto params) {
        if (params.getStart().isAfter(params.getEnd())) {
            throw new BadRequestException("Дата начала диапазона (start) не может быть позже даты конца (end)");
        }

        boolean unique = Boolean.TRUE.equals(params.getUnique());
        List<String> uris = params.getUris();
        boolean withUris = uris != null && !uris.isEmpty();

        log.info("Запрос статистики: start={}, end={}, uris={}, unique={}",
                params.getStart(), params.getEnd(), uris, unique);

        if (unique) {
            return withUris
                    ? statsRepository.findUniqueStatsByUris(params.getStart(), params.getEnd(), uris)
                    : statsRepository.findUniqueStats(params.getStart(), params.getEnd());
        }
        return withUris
                ? statsRepository.findAllStatsByUris(params.getStart(), params.getEnd(), uris)
                : statsRepository.findAllStats(params.getStart(), params.getEnd());
    }
}
