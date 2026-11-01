package jp.co.sss.pr.question_function;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.QuestionReport;
import jp.co.sss.pr.entity.QuestionReportStatus;
import jp.co.sss.pr.service.QuestionStudyException;
import jp.co.sss.pr.service.QuestionStudyService;

/** 管理者向け: 問題・解説の報告一覧と対応状況の更新 */
@Controller
public class AdminQuestionReportController {

    private static final Integer ADMIN = 1;

    private final QuestionStudyService studyService;
    private final HttpSession session;

    public AdminQuestionReportController(QuestionStudyService studyService, HttpSession session) {
        this.studyService = studyService;
        this.session = session;
    }

    @GetMapping("/admin/question-reports")
    public String list(@RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false) String page,
            Model model, HttpServletResponse response) throws java.io.IOException {
        if (!isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        QuestionReportStatus filter = QuestionStudyService.parseEnum(QuestionReportStatus.class, status);
        if (status != null && !status.isBlank() && filter == null) {
            model.addAttribute("error", "対応状況の指定が正しくないため、すべての報告を表示しています。");
        }
        Page<QuestionReport> reports = studyService.adminReports(filter, parsePage(page));
        model.addAttribute("reports", reports);
        model.addAttribute("status", filter);
        model.addAttribute("statuses", QuestionReportStatus.values());
        model.addAttribute("counts", studyService.statusCounts());
        return "admin/question_reports";
    }

    @PostMapping("/admin/question-reports/{id}/status")
    public String updateStatus(@PathVariable("id") String id,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "page", required = false) String page,
            RedirectAttributes redirect, HttpServletResponse response) throws java.io.IOException {
        if (!isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        try {
            QuestionReport report = studyService.updateStatus(QuestionStudyController.parseId(id), status);
            redirect.addFlashAttribute("message",
                    "報告No." + report.getReportId() + " を「" + report.getStatus().getLabel() + "」に更新しました。");
        } catch (QuestionStudyException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        QuestionReportStatus currentFilter = QuestionStudyService.parseEnum(QuestionReportStatus.class, filter);
        UriComponentsBuilder target = UriComponentsBuilder.fromPath("/admin/question-reports");
        if (currentFilter != null) {
            target.queryParam("status", currentFilter.name());
        }
        int pageNumber = parsePage(page);
        if (pageNumber > 0) {
            target.queryParam("page", pageNumber);
        }
        return "redirect:" + target.toUriString();
    }

    private boolean isAdmin() {
        return session.getAttribute("user") instanceof CustomerBean bean && ADMIN.equals(bean.getPermission());
    }

    private static int parsePage(String value) {
        return value != null && value.matches("[0-9]{1,6}") ? Integer.parseInt(value) : 0;
    }
}