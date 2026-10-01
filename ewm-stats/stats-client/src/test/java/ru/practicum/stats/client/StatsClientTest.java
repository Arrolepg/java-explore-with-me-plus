package ru.practicum.stats.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.practicum.stats.dto.parameter.ParamDto;
import ru.practicum.stats.dto.request.EndpointHitDto;
import ru.practicum.stats.dto.response.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;


class StatsClientTest {
    private StatClient statClient;
    private MockRestServiceServer mockServer;
    private ParamDto paramDto;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9090");

        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        statClient = new StatClient(restClient);

        paramDto = new ParamDto();
        paramDto.setStart(LocalDateTime.of(2026, 9, 20, 18, 30, 10));
        paramDto.setEnd(LocalDateTime.of(2026, 9, 25, 18, 30, 10));
        paramDto.setUris(List.of("/events/1"));
        paramDto.setUnique(false);
    }

    @Test
    void createHitShouldSendPostRequest() {

        mockServer.expect(requestTo("http://localhost:9090/hit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                ))
                .andRespond(withSuccess());

        EndpointHitDto dto = new EndpointHitDto();
        dto.setApp("ewm-main-service");
        dto.setUri("/events/1");
        dto.setIp("192.163.0.1");
        dto.setTimestamp(LocalDateTime.of(2026, 9, 20, 18, 30, 10));

        ResponseEntity<Void> response = statClient.createHit(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        mockServer.verify();
    }

    @Test
    void getShouldReturnStats() {

        mockServer.expect(request -> {
                    assertThat(request.getMethod())
                            .isEqualTo(HttpMethod.GET);

                    assertThat(request.getURI().getPath())
                            .isEqualTo("/stats");
                })
                .andRespond(withSuccess("[{\"app\":\"ewm-main-service\",\"uri\":\"/events/1\",\"hits\":10}]",
                        MediaType.APPLICATION_JSON));

        ResponseEntity<List<ViewStatsDto>> response =
                statClient.get(paramDto);

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).hasSize(1)
        );

        mockServer.verify();
    }

    @Test
    void getShouldSendCorrectQueryParams() {
        mockServer.expect(request -> {
                    assertThat(request.getURI().getPath())
                            .isEqualTo("/stats");
                })
                .andExpect(queryParam("start", "2026-09-20%2018:30:10"))
                .andExpect(queryParam("end", "2026-09-25%2018:30:10"))
                .andExpect(queryParam("uris", "/events/1"))
                .andExpect(queryParam("unique", "false"))
                .andRespond(withSuccess());

        statClient.get(paramDto);

        mockServer.verify();
    }

}
