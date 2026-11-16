import Link from "next/link";
import type { Metadata } from "next";
import { ArrowLeft, ArrowRight, Clock, PenLine, Pencil, Plus } from "lucide-react";
import { CopyPromptButton } from "@/components/lesson/CopyPromptButton";
import { SyncBadge } from "@/components/lesson/SyncBadge";
import { serverApi } from "@/lib/server-api";
import type { ConceptDetail, QuestionView } from "@/lib/types";
import { extractToc } from "@/lib/toc";
import { highlightCode } from "@/lib/highlight";
import { Markdown } from "@/components/markdown/Markdown";
import { DifficultyBadge } from "@/components/learn/badges";
import { LessonActions } from "@/components/learn/LessonActions";
import { Toc } from "@/components/learn/Toc";
import { Quiz } from "@/components/learn/Quiz";

type Params = { params: Promise<{ slug: string }> };

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { slug } = await params;
  const c = await serverApi<ConceptDetail>(`/learn/concepts/${slug}`);
  return { title: c.title };
}

export default async function LessonPage({ params }: Params) {
  const { slug } = await params;
  const c = await serverApi<ConceptDetail>(`/learn/concepts/${slug}`);

  // Pre-highlight quiz code snippets on the server (answers are not part of this payload).
  const questions = c.published && c.questionCount > 0 ? await serverApi<QuestionView[]>(`/quiz/concepts/${slug}/questions`) : [];
  const codeHtml: Record<number, string> = {};
  await Promise.all(
    questions.filter((q) => q.code).map(async (q) => (codeHtml[q.id] = await highlightCode(q.code!, "java"))),
  );

  const toc = c.bodyMd ? [...extractToc(c.bodyMd), ...(c.questionCount ? [{ id: "quiz", text: "14. Quiz" }] : [])] : [];

  return (
    <div className="mx-auto flex max-w-6xl gap-12 px-5 py-8 md:px-12 md:py-12">
      <article className="min-w-0 flex-1 xl:max-w-3xl">
        {/* Breadcrumb */}
        <nav aria-label="Breadcrumb" className="flex flex-wrap items-center gap-1 text-sm text-fg-faint">
          <Link href="/learn" className="hover:text-fg">Learn</Link>
          <span>/</span>
          <Link href={`/learn?subject=${c.subject.slug}`} className="hover:text-fg">{c.subject.title}</Link>
          <span>/</span>
          <span className="truncate">{c.chapter.title}</span>
        </nav>

        <h1 className="mt-4 text-3xl leading-tight font-bold tracking-tight md:text-4xl">{c.title}</h1>

        {c.published ? (
          <>
            <div className="mt-3 flex flex-wrap items-center gap-3 text-sm text-fg-muted">
              <DifficultyBadge value={c.difficulty} />
              <span className="inline-flex items-center gap-1"><Clock className="size-3.5" />{c.minutes} min</span>
              {c.questionCount > 0 && <span>{c.questionCount} quiz questions</span>}
            </div>
            {c.summary && <p className="mt-4 text-lg leading-relaxed text-fg-muted">{c.summary}</p>}
            <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
              <LessonActions slug={c.slug} initial={c.progress} />
              <div className="flex items-center gap-3">
                <SyncBadge slug={c.slug} sync={c.sync} />
                <Link
                  href={`/learn/${c.slug}/edit`}
                  className="inline-flex items-center gap-1.5 rounded-md px-2.5 py-1.5 text-sm text-fg-muted hover:bg-bg-hover hover:text-fg"
                >
                  <Pencil className="size-4" /> Edit
                </Link>
              </div>
            </div>

            <div className="mt-6">
              <Markdown source={c.bodyMd ?? ""} />
            </div>

            {c.related.length > 0 && (
              <div className="mt-8 flex flex-wrap gap-2">
                {c.related.map((r) => (
                  <Link
                    key={r.slug}
                    href={`/learn/${r.slug}`}
                    className="rounded-md border border-border px-2.5 py-1 text-sm text-fg-muted transition hover:bg-bg-hover hover:text-fg"
                  >
                    {r.title}
                    {!r.published && <span className="ml-1.5 text-[11px] text-fg-faint">planned</span>}
                  </Link>
                ))}
              </div>
            )}

            {c.questionCount > 0 && (
              <section id="quiz" className="mt-14 scroll-mt-16">
                <h2 className="border-b border-border pb-2 text-2xl font-semibold tracking-tight">14. Quiz</h2>
                <div className="mt-6">
                  <Quiz conceptSlug={c.slug} questionCount={c.questionCount} lastQuiz={c.lastQuiz} codeHtml={codeHtml} />
                </div>
              </section>
            )}
          </>
        ) : (
          <div className="mt-8 rounded-xl border border-dashed border-border p-6 md:p-8">
            <PenLine className="size-8 text-fg-faint" strokeWidth={1.5} />
            <p className="mt-3 font-medium">This lesson hasn&apos;t been written yet</p>
            <p className="mt-1 text-sm text-fg-muted">
              Copy the prompt, send it to ChatGPT or Claude, then paste the full answer — the lesson and its quiz are created
              from it.
            </p>
            <div className="mt-5 flex flex-wrap items-start gap-3">
              <Link
                href={`/learn/${c.slug}/edit`}
                className="inline-flex items-center gap-1.5 rounded-md bg-accent px-4 py-1.5 text-sm font-medium text-white hover:opacity-90"
              >
                <Plus className="size-4" /> Add lesson
              </Link>
              <CopyPromptButton slug={c.slug} />
            </div>
          </div>
        )}

        {/* Prev / next */}
        <nav aria-label="Lesson navigation" className="mt-14 grid gap-3 border-t border-border pt-6 sm:grid-cols-2">
          {c.prev ? (
            <Link href={`/learn/${c.prev.slug}`} className="group rounded-lg border border-border p-3 transition hover:bg-bg-subtle">
              <span className="flex items-center gap-1 text-xs text-fg-faint"><ArrowLeft className="size-3" />Previous</span>
              <span className="mt-0.5 block truncate text-sm font-medium">{c.prev.title}</span>
            </Link>
          ) : <span />}
          {c.next && (
            <Link href={`/learn/${c.next.slug}`} className="group rounded-lg border border-border p-3 text-right transition hover:bg-bg-subtle">
              <span className="flex items-center justify-end gap-1 text-xs text-fg-faint">Next<ArrowRight className="size-3" /></span>
              <span className="mt-0.5 block truncate text-sm font-medium">{c.next.title}</span>
            </Link>
          )}
        </nav>
      </article>

      {toc.length > 0 && (
        <aside className="hidden w-56 shrink-0 xl:block">
          <div className="sticky top-20 max-h-[calc(100dvh-6rem)] overflow-y-auto">
            <Toc items={toc} />
          </div>
        </aside>
      )}
    </div>
  );
}
