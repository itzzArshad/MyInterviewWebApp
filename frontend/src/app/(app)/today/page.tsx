import type { Metadata } from "next";
import { TodayView } from "@/components/today/TodayView";

export const metadata: Metadata = { title: "Today" };

export default function TodayPage() {
  return (
    <div className="mx-auto max-w-3xl px-5 py-8 md:px-12 md:py-12">
      <TodayView />
    </div>
  );
}
