package jp.co.sss.pr.challenge_function;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Achievement;
import jp.co.sss.pr.form.ChallengeAnswerForm;
import jp.co.sss.pr.form.ChallengeCreateForm;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.service.AfterCommit;
import jp.co.sss.pr.service.ChallengeService;
import jp.co.sss.pr.service.ChallengeService.ChallengeException;

/** 非同期フレンド対戦の画面。ログイン確認は既存フィルターに任せ、ここでは一般ユーザーか確認する。 */
@Controller
public class ChallengeController {

    static final String NOTICE = "challengeNotice";
    static final String ACHIEVEMENTS = "challengeAchievements";

    private final ChallengeService challengeService;
    private final CategoryRepository categoryRepository;

    public ChallengeController(ChallengeService challengeService, CategoryRepository categoryRepository) {
        this.challengeService = challengeService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/challenges")
    public String index(HttpSession session, Model model) {
        Integer userId = generalUserId(session);
        if (userId == null) {
            return redirectFor(session);
        }
        if (!model.containsAttribute("challengeCreateForm")) {
            ChallengeCreateForm form = new ChallengeCreateForm();
            form.setDifficulty(1);
            form.setQuestionCount(5);
            model.addAttribute("challengeCreateForm", form);
        }
        populateIndex(userId, model);
        return "challenge/index";
    }

    @PostMapping("/challenges")
    public String create(@Validated @ModelAttribute("challengeCreateForm") ChallengeCreateForm form,
            BindingResult result, HttpSession session, Model model, RedirectAttributes redirect) {
        Integer userId = generalUserId(session);
        if (userId == null) {
            return redirectFor(session);
        }
        if (result.hasErrors()) {
            String message = result.getFieldErrors().stream()
                    .map(error -> error.isBindingFailure() ? "入力値が不正です。" : error.getDefaultMessage())
                    .findFirst().orElse("入力値が不正です。");
            model.addAttribute("errorMessage", message);
            populateIndex(userId, model);
            return "challenge/index";
        }
        try {
            Integer challengeId = challengeService.create(userId, form);
            redirect.addFlashAttribute("noticeMessage", "対戦を作成しました。都合のよいときに解答しましょう。");
            return "redirect:/challenges/" + challengeId;
        } catch (ChallengeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            populateIndex(userId, model);
            return "challenge/index";
        }
    }

    @GetMapping("/challenges/{challengeId}")
    public String detail(@PathVariable Integer challengeId, HttpSession session, Model model) {
        Integer userId = generalUserId(session);
        if (userId == null) {
            return redirectFor(session);
        }
        Object notice = session.getAttribute(NOTICE);
        if (notice != null) {
            model.addAttribute("noticeMessage", notice);
            session.removeAttribute(NOTICE);
        }
        Object achievements = session.getAttribute(ACHIEVEMENTS);
        if (achievements != null) {
            model.addAttribute("newAchievements", achievements);
            session.removeAttribute(ACHIEVEMENTS);
        }
        model.addAttribute("challenge", challengeService.view(challengeId, userId));
        if (!model.containsAttribute("selected")) {
            model.addAttribute("selected", Map.of());
        }
        return "challenge/detail";
    }

    @PostMapping("/challenges/{challengeId}/submit")
    public String submit(@PathVariable Integer challengeId,
            @RequestParam Map<String, String> params, HttpSession session, Model model,
            RedirectAttributes redirect) {
        Integer userId = generalUserId(session);
        if (userId == null) {
            return redirectFor(session);
        }
        ChallengeAnswerForm form = ChallengeAnswerForm.fromParameters(params);
        try {
            ChallengeService.SubmitResult submitted = challengeService.submit(challengeId, userId, form);
            String message = "提出しました。正解数は " + submitted.correctCount() + " / "
                    + submitted.totalCount() + " 問です。";
            List<Achievement> unlocked = submitted.newAchievements();
            AfterCommit.run(() -> {
                session.setAttribute(NOTICE, message);
                if (!unlocked.isEmpty()) {
                    session.setAttribute(ACHIEVEMENTS, unlocked);
                }
            });
            return "redirect:/challenges/" + challengeId;
        } catch (ChallengeService.AlreadySubmittedException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/challenges/" + challengeId;
        } catch (ChallengeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("selected", form.getAnswers());
            model.addAttribute("challenge", challengeService.view(challengeId, userId));
            return "challenge/detail";
        }
    }

    private void populateIndex(Integer userId, Model model) {
        model.addAttribute("friendList", challengeService.acceptedFriends(userId));
        model.addAttribute("categoryList", categoryRepository.findAll());
        model.addAttribute("questionCounts", List.of(5, 10, 15));
        model.addAttribute("challengeList", challengeService.listFor(userId));
    }

    private Integer generalUserId(HttpSession session) {
        if (!(session.getAttribute("user") instanceof CustomerBean user)) {
            return null;
        }
        if (!Integer.valueOf(0).equals(user.getPermission())) {
            return null;
        }
        return user.getUserId();
    }

    private String redirectFor(HttpSession session) {
        return session.getAttribute("user") == null ? "redirect:/" : "redirect:/mypage";
    }
}