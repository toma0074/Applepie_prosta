package jp.co.sss.pr.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.DailyStudySummary;

/** 今日のクエスト。当日の解答実績から達成状況を算出する（DBへの保存はしない）。 */
@Service
public class QuestService {

    static final int ANSWER_TARGET = 5;
    static final int CORRECT_TARGET = 3;
    static final int ACCURACY_MIN_ANSWERS = 5;
    static final int ACCURACY_TARGET = 80;

    private final AnswerRepository answerRepository;

    public QuestService(AnswerRepository answerRepository) {
        this.answerRepository = answerRepository;
    }

    public record Quest(String title, String description, long progress, long target, boolean completed) {
        public int percent() {
            return target <= 0 ? 0 : (int) Math.min(100, progress * 100 / target);
        }
    }

    public List<Quest> dailyQuests(Customer customer, LocalDate today) {
        long answers = 0;
        long correct = 0;
        for (DailyStudySummary daily : answerRepository.summarizeDailyStudy(customer, today, today)) {
            answers += daily.getAnswerCount();
            correct += daily.getCorrectCount();
        }
        return buildQuests(answers, correct);
    }

    List<Quest> buildQuests(long answers, long correct) {
        long accuracy = answers == 0 ? 0 : Math.round(correct * 100.0 / answers);
        // 少数の正解だけで達成にならないよう、表示と同じ丸め正答率に最低解答数の条件を併用する。
        boolean accuracyDone = answers >= ACCURACY_MIN_ANSWERS && accuracy >= ACCURACY_TARGET;
        return List.of(
                new Quest("今日の5問チャレンジ", "今日5問に解答する",
                        Math.min(answers, ANSWER_TARGET), ANSWER_TARGET, answers >= ANSWER_TARGET),
                new Quest("3問正解", "今日3問正解する",
                        Math.min(correct, CORRECT_TARGET), CORRECT_TARGET, correct >= CORRECT_TARGET),
                new Quest("高正答率", "今日5問以上解いて正答率80%以上にする",
                        accuracyDone ? 1 : 0, 1, accuracyDone));
    }
}
