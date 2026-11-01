package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

class LatestQuestionSeedTest {

    private static final String QUOTED = "'((?:[^']|'')*)'";
    private static final Pattern INSERT = Pattern.compile(
            "Insert into PROSTA_USER\\.QUESTION \\([^)]*\\) values \\((\\d+),(\\d+),(\\d+),"
                    + QUOTED + "," + QUOTED + "," + QUOTED + "," + QUOTED + ",(\\d+),"
                    + QUOTED + "\\);");
    private static final Set<Integer> CATEGORIES = Set.of(1, 2, 3, 4, 5);

    private record Seed(int id, int category, int difficulty, String text,
            List<String> options, int correct, String explanation, String insert) {
        String answer() {
            return options.get(correct - 1);
        }
    }

    private String sql() throws Exception {
        return Files.readString(Path.of("docs", "db", "現状最新.sql"), StandardCharsets.UTF_8);
    }

    private List<Seed> seeds() throws Exception {
        var matcher = INSERT.matcher(sql());
        List<Seed> rows = new ArrayList<>();
        while (matcher.find()) {
            rows.add(new Seed(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)), unquote(matcher.group(4)),
                    List.of(unquote(matcher.group(5)), unquote(matcher.group(6)), unquote(matcher.group(7))),
                    Integer.parseInt(matcher.group(8)), unquote(matcher.group(9)), matcher.group()));
        }
        return rows;
    }

    private String unquote(String value) {
        return value.replace("''", "'");
    }

    @Test
    void addsExactlyThreePerDifficultyAndUsesContiguousIds() throws Exception {
        List<Seed> rows = seeds();
        assertEquals(113, rows.size());
        assertEquals(rows.size(), Pattern.compile("Insert into PROSTA_USER\\.QUESTION ")
                .matcher(sql()).results().count());
        assertEquals(113, rows.stream().map(q -> q.id()).distinct().count());
        assertEquals(java.util.stream.IntStream.rangeClosed(1, 113).boxed().toList(),
                rows.stream().map(Seed::id).sorted().toList());
        assertEquals(68, rows.stream().filter(q -> q.id() <= 68).count());
        assertEquals(45, rows.stream().filter(q -> q.id() >= 69).count());
        for (int category : CATEGORIES) {
            for (int difficulty = 1; difficulty <= 3; difficulty++) {
                int level = difficulty;
                assertEquals(3, rows.stream().filter(q -> q.id() >= 69
                        && q.category() == category && q.difficulty() == level).count());
            }
        }
        var sequence = Pattern.compile(
                "CREATE SEQUENCE\\s+\"PROSTA_USER\"\\.\"SEQ_QUESTION\"[^;]*START WITH (\\d+)").matcher(sql());
        assertTrue(sequence.find());
        assertEquals(rows.stream().mapToInt(q -> q.id()).max().orElseThrow() + 1,
                Integer.parseInt(sequence.group(1)));
    }

    @TestFactory
    Stream<DynamicTest> everySeedHasValidOptionsAndExplanation() throws Exception {
        return seeds().stream().map(q -> DynamicTest.dynamicTest("Question " + q.id(), () -> {
            assertTrue(CATEGORIES.contains(q.category()));
            assertTrue(q.difficulty() >= 1 && q.difficulty() <= 3);
            assertTrue(q.correct() >= 1 && q.correct() <= 3);
            assertFalse(q.text().isBlank());
            assertFalse(q.explanation().isBlank());
            assertTrue(q.text().codePointCount(0, q.text().length()) <= 1000);
            assertTrue(q.explanation().codePointCount(0, q.explanation().length()) <= 1000);
            assertEquals(3, new HashSet<>(q.options()).size(), "Duplicate choices");
            for (String option : q.options()) {
                assertFalse(option.isBlank());
                assertTrue(option.codePointCount(0, option.length()) <= 300);
            }
            assertFalse(q.text().contains("\uFFFD"));
            assertFalse(q.explanation().contains("\uFFFD"));
        }));
    }

    @Test
    void reviewListsEveryQuestionWithMatchingDifficultyAndAnswer() throws Exception {
        String review = Files.readString(Path.of("docs", "db", "QUESTION_CONTENT_REVIEW.md"),
                StandardCharsets.UTF_8);
        var matcher = Pattern.compile("\\| (\\d+) \\| ([123]) \\| ([ABC]) \\|").matcher(review);
        Map<Integer, List<Integer>> reviewed = new HashMap<>();
        while (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            assertFalse(reviewed.containsKey(id), "Duplicate review entry " + id);
            reviewed.put(id, List.of(Integer.parseInt(matcher.group(2)),
                    matcher.group(3).charAt(0) - 'A' + 1));
        }
        assertEquals(113, reviewed.size());
        for (Seed q : seeds()) {
            assertEquals(List.of(q.difficulty(), q.correct()), reviewed.get(q.id()), "Question " + q.id());
        }
    }

    @Test
    void allLiteralInsertsLoadWithConstraints() throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:h2:mem:question_seed;MODE=Oracle")) {
            var stmt = db.createStatement();
            stmt.execute("CREATE SCHEMA PROSTA_USER");
            stmt.execute("CREATE TABLE PROSTA_USER.CATEGORY (CATEGORY_ID INT PRIMARY KEY)");
            for (int category : CATEGORIES) {
                stmt.execute("INSERT INTO PROSTA_USER.CATEGORY VALUES (" + category + ")");
            }
            stmt.execute("CREATE TABLE PROSTA_USER.QUESTION (QUESTION_ID INT PRIMARY KEY,"
                    + " CATEGORY_ID INT REFERENCES PROSTA_USER.CATEGORY,"
                    + " DIFFICULTY INT CHECK (DIFFICULTY BETWEEN 1 AND 3),"
                    + " QUESTION_TEXT VARCHAR(1000) NOT NULL, OPTION_A VARCHAR(300) NOT NULL,"
                    + " OPTION_B VARCHAR(300) NOT NULL, OPTION_C VARCHAR(300) NOT NULL,"
                    + " CORRECT_OPTION INT CHECK (CORRECT_OPTION BETWEEN 1 AND 3),"
                    + " EXPLANATION VARCHAR(1000) NOT NULL)");
            for (Seed q : seeds()) {
                stmt.execute(q.insert());
            }
            var result = stmt.executeQuery("SELECT COUNT(*) FROM PROSTA_USER.QUESTION");
            assertTrue(result.next());
            assertEquals(113, result.getInt(1));
        }
    }

    @Test
    void repairedSqlAnswersCoverNullsEscapesDatesAndHaving() throws Exception {
        Map<Integer, Seed> byId = new HashMap<>();
        seeds().forEach(q -> byId.put(q.id(), q));
        try (Connection db = DriverManager.getConnection("jdbc:h2:mem:question_examples;MODE=Oracle")) {
            var stmt = db.createStatement();
            stmt.execute("CREATE TABLE answer(user_id INT, is_correct INT)");
            stmt.execute("INSERT INTO answer VALUES (1,1),(1,1),(1,0),(2,1),(NULL,1),(3,0)");
            var correctUsers = stmt.executeQuery(byId.get(34).answer());
            assertTrue(correctUsers.next());
            assertEquals(1, correctUsers.getInt(1));
            assertFalse(correctUsers.next());
            var count = stmt.executeQuery(byId.get(50).text()
                    .substring(byId.get(50).text().indexOf("SELECT"), byId.get(50).text().indexOf(';') + 1));
            assertTrue(count.next());
            assertEquals(6, count.getInt(1));
            assertFalse(count.next());
            stmt.execute("DELETE FROM answer WHERE user_id IS NULL");
            var none = stmt.executeQuery("SELECT COUNT(*) FROM answer HAVING COUNT(*) > 5");
            assertFalse(none.next());
            stmt.execute("CREATE TABLE question(question_text VARCHAR(100))");
            stmt.execute("INSERT INTO question VALUES ('50%です'),('%先頭'),('末尾%'),('%'),('percent'),('全角％')");
            var escaped = stmt.executeQuery(byId.get(40).answer());
            int matches = 0;
            while (escaped.next()) {
                assertTrue(escaped.getString(1).contains("%"));
                matches++;
            }
            assertEquals(4, matches);
            stmt.execute("CREATE TABLE customer(user_id INT, birthday TIMESTAMP)");
            stmt.execute("INSERT INTO customer VALUES (1,TIMESTAMP '2000-01-01 00:00:00'),"
                    + "(2,TIMESTAMP '2000-12-31 23:59:59'),(3,TIMESTAMP '2001-01-01 00:00:00'),"
                    + "(4,TIMESTAMP '1999-12-31 23:59:59')");
            var dates = stmt.executeQuery(byId.get(44).answer());
            Set<Integer> ids = new HashSet<>();
            while (dates.next()) {
                ids.add(dates.getInt(1));
            }
            assertEquals(Set.of(1, 2), ids);
            var nullNotIn = stmt.executeQuery("SELECT user_id FROM customer WHERE user_id NOT IN "
                    + "(SELECT user_id FROM answer UNION ALL SELECT CAST(NULL AS INT))");
            assertFalse(nullNotIn.next());
        }
    }

    @Test
    void javaOutputQuestionsCompileAndMatchSelectedAnswer(@TempDir Path directory) throws Exception {
        Map<Integer, String> expected = Map.of(4, "7\n5", 5, "30", 7, "13", 8, "3",
                72, "3", 73, "true", 74, "12", 75, "no", 76, "A\nB\nC");
        for (Seed q : seeds()) {
            if (!expected.containsKey(q.id())) {
                continue;
            }
            String className = "SeedExample" + q.id();
            String code = q.text().substring(q.text().indexOf('\n') + 1);
            Path source = directory.resolve(className + ".java");
            Files.writeString(source, "public class " + className
                    + " { public static void main(String[] args) { " + code + " } }", StandardCharsets.UTF_8);
            assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                    "-encoding", "UTF-8", "-d", directory.toString(), source.toString()));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PrintStream original = System.out;
            try (var loader = new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()});
                    var capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
                try {
                    System.setOut(capture);
                    loader.loadClass(className).getMethod("main", String[].class)
                            .invoke(null, (Object) new String[0]);
                } finally {
                    System.setOut(original);
                }
            }
            String actual = output.toString(StandardCharsets.UTF_8).replace("\r\n", "\n").strip();
            assertEquals(expected.get(q.id()), actual, "Question " + q.id());
            if (q.id() == 4) {
                assertEquals("7 と 5", q.answer());
            } else if (q.id() == 76) {
                assertEquals(actual.replace("\n", "、"), q.answer());
            } else {
                assertTrue(q.answer().contains(actual), "Selected choice must match actual output");
            }
        }
    }

    @Test
    void binarySearchAndAvailabilityAnswersHaveExactValues() throws Exception {
        int max = 0;
        for (int target = -1; target <= 1024; target++) {
            int low = 0;
            int high = 1023;
            int comparisons = 0;
            while (low <= high) {
                int mid = (low + high) / 2;
                comparisons++;
                if (mid == target) {
                    break;
                } else if (mid < target) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
            max = Math.max(max, comparisons);
        }
        Map<Integer, Seed> byId = new HashMap<>();
        seeds().forEach(q -> byId.put(q.id(), q));
        assertEquals(11, max);
        assertEquals(max + "回", byId.get(59).answer());
        assertEquals("99%", byId.get(66).answer());
        assertEquals(0.99, 1 - (1 - 0.9) * (1 - 0.9), 0.000001);
        assertEquals("72%", byId.get(113).answer());
        assertEquals(0.72, 0.9 * 0.8, 0.000001);
        assertEquals("62個", byId.get(111).answer());
        assertEquals(62, (1 << (32 - 26)) - 2);
        assertEquals("80%", byId.get(110).answer());
        assertEquals(80, 100 * (15 + 9) / (20 + 10));
    }
}
