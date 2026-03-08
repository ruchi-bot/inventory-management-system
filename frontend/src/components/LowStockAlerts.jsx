import React, { useState, useEffect } from 'react';
import { FiBell, FiAlertTriangle, FiX, FiCheck } from 'react-icons/fi';
import { adminAPI, employeeAPI } from '../services/api';
import { toast } from 'react-toastify';
import './LowStockAlerts.css';

const LowStockAlerts = ({ role }) => {
  const [alerts, setAlerts] = useState([]);
  const [summary, setSummary] = useState(null);
  const [showAll, setShowAll] = useState(false);
  const [loading, setLoading] = useState(true);

  const api = role === 'EMPLOYEE' ? employeeAPI : adminAPI;

  useEffect(() => {
    fetchData();
    // Refresh alerts every 5 minutes
    const interval = setInterval(fetchData, 300000);
    return () => clearInterval(interval);
  }, []);

  const fetchData = async () => {
    try {
      const [alertsRes, summaryRes] = await Promise.all([
        api.getActiveAlerts(),
        api.getAlertSummary()
      ]);
      setAlerts(alertsRes.data);
      setSummary(summaryRes.data);
    } catch (error) {
      console.error('Failed to fetch alerts', error);
    } finally {
      setLoading(false);
    }
  };

  const handleResolve = async (alertId) => {
    if (role === 'EMPLOYEE') {
      toast.info('Only admins can resolve alerts');
      return;
    }
    
    try {
      await adminAPI.resolveAlert(alertId);
      toast.success('Alert resolved successfully');
      fetchData();
    } catch (error) {
      toast.error('Failed to resolve alert');
    }
  };

  const displayedAlerts = showAll ? alerts : alerts.slice(0, 5);

  if (loading) return null;
  if (!summary || summary.totalActiveAlerts === 0) return null;

  return (
    <div className="low-stock-alerts">
      <div className="alerts-header">
        <div className="alerts-title">
          <FiAlertTriangle className="icon-warning" />
          <h3>Low Stock Alerts</h3>
          <span className="alert-count">{summary.totalActiveAlerts}</span>
        </div>
        {summary.todayAlerts > 0 && (
          <span className="badge-new">{summary.todayAlerts} new today</span>
        )}
      </div>

      <div className="alerts-list">
        {displayedAlerts.map((alert) => (
          <div key={alert.id} className="alert-item">
            <div className="alert-content">
              <div className="alert-icon">
                <FiBell />
              </div>
              <div className="alert-details">
                <h4>{alert.productName}</h4>
                <p><strong>SKU:</strong> {alert.sku}</p>
                <p>
                  <strong>Current Stock:</strong> {alert.currentQuantity} units | 
                  <strong> Threshold:</strong> {alert.threshold} units
                </p>
                <span className="alert-time">
                  {new Date(alert.alertSentAt).toLocaleString()}
                </span>
              </div>
            </div>
            {role === 'ADMIN' && (
              <button
                className="btn-resolve"
                onClick={() => handleResolve(alert.id)}
                title="Mark as resolved"
              >
                <FiCheck />
              </button>
            )}
          </div>
        ))}
      </div>

      {alerts.length > 5 && (
        <button
          className="btn-show-all"
          onClick={() => setShowAll(!showAll)}
        >
          {showAll ? 'Show Less' : `Show All (${alerts.length})`}
        </button>
      )}
    </div>
  );
};

export default LowStockAlerts;
