import React, { useState, useEffect } from 'react';
import { employeeAPI } from '../../services/api';
import { toast } from 'react-toastify';
import { formatCurrency, formatDate } from '../../utils/helpers';

const Reports = () => {
  const [summary, setSummary] = useState(null);
  const [lowStockProducts, setLowStockProducts] = useState([]);
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchReports();
  }, []);

  const fetchReports = async () => {
    try {
      const [summaryRes, lowStockRes, transactionsRes] = await Promise.all([
        employeeAPI.getInventorySummary(),
        employeeAPI.getLowStockProducts(),
        employeeAPI.getTransactions({})
      ]);
      setSummary(summaryRes.data);
      setLowStockProducts(lowStockRes.data);
      setRecentTransactions(transactionsRes.data.slice(0, 10)); // Get last 10 transactions
    } catch (error) {
      toast.error('Failed to fetch reports');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="loading">Loading reports...</div>;
  }

  return (
    <div className="reports">
      <h1>Reports</h1>
      <p className="subtitle">Inventory summary and insights</p>

      <div className="stats-grid">
        <div className="stat-card">
          <h3>{summary?.totalProducts || 0}</h3>
          <p>Total Products</p>
        </div>
        <div className="stat-card">
          <h3>{summary?.totalQuantity || 0}</h3>
          <p>Total Stock</p>
        </div>
        <div className="stat-card">
          <h3>{formatCurrency(summary?.totalValue || 0)}</h3>
          <p>Total Value</p>
        </div>
        <div className="stat-card">
          <h3>{summary?.lowStockItems || 0}</h3>
          <p>Low Stock Items</p>
        </div>
      </div>

      {recentTransactions.length > 0 && (
        <div className="report-table">
          <h2>Recent Transactions</h2>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>SKU</th>
                  <th>Product</th>
                  <th>Type</th>
                  <th>Quantity</th>
                  <th>Notes</th>
                </tr>
              </thead>
              <tbody>
                {recentTransactions.map((transaction) => (
                  <tr key={transaction.id}>
                    <td>{formatDate(transaction.transactionDate)}</td>
                    <td>{transaction.sku}</td>
                    <td>{transaction.productName}</td>
                    <td>
                      <span className={`type-badge ${transaction.transactionType.toLowerCase().replace('_', '-')}`}>
                        {transaction.transactionType.replace('_', ' ')}
                      </span>
                    </td>
                    <td>{transaction.quantity}</td>
                    <td>{transaction.notes || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {lowStockProducts.length > 0 && (
        <div className="report-table">
          <h2>Low Stock Alert</h2>
          <p className="subtitle">Products requiring immediate attention</p>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Product Name</th>
                  <th>Category</th>
                  <th>Current Stock</th>
                  <th>Min Threshold</th>
                  <th>Shortage</th>
                  <th>Unit Price</th>
                  <th>Reorder Value</th>
                </tr>
              </thead>
              <tbody>
                {lowStockProducts.map((product) => {
                  const shortage = product.minStockThreshold - product.quantity;
                  return (
                    <tr key={product.id}>
                      <td>{product.sku}</td>
                      <td>{product.productName}</td>
                      <td>{product.category}</td>
                      <td className="text-red"><strong>{product.quantity}</strong></td>
                      <td>{product.minStockThreshold}</td>
                      <td className="text-red"><strong>{shortage}</strong></td>
                      <td>{formatCurrency(product.unitPrice)}</td>
                      <td><strong>{formatCurrency(shortage * product.unitPrice)}</strong></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default Reports;
