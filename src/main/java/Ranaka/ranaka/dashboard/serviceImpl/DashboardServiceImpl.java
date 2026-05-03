package Ranaka.ranaka.dashboard.serviceImpl;

import Ranaka.ranaka.common.enums.RequestPriority;
import Ranaka.ranaka.common.enums.RequestStatus;
import Ranaka.ranaka.common.enums.WorkflowStage;
import Ranaka.ranaka.dashboard.dto.*;
import Ranaka.ranaka.dashboard.service.DashboardService;
import Ranaka.ranaka.request.entity.ProcurementRequest;
import Ranaka.ranaka.request.repository.ProcurementRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ProcurementRequestRepository requestRepository;

    @Override
    public DashboardSummaryDto getDashboardSummary() {
        log.info("Generating overall dashboard summary");

        // This is the broad "system pulse" view used mainly by system-level oversight
        List<ProcurementRequest> allRequests = requestRepository.findAll();

        long totalRequests = allRequests.size();
        long pendingRequests = allRequests.stream()
                .filter(r -> r.getStatus().name().contains("PENDING"))
                .count();
        long overdueRequests = allRequests.stream()
                .filter(ProcurementRequest::getIsOverdue)
                .count();
        long completedRequests = allRequests.stream()
                .filter(this::isCompletedRequest)
                .count();
        long rejectedRequests = allRequests.stream()
                .filter(r -> r.getStatus() == RequestStatus.REJECTED)
                .count();
        long returnedRequests = allRequests.stream()
                .filter(r -> r.getStatus() == RequestStatus.RETURNED_FOR_CORRECTION)
                .count();

        double avgApprovalTime = calculateAverageApprovalTime(allRequests);

        return DashboardSummaryDto.builder()
                .totalRequests(totalRequests)
                .pendingRequests(pendingRequests)
                .overdueRequests(overdueRequests)
                .completedRequests(completedRequests)
                .rejectedRequests(rejectedRequests)
                .returnedRequests(returnedRequests)
                .avgApprovalTimeHours(avgApprovalTime)
                .generatedDate(LocalDate.now())
                .build();
    }

    @Override
    public List<RequestTrendDto> getRequestTrends(LocalDate startDate, LocalDate endDate) {
        log.info("Generating request trends from {} to {}", startDate, endDate);

        List<ProcurementRequest> requests = requestRepository.findAll();

        Map<LocalDate, RequestTrendDto> trendMap = new TreeMap<>();

        // We create one row per day so charting on the frontend stays simple and predictable
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;

            long submitted = requests.stream()
                    .filter(r -> r.getSubmittedAt() != null &&
                            r.getSubmittedAt().toLocalDate().equals(currentDate))
                    .count();

            long completed = requests.stream()
                    .filter(r -> r.getCompletedAt() != null &&
                            r.getCompletedAt().toLocalDate().equals(currentDate))
                    .count();

            long rejected = requests.stream()
                    .filter(r -> r.getStatus() == RequestStatus.REJECTED &&
                            r.getUpdatedAt().toLocalDate().equals(currentDate))
                    .count();

            long returned = requests.stream()
                    .filter(r -> r.getStatus() == RequestStatus.RETURNED_FOR_CORRECTION &&
                            r.getUpdatedAt().toLocalDate().equals(currentDate))
                    .count();

            RequestTrendDto trend = RequestTrendDto.builder()
                    .date(currentDate)
                    .submittedCount(submitted)
                    .completedCount(completed)
                    .rejectedCount(rejected)
                    .returnedCount(returned)
                    .build();

            trendMap.put(currentDate, trend);
        }

        return new ArrayList<>(trendMap.values());

    }

    @Override
    public PriorityDistributionDto getPriorityDistribution() {

        log.info("Calculating priority distribution");

        List<ProcurementRequest> requests = requestRepository.findAll();

        long criticalCount = requests.stream()
                .filter(r -> r.getPriority() == RequestPriority.CRITICAL)
                .count();

        long highCount = requests.stream()
                .filter(r -> r.getPriority() == RequestPriority.HIGH)
                .count();

        long mediumCount = requests.stream()
                .filter(r -> r.getPriority() == RequestPriority.MEDIUM)
                .count();

        long lowCount = requests.stream()
                .filter(r -> r.getPriority() == RequestPriority.LOW)
                .count();

        return PriorityDistributionDto.builder()
                .criticalCount(criticalCount)
                .highCount(highCount)
                .mediumCount(mediumCount)
                .lowCount(lowCount)
                .build();
    }

    @Override
    public List<DepartmentStatsDto> getDepartmentStats() {
        log.info("Calculating department statistics");

        List<ProcurementRequest> requests = requestRepository.findAll();

        return requests.stream()
                .collect(Collectors.groupingBy(r -> r.getDepartment().getName()))
                .entrySet()
                .stream()
                .map(entry -> {
                    String deptName = entry.getKey();
                    List<ProcurementRequest> deptRequests = entry.getValue();

                    long total = deptRequests.size();
                    long pending = deptRequests.stream()
                            .filter(r -> r.getStatus().name().contains("PENDING"))
                            .count();
                    long completed = deptRequests.stream()
                            .filter(this::isCompletedRequest)
                            .count();
                    long rejected = deptRequests.stream()
                            .filter(r -> r.getStatus() == RequestStatus.REJECTED)
                            .count();
                    double avgTime = calculateAverageApprovalTime(deptRequests);

                    return DepartmentStatsDto.builder()
                            .departmentName(deptName)
                            .totalRequests(total)
                            .pendingRequests(pending)
                            .completedRequests(completed)
                            .rejectedRequests(rejected)
                            .avgApprovalTime(avgTime)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<StagePerformanceDto> getStagePerformance() {
        log.info("Calculating stage performance metrics");

        List<ProcurementRequest> requests = requestRepository.findAll();

        return Arrays.stream(WorkflowStage.values())
                .filter(stage -> !stage.equals(WorkflowStage.DRAFT) && !stage.equals(WorkflowStage.COMPLETED))
                .map(stage -> {
                    List<ProcurementRequest> stageRequests = requests.stream()
                            .filter(r -> r.getCurrentStage() == stage)
                            .collect(Collectors.toList());

                    long pending = stageRequests.size();
                    long completed = requests.stream()
                            .filter(r -> isCompletedRequest(r) &&
                                    r.getCurrentStage() != stage)
                            .count();

                    double avgTime = calculateAverageApprovalTime(stageRequests);

                    return StagePerformanceDto.builder()
                            .stageName(stage.name())
                            .pendingCount(pending)
                            .completedCount(completed)
                            .avgProcessingTimeHours(avgTime)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public OverdueSummaryDto getOverdueSummary() {
        log.info("Calculating overdue requests summary");

        List<ProcurementRequest> overdueRequests = requestRepository.findAll().stream()
                .filter(ProcurementRequest::getIsOverdue)
                .collect(Collectors.toList());

        long adminOverdue = overdueRequests.stream()
                .filter(r -> r.getCurrentStage() == WorkflowStage.ADMIN_RECOMMENDATION)
                .count();

        long gmOverdue = overdueRequests.stream()
                .filter(r -> r.getCurrentStage() == WorkflowStage.GM_APPROVAL)
                .count();

        long ceoOverdue = overdueRequests.stream()
                .filter(r -> r.getCurrentStage() == WorkflowStage.CEO_AUTHORIZATION)
                .count();

        long criticalOverdue = overdueRequests.stream()
                .filter(r -> r.getPriority() == RequestPriority.CRITICAL)
                .count();

        return OverdueSummaryDto.builder()
                .totalOverdueRequests((long) overdueRequests.size())
                .adminOverdueCount(adminOverdue)
                .gmOverdueCount(gmOverdue)
                .ceoOverdueCount(ceoOverdue)
                .criticalOverdueCount(criticalOverdue)
                .build();
    }

    @Override
    public DashboardSummaryDto getRequesterDashboard(Long userId) {
        log.info("Generating requester dashboard for user ID: {}", userId);

        // This dashboard is intentionally personal:
        // "What is happening to my requests?" rather than system-wide workload.
        List<ProcurementRequest> userRequests = requestRepository.findByRequesterId(userId);

        long total = userRequests.size();
        long pending = userRequests.stream()
                .filter(r -> r.getStatus().name().contains("PENDING"))
                .count();
        long returned = userRequests.stream()
                .filter(r -> r.getStatus() == RequestStatus.RETURNED_FOR_CORRECTION)
                .count();
        long approved = userRequests.stream()
                .filter(this::isCompletedRequest)
                .count();
        long rejected = userRequests.stream()
                .filter(r -> r.getStatus() == RequestStatus.REJECTED)
                .count();

        return DashboardSummaryDto.builder()
                .totalRequests(total)
                .pendingRequests(pending)
                .returnedRequests(returned)
                .completedRequests(approved)
                .rejectedRequests(rejected)
                .generatedDate(LocalDate.now())
                .build();
    }

    @Override
    public DashboardSummaryDto getAdminDashboard() {
        log.info("Generating admin dashboard");

        // The admin dashboard is queue-focused:
        // what is waiting, what is overdue, and what is critical right now.
        List<ProcurementRequest> adminRequests = requestRepository.findByCurrentStage(WorkflowStage.ADMIN_RECOMMENDATION);

        long waiting = adminRequests.size();
        long overdue = adminRequests.stream()
                .filter(ProcurementRequest::getIsOverdue)
                .count();
        long critical = adminRequests.stream()
                .filter(r -> r.getPriority() == RequestPriority.CRITICAL)
                .count();

        return DashboardSummaryDto.builder()
                .totalRequests(waiting)
                .overdueRequests(overdue)
                .pendingRequests(critical)
                .generatedDate(LocalDate.now())
                .build();
    }

    @Override
    public DashboardSummaryDto getGmDashboard() {
        log.info("Generating GM dashboard");

        List<ProcurementRequest> gmRequests = requestRepository.findByCurrentStage(WorkflowStage.GM_APPROVAL);

        long waiting = gmRequests.size();
        long overdue = gmRequests.stream()
                .filter(ProcurementRequest::getIsOverdue)
                .count();
        long critical = gmRequests.stream()
                .filter(r -> r.getPriority() == RequestPriority.CRITICAL)
                .count();

        return DashboardSummaryDto.builder()
                .totalRequests(waiting)
                .overdueRequests(overdue)
                .pendingRequests(critical)
                .generatedDate(LocalDate.now())
                .build();
    }

    @Override
    public DashboardSummaryDto getCeoDashboard() {
        log.info("Generating CEO dashboard");

        List<ProcurementRequest> ceoRequests = requestRepository.findByCurrentStage(WorkflowStage.CEO_AUTHORIZATION);

        long waiting = ceoRequests.size();
        long overdue = ceoRequests.stream()
                .filter(ProcurementRequest::getIsOverdue)
                .count();
        // High-cost requests are highlighted here because they usually need more executive attention.
        long highCost = ceoRequests.stream()
                .filter(r -> r.getEstimatedCost() != null && r.getEstimatedCost().doubleValue() > 10000)
                .count();

        return DashboardSummaryDto.builder()
                .totalRequests(waiting)
                .overdueRequests(overdue)
                .pendingRequests(highCost)
                .generatedDate(LocalDate.now())
                .build();
    }

    @Override
    public DashboardSummaryDto getSystemAdminDashboard() {
        log.info("Generating system admin dashboard");

        return getDashboardSummary();
    }

    /**
     * Calculate average approval time in hours for a list of requests
     */
    private double calculateAverageApprovalTime(List<ProcurementRequest> requests) {
        if (requests.isEmpty()) {
            return 0.0;
        }

        double totalHours = 0;
        int count = 0;

        for (ProcurementRequest request : requests) {
            if (request.getSubmittedAt() != null && request.getCompletedAt() != null) {
                long durationInMinutes = java.time.temporal.ChronoUnit.MINUTES
                        .between(request.getSubmittedAt(), request.getCompletedAt());
                totalHours += durationInMinutes / 60.0;
                count++;
            }
        }

        return count > 0 ? totalHours / count : 0.0;
    }

    private boolean isCompletedRequest(ProcurementRequest request) {
        return request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.AUTHORIZED;
    }
}
