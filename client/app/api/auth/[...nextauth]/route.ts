import NextAuth, { NextAuthOptions } from "next-auth";

import GithubProvider from "next-auth/providers/github";

export const authOptions: NextAuthOptions = {
    providers: [
        GithubProvider({
            clientId: process.env.GITHUB_CLIENT_ID!,
            clientSecret: process.env.GITHUB_CLIENT_SECRET!,


            //We request the specific permissions from the users's github account:
            // -read:user -> read profile details(avatar,login)
            //repo -> acess public & private repos to read code diffs

            //--admin:repo_hook -> allow our app to register hooks for pr changes

            authorization: {
                params: { scope: "read:user repo admin:repo_hook" },
            },
        }),
    ],

    callbacks: {
        //When the Oauth login succeeds, we capture the Github acess token

        async jwt({ token, account }) {
            if (account?.access_token) {
                token.githubAcessToken = account.access_token;
            }
            return token;
        },

        //Exposes the Github acess token so out react components can access it in the session

        async session({ session, token }) {
            (session as any).githubAcessToken = token.githubAcessToken;
            return session;
        },
    },

    pages: {
        signIn: "/login",//If not authenticated, send them here
    }
}

const handler = NextAuth(authOptions);
export { handler as GET, handler as POST };
