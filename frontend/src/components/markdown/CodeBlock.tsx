"use client";

import { useRef, useState, type ComponentProps } from "react";
import { Check, Copy } from "lucide-react";

type PreProps = ComponentProps<"pre"> & { "data-language"?: string };

/** Code block with a language label and copy button. Scrolls horizontally on small screens. */
export function CodeBlock(props: PreProps) {
  const ref = useRef<HTMLPreElement>(null);
  const [copied, setCopied] = useState(false);
  const lang = props["data-language"];
  const showLang = lang && lang !== "text";

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
