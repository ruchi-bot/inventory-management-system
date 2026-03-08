package com.inventory.service.impl;

import com.inventory.dto.ReportDTO;
import com.inventory.entity.TransactionType;
import com.inventory.exception.ValidationException;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.StockTransactionRepository;
import com.inventory.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {
    
    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;
    
    public ReportServiceImpl(ProductRepository productRepository,
                           StockTransactionRepository stockTransactionRepository) {
        this.productRepository = productRepository;
        this.stockTransactionRepository = stockTransactionRepository;
    }
    
    @Override
    public ReportDTO.TimePeriodReport getDailyReport() {
        LocalDate today = LocalDate.now();
        return generateReportForDateRange(today, today);
    }
    
    @Override
    public ReportDTO.TimePeriodReport getWeeklyReport() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        return generateReportForDateRange(startOfWeek, endOfWeek);
    }
    
    @Override
    public ReportDTO.TimePeriodReport getMonthlyReport() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());
        return generateReportForDateRange(startOfMonth, endOfMonth);
    }
    
    @Override
    public ReportDTO.TimePeriodReport getYearlyReport() {
        LocalDate today = LocalDate.now();
        LocalDate startOfYear = today.with(TemporalAdjusters.firstDayOfYear());
        LocalDate endOfYear = today.with(TemporalAdjusters.lastDayOfYear());
        return generateReportForDateRange(startOfYear, endOfYear);
    }
    
    @Override
    public ReportDTO.TimePeriodReport getCustomReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date cannot be after end date");
        }
        
        if (startDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Start date cannot be in the future");
        }
        
        return generateReportForDateRange(startDate, endDate);
    }
    
    @Override
    public ReportDTO.TimePeriodReport getCategoryReport(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new ValidationException("Category is required");
        }
        
        ReportDTO.TimePeriodReport report = new ReportDTO.TimePeriodReport();
        report.setStartDate(LocalDate.now());
        report.setEndDate(LocalDate.now());
        
        // Get category summary
        List<Object[]> categoryResults = productRepository.getCategoryWiseSummary();
        
        List<ReportDTO.CategoryReport> categoryReports = categoryResults.stream()
                .filter(result -> result[0].toString().equalsIgnoreCase(category))
                .map(result -> new ReportDTO.CategoryReport(
                        (String) result[0],
                        ((Number) result[1]).longValue(),
                        ((Number) result[2]).longValue(),
                        (BigDecimal) result[3]
                ))
                .collect(Collectors.toList());
        
        report.setCategoryReports(categoryReports);
        
        // Calculate inventory summary for this category
        ReportDTO.InventorySummary summary = calculateCategorySummary(category);
        report.setSummary(summary);
        
        return report;
    }
    
    private ReportDTO.TimePeriodReport generateReportForDateRange(LocalDate startDate, LocalDate endDate) {
        ReportDTO.TimePeriodReport report = new ReportDTO.TimePeriodReport();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        
        // Get category-wise summary
        List<ReportDTO.CategoryReport> categoryReports = productRepository.getCategoryWiseSummary().stream()
                .map(result -> new ReportDTO.CategoryReport(
                        (String) result[0],
                        ((Number) result[1]).longValue(),
                        ((Number) result[2]).longValue(),
                        (BigDecimal) result[3]
                ))
                .collect(Collectors.toList());
        
        report.setCategoryReports(categoryReports);
        
        // Get inventory summary
        ReportDTO.InventorySummary summary = new ReportDTO.InventorySummary();
        Long totalProducts = productRepository.getTotalProducts();
        Long totalQuantity = productRepository.getTotalQuantity();
        BigDecimal totalValue = productRepository.getTotalInventoryValue();
        
        summary.setTotalProducts(totalProducts != null ? totalProducts : 0);
        summary.setTotalQuantity(totalQuantity != null ? totalQuantity : 0);
        summary.setTotalValue(totalValue != null ? totalValue : BigDecimal.ZERO);
        summary.setLowStockItems(productRepository
                .findByQuantityLessThanAndIsDeleted(10, false).size());
        
        report.setSummary(summary);
        
        // Get transaction summary for the period
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Object[]> transactionResults = stockTransactionRepository.getTransactionSummary(startDateTime, endDateTime);
        Map<String, Long> transactionSummary = new HashMap<>();
        
        for (Object[] result : transactionResults) {
            TransactionType type = (TransactionType) result[0];
            Long quantity = ((Number) result[1]).longValue();
            transactionSummary.put(type.name(), quantity);
        }
        
        report.setTransactionSummary(transactionSummary);
        
        return report;
    }
    
    private ReportDTO.InventorySummary calculateCategorySummary(String category) {
        ReportDTO.InventorySummary summary = new ReportDTO.InventorySummary();
        
        List<com.inventory.entity.Product> categoryProducts = 
                productRepository.findByCategoryAndIsDeleted(category, false);
        
        long productCount = categoryProducts.size();
        long totalQuantity = categoryProducts.stream()
                .mapToInt(com.inventory.entity.Product::getQuantity)
                .sum();
        
        BigDecimal totalValue = categoryProducts.stream()
                .map(product -> product.getUnitPrice().multiply(BigDecimal.valueOf(product.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long lowStockCount = categoryProducts.stream()
                .filter(product -> product.getQuantity() < product.getMinStockThreshold())
                .count();
        
        summary.setTotalProducts(productCount);
        summary.setTotalQuantity(totalQuantity);
        summary.setTotalValue(totalValue);
        summary.setLowStockItems(lowStockCount);
        
        return summary;
    }
}