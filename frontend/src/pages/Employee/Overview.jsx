import React, { useState, useEffect } from 'react';
import { FiPackage, FiDollarSign, FiAlertTriangle, FiTrendingUp } from 'react-icons/fi';
import { employeeAPI } from '../../services/api';
import { toast } from 'react-toastify';
import { formatCurrency } from '../../utils/helpers';
import LowStockAlerts from '../../components/LowStockAlerts';

const Overview = () => {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const summaryRes = await employeeAPI.getInventorySummary();
      setSummary(summaryRes.data);
    } catch (error) {
      toast.error('Failed to fetch dashboard data');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <div className="overview">
      <h1>Employee Dashboard</h1>
      <p className="subtitle">Manage stock and view inventory</p>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon blue">
            <FiPackage />
          </div>
          <div className="stat-info">
            <h3>{summary?.totalProducts || 0}</h3>
            <p>Total Products</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon green">
            <FiTrendingUp />
          </div>
          <div className="stat-info">
            <h3>{summary?.totalQuantity || 0}</h3>
            <p>Total Stock</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon purple">
            <FiDollarSign />
          </div>
          <div className="stat-info">
            <h3>{formatCurrency(summary?.totalValue || 0)}</h3>
            <p>Total Value</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon red">
            <FiAlertTriangle />
          </div>
          <div className="stat-info">
            <h3>{summary?.lowStockItems || 0}</h3>
            <p>Low Stock Items</p>
          </div>
        </div>
      </div>

      <LowStockAlerts role="EMPLOYEE" />
    </div>
  );
};

export default Overview;
