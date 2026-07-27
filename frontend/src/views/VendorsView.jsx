import React, { useState } from 'react';
import { Users, Plus, ShieldCheck, ShieldAlert, Star } from 'lucide-react';
import { request } from '../services/api';
import { useAuth } from '../context/AuthContext';

export function VendorsView({ vendors = [], onRefresh, showToast }) {
  const { token, isAuthenticated } = useAuth();
  const [filter, setFilter] = useState('ALL');
  const [name, setName] = useState('');
  const [category, setCategory] = useState('Industrial');
  const [rating, setRating] = useState('4.5');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const filteredVendors = vendors.filter(v => {
    if (filter === 'COMPLIANT') return v.complianceStatus === 'COMPLIANT';
    if (filter === 'RISK') return Number(v.riskScore) >= 30;
    return true;
  });

  const handleCreateVendor = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;

    if (!isAuthenticated) {
      showToast('Please connect your workspace to create vendors.', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      await request('/vendors', {
        method: 'POST',
        body: JSON.stringify({
          name: name.trim(),
          category,
          rating: Number(rating),
          complianceStatus: 'COMPLIANT',
          riskScore: 10
        })
      }, token);

      showToast(`Supplier ${name} created successfully.`);
      setName('');
      onRefresh();
    } catch (err) {
      showToast(err.message || 'Failed to create vendor.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="vendors-view">
      <div className="section-card">
        <div className="section-header">
          <div className="section-title">
            <Users size={20} />
            <span>Supplier Directory & Risk Intelligence</span>
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <button 
              className={`btn-secondary ${filter === 'ALL' ? 'active' : ''}`}
              onClick={() => setFilter('ALL')}
            >
              All ({vendors.length})
            </button>
            <button 
              className={`btn-secondary ${filter === 'COMPLIANT' ? 'active' : ''}`}
              onClick={() => setFilter('COMPLIANT')}
            >
              Compliant
            </button>
            <button 
              className={`btn-secondary ${filter === 'RISK' ? 'active' : ''}`}
              onClick={() => setFilter('RISK')}
            >
              High Attention
            </button>
          </div>
        </div>

        <div className="section-body">
          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '24px' }}>
            {/* Vendor List */}
            <div>
              <div className="table-wrapper">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Supplier</th>
                      <th>Category</th>
                      <th>Rating</th>
                      <th>Compliance</th>
                      <th>Risk Score</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredVendors.map(v => (
                      <tr key={v.id}>
                        <td><strong>{v.name}</strong></td>
                        <td>{v.category}</td>
                        <td>
                          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                            <Star size={14} fill="#e6a16d" color="#e6a16d" />
                            {Number(v.rating || 0).toFixed(2)}
                          </span>
                        </td>
                        <td>
                          <span className={`status-pill ${v.complianceStatus !== 'COMPLIANT' ? 'pending' : ''}`}>
                            <i className="pill-dot" />
                            {v.complianceStatus || 'PENDING'}
                          </span>
                        </td>
                        <td>
                          <span className={`risk-score ${Number(v.riskScore) >= 30 ? 'elevated' : ''}`}>
                            {v.riskScore || 0}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Add Vendor Form */}
            <div style={{ background: 'var(--canvas)', padding: '20px', borderRadius: '12px' }}>
              <h4 style={{ margin: '0 0 14px 0', fontSize: '15px', fontWeight: 800 }}>Onboard New Supplier</h4>
              <form onSubmit={handleCreateVendor}>
                <div className="form-group">
                  <label>Supplier Name</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    placeholder="e.g. Apex Dynamics"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Category</label>
                  <select 
                    className="form-control"
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                  >
                    <option value="Industrial">Industrial</option>
                    <option value="Logistics">Logistics</option>
                    <option value="Electronics">Electronics</option>
                    <option value="Raw Materials">Raw Materials</option>
                    <option value="Services">Services</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Initial Score (Rating)</label>
                  <input 
                    type="number" 
                    step="0.1" 
                    max="5.0" 
                    min="1.0" 
                    className="form-control"
                    value={rating}
                    onChange={(e) => setRating(e.target.value)}
                  />
                </div>

                <button className="btn-primary" type="submit" style={{ width: '100%', marginTop: '10px' }} disabled={isSubmitting}>
                  <Plus size={16} />
                  <span>Onboard Supplier</span>
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
