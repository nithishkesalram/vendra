import React, { createContext, useContext, useState, useEffect } from 'react';
import { request } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => sessionStorage.getItem('procureai.accessToken') || null);
  const [user, setUser] = useState(() => {
    const stored = sessionStorage.getItem('procureai.user');
    return stored ? JSON.parse(stored) : null;
  });

  const login = async (email, password) => {
    const session = await request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });

    const authToken = session.accessToken;
    const userData = {
      fullName: session.name || email.split('@')[0],
      email: session.email || email,
      roles: session.roles || ['USER']
    };

    setToken(authToken);
    setUser(userData);

    sessionStorage.setItem('procureai.accessToken', authToken);
    sessionStorage.setItem('procureai.user', JSON.stringify(userData));

    return userData;
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    sessionStorage.removeItem('procureai.accessToken');
    sessionStorage.removeItem('procureai.user');
  };

  return (
    <AuthContext.Provider value={{ token, user, isAuthenticated: Boolean(token), login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
