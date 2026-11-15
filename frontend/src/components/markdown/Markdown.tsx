import { MarkdownAsync, type Components } from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeSlug from "rehype-slug";
import rehypeShikiFromHighlighter from "@shikijs/rehype/core";
import { getHighlighter, languageLabel, SHIKI_THEMES } from "@/lib/highlight";
import { CodeBlock } from "./CodeBlock";

const components: Components = {
  // drop the hast `node` prop so it isn't serialised into the client component
  pre: ({ node: _node, ...props }) => <CodeBlock {...props} />,
  table: ({ node: _node, ...props }) => (
    <div className="table-wrap">
      <table {...props} />
    </div>
  ),
  a: ({ node: _node, href, ...props }) => {
    const external = href?.startsWith("http");
    return <a href={href} {...props} {...(external ? { target: "_blank", rel: "noopener noreferrer" } : {})} />;
  },
};

/**
 * Server-rendered Markdown: GitHub-flavoured (tables, task lists, strikethrough),
 * heading anchors, and Shiki syntax highlighting with light + dark themes.
 * Shared by lessons now and by notes / GitHub files in later phases.
 */
export async function Markdown({ source }: { source: string }) {
  const highlighter = await getHighlighter();
  return (
    <div className="md">
      <MarkdownAsync
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[
          rehypeSlug,
          [
            rehypeShikiFromHighlighter,
            highlighter,
            {
              themes: SHIKI_THEMES,
              defaultLanguage: "text",
              fallbackLanguage: "text",
              transformers: [languageLabel],
            },
          ],
        ]}
        components={components}
      >
        {source}
      </MarkdownAsync>
    </div>
  );
}
