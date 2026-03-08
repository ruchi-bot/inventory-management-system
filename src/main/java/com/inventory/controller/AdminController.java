package com.inventory.controller;

import com.inventory.dto.AlertDTO;
import com.inventory.dto.ProductDTO;
import com.inventory.dto.ReportDTO;
import com.inventory.dto.UserDTO;
import com.inventory.entity.Product;
import com.inventory.entity.StockTransaction;
import com.inventory.service.AlertService;
import com.inventory.service.ExportService;
import com.inventory.service.ProductService;
import com.inventory.service.StockTransactionService;
import com.inventory.service.UserService;
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
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final ProductService productService;
    private final UserService userService;
    private final StockTransactionService stockTransactionService;
    private final AlertService alertService;
    private final ExportService exportService;
    
    public AdminController(ProductService productService, UserService userService,
                          StockTransactionService stockTransactionService,
                          AlertService alertService,
                          ExportService exportService) {
        this.productService = productService;
        this.userService = userService;
        this.stockTransactionService = stockTransactionService;
        this.alertService = alertService;
        this.exportService = exportService;
    }
    
    // Product Management
    @PostMapping("/products")
    public ResponseEntity<ProductDTO.ProductResponse> createProduct(
            @Valid @RequestBody ProductDTO.CreateProductRequest request) {
        Long createdBy = 2L; // Admin id from security context
        ProductDTO.ProductResponse response = productService.createProduct(request, createdBy);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/products/{productId}")
    public ResponseEntity<ProductDTO.ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductDTO.UpdateProductRequest request) {
        Long updatedBy = 2L; // Admin id from security context
        ProductDTO.ProductResponse response = productService.updateProduct(productId, request, updatedBy);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        Long deletedBy = 2L; // Admin id from security context
        productService.deleteProduct(productId, deletedBy);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/products/{productId}/restore")
    public ResponseEntity<Void> restoreProduct(@PathVariable Long productId) {
        Long restoredBy = 2L; // Admin id from security context
        productService.restoreProduct(productId, restoredBy);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/products/{productId}/permanent")
    public ResponseEntity<Void> permanentDeleteProduct(@PathVariable Long productId) {
        Long deletedBy = 2L; // Admin id from security context
        productService.permanentDeleteProduct(productId, deletedBy);
        return ResponseEntity.ok().build();
    }
    
    // Stock Management
    @PostMapping("/stock/in")
    public ResponseEntity<ProductDTO.ProductResponse> stockIn(
            @Valid @RequestBody ProductDTO.StockUpdateRequest request) {
        Long performedBy = 2L; // Admin id from security context
        ProductDTO.ProductResponse response = productService.stockIn(request, performedBy);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/stock/out")
    public ResponseEntity<ProductDTO.ProductResponse> stockOut(
            @Valid @RequestBody ProductDTO.StockUpdateRequest request) {
        Long performedBy = 2L; // Admin id from security context
        ProductDTO.ProductResponse response = productService.stockOut(request, performedBy);
        return ResponseEntity.ok(response);
    }
    
    // Reports
    @GetMapping("/reports/inventory-summary")
    public ResponseEntity<ReportDTO.InventorySummary> getInventorySummary() {
        ReportDTO.InventorySummary summary = productService.getInventorySummary();
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/reports/category-wise")
    public ResponseEntity<List<ReportDTO.CategoryReport>> getCategoryWiseReport() {
        List<ReportDTO.CategoryReport> reports = productService.getCategoryWiseReport();
        return ResponseEntity.ok(reports);
    }
    
    @GetMapping("/reports/low-stock")
    public ResponseEntity<List<ProductDTO.ProductResponse>> getLowStockProducts() {
        List<ProductDTO.ProductResponse> products = productService.getLowStockProducts();
        return ResponseEntity.ok(products);
    }
    
    // Employee Visibility
    @GetMapping("/employees")
    public ResponseEntity<List<UserDTO.UserResponse>> getEmployees() {
        List<UserDTO.UserResponse> employees = userService.getUsersByRole("EMPLOYEE");
        return ResponseEntity.ok(employees);
    }
    
    @GetMapping("/employees/count")
    public ResponseEntity<Long> getEmployeeCount() {
        List<UserDTO.UserResponse> employees = userService.getUsersByRole("EMPLOYEE");
        return ResponseEntity.ok((long) employees.size());
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
    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO.ProductResponse>> getAllProducts() {
        List<ProductDTO.ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/products/active")
    public ResponseEntity<List<ProductDTO.ProductResponse>> getActiveProducts() {
        List<ProductDTO.ProductResponse> products = productService.getActiveProducts();
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/products/deleted")
    public ResponseEntity<List<ProductDTO.ProductResponse>> getDeletedProducts() {
        List<ProductDTO.ProductResponse> products = productService.getDeletedProducts();
        return ResponseEntity.ok(products);
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
    
    @GetMapping("/stock-report/export/pdf")
    public ResponseEntity<byte[]> exportStockReportPDF(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) Boolean lowStock) {
        
        try {
            List<ProductDTO.ProductResponse> productDTOs = productService.getActiveProducts();
            
            // Convert DTOs to entities for export service
            // Filter by criteria if provided
            if (category != null && !category.isEmpty()) {
                productDTOs = productDTOs.stream()
                        .filter(p -> p.getCategory().equalsIgnoreCase(category))
                        .collect(Collectors.toList());
            }
            if (supplier != null && !supplier.isEmpty()) {
                productDTOs = productDTOs.stream()
                        .filter(p -> p.getSupplier() != null && p.getSupplier().equalsIgnoreCase(supplier))
                        .collect(Collectors.toList());
            }
            if (lowStock != null && lowStock) {
                productDTOs = productDTOs.stream()
                        .filter(p -> p.getQuantity() <= p.getMinStockThreshold())
                        .collect(Collectors.toList());
            }
            
            // Get actual Product entities for export
            List<Product> products = productDTOs.stream()
                    .map(dto -> productService.getProductEntityBySku(dto.getSku()))
                    .collect(Collectors.toList());
            
            ByteArrayOutputStream pdfStream = exportService.generateStockReportPDF(
                    products, category, supplier, lowStock);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "stock-report.pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/stock-report/export/csv")
    public ResponseEntity<byte[]> exportStockReportCSV(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) Boolean lowStock) {
        
        try {
            List<ProductDTO.ProductResponse> productDTOs = productService.getActiveProducts();
            
            // Filter by criteria if provided
            if (category != null && !category.isEmpty()) {
                productDTOs = productDTOs.stream()
                        .filter(p -> p.getCategory().equalsIgnoreCase(category))
                        .collect(Collectors.toList());
            }
            if (supplier != null && !supplier.isEmpty()) {
                productDTOs = productDTOs.stream()
                        .filter(p -> p.getSupplier() != null && p.getSupplier().equalsIgnoreCase(supplier))
                        .collect(Collectors.toList());
            }
            if (lowStock != null && lowStock) {
                productDTOs = productDTOs.stream()
                        .filter(p -> p.getQuantity() <= p.getMinStockThreshold())
                        .collect(Collectors.toList());
            }
            
            // Get actual Product entities for export
            List<Product> products = productDTOs.stream()
                    .map(dto -> productService.getProductEntityBySku(dto.getSku()))
                    .collect(Collectors.toList());
            
            ByteArrayOutputStream csvStream = exportService.generateStockReportCSV(
                    products, category, supplier, lowStock);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "stock-report.csv");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Alert Management
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
    
    @PostMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<Void> resolveAlert(@PathVariable Long alertId) {
        alertService.resolveAlert(alertId);
        return ResponseEntity.ok().build();
    }
    
    // Threshold Management
    @PutMapping("/products/{sku}/threshold")
    public ResponseEntity<Void> updateProductThreshold(
            @PathVariable String sku,
            @Valid @RequestBody AlertDTO.UpdateThresholdRequest request) {
        Long updatedBy = 2L; // Admin id from security context
        productService.updateProductThreshold(sku, request.getMinStockThreshold(), updatedBy);
        return ResponseEntity.ok().build();
    }
}