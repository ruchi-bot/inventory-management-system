package com.inventory.util;

import com.inventory.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SkuGenerator {
    
    private final ProductRepository productRepository;
    
    @Value("${app.sku.prefix:SKU-}")
    private String skuPrefix;
    
    @Value("${app.sku.length:6}")
    private int skuLength;
    
    public SkuGenerator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    public String generateNextSku() {
        String lastSku = productRepository.findLastSku(skuPrefix).orElse(null);
        
        if (lastSku == null) {
            return skuPrefix + String.format("%0" + skuLength + "d", 1);
        }
        
        String numberPart = lastSku.substring(skuPrefix.length());
        int nextNumber = Integer.parseInt(numberPart) + 1;
        
        return skuPrefix + String.format("%0" + skuLength + "d", nextNumber);
    }
}