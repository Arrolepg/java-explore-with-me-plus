package ru.practicum.stats.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.parameter.ParamDto;
import ru.practicum.stats.dto.request.EndpointHitDto;
import ru.practicum.stats.dto.response.ViewStatsDto;
import ru.practicum.stats.dto.util.PatternDataTime;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;


@RequiredArgsConstructor
public class StatsClient {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PatternDataTime.PATTERN);
    private final RestClient restClient;

    public ResponseEntity<Void> createHit(EndpointHitDto dto) {
        return restClient.post()
                .uri("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
    }

    public ResponseEntity<List<ViewStatsDto>> get(ParamDto dto) {

        URI uri = UriComponentsBuilder.newInstance()
                .path("/stats")
                .queryParam("start", dto.getStart().format(FORMATTER))
                .queryParam("end", dto.getEnd().format(FORMATTER))
                .queryParam("uris", dto.getUris())
                .queryParam("unique", dto.getUnique())
                .build()
                .toUri();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<List<ViewStatsDto>>() {
                });

    }
}
