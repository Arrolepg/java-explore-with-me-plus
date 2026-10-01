package ru.practicum.stats.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class StatClientConfig {

    @Bean
    public RestClient statRestClient(@Value("${stats.client.url}") String serverUrl) {

        return RestClient.builder()
                .baseUrl(serverUrl)
                .build();
    }

    @Bean
    public StatClient statClient(RestClient statsRestClient) {
        return new StatClient(statsRestClient);
    }

}
