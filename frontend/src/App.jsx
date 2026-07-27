import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Sidebar } from './components/Sidebar';
import { Topbar } from './components/Topbar';
import { Modal } from './components/Modal';

import { OverviewView } from './views/OverviewView';
import { VendorsView } from './views/VendorsView';
import { OrdersView } from './views/OrdersView';
import { QuotationsView } from './views/QuotationsView';
import { ContractsView } from './views/ContractsView';
import { InventoryView } from './views/InventoryView';

import { previewVendors, previewOrders, request } from './services/api';
import { LogIn, FilePlus2, Plus, Trash2 } from 'lucide-react';

function AppContent() {
  const { token, isAuthenticated, login } = useAuth();
  const [currentView, setCurrentView] = useState('Overview');

  const [vendors, setVendors] = useState(previewVendors);
  const [orders, setOrders] = useState(previewOrders);
  const [selectedVendorId, setSelectedVendorId] = useState(101);

  // Modal states
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [isOrderModalOpen, setIsOrderModalOpen] = useState(false);

  // Form states
  const [loginEmail, setLoginEmail] = useState('admin@procureai.local');
  const [loginPassword, setLoginPassword] = useState('password');
  const [isLoggingIn, setIsLoggingIn] = useState(false);

  const [orderVendorId, setOrderVendorId] = useState('101');
  const [orderAmount, setOrderAmount] = useState('45000');
  const [orderApprovers, setOrderApprovers] = useState('APPROVER_L1, APPROVER_L2');
  const [lineItems, setLineItems] = useState([
    { productName: 'Control Module Alpha', quantity: 50 }
  ]);
  const [isCreatingOrder, setIsCreatingOrder] = useState(false);

  // Toast notifications
  const [toasts, setToasts] = useState([]);

  const showToast = (message, type = 'info') => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 4000);
  };

  const loadLiveData = async () => {
    if (!token) return;
    try {
      const [vData, oData] = await Promise.all([
        request('/vendors?size=20', {}, token),
        request('/purchase-orders?size=20', {}, token)
      ]);
      if (vData?.content) setVendors(vData.content);
      if (oData?.content) setOrders(oData.content);
      showToast('Live workspace synced with backend.');
    } catch (err) {
      showToast(err.message || 'Failed to sync with live backend.', 'error');
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      loadLiveData();
    } else {
      setVendors(previewVendors);
      setOrders(previewOrders);
    }
  }, [isAuthenticated, token]);

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setIsLoggingIn(true);
    try {
      const user = await login(loginEmail.trim(), loginPassword);
      showToast(`Connected workspace as ${user.fullName}.`);
      setIsAuthModalOpen(false);
    } catch (err) {
      showToast(err.message || 'Invalid credentials.', 'error');
    } finally {
      setIsLoggingIn(false);
    }
  };

  const handleCreateOrderSubmit = async (e) => {
    e.preventDefault();
    if (!isAuthenticated) {
      setIsOrderModalOpen(false);
      setIsAuthModalOpen(true);
      showToast('Connect your workspace before creating a purchase order.', 'error');
      return;
    }

    if (lineItems.some(i => !i.productName || !i.quantity || i.quantity < 1)) {
      showToast('Please enter a product name and valid quantity for line items.', 'error');
      return;
    }

    setIsCreatingOrder(true);
    try {
      const approverChain = orderApprovers.trim().replace(/\s*,\s*/g, '>') || null;
      const created = await request('/purchase-orders', {
        method: 'POST',
        body: JSON.stringify({
          vendorId: Number(orderVendorId),
          amount: Number(orderAmount),
          lineItems,
          approverChain
        })
      }, token);

      setOrders(prev => [created, ...prev]);
      showToast(`Purchase order PO-${created.id || 'Draft'} created successfully.`);
      setIsOrderModalOpen(false);
    } catch (err) {
      showToast(err.message || 'Failed to create purchase order.', 'error');
    } finally {
      setIsCreatingOrder(false);
    }
  };

  const addLineItem = () => {
    setLineItems(prev => [...prev, { productName: '', quantity: 1 }]);
  };

  const removeLineItem = (index) => {
    if (lineItems.length <= 1) return;
    setLineItems(prev => prev.filter((_, i) => i !== index));
  };

  const updateLineItem = (index, field, value) => {
    setLineItems(prev => prev.map((item, i) => i === index ? { ...item, [field]: value } : item));
  };

  const pendingCount = orders.filter(o => ['PENDING_L1', 'PENDING_L2', 'SUBMITTED', 'DRAFT'].includes(o.status)).length;

  return (
    <div className="app-shell">
      <Sidebar
        currentView={currentView}
        setCurrentView={setCurrentView}
        vendorCount={vendors.length}
        pendingCount={pendingCount}
        onOpenAuthModal={() => setIsAuthModalOpen(true)}
      />

      <main className="workspace">
        <Topbar
          currentView={currentView}
          onRefresh={loadLiveData}
          onOpenNotifications={() => showToast(`${pendingCount} pending decisions require approval.`)}
        />

        {currentView === 'Overview' && (
          <OverviewView
            vendors={vendors}
            orders={orders}
            selectedVendorId={selectedVendorId}
            setSelectedVendorId={setSelectedVendorId}
            onOpenOrderModal={() => setIsOrderModalOpen(true)}
            onOpenAuthModal={() => setIsAuthModalOpen(true)}
            showToast={showToast}
          />
        )}

        {currentView === 'Suppliers' && (
          <VendorsView
            vendors={vendors}
            onRefresh={loadLiveData}
            showToast={showToast}
          />
        )}

        {currentView === 'Decisions' && (
          <OrdersView
            orders={orders}
            onRefresh={loadLiveData}
            showToast={showToast}
          />
        )}

        {currentView === 'Quotations' && (
          <QuotationsView showToast={showToast} />
        )}

        {currentView === 'Contracts' && (
          <ContractsView showToast={showToast} />
        )}

        {currentView === 'Inventory' && (
          <InventoryView showToast={showToast} />
        )}
      </main>

      {/* Auth / Workspace Connection Modal */}
      <Modal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        title="Connect Vendra Workspace"
      >
        <form onSubmit={handleLoginSubmit}>
          <div className="form-group">
            <label>Work Email</label>
            <input
              type="email"
              className="form-control"
              value={loginEmail}
              onChange={(e) => setLoginEmail(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              className="form-control"
              value={loginPassword}
              onChange={(e) => setLoginPassword(e.target.value)}
              required
            />
          </div>

          <div style={{ fontSize: '11px', color: 'var(--muted)', marginBottom: '16px' }}>
            Demo emails: <code>admin@procureai.local</code>, <code>officer@procureai.local</code> (Password: <code>password</code>)
          </div>

          <button className="btn-primary" type="submit" style={{ width: '100%' }} disabled={isLoggingIn}>
            <LogIn size={16} />
            <span>{isLoggingIn ? 'Connecting...' : 'Connect Workspace'}</span>
          </button>
        </form>
      </Modal>

      {/* Create Purchase Order Modal */}
      <Modal
        isOpen={isOrderModalOpen}
        onClose={() => setIsOrderModalOpen(false)}
        title="Draft Purchase Order"
      >
        <form onSubmit={handleCreateOrderSubmit}>
          <div className="form-group">
            <label>Target Supplier</label>
            <select
              className="form-control"
              value={orderVendorId}
              onChange={(e) => setOrderVendorId(e.target.value)}
            >
              {vendors.map(v => (
                <option key={v.id} value={v.id}>{v.name} ({v.category})</option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label>Estimated Total Amount ($)</label>
            <input
              type="number"
              className="form-control"
              value={orderAmount}
              onChange={(e) => setOrderAmount(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label>Approver Chain</label>
            <input
              type="text"
              className="form-control"
              value={orderApprovers}
              onChange={(e) => setOrderApprovers(e.target.value)}
            />
          </div>

          <div className="form-group">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
              <label style={{ margin: 0 }}>Line Items</label>
              <button type="button" className="btn-secondary" style={{ padding: '4px 8px', fontSize: '11px' }} onClick={addLineItem}>
                <Plus size={12} /> Add Item
              </button>
            </div>

            {lineItems.map((item, idx) => (
              <div key={idx} style={{ display: 'flex', gap: '8px', marginBottom: '8px' }}>
                <input
                  type="text"
                  placeholder="Product name"
                  className="form-control"
                  style={{ flex: 2 }}
                  value={item.productName}
                  onChange={(e) => updateLineItem(idx, 'productName', e.target.value)}
                  required
                />
                <input
                  type="number"
                  placeholder="Qty"
                  className="form-control"
                  style={{ flex: 1 }}
                  value={item.quantity}
                  onChange={(e) => updateLineItem(idx, 'quantity', Number(e.target.value))}
                  required
                />
                {lineItems.length > 1 && (
                  <button type="button" className="icon-button" style={{ width: '36px', height: '36px' }} onClick={() => removeLineItem(idx)}>
                    <Trash2 size={14} color="var(--red)" />
                  </button>
                )}
              </div>
            ))}
          </div>

          <button className="btn-primary" type="submit" style={{ width: '100%', marginTop: '10px' }} disabled={isCreatingOrder}>
            <FilePlus2 size={16} />
            <span>{isCreatingOrder ? 'Creating...' : 'Create Purchase Order Draft'}</span>
          </button>
        </form>
      </Modal>

      {/* Toast Notification Container */}
      <div className="toast-container">
        {toasts.map(t => (
          <div key={t.id} className={`toast ${t.type}`}>
            <span>{t.message}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
