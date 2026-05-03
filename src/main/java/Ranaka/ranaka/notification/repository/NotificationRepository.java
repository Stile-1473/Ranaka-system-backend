package Ranaka.ranaka.notification.repository;

import Ranaka.ranaka.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Used by the notification center screen to show the newest items first.
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    java.util.Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    // Prevent duplicate reminder spam for the same business event and user.
    boolean existsByRecipientIdAndTypeAndReferenceIdAndReferenceType(Long recipientId,
                                                                     Ranaka.ranaka.common.enums.NotificationType type,
                                                                     Long referenceId,
                                                                     String referenceType);

    @Modifying
    // Bulk mark-as-read keeps the endpoint efficient when a user clears their entire inbox.
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.recipient.id = :recipientId AND n.isRead = false")
    int markAllAsReadByRecipientId(@Param("recipientId") Long recipientId, @Param("readAt") LocalDateTime readAt);

    // Helpful if the team later adds email retry jobs or dead-letter style recovery.
    List<Notification> findByEmailSentFalseAndCreatedAtBefore(LocalDateTime cutoffDate);
}
