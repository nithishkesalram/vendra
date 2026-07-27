import React, { useState } from 'react';
import { Boxes, Search } from 'lucide-react';
import { request } from '../services/api';
import { useAuth } from '../context/AuthContext';

export function InventoryView({ showToast }) {
  const { token, isAuthenticated } = useAuth();
  const [sku, setSku] = useState('ELEC-CTRL-01');
  const [stockInfo, setStockInfo] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleLookup = async (e) => {
    e.preventDefault();
    if (!sku.trim()) return;
    if (!isAuthenticated) return showToast('Connect workspace to query inventory.', 'error');

    setLoading(true);
    try {
      const res = await request(`/inventory/${sku.trim()}`, {}, token);
      setStockInfo(res);
      showToast(`SKU inventory loaded.`);
    } catch (err) {
      showToast(err.message || 'SKU not found in inventory.', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="inventory-view">
      <div className="section-card">
        <div className="section-header">
          <div className="section-title">
            <Boxes size={20} />
            <span>Inventory SKU Lookup</span>
          </div>
        </div>

        <div className="section-body">
          <form onSubmit={handleLookup} style={{ display: 'flex', gap: '10px', maxWidth: '480px', marginBottom: '24px' }}>
            <input 
              type="text" 
              className="analyst-input" 
              placeholder="e.g. ELEC-CTRL-01, MET-ALUM-500..."
              value={sku}
              onChange={(e) => setSku(e.target.value)}
              required
            />
            <button className="btn-primary" type="submit" disabled={loading}>
              <Search size={16} />
              <span>Lookup SKU</span>
            </button>
          </form>

          {stockInfo && (
            <div style={{ background: 'var(--paper)', border: '1px solid var(--line)', padding: '20px', borderRadius: '12px', maxWidth: '480px' }}>
              <h4 style={{ margin: '0 0 10px 0', fontSize: '16px', fontWeight: 800 }}>SKU: {stockInfo.sku || sku}</h4>
              <div style={{ display: 'grid', gap: '8px', fontSize: '13px' }}>
                <div><strong>Available Quantity:</strong> <span style={{ color: 'var(--aqua-dark)', fontWeight: 800 }}>{stockInfo.quantityAvailable ?? 1250}</span> units</div>
                <div><strong>Reorder Level:</strong> {stockInfo.reorderThreshold ?? 300} units</div>
                <div><strong>Status:</strong> <span className="status-pill"><i className="pill-dot" />{stockInfo.status || 'IN_STOCK'}</span></div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
