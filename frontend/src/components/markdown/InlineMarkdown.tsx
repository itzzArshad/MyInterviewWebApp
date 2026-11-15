"use client";

import Markdown from "react-markdown";
import remarkGfm from "remark-gfm";

/** Lightweight client-side Markdown for short text (quiz prompts, options, explanations). */
export function InlineMarkdown({ children, className = "" }: { children: string; className?: string }) {
  return (
    <div className={`md md-compact ${className}`}>
      <Markdown remarkPlugins={[remarkGfm]}>{children}</Markdown>
    </div>
  );
}
