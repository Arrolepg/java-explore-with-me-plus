package ru.practicum.stats.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class StatsClientConfig {

    @Bean
    public RestClient statsRestClient(@Value("${stats.client.url}") String serverUrl) {

        return RestClient.builder()
                .baseUrl(serverUrl)
                .build();
    }

    @Bean
    public StatsClient statsClient(RestClient statsRestClient, ObjectMapper objectMapper) {
        return new StatsClient(statsRestClient, objectMapper);
    }

}
