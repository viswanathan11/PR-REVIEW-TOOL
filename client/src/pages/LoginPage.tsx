
import LiquidEther from "../components/LiquidEther";

export default function LoginPage() {
    const API_URL = import.meta.env.VITE_API_URL ?? "";

    // Point the login directly to Spring Boot's OAuth entry point
    const loginUrl = `${API_URL}/oauth2/authorization/github`;

    return (
        <main className="relative min-h-screen flex items-center justify-center bg-zinc-950 px-4 text-white overflow-hidden">
            {/* Background animation */}
            <div className="absolute inset-0 z-0">
                {/* @ts-ignore */}
                <LiquidEther
                    mouseForce={20}
                    cursorSize={100}
                    isViscous={false}
                    viscous={30}
                    colors={["#5227FF", "#FF9FFC", "#B497CF"]}
                    autoDemo
                    autoSpeed={0.5}
                    autoIntensity={2.2}
                    isBounce={false}
                    resolution={0.5}
                />
            </div>

            {/* Premium Glassmorphic Login Card */}
            <div className="relative z-10 max-w-md w-full text-center space-y-10 p-10 bg-zinc-950/40 rounded-3xl border border-zinc-800/80 backdrop-blur-xl shadow-[0_0_50px_rgba(82,39,255,0.15)]">
                <div className="space-y-6">
                    {/* Big, Stylish Floating Character */}
                    <div className="inline-flex items-center justify-center w-24 h-24 rounded-3xl bg-zinc-900/80 border border-indigo-500/20 text-white text-5xl font-semibold shadow-inner shadow-indigo-500/10 animate-float">
                        🤖
                    </div>
                    
                    <div className="space-y-3">
                        <h1 className="text-4xl sm:text-5xl font-black uppercase tracking-tighter bg-gradient-to-r from-[#FF9FFC] via-[#7d56ff] to-[#B497CF] bg-clip-text text-transparent">
                            AI PR Reviewer
                        </h1>
                        <p className="text-zinc-400 text-sm max-w-sm mx-auto leading-relaxed">
                            Get automated, high-quality code reviews on every pull request, powered by Google Gemini.
                        </p>
                    </div>
                </div>

                <div>
                    {/* Extremely Stylish Gradient/Glow Sign-in Button */}
                    <a
                        href={loginUrl}
                        className="w-full inline-flex items-center justify-center gap-3 bg-gradient-to-r from-[#5227FF] via-[#7d56ff] to-[#FF9FFC] hover:from-[#4016e3] hover:to-[#e27ee0] text-white font-bold px-6 py-4 rounded-xl transition-all duration-300 ease-in-out shadow-[0_0_20px_rgba(82,39,255,0.3)] hover:shadow-[0_0_35px_rgba(255,159,252,0.5)] active:scale-[0.98] border border-white/10"
                    >
                        <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
                            <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z" />
                        </svg>
                        Sign in with GitHub
                    </a>
                </div>
            </div>
        </main>
    );
}
