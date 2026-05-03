package Ranaka.ranaka.audit.entity;

import Ranaka.ranaka.common.enums.AuditAction;
import Ranaka.ranaka.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(length = 500)
    private String description;

    @Column
    private Long entityId; // ID of affected entity

    @Column
    private String entityType; // Type of affected entity

    @Column(length = 1000)
    private String oldValue; // JSON representation of old state

    @Column(length = 1000)
    private String newValue; // JSON representation of new state

    @Column
    private String ipAddress;

    @Column
    private String userAgent;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

