package Ranaka.ranaka.request.entity;

import Ranaka.ranaka.common.enums.RequestPriority;
import Ranaka.ranaka.common.enums.RequestStatus;
import Ranaka.ranaka.common.enums.WorkflowStage;
import Ranaka.ranaka.user.entity.User;
import Ranaka.ranaka.department.entity.Department;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "procurement_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ProcurementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000, nullable = false)
    private String description;

    // A request can have many concrete cost lines such as laptops, printers, paper, or internet bundles.
    @OneToMany(mappedBy = "procurementRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RequestLineItem> lineItems = new ArrayList<>();

    // This total is derived from line items so approvers can review the request at a glance.
    @Column(precision = 15, scale = 2)
    private BigDecimal estimatedCost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(length = 1000, nullable = false)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestPriority priority;

    @Column(nullable = false)
    private LocalDate requiredByDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    // Stage tells us who should act next, while status describes the broader business state.
    @Enumerated(EnumType.STRING)
    private WorkflowStage currentStage;

    @Column
    private LocalDateTime submittedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    @Builder.Default
    private Integer returnCount = 0;

    // This flag is set by scheduled reminder/escalation logic after SLA checks.
    @Column
    @Builder.Default
    private Boolean isOverdue = false;

    @Column
    private LocalDateTime overdueAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /**
     * Calculate estimated cost from all line items
     */
    public void calculateTotalEstimatedCost() {
        if (this.lineItems == null || this.lineItems.isEmpty()) {
            // A draft without line items should not carry an old stale total.
            this.estimatedCost = BigDecimal.ZERO;
        } else {
            this.estimatedCost = this.lineItems.stream()
                    .filter(RequestLineItem::isActive)
                    .map(RequestLineItem::getTotalCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /**
     * Add a line item and recalculate total
     */
    public void addLineItem(RequestLineItem item) {
        // Keep both sides of the JPA relationship in sync before recalculating totals.
        if (this.lineItems == null) {
            this.lineItems = new ArrayList<>();
        }
        item.setProcurementRequest(this);
        this.lineItems.add(item);
        calculateTotalEstimatedCost();
    }

    /**
     * Remove a line item and recalculate total
     */
    public void removeLineItem(RequestLineItem item) {
        if (this.lineItems == null) {
            this.lineItems = new ArrayList<>();
            this.estimatedCost = BigDecimal.ZERO;
            return;
        }
        this.lineItems.remove(item);
        calculateTotalEstimatedCost();
    }
}
