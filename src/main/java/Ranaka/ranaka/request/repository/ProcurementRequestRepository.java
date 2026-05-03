package Ranaka.ranaka.request.repository;

import Ranaka.ranaka.common.enums.RequestPriority;
import Ranaka.ranaka.common.enums.RequestStatus;
import Ranaka.ranaka.common.enums.WorkflowStage;
import Ranaka.ranaka.request.entity.ProcurementRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProcurementRequestRepository extends JpaRepository<ProcurementRequest, Long> {

    // Useful for the "My Requests" page.
    List<ProcurementRequest> findByRequesterId(Long requesterId);

    List<ProcurementRequest> findByStatus(RequestStatus status);

    List<ProcurementRequest> findByPriority(RequestPriority priority);

    List<ProcurementRequest> findByCurrentStage(WorkflowStage currentStage);

    List<ProcurementRequest> findByIsOverdueTrue();

    Page<ProcurementRequest> findByRequesterId(Long requesterId, Pageable pageable);

    Page<ProcurementRequest> findByStatus(RequestStatus status, Pageable pageable);

    Page<ProcurementRequest> findByPriority(RequestPriority priority, Pageable pageable);

    Page<ProcurementRequest> findByCurrentStage(WorkflowStage currentStage, Pageable pageable);

    // Scheduler uses this to discover items already marked overdue.
    @Query("SELECT r FROM ProcurementRequest r WHERE r.status IN :statuses AND r.isOverdue = false AND r.submittedAt < :cutoffDate")
    List<ProcurementRequest> findOverdueRequests(@Param("statuses") List<RequestStatus> statuses, @Param("cutoffDate") LocalDateTime cutoffDate);

    // Dashboard cards use these counts to avoid loading whole request lists unnecessarily.
    @Query("SELECT COUNT(r) FROM ProcurementRequest r WHERE r.requester.id = :userId AND r.status = :status")
    long countByRequesterIdAndStatus(@Param("userId") Long userId, @Param("status") RequestStatus status);

    @Query("SELECT COUNT(r) FROM ProcurementRequest r WHERE r.currentStage = :stage AND r.isOverdue = true")
    long countOverdueByStage(@Param("stage") WorkflowStage stage);

    // Sort pending work so high-priority items rise to the top of approval queues.
    @Query("SELECT r FROM ProcurementRequest r WHERE r.status IN :statuses ORDER BY r.priority DESC, r.createdAt ASC")
    List<ProcurementRequest> findPendingRequestsByPriority(@Param("statuses") List<RequestStatus> statuses);
}
