package jp.co.sss.pr.mypage_function;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.*;
import jp.co.sss.pr.service.StudyReviewService;
import jp.co.sss.pr.service.WeakQuestionService;

@Controller
public class RecommendationController {
    private final CustomerRepository customers;
    private final CategoryRepository categories;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final StudyReviewService review;
    private final WeakQuestionService weak;

    public RecommendationController(CustomerRepository customers, CategoryRepository categories,
            QuestionRepository questions, AnswerRepository answers, StudyReviewService review, WeakQuestionService weak) {
        this.customers = customers; this.categories = categories; this.questions = questions;
        this.answers = answers; this.review = review; this.weak = weak;
    }

    @GetMapping("/learning/recommendation")
    public String recommend(Model model, HttpSession session) {
        CustomerBean bean = (CustomerBean) session.getAttribute("user");
        if (bean == null) return "redirect:/";
        Customer user = customers.getReferenceById(bean.getUserId());
        if (!review.due(user, LocalDate.now()).isEmpty()) {
            model.addAttribute("recommendationTitle", "今日の復習を5問");
            model.addAttribute("reason", "復習日を迎えた問題があります。忘れる前に確認しましょう。");
            model.addAttribute("mode", "spaced");
        } else if (!weak.pickRecentMistakes(user, 5).isEmpty()) {
            model.addAttribute("recommendationTitle", "間違えた問題をもう一度");
            model.addAttribute("reason", "直近の解答で間違えた問題を確認しましょう。");
            model.addAttribute("mode", "review");
        } else if (!weak.pick(user, 5).isEmpty()) {
            model.addAttribute("recommendationTitle", "弱点問題にチャレンジ");
            model.addAttribute("reason", "正答率の低い問題を復習して理解を固めましょう。");
            model.addAttribute("mode", "weak");
        } else {
            List<Question> available = questions.findAllByOrderByDifficultyAscCategoryAsc();
            Set<Integer> studied = answers.findAllByCustomerOrderByAnsIdDesc(user).stream()
                    .filter(a -> a.getQuestion() != null && a.getQuestion().getCategory() != null)
                    .map(a -> a.getQuestion().getCategory().getCategoryId()).collect(Collectors.toSet());
            Category chosen = categories.findAll().stream()
                    .filter(cat -> available.stream().anyMatch(q -> q.getCategory() != null
                            && cat.getCategoryId().equals(q.getCategory().getCategoryId())))
                    .sorted(java.util.Comparator.comparing((Category cat) -> studied.contains(cat.getCategoryId()))
                            .thenComparing(Category::getCategoryId)).findFirst().orElse(null);
            if (chosen == null) {
                model.addAttribute("recommendationTitle", "出題できる問題がまだありません");
                model.addAttribute("reason", "問題が登録されたら、おすすめ学習を開始できます。");
            } else {
                int wanted = studied.contains(chosen.getCategoryId()) ? Math.min(3, 1 + user.getUserLevel() / 10) : 1;
                int difficulty = available.stream().filter(q -> q.getCategory() != null
                        && chosen.getCategoryId().equals(q.getCategory().getCategoryId()))
                        .map(Question::getDifficulty).filter(d -> d != null && d >= 1 && d <= 3)
                        .min(java.util.Comparator.comparingInt(d -> Math.abs(d - wanted))).orElse(1);
                model.addAttribute("recommendationTitle", chosen.getCategoryName() + "を5問");
                model.addAttribute("reason", studied.contains(chosen.getCategoryId())
                        ? "これまでの学習を次の挑戦につなげましょう。" : "まだ学習していないカテゴリから始めましょう。");
                model.addAttribute("mode", "category");
                model.addAttribute("categoryId", chosen.getCategoryId());
                model.addAttribute("difficulty", difficulty);
            }
        }
        return "mypage/recommendation";
    }
}
