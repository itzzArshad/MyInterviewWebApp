import type { Metadata } from "next";
import { Suspense } from "react";
import { ExplainPractice } from "@/components/interview/ExplainPractice";

export const metadata: Metadata = { title: "Explain practice" };

export default function ExplainPage() {
  return (
    <div className="mx-auto max-w-3xl px-5 py-8 md:px-12 md:py-12">
      <Suspense>
        <ExplainPractice />
      </Suspense>
    </div>
  );
}
