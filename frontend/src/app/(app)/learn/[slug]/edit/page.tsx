import type { Metadata } from "next";
import { serverApi } from "@/lib/server-api";
import type { LessonSource } from "@/lib/types";
import { LessonEditor } from "@/components/lesson/LessonEditor";

type Params = { params: Promise<{ slug: string }> };

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { slug } = await params;
  const s = await serverApi<LessonSource>(`/lessons/${slug}/source`);
  return { title: `Edit · ${s.title}` };
}

export default async function EditLessonPage({ params }: Params) {
  const { slug } = await params;
  const source = await serverApi<LessonSource>(`/lessons/${slug}/source`);
  return <LessonEditor source={source} />;
}
