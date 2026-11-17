import type { Metadata } from "next";
import { MockRunner } from "@/components/interview/MockRunner";

export const metadata: Metadata = { title: "Mock interview" };

export default async function MockPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return (
    <div className="mx-auto max-w-3xl px-5 py-8 md:px-12 md:py-12">
      <MockRunner id={Number(id)} />
    </div>
  );
}
