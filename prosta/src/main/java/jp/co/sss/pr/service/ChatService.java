package jp.co.sss.pr.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.QuestionRepository;
import jp.co.sss.pr.repository.UserAchievementRepository;

/**
 * マイページ用チャットボット。外部APIは使わず、キーワード判定と
 * 自分の学習データの集計だけで返答する。
 */
@Service
public class ChatService {

    public record Link(String label, String url) {}

    public record Reply(String text, List<Link> links) {}

    private static final String NO_CATEGORY = "未分類";

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private LevelService levelService;

    @Transactional(readOnly = true)
    public Reply reply(Customer user, String message, String contextPath) {
        String m = message == null ? "" : message.trim().toLowerCase();

        if (containsAny(m, "苦手", "弱点", "弱い", "間違", "不正解", "ミス")) {
            return weakPoints(user, contextPath);
        }
        if (containsAny(m, "おすすめ", "オススメ", "次", "何を", "何やる", "提案", "勉強", "学習")) {
            return recommend(user, contextPath);
        }
        if (containsAny(m, "正答率", "成績", "進捗", "何問", "正解数", "解いた")) {
            return stats(user, contextPath);
        }
        if (containsAny(m, "レベル", "level", "経験")) {
            return level(user);
        }
        if (containsAny(m, "ログイン", "連続")) {
            return login(user);
        }
        if (containsAny(m, "実績", "バッジ", "称号")) {
            return achievement(user, contextPath);
        }
        if (containsAny(m, "フレンド", "友達", "友だち", "ランキング")) {
            return friend(contextPath);
        }
        if (containsAny(m, "検索", "探")) {
            return search(contextPath);
        }
        if (containsAny(m, "使い方", "ヘルプ", "help", "機能", "メニュー", "できる")) {
            return new Reply(helpText(), List.of());
        }
        if (containsAny(m, "こんにちは", "こんばんは", "おはよう", "はじめまして", "hello")) {
            return new Reply("こんにちは、PROSTAアシスタントです。\n" + helpText(), List.of());
        }
        return new Reply("うまく聞き取れませんでした。\n" + helpText(), List.of());
    }

    // ----------------------------------------------------------------
    // 各返答
    // ----------------------------------------------------------------

    private Reply weakPoints(Customer user, String ctx) {
        Map<String, int[]> byCategory = aggregateByCategory(user);
        if (byCategory.isEmpty()) {
            return new Reply("まだ解答履歴がありません。まずは苦手範囲特定問題に挑戦してみましょう。",
                    List.of(new Link("苦手範囲特定問題", ctx + "/test/diagnostic")));
        }

        List<Map.Entry<String, int[]>> sorted = new ArrayList<>(byCategory.entrySet());
        sorted.sort(Comparator.comparingDouble(e -> rate(e.getValue())));

        StringBuilder sb = new StringBuilder("正答率の低い順に並べました。\n");
        for (Map.Entry<String, int[]> e : sorted) {
            sb.append("・").append(e.getKey()).append("：")
              .append(percent(e.getValue())).append("%（")
              .append(e.getValue()[1]).append("/").append(e.getValue()[0]).append("問）\n");
        }
        sb.append("\n一番苦手なのは「").append(sorted.get(0).getKey()).append("」です。");
        return new Reply(sb.toString(), List.of(
                new Link("カテゴリ別問題", ctx + "/test/category"),
                new Link("苦手範囲特定問題", ctx + "/test/diagnostic")));
    }

    private Reply recommend(Customer user, String ctx) {
        Map<String, int[]> byCategory = aggregateByCategory(user);
        if (byCategory.isEmpty()) {
            return new Reply("まだ解答履歴がありません。まずは苦手範囲特定問題で、自分の得意・不得意を調べましょう。",
                    List.of(new Link("苦手範囲特定問題", ctx + "/test/diagnostic")));
        }

        Map.Entry<String, int[]> weakest = byCategory.entrySet().stream()
                .min(Comparator.comparingDouble(e -> rate(e.getValue())))
                .get();
        int unanswered = unansweredCount(user);

        StringBuilder sb = new StringBuilder();
        if (rate(weakest.getValue()) < 1.0) {
            sb.append("次は「").append(weakest.getKey()).append("」の復習がおすすめです。")
              .append("（正答率").append(percent(weakest.getValue())).append("%）\n");
        } else {
            sb.append("解いた問題はすべて正解です。すばらしい！\n");
        }
        if (unanswered > 0) {
            sb.append("まだ解いていない問題が").append(unanswered).append("問あります。");
        } else {
            sb.append("登録されている問題はすべて解答済みです。");
        }
        return new Reply(sb.toString(), List.of(
                new Link("カテゴリ別問題", ctx + "/test/category"),
                new Link("苦手範囲特定問題", ctx + "/test/diagnostic")));
    }

    private Reply stats(Customer user, String ctx) {
        List<Answer> answers = answerRepository.findAllByCustomerOrderByAnsIdDesc(user);
        if (answers.isEmpty()) {
            return new Reply("まだ解答履歴がありません。",
                    List.of(new Link("カテゴリ別問題", ctx + "/test/category")));
        }
        long correct = answers.stream().filter(Answer::getCorrectFlag).count();
        int total = answers.size();
        StringBuilder sb = new StringBuilder();
        sb.append("これまでの解答数：").append(total).append("問\n")
          .append("正解数：").append(correct).append("問\n")
          .append("正答率：").append(Math.round(correct * 100.0 / total)).append("%\n");

        for (Map.Entry<String, int[]> e : aggregateByCategory(user).entrySet()) {
            sb.append("・").append(e.getKey()).append("：")
              .append(percent(e.getValue())).append("%\n");
        }
        return new Reply(sb.toString().trim(), List.of());
    }

    private Reply level(Customer user) {
        int total = levelService.getTotalCorrect(user);
        int perfect = levelService.getPerfectCount(user);
        String text = "現在のレベルは " + user.getUserLevel() + " です。\n"
                + "レベルは次の合計で決まります。\n"
                + "・ログイン日数 " + user.getLoginCount() + "日（×1）\n"
                + "・連続ログイン 7日ごとに+10\n"
                + "・累計正解数 " + total + "問（×2）\n"
                + "・満点の日 " + perfect + "回（×5）";
        return new Reply(text, List.of());
    }

    private Reply login(Customer user) {
        return new Reply("ログイン日数：" + user.getLoginCount() + "日\n"
                + "連続ログイン：" + user.getConsecutiveLoginDays() + "日\n"
                + "7日連続でログインするとレベルボーナスと実績がもらえます。", List.of());
    }

    private Reply achievement(Customer user, String ctx) {
        int count = userAchievementRepository.findByCustomerOrderByUnlockedDateDesc(user).size();
        return new Reply("解除した実績は " + count + " 個です。",
                List.of(new Link("実績一覧", ctx + "/achievement")));
    }

    private Reply friend(String ctx) {
        return new Reply("フレンド画面でユーザー名を検索して申請できます。\n"
                + "承認されるとフレンドの学習状況と比較したランキングを見られます。",
                List.of(new Link("フレンド", ctx + "/friends")));
    }

    private Reply search(String ctx) {
        return new Reply("問題は問題文・カテゴリ・難易度で検索できます。左のメニューの検索欄か、問題検索ページを使ってください。",
                List.of(new Link("問題検索", ctx + "/question")));
    }

    private String helpText() {
        return "次のようなことを聞けます。\n"
                + "・苦手な分野は？\n"
                + "・次に何をやればいい？\n"
                + "・成績（正答率）を教えて\n"
                + "・レベルについて\n"
                + "・実績 / フレンド / 問題の検索";
    }

    // ----------------------------------------------------------------
    // 集計
    // ----------------------------------------------------------------

    /** カテゴリ名 → [解答数, 正解数] */
    private Map<String, int[]> aggregateByCategory(Customer user) {
        Map<String, int[]> map = new LinkedHashMap<>();
        for (Answer a : answerRepository.findAllByCustomerOrderByAnsIdDesc(user)) {
            int[] v = map.computeIfAbsent(categoryName(a.getQuestion()), k -> new int[2]);
            v[0]++;
            if (a.getCorrectFlag()) {
                v[1]++;
            }
        }
        return map;
    }

    private int unansweredCount(Customer user) {
        Set<Integer> answered = new HashSet<>();
        for (Answer a : answerRepository.findAllByCustomerOrderByAnsIdDesc(user)) {
            if (a.getQuestion() != null) {
                answered.add(a.getQuestion().getQuestionId());
            }
        }
        int count = 0;
        for (Question q : questionRepository.findAll()) {
            if (!answered.contains(q.getQuestionId())) {
                count++;
            }
        }
        return count;
    }

    private String categoryName(Question q) {
        if (q == null || q.getCategory() == null) {
            return NO_CATEGORY;
        }
        return q.getCategory().getCategoryName();
    }

    private double rate(int[] v) {
        return v[0] == 0 ? 0 : (double) v[1] / v[0];
    }

    private long percent(int[] v) {
        return Math.round(rate(v) * 100);
    }

    private boolean containsAny(String text, String... words) {
        for (String w : words) {
            if (text.contains(w.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
