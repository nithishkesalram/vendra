import React, { useState } from 'react';
import { FileCheck2, Send, CheckCircle2, XCircle, Clock } from 'lucide-react';
import { request } from '../services/api';
import { useAuth } from '../context/AuthContext';

const currencyFormatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });

export function OrdersView({ orders = [], onRefresh, showToast }) {
  const { token, isAuthenticated } = useAuth();
  const [filter, setFilter] = useState('ALL');
  const [loadingId, setLoadingId] = useState(null);

  const filteredOrders = orders.filter(o => {
    if (filter === 'ACTION') return ['DRAFT', 'SUBMITTED', 'PENDING_L1', 'PENDING_L2'].includes(o.status);
    if (filter === 'APPROVED') return o.status === 'APPROVED';
    return true;
  });

  const handleSubmit = async (id) => {
    if (!isAuthenticated) return showToast('Connect your workspace first.', 'error');
    setLoadingId(id);
    try {
      await request(`/purchase-orders/${id}/submit`, { method: 'POST' }, token);
      showToast(`Purchase order #${id} submitted for approval.`);
      onRefresh();
    } catch (err) {
      showToast(err.message || 'Failed to submit order.', 'error');
    } finally {
      setLoadingId(null);
    }
  };

  const handleApprove = async (id) => {
    if (!isAuthenticated) return showToast('Connect your workspace first.', 'error');
    setLoadingId(id);
    try {
      await request(`/purchase-orders/${id}/approve`, {
        method: 'POST',
        body: JSON.stringify({ decision: 'APPROVED', comment: 'Approved in workspace' })
      }, token);
      showToast(`Purchase order #${id} decision logged.`);
      onRefresh();
    } catch (err) {
      showToast(err.message || 'Failed to record decision.', 'error');
    } finally {
      setLoadingId(null);
    }
  };

  const handleReject = async (id) => {
    if (!isAuthenticated) return showToast('Connect your workspace first.', 'error');
    setLoadingId(id);
    try {
      await request(`/purchase-orders/${id}/approve`, {
        method: 'POST',
        body: JSON.stringify({ decision: 'REJECTED', comment: 'Rejected in workspace' })
      }, token);
      showToast(`Purchase order #${id} rejected.`);
      onRefresh();
    } catch (err) {
      showToast(err.message || 'Failed to record rejection.', 'error');
    } finally {
      setLoadingId(null);
    }
  };

  return (
    <div className="orders-view">
      <div className="section-card">
        <div className="section-header">
          <div className="section-title">
            <FileCheck2 size={20} />
            <span>Purchase Order Lifecycle & Approval Chain</span>
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <button className={`btn-secondary ${filter === 'ALL' ? 'active' : ''}`} onClick={() => setFilter('ALL')}>
              All ({orders.length})
            </button>
            <button className={`btn-secondary ${filter === 'ACTION' ? 'active' : ''}`} onClick={() => setFilter('ACTION')}>
              Pending Decisions
            </button>
            <button className={`btn-secondary ${filter === 'APPROVED' ? 'active' : ''}`} onClick={() => setFilter('APPROVED')}>
              Approved
            </button>
          </div>
        </div>

        <div className="section-body">
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>PO Reference</th>
                  <th>Supplier</th>
                  <th>Amount</th>
                  <th>Routing Chain</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredOrders.map(o => (
                  <tr key={o.id}>
                    <td><strong>PO-{o.id}</strong></td>
                    <td>{o.vendorName || `Supplier #${o.vendorId}`}</td>
                    <td><strong className="score-badge">{currencyFormatter.format(Number(o.amount || 0))}</strong></td>
                    <td><small style={{ color: 'var(--muted)' }}>{o.approverChain || 'APPROVER_L1 > APPROVER_L2'}</small></td>
                    <td>
                      <span className={`status-pill ${o.status === 'APPROVED' ? '' : o.status === 'REJECTED' ? 'rejected' : 'pending'}`}>
                        <i className="pill-dot" />
                        {String(o.status).replace('_', ' ')}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '6px' }}>
                        {o.status === 'DRAFT' && (
                          <button 
                            className="btn-primary" 
                            style={{ padding: '6px 12px', fontSize: '11px' }}
                            onClick={() => handleSubmit(o.id)}
                            disabled={loadingId === o.id}
                          >
                            <Send size={12} /> Submit
                          </button>
                        )}
                        {['SUBMITTED', 'PENDING_L1', 'PENDING_L2'].includes(o.status) && (
                          <>
                            <button 
                              className="btn-primary" 
                              style={{ padding: '6px 12px', fontSize: '11px', background: 'var(--aqua-dark)' }}
                              onClick={() => handleApprove(o.id)}
                              disabled={loadingId === o.id}
                            >
                              <CheckCircle2 size={12} /> Approve
                            </button>
                            <button 
                              className="btn-secondary" 
                              style={{ padding: '6px 12px', fontSize: '11px', color: 'var(--red)' }}
                              onClick={() => handleReject(o.id)}
                              disabled={loadingId === o.id}
                            >
                              <XCircle size={12} /> Reject
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
