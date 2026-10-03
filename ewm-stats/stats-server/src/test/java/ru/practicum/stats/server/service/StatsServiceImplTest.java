package ru.practicum.stats.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.stats.dto.parameter.ParamDto;
import ru.practicum.stats.dto.request.EndpointHitDto;
import ru.practicum.stats.dto.response.ViewStatsDto;
import ru.practicum.stats.server.exception.BadRequestException;
import ru.practicum.stats.server.model.Stats;
import ru.practicum.stats.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {
    private static final LocalDateTime START = LocalDateTime.of(2020, 5, 5, 0, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2035, 5, 5, 0, 0, 0);

    @Mock
    private StatsRepository statsRepository;

    @InjectMocks
    private StatsServiceImpl statsService;

    private List<ViewStatsDto> expected;

    @BeforeEach
    void setUp() {
        expected = List.of(new ViewStatsDto("ewm-main-service", "/events/1", 3L));
    }

    @Test
    void saveHitShouldSaveAndReturnDto() {
        EndpointHitDto dto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.1")
                .timestamp(START)
                .build();
        when(statsRepository.save(any(Stats.class))).thenAnswer(invocation -> {
            Stats stats = invocation.getArgument(0);
            stats.setId(1L);
            return stats;
        });

        EndpointHitDto saved = statsService.saveHit(dto);

        assertAll(
                () -> assertThat(saved.getId()).isEqualTo(1L),
                () -> assertThat(saved.getApp()).isEqualTo("ewm-main-service"),
                () -> assertThat(saved.getUri()).isEqualTo("/events/1"),
                () -> assertThat(saved.getIp()).isEqualTo("192.163.0.1"),
                () -> assertThat(saved.getTimestamp()).isEqualTo(START)
        );
    }

    @Test
    void getStatsWithoutUrisNotUnique() {
        when(statsRepository.findAllStats(START, END)).thenReturn(expected);

        assertThat(statsService.getStats(params(null, false))).isEqualTo(expected);
        verify(statsRepository).findAllStats(START, END);
    }

    @Test
    void getStatsWithUrisNotUnique() {
        List<String> uris = List.of("/events/1");
        when(statsRepository.findAllStatsByUris(START, END, uris)).thenReturn(expected);

        assertThat(statsService.getStats(params(uris, false))).isEqualTo(expected);
    }

    @Test
    void getStatsWithoutUrisUnique() {
        when(statsRepository.findUniqueStats(START, END)).thenReturn(expected);

        assertThat(statsService.getStats(params(List.of(), true))).isEqualTo(expected);
    }

    @Test
    void getStatsWithUrisUnique() {
        List<String> uris = List.of("/events/1");
        when(statsRepository.findUniqueStatsByUris(START, END, uris)).thenReturn(expected);

        assertThat(statsService.getStats(params(uris, true))).isEqualTo(expected);
    }

    @Test
    void getStatsWithStartAfterEndShouldThrow() {
        ParamDto params = ParamDto.builder().start(END).end(START).build();

        assertThatThrownBy(() -> statsService.getStats(params))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(statsRepository);
    }

    private ParamDto params(List<String> uris, boolean unique) {
        return ParamDto.builder()
                .start(START)
                .end(END)
                .uris(uris)
                .unique(unique)
                .build();
    }
}
