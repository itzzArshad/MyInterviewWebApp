import type { Metadata } from "next";
import { serverApi } from "@/lib/server-api";
import type { GitHubStatus } from "@/lib/types";
import { GitHubSettings } from "./GitHubSettings";

export const metadata: Metadata = { title: "Settings" };

export default async function SettingsPage() {
  const status = await serverApi<GitHubStatus>("/github/status");
  return (
    <div className="mx-auto max-w-3xl px-5 py-8 md:px-12 md:py-12">
      <h1 className="text-2xl font-bold tracking-tight md:text-3xl">Settings</h1>
      <GitHubSettings status={status} />
    </div>
  );
}
