package Ranaka.ranaka.request.repository;

import Ranaka.ranaka.request.entity.RequestLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RequestLineItemRepository extends JpaRepository<RequestLineItem, Long> {

    /**
     * Find all line items for a specific procurement request
     */
    List<RequestLineItem> findByProcurementRequestIdAndIsActiveTrue(Long requestId);

    /**
     * Delete all line items for a specific request
     */
    @Modifying
    @Query("UPDATE RequestLineItem item SET item.isActive = false, item.deletedAt = :deletedAt WHERE item.procurementRequest.id = :requestId AND item.isActive = true")
    void softDeleteByProcurementRequestId(@Param("requestId") Long requestId, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * Count line items for a request
     */
    Integer countByProcurementRequestIdAndIsActiveTrue(Long requestId);

    /**
     * Find line items by request with ordering
     */
    @Query("SELECT item FROM RequestLineItem item WHERE item.procurementRequest.id = :requestId AND item.isActive = true ORDER BY item.id ASC")
    List<RequestLineItem> findByRequestIdOrdered(@Param("requestId") Long requestId);
}
