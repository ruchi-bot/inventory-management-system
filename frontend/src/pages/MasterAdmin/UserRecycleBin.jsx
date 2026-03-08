import React, { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { FiRefreshCw, FiTrash2, FiAlertCircle, FiSearch } from 'react-icons/fi';
import { masterAdminAPI } from '../../services/api';
import { formatDate } from '../../utils/helpers';
import './Dashboard.css';
import './RecycleBin.css';

const UserRecycleBin = () => {
  const [deletedUsers, setDeletedUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filteredUsers, setFilteredUsers] = useState([]);

  useEffect(() => {
    fetchDeletedUsers();
  }, []);

  useEffect(() => {
    filterUsers();
  }, [deletedUsers, searchTerm]);

  const fetchDeletedUsers = async () => {
    try {
      const response = await masterAdminAPI.getDeletedUsers();
      setDeletedUsers(response.data);
    } catch (error) {
      console.error('Failed to fetch deleted users:', error);
      toast.error('Failed to fetch deleted users');
    } finally {
      setLoading(false);
    }
  };

  const filterUsers = () => {
    if (!searchTerm.trim()) {
      setFilteredUsers(deletedUsers);
      return;
    }

    const filtered = deletedUsers.filter(user =>
      user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.firstName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.lastName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.role.toLowerCase().includes(searchTerm.toLowerCase())
    );
    setFilteredUsers(filtered);
  };

  const handleRestore = async (userId) => {
    if (window.confirm('Are you sure you want to restore this user?')) {
      try {
        await masterAdminAPI.restoreUser(userId);
        toast.success('User restored successfully');
        fetchDeletedUsers();
      } catch (error) {
        console.error('Error restoring user:', error);
        toast.error(error.response?.data?.message || 'Failed to restore user');
      }
    }
  };

  const handlePermanentDelete = async (userId) => {
    if (window.confirm('⚠️ WARNING: This will PERMANENTLY delete the user. This action cannot be undone! Are you absolutely sure?')) {
      try {
        await masterAdminAPI.permanentDeleteUser(userId);
        toast.success('User permanently deleted');
        fetchDeletedUsers();
      } catch (error) {
        console.error('Error permanently deleting user:', error);
        toast.error('Failed to permanently delete user');
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
    if (days === 'Unknown') return 'text-gray';
    if (days <= 5) return 'red';
    if (days <= 10) return 'orange';
    return 'green';
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1>User Recycle Bin</h1>
          <p>Deleted users will be automatically removed after 30 days</p>
        </div>
        <div className="info-badge">
          <FiAlertCircle />
          <span>{deletedUsers.length} user{deletedUsers.length !== 1 ? 's' : ''} in recycle bin</span>
        </div>
      </div>

      <div className="search-box">
        <FiSearch />
        <input
          type="text"
          placeholder="Search deleted users..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>

      {loading ? (
        <div className="loading">Loading deleted users...</div>
      ) : filteredUsers.length === 0 ? (
        <div className="empty-state">
          <FiTrash2 className="empty-icon" />
          <h2>{searchTerm ? 'No matching users found' : 'Recycle bin is empty'}</h2>
          <p>{searchTerm ? 'Try adjusting your search criteria' : 'Deleted users will appear here'}</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Email</th>
                <th>Name</th>
                <th>Role</th>
                <th>Created At</th>
                <th>Deleted At</th>
                <th>Days Remaining</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.map((user) => (
                <tr key={user.id}>
                  <td>{user.email}</td>
                  <td>{user.firstName} {user.lastName}</td>
                  <td>
                    <span className={`role-badge ${user.role.toLowerCase().replace('_', '-')}`}>
                      {user.role.replace('_', ' ')}
                    </span>
                  </td>
                  <td>{formatDate(user.createdAt)}</td>
                  <td className="deleted-at">
                    {user.deletedAt ? formatDate(user.deletedAt) : <span className="text-gray">Unknown</span>}
                  </td>
                  <td className={getDaysRemaining(user.deletedAt) === 'Unknown' ? 'text-gray' : getExpiryStatus(user.deletedAt)}>
                    {user.deletedAt ? `${getDaysRemaining(user.deletedAt)} days` : 'Not set - restore soon!'}
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="btn-icon btn-success"
                        onClick={() => handleRestore(user.id)}
                        title="Restore User"
                      >
                        <FiRefreshCw />
                      </button>
                      <button
                        className="btn-icon btn-danger"
                        onClick={() => handlePermanentDelete(user.id)}
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

export default UserRecycleBin;
