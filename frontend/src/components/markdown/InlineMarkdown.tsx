"use client";

import Markdown, { type Components } from "react-markdown";
import remarkGfm from "remark-gfm";
import { CodeBlock } from "./CodeBlock";

const components: Components = {
  pre: ({ node: _node, ...props }) => <CodeBlock {...props} />,
  table: ({ node: _node, ...props }) => (
    <div className="table-wrap">
      <table {...props} />
    </div>
  ),
};

/**
 * Client-side Markdown (no syntax highlighting): quiz prompts/explanations and the editor preview.
 * `compact` tightens spacing for short snippets.
 */
export function InlineMarkdown({
  children,
  className = "",
  compact = true,
}: {
  children: string;
  className?: string;
  compact?: boolean;
}) {
  return (
    <div className={`md ${compact ? "md-compact" : ""} ${className}`}>
      <Markdown remarkPlugins={[remarkGfm]} components={components}>
        {children}
      </Markdown>
    </div>
  );
}
