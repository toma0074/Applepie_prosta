package jp.co.sss.pr.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 *   <li>customer : login_date, login_count, login_flag, user_level</li>
 *   <li>answer   : correct_flag, ans_date</li>
 *   <li>achievement / user_achievement : 実績マスター・解除記録</li>
 * </ul>
 *
 * <p>レベル計算式：
 * <pre>
 *   レベル = ログイン日数×1 + 連続ログインボーナス + 正解数×2 + 満点ボーナス×5
 *   ※ 計算後の値を customer.user_level に直接保存する
 * </pre>
 */
@Service
public class LevelService {

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
     *   <li>同日2回目以降のログインはスキップ（login_flag=true の場合）</li>
     *   <li>login_count をインクリメント</li>
     *   <li>login_date を今日に更新</li>
     *   <li>login_flag を true に設定</li>
     *   <li>レベルを再計算して user_level に保存</li>
     * </ul>
     *
     * @param customer ログインユーザーEntity
     * @return 今回新しく解除された実績リスト
     */
    @Transactional
    public List<Achievement> processLogin(Customer customer) {
        LocalDate today = LocalDate.now();

        // 同日の重複ログインはスキップ
        if (Boolean.TRUE.equals(customer.getLoginFlag())
                && today.equals(customer.getLoginDate())) {
            return new ArrayList<>();
        }

        // 日付が変わったら login_flag をリセット
        if (!today.equals(customer.getLoginDate())) {
            customer.setLoginFlag(false);
        }

        // 初回ログイン or 日付変わり
        if (!Boolean.TRUE.equals(customer.getLoginFlag())) {
            customer.setLoginCount(customer.getLoginCount() + 1);
            customer.setLoginDate(today);
            customer.setLoginFlag(true);
        }

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
     * <p>レベル計算は answer テーブルの集計値（正解数・満点回数）を参照する。
     *
     * @param customer     解答したユーザーEntity
     * @param correctCount 今回の正解数
     * @param totalCount   今回の総問題数
     * @return 今回新しく解除された実績リスト
     */
    @Transactional
    public List<Achievement> processTestResult(Customer customer, int correctCount, int totalCount) {
        // レベル再計算（answerテーブルの全履歴を参照）
        recalcLevel(customer);
        customerRepository.save(customer);

        return checkAchievements(customer);
    }

    // ----------------------------------------------------------------
    // レベル計算
    // ----------------------------------------------------------------

    /**
     * customer.user_level を再計算して customer オブジェクトに反映する（DBへの保存は呼び出し元が行う）。
     *
     * <p>レベル = ログイン日数 + 連続ログインボーナス + (総正解数 × 2) + (満点回数 × 5)
     * <ul>
     *   <li>ログイン日数 : customer.login_count（1日1カウント）</li>
     *   <li>連続ログインボーナス : login_count が 7の倍数のとき +10</li>
     *   <li>総正解数   : answer テーブルの correct_flag=true の件数</li>
     *   <li>満点回数   : 同一 ans_date に全問正解した日の回数</li>
     * </ul>
     * 最小値は 1。
     */
    private void recalcLevel(Customer customer) {
        int loginCount = customer.getLoginCount();

        // 連続ログインボーナス（7の倍数ごとに+10）
        int streakBonus = (loginCount / 7) * 10;

        // 正解数（answer.correct_flag = true の件数）
        int totalCorrect = answerRepository.countByCustomerAndCorrectFlagTrue(customer);

        // 満点回数（1日内で全問正解した回数）
        int perfectDays = answerRepository.countPerfectDaysByCustomer(customer);

        int level = loginCount + streakBonus + (totalCorrect * 2) + (perfectDays * 5);
        level = Math.max(1, level);

        customer.setUserLevel(level);
    }

    /**
     * 現在のレベルを返す静的ユーティリティ。
     */
    public static int getLevel(Customer customer) {
        return customer.getUserLevel();
    }

    /**
     * 次のレベルまでの残りポイント（表示用）。
     * 「あと何点で次のレベルか」を計算する。
     * ログイン日数 1 = 1レベル換算で計算。
     */
    public static int pointsToNextLevel(int currentLevel) {
        // 次のレベルに必要な追加ポイント（ログイン換算）
        return 1; // レベルは1ログインまたは1正解ごとに上がる設計のため常に1
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

        int loginCount  = customer.getLoginCount();
        int level       = customer.getUserLevel();
        int perfectDays = answerRepository.countPerfectDaysByCustomer(customer);

        // 実績ID 1: 連続7日ログイン（7日以上ログイン済み）
        if (loginCount >= 7) {
            tryUnlock(customer, 1, newlyUnlocked);
        }
        // 実績ID 2: 満点1回
        if (perfectDays >= 1) {
            tryUnlock(customer, 2, newlyUnlocked);
        }
        // 実績ID 3: 満点3回
        if (perfectDays >= 3) {
            tryUnlock(customer, 3, newlyUnlocked);
        }
        // 実績ID 4: 満点7回
        if (perfectDays >= 7) {
            tryUnlock(customer, 4, newlyUnlocked);
        }
        // 実績ID 5: レベル10
        if (level >= 10) {
            tryUnlock(customer, 5, newlyUnlocked);
        }
        // 実績ID 6: レベル50
        if (level >= 50) {
            tryUnlock(customer, 6, newlyUnlocked);
        }

        return newlyUnlocked;
    }

    /**
     * 指定実績がまだ解除されていなければ解除して newlyUnlocked に追加する。
     */
    private void tryUnlock(Customer customer, int achievementId, List<Achievement> newlyUnlocked) {
        Achievement ach = achievementRepository.getReferenceById(achievementId);
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
        return answerRepository.countPerfectDaysByCustomer(customer);
    }

    /**
     * 総正解数を返す（表示用）。
     */
    public int getTotalCorrect(Customer customer) {
        return answerRepository.countByCustomerAndCorrectFlagTrue(customer);
    }
}
