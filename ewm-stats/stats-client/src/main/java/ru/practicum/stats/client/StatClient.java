package ru.practicum.stats.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.parameter.ParamDto;
import ru.practicum.stats.dto.request.EndpointHitDto;
import ru.practicum.stats.dto.response.ViewStatsDto;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class StatClient {
    private final RestClient restClient;

    public StatClient(RestClient.Builder builder, @Value("${stats.client.url}") String statUrl) {
        this.restClient = builder.baseUrl(statUrl).build();

    }

    public ResponseEntity<Void> createHit(EndpointHitDto dto) {
        return restClient.post()
                .uri("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
    }

    public ResponseEntity<List<ViewStatsDto>> get(ParamDto dto) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        URI uri = UriComponentsBuilder.newInstance()
                .path("/stats")
                .queryParam("start", dto.getStart().format(formatter))
                .queryParam("end", dto.getEnd().format(formatter))
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
