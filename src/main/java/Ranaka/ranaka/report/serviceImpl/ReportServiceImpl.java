package Ranaka.ranaka.report.serviceImpl;

import Ranaka.ranaka.common.enums.ApprovalAction;
import Ranaka.ranaka.common.enums.RequestStatus;
import Ranaka.ranaka.common.enums.WorkflowStage;
import Ranaka.ranaka.report.dto.ApprovalTimeReportDto;
import Ranaka.ranaka.report.dto.BottleneckReportDto;
import Ranaka.ranaka.report.dto.DepartmentUsageReportDto;
import Ranaka.ranaka.report.dto.OverdueRequestReportDto;
import Ranaka.ranaka.report.dto.ReportFilterDto;
import Ranaka.ranaka.report.dto.ReturnsRejectionsReportDto;
import Ranaka.ranaka.report.service.ReportService;
import Ranaka.ranaka.request.entity.ProcurementRequest;
import Ranaka.ranaka.request.entity.RequestApproval;
import Ranaka.ranaka.request.repository.ProcurementRequestRepository;
import Ranaka.ranaka.request.repository.RequestApprovalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ProcurementRequestRepository requestRepository;
    private final RequestApprovalRepository approvalRepository;

    @Override
    public ApprovalTimeReportDto getApprovalTimesReport(ReportFilterDto filter) {
        // We only calculate approval timing from requests that actually reached the finish line.
        List<ProcurementRequest> filteredRequests = getFilteredRequests(filter).stream()
                .filter(this::isCompletedRequest)
                .toList();

        List<Double> adminDurations = new ArrayList<>();
        List<Double> gmDurations = new ArrayList<>();
        List<Double> ceoDurations = new ArrayList<>();
        List<Double> overallDurations = new ArrayList<>();

        for (ProcurementRequest request : filteredRequests) {
            List<RequestApproval> approvals = getApprovalsChronologically(request.getId());
            RequestApproval adminRecommendation = findApproval(approvals, ApprovalAction.RECOMMEND);
            RequestApproval gmApproval = findApproval(approvals, ApprovalAction.APPROVE);
            RequestApproval ceoAuthorization = findApproval(approvals, ApprovalAction.AUTHORIZE);

            // Human example:
            // submitted Monday 09:00 -> recommended Monday 13:00
            // means the Admin part took about 4 hours.
            if (request.getSubmittedAt() != null && adminRecommendation != null) {
                adminDurations.add(hoursBetween(request.getSubmittedAt(), getApprovalTimestamp(adminRecommendation)));
            }
            if (adminRecommendation != null && gmApproval != null) {
                gmDurations.add(hoursBetween(getApprovalTimestamp(adminRecommendation), getApprovalTimestamp(gmApproval)));
            }
            if (gmApproval != null && ceoAuthorization != null) {
                ceoDurations.add(hoursBetween(getApprovalTimestamp(gmApproval), getApprovalTimestamp(ceoAuthorization)));
            }
            if (request.getSubmittedAt() != null && request.getCompletedAt() != null) {
                overallDurations.add(hoursBetween(request.getSubmittedAt(), request.getCompletedAt()));
            }
        }

        return ApprovalTimeReportDto.builder()
                .startDate(resolveStartDate(filter))
                .endDate(resolveEndDate(filter))
                .completedRequestCount((long) filteredRequests.size())
                .overallAverageHours(average(overallDurations))
                .adminAverageHours(average(adminDurations))
                .gmAverageHours(average(gmDurations))
                .ceoAverageHours(average(ceoDurations))
                .build();
    }

    @Override
    public List<BottleneckReportDto> getBottlenecksReport(ReportFilterDto filter) {
        List<ProcurementRequest> filteredRequests = getFilteredRequests(filter);
        List<RequestApproval> allApprovals = approvalRepository.findAll();

        // We rank stages by observed processing time so leadership can quickly see
        // where requests are slowing down the most.
        return List.of(
                buildStageBottleneck(filteredRequests, allApprovals, WorkflowStage.ADMIN_RECOMMENDATION, ApprovalAction.RECOMMEND),
                buildStageBottleneck(filteredRequests, allApprovals, WorkflowStage.GM_APPROVAL, ApprovalAction.APPROVE),
                buildStageBottleneck(filteredRequests, allApprovals, WorkflowStage.CEO_AUTHORIZATION, ApprovalAction.AUTHORIZE)
        ).stream()
                .sorted(Comparator.comparing(BottleneckReportDto::getAverageProcessingHours, Comparator.nullsLast(Double::compareTo)).reversed())
                .toList();
    }

    @Override
    public List<DepartmentUsageReportDto> getDepartmentUsageReport(ReportFilterDto filter) {
        List<ProcurementRequest> filteredRequests = getFilteredRequests(filter);

        return filteredRequests.stream()
                .collect(Collectors.groupingBy(ProcurementRequest::getDepartment))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<ProcurementRequest> requests = entry.getValue();
                    BigDecimal totalEstimatedCost = requests.stream()
                            .map(request -> request.getEstimatedCost() != null ? request.getEstimatedCost() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal averageEstimatedCost = requests.isEmpty()
                            ? BigDecimal.ZERO
                            : totalEstimatedCost.divide(BigDecimal.valueOf(requests.size()), 2, java.math.RoundingMode.HALF_UP);

                    return DepartmentUsageReportDto.builder()
                            .departmentId(entry.getKey().getId())
                            .departmentName(entry.getKey().getName())
                            .totalRequests((long) requests.size())
                            .completedRequests(requests.stream().filter(this::isCompletedRequest).count())
                            .rejectedRequests(requests.stream().filter(request -> request.getStatus() == RequestStatus.REJECTED).count())
                            .returnedRequests(requests.stream().filter(request -> request.getStatus() == RequestStatus.RETURNED_FOR_CORRECTION).count())
                            .overdueRequests(requests.stream().filter(request -> Boolean.TRUE.equals(request.getIsOverdue())).count())
                            .totalEstimatedCost(totalEstimatedCost)
                            .averageEstimatedCost(averageEstimatedCost)
                            .build();
                })
                .sorted(Comparator.comparing(DepartmentUsageReportDto::getTotalRequests).reversed())
                .toList();
    }

    @Override
    public ReturnsRejectionsReportDto getReturnsRejectionsReport(ReportFilterDto filter) {
        List<ProcurementRequest> filteredRequests = getFilteredRequests(filter);
        long totalRequests = filteredRequests.size();
        long returnedRequests = filteredRequests.stream()
                .filter(request -> request.getStatus() == RequestStatus.RETURNED_FOR_CORRECTION
                        || (request.getReturnCount() != null && request.getReturnCount() > 0))
                .count();
        long rejectedRequests = filteredRequests.stream()
                .filter(request -> request.getStatus() == RequestStatus.REJECTED)
                .count();

        return ReturnsRejectionsReportDto.builder()
                .totalRequests(totalRequests)
                .returnedRequests(returnedRequests)
                .rejectedRequests(rejectedRequests)
                .returnRatePercent(toPercentage(returnedRequests, totalRequests))
                .rejectionRatePercent(toPercentage(rejectedRequests, totalRequests))
                .build();
    }

    @Override
    public List<OverdueRequestReportDto> getOverdueRequestsReport(ReportFilterDto filter) {
        return getFilteredRequests(filter).stream()
                .filter(request -> Boolean.TRUE.equals(request.getIsOverdue()))
                .sorted(Comparator.comparing(ProcurementRequest::getOverdueAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .map(this::toOverdueDto)
                .toList();
    }

    @Override
    public byte[] exportReport(String type, ReportFilterDto filter) {
        String normalizedType = type == null ? "" : type.trim().toLowerCase();
        // CSV is the easiest MVP export format because users can open it in Excel immediately.
        String csv = switch (normalizedType) {
            case "approval-times" -> exportApprovalTimesCsv(filter);
            case "bottlenecks" -> exportBottlenecksCsv(filter);
            case "department-usage" -> exportDepartmentUsageCsv(filter);
            case "returns-rejections" -> exportReturnsRejectionsCsv(filter);
            case "overdue-requests" -> exportOverdueRequestsCsv(filter);
            default -> throw new IllegalArgumentException("Unsupported report type: " + type);
        };
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    private List<ProcurementRequest> getFilteredRequests(ReportFilterDto filter) {
        ReportFilterDto effectiveFilter = filter != null ? filter : new ReportFilterDto();
        LocalDate startDate = resolveStartDate(effectiveFilter);
        LocalDate endDate = resolveEndDate(effectiveFilter);

        // Reports are built from the same request source of truth used by the rest of the app.
        return requestRepository.findAll().stream()
                .filter(request -> matchesDateRange(request, startDate, endDate))
                .filter(request -> effectiveFilter.getDepartmentId() == null
                        || request.getDepartment().getId().equals(effectiveFilter.getDepartmentId()))
                .filter(request -> effectiveFilter.getPriority() == null
                        || request.getPriority() == effectiveFilter.getPriority())
                .filter(request -> effectiveFilter.getStatus() == null
                        || request.getStatus() == effectiveFilter.getStatus())
                .filter(request -> effectiveFilter.getOverdueOnly() == null
                        || !effectiveFilter.getOverdueOnly()
                        || Boolean.TRUE.equals(request.getIsOverdue()))
                .toList();
    }

    private boolean matchesDateRange(ProcurementRequest request, LocalDate startDate, LocalDate endDate) {
        LocalDate referenceDate = resolveReferenceDate(request);
        return (referenceDate.isEqual(startDate) || referenceDate.isAfter(startDate))
                && (referenceDate.isEqual(endDate) || referenceDate.isBefore(endDate));
    }

    private LocalDate resolveReferenceDate(ProcurementRequest request) {
        if (request.getSubmittedAt() != null) {
            return request.getSubmittedAt().toLocalDate();
        }
        return request.getCreatedAt().toLocalDate();
    }

    private LocalDate resolveStartDate(ReportFilterDto filter) {
        if (filter != null && filter.getStartDate() != null) {
            return filter.getStartDate();
        }
        return LocalDate.now().minusDays(29);
    }

    private LocalDate resolveEndDate(ReportFilterDto filter) {
        if (filter != null && filter.getEndDate() != null) {
            return filter.getEndDate();
        }
        return LocalDate.now();
    }

    private BottleneckReportDto buildStageBottleneck(List<ProcurementRequest> requests,
                                                     List<RequestApproval> allApprovals,
                                                     WorkflowStage stage,
                                                     ApprovalAction completionAction) {
        List<ProcurementRequest> stageRelevantRequests = requests.stream()
                .filter(request -> hasStageActivity(request, allApprovals, stage, completionAction))
                .toList();

        List<Double> stageDurations = stageRelevantRequests.stream()
                .map(request -> calculateStageDurationHours(request, allApprovals, stage, completionAction))
                .filter(duration -> duration != null)
                .toList();

        return BottleneckReportDto.builder()
                .stageName(stage.name())
                .requestCount((long) stageRelevantRequests.size())
                .pendingCount(requests.stream().filter(request -> request.getCurrentStage() == stage).count())
                .overdueCount(requests.stream()
                        .filter(request -> request.getCurrentStage() == stage && Boolean.TRUE.equals(request.getIsOverdue()))
                        .count())
                .averageProcessingHours(average(stageDurations))
                .maxProcessingHours(max(stageDurations))
                .build();
    }

    private boolean hasStageActivity(ProcurementRequest request,
                                     List<RequestApproval> allApprovals,
                                     WorkflowStage stage,
                                     ApprovalAction completionAction) {
        if (request.getCurrentStage() == stage) {
            return true;
        }
        return allApprovals.stream()
                .anyMatch(approval -> approval.getRequest().getId().equals(request.getId())
                        && approval.getStage() == stage
                        && approval.getAction() == completionAction);
    }

    private Double calculateStageDurationHours(ProcurementRequest request,
                                               List<RequestApproval> allApprovals,
                                               WorkflowStage stage,
                                               ApprovalAction completionAction) {
        List<RequestApproval> approvals = allApprovals.stream()
                .filter(approval -> approval.getRequest().getId().equals(request.getId()))
                .sorted(Comparator.comparing(this::getApprovalTimestamp))
                .toList();

        LocalDateTime start = switch (stage) {
            case ADMIN_RECOMMENDATION -> request.getSubmittedAt();
            case GM_APPROVAL -> {
                RequestApproval adminRecommendation = findApproval(approvals, ApprovalAction.RECOMMEND);
                yield adminRecommendation != null ? getApprovalTimestamp(adminRecommendation) : null;
            }
            case CEO_AUTHORIZATION -> {
                RequestApproval gmApproval = findApproval(approvals, ApprovalAction.APPROVE);
                yield gmApproval != null ? getApprovalTimestamp(gmApproval) : null;
            }
            default -> null;
        };

        RequestApproval completion = approvals.stream()
                .filter(approval -> approval.getStage() == stage && approval.getAction() == completionAction)
                .findFirst()
                .orElse(null);

        if (start == null || completion == null) {
            return null;
        }

        return hoursBetween(start, getApprovalTimestamp(completion));
    }

    private List<RequestApproval> getApprovalsChronologically(Long requestId) {
        return approvalRepository.findByRequestIdOrderByCreatedAtDesc(requestId).stream()
                .sorted(Comparator.comparing(this::getApprovalTimestamp))
                .toList();
    }

    private RequestApproval findApproval(List<RequestApproval> approvals, ApprovalAction action) {
        return approvals.stream()
                .filter(approval -> approval.getAction() == action)
                .findFirst()
                .orElse(null);
    }

    private LocalDateTime getApprovalTimestamp(RequestApproval approval) {
        return approval.getActionDate() != null ? approval.getActionDate() : approval.getCreatedAt();
    }

    private boolean isCompletedRequest(ProcurementRequest request) {
        return request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.AUTHORIZED;
    }

    private Double hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) {
            return null;
        }
        long minutes = Duration.between(start, end).toMinutes();
        return minutes / 60.0;
    }

    private Double average(List<Double> values) {
        return values.isEmpty() ? 0.0 : values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private Double max(List<Double> values) {
        return values.isEmpty() ? 0.0 : values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    private Double toPercentage(long value, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round(((value * 100.0) / total) * 100.0) / 100.0;
    }

    private OverdueRequestReportDto toOverdueDto(ProcurementRequest request) {
        return OverdueRequestReportDto.builder()
                .requestId(request.getId())
                .title(request.getTitle())
                .requesterName(request.getRequester().getFirstName() + " " + request.getRequester().getLastName())
                .departmentName(request.getDepartment().getName())
                .priority(request.getPriority())
                .status(request.getStatus())
                .currentStage(request.getCurrentStage())
                .estimatedCost(request.getEstimatedCost())
                .requiredByDate(request.getRequiredByDate())
                .submittedAt(request.getSubmittedAt())
                .overdueAt(request.getOverdueAt())
                // Example meaning:
                // if a request has been sitting since yesterday morning, ageHours shows that delay in a simple number.
                .ageHours(request.getSubmittedAt() != null
                        ? Duration.between(request.getSubmittedAt(), LocalDateTime.now()).toHours()
                        : 0L)
                .build();
    }

    private String exportApprovalTimesCsv(ReportFilterDto filter) {
        ApprovalTimeReportDto report = getApprovalTimesReport(filter);
        return new StringBuilder()
                .append("startDate,endDate,completedRequestCount,overallAverageHours,adminAverageHours,gmAverageHours,ceoAverageHours\n")
                .append(csv(report.getStartDate()))
                .append(",")
                .append(csv(report.getEndDate()))
                .append(",")
                .append(report.getCompletedRequestCount())
                .append(",")
                .append(report.getOverallAverageHours())
                .append(",")
                .append(report.getAdminAverageHours())
                .append(",")
                .append(report.getGmAverageHours())
                .append(",")
                .append(report.getCeoAverageHours())
                .append("\n")
                .toString();
    }

    private String exportBottlenecksCsv(ReportFilterDto filter) {
        StringBuilder csvBuilder = new StringBuilder("stageName,requestCount,pendingCount,overdueCount,averageProcessingHours,maxProcessingHours\n");
        for (BottleneckReportDto row : getBottlenecksReport(filter)) {
            csvBuilder.append(csv(row.getStageName())).append(",")
                    .append(row.getRequestCount()).append(",")
                    .append(row.getPendingCount()).append(",")
                    .append(row.getOverdueCount()).append(",")
                    .append(row.getAverageProcessingHours()).append(",")
                    .append(row.getMaxProcessingHours()).append("\n");
        }
        return csvBuilder.toString();
    }

    private String exportDepartmentUsageCsv(ReportFilterDto filter) {
        StringBuilder csvBuilder = new StringBuilder("departmentId,departmentName,totalRequests,completedRequests,rejectedRequests,returnedRequests,overdueRequests,totalEstimatedCost,averageEstimatedCost\n");
        for (DepartmentUsageReportDto row : getDepartmentUsageReport(filter)) {
            csvBuilder.append(row.getDepartmentId()).append(",")
                    .append(csv(row.getDepartmentName())).append(",")
                    .append(row.getTotalRequests()).append(",")
                    .append(row.getCompletedRequests()).append(",")
                    .append(row.getRejectedRequests()).append(",")
                    .append(row.getReturnedRequests()).append(",")
                    .append(row.getOverdueRequests()).append(",")
                    .append(row.getTotalEstimatedCost()).append(",")
                    .append(row.getAverageEstimatedCost()).append("\n");
        }
        return csvBuilder.toString();
    }

    private String exportReturnsRejectionsCsv(ReportFilterDto filter) {
        ReturnsRejectionsReportDto report = getReturnsRejectionsReport(filter);
        return new StringBuilder()
                .append("totalRequests,returnedRequests,rejectedRequests,returnRatePercent,rejectionRatePercent\n")
                .append(report.getTotalRequests()).append(",")
                .append(report.getReturnedRequests()).append(",")
                .append(report.getRejectedRequests()).append(",")
                .append(report.getReturnRatePercent()).append(",")
                .append(report.getRejectionRatePercent()).append("\n")
                .toString();
    }

    private String exportOverdueRequestsCsv(ReportFilterDto filter) {
        StringBuilder csvBuilder = new StringBuilder("requestId,title,requesterName,departmentName,priority,status,currentStage,estimatedCost,requiredByDate,submittedAt,overdueAt,ageHours\n");
        for (OverdueRequestReportDto row : getOverdueRequestsReport(filter)) {
            csvBuilder.append(row.getRequestId()).append(",")
                    .append(csv(row.getTitle())).append(",")
                    .append(csv(row.getRequesterName())).append(",")
                    .append(csv(row.getDepartmentName())).append(",")
                    .append(csv(row.getPriority())).append(",")
                    .append(csv(row.getStatus())).append(",")
                    .append(csv(row.getCurrentStage())).append(",")
                    .append(row.getEstimatedCost()).append(",")
                    .append(csv(row.getRequiredByDate())).append(",")
                    .append(csv(row.getSubmittedAt())).append(",")
                    .append(csv(row.getOverdueAt())).append(",")
                    .append(row.getAgeHours()).append("\n");
        }
        return csvBuilder.toString();
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
