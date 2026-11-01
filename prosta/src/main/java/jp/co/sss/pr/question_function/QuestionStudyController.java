package jp.co.sss.pr.question_function;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.entity.QuestionNote;
import jp.co.sss.pr.entity.QuestionReportReason;
import jp.co.sss.pr.form.QuestionNoteForm;
import jp.co.sss.pr.form.QuestionReportForm;
import jp.co.sss.pr.repository.FavoriteRepository;
import jp.co.sss.pr.service.QuestionStudyException;
import jp.co.sss.pr.service.QuestionStudyService;

/** 問題の学習ページ(問題・選択肢・解説、本人専用メモ、問題/解説の報告) */
@Controller
public class QuestionStudyController {

    static final String DEFAULT_RETURN_URL = "/question";

    /** 「戻る」先の許可リスト(オープンリダイレクト対策) */
    static final Map<String, String> RETURN_URLS;
    static {
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("/question", "問題一覧に戻る");
        urls.put("/question/bookmarks", "ブックマーク一覧に戻る");
        urls.put("/test/result", "テスト結果に戻る");
        urls.put("/mypage", "マイページに戻る");
        RETURN_URLS = Map.copyOf(urls);
    }

    private final QuestionStudyService studyService;
    private final HttpSession session;
    private final FavoriteRepository favoriteRepository;

    public QuestionStudyController(QuestionStudyService studyService, HttpSession session,
            FavoriteRepository favoriteRepository) {
        this.studyService = studyService;
        this.session = session;
        this.favoriteRepository = favoriteRepository;
    }

    @GetMapping("/question/{id}/study")
    public String study(@PathVariable("id") String id,
            @RequestParam(name = "returnUrl", required = false) String returnUrl,
            Model model, RedirectAttributes redirect) {
        CustomerBean user = currentUser();
        if (user == null) {
            return "redirect:/";
        }
        String back = resolveReturnUrl(returnUrl);
        Integer questionId = parseId(id);
        Question question = studyService.findQuestion(questionId).orElse(null);
        if (question == null) {
            redirect.addFlashAttribute("error", "対象の問題が見つかりません。");
            return "redirect:" + back;
        }
        QuestionNote note = studyService.findNote(user.getUserId(), questionId).orElse(null);

        QuestionNoteForm noteForm = (QuestionNoteForm) model.getAttribute("noteForm");
        if (noteForm == null) {
            noteForm = new QuestionNoteForm();
            noteForm.setContent(note == null ? "" : note.getContent());
        }
        noteForm.setReturnUrl(back);
        QuestionReportForm reportForm = (QuestionReportForm) model.getAttribute("reportForm");
        if (reportForm == null) {
            reportForm = new QuestionReportForm();
        }
        reportForm.setReturnUrl(back);

        model.addAttribute("question", question);
        model.addAttribute("bookmarked",
                favoriteRepository.existsByCustomerUserIdAndQuestionQuestionId(user.getUserId(), questionId));
        model.addAttribute("note", note);
        model.addAttribute("noteForm", noteForm);
        model.addAttribute("reportForm", reportForm);
        model.addAttribute("reasons", QuestionReportReason.values());
        model.addAttribute("myReports", studyService.recentOwnReports(user.getUserId(), questionId));
        model.addAttribute("maxLength", QuestionStudyService.MAX_TEXT_LENGTH);
        model.addAttribute("returnUrl", back);
        model.addAttribute("returnLabel", RETURN_URLS.get(back));
        return "question/question_study";
    }

    @PostMapping("/question/{id}/note")
    public String saveNote(@PathVariable("id") String id, @ModelAttribute QuestionNoteForm form,
            RedirectAttributes redirect) {
        CustomerBean user = currentUser();
        if (user == null) {
            return "redirect:/";
        }
        Integer questionId = parseId(id);
        try {
            boolean existed = studyService.findNote(user.getUserId(), questionId).isPresent();
            studyService.saveNote(user.getUserId(), questionId, form.getContent());
            redirect.addFlashAttribute("noteMessage", existed ? "メモを更新しました。" : "メモを保存しました。");
        } catch (QuestionStudyException e) {
            return failed(questionId, form.getReturnUrl(), redirect, "noteError", e.getMessage(), "noteForm", form);
        } catch (DataIntegrityViolationException e) {
            return failed(questionId, form.getReturnUrl(), redirect, "noteError",
                    "メモの保存が重なりました。もう一度お試しください。", "noteForm", form);
        }
        return studyRedirect(questionId, form.getReturnUrl());
    }

    @PostMapping("/question/{id}/note/delete")
    public String deleteNote(@PathVariable("id") String id,
            @RequestParam(name = "returnUrl", required = false) String returnUrl,
            RedirectAttributes redirect) {
        CustomerBean user = currentUser();
        if (user == null) {
            return "redirect:/";
        }
        Integer questionId = parseId(id);
        try {
            boolean deleted = studyService.deleteNote(user.getUserId(), questionId);
            if (deleted) {
                redirect.addFlashAttribute("noteMessage", "メモを削除しました。");
            } else {
                redirect.addFlashAttribute("noteError", "削除するメモがありません。");
            }
        } catch (QuestionStudyException e) {
            return failed(questionId, returnUrl, redirect, "noteError", e.getMessage(), null, null);
        }
        return studyRedirect(questionId, returnUrl);
    }

    @PostMapping("/question/{id}/report")
    public String report(@PathVariable("id") String id, @ModelAttribute QuestionReportForm form,
            RedirectAttributes redirect) {
        CustomerBean user = currentUser();
        if (user == null) {
            return "redirect:/";
        }
        Integer questionId = parseId(id);
        try {
            studyService.submitReport(user.getUserId(), questionId, form.getReason(), form.getDetail());
            redirect.addFlashAttribute("reportMessage", "報告を受け付けました。ご協力ありがとうございます。");
        } catch (QuestionStudyException e) {
            return failed(questionId, form.getReturnUrl(), redirect, "reportError", e.getMessage(), "reportForm",
                    form);
        }
        return studyRedirect(questionId, form.getReturnUrl());
    }

    private String failed(Integer questionId, String returnUrl, RedirectAttributes redirect,
            String errorKey, String message, String formKey, Object form) {
        if (studyService.findQuestion(questionId).isEmpty()) {
            redirect.addFlashAttribute("error", "対象の問題が見つかりません。");
            return "redirect:" + resolveReturnUrl(returnUrl);
        }
        redirect.addFlashAttribute(errorKey, message);
        if (formKey != null) {
            redirect.addFlashAttribute(formKey, form);
        }
        return studyRedirect(questionId, returnUrl);
    }

    private String studyRedirect(Integer questionId, String returnUrl) {
        return "redirect:" + UriComponentsBuilder.fromPath("/question/{id}/study")
                .queryParam("returnUrl", resolveReturnUrl(returnUrl))
                .buildAndExpand(questionId).encode().toUriString();
    }

    private CustomerBean currentUser() {
        Object user = session.getAttribute("user");
        return user instanceof CustomerBean bean && bean.getUserId() != null ? bean : null;
    }

    static String resolveReturnUrl(String requested) {
        return requested != null && RETURN_URLS.containsKey(requested) ? requested : DEFAULT_RETURN_URL;
    }

    static Integer parseId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,8}")) {
            return null;
        }
        return Integer.valueOf(value);
    }
}