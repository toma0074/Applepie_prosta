package jp.co.sss.pr.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.form.AnswerForm;
import jp.co.sss.pr.repository.QuestionRepository;

class AnswerGradingServiceTest {
    @Test
    void ignoresClientCorrectAnswerAndTextAndReloadsDatabase() {
        QuestionRepository repository = mock(QuestionRepository.class);
        Question q = question(1);
        when(repository.findAllById(List.of(1))).thenReturn(List.of(q));
        AnswerForm answer = answer(1);
        answer.setCorrectOption(3);
        answer.setQuestionText("tampered");
        new AnswerGradingService(repository).normalize(List.of(q), List.of(answer), false);
        assertEquals(2, answer.getCorrectOption());
        assertEquals("database", answer.getQuestionText());
    }
    @Test
    void refusesMissingDuplicateOrSubstitutedQuestionsBeforeGrading() {
        AnswerGradingService service = new AnswerGradingService(mock(QuestionRepository.class));
        List<Question> questions = List.of(question(1), question(2));
        assertThrows(ResponseStatusException.class, () -> service.normalize(questions, List.of(answer(1)), false));
        assertThrows(ResponseStatusException.class, () -> service.normalize(questions, List.of(answer(1), answer(1)), false));
        assertThrows(ResponseStatusException.class, () -> service.normalize(questions, List.of(answer(1), answer(99)), false));
    }
    @Test
    void timerTreatsUnansweredAsUnknownButNormalGradingRejectsInvalidOption() {
        QuestionRepository repository = mock(QuestionRepository.class);
        Question q = question(1);
        when(repository.findAllById(List.of(1))).thenReturn(List.of(q));
        AnswerGradingService service = new AnswerGradingService(repository);
        AnswerForm answer = answer(1);
        answer.setAnsOption(99);
        assertThrows(ResponseStatusException.class, () -> service.normalize(List.of(q), List.of(answer), false));
        service.normalize(List.of(q), List.of(answer), true);
        assertEquals(4, answer.getAnsOption());
    }
    private Question question(int id) {
        Question q = new Question();
        q.setQuestionId(id); q.setQuestionText("database"); q.setCorrectOption(2);
        return q;
    }
    private AnswerForm answer(int id) {
        AnswerForm a = new AnswerForm(); a.setQuestionId(id); a.setAnsOption(1); return a;
    }
}
