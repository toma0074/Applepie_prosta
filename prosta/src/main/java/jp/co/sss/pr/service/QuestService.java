package jp.co.sss.pr.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.DailyStudySummary;

/** 当日の実績と保存済みの達成状態を組み合わせる。保存は採点トランザクション内で行う。 */
@Service
public class QuestService {

    static final int ANSWER_TARGET = 5;
    static final int CORRECT_TARGET = 3;
    static final int ACCURACY_MIN_ANSWERS = 5;
    static final int ACCURACY_TARGET = 80;
    public static final int COMPLETION_REWARD_EXP = 10;

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
        List<Quest> actual = buildQuests(answers, correct);
        int mask = today.equals(customer.getQuestRewardDate()) ? 7
                : today.equals(customer.getQuestProgressDate()) ? customer.getQuestCompletedMask() : 0;
        java.util.ArrayList<Quest> result = new java.util.ArrayList<>();
        for (int i = 0; i < actual.size(); i++) {
            Quest quest = actual.get(i);
            boolean completed = quest.completed() || (mask & (1 << i)) != 0;
            result.add(new Quest(quest.title(), quest.description(), completed ? quest.target() : quest.progress(),
                    quest.target(), completed));
        }
        return result;
    }

    public void recordDailyCompletion(Customer customer, LocalDate today) {
        List<Quest> quests = dailyQuests(customer, today);
        int mask = 0;
        for (int i = 0; i < quests.size(); i++) {
            if (quests.get(i).completed()) mask |= 1 << i;
        }
        customer.setQuestProgressDate(today);
        customer.setQuestCompletedMask(mask);
    }

    public boolean allCompleted(Customer customer, LocalDate today) {
        return dailyQuests(customer, today).stream().allMatch(quest -> quest.completed());
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
