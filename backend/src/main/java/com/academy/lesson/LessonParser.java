package com.academy.lesson;

import com.academy.curriculum.Concept.Difficulty;
import com.academy.quiz.QuizQuestion.Type;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns one pasted lesson (as produced by the master prompt) into structured data.
 *
 * <pre>
 * ---                       optional YAML front matter (difficulty, minutes, summary, related)
 * ---
 * ## 1. …  ## 13. …         lesson body (Markdown)
 * ## 14. Quiz
 * ~~~quiz                   YAML quiz block (``` fences also accepted)
 * questions: [...]
 * ~~~
 * </pre>
 *
 * Pure and side-effect free: problems are reported as {@code errors} (block saving) or
 * {@code warnings} (shown, but the lesson can still be saved). Never throws on bad input.
 */
@Component
public class LessonParser {

    public static final int MAX_LENGTH = 500_000;

    private static final Pattern FENCE = Pattern.compile("^\\s{0,3}(`{3,}|~{3,})\\s*([\\w-]*).*$");
    private static final Pattern QUIZ_FENCE = Pattern.compile("^\\s{0,3}(`{3,}|~{3,})\\s*quiz\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBERED_H2 = Pattern.compile("^##\\s+(\\d{1,2})\\.\\s+.+$");
    private static final Pattern H2 = Pattern.compile("^##\\s+.+$");
    private static final Pattern TRAILING_QUIZ_HEADING = Pattern.compile("(?s)\\n*#{2,3}\\s*(?:\\d+\\.\\s*)?Quiz\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LETTER = Pattern.compile("^\\(?([A-Za-z])[).:]?(?:\\s.*)?$");

    public record ParsedQuestion(String key, Type type, Difficulty difficulty, String prompt, String code,
                                 List<String> options, String correctAnswer, List<String> hints, String explanation,
                                 List<String> keyPoints) {
        public boolean selfGraded() {
            return correctAnswer == null;
        }
    }

    public record FollowUp(String question, String answer) {
    }

    /** An interview Q&A from the lesson's interview section. */
    public record ParsedInterviewItem(String key, String question, String answer, List<String> keyPoints, List<FollowUp> followUps) {
    }

    public record ParsedLesson(String source, String body, Difficulty difficulty, Integer minutes, String summary,
                               List<String> related, List<ParsedQuestion> questions, int numberedSections,
                               List<ParsedInterviewItem> interview, List<String> errors, List<String> warnings) {
        public boolean valid() {
            return errors.isEmpty();
        }
    }

    public ParsedLesson parse(String raw, int expectedSections) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return new ParsedLesson("", "", null, null, null, List.of(), List.of(), 0, List.of(), List.of("The lesson is empty."), List.of());
        }
        if (raw.length() > MAX_LENGTH) {
            return new ParsedLesson("", "", null, null, null, List.of(), List.of(), 0, List.of(),
                    List.of("The lesson is too long (" + raw.length() + " characters, max " + MAX_LENGTH + ")."), List.of());
        }
        String source = normalise(raw);
        String text = source;

        // ---- front matter
        Map<String, Object> meta = Map.of();
        if (text.startsWith("---\n")) {
            int end = text.indexOf("\n---", 4);
            if (end < 0) {
                errors.add("Front matter starts with '---' but is never closed with another '---' line.");
            } else {
                try {
                    Object loaded = yaml().load(text.substring(4, end));
                    if (loaded instanceof Map<?, ?> m) meta = castMap(m);
                } catch (Exception e) {
                    errors.add("Front matter is not valid YAML: " + yamlMessage(e));
                }
                int after = text.indexOf('\n', end + 1);
                text = after < 0 ? "" : text.substring(after + 1);
            }
        } else {
            warnings.add("No front matter found — difficulty, minutes and summary use defaults.");
        }

        Difficulty difficulty = parseDifficulty(meta.get("difficulty"), warnings, "Front matter");
        Integer minutes = meta.get("minutes") instanceof Number n ? n.intValue()
                : meta.get("minutes") != null ? parseIntOrNull(String.valueOf(meta.get("minutes"))) : null;
        String summary = meta.get("summary") == null ? null : String.valueOf(meta.get("summary")).strip();
        List<String> related = meta.get("related") instanceof List<?> l ? l.stream().map(String::valueOf).toList() : List.of();

        // ---- quiz block (the last fence tagged "quiz")
        List<String> lines = new ArrayList<>(Arrays.asList(text.split("\n", -1)));
        int quizStart = -1, quizEnd = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (QUIZ_FENCE.matcher(lines.get(i)).matches()) quizStart = i;
        }
        List<ParsedQuestion> questions = List.of();
        if (quizStart >= 0) {
            Matcher open = QUIZ_FENCE.matcher(lines.get(quizStart));
            open.matches();
            String fence = open.group(1);
            // CommonMark closing fence: same character, at least as long, indented ≤ 3 spaces.
            // Fences inside YAML block scalars are indented deeper, so they never close the quiz.
            Pattern close = Pattern.compile("^ {0,3}" + Pattern.quote(String.valueOf(fence.charAt(0))) + "{" + fence.length() + ",}\\s*$");
            for (int i = quizStart + 1; i < lines.size(); i++) {
                if (close.matcher(lines.get(i)).matches()) {
                    quizEnd = i;
                    break;
                }
            }
            if (quizEnd < 0) {
                errors.add("The quiz block is never closed — add a line with " + fence + " after the last question.");
            } else {
                String quizYaml = String.join("\n", lines.subList(quizStart + 1, quizEnd));
                questions = parseQuiz(quizYaml, errors, warnings, fence.charAt(0) == '`');
                lines.subList(quizStart, quizEnd + 1).clear();
            }
        } else {
            warnings.add("No quiz block found (~~~quiz … ~~~) — the lesson will be saved without a quiz.");
        }

        String body = TRAILING_QUIZ_HEADING.matcher(String.join("\n", lines).strip()).replaceFirst("").strip();
        if (body.isBlank()) errors.add("The lesson body is empty.");

        // ---- section structure (warnings only — any Markdown still renders fine)
        Set<Integer> numbers = new TreeSet<>();
        int h2Count = 0;
        boolean inFence = false;
        for (String l : body.split("\n")) {
            if (FENCE.matcher(l).matches()) inFence = !inFence;
            if (inFence) continue;
            if (H2.matcher(l).matches()) h2Count++;
            Matcher m = NUMBERED_H2.matcher(l);
            if (m.matches()) numbers.add(Integer.parseInt(m.group(1)));
        }
        if (h2Count == 0) {
            warnings.add("No '## ' headings found — the lesson will have no table of contents.");
        } else if (expectedSections > 0) {
            List<Integer> missing = new ArrayList<>();
            for (int i = 1; i <= expectedSections; i++) if (!numbers.contains(i)) missing.add(i);
            if (!missing.isEmpty()) warnings.add("Missing numbered sections: " + missing + " (expected 1–" + expectedSections + ").");
        }

        List<ParsedInterviewItem> interview = extractInterview(body);
        if (h2Count > 0 && interview.isEmpty()) {
            warnings.add("No interview Q&A found (\"**Q1. …**\" lines in the interview section) — this lesson adds nothing to the interview bank.");
        }

        return new ParsedLesson(source, body, difficulty, minutes, summary, related, questions, numbers.size(),
                interview, List.copyOf(errors), List.copyOf(warnings));
    }

    // ---------------------------------------------------------------- interview section

    private static final Pattern INTERVIEW_H2 = Pattern.compile("^##\\s+(?:\\d{1,2}\\.\\s*)?.*interview.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern QA_START = Pattern.compile(
            "^(?:\\*\\*\\s*Q(\\d{1,3})[.:)]\\s*(.+?)\\s*\\*\\*|#{3,4}\\s*Q(\\d{1,3})[.:)]\\s*(.+?))\\s*$");
    private static final Pattern KEY_POINTS = Pattern.compile("^\\s*[-*]?\\s*\\**\\s*Key points\\s*:?\\s*\\**\\s*:?\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOLLOW_UP = Pattern.compile("^\\s*[-*]?\\s*\\**\\s*Follow-?up\\s*:?\\s*\\**\\s*:?\\s*(.*)$", Pattern.CASE_INSENSITIVE);

    /**
     * Finds the lesson's interview section (an H2 whose title mentions "interview") and splits it into
     * Q&As. Each question starts with a bold "**Q1. …**" (or "### Q1. …") line; optional lines
     * "- Key points: a | b | c" and "- Follow-up: question? → answer" are pulled out of the answer.
     */
    /** Markdown of the first H2 section whose title contains {@code word} (e.g. "Summary"), or null. */
    public static String section(String body, String word) {
        if (body == null) return null;
        StringBuilder out = new StringBuilder();
        boolean in = false, fence = false;
        String w = word.toLowerCase(Locale.ROOT);
        for (String line : body.split("\n", -1)) {
            if (FENCE.matcher(line).matches()) fence = !fence;
            if (!fence && H2.matcher(line).matches()) {
                if (in) break;
                in = line.toLowerCase(Locale.ROOT).contains(w);
                continue;
            }
            if (in) out.append(line).append('\n');
        }
        String s = out.toString().strip();
        return s.isEmpty() ? null : s;
    }

    public static List<ParsedInterviewItem> extractInterview(String body) {
        List<String> section = new ArrayList<>();
        boolean in = false, fence = false;
        for (String line : body.split("\n", -1)) {
            if (FENCE.matcher(line).matches()) fence = !fence;
            if (!fence && H2.matcher(line).matches()) {
                if (in) break;
                in = INTERVIEW_H2.matcher(line).matches();
                continue;
            }
            if (in) section.add(line);
        }

        List<ParsedInterviewItem> items = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        String key = null, question = null;
        List<String> answer = new ArrayList<>();
        List<String> keyPoints = new ArrayList<>();
        List<FollowUp> followUps = new ArrayList<>();
        fence = false;
        for (String line : section) {
            if (FENCE.matcher(line).matches()) fence = !fence;
            Matcher m = fence ? null : QA_START.matcher(line.strip());
            if (m != null && m.matches()) {
                addItem(items, keys, key, question, answer, keyPoints, followUps);
                key = "Q" + (m.group(1) != null ? m.group(1) : m.group(3));
                question = (m.group(2) != null ? m.group(2) : m.group(4)).strip();
                answer = new ArrayList<>();
                keyPoints = new ArrayList<>();
                followUps = new ArrayList<>();
                continue;
            }
            if (key == null) continue;
            Matcher kp = fence ? null : KEY_POINTS.matcher(line);
            Matcher fu = fence ? null : FOLLOW_UP.matcher(line);
            if (kp != null && kp.matches()) {
                for (String p : kp.group(1).split("\\s*[|;]\\s*")) if (!p.isBlank()) keyPoints.add(p.strip());
            } else if (fu != null && fu.matches()) {
                String[] qa = fu.group(1).split("\\s*(?:→|->)\\s*", 2);
                followUps.add(new FollowUp(qa[0].strip(), qa.length > 1 ? qa[1].strip() : ""));
            } else {
                answer.add(line);
            }
        }
        addItem(items, keys, key, question, answer, keyPoints, followUps);
        return items;
    }

    private static void addItem(List<ParsedInterviewItem> items, Set<String> keys, String key, String question,
                                List<String> answer, List<String> keyPoints, List<FollowUp> followUps) {
        if (key == null) return;
        String text = String.join("\n", answer).strip();
        if (text.isEmpty() || question.isEmpty()) return;
        String unique = key;
        for (int i = 2; !keys.add(unique); i++) unique = key + "-" + i;
        items.add(new ParsedInterviewItem(unique, question, text, List.copyOf(keyPoints), List.copyOf(followUps)));
    }

    // ---------------------------------------------------------------- quiz

    private List<ParsedQuestion> parseQuiz(String yamlText, List<String> errors, List<String> warnings, boolean backtickFence) {
        Object loaded;
        try {
            loaded = yaml().load(yamlText);
        } catch (Exception e) {
            String hint = backtickFence
                    ? " (Tip: fence the quiz with ~~~quiz … ~~~ so ``` code fences inside explanations don't end it early.)"
                    : "";
            errors.add("Quiz is not valid YAML: " + yamlMessage(e) + hint);
            return List.of();
        }
        List<?> items = loaded instanceof Map<?, ?> m && m.get("questions") instanceof List<?> l ? l
                : loaded instanceof List<?> l2 ? l2 : null;
        if (items == null || items.isEmpty()) {
            errors.add("The quiz block has no 'questions:' list.");
            return List.of();
        }

        List<ParsedQuestion> result = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        int index = 0;
        for (Object item : items) {
            index++;
            if (!(item instanceof Map<?, ?> raw)) {
                errors.add("Question #" + index + " is not a mapping (each question starts with '- key: …').");
                continue;
            }
            Map<String, Object> q = castMap(raw);
            String key = str(q.get("key"));
            if (key == null) {
                key = "q" + index;
                warnings.add("Question #" + index + " has no key — using '" + key + "'.");
            }
            String where = "Question " + key;
            if (!keys.add(key)) {
                errors.add(where + ": duplicate key.");
                continue;
            }

            Type type;
            try {
                type = Type.valueOf(Objects.requireNonNullElse(str(q.get("type")), "").toUpperCase(Locale.ROOT).replace(' ', '_'));
            } catch (IllegalArgumentException e) {
                errors.add(where + ": unknown type '" + q.get("type") + "'. Use one of " + Arrays.toString(Type.values()) + ".");
                continue;
            }
            Difficulty difficulty = parseDifficulty(q.get("difficulty"), warnings, where);
            if (difficulty == null) difficulty = Difficulty.INTERMEDIATE;

            String prompt = str(q.get("prompt"));
            if (prompt == null) errors.add(where + ": missing 'prompt'.");
            String explanation = str(q.get("explanation"));
            if (explanation == null) {
                warnings.add(where + ": no explanation.");
                explanation = "";
            }

            List<String> options = q.get("options") instanceof List<?> l
                    ? l.stream().map(o -> String.valueOf(o).strip()).toList() : null;
            if (options != null && options.size() < 2) {
                errors.add(where + ": needs at least 2 options.");
                continue;
            }
            List<String> hints = q.get("hints") instanceof List<?> h ? h.stream().map(String::valueOf).toList()
                    : q.get("hints") != null ? List.of(String.valueOf(q.get("hints"))) : List.of();

            String answer = normaliseAnswer(type, options, q.get("answer"), where, errors);
            if (answer == null && options != null) continue; // error already recorded
            if (answer == null && (type == Type.MCQ || type == Type.TRUE_FALSE)) {
                errors.add(where + ": " + type + " questions need an answer.");
                continue;
            }
            if (answer == null && explanation.isBlank()) {
                errors.add(where + ": self-graded questions need a model answer in 'explanation'.");
            }
            Object kp = q.containsKey("keyPoints") ? q.get("keyPoints") : q.get("key_points");
            List<String> keyPoints = kp instanceof List<?> kl ? kl.stream().map(o -> String.valueOf(o).strip()).filter(s -> !s.isEmpty()).toList() : List.of();
            result.add(new ParsedQuestion(key, type, difficulty, prompt == null ? "" : prompt.strip(), stripTrailingNewline(str(q.get("code"))),
                    options, answer, hints, explanation.strip(), keyPoints));
        }
        return result;
    }

    /** Canonical answer: option index ("0".."n"), "true"/"false", exact output text, or null (self-graded). */
    static String normaliseAnswer(Type type, List<String> options, Object answer, String where, List<String> errors) {
        if (type == Type.TRUE_FALSE) {
            if (answer instanceof Boolean b) return b.toString();
            String s = answer == null ? "" : String.valueOf(answer).strip().toLowerCase(Locale.ROOT);
            if (s.equals("true") || s.equals("false")) return s;
            errors.add(where + ": TRUE_FALSE answer must be true or false.");
            return null;
        }
        if (options != null) {
            Integer idx = null;
            if (answer instanceof Integer i) {
                idx = i; // 0-based index
            } else if (answer != null) {
                Matcher m = LETTER.matcher(String.valueOf(answer).strip());
                if (m.matches()) idx = Character.toUpperCase(m.group(1).charAt(0)) - 'A';
            }
            if (idx == null || idx < 0 || idx >= options.size()) {
                errors.add(where + ": answer '" + answer + "' must be an option letter A–" + (char) ('A' + options.size() - 1) + ".");
                return null;
            }
            return idx.toString();
        }
        if (answer == null) return null;
        String s = String.valueOf(answer).strip();
        return s.isEmpty() ? null : s;
    }

    // ---------------------------------------------------------------- helpers

    /** Line endings, BOM, and a whole-answer ```markdown wrapper that chat UIs sometimes add. */
    static String normalise(String raw) {
        String s = raw.replace("\r\n", "\n").replace('\r', '\n');
        if (s.startsWith("﻿")) s = s.substring(1);
        s = s.strip();
        String[] lines = s.split("\n", -1);
        if (lines.length >= 2 && lines[0].matches("^`{3,}\\s*(markdown|md)?\\s*$") && lines[lines.length - 1].matches("^`{3,}\\s*$")) {
            s = String.join("\n", Arrays.copyOfRange(lines, 1, lines.length - 1)).strip();
        }
        return s;
    }

    private static Difficulty parseDifficulty(Object v, List<String> warnings, String where) {
        if (v == null) return null;
        String s = String.valueOf(v).strip().toUpperCase(Locale.ROOT);
        try {
            return Difficulty.valueOf(s);
        } catch (IllegalArgumentException e) {
            warnings.add(where + ": difficulty '" + v + "' is not BEGINNER, INTERMEDIATE or ADVANCED — ignored.");
            return null;
        }
    }

    private static Yaml yaml() {
        return new Yaml(new SafeConstructor(new LoaderOptions()));
    }

    private static String yamlMessage(Exception e) {
        if (e instanceof MarkedYAMLException m && m.getProblemMark() != null) {
            return m.getProblem() + " (line " + (m.getProblemMark().getLine() + 1) + ")";
        }
        return e.getMessage();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> m) {
        return (Map<String, Object>) m;
    }

    private static String str(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o);
        return s.isBlank() ? null : s;
    }

    private static String stripTrailingNewline(String s) {
        return s == null ? null : s.stripTrailing();
    }

    private static Integer parseIntOrNull(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
