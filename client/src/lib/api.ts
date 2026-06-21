const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8081";

export interface Repository {
    id: number;
    githubRepoId: string;
    fullName: string;
    description: string | null;
    isPrivate: boolean;
    webhookId: string | null;
    webhookActive: boolean;
}

export interface GithubRepo {
    id: number;
    name: string;
    full_name: string;
    description: string | null;
    private: boolean;
}

// 1. Fetch tracked repositories from database
export async function getTrackedRepositories(): Promise<Repository[]> {
    const res = await fetch(`${API_URL}/api/repos`, {
        credentials: "include",
    });
    if (!res.ok) throw new Error("Failed to fetch tracked repositories");
    return res.json();
}

// 2. Fetch all repositories from Github
export async function getGitHubRepositories(): Promise<GithubRepo[]> {
    const res = await fetch(`${API_URL}/api/repos/github`, {
        credentials: "include",
    });
    if (!res.ok) throw new Error("Failed to fetch Github Repositories");
    return res.json();
}

// 3. Start tracking a repository
export async function trackRepository(repo: { githubRepoId: string; fullName: string; description: string | null; private: boolean }): Promise<Repository> {
    const res = await fetch(`${API_URL}/api/repos`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(repo),
        credentials: "include",
    });
    if (!res.ok) throw new Error("Failed to track repository");
    return res.json();
}

// 4. Enable webhook (start automated code reviews)
export async function enableWebhook(repoId: number): Promise<Repository> {
    const res = await fetch(`${API_URL}/api/repos/${repoId}/webhook`, {
        method: "POST",
        credentials: "include",
    });
    if (!res.ok) throw new Error("Failed to enable webhook");
    return res.json();
}

// 5. Disable webhook (stops automated code reviews)
export async function disableWebhook(repoId: number): Promise<Repository> {
    const res = await fetch(`${API_URL}/api/repos/${repoId}/webhook`, {
        method: "DELETE",
        credentials: "include",
    });
    if (!res.ok) throw new Error("Failed to disable webhook");
    return res.json();
}
