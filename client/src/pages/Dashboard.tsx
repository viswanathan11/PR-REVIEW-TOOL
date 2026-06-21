import { Activity, AlertCircle, LayoutDashboard, Loader2, LogOut, Power, RefreshCw, Search } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../App";
import {
  disableWebhook,
  enableWebhook,
  getGitHubRepositories,
  getTrackedRepositories,
  trackRepository,
  type GithubRepo,
  type Repository
} from "../lib/api";

export default function Dashboard() {
  const navigate = useNavigate();
  const { user, checkAuth } = useAuth();
  const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8081";

  const [trackedRepos, setTrackedRepos] = useState<Repository[]>([]);
  const [githubRepos, setGithubRepos] = useState<GithubRepo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [actionLoading, setActionLoading] = useState<Record<string, boolean>>({});

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [tracked, github] = await Promise.all([
        getTrackedRepositories(),
        getGitHubRepositories(),
      ]);
      setTrackedRepos(tracked);
      setGithubRepos(github);
    } catch (err: any) {
      console.error("Error fetching dashboard data:", err);
      // Change this line to display the real error message:
      setError(`Failed to load data: ${err.message || err}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleLogout = async () => {
    try {
      await fetch(`${API_URL}/api/auth/logout`, {
        credentials: "include",
      });
      await checkAuth();
    } catch (error) {
      console.error("Logout failed:", error);
    }
  };

  const handleTrackRepo = async (ghRepo: GithubRepo) => {
    const key = `track-${ghRepo.id}`;
    setActionLoading(prev => ({ ...prev, [key]: true }));
    try {
      const newTracked = await trackRepository({
        githubRepoId: ghRepo.id.toString(),
        fullName: ghRepo.full_name,
        description: ghRepo.description,
        private: ghRepo.private,
      });
      setTrackedRepos(prev => [...prev, newTracked]);
    } catch (err) {
      console.error("Failed to track repo:", err);
      alert("Failed to track repository. Please try again.");
    } finally {
      setActionLoading(prev => ({ ...prev, [key]: false }));
    }
  };

  const handleEnableWebhook = async (repoId: number) => {
    const key = `webhook-${repoId}`;
    setActionLoading(prev => ({ ...prev, [key]: true }));
    try {
      const updated = await enableWebhook(repoId);
      setTrackedRepos(prev => prev.map(r => r.id === repoId ? updated : r));
    } catch (err) {
      console.error("Failed to enable webhook:", err);
      alert("Failed to enable webhook on GitHub. Ensure your OAuth Application has 'admin:repo_hook' permissions.");
    } finally {
      setActionLoading(prev => ({ ...prev, [key]: false }));
    }
  };

  const handleDisableWebhook = async (repoId: number) => {
    const key = `webhook-${repoId}`;
    setActionLoading(prev => ({ ...prev, [key]: true }));
    try {
      const updated = await disableWebhook(repoId);
      setTrackedRepos(prev => prev.map(r => r.id === repoId ? updated : r));
    } catch (err) {
      console.error("Failed to disable webhook:", err);
      alert("Failed to disable webhook.");
    } finally {
      setActionLoading(prev => ({ ...prev, [key]: false }));
    }
  };

  // Filter repositories based on query
  const filteredRepos = githubRepos.filter(repo =>
    repo.full_name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const activeWebhookCount = trackedRepos.filter(r => r.webhookActive).length;

  return (
    <main className="min-h-screen bg-zinc-950 text-white font-sans selection:bg-white/10 selection:text-white">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-zinc-950/80 backdrop-blur-md border-b border-zinc-900 px-6 py-4">
        <div className="max-w-6xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2">
            <LayoutDashboard className="w-5 h-5 text-white" />
            <span className="text-lg font-bold tracking-tight bg-gradient-to-r from-white via-zinc-200 to-zinc-500 bg-clip-text text-transparent">
              AI Code Reviewer
            </span>
          </div>

          {user && (
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2 bg-zinc-900 border border-zinc-800 px-3 py-1.5 rounded-lg">
                <img
                  src={user.avatarUrl}
                  alt="GitHub Avatar"
                  className="w-5 h-5 rounded-full border border-white/10"
                />
                <span className="text-sm font-medium text-zinc-300">@{user.githubLogin}</span>
              </div>
              <button
                onClick={handleLogout}
                className="inline-flex items-center gap-2 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 hover:border-zinc-700 text-zinc-300 px-3.5 py-1.5 rounded-lg text-sm font-semibold transition cursor-pointer"
              >
                <LogOut className="w-3.5 h-3.5" />
                Log Out
              </button>
            </div>
          )}
        </div>
      </header>

      <div className="max-w-6xl mx-auto px-6 py-8 space-y-8">
        {/* Quick Stats Grid */}
        <section className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-zinc-900/40 border border-zinc-900 p-5 rounded-xl space-y-1">
            <p className="text-zinc-500 text-xs font-semibold uppercase tracking-wider">Active Webhooks</p>
            <div className="flex items-baseline gap-2">
              <p className="text-3xl font-extrabold">{activeWebhookCount}</p>
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
            </div>
          </div>
          <div className="bg-zinc-900/40 border border-zinc-900 p-5 rounded-xl space-y-1">
            <p className="text-zinc-500 text-xs font-semibold uppercase tracking-wider">Tracked Repositories</p>
            <p className="text-3xl font-extrabold">{trackedRepos.length}</p>
          </div>
          <div className="bg-zinc-900/40 border border-zinc-900 p-5 rounded-xl space-y-1">
            <p className="text-zinc-500 text-xs font-semibold uppercase tracking-wider">Total GitHub Repos</p>
            <p className="text-3xl font-extrabold">{githubRepos.length}</p>
          </div>
        </section>

        {/* Toolbar */}
        <section className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
            <input
              type="text"
              placeholder="Search GitHub repositories..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-zinc-900 border border-zinc-800 rounded-lg pl-10 pr-4 py-2 text-sm text-white placeholder-zinc-500 focus:outline-none focus:border-zinc-700 transition"
            />
          </div>
          <button
            onClick={fetchData}
            disabled={loading}
            className="inline-flex items-center justify-center gap-2 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 text-zinc-300 px-4 py-2 rounded-lg text-sm font-semibold transition cursor-pointer disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </button>
        </section>

        {/* Repository Grid */}
        <section>
          {loading ? (
            <div className="flex flex-col items-center justify-center py-20 gap-3">
              <Loader2 className="w-8 h-8 text-zinc-500 animate-spin" />
              <p className="text-zinc-400 text-sm">Loading repositories...</p>
            </div>
          ) : error ? (
            <div className="flex flex-col items-center justify-center text-center p-8 bg-red-950/20 border border-red-900/30 rounded-xl gap-3">
              <AlertCircle className="w-8 h-8 text-red-500" />
              <p className="text-red-400 text-sm font-semibold">{error}</p>
              <button
                onClick={fetchData}
                className="bg-red-900/40 hover:bg-red-900/60 border border-red-800 px-4 py-2 rounded-lg text-xs font-bold text-red-200 transition cursor-pointer"
              >
                Try Again
              </button>
            </div>
          ) : filteredRepos.length === 0 ? (
            <div className="text-center py-16 border border-dashed border-zinc-900 rounded-xl">
              <p className="text-zinc-500 text-sm">No repositories found.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {filteredRepos.map((ghRepo) => {
                const tracked = trackedRepos.find(r => r.githubRepoId === ghRepo.id.toString());
                const isTracked = !!tracked;
                const isWebhookActive = tracked?.webhookActive;
                const key = isTracked ? `webhook-${tracked.id}` : `track-${ghRepo.id}`;
                const isButtonLoading = !!actionLoading[key];

                return (
                  <div
                    key={ghRepo.id}
                    className="flex flex-col justify-between p-5 bg-zinc-900/20 hover:bg-zinc-900/40 border border-zinc-900 hover:border-zinc-800 rounded-xl transition duration-200 gap-4"
                  >
                    <div className="space-y-2">
                      <div className="flex items-center justify-between gap-2">
                        <h3 className="font-semibold text-zinc-100 truncate text-base">
                          {ghRepo.full_name}
                        </h3>
                        <div className="flex items-center gap-1.5 shrink-0">
                          {ghRepo.private ? (
                            <span className="text-[10px] font-bold tracking-wide uppercase px-2 py-0.5 bg-zinc-800/80 text-zinc-400 rounded-full border border-zinc-700/50">
                              Private
                            </span>
                          ) : (
                            <span className="text-[10px] font-bold tracking-wide uppercase px-2 py-0.5 bg-white/5 text-zinc-400 rounded-full border border-white/5">
                              Public
                            </span>
                          )}

                          {/* Tracked / Webhook Badge Status */}
                          {isTracked ? (
                            isWebhookActive ? (
                              <span className="inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-wide px-2.5 py-0.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded-full">
                                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                                Active
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-wide px-2.5 py-0.5 bg-zinc-800 text-zinc-400 border border-zinc-700 rounded-full">
                                <span className="w-1.5 h-1.5 rounded-full bg-zinc-500"></span>
                                Inactive
                              </span>
                            )
                          ) : (
                            <span className="text-[10px] font-bold tracking-wide uppercase px-2.5 py-0.5 bg-zinc-950 text-zinc-500 border border-zinc-900 rounded-full">
                              Untracked
                            </span>
                          )}
                        </div>
                      </div>

                      <p className="text-zinc-400 text-xs line-clamp-2 h-8">
                        {ghRepo.description || "No description provided."}
                      </p>
                    </div>

                    <div className="flex items-center justify-end border-t border-zinc-900/60 pt-3">
                      {isTracked ? (
                        isWebhookActive ? (
                          <button
                            onClick={() => handleDisableWebhook(tracked.id)}
                            disabled={isButtonLoading}
                            className="inline-flex items-center gap-2 bg-red-950/20 hover:bg-red-950/40 border border-red-900/30 text-red-400 hover:text-red-300 px-4 py-2 rounded-lg text-xs font-bold transition cursor-pointer disabled:opacity-50"
                          >
                            {isButtonLoading ? (
                              <Loader2 className="w-3.5 h-3.5 animate-spin" />
                            ) : (
                              <Power className="w-3.5 h-3.5" />
                            )}
                            Disable Review
                          </button>
                        ) : (
                          <button
                            onClick={() => handleEnableWebhook(tracked.id)}
                            disabled={isButtonLoading}
                            className="inline-flex items-center gap-2 bg-emerald-950/20 hover:bg-emerald-950/40 border border-emerald-900/30 text-emerald-400 hover:text-emerald-300 px-4 py-2 rounded-lg text-xs font-bold transition cursor-pointer disabled:opacity-50"
                          >
                            {isButtonLoading ? (
                              <Loader2 className="w-3.5 h-3.5 animate-spin" />
                            ) : (
                              <Activity className="w-3.5 h-3.5" />
                            )}
                            Enable Review
                          </button>
                        )
                      ) : (
                        <button
                          onClick={() => handleTrackRepo(ghRepo)}
                          disabled={isButtonLoading}
                          className="inline-flex items-center gap-2 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 text-zinc-300 hover:text-white px-4 py-2 rounded-lg text-xs font-bold transition cursor-pointer disabled:opacity-50"
                        >
                          {isButtonLoading ? (
                            <Loader2 className="w-3.5 h-3.5 animate-spin" />
                          ) : (
                            <RefreshCw className="w-3.5 h-3.5" />
                          )}
                          Track Repo
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
