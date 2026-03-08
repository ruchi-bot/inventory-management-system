package com.inventory.service.impl;

import com.inventory.dto.ProductDTO;
import com.inventory.dto.ReportDTO;
import com.inventory.entity.Product;
import com.inventory.entity.StockTransaction;
import com.inventory.entity.TransactionType;
import com.inventory.entity.AuditLog;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.ValidationException;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.StockTransactionRepository;
import com.inventory.repository.AuditLogRepository;
import com.inventory.service.AlertService;
import com.inventory.service.ProductService;
import com.inventory.util.SkuGenerator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final AlertService alertService;
    private final HttpServletRequest request;
    private final SkuGenerator skuGenerator;
    
    @Value("${app.low-stock-threshold:10}")
    private int lowStockThreshold;
    
    public ProductServiceImpl(ProductRepository productRepository,
                            StockTransactionRepository stockTransactionRepository,
                            AuditLogRepository auditLogRepository,
                            AlertService alertService,
                            HttpServletRequest request,
                            SkuGenerator skuGenerator) {
        this.productRepository = productRepository;
        this.stockTransactionRepository = stockTransactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.alertService = alertService;
        this.request = request;
        this.skuGenerator = skuGenerator;
    }
    
    @Override
    public ProductDTO.ProductResponse createProduct(ProductDTO.CreateProductRequest request, Long createdBy) {
        // Validate request
        if (request.getQuantity() < 0) {
            throw new ValidationException("Quantity cannot be negative");
        }
        
        if (request.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Unit price must be greater than 0");
        }
        
        // Check if product with same name already exists
        List<Product> existingProducts = productRepository.searchProducts(request.getProductName());
        if (!existingProducts.isEmpty()) {
            throw new ValidationException("Product with similar name already exists");
        }
        
        String sku = skuGenerator.generateNextSku();
        
        Product product = new Product();
        product.setSku(sku);
        product.setProductName(request.getProductName());
        product.setCategory(request.getCategory());
        product.setSupplier(request.getSupplier());
        product.setUnitPrice(request.getUnitPrice());
        product.setQuantity(request.getQuantity());
        product.setMinStockThreshold(request.getMinStockThreshold());
        product.setCreatedBy(createdBy);
        product.setLastUpdatedBy(createdBy);
        
        Product savedProduct = productRepository.save(product);
        
        // Log stock transaction
        logStockTransaction(savedProduct, TransactionType.STOCK_IN, 
                           request.getQuantity(), 0, request.getQuantity(), createdBy, "Initial stock");
        
        logAudit(createdBy, getCurrentUserEmail(createdBy), "CREATE_PRODUCT", "PRODUCT", 
                savedProduct.getId(), "Created product: " + savedProduct.getProductName(), getClientIp());
        
        // Check for low stock
        checkAndSendLowStockAlert(savedProduct);
        
        return mapToProductResponse(savedProduct);
    }
    
    @Override
    public ProductDTO.ProductResponse updateProduct(Long productId, ProductDTO.UpdateProductRequest request, Long updatedBy) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        if (product.isDeleted()) {
            throw new ResourceNotFoundException("Product is deleted");
        }
        
        product.setProductName(request.getProductName());
        product.setCategory(request.getCategory());
        product.setSupplier(request.getSupplier());
        product.setLastUpdatedBy(updatedBy);
        
        Product updatedProduct = productRepository.save(product);
        
        logAudit(updatedBy, getCurrentUserEmail(updatedBy), "UPDATE_PRODUCT", "PRODUCT", 
                productId, "Updated product details", getClientIp());
        
        return mapToProductResponse(updatedProduct);
    }
    
    @Override
    public void deleteProduct(Long productId, Long deletedBy) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        if (product.isDeleted()) {
            throw new ValidationException("Product is already deleted");
        }
        
        product.setDeleted(true);
        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
        
        logAudit(deletedBy, getCurrentUserEmail(deletedBy), "DELETE_PRODUCT", "PRODUCT", 
                productId, "Soft deleted product", getClientIp());
    }
    
    @Override
    public void restoreProduct(Long productId, Long restoredBy) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        if (!product.isDeleted()) {
            throw new ValidationException("Product is not deleted");
        }
        
        // Check if deleted within 30 days (only if deletedAt is set)
        if (product.getDeletedAt() != null && 
            product.getDeletedAt().isBefore(LocalDateTime.now().minusDays(30))) {
            throw new ValidationException("Cannot restore product after 30 days");
        }
        
        product.setDeleted(false);
        product.setDeletedAt(null);
        productRepository.save(product);
        
        logAudit(restoredBy, getCurrentUserEmail(restoredBy), "RESTORE_PRODUCT", "PRODUCT", 
                productId, "Restored deleted product", getClientIp());
    }
    
    @Override
    public void permanentDeleteProduct(Long productId, Long deletedBy) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        if (!product.isDeleted()) {
            throw new ValidationException("Product must be soft-deleted before permanent deletion");
        }
        
        logAudit(deletedBy, getCurrentUserEmail(deletedBy), "PERMANENT_DELETE_PRODUCT", "PRODUCT", 
                productId, "Permanently deleted product: " + product.getProductName() + " (SKU: " + product.getSku() + ")", getClientIp());
        
        productRepository.delete(product);
    }
    
    @Override
    public ProductDTO.ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySkuAndIsDeleted(sku, false)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "SKU", sku));
        return mapToProductResponse(product);
    }
    
    @Override
    public List<ProductDTO.ProductResponse> getAllProducts() {
        return productRepository.findByIsDeleted(false).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ProductDTO.ProductResponse> getActiveProducts() {
        return productRepository.findByIsDeleted(false).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ProductDTO.ProductResponse> getDeletedProducts() {
        return productRepository.findByIsDeleted(true).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ProductDTO.ProductResponse> getLowStockProducts() {
        return productRepository.findByQuantityLessThanAndIsDeleted(lowStockThreshold, false).stream()
                .map(this::mapToProductResponse)
                .sorted((p1, p2) -> p1.getQuantity() - p2.getQuantity())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ProductDTO.ProductResponse> searchProducts(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new ValidationException("Search term cannot be empty");
        }
        
        return productRepository.searchProducts(searchTerm).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public ProductDTO.ProductResponse stockIn(ProductDTO.StockUpdateRequest request, Long performedBy) {
        Product product = productRepository.findBySkuAndIsDeleted(request.getSku(), false)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "SKU", request.getSku()));
        
        if (request.getQuantity() <= 0) {
            throw new ValidationException("Stock in quantity must be positive");
        }
        
        int previousQuantity = product.getQuantity();
        int newQuantity = previousQuantity + request.getQuantity();
        
        product.setQuantity(newQuantity);
        product.setLastUpdatedBy(performedBy);
        Product updatedProduct = productRepository.save(product);
        
        // Log stock transaction
        logStockTransaction(updatedProduct, TransactionType.STOCK_IN, 
                           request.getQuantity(), previousQuantity, newQuantity, 
                           performedBy, request.getNotes());
        
        logAudit(performedBy, getCurrentUserEmail(performedBy), "STOCK_IN", "PRODUCT", 
                updatedProduct.getId(), "Added " + request.getQuantity() + " units", getClientIp());
        
        return mapToProductResponse(updatedProduct);
    }
    
    @Override
    public ProductDTO.ProductResponse stockOut(ProductDTO.StockUpdateRequest request, Long performedBy) {
        Product product = productRepository.findBySkuAndIsDeleted(request.getSku(), false)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "SKU", request.getSku()));
        
        if (request.getQuantity() <= 0) {
            throw new ValidationException("Stock out quantity must be positive");
        }
        
        if (product.getQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(
                    product.getProductName(), 
                    product.getQuantity(), 
                    request.getQuantity()
            );
        }
        
        int previousQuantity = product.getQuantity();
        int newQuantity = previousQuantity - request.getQuantity();
        
        product.setQuantity(newQuantity);
        product.setLastUpdatedBy(performedBy);
        Product updatedProduct = productRepository.save(product);
        
        // Log stock transaction
        logStockTransaction(updatedProduct, TransactionType.STOCK_OUT, 
                           request.getQuantity(), previousQuantity, newQuantity, 
                           performedBy, request.getNotes());
        
        logAudit(performedBy, getCurrentUserEmail(performedBy), "STOCK_OUT", "PRODUCT", 
                updatedProduct.getId(), "Removed " + request.getQuantity() + " units", getClientIp());
        
        // Check for low stock after stock out
        checkAndSendLowStockAlert(updatedProduct);
        
        return mapToProductResponse(updatedProduct);
    }
    
    @Override
    public ReportDTO.InventorySummary getInventorySummary() {
        ReportDTO.InventorySummary summary = new ReportDTO.InventorySummary();
        
        Long totalProducts = productRepository.getTotalProducts();
        Long totalQuantity = productRepository.getTotalQuantity();
        BigDecimal totalValue = productRepository.getTotalInventoryValue();
        
        summary.setTotalProducts(totalProducts != null ? totalProducts : 0);
        summary.setTotalQuantity(totalQuantity != null ? totalQuantity : 0);
        summary.setTotalValue(totalValue != null ? totalValue : BigDecimal.ZERO);
        summary.setLowStockItems(productRepository
                .findByQuantityLessThanAndIsDeleted(lowStockThreshold, false).size());
        
        return summary;
    }
    
    @Override
    public List<ReportDTO.CategoryReport> getCategoryWiseReport() {
        List<Object[]> results = productRepository.getCategoryWiseSummary();
        return results.stream()
                .map(result -> new ReportDTO.CategoryReport(
                        (String) result[0],
                        ((Number) result[1]).longValue(),
                        ((Number) result[2]).longValue(),
                        (BigDecimal) result[3]
                ))
                .collect(Collectors.toList());
    }
    
    private ProductDTO.ProductResponse mapToProductResponse(Product product) {
        ProductDTO.ProductResponse response = new ProductDTO.ProductResponse();
        response.setId(product.getId());
        response.setSku(product.getSku());
        response.setProductName(product.getProductName());
        response.setCategory(product.getCategory());
        response.setSupplier(product.getSupplier());
        response.setUnitPrice(product.getUnitPrice());
        response.setQuantity(product.getQuantity());
        response.setTotalValue(product.getTotalValue());
        response.setMinStockThreshold(product.getMinStockThreshold());
        response.setLowStock(product.getQuantity() < lowStockThreshold);
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        response.setDeletedAt(product.getDeletedAt());
        response.setDeleted(product.isDeleted());
        return response;
    }
    
    private void logStockTransaction(Product product, TransactionType type, int quantity,
                                    int previousQuantity, int newQuantity, 
                                    Long performedBy, String notes) {
        StockTransaction transaction = new StockTransaction(
                product.getSku(),
                product.getProductName(),
                type,
                quantity,
                previousQuantity,
                newQuantity,
                performedBy
        );
        transaction.setNotes(notes);
        stockTransactionRepository.save(transaction);
    }
    
    private void checkAndSendLowStockAlert(Product product) {
        alertService.checkAndCreateLowStockAlert(product);
    }
    
    @Override
    public void updateProductThreshold(String sku, Integer threshold, Long updatedBy) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
        
        if (product.isDeleted()) {
            throw new ResourceNotFoundException("Product is deleted");
        }
        
        if (threshold == null || threshold < 0) {
            throw new ValidationException("Threshold must be a positive number");
        }
        
        Integer oldThreshold = product.getMinStockThreshold();
        product.setMinStockThreshold(threshold);
        product.setLastUpdatedBy(updatedBy);
        
        Product updatedProduct = productRepository.save(product);
        
        logAudit(updatedBy, getCurrentUserEmail(updatedBy), "UPDATE_THRESHOLD", "PRODUCT", 
                product.getId(), String.format("Updated threshold from %d to %d", oldThreshold, threshold), 
                getClientIp());
        
        // Send threshold update notification to admins
        alertService.sendThresholdUpdateNotification(
            updatedProduct.getProductName(),
            updatedProduct.getSku(),
            oldThreshold,
            threshold,
            updatedBy
        );
        
        // Check if we need to create/resolve alerts after threshold change
        checkAndSendLowStockAlert(updatedProduct);
    }
    
    @Override
    public Product getProductEntityBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
    }
    
    private void logAudit(Long userId, String userEmail, String action, 
                         String entityType, Long entityId, String details, String ipAddress) {
        AuditLog auditLog = new AuditLog(userId, userEmail, action, entityType, entityId, details, ipAddress);
        auditLogRepository.save(auditLog);
    }
    
    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    private String getCurrentUserEmail(Long userId) {
        // In real implementation, get from Security Context
        // For now, return placeholder or fetch from userRepository
        return "system@inventory.com";
    }
}