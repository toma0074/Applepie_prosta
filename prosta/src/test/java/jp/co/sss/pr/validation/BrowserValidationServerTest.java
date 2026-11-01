package jp.co.sss.pr.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.web.server.LocalServerPort;

import jp.co.sss.pr.entity.Achievement;
import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.UserAchievement;

/** Opt-in isolated browser fixture; never connects to the configured Oracle datasource. */
@EnabledIfSystemProperty(named = "browser.validation", matches = "true")
class BrowserValidationServerTest extends ValidationSupport {
    @LocalServerPort int port;

    @Test
    void serveIsolatedFixtureUntilExplicitStopOrTimeout() throws Exception {
        acceptedFriendship();
        Achievement a = new Achievement();
        a.setAchievementId(6);
        a.setAchievementName("レベル25到達");
        a.setDescription("レベル25に到達する");
        a.setIcon("⭐");
        a = achievements.saveAndFlush(a);
        UserAchievement ua = new UserAchievement();
        ua.setCustomer(friend);
        ua.setAchievement(a);
        ua.setUnlockedDate(LocalDate.now());
        unlocked.saveAndFlush(ua);
        if (Boolean.getBoolean("browser.achievements.all")) {
            seedAllAchievements();
        }
        for (var c : java.util.List.of(user, friend)) {
            for (int i = 0; i < 12; i++) {
                Answer answer = new Answer();
                answer.setCustomer(c);
                answer.setQuestion(question);
                answer.setAnsOption(i % 3 + 1);
                answer.setCorrectOption(1);
                answer.setCorrectFlag(i % 3 == 0);
                answer.setAnsDate(LocalDate.now().minusDays(i % 4));
                answer.setAnsCount(1);
                answers.saveAndFlush(answer);
            }
        }
        Path stop = Path.of(System.getProperty("browser.stop.file"));
        System.out.printf("BROWSER_READY port=%d user=%s friend=%s admin=%s category=%d question=%d%n",
                port, user.getUserName(), friend.getUserName(), admin.getUserName(),
                category.getCategoryId(), question.getQuestionId());
        Instant deadline = Instant.now().plus(Duration.ofMinutes(25));
        while (!Files.exists(stop) && Instant.now().isBefore(deadline)) {
            Thread.sleep(500);
        }
        if (!Files.exists(stop)) throw new AssertionError("Browser validation exceeded 25 minutes");
    }

    private void seedAllAchievements() throws Exception {
        String sql = Files.readString(Path.of("docs", "db", "現状最新.sql"),
                java.nio.charset.StandardCharsets.UTF_8);
        var rows = java.util.regex.Pattern.compile(
                "Insert into PROSTA_USER\\.ACHIEVEMENT \\([^)]*\\) values "
                        + "\\((\\d+),'((?:[^']|'')*)','((?:[^']|'')*)','((?:[^']|'')*)'\\);")
                .matcher(sql);
        int count = 0;
        while (rows.find()) {
            Achievement achievement = new Achievement();
            achievement.setAchievementId(Integer.parseInt(rows.group(1)));
            achievement.setAchievementName(rows.group(2).replace("''", "'"));
            achievement.setDescription(rows.group(3).replace("''", "'"));
            achievement.setIcon(rows.group(4).replace("''", "'"));
            achievement = achievements.saveAndFlush(achievement);
            UserAchievement record = new UserAchievement();
            record.setCustomer(user);
            record.setAchievement(achievement);
            record.setUnlockedDate(LocalDate.now());
            unlocked.saveAndFlush(record);
            count++;
        }
        org.junit.jupiter.api.Assertions.assertEquals(20, count);
    }
}
