const API_URL=import.meta.env.VITE_API_URL;

export interface Repository{
    id:number;
    githubRepId:string;
    fullname:string|null;
    isPrivate:boolean;
    webhookId:string|null;
    webhookActive: boolean;
}

export interface GithubRepo{
    id: number;
    name: string;
    full_name:string;
    description:string|null;
    private:boolean;
}

//1. Fetch tracked repositores from database
export async function getTrackedRepositories():Promise<Repository[]> {
    const res=await fetch(`${API_URL}/api/repos`,{
        credentials:"include",
    });

    if(!res.ok) throw new Error("Failed to fetch tracked repositories");
    return res.json();
}

//2. Fetch all repositores from Github
export async function getGitHubRepositories():Promise<GithubRepo[]>{
    const res=await fetch(`${API_URL}/api/repos/github`,{
        credentials:"include",
    });

    if(!res.ok) throw new Error("Failed to fetch Github Repositories");
    return res.json();
}

//3. start tracking a repsoitory
export async function trackRepository(repo:{githubRepoId: string,fullName:string})

