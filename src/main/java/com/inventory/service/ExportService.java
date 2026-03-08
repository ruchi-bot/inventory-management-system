package com.inventory.service;

import com.inventory.entity.StockTransaction;
import com.inventory.entity.Product;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ExportService {
    
    /**
     * Generate PDF report for transactions with filters
     */
    ByteArrayOutputStream generateTransactionsPDF(
            List<StockTransaction> transactions,
            String sku,
            String productName,
            String transactionType,
            LocalDate startDate,
            LocalDate endDate
    ) throws Exception;
    
    /**
     * Generate CSV report for transactions with filters
     */
    ByteArrayOutputStream generateTransactionsCSV(
            List<StockTransaction> transactions,
            String sku,
            String productName,
            String transactionType,
            LocalDate startDate,
            LocalDate endDate
    ) throws Exception;
    
    /**
     * Generate PDF report for current stock status
     */
    ByteArrayOutputStream generateStockReportPDF(
            List<Product> products,
            String category,
            String supplier,
            Boolean lowStock
    ) throws Exception;
    
    /**
     * Generate CSV report for current stock status
     */
    ByteArrayOutputStream generateStockReportCSV(
            List<Product> products,
            String category,
            String supplier,
            Boolean lowStock
    ) throws Exception;
}
