package Ranaka.ranaka.request.dto.response;

import Ranaka.ranaka.common.enums.ApprovalAction;
import Ranaka.ranaka.common.enums.WorkflowStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalHistoryDto {

    private Long id;
    private WorkflowStage stage;
    private ApprovalAction action;
    private String comment;
    private LocalDateTime actionDate;
    private String approverName;
    private String approverRole;
}

