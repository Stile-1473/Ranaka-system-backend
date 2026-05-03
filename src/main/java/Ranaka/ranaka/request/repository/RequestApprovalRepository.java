package Ranaka.ranaka.request.repository;

import Ranaka.ranaka.common.enums.WorkflowStage;
import Ranaka.ranaka.request.entity.RequestApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestApprovalRepository extends JpaRepository<RequestApproval, Long> {

    List<RequestApproval> findByRequestIdOrderByCreatedAtDesc(Long requestId);

    List<RequestApproval> findByApproverIdOrderByCreatedAtDesc(Long approverId);

    List<RequestApproval> findByStage(WorkflowStage stage);
}

