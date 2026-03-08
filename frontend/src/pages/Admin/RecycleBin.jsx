import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { FiRefreshCw, FiTrash2, FiAlertCircle, FiSearch } from 'react-icons/fi';
import { adminAPI } from '../../services/api';
import { formatCurrency, formatDate } from '../../utils/helpers';
import './Dashboard.css';
import './RecycleBin.css';

const RecycleBin = () => {
  const [deletedProducts, setDeletedProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filteredProducts, setFilteredProducts] = useState([]);

  useEffect(() => {
    fetchDeletedProducts();
  }, []);

  useEffect(() => {
    filterProducts();
  }, [searchTerm, deletedProducts]);

  const fetchDeletedProducts = async () => {
    try {
      setLoading(true);
      const response = await adminAPI.getDeletedProducts();
      setDeletedProducts(response.data);
    } catch (error) {
      console.error('Error fetching deleted products:', error);
      toast.error('Failed to load deleted products');
    } finally {
      setLoading(false);
    }
  };

  const filterProducts = () => {
    if (!searchTerm.trim()) {
      setFilteredProducts(deletedProducts);
      return;
    }

    const filtered = deletedProducts.filter(product =>
      product.productName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      product.sku.toLowerCase().includes(searchTerm.toLowerCase()) ||
      product.category.toLowerCase().includes(searchTerm.toLowerCase()) ||
      product.supplier.toLowerCase().includes(searchTerm.toLowerCase())
    );
    setFilteredProducts(filtered);
  };

  const handleRestore = async (productId) => {
    if (window.confirm('Are you sure you want to restore this product?')) {
      try {
        await adminAPI.restoreProduct(productId);
        toast.success('Product restored successfully!');
        fetchDeletedProducts();
      } catch (error) {
        console.error('Error restoring product:', error);
        toast.error('Failed to restore product');
      }
    }
  };

  const handlePermanentDelete = async (productId) => {
    if (window.confirm('⚠️ WARNING: This will PERMANENTLY delete the product. This action cannot be undone! Are you absolutely sure?')) {
      try {
        await adminAPI.permanentDeleteProduct(productId);
        toast.success('Product permanently deleted');
        fetchDeletedProducts();
      } catch (error) {
        console.error('Error permanently deleting product:', error);
        toast.error('Failed to permanently delete product');
      }
    }
  };

  const getDaysRemaining = (deletedAt) => {
    if (!deletedAt) return 'Unknown';
    const deletedDate = new Date(deletedAt);
    const expiryDate = new Date(deletedDate);
    expiryDate.setDate(expiryDate.getDate() + 30);
    const today = new Date();
    const daysRemaining = Math.ceil((expiryDate - today) / (1000 * 60 * 60 * 24));
    return daysRemaining > 0 ? daysRemaining : 0;
  };

  const getExpiryStatus = (deletedAt) => {
    const days = getDaysRemaining(deletedAt);
    if (days === 'Unknown') return 'text-gray-500 italic';
    if (days <= 5) return 'text-red-600 font-bold';
    if (days <= 10) return 'text-orange-600 font-semibold';
    return 'text-green-600';
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-xl text-gray-600">Loading...</div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1>Recycle Bin</h1>
          <p>Products deleted will be automatically removed after 30 days</p>
        </div>
        <div className="info-badge">
          <FiAlertCircle />
          <span>{deletedProducts.length} item{deletedProducts.length !== 1 ? 's' : ''} in recycle bin</span>
        </div>
      </div>

      <div className="search-box">
        <FiSearch />
        <input
          type="text"
          placeholder="Search deleted products..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>

      {loading ? (
        <div className="loading">Loading deleted products...</div>
      ) : filteredProducts.length === 0 ? (
        <div className="empty-state">
          <FiTrash2 className="empty-icon" />
          <h2>{searchTerm ? 'No matching products found' : 'Recycle bin is empty'}</h2>
          <p>{searchTerm ? 'Try adjusting your search criteria' : 'Deleted products will appear here'}</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>SKU</th>
                <th>Product Name</th>
                <th>Category</th>
                <th>Supplier</th>
                <th>Price</th>
                <th>Quantity</th>
                <th>Deleted At</th>
                <th>Days Remaining</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredProducts.map((product) => (
                <tr key={product.id}>
                  <td>{product.sku}</td>
                  <td>{product.productName}</td>
                  <td>{product.category}</td>
                  <td>{product.supplier}</td>
                  <td>{formatCurrency(product.unitPrice)}</td>
                  <td>{product.quantity}</td>
                  <td className="deleted-at">
                    {product.deletedAt ? formatDate(product.deletedAt) : <span className="text-gray">Unknown</span>}
                  </td>
                  <td className={getDaysRemaining(product.deletedAt) === 'Unknown' ? 'text-gray' : getExpiryStatus(product.deletedAt)}>
                    {product.deletedAt ? `${getDaysRemaining(product.deletedAt)} days` : 'Not set - restore soon!'}
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="btn-icon btn-success"
                        onClick={() => handleRestore(product.id)}
                        title="Restore Product"
                      >
                        <FiRefreshCw />
                      </button>
                      <button
                        className="btn-icon btn-danger"
                        onClick={() => handlePermanentDelete(product.id)}
                        title="Permanently Delete"
                      >
                        <FiTrash2 />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="legend-box">
        <h3>Status Legend:</h3>
        <div className="legend-items">
          <div className="legend-item">
            <span className="legend-dot green"></span>
            <span>More than 10 days remaining</span>
          </div>
          <div className="legend-item">
            <span className="legend-dot orange"></span>
            <span>6-10 days remaining</span>
          </div>
          <div className="legend-item">
            <span className="legend-dot red"></span>
            <span>5 days or less (Critical)</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RecycleBin;
