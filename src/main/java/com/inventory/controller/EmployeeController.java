package com.inventory.controller;

import com.inventory.dto.AlertDTO;
import com.inventory.dto.ProductDTO;
import com.inventory.dto.ReportDTO;
import com.inventory.entity.Product;
import com.inventory.entity.StockTransaction;
import com.inventory.service.AlertService;
import com.inventory.service.ExportService;
import com.inventory.service.ProductService;
import com.inventory.service.StockTransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employee")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeController {
    
    private final ProductService productService;
    private final StockTransactionService stockTransactionService;
    private final AlertService alertService;
    private final ExportService exportService;
    
    public EmployeeController(ProductService productService, 
                             StockTransactionService stockTransactionService,
                             AlertService alertService,
                             ExportService exportService) {
        this.productService = productService;
        this.stockTransactionService = stockTransactionService;
        this.alertService = alertService;
        this.exportService = exportService;
    }
    
    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO.ProductResponse>> getAllProducts() {
        List<ProductDTO.ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/products/{sku}")
    public ResponseEntity<ProductDTO.ProductResponse> getProductBySku(@PathVariable String sku) {
        ProductDTO.ProductResponse product = productService.getProductBySku(sku);
        return ResponseEntity.ok(product);
    }
    
    @GetMapping("/products/search")
    public ResponseEntity<List<ProductDTO.ProductResponse>> searchProducts(
            @RequestParam String query) {
        List<ProductDTO.ProductResponse> products = productService.searchProducts(query);
        return ResponseEntity.ok(products);
    }
    
    @PutMapping("/products/{sku}/threshold")
    public ResponseEntity<Void> updateProductThreshold(
            @PathVariable String sku,
            @Valid @RequestBody ProductDTO.ThresholdUpdateRequest request) {
        Long performedBy = 3L; // Employee id from security context
        productService.updateProductThreshold(sku, request.getMinStockThreshold(), performedBy);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/stock/in")
    public ResponseEntity<ProductDTO.ProductResponse> stockIn(
            @Valid @RequestBody ProductDTO.StockUpdateRequest request) {
        Long performedBy = 3L; // Employee id from security context
        ProductDTO.ProductResponse response = productService.stockIn(request, performedBy);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/stock/out")
    public ResponseEntity<ProductDTO.ProductResponse> stockOut(
            @Valid @RequestBody ProductDTO.StockUpdateRequest request) {
        Long performedBy = 3L; // Employee id from security context
        ProductDTO.ProductResponse response = productService.stockOut(request, performedBy);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/transactions")
    public ResponseEntity<List<StockTransaction>> getTransactions(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<StockTransaction> transactions = stockTransactionService.searchTransactions(
                sku, productName, transactionType, startDate, endDate);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/reports/inventory-summary")
    public ResponseEntity<ReportDTO.InventorySummary> getInventorySummary() {
        ReportDTO.InventorySummary summary = productService.getInventorySummary();
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/reports/low-stock")
    public ResponseEntity<List<ProductDTO.ProductResponse>> getLowStockProducts() {
        List<ProductDTO.ProductResponse> products = productService.getLowStockProducts();
        return ResponseEntity.ok(products);
    }
    
    // Alert Viewing (Read-only for employees)
    @GetMapping("/alerts")
    public ResponseEntity<List<AlertDTO.LowStockAlertResponse>> getActiveAlerts() {
        List<AlertDTO.LowStockAlertResponse> alerts = alertService.getActiveAlerts();
        return ResponseEntity.ok(alerts);
    }
    
    @GetMapping("/alerts/summary")
    public ResponseEntity<AlertDTO.AlertSummary> getAlertSummary() {
        AlertDTO.AlertSummary summary = alertService.getAlertSummary();
        return ResponseEntity.ok(summary);
    }
    
    // Export Endpoints
    @GetMapping("/transactions/export/pdf")
    public ResponseEntity<byte[]> exportTransactionsPDF(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            List<StockTransaction> transactions = stockTransactionService.searchTransactions(
                    sku, productName, transactionType, startDate, endDate);
            
            ByteArrayOutputStream pdfStream = exportService.generateTransactionsPDF(
                    transactions, sku, productName, transactionType, startDate, endDate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "transactions-report.pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/transactions/export/csv")
    public ResponseEntity<byte[]> exportTransactionsCSV(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            List<StockTransaction> transactions = stockTransactionService.searchTransactions(
                    sku, productName, transactionType, startDate, endDate);
            
            ByteArrayOutputStream csvStream = exportService.generateTransactionsCSV(
                    transactions, sku, productName, transactionType, startDate, endDate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "transactions-report.csv");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}