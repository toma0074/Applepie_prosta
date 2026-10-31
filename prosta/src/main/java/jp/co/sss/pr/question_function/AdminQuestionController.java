package jp.co.sss.pr.question_function;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.form.QuestionForm;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.FavoriteRepository;
import jp.co.sss.pr.repository.QuestionAttemptStat;
import jp.co.sss.pr.repository.QuestionRepository;
import jp.co.sss.pr.service.ReturnUrls;

/** 管理者用の問題管理（アクセス制御は LoginCheck / AccessPolicy の /admin/ 配下） */
@Controller
public class AdminQuestionController {

    static final int PAGE_SIZE = 10;

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    AnswerRepository answerRepository;

    @Autowired
    FavoriteRepository favoriteRepository;

    /** 一覧の1行分（問題と全体の解答統計） */
    public record Row(Question question, long attempts, long correct) {
        public int rate() {
            return attempts == 0 ? 0 : (int) Math.round(correct * 100.0 / attempts);
        }
    }

    @GetMapping("/admin/questions")
    public String list(@RequestParam(required = false) Integer categoryId,
                       @RequestParam(required = false) Integer difficulty,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "false") boolean noExplanation,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        String kw = keyword == null ? "" : keyword.trim();
        Page<Question> result = questionRepository.search(categoryId, difficulty,
                "%" + kw + "%", noExplanation, PageRequest.of(Math.max(page, 0), PAGE_SIZE));

        Map<Integer, QuestionAttemptStat> stats = new HashMap<>();
        List<Integer> ids = result.getContent().stream().map(q -> q.getQuestionId()).toList();
        if (!ids.isEmpty()) {
            for (QuestionAttemptStat s : answerRepository.summarizeByQuestionIds(ids)) {
                stats.put(s.getQuestionId(), s);
            }
        }
        List<Row> rows = result.getContent().stream().map(q -> {
            QuestionAttemptStat s = stats.get(q.getQuestionId());
            return new Row(q, s == null ? 0 : s.getAttemptCount(), s == null ? 0 : s.getCorrectCount());
        }).toList();

        model.addAttribute("rows", rows);
        model.addAttribute("page", result);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("keyword", kw);
        model.addAttribute("noExplanation", noExplanation);
        return "admin/question_list";
    }

    @GetMapping("/admin/questions/new")
    public String newForm(@RequestParam(required = false) String returnUrl, Model model) {
        QuestionForm form = new QuestionForm();
        form.setReturnUrl(returnUrl);
        form.setDifficulty(1);
        form.setCorrectOption(1);
        return form(model, form, true);
    }

    @GetMapping("/admin/questions/{id}/edit")
    public String editForm(@PathVariable Integer id,
                           @RequestParam(required = false) String returnUrl, Model model) {
        Question q = find(id);
        QuestionForm form = new QuestionForm();
        form.setReturnUrl(returnUrl);
        form.setQuestionId(q.getQuestionId());
        form.setCategory(q.getCategory() == null ? null : q.getCategory().getCategoryId());
        form.setDifficulty(q.getDifficulty());
        form.setQuestionText(q.getQuestionText());
        form.setOptionA(q.getOptionA());
        form.setOptionB(q.getOptionB());
        form.setOptionC(q.getOptionC());
        form.setCorrectOption(q.getCorrectOption());
        form.setExplanation(q.getExplanation());
        return form(model, form, false);
    }

    @PostMapping("/admin/questions/save")
    public String save(@Valid @ModelAttribute("questionForm") QuestionForm form,
                       BindingResult result, Model model, RedirectAttributes redirect) {
        boolean creating = form.getQuestionId() == null;
        Category category = form.getCategory() == null ? null
                : categoryRepository.findById(form.getCategory()).orElse(null);
        if (form.getCategory() != null && category == null) {
            result.rejectValue("category", "invalid", "カテゴリーが存在しません");
        }
        if (result.hasErrors()) {
            return form(model, form, creating);
        }

        Question q = creating ? new Question() : find(form.getQuestionId());
        q.setCategory(category);
        q.setDifficulty(form.getDifficulty());
        q.setQuestionText(form.getQuestionText());
        q.setOptionA(form.getOptionA());
        q.setOptionB(form.getOptionB());
        q.setOptionC(form.getOptionC());
        q.setCorrectOption(form.getCorrectOption());
        String explanation = form.getExplanation() == null ? "" : form.getExplanation().trim();
        q.setExplanation(explanation.isEmpty() ? null : explanation);
        try {
            questionRepository.save(q);
            questionRepository.flush();
        } catch (DataIntegrityViolationException e) {
            if (creating) {
                result.reject("saveFailed",
                        "問題の登録に失敗しました。連番の不整合が疑われます。管理者にSEQ_QUESTIONの値を確認してください。");
            } else {
                result.reject("saveFailed", "問題の更新に失敗しました。入力値を確認して再実行してください。");
            }
            return form(model, form, creating);
        }

        redirect.addFlashAttribute("message", creating ? "問題を登録しました。" : "問題を更新しました。");
        return "redirect:" + ReturnUrls.resolve(form.getReturnUrl(), "/admin/questions");
    }

    @PostMapping("/admin/questions/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirect) {
        Question q = find(id);
        if (answerRepository.existsByQuestion(q) || favoriteRepository.existsByQuestion(q)) {
            redirect.addFlashAttribute("error",
                    "解答履歴またはブックマークに使われている問題は削除できません。");
            return "redirect:/admin/questions";
        }
        try {
            questionRepository.delete(q);
            questionRepository.flush();
            redirect.addFlashAttribute("message", "問題を削除しました。");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error", "他のデータから参照されているため削除できません。");
        }
        return "redirect:/admin/questions";
    }

    private String form(Model model, QuestionForm form, boolean creating) {
        model.addAttribute("returnUrl", ReturnUrls.resolve(form.getReturnUrl(), "/admin/questions"));
        model.addAttribute("questionForm", form);
        model.addAttribute("creating", creating);
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/question_form";
    }

    private Question find(Integer id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
