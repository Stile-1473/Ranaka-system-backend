package Ranaka.ranaka.report.service;

import Ranaka.ranaka.report.dto.ApprovalTimeReportDto;
import Ranaka.ranaka.report.dto.BottleneckReportDto;
import Ranaka.ranaka.report.dto.DepartmentUsageReportDto;
import Ranaka.ranaka.report.dto.OverdueRequestReportDto;
import Ranaka.ranaka.report.dto.ReportFilterDto;
import Ranaka.ranaka.report.dto.ReturnsRejectionsReportDto;

import java.util.List;

public interface ReportService {

    ApprovalTimeReportDto getApprovalTimesReport(ReportFilterDto filter);

    List<BottleneckReportDto> getBottlenecksReport(ReportFilterDto filter);

    List<DepartmentUsageReportDto> getDepartmentUsageReport(ReportFilterDto filter);

    ReturnsRejectionsReportDto getReturnsRejectionsReport(ReportFilterDto filter);

    List<OverdueRequestReportDto> getOverdueRequestsReport(ReportFilterDto filter);

    byte[] exportReport(String type, ReportFilterDto filter);
}
