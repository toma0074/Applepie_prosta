package jp.co.sss.pr.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.entity.QuestionNote;
import jp.co.sss.pr.entity.QuestionReport;
import jp.co.sss.pr.entity.QuestionReportReason;
import jp.co.sss.pr.entity.QuestionReportStatus;
import jp.co.sss.pr.repository.QuestionNoteRepository;
import jp.co.sss.pr.repository.QuestionReportRepository;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QuestionStudyService.class)
class QuestionStudyServiceTest {
    @Autowired private QuestionStudyService service;
    @Autowired private QuestionNoteRepository notes;
    @Autowired private QuestionReportRepository reports;
    @Autowired private TestEntityManager entities;

    private Customer me;
    private Customer other;
    private Question question;

    @BeforeEach
    void setUp() {
        me = customer("me");
        other = customer("other");
        question = question();
    }

    private Customer customer(String name) {
        Customer c = new Customer();
        c.setUserName(name);
        c.setUserPass("test");
        c.setPermission(0);
        c.setDeleteFlag(false);
        return entities.persist(c);
    }

    private Question question() {
        Question q = new Question();
        q.setDifficulty(1);
        q.setQuestionText("Question");
        q.setOptionA("A");
        q.setOptionB("B");
        q.setOptionC("C");
        q.setCorrectOption(1);
        return entities.persist(q);
    }

    private void flushClear() {
        entities.flush();
        entities.clear();
    }

    @Test
    void ownerCreatesUpdatesAndDeletesOnlyTheirOwnNote() {
        service.saveNote(me.getUserId(), question.getQuestionId(), "  first\r\nline  ");
        service.saveNote(other.getUserId(), question.getQuestionId(), "other note");
        flushClear();
        assertEquals("first\nline", service.findNote(me.getUserId(), question.getQuestionId()).orElseThrow().getContent());

        QuestionNote updated = service.saveNote(me.getUserId(), question.getQuestionId(), "second");
        flushClear();
        assertEquals(2, notes.count());
        assertEquals("second", service.findNote(me.getUserId(), question.getQuestionId()).orElseThrow().getContent());
        assertEquals(updated.getNoteId(),
                service.findNote(me.getUserId(), question.getQuestionId()).orElseThrow().getNoteId());

        assertTrue(service.deleteNote(me.getUserId(), question.getQuestionId()));
        flushClear();
        assertTrue(service.findNote(me.getUserId(), question.getQuestionId()).isEmpty());
        assertEquals("other note",
                service.findNote(other.getUserId(), question.getQuestionId()).orElseThrow().getContent());
        assertFalse(service.deleteNote(me.getUserId(), question.getQuestionId()));
    }

    @Test
    void noteValidationRejectsBlankTooLongAndUnknownTargets() {
        assertEquals("メモを入力してください。", assertThrows(QuestionStudyException.class,
                () -> service.saveNote(me.getUserId(), question.getQuestionId(), " \r\n ")).getMessage());
        assertEquals("メモは1000文字以内で入力してください。", assertThrows(QuestionStudyException.class,
                () -> service.saveNote(me.getUserId(), question.getQuestionId(), "あ".repeat(1001))).getMessage());
        assertEquals("対象の問題が見つかりません。", assertThrows(QuestionStudyException.class,
                () -> service.saveNote(me.getUserId(), 999999, "memo")).getMessage());
        assertThrows(QuestionStudyException.class, () -> service.saveNote(me.getUserId(), null, "memo"));
        assertThrows(QuestionStudyException.class, () -> service.saveNote(null, question.getQuestionId(), "memo"));
        assertThrows(QuestionStudyException.class, () -> service.deleteNote(me.getUserId(), -1));
        assertEquals(0, notes.count());

        // 改行はCRLFでも1文字、サロゲートペアも1文字として数える
        String exact = "\uD83D\uDE00".repeat(998) + "\r\nx";
        assertThrows(QuestionStudyException.class,
                () -> service.saveNote(me.getUserId(), question.getQuestionId(), "\uD83D\uDE00" + exact));
        assertDoesNotThrow(() -> service.saveNote(me.getUserId(), question.getQuestionId(), exact));
    }

    @Test
    void deletedUsersCannotWriteNotes() {
        other.setDeleteFlag(true);
        entities.persistAndFlush(other);
        assertThrows(QuestionStudyException.class,
                () -> service.saveNote(other.getUserId(), question.getQuestionId(), "memo"));
    }

    @Test
    void databaseKeepsOneNotePerUserAndQuestion() {
        service.saveNote(me.getUserId(), question.getQuestionId(), "memo");
        flushClear();
        QuestionNote duplicate = new QuestionNote();
        duplicate.setCustomer(entities.find(Customer.class, me.getUserId()));
        duplicate.setQuestion(entities.find(Question.class, question.getQuestionId()));
        duplicate.setContent("dup");
        duplicate.setCreatedAt(java.time.LocalDateTime.now());
        duplicate.setUpdatedAt(java.time.LocalDateTime.now());
        assertThrows(DataIntegrityViolationException.class, () -> notes.saveAndFlush(duplicate));
    }

    @Test
    void reportValidationRequiresKnownReasonDetailForOtherAndLengthLimit() {
        assertEquals("報告理由を選択してください。", assertThrows(QuestionStudyException.class,
                () -> service.submitReport(me.getUserId(), question.getQuestionId(), "HACK", "x")).getMessage());
        assertThrows(QuestionStudyException.class,
                () -> service.submitReport(me.getUserId(), question.getQuestionId(), null, "x"));
        assertEquals("「その他」を選んだ場合は報告内容を入力してください。", assertThrows(QuestionStudyException.class,
                () -> service.submitReport(me.getUserId(), question.getQuestionId(), "OTHER", "  ")).getMessage());
        assertEquals("報告内容は1000文字以内で入力してください。", assertThrows(QuestionStudyException.class,
                () -> service.submitReport(me.getUserId(), question.getQuestionId(), "WRONG_ANSWER",
                        "a".repeat(1001))).getMessage());
        assertThrows(QuestionStudyException.class,
                () -> service.submitReport(me.getUserId(), 999999, "WRONG_ANSWER", ""));
        assertEquals(0, reports.count());
    }

    @Test
    void reportWorkflowFromSubmissionToAdminStatusUpdates() {
        QuestionReport first = service.submitReport(me.getUserId(), question.getQuestionId(), "WRONG_ANSWER", "");
        assertNull(first.getDetail());
        assertEquals(QuestionReportStatus.OPEN, first.getStatus());
        assertEquals("同じ理由の報告はすでに受け付けています。対応をお待ちください。", assertThrows(QuestionStudyException.class,
                () -> service.submitReport(me.getUserId(), question.getQuestionId(), "WRONG_ANSWER", "again"))
                .getMessage());
        service.submitReport(me.getUserId(), question.getQuestionId(), "UNCLEAR_EXPLANATION", "解説が短い");
        service.submitReport(other.getUserId(), question.getQuestionId(), "WRONG_ANSWER", "other");
        flushClear();

        List<QuestionReport> mine = service.recentOwnReports(me.getUserId(), question.getQuestionId());
        assertEquals(2, mine.size());
        assertTrue(mine.stream().allMatch(r -> r.getCustomer().getUserId().equals(me.getUserId())));

        service.updateStatus(first.getReportId(), "RESOLVED");
        flushClear();
        assertEquals(QuestionReportStatus.RESOLVED, reports.findById(first.getReportId()).orElseThrow().getStatus());
        assertEquals(2L, service.statusCounts().get(QuestionReportStatus.OPEN));
        assertEquals(1L, service.statusCounts().get(QuestionReportStatus.RESOLVED));
        assertEquals(0L, service.statusCounts().get(QuestionReportStatus.ON_HOLD));
        assertEquals(1, service.adminReports(QuestionReportStatus.RESOLVED, 0).getTotalElements());
        assertEquals(3, service.adminReports(null, 0).getTotalElements());

        // 解決済みになれば同じ理由で再報告できる
        assertDoesNotThrow(() -> service.submitReport(me.getUserId(), question.getQuestionId(), "WRONG_ANSWER", ""));

        assertEquals("対応状況の指定が正しくありません。", assertThrows(QuestionStudyException.class,
                () -> service.updateStatus(first.getReportId(), "DONE")).getMessage());
        assertEquals("対象の報告が見つかりません。", assertThrows(QuestionStudyException.class,
                () -> service.updateStatus(999999, "ON_HOLD")).getMessage());
        assertThrows(QuestionStudyException.class, () -> service.updateStatus(null, "ON_HOLD"));
    }

    @Test
    void adminListIsPagedNewestFirstAndClampsOutOfRangePages() {
        for (int i = 0; i < QuestionStudyService.ADMIN_PAGE_SIZE + 3; i++) {
            Customer c = customer("reporter" + i);
            service.submitReport(c.getUserId(), question.getQuestionId(), QuestionReportReason.OTHER.name(), "r" + i);
        }
        flushClear();
        Page<QuestionReport> first = service.adminReports(QuestionReportStatus.OPEN, 0);
        assertEquals(QuestionStudyService.ADMIN_PAGE_SIZE, first.getContent().size());
        assertEquals(2, first.getTotalPages());
        assertTrue(first.getContent().get(0).getReportId() > first.getContent().get(1).getReportId());
        Page<QuestionReport> clamped = service.adminReports(QuestionReportStatus.OPEN, 99);
        assertEquals(1, clamped.getNumber());
        assertEquals(3, clamped.getContent().size());
        assertEquals(0, service.adminReports(QuestionReportStatus.ON_HOLD, 5).getTotalElements());
    }
}