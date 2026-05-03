package Ranaka.ranaka.request.dto.response;

import Ranaka.ranaka.common.enums.RequestPriority;
import Ranaka.ranaka.common.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestFilterDto {

    private RequestStatus status;
    private RequestPriority priority;
    private Long departmentId;
    private Long requesterId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isOverdue;
    private String searchTerm; // for title or description search
}

