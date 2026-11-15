import "server-only";
import { createHighlighter, type Highlighter, type ShikiTransformer } from "shiki";

export const SHIKI_THEMES = { light: "github-light", dark: "github-dark" } as const;

const LANGS = [
  "java", "kotlin", "bash", "shell", "yaml", "json", "sql", "xml", "properties",
  "typescript", "javascript", "tsx", "html", "css", "dockerfile", "http", "python", "diff",
];

let highlighter: Promise<Highlighter> | null = null;

/** One shared Shiki instance per server process (loading grammars is the expensive part). */
export function getHighlighter(): Promise<Highlighter> {
  highlighter ??= createHighlighter({ themes: Object.values(SHIKI_THEMES), langs: LANGS });
  return highlighter;
}

export function isSupportedLang(lang: string) {
  return LANGS.includes(lang);
}

/** Adds data-language to <pre> so the code block header can show it. */
export const languageLabel: ShikiTransformer = {
  pre(node) {
    node.properties["data-language"] = this.options.lang;
  },
};

/** Highlight a standalone snippet (quiz code) to HTML. */
export async function highlightCode(code: string, lang = "java"): Promise<string> {
  const h = await getHighlighter();
  return h.codeToHtml(code.replace(/\n$/, ""), {
    lang: isSupportedLang(lang) ? lang : "text",
    themes: SHIKI_THEMES,
    transformers: [languageLabel],
  });
}
