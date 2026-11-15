"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { Me } from "@/lib/types";

const UserContext = createContext<Me | null>(null);

export function UserProvider({ children }: { children: React.ReactNode }) {
  const [me, setMe] = useState<Me | null>(null);
  useEffect(() => {
    api<Me>("/auth/me").then(setMe).catch(() => {
      /* 401 handled by api() redirect */
    });
  }, []);
  return <UserContext.Provider value={me}>{children}</UserContext.Provider>;
}

export const useMe = () => useContext(UserContext);

export async function logout() {
  try {
    await api<void>("/auth/logout", { method: "POST" });
  } finally {
    window.location.href = "/login";
  }
}
