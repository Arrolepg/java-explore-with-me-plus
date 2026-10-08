package ru.practicum.stats.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.common.error.ApiError;
import ru.practicum.common.util.PatternDataTime;
import ru.practicum.stats.client.exception.StatsClientException;
import ru.practicum.stats.dto.parameter.ParamDto;
import ru.practicum.stats.dto.request.EndpointHitDto;
import ru.practicum.stats.dto.response.ViewStatsDto;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class StatsClient {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PatternDataTime.PATTERN);
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ResponseEntity<Void> createHit(EndpointHitDto dto) {
        return restClient.post()
                .uri("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xx)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xx)
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
                .onStatus(HttpStatusCode::is4xxClientError, this::handle4xx)
                .onStatus(HttpStatusCode::is5xxServerError, this::handle5xx)
                .toEntity(new ParameterizedTypeReference<List<ViewStatsDto>>() {
                });

    }

    private void handle4xx(HttpRequest request, ClientHttpResponse response) throws IOException {
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        String message = extractMessage(body);
        log.warn("Сервер статистики (stats-server) вернул 4xx, status = {}, message = {}",
                response.getStatusCode().value(), message);

        throw new StatsClientException(message, response.getStatusCode().value(), body);
    }

    private void handle5xx(HttpRequest request, ClientHttpResponse response) throws IOException {
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        log.warn("Сервер статистики (stats-server) вернул 5xx, status = {}",
                response.getStatusCode().value());

        throw new StatsClientException("Сервер статистики временно недоступен", response.getStatusCode().value(), body);
    }

    private String extractMessage(String body) {
        try {
            ApiError apiError = objectMapper.readValue(body, ApiError.class);
            return apiError.message();
        } catch (Exception e) {
            log.debug("ApiError не удалось распарсить, используется сырое тело", e);
            return body;
        }
    }
}
