package Ranaka.ranaka.bootstrap;

import Ranaka.ranaka.common.enums.ApprovalAction;
import Ranaka.ranaka.common.enums.RequestPriority;
import Ranaka.ranaka.common.enums.RequestStatus;
import Ranaka.ranaka.common.enums.WorkflowStage;
import Ranaka.ranaka.audit.repository.AuditLogRepository;
import Ranaka.ranaka.department.entity.Department;
import Ranaka.ranaka.department.repository.DepartmentRepository;
import Ranaka.ranaka.notification.repository.NotificationRepository;
import Ranaka.ranaka.request.entity.ProcurementRequest;
import Ranaka.ranaka.request.entity.RequestApproval;
import Ranaka.ranaka.request.entity.RequestAttachment;
import Ranaka.ranaka.request.entity.RequestComment;
import Ranaka.ranaka.request.repository.ProcurementRequestRepository;
import Ranaka.ranaka.request.repository.RequestApprovalRepository;
import Ranaka.ranaka.request.repository.RequestAttachmentRepository;
import Ranaka.ranaka.request.repository.RequestCommentRepository;
import Ranaka.ranaka.user.domain.Role;
import Ranaka.ranaka.user.entity.User;
import Ranaka.ranaka.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements CommandLineRunner {

    private static final Set<String> MINIMAL_ACTIVE_EMAILS = Set.of(
            "stilesmvura@gmail.com",
            "admin@ranaka.org",
            "gm@ranaka.org",
            "ceo@ranaka.org",
            "requester1@ranaka.org",
            "requester2@ranaka.org",
            "requester3@ranaka.org"
    );

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ProcurementRequestRepository procurementRequestRepository;
    private final RequestApprovalRepository requestApprovalRepository;
    private final RequestCommentRepository requestCommentRepository;
    private final RequestAttachmentRepository requestAttachmentRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.mode:demo}")
    private String bootstrapMode;

    @Value("${app.bootstrap.reset:false}")
    private boolean resetBootstrapData;

    @Value("${app.bootstrap.default-password:Password@123}")
    private String defaultPassword;

    @Override
    @Transactional
    public void run(String... args) {
        String mode = normalizeBootstrapMode(bootstrapMode);

        if ("off".equals(mode)) {
            log.info("Bootstrap seeding is disabled.");
            return;
        }

        Map<String, Department> departments = seedDepartments();

        if (resetBootstrapData) {
            resetWorkflowData();
        }

        Map<String, User> users = "minimal".equals(mode) ? seedMinimalUsers() : seedDemoUsers();

        if ("minimal".equals(mode)) {
            deactivateUsersOutside();
            log.info("Minimal bootstrap completed for company testing.");
            return;
        }

        if (procurementRequestRepository.count() == 0) {
            seedRequests(departments, users);
            log.info("Demo workflow data seeded.");
        } else {
            log.info("Requests already exist; skipping demo workflow data seeding.");
        }
    }

    private String normalizeBootstrapMode(String rawMode) {
        String mode = rawMode == null ? "demo" : rawMode.trim().toLowerCase();
        return switch (mode) {
            case "demo", "minimal", "off" -> mode;
            default -> {
                log.warn("Unknown bootstrap mode '{}'. Falling back to 'minimal'.", rawMode);
                yield "minimal";
            }
        };
    }

    private void resetWorkflowData() {
        log.warn("Reset bootstrap flag is enabled. Clearing workflow data, audit logs, and users before seeding.");

        notificationRepository.deleteAll();
        requestAttachmentRepository.deleteAll();
        requestCommentRepository.deleteAll();
        requestApprovalRepository.deleteAll();
        procurementRequestRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Map<String, Department> seedDepartments() {
        Department legalServices = findOrCreateDepartment(
                "Legal Services",
                "LEGAL_SERVICES",
                "Supports legal aid casework, advocacy, and direct client service operations."
        );

        Department finance = findOrCreateDepartment(
                "Finance and Administration",
                "FINANCE_ADMIN",
                "Handles budgeting, procurement administration, and operational support."
        );

        Department it = findOrCreateDepartment(
                "ICT and Systems",
                "ICT_SYSTEMS",
                "Maintains infrastructure, devices, software subscriptions, and internal platforms."
        );

        Department outreach = findOrCreateDepartment(
                "Community Outreach",
                "COMMUNITY_OUTREACH",
                "Coordinates mobile clinics, awareness campaigns, and stakeholder engagement."
        );

        return Map.of(
                "legal", legalServices,
                "finance", finance,
                "it", it,
                "outreach", outreach
        );
    }

    private Map<String, User> seedDemoUsers() {
        User systemAdmin = findOrCreateUser(
                "Stiles", "Mvura", "stilesmvura@gmail.com", "+263771000001", Role.SYSTEM_ADMIN
        );
        User admin = findOrCreateUser(
                "Tariro", "Ncube", "admin@ranaka.org", "+263771000002", Role.ADMIN
        );
        User gm = findOrCreateUser(
                "Nyasha", "Dube", "gm@ranaka.org", "+263771000003", Role.GM
        );
        User ceo = findOrCreateUser(
                "Rumbidzai", "Chikore", "ceo@ranaka.org", "+263771000004", Role.CEO
        );
        User requesterOne = findOrCreateUser(
                "Lindiwe", "Sibanda", "requester1@ranaka.org", "+263771000005", Role.REQUESTER
        );
        User requesterTwo = findOrCreateUser(
                "Farai", "Mhlanga", "requester2@ranaka.org", "+263771000006", Role.REQUESTER
        );

        return Map.of(
                "systemAdmin", systemAdmin,
                "admin", admin,
                "gm", gm,
                "ceo", ceo,
                "requesterOne", requesterOne,
                "requesterTwo", requesterTwo
        );
    }

    private Map<String, User> seedMinimalUsers() {
        User systemAdmin = findOrCreateUser(
                "Stiles", "Mvura", "stilesmvura@gmail.com", "+263771000001", Role.SYSTEM_ADMIN
        );
        User admin = findOrCreateUser(
                "Admin", "Reviewer", "admin@ranaka.org", "+263771000002", Role.ADMIN
        );
        User gm = findOrCreateUser(
                "General", "Manager", "gm@ranaka.org", "+263771000003", Role.GM
        );
        User ceo = findOrCreateUser(
                "Chief", "Executive", "ceo@ranaka.org", "+263771000004", Role.CEO
        );
        User requesterOne = findOrCreateUser(
                "Requester", "One", "requester1@ranaka.org", "+263771000005", Role.REQUESTER
        );
        User requesterTwo = findOrCreateUser(
                "Requester", "Two", "requester2@ranaka.org", "+263771000006", Role.REQUESTER
        );
        User requesterThree = findOrCreateUser(
                "Requester", "Three", "requester3@ranaka.org", "+263771000007", Role.REQUESTER
        );

        return Map.of(
                "systemAdmin", systemAdmin,
                "admin", admin,
                "gm", gm,
                "ceo", ceo,
                "requesterOne", requesterOne,
                "requesterTwo", requesterTwo,
                "requesterThree", requesterThree
        );
    }

    private void deactivateUsersOutside() {
        Set<Long> keptIds = MINIMAL_ACTIVE_EMAILS.stream()
                .map(userRepository::findByEmail)
                .flatMap(optionalUser -> optionalUser.stream())
                .map(User::getId)
                .collect(Collectors.toSet());

        for (User user : userRepository.findAll()) {
            boolean shouldRemainActive = keptIds.contains(user.getId());
            if (user.isActive() != shouldRemainActive) {
                user.setActive(shouldRemainActive);
                userRepository.save(user);
            }
        }
    }

    private Department findOrCreateDepartment(String name, String code, String description) {
        Department existingByCode = departmentRepository.findByCode(code).orElse(null);
        if (existingByCode != null) {
            existingByCode.setName(name);
            existingByCode.setDescription(description);
            existingByCode.setActive(true);
            existingByCode.setDeletedAt(null);
            return departmentRepository.save(existingByCode);
        }

        Department existingByName = departmentRepository.findByName(name).orElse(null);
        if (existingByName != null) {
            existingByName.setCode(code);
            existingByName.setDescription(description);
            existingByName.setActive(true);
            existingByName.setDeletedAt(null);
            return departmentRepository.save(existingByName);
        }

        return departmentRepository.save(Department.builder()
                .name(name)
                .code(code)
                .description(description)
                .isActive(true)
                .build());
    }

    private User findOrCreateUser(String firstName, String lastName, String email, String phoneNumber, Role role) {
        User emailMatch = userRepository.findByEmail(email).orElse(null);
        User phoneMatch = userRepository.findByPhoneNumber(phoneNumber).orElse(null);

        if (emailMatch != null && phoneMatch != null && !emailMatch.getId().equals(phoneMatch.getId())) {
            log.warn(
                    "Bootstrap user conflict for email '{}' and phone '{}'. Reusing email-matched user id={} and leaving existing phone owner id={} untouched.",
                    email,
                    phoneNumber,
                    emailMatch.getId(),
                    phoneMatch.getId()
            );

            emailMatch.setFirstName(firstName);
            emailMatch.setLastName(lastName);
            emailMatch.setRole(role);
            emailMatch.setActive(true);
            emailMatch.setPassword(passwordEncoder.encode(defaultPassword));
            return userRepository.save(emailMatch);
        }

        User existing = emailMatch != null ? emailMatch : phoneMatch;

        if (existing != null) {
            existing.setFirstName(firstName);
            existing.setLastName(lastName);
            existing.setEmail(email);
            existing.setPhoneNumber(phoneNumber);
            existing.setRole(role);
            existing.setActive(true);
            existing.setPassword(passwordEncoder.encode(defaultPassword));
            return userRepository.save(existing);
        }

        return userRepository.save(buildUser(
                firstName,
                lastName,
                email,
                phoneNumber,
                role,
                passwordEncoder.encode(defaultPassword)
        ));
    }

    private void seedRequests(Map<String, Department> departments, Map<String, User> users) {
        ProcurementRequest draftRequest = saveRequest(ProcurementRequest.builder()
                .title("Desktop printers for legal aid intake office")
                .description("Procurement of two network printers to support faster client intake processing.")
                .estimatedCost(new BigDecimal("850.00"))
                .department(departments.get("legal"))
                .justification("Current equipment is unreliable and causes delays in opening client matter files.")
                .priority(RequestPriority.MEDIUM)
                .requiredByDate(LocalDate.now().plusDays(14))
                .requester(users.get("requesterOne"))
                .status(RequestStatus.DRAFT)
                .currentStage(WorkflowStage.DRAFT)
                .returnCount(0)
                .isOverdue(false)
                .build());
        addComment(draftRequest, users.get("requesterOne"), "Draft request prepared for review before submission.", false);

        ProcurementRequest pendingAdminRequest = saveRequest(ProcurementRequest.builder()
                .title("Stationery packs for district legal clinics")
                .description("Bulk stationery replenishment for intake desks and field paralegal teams.")
                .estimatedCost(new BigDecimal("1200.00"))
                .department(departments.get("outreach"))
                .justification("Field clinics are running low on supplies ahead of the next outreach cycle.")
                .priority(RequestPriority.HIGH)
                .requiredByDate(LocalDate.now().plusDays(10))
                .requester(users.get("requesterTwo"))
                .status(RequestStatus.PENDING_ADMIN_RECOMMENDATION)
                .currentStage(WorkflowStage.ADMIN_RECOMMENDATION)
                .submittedAt(LocalDateTime.now().minusDays(1))
                .returnCount(0)
                .isOverdue(false)
                .build());
        addComment(pendingAdminRequest, users.get("requesterTwo"), "Submitted for administrative recommendation.", false);

        ProcurementRequest pendingGmRequest = saveRequest(ProcurementRequest.builder()
                .title("Laptop replacements for legal officers")
                .description("Replacement of aging laptops used by legal officers for case preparation and court work.")
                .estimatedCost(new BigDecimal("3600.00"))
                .department(departments.get("it"))
                .justification("Existing devices are failing and affecting court filing turnaround times.")
                .priority(RequestPriority.HIGH)
                .requiredByDate(LocalDate.now().plusDays(7))
                .requester(users.get("requesterOne"))
                .status(RequestStatus.PENDING_GM_APPROVAL)
                .currentStage(WorkflowStage.GM_APPROVAL)
                .submittedAt(LocalDateTime.now().minusDays(3))
                .returnCount(0)
                .isOverdue(false)
                .build());
        addApproval(pendingGmRequest, users.get("admin"), WorkflowStage.ADMIN_RECOMMENDATION,
                ApprovalAction.RECOMMEND, "Budget line verified and recommendation granted.", LocalDateTime.now().minusDays(2));
        addComment(pendingGmRequest, users.get("admin"), "Recommended after checking current allocation.", true);

        ProcurementRequest pendingCeoRequest = saveRequest(ProcurementRequest.builder()
                .title("Office chairs for reception and client waiting area")
                .description("Replacement of damaged seating in reception and waiting space for daily clients.")
                .estimatedCost(new BigDecimal("1800.00"))
                .department(departments.get("finance"))
                .justification("Current seating is worn out and not suitable for clients waiting for consultations.")
                .priority(RequestPriority.MEDIUM)
                .requiredByDate(LocalDate.now().plusDays(12))
                .requester(users.get("requesterTwo"))
                .status(RequestStatus.PENDING_CEO_AUTHORIZATION)
                .currentStage(WorkflowStage.CEO_AUTHORIZATION)
                .submittedAt(LocalDateTime.now().minusDays(5))
                .returnCount(0)
                .isOverdue(false)
                .build());
        addApproval(pendingCeoRequest, users.get("admin"), WorkflowStage.ADMIN_RECOMMENDATION,
                ApprovalAction.RECOMMEND, "Administrative review completed and recommended.", LocalDateTime.now().minusDays(4));
        addApproval(pendingCeoRequest, users.get("gm"), WorkflowStage.GM_APPROVAL,
                ApprovalAction.APPROVE, "Approved at GM level pending final authorization.", LocalDateTime.now().minusDays(2));
        addComment(pendingCeoRequest, users.get("gm"), "Please prioritize vendor selection once approved.", true);

        ProcurementRequest returnedRequest = saveRequest(ProcurementRequest.builder()
                .title("Internet data bundles for mobile legal clinics")
                .description("Connectivity support for outreach teams conducting remote client interviews.")
                .estimatedCost(new BigDecimal("620.00"))
                .department(departments.get("outreach"))
                .justification("Connectivity is essential for accessing case systems during rural field operations.")
                .priority(RequestPriority.MEDIUM)
                .requiredByDate(LocalDate.now().plusDays(8))
                .requester(users.get("requesterOne"))
                .status(RequestStatus.RETURNED_FOR_CORRECTION)
                .currentStage(WorkflowStage.DRAFT)
                .submittedAt(LocalDateTime.now().minusDays(4))
                .returnCount(1)
                .isOverdue(false)
                .build());
        addApproval(returnedRequest, users.get("admin"), WorkflowStage.ADMIN_RECOMMENDATION,
                ApprovalAction.RETURN_FOR_CORRECTION, "Please attach a clearer cost breakdown for each team.", LocalDateTime.now().minusDays(1));
        addComment(returnedRequest, users.get("admin"), "Returned for a more detailed cost schedule.", false);

        ProcurementRequest rejectedRequest = saveRequest(ProcurementRequest.builder()
                .title("Premium office refreshments for executive meeting room")
                .description("Monthly refreshments and catering stock for internal executive hosting.")
                .estimatedCost(new BigDecimal("950.00"))
                .department(departments.get("finance"))
                .justification("Requested to improve internal executive hospitality arrangements.")
                .priority(RequestPriority.LOW)
                .requiredByDate(LocalDate.now().plusDays(20))
                .requester(users.get("requesterTwo"))
                .status(RequestStatus.REJECTED)
                .submittedAt(LocalDateTime.now().minusDays(6))
                .completedAt(LocalDateTime.now().minusDays(4))
                .returnCount(0)
                .isOverdue(false)
                .build());
        addApproval(rejectedRequest, users.get("admin"), WorkflowStage.ADMIN_RECOMMENDATION,
                ApprovalAction.REJECT, "Request does not align with current spending priorities.", LocalDateTime.now().minusDays(4));
        addComment(rejectedRequest, users.get("admin"), "Rejected due to non-essential expenditure.", false);

        ProcurementRequest authorizedRequest = saveRequest(ProcurementRequest.builder()
                .title("Case management software subscription renewal")
                .description("Annual renewal of the case management platform used by legal aid teams.")
                .estimatedCost(new BigDecimal("5400.00"))
                .department(departments.get("it"))
                .justification("The platform is essential for tracking cases, appointments, and reporting.")
                .priority(RequestPriority.CRITICAL)
                .requiredByDate(LocalDate.now().plusDays(5))
                .requester(users.get("requesterOne"))
                .status(RequestStatus.AUTHORIZED)
                .currentStage(WorkflowStage.COMPLETED)
                .submittedAt(LocalDateTime.now().minusDays(8))
                .completedAt(LocalDateTime.now().minusDays(2))
                .returnCount(0)
                .isOverdue(false)
                .build());
        addApproval(authorizedRequest, users.get("admin"), WorkflowStage.ADMIN_RECOMMENDATION,
                ApprovalAction.RECOMMEND, "Recommended due to service continuity needs.", LocalDateTime.now().minusDays(7));
        addApproval(authorizedRequest, users.get("gm"), WorkflowStage.GM_APPROVAL,
                ApprovalAction.APPROVE, "Approved because the platform is mission critical.", LocalDateTime.now().minusDays(5));
        addApproval(authorizedRequest, users.get("ceo"), WorkflowStage.CEO_AUTHORIZATION,
                ApprovalAction.AUTHORIZE, "Authorized for immediate renewal.", LocalDateTime.now().minusDays(2));
        addComment(authorizedRequest, users.get("requesterOne"), "Vendor quote attached for records.", false);
        addAttachment(authorizedRequest, users.get("requesterOne"),
                "case-management-renewal-quote.pdf", "/uploads/case-management-renewal-quote.pdf",
                "application/pdf", 245_760L);

        ProcurementRequest overdueRequest = saveRequest(ProcurementRequest.builder()
                .title("Backup power units for reception and records office")
                .description("UPS units to protect reception and records operations during outages.")
                .estimatedCost(new BigDecimal("2100.00"))
                .department(departments.get("it"))
                .justification("Power instability is causing interruptions in client service and document handling.")
                .priority(RequestPriority.HIGH)
                .requiredByDate(LocalDate.now().minusDays(2))
                .requester(users.get("requesterTwo"))
                .status(RequestStatus.PENDING_GM_APPROVAL)
                .currentStage(WorkflowStage.GM_APPROVAL)
                .submittedAt(LocalDateTime.now().minusDays(9))
                .returnCount(0)
                .isOverdue(true)
                .overdueAt(LocalDateTime.now().minusDays(1))
                .build());
        addApproval(overdueRequest, users.get("admin"), WorkflowStage.ADMIN_RECOMMENDATION,
                ApprovalAction.RECOMMEND, "Recommended and escalated due to recurring outages.", LocalDateTime.now().minusDays(6));
        addComment(overdueRequest, users.get("admin"), "Marked urgent because the service window has already passed.", true);
    }

    private User buildUser(String firstName, String lastName, String email, String phoneNumber, Role role, String password) {
        return User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phoneNumber(phoneNumber)
                .password(password)
                .role(role)
                .isActive(true)
                .build();
    }

    private ProcurementRequest saveRequest(ProcurementRequest request) {
        return procurementRequestRepository.save(request);
    }

    private void addApproval(
            ProcurementRequest request,
            User approver,
            WorkflowStage stage,
            ApprovalAction action,
            String comment,
            LocalDateTime actionDate
    ) {
        requestApprovalRepository.save(RequestApproval.builder()
                .request(request)
                .approver(approver)
                .stage(stage)
                .action(action)
                .comment(comment)
                .actionDate(actionDate)
                .build());
    }

    private void addComment(ProcurementRequest request, User commenter, String comment, boolean isInternal) {
        requestCommentRepository.save(RequestComment.builder()
                .request(request)
                .commenter(commenter)
                .comment(comment)
                .isInternal(isInternal)
                .build());
    }

    private void addAttachment(
            ProcurementRequest request,
            User uploadedBy,
            String fileName,
            String filePath,
            String contentType,
            long fileSize
    ) {
        requestAttachmentRepository.save(RequestAttachment.builder()
                .request(request)
                .uploadedBy(uploadedBy)
                .fileName(fileName)
                .filePath(filePath)
                .contentType(contentType)
                .fileSize(fileSize)
                .build());
    }
}
