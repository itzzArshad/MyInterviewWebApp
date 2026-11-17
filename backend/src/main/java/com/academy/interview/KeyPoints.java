package com.academy.interview;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The checklist you score a spoken answer against. Uses the lesson's explicit key points when present;
 * otherwise derives them from the model answer (its bold terms, else its first sentences).
 */
public final class KeyPoints {

    private static final TypeReference<List<String>> LIST = new TypeReference<>() {
    };
    private static final Pattern BOLD = Pattern.compile("\\*\\*([^*\\n]{3,90})\\*\\*");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");

    private KeyPoints() {
    }

    public static List<String> resolve(ObjectMapper json, String keyPointsJson, String answerMd) {
        if (keyPointsJson != null && !keyPointsJson.isBlank()) {
            try {
                List<String> explicit = json.readValue(keyPointsJson, LIST);
                if (!explicit.isEmpty()) return explicit;
            } catch (Exception ignored) {
                // fall through to derived points
            }
        }
        return derive(answerMd);
    }

    static List<String> derive(String answerMd) {
        if (answerMd == null || answerMd.isBlank()) return List.of();
        String text = answerMd.replaceAll("(?s)```.*?```", " ").replaceAll("(?s)~~~.*?~~~", " ");

        Set<String> bold = new LinkedHashSet<>();
        Matcher m = BOLD.matcher(text);
        while (m.find() && bold.size() < 6) {
            String b = m.group(1).strip().replaceAll("[:.]$", "");
            if (!b.toLowerCase().startsWith("strong answer") && !b.toLowerCase().startsWith("model answer")) bold.add(b);
        }
        if (bold.size() >= 3) return List.copyOf(bold);

        String plain = text.replaceAll("\\*\\*|__|`|^>\\s*", "").replaceAll("(?m)^\\s*[-*\\d.]+\\s+", "").replaceAll("\\s+", " ").strip();
        List<String> points = new ArrayList<>();
        for (String s : SENTENCE_SPLIT.split(plain)) {
            String t = s.strip().replaceAll("^\"|\"$", "");
            if (t.length() < 25) continue;
            points.add(t.length() > 160 ? t.substring(0, 157) + "…" : t);
            if (points.size() == 5) break;
        }
        return points.isEmpty() ? List.of(plain.length() > 160 ? plain.substring(0, 157) + "…" : plain) : points;
    }
}
