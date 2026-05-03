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
public class CreateRequestDto {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<LineItemRequest> lineItems;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotBlank(message = "Justification is required")
    @Size(max = 1000, message = "Justification must not exceed 1000 characters")
    private String justification;

    @NotNull(message = "Priority is required")
    private RequestPriority priority;

    @NotNull(message = "Required by date is required")
    @Future(message = "Required by date must be in the future")
    private LocalDate requiredByDate;
}
