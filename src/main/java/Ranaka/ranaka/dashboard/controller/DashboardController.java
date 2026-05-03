package Ranaka.ranaka.dashboard.controller;

import Ranaka.ranaka.dashboard.dto.DashboardSummaryDto;
import Ranaka.ranaka.dashboard.dto.DepartmentStatsDto;
import Ranaka.ranaka.dashboard.dto.OverdueSummaryDto;
import Ranaka.ranaka.dashboard.dto.PriorityDistributionDto;
import Ranaka.ranaka.dashboard.dto.RequestTrendDto;
import Ranaka.ranaka.dashboard.dto.StagePerformanceDto;
import Ranaka.ranaka.dashboard.service.DashboardService;
import Ranaka.ranaka.user.domain.Role;
import Ranaka.ranaka.user.entity.User;
import Ranaka.ranaka.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary() {
        User currentUser = getCurrentUser();
        // The same endpoint adapts to the current role.
        // Example: a requester sees "my requests", while a CEO sees executive queue numbers.
        DashboardSummaryDto response = switch (currentUser.getRole()) {
            case REQUESTER -> dashboardService.getRequesterDashboard(currentUser.getId());
            case ADMIN -> dashboardService.getAdminDashboard();
            case GM -> dashboardService.getGmDashboard();
            case CEO -> dashboardService.getCeoDashboard();
            case SYSTEM_ADMIN -> dashboardService.getSystemAdminDashboard();
        };

        return ResponseEntity.ok(response);
    }

    @GetMapping("/request-trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'GM', 'CEO', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<RequestTrendDto>> getRequestTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(29);
        return ResponseEntity.ok(dashboardService.getRequestTrends(effectiveStart, effectiveEnd));
    }

    @GetMapping("/priority-distribution")
    @PreAuthorize("hasAnyRole('ADMIN', 'GM', 'CEO', 'SYSTEM_ADMIN')")
    public ResponseEntity<PriorityDistributionDto> getPriorityDistribution() {
        return ResponseEntity.ok(dashboardService.getPriorityDistribution());
    }

    @GetMapping("/department-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'GM', 'CEO', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<DepartmentStatsDto>> getDepartmentStats() {
        return ResponseEntity.ok(dashboardService.getDepartmentStats());
    }

    @GetMapping("/stage-performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'GM', 'CEO', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<StagePerformanceDto>> getStagePerformance() {
        return ResponseEntity.ok(dashboardService.getStagePerformance());
    }

    @GetMapping("/overdue-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'GM', 'CEO', 'SYSTEM_ADMIN')")
    public ResponseEntity<OverdueSummaryDto> getOverdueSummary() {
        return ResponseEntity.ok(dashboardService.getOverdueSummary());
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        // Just like auth services, the principal may be our entity directly or a UserDetails wrapper.
        if (principal instanceof User user) {
            return user;
        }

        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }
}
