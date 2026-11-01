package jp.co.sss.pr.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class LatestDatabaseSeedTest {

    private static final Pattern INSERT = Pattern.compile(
            "Insert into PROSTA_USER\\.(\\w+) \\(([^)]*)\\) values "
                    + "\\(((?:[^']|'(?:[^']|'')*')*?)\\);");
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "CREATE TABLE\\s+(?:\"PROSTA_USER\"\\.)?\"?(\\w+)\"?\\s*\\(",
            Pattern.CASE_INSENSITIVE);
    private static final Map<String, Integer> COUNTS = Map.of(
            "ACHIEVEMENT", 20, "ANSWER", 189, "CATEGORY", 5, "CUSTOMER", 6,
            "FAVORITE", 2, "FRIEND_CONNECTION", 3, "QUESTION", 113, "USER_ACHIEVEMENT", 14);
    private static final Map<String, String> SEQUENCE_IDS = Map.ofEntries(
            Map.entry("SEQ_ANSWER", "ANSWER.ANS_ID"),
            Map.entry("SEQ_CATEGORY", "CATEGORY.CATEGORY_ID"),
            Map.entry("SEQ_FAVORITE", "FAVORITE.FAVORITE_ID"),
            Map.entry("SEQ_FRIEND_CONNECTION", "FRIEND_CONNECTION.FRIEND_CONNECTION_ID"),
            Map.entry("SEQ_QUESTION", "QUESTION.QUESTION_ID"),
            Map.entry("SEQ_STUDY_PLAN", "STUDY_PLAN.STUDY_PLAN_ID"),
            Map.entry("SEQ_USER", "CUSTOMER.USER_ID"),
            Map.entry("SEQ_USER_ACHIEVEMENT", "USER_ACHIEVEMENT.USER_ACH_ID"),
            Map.entry("SEQ_QUESTION_NOTE", "QUESTION_NOTE.NOTE_ID"),
            Map.entry("SEQ_QUESTION_REPORT", "QUESTION_REPORT.REPORT_ID"),
            Map.entry("SEQ_FRIEND_CHALLENGE", "FRIEND_CHALLENGE.CHALLENGE_ID"),
            Map.entry("SEQ_FRIEND_CHALLENGE_QUESTION", "FRIEND_CHALLENGE_QUESTION.CHALLENGE_QUESTION_ID"),
            Map.entry("SEQ_FRIEND_CHALLENGE_PARTICIPANT", "FRIEND_CHALLENGE_PARTICIPANT.PARTICIPANT_ID"),
            Map.entry("SEQ_FRIEND_CHALLENGE_ANSWER", "FRIEND_CHALLENGE_ANSWER.CHALLENGE_ANSWER_ID"));

    private String sql() throws Exception {
        return Files.readString(Path.of("docs", "db", "現状最新.sql"), StandardCharsets.UTF_8);
    }

    private List<String> split(String text) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\'') {
                if (quoted && i + 1 < text.length() && text.charAt(i + 1) == '\'') {
                    i++;
                    continue;
                }
                quoted = !quoted;
            }
            if (!quoted) {
                if (ch == '(') depth++;
                if (ch == ')') depth--;
                if (ch == ',' && depth == 0) {
                    result.add(text.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }
        assertFalse(quoted);
        assertEquals(0, depth);
        result.add(text.substring(start).trim());
        return result;
    }

    private String portable(String text) {
        return text.replace("\"PROSTA_USER\".", "").replace("PROSTA_USER.", "")
                .replaceAll("(?i)\\s+ENABLE\\b", "")
                .replaceAll("(?is)\\s+USING INDEX.*", "");
    }

    @Test
    void loadsMergedRowsWithAllLogicalConstraintsAndRejectsInvalidData() throws Exception {
        String sql = sql().replaceAll("(?m)^\\s*--[^\\r\\n]*", "");
        List<String> constraints = new ArrayList<>();
        List<String> tables = new ArrayList<>();
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:merged_" + UUID.randomUUID() + ";MODE=Oracle", "sa", "");
                var statement = connection.createStatement()) {
            var creates = CREATE_TABLE.matcher(sql);
            while (creates.find()) {
                String table = creates.group(1).toUpperCase(Locale.ROOT);
                assertFalse(tables.contains(table), "Duplicate table " + table);
                tables.add(table);
                int start = creates.end();
                int end = start;
                int depth = 1;
                while (depth > 0 && end < sql.length()) {
                    char ch = sql.charAt(end++);
                    if (ch == '(') depth++;
                    if (ch == ')') depth--;
                }
                assertEquals(0, depth);
                List<String> columns = new ArrayList<>();
                for (String part : split(sql.substring(start, end - 1))) {
                    if (part.stripLeading().toUpperCase(Locale.ROOT).startsWith("CONSTRAINT ")) {
                        constraints.add("ALTER TABLE " + table + " ADD " + portable(part));
                    } else {
                        columns.add(part);
                    }
                }
                statement.execute("CREATE TABLE " + table + " (" + String.join(",", columns) + ")");
            }
            assertEquals(15, tables.size());
            var adds = Pattern.compile("(?is)ALTER TABLE\\s+(?:\"PROSTA_USER\"\\.)?\"?\\w+\"?"
                    + "\\s+ADD\\s*\\([\\s\\S]*?\\);").matcher(sql);
            while (adds.find()) statement.execute(portable(adds.group()));
            var inserts = INSERT.matcher(sql);
            Map<String, Integer> counts = new HashMap<>();
            while (inserts.find()) {
                assertEquals(split(inserts.group(2)).size(), split(inserts.group(3)).size());
                statement.execute(portable(inserts.group()));
                counts.merge(inserts.group(1), 1, (left, right) -> left.intValue() + right.intValue());
            }
            assertEquals(COUNTS, counts);
            var alters = Pattern.compile("(?is)ALTER TABLE\\s+(?:\"PROSTA_USER\"\\.)?\"?\\w+\"?"
                    + "\\s+ADD CONSTRAINT\\s+[^;]+;").matcher(sql);
            while (alters.find()) constraints.add(portable(alters.group()));
            for (String constraint : constraints) {
                // Oracle makes primary-key columns NOT NULL; H2 requires that step explicitly.
                var primaryKey = Pattern.compile("(?is)ALTER TABLE\\s+\"?(\\w+)\"?.*"
                        + "PRIMARY KEY\\s*\\(([^)]+)\\)").matcher(constraint);
                if (primaryKey.find()) {
                    for (String column : split(primaryKey.group(2))) {
                        statement.execute("ALTER TABLE " + primaryKey.group(1)
                                + " ALTER COLUMN " + column + " SET NOT NULL");
                    }
                }
                if (!constraint.toUpperCase(Locale.ROOT).contains("FOREIGN KEY")) statement.execute(constraint);
            }
            for (String constraint : constraints) {
                if (constraint.toUpperCase(Locale.ROOT).contains("FOREIGN KEY")) statement.execute(constraint);
            }
            var notNulls = Pattern.compile("(?i)ALTER TABLE\\s+\"PROSTA_USER\"\\.\"(\\w+)\""
                    + "\\s+MODIFY\\s*\\(\"(\\w+)\" NOT NULL ENABLE\\);").matcher(sql);
            while (notNulls.find()) {
                statement.execute("ALTER TABLE " + notNulls.group(1)
                        + " ALTER COLUMN " + notNulls.group(2) + " SET NOT NULL");
            }
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM CUSTOMER"
                    + " WHERE LOGIN_FLAG = 0 AND ACTIVE_LOGIN_TOKEN IS NULL")) {
                assertTrue(result.next());
                assertEquals(6, result.getInt(1));
            }
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM ANSWER a JOIN QUESTION q"
                    + " ON q.QUESTION_ID = a.QUESTION_ID WHERE a.CORRECT_OPTION <> q.CORRECT_OPTION")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1), "Historical correct options must remain unchanged");
            }
            for (var entry : SEQUENCE_IDS.entrySet()) {
                String[] target = entry.getValue().split("\\.");
                var sequence = Pattern.compile("(?i)CREATE SEQUENCE\\s+(?:\"PROSTA_USER\"\\.)?\"?"
                        + entry.getKey() + "\"?\\s+[^;]*?START WITH (\\d+)").matcher(sql);
                assertTrue(sequence.find(), entry.getKey());
                long start = Long.parseLong(sequence.group(1));
                assertFalse(sequence.find(), "Duplicate sequence " + entry.getKey());
                try (var result = statement.executeQuery("SELECT COALESCE(MAX(" + target[1]
                        + "), 0) FROM " + target[0])) {
                    assertTrue(result.next());
                    assertEquals(result.getLong(1) + 1, start, entry.getKey() + " must follow inserted IDs");
                }
            }
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO FAVORITE (FAVORITE_ID,USER_ID,QUESTION_ID)"
                            + " SELECT 999,USER_ID,QUESTION_ID FROM FAVORITE WHERE FAVORITE_ID=1"));
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO FAVORITE (FAVORITE_ID,USER_ID,QUESTION_ID) VALUES (999,99999,1)"));
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO FAVORITE (FAVORITE_ID,USER_ID,QUESTION_ID) VALUES (999,3,9999)"));
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO STUDY_PLAN (STUDY_PLAN_ID,USER_ID,PLAN_DATE,TITLE,TARGET_QUESTION_COUNT)"
                            + " VALUES (999,3,CURRENT_DATE,'invalid',7)"));
        }
    }

    @Test
    void preservesRebuildGuardAndUniqueSequenceDefinitions() throws Exception {
        String sql = sql();
        int guard = sql.indexOf("IF USER <> 'PROSTA_USER' THEN");
        assertTrue(guard >= 0);
        assertTrue(guard < sql.indexOf("FROM user_tables"));
        assertTrue(sql.contains("FROM user_sequences"));
        assertEquals(15, Pattern.compile("(?i)CREATE SEQUENCE\\s+").matcher(sql).results().count());
        assertTrue(sql.contains("\"SEQ_OPTION\""));
    }

    @Test
    void preservesExistingIconsAndKeepsPasswordsHashed() throws Exception {
        List<String> expectedIcons = List.of(
                "\uD83D\uDCC5", "\uD83C\uDFAF", "\uD83E\uDD49", "\uD83C\uDFC6", "\uD83C\uDF31",
                "\u2B50", "\uD83D\uDEE1", "\uD83D\uDCAB", "\uD83D\uDC51", "\uD83D\uDCD8",
                "\uD83D\uDCDA", "\uD83D\uDC8E", "\uD83D\uDD25", "\uD83C\uDF93", "\uD83C\uDF31",
                "\uD83E\uDD1D", "\uD83D\uDC65", "\uD83C\uDF1F", "\uD83D\uDC51", "\uD83D\uDCDA");
        var matcher = INSERT.matcher(sql());
        int icons = 0;
        int customers = 0;
        while (matcher.find()) {
            List<String> columns = split(matcher.group(2));
            List<String> values = split(matcher.group(3));
            if (matcher.group(1).equals("ACHIEVEMENT")) {
                String icon = values.get(columns.indexOf("ICON"));
                assertEquals("'" + expectedIcons.get(Integer.parseInt(values.get(0)) - 1) + "'", icon);
                assertFalse(icon.contains("\uFFFD"));
                icons++;
            }
            if (matcher.group(1).equals("CUSTOMER")) {
                assertTrue(values.get(columns.indexOf("USER_PASS")).matches("'\\$2[aby]\\$\\d{2}\\$[^']{53}'"),
                        "Passwords must remain BCrypt hashes");
                assertEquals("0", values.get(columns.indexOf("LOGIN_FLAG")));
                assertEquals("null", values.get(columns.indexOf("ACTIVE_LOGIN_TOKEN")));
                customers++;
            }
        }
        assertEquals(20, icons);
        assertEquals(6, customers);
    }

    @Test
    void utf8PreservesSeedlingButMs932CannotEncodeIt() throws Exception {
        String icon = "\uD83C\uDF31";
        assertEquals(icon, new String(icon.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        assertFalse(Charset.forName("MS932").newEncoder().canEncode(icon));
        assertTrue(sql().contains("'" + icon + "'"));
    }

    @Test
    void levelAchievementIdsAndHistoryStayConsistent() throws Exception {
        var matcher = INSERT.matcher(sql());
        Map<Integer, String> names = new HashMap<>();
        Map<Integer, List<String>> history = new HashMap<>();
        while (matcher.find()) {
            List<String> values = split(matcher.group(3));
            if (matcher.group(1).equals("ACHIEVEMENT")) {
                names.put(Integer.parseInt(values.get(0)), values.get(1));
            } else if (matcher.group(1).equals("USER_ACHIEVEMENT")) {
                history.put(Integer.parseInt(values.get(0)), values);
            }
        }
        assertEquals("'レベル25到達'", names.get(6));
        assertEquals("'レベル50到達'", names.get(7));
        assertEquals("7", history.get(2).get(2));
        assertEquals("to_date('26-10-23','RR-MM-DD')", history.get(2).get(3));
        assertEquals("6", history.get(3).get(2));
        assertEquals("to_date('26-10-30','RR-MM-DD')", history.get(3).get(3));
    }
}
