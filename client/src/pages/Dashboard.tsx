import { useNavigate } from "react-router-dom";
import { LogOut, LayoutDashboard } from "lucide-react"; // Corrected 'LayoutDashboard' spelling
import { useAuth } from "../App";

export default function Dashboard() {
  const navigate = useNavigate();
  const { user, checkAuth } = useAuth();
  const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8081";

  const handleLogout = async () => {
    try {
      // Call the backend to clear the HttpOnly Cookie
      await fetch(`${API_URL}/api/auth/logout`, {
        credentials: "include",
      });
      // Force App.tsx to verify session again (sends user to /login)
      await checkAuth();
    } catch (error) {
      console.error("Logout failed:", error);
    }
  };

  return (
    <main className="min-h-screen bg-zinc-950 text-white p-6">
      <header className="max-w-6xl mx-auto flex items-center justify-between border-b border-zinc-800 pb-4 mb-8">
        <div className="flex items-center gap-2">
          <LayoutDashboard className="w-6 h-6 text-white" />
          <h1 className="text-xl font-bold tracking-tight">AI Code Reviewer</h1>
        </div>
        {user && (
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2 bg-zinc-900 border border-zinc-800 px-3 py-1.5 rounded-lg">
              <img
                src={user.avatarUrl}
                alt="GitHub Avatar"
                className="w-6 h-6 rounded-full border border-white/10"
              />
              <span className="text-sm font-medium text-zinc-300">@{user.githubLogin}</span>
            </div>
            <button
              onClick={handleLogout}
              className="inline-flex items-center gap-2 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 hover:border-zinc-700 text-zinc-300 px-4 py-2 rounded-lg text-sm font-semibold transition cursor-pointer"
            >
              <LogOut className="w-4 h-4" />
              Log Out
            </button>
          </div>
        )}
      </header>
      <section className="max-w-4xl mx-auto text-center space-y-4 py-16">
        <h2 className="text-2xl font-bold">Welcome back, {user?.githubLogin}!</h2>
        <p className="text-zinc-400 max-w-md mx-auto text-sm">
          Your session cookie is secured on HttpOnly storage. Next, we will query GitHub to retrieve your repositories!
        </p>
      </section>
    </main>
  );
}
