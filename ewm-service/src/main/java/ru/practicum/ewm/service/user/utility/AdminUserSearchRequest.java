package ru.practicum.ewm.service.user.utility;

import java.util.List;

public record AdminUserSearchRequest(
        List<Long> ids,
        Integer from,
        Integer size
) {
}
