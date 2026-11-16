"use client";

import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import clsx from "clsx";
import { AlertTriangle, CheckCircle2, Loader2, Save, Trash2, XCircle } from "lucide-react";
import { api, ApiError } from "@/lib/api";
import type { LessonPreview, LessonSource, SaveResult } from "@/lib/types";
import { InlineMarkdown } from "@/components/markdown/InlineMarkdown";
import { CopyPromptButton } from "./CopyPromptButton";

const draftKey = (slug: string) => `academy.draft.${slug}`;

function loadDraft(slug: string): string | null {
  try {
    return localStorage.getItem(draftKey(slug));
  } catch {
    return null;
  }
}
function saveDraft(slug: string, text: string | null) {
  try {
    if (text === null) localStorage.removeItem(draftKey(slug));
    else localStorage.setItem(draftKey(slug), text);
  } catch {
    /* storage unavailable */
  }
}

export function LessonEditor({ source }: { source: LessonSource }) {
  const router = useRouter();
  const [text, setText] = useState(source.markdown);
  const [restoredDraft, setRestoredDraft] = useState(false);
  const [tab, setTab] = useState<"write" | "preview">("write");
  const [preview, setPreview] = useState<LessonPreview | null>(null);
  const [checking, setChecking] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [saveErrors, setSaveErrors] = useState<string[] | null>(null);
  const dirty = text !== source.markdown;
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Restore an unsaved draft (e.g. the phone reloaded the tab mid-paste).
  useEffect(() => {
    const d = loadDraft(source.slug);
    if (d && d !== source.markdown) {
      setText(d);
      setRestoredDraft(true);
    }
  }, [source.slug, source.markdown]);

  // Keep the draft and live validation up to date (debounced).
  useEffect(() => {
    saveDraft(source.slug, dirty ? text : null);
    if (timer.current) clearTimeout(timer.current);
    if (!text.trim()) {
      setPreview(null);
      return;
    }
    timer.current = setTimeout(async () => {
      setChecking(true);
      try {
        setPreview(await api<LessonPreview>(`/lessons/${source.slug}/preview`, { method: "POST", body: JSON.stringify({ markdown: text }) }));
      } catch {
        /* keep last preview */
      } finally {
        setChecking(false);
      }
    }, 500);
    return () => {
      if (timer.current) clearTimeout(timer.current);
    };
  }, [text, dirty, source.slug]);

  // Warn before leaving with unsaved changes.
  useEffect(() => {
    const onBeforeUnload = (e: BeforeUnloadEvent) => {
      if (dirty) e.preventDefault();
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, [dirty]);

  async function save() {
    setSaving(true);
    setSaveErrors(null);
    try {
      await api<SaveResult>(`/lessons/${source.slug}`, { method: "PUT", body: JSON.stringify({ markdown: text }) });
      saveDraft(source.slug, null);
      router.push(`/learn/${source.slug}`);
      router.refresh();
    } catch (e) {
      const errs = e instanceof ApiError && Array.isArray(e.problem?.errors) ? (e.problem!.errors as string[]) : [(e as Error).message];
      setSaveErrors(errs);
      setSaving(false);
    }
  }

  async function remove() {
    if (!confirm(`Delete the lesson "${source.title}"?\n\nYour progress and quiz history are kept, and the topic goes back to "Planned". GitHub keeps the file in its history.`)) return;
    setDeleting(true);
    try {
      await api(`/lessons/${source.slug}`, { method: "DELETE" });
      saveDraft(source.slug, null);
      router.push(`/learn/${source.slug}`);
      router.refresh();
    } catch (e) {
      setSaveErrors([(e as Error).message]);
      setDeleting(false);
    }
  }

  const status = useMemo(() => {
    if (!text.trim()) return { tone: "idle" as const, label: "Paste the AI's full answer below." };
    if (!preview) return { tone: "idle" as const, label: "Checking…" };
    if (!preview.valid) return { tone: "error" as const, label: `${preview.errors.length} problem(s) to fix before saving` };
    const types = Object.entries(preview.questionsByType).map(([t, n]) => `${n} ${t.replace("_", " ").toLowerCase()}`).join(", ");
    return {
      tone: preview.warnings.length ? ("warn" as const) : ("ok" as const),
      label: `${preview.sections}/${preview.expectedSections} sections · ${preview.questions} quiz questions${types ? ` (${types})` : ""}`,
    };
  }, [text, preview]);

  const canSave = !!text.trim() && !!preview?.valid && !saving && !checking;

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 md:px-8 md:py-8">
      {/* Header */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <Link href={`/learn/${source.slug}`} className="text-sm text-fg-faint hover:text-fg">
            ← {source.title}
          </Link>
          <h1 className="mt-1 text-2xl font-bold tracking-tight">{source.published ? "Edit lesson" : "Add lesson"}</h1>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {source.published && (
            <button
              onClick={remove}
              disabled={deleting || saving}
              className="inline-flex items-center gap-1.5 rounded-md border border-border px-3 py-1.5 text-sm text-danger hover:bg-danger/10 disabled:opacity-60"
            >
              {deleting ? <Loader2 className="size-4 animate-spin" /> : <Trash2 className="size-4" />} Delete
            </button>
          )}
          <button
            onClick={save}
            disabled={!canSave}
            className="inline-flex items-center gap-1.5 rounded-md bg-accent px-4 py-1.5 text-sm font-medium text-white hover:opacity-90 disabled:opacity-40"
          >
            {saving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />} Save lesson
          </button>
        </div>
      </div>

      {/* How-to (new lessons) */}
      {!source.published && !text.trim() && (
        <ol className="mt-5 space-y-1.5 rounded-xl border border-border bg-bg-subtle p-4 text-sm text-fg-muted">
          <li><strong className="text-fg">1.</strong> Copy the prompt for this topic and send it to ChatGPT or Claude.</li>
          <li><strong className="text-fg">2.</strong> Copy the <em>whole</em> answer with the chat&apos;s copy button (it copies raw Markdown).</li>
          <li><strong className="text-fg">3.</strong> Paste it below, check the preview, and save. The quiz at the end becomes this lesson&apos;s quiz.</li>
        </ol>
      )}
      <CopyPromptButton slug={source.slug} className="mt-4" />

      {restoredDraft && dirty && (
        <p className="mt-3 flex items-center gap-2 text-sm text-warning">
          <AlertTriangle className="size-4" /> Restored your unsaved draft.
          <button className="underline" onClick={() => { setText(source.markdown); setRestoredDraft(false); }}>Discard it</button>
        </p>
      )}

      {/* Validation status */}
      <div
        className={clsx(
          "mt-4 rounded-lg border px-3.5 py-2.5 text-sm",
          status.tone === "ok" && "border-success/40 bg-success/5",
          status.tone === "warn" && "border-warning/40 bg-warning/5",
          status.tone === "error" && "border-danger/40 bg-danger/5",
          status.tone === "idle" && "border-border",
        )}
      >
        <p className="flex items-center gap-2 font-medium">
          {checking ? (
            <Loader2 className="size-4 animate-spin text-fg-faint" />
          ) : status.tone === "ok" ? (
            <CheckCircle2 className="size-4 text-success" />
          ) : status.tone === "warn" ? (
            <AlertTriangle className="size-4 text-warning" />
          ) : status.tone === "error" ? (
            <XCircle className="size-4 text-danger" />
          ) : null}
          {status.label}
        </p>
        {(saveErrors ?? preview?.errors ?? []).length > 0 && (
          <ul className="mt-2 list-disc space-y-0.5 pl-5 text-danger">
            {(saveErrors ?? preview!.errors).map((e, i) => <li key={i}>{e}</li>)}
          </ul>
        )}
        {preview && preview.warnings.length > 0 && (
          <ul className="mt-2 list-disc space-y-0.5 pl-5 text-warning">
            {preview.warnings.map((w, i) => <li key={i}>{w}</li>)}
          </ul>
        )}
      </div>

      {/* Mobile tabs */}
      <div className="mt-4 flex gap-1 lg:hidden">
        {(["write", "preview"] as const).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={clsx("rounded-md px-3 py-1.5 text-sm capitalize", tab === t ? "bg-fg text-bg" : "text-fg-muted hover:bg-bg-hover")}
          >
            {t}
          </button>
        ))}
      </div>

      <div className="mt-3 grid gap-4 lg:mt-4 lg:grid-cols-2">
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          spellCheck={false}
          placeholder={"---\ntitle: …\ndifficulty: INTERMEDIATE\n---\n\n## 1. What is this concept?\n…\n\n## 14. Quiz\n\n~~~quiz\nquestions:\n  - key: q1\n…\n~~~"}
          className={clsx(
            "h-[70dvh] w-full resize-none rounded-xl border border-border bg-bg p-4 font-mono text-[13px] leading-relaxed outline-none focus:border-accent",
            tab === "preview" && "hidden lg:block",
          )}
        />
        <div className={clsx("h-[70dvh] overflow-y-auto rounded-xl border border-border p-5", tab === "write" && "hidden lg:block")}>
          {preview?.body ? (
            <>
              {preview.summary && <p className="mb-4 text-fg-muted">{preview.summary}</p>}
              <InlineMarkdown compact={false}>{preview.body}</InlineMarkdown>
              {preview.questions > 0 && (
                <p className="mt-8 rounded-lg bg-bg-subtle p-3 text-sm text-fg-muted">
                  + Quiz with {preview.questions} questions (shown at the end of the lesson after saving)
                </p>
              )}
            </>
          ) : (
            <p className="text-sm text-fg-faint">The preview appears here. Code highlighting is applied after saving.</p>
          )}
        </div>
      </div>
    </div>
  );
}
