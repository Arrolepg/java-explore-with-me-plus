package ru.practicum.ewm.service.request.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.ewm.service.event.model.Event;
import ru.practicum.ewm.service.user.model.User;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "participation_request")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    @ToString.Exclude
    private Event event;

    private LocalDateTime created;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    @ToString.Exclude
    private User requester;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Request)) return false;

        return id != null && id.equals(((Request) o).getId());
    }

    @Override
    public final int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }

}
