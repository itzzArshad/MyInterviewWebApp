import { notFound } from "next/navigation";
import type { Metadata } from "next";
import { ComingSoon } from "@/components/ComingSoon";
import { NAV } from "@/lib/nav";

// Placeholder pages for modules that ship in later phases. Each gets its own route when built.
const PLACEHOLDERS = NAV.filter((n) => !n.available).map((n) => n.key);

export const dynamicParams = false;

export function generateStaticParams() {
  return PLACEHOLDERS.map((module) => ({ module }));
}

export async function generateMetadata({ params }: { params: Promise<{ module: string }> }): Promise<Metadata> {
  const { module } = await params;
  return { title: NAV.find((n) => n.key === module)?.label };
}

export default async function ModulePage({ params }: { params: Promise<{ module: string }> }) {
  const { module } = await params;
  if (!PLACEHOLDERS.includes(module)) notFound();
  return <ComingSoon moduleKey={module} />;
}
