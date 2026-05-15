package Ranaka.ranaka;

import Ranaka.ranaka.audit.repository.AuditLogRepository;
import Ranaka.ranaka.common.enums.RequestPriority;
import Ranaka.ranaka.common.enums.RequestStatus;
import Ranaka.ranaka.common.enums.WorkflowStage;
import Ranaka.ranaka.department.entity.Department;
import Ranaka.ranaka.department.repository.DepartmentRepository;
import Ranaka.ranaka.notification.service.NotificationService;
import Ranaka.ranaka.request.dto.response.RequestListResponseDto;
import Ranaka.ranaka.request.entity.ProcurementRequest;
import Ranaka.ranaka.request.repository.ProcurementRequestRepository;
import Ranaka.ranaka.request.repository.RequestApprovalRepository;
import Ranaka.ranaka.request.repository.RequestAttachmentRepository;
import Ranaka.ranaka.request.repository.RequestCommentRepository;
import Ranaka.ranaka.request.repository.RequestLineItemRepository;
import Ranaka.ranaka.request.serviceImpl.RequestServiceImpl;
import Ranaka.ranaka.user.domain.Role;
import Ranaka.ranaka.user.entity.User;
import Ranaka.ranaka.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private ProcurementRequestRepository requestRepository;

    @Mock
    private RequestApprovalRepository approvalRepository;

    @Mock
    private RequestCommentRepository commentRepository;

    @Mock
    private RequestAttachmentRepository attachmentRepository;

    @Mock
    private RequestLineItemRepository lineItemRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RequestServiceImpl requestService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getOverdueRequests_filtersAdminViewToAdminStageOnly() {
        User admin = buildUser(10L, "admin@ranaka.org", Role.ADMIN);
        setAuthenticatedUser(admin);

        when(requestRepository.findByIsOverdueTrue()).thenReturn(List.of(
                buildRequest(1L, "Admin overdue", WorkflowStage.ADMIN_RECOMMENDATION),
                buildRequest(2L, "GM overdue", WorkflowStage.GM_APPROVAL),
                buildRequest(3L, "CEO overdue", WorkflowStage.CEO_AUTHORIZATION)
        ));

        List<RequestListResponseDto> result = requestService.getOverdueRequests();

        assertEquals(1, result.size());
        assertEquals("Admin overdue", result.get(0).getTitle());
    }

    @Test
    void getOverdueRequests_allowsSystemAdminToSeeAllStages() {
        User systemAdmin = buildUser(11L, "sysadmin@ranaka.org", Role.SYSTEM_ADMIN);
        setAuthenticatedUser(systemAdmin);

        when(requestRepository.findByIsOverdueTrue()).thenReturn(List.of(
                buildRequest(1L, "Admin overdue", WorkflowStage.ADMIN_RECOMMENDATION),
                buildRequest(2L, "GM overdue", WorkflowStage.GM_APPROVAL),
                buildRequest(3L, "CEO overdue", WorkflowStage.CEO_AUTHORIZATION)
        ));

        List<RequestListResponseDto> result = requestService.getOverdueRequests();

        assertEquals(3, result.size());
    }

    private void setAuthenticatedUser(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    private User buildUser(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .firstName("Test")
                .lastName("User")
                .email(email)
                .phoneNumber("+263700000000")
                .password("secret")
                .role(role)
                .isActive(true)
                .build();
    }

    private ProcurementRequest buildRequest(Long id, String title, WorkflowStage stage) {
        Department department = Department.builder()
                .id(50L + id)
                .name("Operations")
                .code("OPS")
                .description("Operations")
                .isActive(true)
                .build();

        return ProcurementRequest.builder()
                .id(id)
                .title(title)
                .description("Description")
                .department(department)
                .justification("Needed for operations")
                .priority(RequestPriority.HIGH)
                .requiredByDate(LocalDate.now().plusDays(3))
                .requester(buildUser(100L + id, "requester" + id + "@ranaka.org", Role.REQUESTER))
                .status(switch (stage) {
                    case ADMIN_RECOMMENDATION -> RequestStatus.PENDING_ADMIN_RECOMMENDATION;
                    case GM_APPROVAL -> RequestStatus.PENDING_GM_APPROVAL;
                    case CEO_AUTHORIZATION -> RequestStatus.PENDING_CEO_AUTHORIZATION;
                    default -> RequestStatus.DRAFT;
                })
                .currentStage(stage)
                .estimatedCost(BigDecimal.valueOf(1500))
                .isOverdue(true)
                .returnCount(0)
                .build();
    }
}
