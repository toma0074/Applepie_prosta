package jp.co.sss.pr.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.AnswerRepository;

@Service
public class StudyReviewService {
    private final AnswerRepository answers;
    private static final int[] INTERVALS = {1, 3, 7, 14, 30};

    public StudyReviewService(AnswerRepository answers) { this.answers = answers; }

    public record ScheduledReview(Question question, LocalDate nextDate, int correctStreak, String previousAnswer) {}

    @Transactional(readOnly = true)
    public List<ScheduledReview> due(Customer user, LocalDate today) {
        return schedule(user).stream().filter(review -> !review.nextDate().isAfter(today)).toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduledReview> schedule(Customer user) {
        Map<Integer, List<Answer>> byQuestion = new LinkedHashMap<>();
        for (Answer answer : answers.findAllByCustomerOrderByAnsIdDesc(user)) {
            if (answer.getQuestion() != null && answer.getAnsDate() != null) {
                byQuestion.computeIfAbsent(answer.getQuestion().getQuestionId(), key -> new ArrayList<>()).add(answer);
            }
        }
        List<ScheduledReview> result = new ArrayList<>();
        for (List<Answer> history : byQuestion.values()) {
            Answer latest = history.get(0);
            int streak = 0;
            LocalDate countedDate = null;
            for (Answer answer : history) {
                if (!Boolean.TRUE.equals(answer.getCorrectFlag())) break;
                // 同日に同じ問題を連打しても復習間隔を延ばさない。
                if (!answer.getAnsDate().equals(countedDate)) { streak++; countedDate = answer.getAnsDate(); }
            }
            int days = streak == 0 ? 1 : INTERVALS[Math.min(streak - 1, INTERVALS.length - 1)];
            String chosen = latest.getAnsOption() != null && latest.getAnsOption() >= 1 && latest.getAnsOption() <= 3
                    ? List.of("A", "B", "C").get(latest.getAnsOption() - 1) : "わからない";
            result.add(new ScheduledReview(latest.getQuestion(), latest.getAnsDate().plusDays(days), streak,
                    "前回：" + chosen + "（" + (Boolean.TRUE.equals(latest.getCorrectFlag()) ? "正解" : "不正解") + "）"));
        }
        result.sort(java.util.Comparator.comparing(ScheduledReview::nextDate)
                .thenComparing(review -> review.question().getQuestionId()));
        return result;
    }
}
