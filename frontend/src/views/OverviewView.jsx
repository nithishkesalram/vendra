import React, { useState } from 'react';
import { 
  Plus, 
  Sparkles, 
  Clock, 
  Send, 
  ShieldAlert, 
  FileSearch, 
  Database,
  ArrowUpRight
} from 'lucide-react';
import { NetworkCanvas } from '../components/NetworkCanvas';
import { useAuth } from '../context/AuthContext';
import { request } from '../services/api';

const currencyFormatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });

export function OverviewView({ 
  vendors = [], 
  orders = [], 
  selectedVendorId, 
  setSelectedVendorId, 
  onOpenOrderModal, 
  onOpenAuthModal,
  showToast 
}) {
  const { token, isAuthenticated } = useAuth();
  const [chatInput, setChatInput] = useState('');
  const [chatResponse, setChatResponse] = useState('Connect your workspace or ask the analyst for supplier risk signals, pending PO recommendations, or quote evaluations.');
  const [citations, setCitations] = useState([]);
  const [isAsking, setIsAsking] = useState(false);

  const totalSpend = orders.length 
    ? orders.reduce((acc, item) => acc + Number(item.amount || 0), 0) 
    : 379750;

  const avgRating = vendors.length 
    ? vendors.reduce((acc, v) => acc + Number(v.rating || 0), 0) / vendors.length 
    : 4.5;

  const avgRisk = vendors.length 
    ? vendors.reduce((acc, v) => acc + Number(v.riskScore || 0), 0) / vendors.length 
    : 22;

  const riskVendors = vendors.filter(v => Number(v.riskScore) >= 30 || v.complianceStatus !== 'COMPLIANT');
  const actionableOrders = orders.filter(o => ['PENDING_L1', 'PENDING_L2', 'SUBMITTED', 'DRAFT'].includes(o.status));

  const handleAskAnalyst = async (e) => {
    e.preventDefault();
    if (!chatInput.trim()) return;

    if (!isAuthenticated) {
      onOpenAuthModal();
      showToast('Connect your workspace to ask the AI analyst.', 'error');
      return;
    }

    setIsAsking(true);
    setChatResponse('Reviewing connected procurement data...');
    try {
      const res = await request('/ai/chat', {
        method: 'POST',
        body: JSON.stringify({ message: chatInput.trim(), vendorId: selectedVendorId, history: [] })
      }, token);

      setChatResponse(res.reply || 'The analyst did not return a response.');
      setCitations(res.citations || []);
      setChatInput('');
    } catch (err) {
      setChatResponse('The analyst could not complete that request.');
      showToast(err.message || 'Unable to reach AI analyst.', 'error');
    } finally {
      setIsAsking(false);
    }
  };

  const getComplianceLabel = (status) => 
    String(status || 'PENDING_REVIEW')
      .replace('_', ' ')
      .toLowerCase()
      .replace(/\b\w/g, c => c.toUpperCase());

  const getInitials = (name) => 
    String(name || 'Vendra')
      .split(' ')
      .slice(0, 2)
      .map(p => p[0])
      .join('')
      .toUpperCase();

  return (
    <div className="overview-view">
      {/* Hero Section */}
      <section className="hero">
        <div className="hero-main">
          <h1>Procurement Command Center</h1>
          <p>
            Automated vendor intelligence, PO workflow approvals, contract risk extraction, 
            and AI-driven procurement recommendations.
          </p>

          <div className="hero-actions">
            <button className="btn-primary" onClick={onOpenOrderModal}>
              <Plus size={16} />
              <span>Create Purchase Order</span>
            </button>
            <button 
              className="btn-secondary" 
              onClick={() => {
                setChatInput("Which supplier should I review before the next award decision?");
              }}
            >
              <Sparkles size={16} />
              <span>Ask AI Analyst</span>
            </button>
          </div>
        </div>

        <div className="hero-card">
          <NetworkCanvas selectedVendorId={selectedVendorId} vendors={vendors} />
          <div className="hero-card-content">
            <div>
              <div className="hero-card-title">SUPPLIER NETWORK GRAPH</div>
              <div className="hero-card-meta">
                <div className="hero-card-value">
                  {Math.max(0, 100 - avgRisk * 0.25).toFixed(1)}%
                </div>
                <small style={{ color: '#a2c8c0', fontSize: '12px' }}>Portfolio Resilience Index</small>
              </div>
            </div>

            <div className="hero-card-foot">
              <span>{vendors.length} Active Suppliers</span>
              <span>{riskVendors.length} High Attention</span>
            </div>
          </div>
        </div>
      </section>

      {/* Summary Cards */}
      <section className="summary-grid">
        <div className="summary-card">
          <div className="summary-title">MANAGED SPEND</div>
          <div className="summary-value">{currencyFormatter.format(totalSpend)}</div>
          <div className="summary-meta">{orders.length} active purchase orders</div>
        </div>

        <div className="summary-card">
          <div className="summary-title">SUPPLIER HEALTH</div>
          <div className="summary-value">{avgRating.toFixed(2)} / 5.0</div>
          <div className="summary-meta">Across active portfolio</div>
        </div>

        <div className="summary-card">
          <div className="summary-title">ATTENTION SIGNALS</div>
          <div className="summary-value" style={{ color: riskVendors.length ? 'var(--orange)' : 'var(--ink)' }}>
            {String(riskVendors.length).padStart(2, '0')}
          </div>
          <div className="summary-meta">Compliance or elevated risk</div>
        </div>

        <div className="summary-card">
          <div className="summary-title">DECISION QUEUE</div>
          <div className="summary-value" style={{ color: actionableOrders.length ? 'var(--aqua-dark)' : 'var(--ink)' }}>
            {String(actionableOrders.length).padStart(2, '0')}
          </div>
          <div className="summary-meta">Pending PO approvals</div>
        </div>
      </section>

      {/* Main Dashboard Layout */}
      <div className="dashboard-grid">
        {/* Left Column: Vendor Table */}
        <div className="dashboard-left">
          <div className="section-card">
            <div className="section-header">
              <div className="section-title">
                <Database size={18} />
                <span>Supplier Portfolio</span>
              </div>
              <span style={{ fontSize: '12px', color: 'var(--muted)', fontWeight: 600 }}>
                {vendors.length} Total
              </span>
            </div>

            <div className="section-body" style={{ padding: 0 }}>
              <div className="table-wrapper">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Supplier Name</th>
                      <th>Rating</th>
                      <th>Compliance</th>
                      <th>Risk Score</th>
                    </tr>
                  </thead>
                  <tbody>
                    {vendors.map(vendor => {
                      const isPending = vendor.complianceStatus !== 'COMPLIANT';
                      const isElevated = Number(vendor.riskScore) >= 30;
                      const isSelected = vendor.id === selectedVendorId;

                      return (
                        <tr 
                          key={vendor.id} 
                          className={isSelected ? 'selected' : ''}
                          onClick={() => setSelectedVendorId(vendor.id)}
                        >
                          <td>
                            <div className="supplier-cell">
                              <span className="supplier-badge">{getInitials(vendor.name)}</span>
                              <div className="supplier-info">
                                <strong>{vendor.name}</strong>
                                <span>{vendor.category}</span>
                              </div>
                            </div>
                          </td>
                          <td>
                            <span className="score-badge">
                              {Number(vendor.rating || 0).toFixed(2)}
                            </span>
                          </td>
                          <td>
                            <span className={`status-pill ${isPending ? 'pending' : ''}`}>
                              <i className="pill-dot" />
                              {getComplianceLabel(vendor.complianceStatus)}
                            </span>
                          </td>
                          <td>
                            <span className={`risk-score ${isElevated ? 'elevated' : ''}`}>
                              {String(vendor.riskScore || 0).padStart(2, '0')}
                            </span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Decisions & AI Analyst */}
        <div className="dashboard-right">
          {/* Approval Queue */}
          <div className="section-card">
            <div className="section-header">
              <div className="section-title">
                <Clock size={18} />
                <span>Decision Queue</span>
              </div>
              <span style={{ fontSize: '12px', color: 'var(--muted)', fontWeight: 600 }}>
                {actionableOrders.length} Waiting
              </span>
            </div>

            <div className="section-body">
              {actionableOrders.length === 0 ? (
                <div style={{ color: 'var(--muted)', fontSize: '13px', textAlign: 'center', padding: '16px 0' }}>
                  No pending approvals in queue.
                </div>
              ) : (
                <div className="approval-list">
                  {actionableOrders.slice(0, 4).map(order => (
                    <div key={order.id} className="approval-item">
                      <div className="approval-icon">
                        <Clock size={18} />
                      </div>
                      <div className="approval-main">
                        <strong>{order.vendorName || 'Supplier order'}</strong>
                        <span>{String(order.status).replace('_', ' ')} | {order.approverChain || 'Routing'}</span>
                      </div>
                      <div className="approval-amount">
                        {currencyFormatter.format(Number(order.amount || 0))}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* AI Procurement Analyst Chat Workspace */}
          <div className="section-card">
            <div className="section-header">
              <div className="section-title">
                <Sparkles size={18} />
                <span>AI Procurement Analyst</span>
              </div>
            </div>

            <div className="section-body">
              <div className="analyst-box">
                <div className="analyst-response">
                  {chatResponse}
                </div>

                {citations.length > 0 && (
                  <div className="citation-list">
                    {citations.map((c, idx) => (
                      <span key={idx} className="citation">
                        <FileSearch size={12} />
                        {c.source || c.title || 'Retrieved record'}
                      </span>
                    ))}
                  </div>
                )}

                <form onSubmit={handleAskAnalyst} className="analyst-form">
                  <input
                    type="text"
                    className="analyst-input"
                    placeholder="Ask about risk, quotations, or vendor scores..."
                    value={chatInput}
                    onChange={(e) => setChatInput(e.target.value)}
                    disabled={isAsking}
                  />
                  <button className="btn-primary" type="submit" disabled={isAsking}>
                    <Send size={15} />
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
