package com.inventory.service;

import com.inventory.dto.ReportDTO;
import java.time.LocalDate;

public interface ReportService {
    ReportDTO.TimePeriodReport getDailyReport();
    ReportDTO.TimePeriodReport getWeeklyReport();
    ReportDTO.TimePeriodReport getMonthlyReport();
    ReportDTO.TimePeriodReport getYearlyReport();
    ReportDTO.TimePeriodReport getCustomReport(LocalDate startDate, LocalDate endDate);
    ReportDTO.TimePeriodReport getCategoryReport(String category);
}