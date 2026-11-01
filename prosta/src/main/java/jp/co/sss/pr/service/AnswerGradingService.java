package jp.co.sss.pr.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.form.AnswerForm;
import jp.co.sss.pr.repository.QuestionRepository;

@Service
public class AnswerGradingService {
    private final QuestionRepository questions;

    public AnswerGradingService(QuestionRepository questions) { this.questions = questions; }

    public void normalize(List<Question> expected, List<AnswerForm> submitted, boolean timeUp) {
        if (expected == null || submitted == null || expected.size() != submitted.size()) {
            throw invalid();
        }
        List<Integer> ids = expected.stream().map(q -> q.getQuestionId()).toList();
        if (ids.contains(null) || ids.stream().distinct().count() != ids.size()) throw invalid();
        for (int i = 0; i < ids.size(); i++) {
            if (submitted.get(i) == null || !ids.get(i).equals(submitted.get(i).getQuestionId())) throw invalid();
            Integer choice = submitted.get(i).getAnsOption();
            if (choice == null || choice < 1 || choice > 4) {
                if (timeUp) submitted.get(i).setAnsOption(4);
                else if (choice != null) throw invalid();
            }
        }
        Map<Integer, Question> current = questions.findAllById(ids).stream()
                .collect(Collectors.toMap(q -> q.getQuestionId(), Function.identity()));
        if (current.size() != ids.size()) throw invalid();
        for (AnswerForm answer : submitted) {
            Question q = current.get(answer.getQuestionId());
            if (q == null || q.getCorrectOption() == null || q.getCorrectOption() < 1 || q.getCorrectOption() > 3) {
                throw invalid();
            }
            answer.setCorrectOption(q.getCorrectOption());
            answer.setCategory(q.getCategory() == null ? 0 : q.getCategory().getCategoryId());
            answer.setQuestionText(q.getQuestionText());
            answer.setOptionA(q.getOptionA()); answer.setOptionB(q.getOptionB()); answer.setOptionC(q.getOptionC());
            answer.setExplanation(q.getExplanation());
        }
    }

    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "解答データが出題内容と一致しません。問題選択からやり直してください。");
    }
}
