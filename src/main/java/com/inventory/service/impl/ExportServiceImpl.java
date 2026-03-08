package com.inventory.service.impl;

import com.inventory.entity.Product;
import com.inventory.entity.StockTransaction;
import com.inventory.service.ExportService;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportServiceImpl implements ExportService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    
    @Override
    public ByteArrayOutputStream generateTransactionsPDF(
            List<StockTransaction> transactions,
            String sku,
            String productName,
            String transactionType,
            LocalDate startDate,
            LocalDate endDate
    ) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Title
        Paragraph title = new Paragraph("Stock Transactions Report")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);
        
        // Report metadata
        document.add(new Paragraph("Generated on: " + LocalDate.now().format(DATE_FORMATTER))
                .setFontSize(10));
        
        // Filters applied
        StringBuilder filters = new StringBuilder("Filters Applied: ");
        if (sku != null && !sku.isEmpty()) filters.append("SKU: ").append(sku).append(" | ");
        if (productName != null && !productName.isEmpty()) filters.append("Product: ").append(productName).append(" | ");
        if (transactionType != null && !transactionType.isEmpty()) filters.append("Type: ").append(transactionType).append(" | ");
        if (startDate != null) filters.append("From: ").append(startDate.format(DATE_FORMATTER)).append(" | ");
        if (endDate != null) filters.append("To: ").append(endDate.format(DATE_FORMATTER));
        
        document.add(new Paragraph(filters.toString()).setFontSize(10));
        document.add(new Paragraph(" ")); // Spacer
        
        // Table
        float[] columnWidths = {3, 2, 3, 2, 2, 2, 2, 3};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        
        // Header
        String[] headers = {"Date", "SKU", "Product Name", "Type", "Quantity", "Before", "After", "Notes"};
        for (String header : headers) {
            Cell cell = new Cell().add(new Paragraph(header).setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(cell);
        }
        
        // Data rows
        for (StockTransaction transaction : transactions) {
            table.addCell(new Cell().add(new Paragraph(
                    transaction.getTransactionDate().format(DATETIME_FORMATTER))));
            table.addCell(new Cell().add(new Paragraph(
                    transaction.getSku())));
            table.addCell(new Cell().add(new Paragraph(
                    transaction.getProductName())));
            
            Cell typeCell = new Cell().add(new Paragraph(transaction.getTransactionType().toString()))
                    .setTextAlignment(TextAlignment.CENTER);
            if (transaction.getTransactionType().toString().equals("IN")) {
                typeCell.setBackgroundColor(ColorConstants.GREEN).setFontColor(ColorConstants.WHITE);
            } else {
                typeCell.setBackgroundColor(ColorConstants.RED).setFontColor(ColorConstants.WHITE);
            }
            table.addCell(typeCell);
            
            table.addCell(new Cell().add(new Paragraph(String.valueOf(transaction.getQuantity())))
                    .setTextAlignment(TextAlignment.CENTER));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(transaction.getPreviousQuantity())))
                    .setTextAlignment(TextAlignment.CENTER));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(transaction.getNewQuantity())))
                    .setTextAlignment(TextAlignment.CENTER));
            table.addCell(new Cell().add(new Paragraph(
                    transaction.getNotes() != null ? transaction.getNotes() : "-")));
        }
        
        document.add(table);
        
        // Summary
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Total Transactions: " + transactions.size())
                .setBold()
                .setFontSize(12));
        
        document.close();
        return baos;
    }
    
    @Override
    public ByteArrayOutputStream generateTransactionsCSV(
            List<StockTransaction> transactions,
            String sku,
            String productName,
            String transactionType,
            LocalDate startDate,
            LocalDate endDate
    ) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(baos);
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                "Date", "SKU", "Product Name", "Type", "Quantity", "Before", "After", "Notes"
        ));
        
        for (StockTransaction transaction : transactions) {
            csvPrinter.printRecord(
                    transaction.getTransactionDate().format(DATETIME_FORMATTER),
                    transaction.getSku(),
                    transaction.getProductName(),
                    transaction.getTransactionType().toString(),
                    transaction.getQuantity(),
                    transaction.getPreviousQuantity(),
                    transaction.getNewQuantity(),
                    transaction.getNotes() != null ? transaction.getNotes() : ""
            );
        }
        
        csvPrinter.flush();
        csvPrinter.close();
        return baos;
    }
    
    @Override
    public ByteArrayOutputStream generateStockReportPDF(
            List<Product> products,
            String category,
            String supplier,
            Boolean lowStock
    ) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Title
        Paragraph title = new Paragraph("Current Stock Status Report")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);
        
        // Report metadata
        document.add(new Paragraph("Generated on: " + LocalDate.now().format(DATE_FORMATTER))
                .setFontSize(10));
        
        // Filters
        StringBuilder filters = new StringBuilder("Filters Applied: ");
        if (category != null && !category.isEmpty()) filters.append("Category: ").append(category).append(" | ");
        if (supplier != null && !supplier.isEmpty()) filters.append("Supplier: ").append(supplier).append(" | ");
        if (lowStock != null && lowStock) filters.append("Low Stock Items Only");
        
        document.add(new Paragraph(filters.toString()).setFontSize(10));
        document.add(new Paragraph(" "));
        
        // Table
        float[] columnWidths = {2, 4, 2, 2, 2, 2, 3};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        
        // Header
        String[] headers = {"SKU", "Product Name", "Category", "Quantity", "Min Stock", "Unit Price", "Supplier"};
        for (String header : headers) {
            Cell cell = new Cell().add(new Paragraph(header).setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(cell);
        }
        
        // Data rows
        for (Product product : products) {
            table.addCell(new Cell().add(new Paragraph(product.getSku())));
            table.addCell(new Cell().add(new Paragraph(product.getProductName())));
            table.addCell(new Cell().add(new Paragraph(product.getCategory())));
            
            Cell qtyCell = new Cell().add(new Paragraph(String.valueOf(product.getQuantity())))
                    .setTextAlignment(TextAlignment.CENTER);
            if (product.getQuantity() <= product.getMinStockThreshold()) {
                qtyCell.setBackgroundColor(ColorConstants.RED).setFontColor(ColorConstants.WHITE);
            }
            table.addCell(qtyCell);
            
            table.addCell(new Cell().add(new Paragraph(String.valueOf(product.getMinStockThreshold())))
                    .setTextAlignment(TextAlignment.CENTER));
            table.addCell(new Cell().add(new Paragraph("₹" + product.getUnitPrice()))
                    .setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new Cell().add(new Paragraph(
                    product.getSupplier() != null ? product.getSupplier() : "-")));
        }
        
        document.add(table);
        
        // Summary
        document.add(new Paragraph(" "));
        long lowStockCount = products.stream()
                .filter(p -> p.getQuantity() <= p.getMinStockThreshold())
                .count();
        document.add(new Paragraph("Total Products: " + products.size())
                .setBold()
                .setFontSize(12));
        document.add(new Paragraph("Low Stock Items: " + lowStockCount)
                .setBold()
                .setFontSize(12)
                .setFontColor(ColorConstants.RED));
        
        document.close();
        return baos;
    }
    
    @Override
    public ByteArrayOutputStream generateStockReportCSV(
            List<Product> products,
            String category,
            String supplier,
            Boolean lowStock
    ) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(baos);
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                "SKU", "Product Name", "Category", "Quantity", "Min Stock Level", 
                "Unit Price", "Supplier", "Created Date"
        ));
        
        for (Product product : products) {
            csvPrinter.printRecord(
                    product.getSku(),
                    product.getProductName(),
                    product.getCategory(),
                    product.getQuantity(),
                    product.getMinStockThreshold(),
                    product.getUnitPrice(),
                    product.getSupplier() != null ? product.getSupplier() : "",
                    product.getCreatedAt().format(DATETIME_FORMATTER)
            );
        }
        
        csvPrinter.flush();
        csvPrinter.close();
        return baos;
    }
}
