package com.academy.lesson;

import com.academy.interview.KeyPointsAccess;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewExtractionTest {

    static final String BODY = """
            ## 10. Performance
            **Q9. Not an interview item (wrong section)**
            Ignored.

            ## 11. Interview questions and answers

            **Q1. What is a HashMap?**

            An array of buckets. Keys are hashed to find a bucket.

            - Key points: array of buckets | hash spreading | collisions chain then treeify | resize doubles
            - Follow-up: What happens on resize? → Entries split between i and i + oldCap

            **Q2. Is it thread-safe?**
            No. Use ConcurrentHashMap.

            ```java
            **Q3. inside code is not a question**
            ```

            ## 12. Related concepts
            Text.
            """;

    @Test
    void extractsQuestionsKeyPointsAndFollowUpsFromInterviewSectionOnly() {
        var items = LessonParser.extractInterview(BODY);
        assertThat(items).hasSize(2);

        var q1 = items.get(0);
        assertThat(q1.key()).isEqualTo("Q1");
        assertThat(q1.question()).isEqualTo("What is a HashMap?");
        assertThat(q1.answer()).startsWith("An array of buckets").doesNotContain("Key points").doesNotContain("Follow-up");
        assertThat(q1.keyPoints()).containsExactly("array of buckets", "hash spreading", "collisions chain then treeify", "resize doubles");
        assertThat(q1.followUps()).hasSize(1);
        assertThat(q1.followUps().get(0).question()).isEqualTo("What happens on resize?");
        assertThat(q1.followUps().get(0).answer()).startsWith("Entries split");

        var q2 = items.get(1);
        assertThat(q2.keyPoints()).isEmpty();
        assertThat(q2.answer()).contains("ConcurrentHashMap").contains("**Q3. inside code");
    }

    @Test
    void sectionHelperReturnsSummaryText() {
        String body = "## 12. Related\nx\n## 13. Summary and revision notes\n- one\n- two\n";
        assertThat(LessonParser.section(body, "summary")).isEqualTo("- one\n- two");
        assertThat(LessonParser.section(body, "missing")).isNull();
    }

    @Test
    void keyPointsAreDerivedWhenNotGiven() {
        assertThat(KeyPointsAccess.derive("Uses **buckets**, **hashing** and **equals** to find entries.")).containsExactly("buckets", "hashing", "equals");
        assertThat(KeyPointsAccess.derive("HashMap stores entries in an array of buckets. Collisions are chained in a linked list. Short."))
                .containsExactly("HashMap stores entries in an array of buckets.", "Collisions are chained in a linked list.");
    }
}
