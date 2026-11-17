import type { Metadata } from "next";
import { Suspense } from "react";
import { RevisionView } from "@/components/revision/RevisionView";

export const metadata: Metadata = { title: "Revision" };

export default function RevisionPage() {
  return (
    <div className="mx-auto max-w-3xl px-5 py-8 md:px-12 md:py-12">
      <Suspense>
        <RevisionView />
      </Suspense>
    </div>
  );
}
