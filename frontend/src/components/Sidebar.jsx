import React from 'react';
import { 
  LayoutDashboard, 
  Users, 
  FileCheck2, 
  Scale, 
  FileText, 
  Boxes, 
  PlugZap, 
  LogOut 
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export function Sidebar({ currentView, setCurrentView, vendorCount = 0, pendingCount = 0, onOpenAuthModal }) {
  const { user, isAuthenticated, logout } = useAuth();

  const navItems = [
    { id: 'Overview', label: 'Overview', icon: LayoutDashboard },
    { id: 'Suppliers', label: 'Suppliers', icon: Users, count: vendorCount },
    { id: 'Decisions', label: 'Approval Queue', icon: FileCheck2, count: pendingCount },
    { id: 'Quotations', label: 'RFQs & Quotes', icon: Scale },
    { id: 'Contracts', label: 'Contracts & Risk', icon: FileText },
    { id: 'Inventory', label: 'Inventory SKU', icon: Boxes }
  ];

  const userInitials = user?.fullName
    ? user.fullName.split(' ').slice(0, 2).map(n => n[0]).join('').toUpperCase()
    : 'V';

  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark">V</div>
        <div>
          <strong>VENDRA</strong>
          <small>Procurement AI</small>
        </div>
      </div>

      <div className="nav-label">Navigation</div>
      <nav className="nav-list">
        {navItems.map(item => {
          const Icon = item.icon;
          const isActive = currentView === item.id;
          return (
            <button
              key={item.id}
              className={`nav-button ${isActive ? 'active' : ''}`}
              onClick={() => setCurrentView(item.id)}
            >
              <Icon />
              <span>{item.label}</span>
              {item.count !== undefined && item.count > 0 && (
                <span className="nav-count">{String(item.count).padStart(2, '0')}</span>
              )}
            </button>
          );
        })}
      </nav>

      <div className="sidebar-spacer" />

      <button 
        className="sidebar-action" 
        onClick={isAuthenticated ? logout : onOpenAuthModal}
      >
        {isAuthenticated ? <LogOut size={16} /> : <PlugZap size={16} />}
        <span>{isAuthenticated ? 'Disconnect' : 'Connect Workspace'}</span>
      </button>

      <div className="sidebar-footer">
        <button className="user-button" onClick={isAuthenticated ? logout : onOpenAuthModal}>
          <div className="user-avatar">{userInitials}</div>
          <div className="user-meta">
            <strong>{user?.fullName || 'Preview Workspace'}</strong>
            <span>{user?.roles?.[0]?.replace('_', ' ') || 'No API session'}</span>
          </div>
        </button>
      </div>
    </aside>
  );
}
