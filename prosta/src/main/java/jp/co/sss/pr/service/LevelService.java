package jp.co.sss.pr.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jp.co.sss.pr.entity.Achievement;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.UserAchievement;
import jp.co.sss.pr.repository.AchievementRepository;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.UserAchievementRepository;

/**
 * レベル・ログイン状態・実績を管理するサービス。
 *
 * <p>使用テーブル（既存テーブルのみ）：
 * <ul>
 *   <li>customer : ログイン情報、total_exp、user_level、perfect_test_count</li>
 *   <li>answer   : correct_flag, ans_date</li>
 *   <li>achievement / user_achievement : 実績マスター・解除記録</li>
 * </ul>
 *
 * <p>レベル計算式：
 * <pre>
 *   獲得EXP = 今回の正解数×2 + 満点時のみ5
 *   レベル = floor(累計EXP / 15) + 1
 *   ※ ログインではEXPを加算しない。ログイン日数は実績判定に使用する。
 * </pre>
 */
@Service
public class LevelService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    // ----------------------------------------------------------------
    // ログイン時処理
    // ----------------------------------------------------------------

    /**
     * ログイン時に customer テーブルのログイン情報を更新し、レベルを再計算する。
     *
     * <p>処理内容：
     * <ul>
     *   <li>同日2回目以降のログインはログイン日数を増やさない</li>
     *   <li>連続日数を前回ログイン日から計算</li>
     *   <li>login_count と login_date を更新</li>
     *   <li>login_flag を true に設定</li>
     *   <li>last_seen_at を現在時刻に更新</li>
     *   <li>レベルを再計算して user_level に保存</li>
     * </ul>
     *
     * @param customer ログインユーザーEntity
     * @return 今回新しく解除された実績リスト
     */
    @Transactional
    public List<Achievement> processLogin(Customer customer) {
        customer = lockCurrentCustomer(customer);
        LocalDate today = LocalDate.now();
        LocalDate lastLoginDate = customer.getLoginDate();
        customer.setLastSeenAt(LocalDateTime.now());
        customer.setLoginFlag(true);

        if (today.equals(lastLoginDate)) {
            customerRepository.save(customer);
            return new ArrayList<>();
        }

        customer.setConsecutiveLoginDays(
                today.minusDays(1).equals(lastLoginDate)
                        ? customer.getConsecutiveLoginDays() + 1
                        : 1);
        customer.setLoginCount(customer.getLoginCount() + 1);
        customer.setLoginDate(today);

        // レベル再計算
        recalcLevel(customer);
        customerRepository.save(customer);

        return checkAchievements(customer);
    }

    // ----------------------------------------------------------------
    // テスト結果時処理
    // ----------------------------------------------------------------

    /**
     * テスト解答結果を受けてレベルを再計算し、実績を確認する。
     *
     * <p>今回の正解数から累計EXPへ加算し、満点回数をテスト単位で更新する。
     * answerの累計正解数は実績判定に使用し、過去の履歴からEXPを再生成しない。
     *
     * @param customer     解答したユーザーEntity
     * @param correctCount 今回の正解数
     * @param totalCount   今回の総問題数
     * @return 今回新しく解除された実績リスト
     */
    @Transactional
    public List<Achievement> processTestResult(
            Customer customer, int correctCount, int totalCount) {
        customer = lockCurrentCustomer(customer);

        // 1回のテストで満点なら、満点回数を1増やす
        if (totalCount > 0 && correctCount == totalCount) {
            customer.setPerfectTestCount(
                    customer.getPerfectTestCount() + 1);
        }

     // 正解1問につき2 EXP
        long earnedExp = correctCount * 2L;

        // 満点の場合は追加で5 EXP
        if (totalCount > 0 && correctCount == totalCount) {
            earnedExp += 5L;
        }

        // 累計EXPに加算
        customer.setTotalExp(customer.getTotalExp() + earnedExp);

        // 新しいEXPからレベルを計算
        recalcLevel(customer);
        customerRepository.save(customer);
        
        // 通常の実績をチェック
        List<Achievement> newlyUnlocked = checkAchievements(customer);

        // 実績ID 15：1回のテストで0問正解
        if (totalCount > 0 && correctCount == 0) {
            tryUnlock(customer, 15, newlyUnlocked);
        }

        return newlyUnlocked;
    }

    private Customer lockCurrentCustomer(Customer customer) {
        if (entityManager.contains(customer)) {
            // 別要求の確定前に読んだEntityでも、加算前にロック下の最新値へ更新する。
            entityManager.refresh(customer, LockModeType.PESSIMISTIC_WRITE);
            return customer;
        }
        Customer current = entityManager.find(Customer.class, customer.getUserId(), LockModeType.PESSIMISTIC_WRITE);
        if (current == null) {
            throw new jakarta.persistence.EntityNotFoundException("Customer no longer exists");
        }
        return current;
    }

    // ----------------------------------------------------------------
    // レベル計算
    // ----------------------------------------------------------------

    private void recalcLevel(Customer customer) {
        long totalExp = customer.getTotalExp();

        // 15 EXPごとに1レベルアップ
        int level = (int) (totalExp / 15L) + 1;

        customer.setUserLevel(level);
    }

    /**
     * 現在のレベルを返す静的ユーティリティ。
     */
    public static int getLevel(Customer customer) {
        return customer.getUserLevel();
    }

    /**
     * 旧画面向けの互換メソッド。現在のEXP進捗の計算には使用しない。
     * 次レベルまでのEXPは、累計EXPを使って 15 - (totalExp % 15) で求める。
     */
    public static int pointsToNextLevel(int currentLevel) {
        return 1;
    }

    // ----------------------------------------------------------------
    // 実績チェック
    // ----------------------------------------------------------------

    /**
     * customer の現在値・answer 集計値に基づいて未解除実績を確認し、
     * 新規解除分を DB に保存して返す。
     */
    private List<Achievement> checkAchievements(Customer customer) {
        List<Achievement> newlyUnlocked = new ArrayList<>();

        int consecutiveLoginDays = customer.getConsecutiveLoginDays();
        int loginCount = customer.getLoginCount();
        int level = customer.getUserLevel();

        // テスト単位で集計した累計満点回数
        int perfectTestCount = customer.getPerfectTestCount();

        // 累計正解数
        int totalCorrect =
                answerRepository.countByCustomerAndCorrectFlagTrue(customer);

        // ID 1: 連続7日ログイン
        if (consecutiveLoginDays >= 7) {
            tryUnlock(customer, 1, newlyUnlocked);
        }

        // ID 2: 満点1回
        if (perfectTestCount >= 1) {
            tryUnlock(customer, 2, newlyUnlocked);
        }

        // ID 3: 満点3回
        if (perfectTestCount >= 3) {
            tryUnlock(customer, 3, newlyUnlocked);
        }

        // ID 4: 満点7回
        if (perfectTestCount >= 7) {
            tryUnlock(customer, 4, newlyUnlocked);
        }

        // ID 5: レベル10
        if (level >= 10) {
            tryUnlock(customer, 5, newlyUnlocked);
        }

        // ID 6: レベル50
        if (level >= 50) {
            tryUnlock(customer, 6, newlyUnlocked);
        }

        // ID 7: レベル25
        if (level >= 25) {
            tryUnlock(customer, 7, newlyUnlocked);
        }

        // ID 8: レベル75
        if (level >= 75) {
            tryUnlock(customer, 8, newlyUnlocked);
        }

        // ID 9: レベル100
        if (level >= 100) {
            tryUnlock(customer, 9, newlyUnlocked);
        }

        // ID 10: 正解100問
        if (totalCorrect >= 100) {
            tryUnlock(customer, 10, newlyUnlocked);
        }

        // ID 11: 正解500問
        if (totalCorrect >= 500) {
            tryUnlock(customer, 11, newlyUnlocked);
        }

        // ID 12: 満点10回
        if (perfectTestCount >= 10) {
            tryUnlock(customer, 12, newlyUnlocked);
        }

        // ID 13: 30日連続ログイン
        if (consecutiveLoginDays >= 30) {
            tryUnlock(customer, 13, newlyUnlocked);
        }

        // ID 14: 累計ログイン100日
        if (loginCount >= 100) {
            tryUnlock(customer, 14, newlyUnlocked);
        }

        return newlyUnlocked;
    }

    /**
     * 指定実績がまだ解除されていなければ解除して newlyUnlocked に追加する。
     */
    private void tryUnlock(Customer customer, int achievementId, List<Achievement> newlyUnlocked) {
        // 遅延プロキシだと画面描画時に LazyInitializationException になるため実体を取得する
        Achievement ach = achievementRepository.findById(achievementId).orElse(null);
        if (ach == null) {
            return;
        }
        if (!userAchievementRepository.existsByCustomerAndAchievement(customer, ach)) {
            UserAchievement ua = new UserAchievement();
            ua.setCustomer(customer);
            ua.setAchievement(ach);
            ua.setUnlockedDate(LocalDate.now());
            userAchievementRepository.save(ua);
            newlyUnlocked.add(ach);
        }
    }

    // ----------------------------------------------------------------
    // ユーティリティ
    // ----------------------------------------------------------------

    /**
     * 満点回数を返す（AchievementController などの表示用）。
     */
    public int getPerfectCount(Customer customer) {
        return customer.getPerfectTestCount();
    }
    /**
     * 総正解数を返す（表示用）。
     */
    public int getTotalCorrect(Customer customer) {
        return answerRepository.countByCustomerAndCorrectFlagTrue(customer);
    }
}
