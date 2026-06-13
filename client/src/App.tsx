import { createContext, useContext, useEffect, useState } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import Dashboard from "./pages/Dashboard";
import LoginPage from "./pages/LoginPage";

// 1. Define User Type
export interface User {
  id: number;
  githubLogin: string;
  avatarUrl: string;
}

// 2. Create Auth Context
interface AuthContextType {
  user: User | null;
  checkAuth: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within an AuthProvider");
  return context;
};

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const checkAuth = async () => {
    const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8081";
    try {
      const res = await fetch(`${API_URL}/api/auth/me`, {
        credentials: "include", // Tells browser to send the HttpOnly cookie!
      });
      if (res.ok) {
        const data = await res.json();
        setUser(data);
      } else {
        setUser(null);
      }
    } catch (error) {
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkAuth();
  }, []);

  // Show a clean loading state while we verify if the user is logged in
  if (loading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-zinc-950 text-white gap-4">
        <div className="w-8 h-8 border-4 border-white/20 border-t-white rounded-full animate-spin"></div>
        <p className="text-zinc-400 text-sm animate-pulse">Checking session status...</p>
      </div>
    );
  }

  return (
    <AuthContext.Provider value={{ user, checkAuth }}>
      <BrowserRouter>
        <Routes>
          {/* If logged in, redirect away from /login to /dashboard */}
          <Route path="/login" element={user ? <Navigate to="/dashboard" replace /> : <LoginPage />} />

          {/* If NOT logged in, block /dashboard and redirect to /login */}
          <Route path="/dashboard" element={user ? <Dashboard /> : <Navigate to="/login" replace />} />

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthContext.Provider>
  );
}

export default App;
