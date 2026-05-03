package Ranaka.ranaka.request.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalActionRequest {

    //@NotBlank(message = "Comment is required for reject/return actions")
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}

