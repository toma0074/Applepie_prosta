package jp.co.sss.pr.question_function;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.entity.QuestionNote;
import jp.co.sss.pr.entity.QuestionReport;
import jp.co.sss.pr.entity.QuestionReportReason;
import jp.co.sss.pr.entity.QuestionReportStatus;
import jp.co.sss.pr.form.QuestionNoteForm;
import jp.co.sss.pr.login_function.AccessPolicy;
import jp.co.sss.pr.repository.*;
import jp.co.sss.pr.service.QuestionStudyException;
import jp.co.sss.pr.service.QuestionStudyService;

@WebMvcTest({ QuestionStudyController.class, AdminQuestionReportController.class })
@AutoConfigureMockMvc(addFilters = false)
class QuestionStudyControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private QuestionStudyService service;
    @MockBean private FavoriteRepository favorites;
    @MockBean private QuestionRepository questions;
    @MockBean private CustomerRepository customers;
    @MockBean private CategoryRepository categories;
    @MockBean private FriendConnectionRepository friends;
    @MockBean private UserAchievementRepository achievements;

    private MockHttpSession session(int userId, int permission) {
        CustomerBean user = new CustomerBean();
        user.setUserId(userId);
        user.setUserName("user" + userId);
        user.setPermission(permission);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        return session;
    }

    private Question question() {
        Question q = new Question();
        q.setQuestionId(42);
        q.setQuestionText("What is <b>PROSTA</b>?");
        q.setDifficulty(2);
        q.setOptionA("Alpha");
        q.setOptionB("Beta");
        q.setOptionC("Gamma");
        q.setCorrectOption(2);
        q.setExplanation("Because beta.");
        return q;
    }

    private QuestionReport report(int id, QuestionReportStatus status) {
        Customer reporter = new Customer();
        reporter.setUserId(8);
        reporter.setUserName("reporter");
        QuestionReport r = new QuestionReport();
        r.setReportId(id);
        r.setCustomer(reporter);
        r.setQuestion(question());
        r.setReason(QuestionReportReason.UNCLEAR_EXPLANATION);
        r.setDetail("<script>alert(1)</script>");
        r.setStatus(status);
        r.setCreatedAt(LocalDateTime.of(2026, 11, 2, 10, 0));
        r.setUpdatedAt(LocalDateTime.of(2026, 11, 2, 10, 0));
        return r;
    }

    @Test
    void studyPageShowsQuestionExplanationAndOnlyTheCurrentUsersNote() throws Exception {
        QuestionNote note = new QuestionNote();
        note.setContent("my <secret> memo");
        note.setUpdatedAt(LocalDateTime.of(2026, 11, 1, 9, 30));
        when(service.findQuestion(42)).thenReturn(Optional.of(question()));
        when(service.findNote(7, 42)).thenReturn(Optional.of(note));
        when(service.recentOwnReports(7, 42)).thenReturn(List.of(report(3, QuestionReportStatus.OPEN)));

        mvc.perform(get("/question/42/study").param("returnUrl", "/test/result").session(session(7, 0)))
                .andExpect(status().isOk())
                .andExpect(view().name("question/question_study"))
                .andExpect(content().string(containsString("class=\"qs-actions qs-toolbar\"")))
                .andExpect(content().string(containsString(".aq-wrap.qs-wrap { max-width: 900px; margin: 0 auto; text-align: left; }")))
                .andExpect(content().string(containsString("What is &lt;b&gt;PROSTA&lt;/b&gt;?")))
                .andExpect(content().string(containsString("Because beta.")))
                .andExpect(content().string(containsString("my &lt;secret&gt; memo")))
                .andExpect(content().string(containsString("メモを更新")))
                .andExpect(content().string(containsString("メモを削除")))
                .andExpect(content().string(containsString("テスト結果に戻る")))
                .andExpect(content().string(containsString("解説がわかりにくい")))
                .andExpect(content().string(containsString("未対応")));
        verify(service).findNote(7, 42);
        verify(service, never()).findNote(eq(8), anyInt());
    }

    @Test
    void invalidOrMissingQuestionIdsRedirectWithJapaneseError() throws Exception {
        when(service.findQuestion(any())).thenReturn(Optional.empty());
        mvc.perform(get("/question/abc/study").session(session(7, 0)))
                .andExpect(redirectedUrl("/question"))
                .andExpect(flash().attribute("error", "対象の問題が見つかりません。"));
        mvc.perform(get("/question/999/study").param("returnUrl", "https://evil.example").session(session(7, 0)))
                .andExpect(redirectedUrl("/question"));
        verify(service).findQuestion(isNull());
        verify(service, never()).findNote(any(), any());
    }

    @Test
    void anonymousUsersAreSentToLogin() throws Exception {
        mvc.perform(get("/question/42/study")).andExpect(redirectedUrl("/"));
        mvc.perform(post("/question/42/note").param("content", "x")).andExpect(redirectedUrl("/"));
        mvc.perform(post("/question/42/report").param("reason", "OTHER")).andExpect(redirectedUrl("/"));
        verifyNoInteractions(service);
    }

    @Test
    void savingUsesTheSessionUserAndIgnoresForgedUserIds() throws Exception {
        when(service.findNote(7, 42)).thenReturn(Optional.empty());
        mvc.perform(post("/question/42/note").session(session(7, 0))
                .param("content", "memo").param("userId", "8").param("returnUrl", "/mypage"))
                .andExpect(redirectedUrl("/question/42/study?returnUrl=/mypage"))
                .andExpect(flash().attribute("noteMessage", "メモを保存しました。"));
        verify(service).saveNote(7, 42, "memo");
    }

    @Test
    void validationErrorsKeepTheDraftAndShowJapaneseMessage() throws Exception {
        when(service.findQuestion(42)).thenReturn(Optional.of(question()));
        when(service.saveNote(7, 42, "too long"))
                .thenThrow(new QuestionStudyException("メモは1000文字以内で入力してください。"));
        var result = mvc.perform(post("/question/42/note").session(session(7, 0)).param("content", "too long"))
                .andExpect(redirectedUrl("/question/42/study?returnUrl=/question"))
                .andExpect(flash().attribute("noteError", "メモは1000文字以内で入力してください。"))
                .andReturn();
        QuestionNoteForm draft = (QuestionNoteForm) result.getFlashMap().get("noteForm");
        assertEquals("too long", draft.getContent());

        mvc.perform(get("/question/42/study").session(session(7, 0))
                .flashAttr("noteForm", draft).flashAttr("noteError", "メモは1000文字以内で入力してください。"))
                .andExpect(content().string(containsString("too long")))
                .andExpect(content().string(containsString("メモは1000文字以内で入力してください。")))
                .andExpect(content().string(containsString("メモを保存")))
                .andExpect(content().string(not(containsString("メモを削除"))));
    }

    @Test
    void deleteAndReportFlowsUseSessionUser() throws Exception {
        when(service.deleteNote(7, 42)).thenReturn(true);
        mvc.perform(post("/question/42/note/delete").session(session(7, 0)))
                .andExpect(flash().attribute("noteMessage", "メモを削除しました。"));
        when(service.deleteNote(7, 43)).thenReturn(false);
        mvc.perform(post("/question/43/note/delete").session(session(7, 0)))
                .andExpect(flash().attribute("noteError", "削除するメモがありません。"));

        mvc.perform(post("/question/42/report").session(session(7, 0))
                .param("reason", "WRONG_ANSWER").param("detail", "Bが正しいはず"))
                .andExpect(redirectedUrl("/question/42/study?returnUrl=/question"))
                .andExpect(flash().attribute("reportMessage", "報告を受け付けました。ご協力ありがとうございます。"));
        verify(service).submitReport(7, 42, "WRONG_ANSWER", "Bが正しいはず");

        when(service.findQuestion(42)).thenReturn(Optional.of(question()));
        when(service.submitReport(7, 42, "OTHER", ""))
                .thenThrow(new QuestionStudyException("「その他」を選んだ場合は報告内容を入力してください。"));
        mvc.perform(post("/question/42/report").session(session(7, 0)).param("reason", "OTHER").param("detail", ""))
                .andExpect(flash().attribute("reportError", "「その他」を選んだ場合は報告内容を入力してください。"))
                .andExpect(flash().attributeExists("reportForm"));
    }

    @Test
    void adminListFiltersByStatusAndEscapesReportText() throws Exception {
        when(service.adminReports(QuestionReportStatus.ON_HOLD, 0))
                .thenReturn(new PageImpl<>(List.of(report(5, QuestionReportStatus.ON_HOLD)), PageRequest.of(0, 20), 1));
        when(service.statusCounts()).thenReturn(Map.of(QuestionReportStatus.OPEN, 2L,
                QuestionReportStatus.RESOLVED, 0L, QuestionReportStatus.ON_HOLD, 1L));

        mvc.perform(get("/admin/question-reports").param("status", "ON_HOLD").session(session(1, 1)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/question_reports"))
                .andExpect(content().string(containsString("未対応(2)")))
                .andExpect(content().string(containsString("保留(1)")))
                .andExpect(content().string(containsString("data-label=\"対応状況\"")))
                .andExpect(content().string(containsString(".qr-table select { font: inherit; max-width: 100%; }")))
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(containsString("reporter")));

        when(service.adminReports(null, 0)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        mvc.perform(get("/admin/question-reports").param("status", "BOGUS").param("page", "-1").session(session(1, 1)))
                .andExpect(content().string(containsString("該当する報告はありません。")))
                .andExpect(content().string(containsString("対応状況の指定が正しくないため")));
    }

    @Test
    void adminStatusUpdateRedirectsBackToTheSameFilter() throws Exception {
        QuestionReport updated = report(5, QuestionReportStatus.RESOLVED);
        when(service.updateStatus(5, "RESOLVED")).thenReturn(updated);
        mvc.perform(post("/admin/question-reports/5/status").session(session(1, 1))
                .param("status", "RESOLVED").param("filter", "OPEN").param("page", "2"))
                .andExpect(redirectedUrl("/admin/question-reports?status=OPEN&page=2"))
                .andExpect(flash().attribute("message", "報告No.5 を「対応済み」に更新しました。"));

        when(service.updateStatus(isNull(), eq("RESOLVED")))
                .thenThrow(new QuestionStudyException("対象の報告が見つかりません。"));
        mvc.perform(post("/admin/question-reports/x/status").session(session(1, 1)).param("status", "RESOLVED"))
                .andExpect(redirectedUrl("/admin/question-reports"))
                .andExpect(flash().attribute("error", "対象の報告が見つかりません。"));
    }

    @Test
    void nonAdminsCannotSeeOrUpdateReports() throws Exception {
        mvc.perform(get("/admin/question-reports").session(session(7, 0))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/question-reports/5/status").session(session(7, 0)).param("status", "RESOLVED"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
        assertTrue(AccessPolicy.isAdminOnly("/admin/question-reports"));
        assertTrue(!AccessPolicy.isPublic("/question/42/study"));
    }
}