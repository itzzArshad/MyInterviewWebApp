"use client";

import { useCallback, useEffect, useRef, useState } from "react";

/**
 * Records your spoken answer with the browser's MediaRecorder so you can play it back and critique
 * yourself (clarity, structure, filler words). Audio stays in memory on this device — never uploaded.
 */
export function useRecorder() {
  const [supported, setSupported] = useState(false);
  const [recording, setRecording] = useState(false);
  const [url, setUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const recorder = useRef<MediaRecorder | null>(null);
  const chunks = useRef<Blob[]>([]);
  const stream = useRef<MediaStream | null>(null);

  useEffect(() => {
    setSupported(typeof window !== "undefined" && !!navigator.mediaDevices?.getUserMedia && typeof MediaRecorder !== "undefined");
  }, []);

  const releaseStream = () => {
    stream.current?.getTracks().forEach((t) => t.stop());
    stream.current = null;
  };

  const start = useCallback(async () => {
    setError(null);
    try {
      stream.current = await navigator.mediaDevices.getUserMedia({ audio: true });
      const r = new MediaRecorder(stream.current);
      chunks.current = [];
      r.ondataavailable = (e) => e.data.size > 0 && chunks.current.push(e.data);
      r.onstop = () => {
        const blob = new Blob(chunks.current, { type: r.mimeType || "audio/webm" });
        setUrl((old) => {
          if (old) URL.revokeObjectURL(old);
          return URL.createObjectURL(blob);
        });
        releaseStream();
      };
      r.start();
      recorder.current = r;
      setRecording(true);
    } catch {
      releaseStream();
      setError("Microphone not available — allow microphone access to record (or just answer out loud).");
    }
  }, []);

  const stop = useCallback(() => {
    if (recorder.current && recorder.current.state !== "inactive") recorder.current.stop();
    setRecording(false);
  }, []);

  const reset = useCallback(() => {
    stop();
    setUrl((old) => {
      if (old) URL.revokeObjectURL(old);
      return null;
    });
  }, [stop]);

  useEffect(
    () => () => {
      if (recorder.current && recorder.current.state !== "inactive") recorder.current.stop();
      releaseStream();
    },
    [],
  );

  return { supported, recording, url, error, start, stop, reset };
}
