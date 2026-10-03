package ru.practicum.stats.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.stats.dto.parameter.ParamDto;
import ru.practicum.stats.dto.response.ViewStatsDto;
import ru.practicum.stats.server.exception.BadRequestException;
import ru.practicum.stats.server.service.StatsService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
class StatsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    @Test
    void hitShouldReturnCreated() throws Exception {
        String body = "{\"app\":\"ewm-main-service\",\"uri\":\"/events/1\","
                + "\"ip\":\"192.163.0.1\",\"timestamp\":\"2022-09-06 11:00:23\"}";

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(statsService).saveHit(any());
    }

    @Test
    void hitWithBlankAppShouldReturnBadRequest() throws Exception {
        String body = "{\"app\":\"\",\"uri\":\"/events/1\","
                + "\"ip\":\"192.163.0.1\",\"timestamp\":\"2022-09-06 11:00:23\"}";

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(statsService, never()).saveHit(any());
    }

    @Test
    void getStatsShouldReturnList() throws Exception {
        when(statsService.getStats(any(ParamDto.class)))
                .thenReturn(List.of(new ViewStatsDto("ewm-main-service", "/events/1", 6L)));

        mockMvc.perform(get("/stats")
                        .param("start", "2020-05-05 00:00:00")
                        .param("end", "2035-05-05 00:00:00")
                        .param("uris", "/events/1")
                        .param("unique", "true"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$[0].app").value("ewm-main-service"),
                        jsonPath("$[0].uri").value("/events/1"),
                        jsonPath("$[0].hits").value(6)
                );
    }

    @Test
    void getStatsWithoutStartShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("end", "2035-05-05 00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStatsWithWrongDateFormatShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "not-a-date")
                        .param("end", "2035-05-05 00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStatsWithStartAfterEndShouldReturnBadRequest() throws Exception {
        when(statsService.getStats(any(ParamDto.class)))
                .thenThrow(new BadRequestException("start after end"));

        mockMvc.perform(get("/stats")
                        .param("start", "2035-05-05 00:00:00")
                        .param("end", "2020-05-05 00:00:00"))
                .andExpect(status().isBadRequest());
    }
}
