package Ranaka.ranaka.request.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "request_line_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class RequestLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ProcurementRequest procurementRequest;

    @Column(length = 500, nullable = false)
    private String itemDescription;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal unitCost;

    // Stored total makes reporting and request detail reads simpler than recalculating everywhere.
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal totalCost;

    @Column(length = 50)
    @Builder.Default
    private String unit = "PCS";  // PCS, KG, L, M, BOX, etc.

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate total cost when setting quantity or unit cost
     */
    public void calculateTotalCost() {
        if (this.quantity != null && this.unitCost != null) {
            // Example: 3 laptops x 1200.00 each = 3600.00 total.
            this.totalCost = this.unitCost.multiply(new BigDecimal(this.quantity));
        }
    }
}
