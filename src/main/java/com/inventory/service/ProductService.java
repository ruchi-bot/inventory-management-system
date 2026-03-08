package com.inventory.service;

import com.inventory.dto.ProductDTO;
import com.inventory.dto.ReportDTO;
import com.inventory.entity.Product;
import java.util.List;

public interface ProductService {
    ProductDTO.ProductResponse createProduct(ProductDTO.CreateProductRequest request, Long createdBy);
    ProductDTO.ProductResponse updateProduct(Long productId, ProductDTO.UpdateProductRequest request, Long updatedBy);
    void deleteProduct(Long productId, Long deletedBy);
    void restoreProduct(Long productId, Long restoredBy);
    void permanentDeleteProduct(Long productId, Long deletedBy);
    ProductDTO.ProductResponse getProductBySku(String sku);
    List<ProductDTO.ProductResponse> getAllProducts();
    List<ProductDTO.ProductResponse> getActiveProducts();
    List<ProductDTO.ProductResponse> getDeletedProducts();
    List<ProductDTO.ProductResponse> getLowStockProducts();
    List<ProductDTO.ProductResponse> searchProducts(String searchTerm);
    ProductDTO.ProductResponse stockIn(ProductDTO.StockUpdateRequest request, Long performedBy);
    ProductDTO.ProductResponse stockOut(ProductDTO.StockUpdateRequest request, Long performedBy);
    ReportDTO.InventorySummary getInventorySummary();
    List<ReportDTO.CategoryReport> getCategoryWiseReport();
    void updateProductThreshold(String sku, Integer threshold, Long updatedBy);
    Product getProductEntityBySku(String sku);  // For export service
    
}