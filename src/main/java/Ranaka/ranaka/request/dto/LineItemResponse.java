package Ranaka.ranaka.request.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineItemResponse {
    private Long id;
    private String itemDescription;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String unit;
    private String notes;
    private LocalDateTime createdAt;
}
