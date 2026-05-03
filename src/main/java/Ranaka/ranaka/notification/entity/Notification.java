package Ranaka.ranaka.notification.entity;

import Ranaka.ranaka.common.enums.NotificationType;
import Ranaka.ranaka.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000, nullable = false)
    private String message;

    @Column
    private Long referenceId; // ID of related entity

    @Column
    private String referenceType; // Type of related entity

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column
    private LocalDateTime readAt;

    @Column
    @Builder.Default
    private boolean emailSent = false;

    @Column
    private LocalDateTime emailSentAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
