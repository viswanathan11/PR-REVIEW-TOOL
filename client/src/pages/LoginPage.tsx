import { Github } from "lucide-react";

export default function LoginPage() {
    const API_URL = import.meta.env.VITE_API_URL;

    const loginUrl = `${API_URL}/oauth2/authorization/github`;

    return (
        <main className="min-h-screen flex items-center justify-center bg-zinc-900/50 px-4 text-white">
            <div className="max-w-md w-full text-center space-y-8 p-8 bg-zinc-800/50 rounded-2x1 border border-zinc-800 backdrop-clur-md shadow-2xl">
                <div className="space-y-3">
                    <div className="inline-flex items-center justify-center w-16 h-16 rounded-2x1 bg-white/5 border border-white/10 text-white text-3x1 font-semibold shadow-inner">
                        🤖
                    </div>

                    <h1 className="text-3x1 font-extrabold tracking-tight text-white sm:text-4xl bg-gradient-to-r from-white via-zinc-300 to-zinc-500 bg-clip-text text-transparent">
                        AI Code Reviewer
                    </h1>
                    <p className="text-sinc-400 text0sm max-w-sm mx-auto">
                        Get automated, high-quality code review on every pull request
                    </p>
                </div>
                <div>
                    <a
                        href={loginUrl}
                        className="w-full inline-flex items-center justify-center gap-3 bg-white hover:bg-zinc-100 text-zinc-900 font-semibold px-6 py-3.5 rounded-xl transition-all duration-200 ease-in-out shadow-lg hover:shadow-xl active:scale-[0.98]"
                    >
                        <Github className="w-5 h-5 fill-current" />
                    </a>
                </div>
            </div>
        </main>
    )
}