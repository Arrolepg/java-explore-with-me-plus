package ru.practicum.stats.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class StatsClientConfig {
    @Bean
    public RestTemplate statsRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public StatsClient statsClient(@Value("${stats.client.url}") String serverUrl,
                                   RestTemplate statsRestTemplate) {
        return new StatsClient(serverUrl, statsRestTemplate);
    }
}
