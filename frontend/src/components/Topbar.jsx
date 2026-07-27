import React from 'react';
import { Bell, BookOpen, RefreshCw } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export function Topbar({ currentView, onRefresh, onOpenNotifications }) {
  const { isAuthenticated } = useAuth();

  return (
    <header className="topbar">
      <div className="crumb">
        Workspace / <strong>{currentView}</strong>
      </div>

      <div className="top-actions">
        <div className="state-indicator">
          <div className={`state-dot ${!isAuthenticated ? 'preview' : ''}`} />
          <span>{isAuthenticated ? 'Live Workspace' : 'Preview Mode'}</span>
        </div>

        <button 
          className="icon-button" 
          title="Refresh Data"
          onClick={onRefresh}
        >
          <RefreshCw />
        </button>

        <button 
          className="icon-button" 
          title="Notifications"
          onClick={onOpenNotifications}
        >
          <Bell />
        </button>

        <button 
          className="icon-button" 
          title="Swagger API Documentation"
          onClick={() => window.open('/swagger-ui.html', '_blank')}
        >
          <BookOpen />
        </button>
      </div>
    </header>
  );
}
