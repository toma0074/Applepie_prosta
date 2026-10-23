package jp.co.sss.pr.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.QuestionAttemptStat;
import jp.co.sss.pr.repository.QuestionRepository;

/** 解答履歴から、正答率の低い問題（弱点問題）を抽出する。 */
@Service
public class WeakQuestionService {

    /** 弱点判定に必要な最低解答回数 */
    public static final int MIN_ATTEMPTS = 2;

    /** 1回の出題数の上限 */
    public static final int MAX_QUESTIONS = 15;

    public record WeakQuestion(Question question, int attempts, int correct) {
        public double rate() {
            return attempts == 0 ? 0 : (double) correct / attempts;
        }

        public long percent() {
            return Math.round(rate() * 100);
        }
    }

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private QuestionRepository questionRepository;

    /** 正答率が低い順（同率なら解答回数が多い順）に、最大 limit 件返す。全問正解の問題は含めない。 */
    @Transactional(readOnly = true)
    public List<WeakQuestion> pick(Customer user, int limit) {
        int max = Math.max(0, Math.min(limit, MAX_QUESTIONS));
        List<QuestionAttemptStat> weakStats = answerRepository
                .summarizeByQuestion(user, MIN_ATTEMPTS).stream()
                .filter(s -> s.getCorrectCount() < s.getAttemptCount())
                .sorted(Comparator
                        .comparingDouble((QuestionAttemptStat s) -> (double) s.getCorrectCount() / s.getAttemptCount())
                        .thenComparing(Comparator.comparingLong(QuestionAttemptStat::getAttemptCount).reversed())
                        .thenComparing(QuestionAttemptStat::getQuestionId))
                .limit(max)
                .toList();
        if (weakStats.isEmpty()) {
            return List.of();
        }

        Map<Integer, Question> questions = questionRepository
                .findAllById(weakStats.stream().map(QuestionAttemptStat::getQuestionId).toList())
                .stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity()));

        List<WeakQuestion> result = new ArrayList<>();
        for (QuestionAttemptStat s : weakStats) {
            Question q = questions.get(s.getQuestionId());
            if (q != null) {
                result.add(new WeakQuestion(q, s.getAttemptCount().intValue(), s.getCorrectCount().intValue()));
            }
        }
        return result;
    }
}
