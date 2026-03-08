package com.inventory.service;

import com.inventory.entity.StockTransaction;
import java.time.LocalDate;
import java.util.List;

public interface StockTransactionService {
    List<StockTransaction> getTransactionsBySku(String sku);
    List<StockTransaction> searchTransactions(String sku, String productName, 
                                            String transactionType, 
                                            LocalDate startDate, LocalDate endDate);
}