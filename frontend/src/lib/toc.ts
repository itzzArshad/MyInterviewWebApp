import GithubSlugger from "github-slugger";

export interface TocItem {
  id: string;
  text: string;
}

/**
 * Extracts `##` headings with the same ids rehype-slug will generate
 * (one slugger over *all* headings in document order, code fences skipped).
 */
export function extractToc(markdown: string): TocItem[] {
  const slugger = new GithubSlugger();
  const items: TocItem[] = [];
  let inFence = false;
  for (const line of markdown.split(/\r?\n/)) {
    if (/^\s*(```|~~~)/.test(line)) {
      inFence = !inFence;
      continue;
    }
    if (inFence) continue;
    const m = /^(#{1,6})\s+(.+?)\s*#*\s*$/.exec(line);
    if (!m) continue;
    const text = m[2]
      .replace(/`([^`]*)`/g, "$1")
      .replace(/\*\*([^*]+)\*\*|\*([^*]+)\*/g, "$1$2")
      .replace(/\[([^\]]+)\]\([^)]*\)/g, "$1");
    const id = slugger.slug(text);
    if (m[1].length === 2) items.push({ id, text });
  }
  return items;
}
