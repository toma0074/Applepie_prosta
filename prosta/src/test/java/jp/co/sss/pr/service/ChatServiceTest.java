package jp.co.sss.pr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.QuestionRepository;
import jp.co.sss.pr.repository.UserAchievementRepository;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private UserAchievementRepository userAchievementRepository;
    @Mock
    private LevelService levelService;
    @Mock
    private WeakQuestionService weakQuestionService;

    @InjectMocks
    private ChatService chatService;

    private final Customer user = new Customer();

    @Test
    void weakQuizListsWorstQuestionsAndLinksToWeakTest() {
        Question q = new Question();
        q.setQuestionText("Javaの継承について正しいものはどれか");
        when(weakQuestionService.pick(user, WeakQuestionService.MAX_QUESTIONS))
                .thenReturn(List.of(new WeakQuestionService.WeakQuestion(q, 4, 1)));

        ChatService.Reply reply = chatService.reply(user, "弱点問題を出して", "/prosta");

        assertTrue(reply.text().contains("1問"));
        assertTrue(reply.text().contains("正答率25%"));
        assertTrue(reply.text().contains("1/4回"));
        assertEquals("/prosta/test/weak", reply.links().get(0).url());
    }

    @Test
    void weakQuizExplainsWhenThereAreNoWeakQuestions() {
        when(weakQuestionService.pick(user, WeakQuestionService.MAX_QUESTIONS)).thenReturn(List.of());

        ChatService.Reply reply = chatService.reply(user, "復習したい", "/prosta");

        assertTrue(reply.text().contains("弱点問題はまだありません"));
    }

    @Test
    void weakPointsOffersWeakTestLinkAndToleratesNullCorrectFlag() {
        Answer wrong = answer(false);
        Answer unknown = answer(null);
        when(answerRepository.findAllByCustomerOrderByAnsIdDesc(user)).thenReturn(List.of(wrong, unknown));

        ChatService.Reply reply = chatService.reply(user, "苦手な分野は？", "/prosta");

        assertTrue(reply.text().contains("未分類：0%（0/2問）"));
        assertEquals("/prosta/test/weak", reply.links().get(0).url());
    }

    @Test
    void statsToleratesNullCorrectFlag() {
        when(answerRepository.findAllByCustomerOrderByAnsIdDesc(user))
                .thenReturn(List.of(answer(true), answer(null)));

        ChatService.Reply reply = chatService.reply(user, "成績を教えて", "/prosta");

        assertTrue(reply.text().contains("正解数：1問"));
        assertTrue(reply.text().contains("正答率：50%"));
    }

    private Answer answer(Boolean correct) {
        Answer a = new Answer();
        a.setCorrectFlag(correct);
        return a;
    }
}
