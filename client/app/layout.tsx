import SessionProviderWrapper from "@/component/SessionProviderWrapper";
import type { Metadata } from "next";
import { getServerSession } from "next-auth";
import { Geist, Geist_Mono } from "next/font/google";
import React from "react";
import { authOptions } from "./api/auth/[...nextauth]/route";
import "./global.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"]
})

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
})

export const metadata: Metadata = {
  title: "Ai Code Reviewer",
  description: "Automated PR reviews powered by AI",
};

export default async function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const session = await getServerSession(authOptions);
  return (
    <html lang="en" className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col bg-gray-950 text-white">
        <SessionProviderWrapper session={session}>
          {children}
        </SessionProviderWrapper>
      </body>
    </html>
  );
}
