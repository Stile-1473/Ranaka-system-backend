package Ranaka.ranaka.dashboard.service;

import Ranaka.ranaka.dashboard.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {

    /**
     * Get overall dashboard summary
     */
    DashboardSummaryDto getDashboardSummary();

    /**
     * Get request trends for a date range
     */
    List<RequestTrendDto> getRequestTrends(LocalDate startDate, LocalDate endDate);

    /**
     * Get priority distribution across all requests
     */
    PriorityDistributionDto getPriorityDistribution();

    /**
     * Get statistics by department
     */
    List<DepartmentStatsDto> getDepartmentStats();

    /**
     * Get stage performance metrics
     */
    List<StagePerformanceDto> getStagePerformance();

    /**
     * Get overdue requests summary
     */
    OverdueSummaryDto getOverdueSummary();

    /**
     * Get requester dashboard (for REQUESTER role)
     */
    DashboardSummaryDto getRequesterDashboard(Long userId);

    /**
     * Get admin dashboard (for ADMIN role)
     */
    DashboardSummaryDto getAdminDashboard();

    /**
     * Get GM dashboard (for GM role)
     */
    DashboardSummaryDto getGmDashboard();

    /**
     * Get CEO dashboard (for CEO role)
     */
    DashboardSummaryDto getCeoDashboard();

    /**
     * Get system admin dashboard (for SYSTEM_ADMIN role)
     */
    DashboardSummaryDto getSystemAdminDashboard();
}
