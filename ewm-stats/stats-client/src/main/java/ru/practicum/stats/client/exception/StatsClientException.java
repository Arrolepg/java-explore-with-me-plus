package ru.practicum.stats.client.exception;

import lombok.Getter;

@Getter
public class StatsClientException extends RuntimeException {
    private final int status;
    private final String responseBody;

    public StatsClientException(String message, int status, String responseBody) {
        super(message);
        this.status = status;
        this.responseBody = responseBody;
    }
}
