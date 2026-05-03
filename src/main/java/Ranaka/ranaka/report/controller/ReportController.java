package Ranaka.ranaka.report.controller;

import Ranaka.ranaka.report.dto.ApprovalTimeReportDto;
import Ranaka.ranaka.report.dto.BottleneckReportDto;
import Ranaka.ranaka.report.dto.DepartmentUsageReportDto;
import Ranaka.ranaka.report.dto.OverdueRequestReportDto;
import Ranaka.ranaka.report.dto.ReportFilterDto;
import Ranaka.ranaka.report.dto.ReturnsRejectionsReportDto;
import Ranaka.ranaka.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'GM', 'CEO', 'SYSTEM_ADMIN')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/approval-times")
    public ResponseEntity<ApprovalTimeReportDto> getApprovalTimes(ReportFilterDto filter) {
        return ResponseEntity.ok(reportService.getApprovalTimesReport(filter));
    }

    @GetMapping("/bottlenecks")
    public ResponseEntity<List<BottleneckReportDto>> getBottlenecks(ReportFilterDto filter) {
        return ResponseEntity.ok(reportService.getBottlenecksReport(filter));
    }

    @GetMapping("/department-usage")
    public ResponseEntity<List<DepartmentUsageReportDto>> getDepartmentUsage(ReportFilterDto filter) {
        return ResponseEntity.ok(reportService.getDepartmentUsageReport(filter));
    }

    @GetMapping("/returns-rejections")
    public ResponseEntity<ReturnsRejectionsReportDto> getReturnsRejections(ReportFilterDto filter) {
        return ResponseEntity.ok(reportService.getReturnsRejectionsReport(filter));
    }

    @GetMapping("/overdue-requests")
    public ResponseEntity<List<OverdueRequestReportDto>> getOverdueRequests(ReportFilterDto filter) {
        return ResponseEntity.ok(reportService.getOverdueRequestsReport(filter));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam String type,
            ReportFilterDto filter) {
        byte[] body = reportService.exportReport(type, filter);
        String filename = type.toLowerCase() + "-report.csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
