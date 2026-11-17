"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2, Play } from "lucide-react";
import { api } from "@/lib/api";
import type { MockView } from "@/lib/types";

export function StartMockButton({ disabled }: { disabled?: boolean }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function start() {
    setBusy(true);
    setError(null);
    try {
      const m = await api<MockView>("/interview/mocks", { method: "POST" });
      router.push(`/interview/mock/${m.id}`);
    } catch (e) {
      setError((e as Error).message);
      setBusy(false);
    }
  }

  return (
    <>
      <button
        onClick={start}
        disabled={disabled || busy}
        className="mt-4 inline-flex items-center gap-1.5 rounded-md bg-accent px-4 py-1.5 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50"
      >
        {busy ? <Loader2 className="size-4 animate-spin" /> : <Play className="size-4" />} Start mock interview
      </button>
      {error && <p className="mt-2 text-sm text-danger">{error}</p>}
    </>
  );
}
