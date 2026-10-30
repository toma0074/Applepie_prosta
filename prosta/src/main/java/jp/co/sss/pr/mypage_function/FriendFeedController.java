package jp.co.sss.pr.mypage_function;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.FriendConnection;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;
import jp.co.sss.pr.repository.UserAchievementRepository;

@RestController
public class FriendFeedController {
    private final CustomerRepository customers;
    private final FriendConnectionRepository connections;
    private final AnswerRepository answers;
    private final UserAchievementRepository achievements;

    public FriendFeedController(CustomerRepository customers, FriendConnectionRepository connections,
            AnswerRepository answers, UserAchievementRepository achievements) {
        this.customers = customers;
        this.connections = connections;
        this.answers = answers;
        this.achievements = achievements;
    }

    @GetMapping("/api/friends/{friendId}/feed")
    // 読み取り中の想定内4xxで、SingleLoginFilterの共有トランザクションをrollback-onlyにしない。
    // DB障害などResponseStatusException以外の実行時例外は、通常どおりロールバックする。
    @Transactional(readOnly = true, noRollbackFor = ResponseStatusException.class)
    public List<FeedEntry> feed(@PathVariable Integer friendId, HttpSession session) {
        if (!(session.getAttribute("user") instanceof CustomerBean bean)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ログインしてください");
        }
        Customer user = customers.findById(bean.getUserId())
                .filter(c -> !Boolean.TRUE.equals(c.getDeleteFlag()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ログインしてください"));
        if (!Integer.valueOf(0).equals(user.getPermission())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "利用できません");
        }
        Customer friend = customers.findById(friendId)
                .filter(c -> !Boolean.TRUE.equals(c.getDeleteFlag()) && Integer.valueOf(0).equals(c.getPermission()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "友達が見つかりません"));
        // 接続はIDの小さい側/大きい側で一意に保存する。相手の存在だけで活動を公開しない。
        Customer low = user.getUserId() < friendId ? user : friend;
        Customer high = user.getUserId() < friendId ? friend : user;
        connections.findByUserLowAndUserHigh(low, high)
                .filter(c -> FriendConnection.ACCEPTED.equals(c.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "友達が見つかりません"));

        LocalDate to = LocalDate.now();
        // 今日を含む30日。日時が記録されていないレベルアップ等は推定せず、保存済み活動だけを返す。
        LocalDate from = to.minusDays(29);
        List<FeedEntry> entries = new ArrayList<>();
        answers.summarizeDailyStudy(friend, from, to).forEach(day -> entries.add(new FeedEntry(
                day.getStudyDate(), "学習", day.getAnswerCount() + "問に解答・"
                        + day.getCorrectCount() + "問正解しました。")));
        achievements.findByCustomerAndUnlockedDateBetween(friend, from, to).forEach(unlocked ->
                entries.add(new FeedEntry(unlocked.getUnlockedDate(), "実績",
                        unlocked.getAchievement().getIcon() + " 「"
                                + unlocked.getAchievement().getAchievementName() + "」を獲得しました。")));
        return entries.stream()
                .sorted(Comparator.comparing((FeedEntry entry) -> entry.date()).reversed()
                        .thenComparing(entry -> entry.type()).thenComparing(entry -> entry.message()))
                .toList();
    }

    public record FeedEntry(@JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate date,
            String type, String message) { }
}
