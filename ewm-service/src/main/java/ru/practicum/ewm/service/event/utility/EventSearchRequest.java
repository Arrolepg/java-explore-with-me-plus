package ru.practicum.ewm.service.event.utility;

public record EventSearchRequest(
   Integer from,
   Integer size,
   String sort
) {}
