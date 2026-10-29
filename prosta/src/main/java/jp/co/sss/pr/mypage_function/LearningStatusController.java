package jp.co.sss.pr.mypage_function;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CategoryStudySummary;
import jp.co.sss.pr.repository.CustomerRepository;

@Controller
public class LearningStatusController {

    private static final int RECENT_ACTIVITY_LIMIT = 15;
    private static final int WEAK_CATEGORY_THRESHOLD = 60;

    private final AnswerRepository answerRepository;
    private final CategoryRepository categoryRepository;
    private final CustomerRepository customerRepository;

    public LearningStatusController(
            AnswerRepository answerRepository,
            CategoryRepository categoryRepository,
            CustomerRepository customerRepository) {
        this.answerRepository = answerRepository;
        this.categoryRepository = categoryRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/learning-status")
    public String learningStatus(HttpSession session, Model model) {
        CustomerBean userBean = (CustomerBean) session.getAttribute("user");
        if (userBean == null) {
            return "redirect:/";
        }

        Customer customer = customerRepository.getReferenceById(userBean.getUserId());
        List<CategoryStudySummary> summaries = answerRepository.summarizeByCategory(customer);
        Map<Integer, CategoryStudySummary> summariesByCategory = new HashMap<>();
        summaries.forEach(summary -> summariesByCategory.put(summary.getCategoryId(), summary));

        List<CategoryProgress> categoryProgress = new ArrayList<>();
        for (Category category : categoryRepository.findAll()) {
            CategoryStudySummary summary = summariesByCategory.get(category.getCategoryId());
            long answers = summary == null ? 0 : summary.getAnswerCount();
            long correct = summary == null ? 0 : summary.getCorrectCount();
            int accuracy = answers == 0 ? 0 : (int) Math.round(correct * 100.0 / answers);
            categoryProgress.add(new CategoryProgress(
                    category.getCategoryId(),
                    category.getCategoryName(),
                    answers,
                    correct,
                    accuracy,
                    answers > 0 && accuracy < WEAK_CATEGORY_THRESHOLD));
        }
        categoryProgress.sort(Comparator
                .comparing((CategoryProgress progress) -> progress.hasAnswers()).reversed()
                .thenComparingInt(progress -> progress.accuracy())
                .thenComparing(progress -> progress.categoryId()));

        long totalAnswers = categoryProgress.stream().mapToLong(progress -> progress.answerCount()).sum();
        long totalCorrect = categoryProgress.stream().mapToLong(progress -> progress.correctCount()).sum();
        int overallAccuracy = totalAnswers == 0
                ? 0
                : (int) Math.round(totalCorrect * 100.0 / totalAnswers);

        model.addAttribute("categoryProgress", categoryProgress);
        model.addAttribute("totalAnswers", totalAnswers);
        model.addAttribute("totalCorrect", totalCorrect);
        model.addAttribute("totalIncorrect", totalAnswers - totalCorrect);
        model.addAttribute("overallAccuracy", overallAccuracy);
        model.addAttribute("recentActivities",
                answerRepository.findRecentActivities(customer, PageRequest.of(0, RECENT_ACTIVITY_LIMIT))
                        .getContent());
        model.addAttribute("activityLimit", RECENT_ACTIVITY_LIMIT);
        return "mypage/learning_status";
    }

    public record CategoryProgress(
            Integer categoryId,
            String categoryName,
            long answerCount,
            long correctCount,
            int accuracy,
            boolean weak) {
        public boolean hasAnswers() {
            return answerCount > 0;
        }
    }
}
