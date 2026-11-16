"use client";

import { isValidElement, useEffect, useRef, useState, type ComponentProps, type ReactNode } from "react";
import { Check, Copy } from "lucide-react";
import { MermaidDiagram } from "./MermaidDiagram";

type PreProps = ComponentProps<"pre"> & { "data-language"?: string };

/** Language from Shiki's data attribute (server) or the `language-x` class on <code> (client markdown). */
function languageOf(props: PreProps): string | undefined {
  if (props["data-language"]) return props["data-language"];
  const child = props.children as ReactNode;
  if (isValidElement<{ className?: string }>(child)) {
    return /language-([\w-]+)/.exec(child.props.className ?? "")?.[1];
  }
  return undefined;
}

/** Code block with a language label and copy button; ```mermaid blocks render as diagrams. */
export function CodeBlock(props: PreProps) {
  const ref = useRef<HTMLPreElement>(null);
  const [copied, setCopied] = useState(false);
  const [mermaidSource, setMermaidSource] = useState<string | null>(null);
  const lang = languageOf(props);
  const showLang = lang && lang !== "text";

  useEffect(() => {
    if (lang === "mermaid" && ref.current) setMermaidSource(ref.current.textContent ?? "");
  }, [lang, props.children]);

  async function copy() {
    const text = ref.current?.textContent ?? "";
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      /* clipboard blocked */
    }
  }

  if (lang === "mermaid") {
    return (
      <>
        <pre ref={ref} hidden {...props} />
        {mermaidSource !== null && <MermaidDiagram code={mermaidSource} />}
      </>
    );
  }

  return (
    <div className="code-block group">
      <div className="code-block-bar">
        <span>{showLang ? lang : ""}</span>
        <button type="button" onClick={copy} aria-label="Copy code" className="code-copy">
          {copied ? <Check className="size-3.5" /> : <Copy className="size-3.5" />}
          <span>{copied ? "Copied" : "Copy"}</span>
        </button>
      </div>
      <pre ref={ref} {...props} />
    </div>
  );
}
