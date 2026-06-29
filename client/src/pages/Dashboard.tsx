import { 
  Activity, 
  AlertCircle, 
  LayoutDashboard, 
  Loader2, 
  LogOut, 
  Power, 
  RefreshCw, 
  Search,
  ArrowLeft,
  Calendar,
  MessageSquare,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  ChevronRight
} from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../App";
import {
  disableWebhook,
  enableWebhook,
  getGitHubRepositories,
  getTrackedRepositories,
  getRepoPullRequests,
  getReview,
  getReviewComments,
  trackRepository,
  type GithubRepo,
  type Repository,
  type PullRequest,
  type Review,
  type ReviewComment
} from "../lib/api";

export default function Dashboard() {
  const navigate = useNavigate();
  const { user, checkAuth } = useAuth();
  const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8081";

  // Data fetching states
  const [trackedRepos, setTrackedRepos] = useState<Repository[]>([]);
  const [githubRepos, setGithubRepos] = useState<GithubRepo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [actionLoading, setActionLoading] = useState<Record<string, boolean>>({});

  // Navigation states for our SPA Sub-Views
  const [selectedRepo, setSelectedRepo] = useState<Repository | null>(null);
  const [pullRequests, setPullRequests] = useState<PullRequest[]>([]);
  const [prLoading, setPrLoading] = useState(false);

  const [selectedPr, setSelectedPr] = useState<PullRequest | null>(null);
  const [review, setReview] = useState<Review | null>(null);
  const [reviewComments, setReviewComments] = useState<ReviewComment[]>([]);
  const [reviewLoading, setReviewLoading] = useState(false);

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

  // Navigating to Repository PRs view
  const handleSelectRepo = async (repo: Repository) => {
    setSelectedRepo(repo);
    setPrLoading(true);
    setPullRequests([]);
    setSelectedPr(null);
    try {
      const prs = await getRepoPullRequests(repo.id);
      setPullRequests(prs);
    } catch (err: any) {
      console.error("Failed to fetch pull requests:", err);
      alert("Failed to fetch Pull Request list.");
    } finally {
      setPrLoading(false);
    }
  };

  // Navigating to PR Review details view
  const handleSelectPr = async (pr: PullRequest) => {
    setSelectedPr(pr);
    setReviewLoading(true);
    setReview(null);
    setReviewComments([]);
    try {
      const [revData, commentsData] = await Promise.all([
        getReview(pr.id),
        getReviewComments(pr.id)
      ]);
      setReview(revData);
      setReviewComments(commentsData);
    } catch (err: any) {
      console.error("Failed to fetch review details:", err);
    } finally {
      setReviewLoading(false);
    }
  };

  const getSeverityStyles = (severity: string) => {
    switch (severity.toUpperCase()) {
      case "BUG":
        return { badge: "bg-red-500/10 text-red-400 border-red-500/20", icon: <XCircle className="w-4 h-4 text-red-400" /> };
      case "SECURITY":
        return { badge: "bg-orange-500/10 text-orange-400 border-orange-500/20", icon: <AlertTriangle className="w-4 h-4 text-orange-400" /> };
      case "PERFORMANCE":
        return { badge: "bg-yellow-500/10 text-yellow-400 border-yellow-500/20", icon: <Activity className="w-4 h-4 text-yellow-400" /> };
      case "STYLE":
        return { badge: "bg-blue-500/10 text-blue-400 border-blue-500/20", icon: <MessageSquare className="w-4 h-4 text-blue-400" /> };
      default:
        return { badge: "bg-zinc-800 text-zinc-400 border-zinc-700", icon: <AlertCircle className="w-4 h-4 text-zinc-400" /> };
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "DONE":
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded-full text-xs font-semibold">
            <CheckCircle2 className="w-3.5 h-3.5" />
            Done
          </span>
        );
      case "PROCESSING":
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-blue-500/10 text-blue-400 border border-blue-500/20 rounded-full text-xs font-semibold animate-pulse">
            <Loader2 className="w-3.5 h-3.5 animate-spin" />
            Analyzing...
          </span>
        );
      case "FAILED":
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-red-500/10 text-red-400 border border-red-500/20 rounded-full text-xs font-semibold">
            <XCircle className="w-3.5 h-3.5" />
            Failed
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-zinc-800 text-zinc-400 border border-zinc-700 rounded-full text-xs font-semibold">
            Pending
          </span>
        );
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
          <div className="flex items-center gap-2 cursor-pointer" onClick={() => { setSelectedRepo(null); setSelectedPr(null); }}>
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

      <div className="max-w-6xl mx-auto px-6 py-8">
        
        {/* VIEW 3: AI REVIEW DETAIL VIEW */}
        {selectedPr ? (
          <div className="space-y-6">
            <button
              onClick={() => setSelectedPr(null)}
              className="inline-flex items-center gap-2 text-zinc-400 hover:text-white text-sm transition cursor-pointer"
            >
              <ArrowLeft className="w-4 h-4" />
              Back to Pull Requests
            </button>

            <div className="bg-zinc-900/30 border border-zinc-900 p-6 rounded-xl space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-zinc-900/60 pb-6">
                <div className="space-y-1">
                  <h2 className="text-xl font-bold">{selectedPr.title}</h2>
                  <p className="text-sm text-zinc-500">
                    PR #{selectedPr.prNumber} by @{selectedPr.author} | Commit: <code className="text-xs text-zinc-400 font-mono bg-zinc-900 px-1 py-0.5 rounded">{selectedPr.headSha.substring(0, 7)}</code>
                  </p>
                </div>
                <div>
                  {review ? getStatusBadge(review.status) : getStatusBadge("PENDING")}
                </div>
              </div>

              {reviewLoading ? (
                <div className="flex flex-col items-center justify-center py-16 gap-3">
                  <Loader2 className="w-8 h-8 text-zinc-500 animate-spin" />
                  <p className="text-zinc-400 text-sm">Loading AI Code Review...</p>
                </div>
              ) : !review ? (
                <div className="text-center py-12 border border-dashed border-zinc-900 rounded-lg">
                  <p className="text-zinc-500 text-sm">No review details recorded for this commit.</p>
                </div>
              ) : review.status === "FAILED" ? (
                <div className="p-4 bg-red-950/20 border border-red-900/30 text-red-400 rounded-lg space-y-1">
                  <p className="font-semibold text-sm">Analysis Pipeline Error</p>
                  <p className="text-xs text-red-500">{review.errorMessage || "An unknown exception occurred."}</p>
                </div>
              ) : review.status === "PROCESSING" ? (
                <div className="flex flex-col items-center justify-center py-12 gap-3 text-center">
                  <Loader2 className="w-8 h-8 text-blue-500 animate-spin" />
                  <p className="text-zinc-300 font-semibold text-sm">AI analysis is in progress...</p>
                  <p className="text-zinc-500 text-xs max-w-sm">
                    Claude is reviewing the git diff. This takes about 5-15 seconds. Please refresh in a moment.
                  </p>
                </div>
              ) : (
                <div className="space-y-6">
                  {/* Summary & Score Cards */}
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <div className="md:col-span-3 bg-zinc-950/60 border border-zinc-900 p-5 rounded-lg space-y-2">
                      <h4 className="text-xs font-bold uppercase tracking-wider text-zinc-500">Summary Review</h4>
                      <p className="text-sm text-zinc-300 leading-relaxed italic">"{review.reviewSummary}"</p>
                    </div>
                    <div className="bg-zinc-950/60 border border-zinc-900 p-5 rounded-lg flex flex-col items-center justify-center text-center">
                      <h4 className="text-xs font-bold uppercase tracking-wider text-zinc-500 mb-2">Overall Score</h4>
                      <div className="w-16 h-16 rounded-full border-4 border-emerald-500/20 border-t-emerald-500 flex items-center justify-center text-xl font-black text-emerald-400">
                        {review.overallScore || 0}/10
                      </div>
                    </div>
                  </div>

                  {/* Inline Comments */}
                  <div className="space-y-4">
                    <h3 className="text-base font-bold tracking-tight border-b border-zinc-900 pb-2">Line Annotations ({reviewComments.length})</h3>
                    {reviewComments.length === 0 ? (
                      <div className="text-center py-8 border border-zinc-900 bg-zinc-950/20 rounded-lg">
                        <p className="text-zinc-500 text-sm">No issues or suggestions found by the AI reviewer. Code is clean!</p>
                      </div>
                    ) : (
                      <div className="space-y-3">
                        {reviewComments.map((c) => {
                          const severityStyle = getSeverityStyles(c.severity);
                          return (
                            <div key={c.id} className="bg-zinc-950/40 border border-zinc-900 rounded-lg p-4 space-y-3">
                              <div className="flex items-center justify-between gap-3">
                                <div className="flex items-center gap-2">
                                  {severityStyle.icon}
                                  <span className="text-xs font-mono text-zinc-400 truncate max-w-xs sm:max-w-md">
                                    {c.filePath} : Line {c.lineNumber}
                                  </span>
                                </div>
                                <span className={`text-[9px] font-black uppercase tracking-wider px-2 py-0.5 border rounded-full ${severityStyle.badge}`}>
                                  {c.severity}
                                </span>
                              </div>
                              <p className="text-zinc-300 text-sm">{c.comment}</p>
                              
                              {c.suggestion && (
                                <div className="space-y-1">
                                  <span className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider">Suggested Fix:</span>
                                  <pre className="bg-zinc-950 border border-zinc-900 p-3 rounded text-xs font-mono text-emerald-400 overflow-x-auto">
                                    {c.suggestion}
                                  </pre>
                                </div>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        ) : selectedRepo ? (
          
          /* VIEW 2: PULL REQUESTS HISTORY TABLE */
          <div className="space-y-6">
            <button
              onClick={() => setSelectedRepo(null)}
              className="inline-flex items-center gap-2 text-zinc-400 hover:text-white text-sm transition cursor-pointer"
            >
              <ArrowLeft className="w-4 h-4" />
              Back to Repositories
            </button>

            <div className="bg-zinc-900/30 border border-zinc-900 p-6 rounded-xl space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-zinc-900/60 pb-6">
                <div className="space-y-1">
                  <h2 className="text-xl font-bold">{selectedRepo.fullName}</h2>
                  <p className="text-zinc-500 text-xs">{selectedRepo.description || "No description provided."}</p>
                </div>
                <button
                  onClick={() => handleSelectRepo(selectedRepo)}
                  className="self-start inline-flex items-center gap-2 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 text-zinc-300 px-3.5 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${prLoading ? "animate-spin" : ""}`} />
                  Refresh PRs
                </button>
              </div>

              {prLoading ? (
                <div className="flex flex-col items-center justify-center py-20 gap-3">
                  <Loader2 className="w-8 h-8 text-zinc-500 animate-spin" />
                  <p className="text-zinc-400 text-sm">Loading Pull Requests...</p>
                </div>
              ) : pullRequests.length === 0 ? (
                <div className="text-center py-16 border border-dashed border-zinc-900 rounded-xl">
                  <p className="text-zinc-500 text-sm mb-1">No reviewed Pull Requests found.</p>
                  <p className="text-zinc-600 text-xs max-w-sm mx-auto">
                    Open a Pull Request on GitHub or synchronize commits to trigger an automated code review.
                  </p>
                </div>
              ) : (
                <div className="overflow-x-auto border border-zinc-900 rounded-xl bg-zinc-950/20">
                  <table className="w-full text-left border-collapse">
                    <thead>
                      <tr className="border-b border-zinc-900 bg-zinc-900/20 text-zinc-400 text-xs font-bold">
                        <th className="p-4">Pull Request</th>
                        <th className="p-4">Branches</th>
                        <th className="p-4">Latest Commit SHA</th>
                        <th className="p-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-900 text-sm">
                      {pullRequests.map((pr) => (
                        <tr key={pr.id} className="hover:bg-zinc-900/20 transition-colors">
                          <td className="p-4 space-y-1">
                            <p className="font-semibold text-zinc-100 hover:text-white cursor-pointer" onClick={() => handleSelectPr(pr)}>
                              #{pr.prNumber} {pr.title}
                            </p>
                            <p className="text-xs text-zinc-500">by @{pr.author}</p>
                          </td>
                          <td className="p-4 text-xs font-mono text-zinc-400 space-y-0.5">
                            <p><span className="text-zinc-600">from</span> {pr.headBranch}</p>
                            <p><span className="text-zinc-600">into</span> {pr.baseBranch}</p>
                          </td>
                          <td className="p-4">
                            <code className="text-xs text-zinc-400 font-mono bg-zinc-900/60 px-2 py-1 rounded border border-zinc-800">
                              {pr.headSha.substring(0, 7)}
                            </code>
                          </td>
                          <td className="p-4 text-right">
                            <button
                              onClick={() => handleSelectPr(pr)}
                              className="inline-flex items-center gap-1.5 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 hover:border-zinc-700 text-zinc-300 hover:text-white px-3 py-1.5 rounded-lg text-xs font-semibold transition cursor-pointer"
                            >
                              View Review
                              <ChevronRight className="w-3.5 h-3.5" />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        ) : (
          
          /* VIEW 1: REPOSITORY GRID VIEW (DEFAULT) */
          <div className="space-y-8">
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
                            <h3 
                              className={`font-semibold truncate text-base ${isTracked ? "text-zinc-100 hover:text-white hover:underline cursor-pointer" : "text-zinc-400"}`}
                              onClick={() => isTracked && handleSelectRepo(tracked)}
                            >
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

                        <div className="flex items-center justify-between border-t border-zinc-900/60 pt-3">
                          <div>
                            {isTracked && (
                              <button
                                onClick={() => handleSelectRepo(tracked)}
                                className="text-zinc-400 hover:text-white hover:underline text-xs font-semibold cursor-pointer"
                              >
                                View PR History
                              </button>
                            )}
                          </div>
                          <div className="flex items-center gap-2">
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
                      </div>
                    );
                  })}
                </div>
              )}
            </section>
          </div>
        )}
      </div>
    </main>
  );
}
