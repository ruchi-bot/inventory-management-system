package com.inventory.service.impl;

import com.inventory.entity.StockTransaction;
import com.inventory.entity.TransactionType;
import com.inventory.exception.ValidationException;
import com.inventory.repository.StockTransactionRepository;
import com.inventory.service.StockTransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StockTransactionServiceImpl implements StockTransactionService {
    
    private final StockTransactionRepository stockTransactionRepository;
    
    public StockTransactionServiceImpl(StockTransactionRepository stockTransactionRepository) {
        this.stockTransactionRepository = stockTransactionRepository;
    }
    
    @Override
    public List<StockTransaction> getTransactionsBySku(String sku) {
        if (sku == null || sku.trim().isEmpty()) {
            throw new ValidationException("SKU cannot be empty");
        }
        
        return stockTransactionRepository.findBySkuOrderByTransactionDateDesc(sku);
    }
    
    @Override
    public List<StockTransaction> searchTransactions(String sku, String productName, 
                                                   String transactionType, 
                                                   LocalDate startDate, LocalDate endDate) {
        
        TransactionType type = null;
        if (transactionType != null && !transactionType.trim().isEmpty()) {
            try {
                type = TransactionType.valueOf(transactionType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid transaction type. Allowed values: STOCK_IN, STOCK_OUT, ADJUSTMENT, DAMAGE, RETURN");
            }
        }
        
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        
        if (startDate != null) {
            startDateTime = startDate.atStartOfDay();
        }
        
        if (endDate != null) {
            endDateTime = endDate.atTime(LocalTime.MAX);
        }
        
        // If both dates are provided, validate
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ValidationException("Start date cannot be after end date");
        }
        
        return stockTransactionRepository.searchTransactions(
                sku, productName, type, startDateTime, endDateTime
        );
    }
}