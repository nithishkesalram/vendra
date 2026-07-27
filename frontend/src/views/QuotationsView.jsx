import React, { useState } from 'react';
import { Scale, Sparkles, Award } from 'lucide-react';
import { request } from '../services/api';
import { useAuth } from '../context/AuthContext';

export function QuotationsView({ showToast }) {
  const { token, isAuthenticated } = useAuth();
  const [rfqId, setRfqId] = useState('RFQ-2026-001');
  const [comparison, setComparison] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleCompare = async () => {
    if (!isAuthenticated) return showToast('Connect workspace to compare quotations.', 'error');
    setLoading(true);
    try {
      const res = await request(`/quotations/rfq/${rfqId}/compare`, { method: 'POST' }, token);
      setComparison(res);
      showToast('Deterministic AI quotation comparison computed.');
    } catch (err) {
      showToast(err.message || 'Failed to compare quotations.', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="quotations-view">
      <div className="section-card">
        <div className="section-header">
          <div className="section-title">
            <Scale size={20} />
            <span>RFQ Intake & AI Quotation Comparison</span>
          </div>
        </div>

        <div className="section-body">
          <div style={{ display: 'flex', gap: '12px', alignItems: 'center', marginBottom: '20px' }}>
            <label style={{ fontSize: '13px', fontWeight: 700 }}>Active RFQ Code:</label>
            <input 
              type="text" 
              className="analyst-input" 
              style={{ maxWidth: '240px' }}
              value={rfqId}
              onChange={(e) => setRfqId(e.target.value)}
            />
            <button className="btn-primary" onClick={handleCompare} disabled={loading}>
              <Sparkles size={16} />
              <span>Run AI Comparison Engine</span>
            </button>
          </div>

          {comparison ? (
            <div style={{ background: 'var(--canvas)', padding: '20px', borderRadius: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--aqua-dark)', marginBottom: '14px' }}>
                <Award size={20} />
                <h4 style={{ margin: 0, fontSize: '16px', fontWeight: 800 }}>Comparison Results: {comparison.rfqId}</h4>
              </div>

              {comparison.rankings && comparison.rankings.length > 0 ? (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Rank</th>
                      <th>Supplier ID</th>
                      <th>Total Cost</th>
                      <th>Delivery Days</th>
                      <th>Composite AI Score</th>
                    </tr>
                  </thead>
                  <tbody>
                    {comparison.rankings.map((r, index) => (
                      <tr key={index}>
                        <td><strong>#{index + 1}</strong></td>
                        <td>Supplier #{r.vendorId}</td>
                        <td>${r.totalPrice}</td>
                        <td>{r.deliveryDays} days</td>
                        <td>
                          <span className="score-badge" style={{ color: index === 0 ? 'var(--aqua-dark)' : 'var(--ink)' }}>
                            {(r.compositeScore || 0).toFixed(2)}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <div style={{ fontSize: '13px', color: 'var(--muted)' }}>
                  {comparison.summary || 'Quotation comparison evaluated.'}
                </div>
              )}
            </div>
          ) : (
            <div style={{ padding: '30px', textAlign: 'center', color: 'var(--muted)', fontSize: '13px' }}>
              Click <strong>Run AI Comparison Engine</strong> to analyze multi-supplier quotations for {rfqId}.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
