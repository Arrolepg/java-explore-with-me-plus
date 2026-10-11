package ru.practicum.ewm.service.event.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.ewm.service.category.model.Category;
import ru.practicum.ewm.service.user.model.User;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "event")
@EqualsAndHashCode(of = "id")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String annotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    private Category category;

    private String description;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Embedded
    private EventLocation eventLocation;

    private Boolean paid;

    @Column(name = "participant_limit")
    private Integer participantLimit;

    @Column(name = "request_moderation")
    private Boolean requestModeration;

    private String title;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Enumerated(EnumType.STRING)
    private EventState state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id")
    @ToString.Exclude
    private User initiator;

    @PrePersist
    void applyDefaults() {
        if (state == null) state = EventState.PENDING;
        if (createdOn == null) createdOn = LocalDateTime.now();
    }
}
