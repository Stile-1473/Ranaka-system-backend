package Ranaka.ranaka.request.dto.request;

import Ranaka.ranaka.common.enums.RequestPriority;
import Ranaka.ranaka.request.dto.LineItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRequestDto {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Valid
    private List<LineItemRequest> lineItems;

    private Long departmentId;

    @Size(max = 1000, message = "Justification must not exceed 1000 characters")
    private String justification;

    private RequestPriority priority;

    @Future(message = "Required by date must be in the future")
    private LocalDate requiredByDate;
}
