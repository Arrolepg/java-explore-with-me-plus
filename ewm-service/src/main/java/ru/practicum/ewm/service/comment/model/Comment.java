package ru.practicum.ewm.service.comment.model;

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
@Table(name = "comment")
@EqualsAndHashCode(of = "id")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    @ToString.Exclude
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JoinColumn(name = "author_id")
    private User author;

    private String text;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Enumerated(EnumType.STRING)
    private CommentState state;

    @PrePersist
    void applyDefaults() {
        if (state == null) state = CommentState.PENDING;
        if (createdOn == null) createdOn = LocalDateTime.now();
    }
}
