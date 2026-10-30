package jp.co.sss.pr.question_function;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.QuestionRepository;

@Controller
public class QuestionController {

    @Autowired
    QuestionRepository qRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    HttpSession session;

    // 問題一覧（全件検索）
    @RequestMapping(path = "/question")
    public String questionFindAll(Model model) {

        List<Question> questionList =
                qRepository.findAllByOrderByDifficultyAscCategoryAsc();

        model.addAttribute("Questions", questionList);
        model.addAttribute("QuestionResult", "全件検索");

        return "question/question_list";
    }

    // カテゴリ別検索
    @RequestMapping(path = "/question/list/category")
    public String questionList(Model model, Integer category) {

        String resultText = "";
        if (category == null) {
            resultText = "全件検索";
        } else if (category == 1) {
            resultText = "Java";
        } else if (category == 2) {
            resultText = "Spring";
        } else if (category == 3) {
            resultText = "HTML";
        } else {
            resultText = "SQL";
        }

        model.addAttribute("QuestionResult", resultText);

        if (category == null) {
            List<Question> questionList =
                    qRepository.findAllByOrderByDifficultyAscCategoryAsc();
            model.addAttribute("Questions", questionList);
        } else {
            Category categoryEntity =
                    categoryRepository.getReferenceById(category);

            List<Question> categoryList =
                    qRepository.findByCategoryOrderByDifficultyAscCategoryAsc(
                            categoryEntity);

            model.addAttribute("Questions", categoryList);
        }

        return "question/question_list";
    }

    // 問題文のあいまい検索
    @RequestMapping(path = "/question/list/text")
    public String questionText(Model model, String questionText) {

        List<Question> textList;

        if (questionText == null || questionText.isEmpty()) {
            textList = qRepository.findAllByOrderByDifficultyAscCategoryAsc();
            model.addAttribute("QuestionResult", "全件検索");
        } else {
            textList =
                    qRepository
                    .findByQuestionTextContainingOrderByDifficultyAscCategoryAsc(
                            questionText);

            model.addAttribute("QuestionResult", questionText);
        }

        model.addAttribute("Questions", textList);

        return "question/question_list";
    }

    // 難易度別検索
    @RequestMapping(path = "/question/list/difficulty")
    public String questionResult(Model model, Integer difficulty) {

        String resultd = "";
        if (difficulty == null) {
            resultd = "全件検索";
        } else if (difficulty == 1) {
            resultd = "★☆☆";
        } else if (difficulty == 2) {
            resultd = "★★☆";
        } else {
            resultd = "★★★";
        }

        model.addAttribute("QuestionResult", resultd);

        List<Question> difList;

        if (difficulty == null) {
            difList = qRepository.findAllByOrderByDifficultyAscCategoryAsc();
        } else {
            difList =
                    qRepository.findByDifficultyOrderByCategoryAsc(difficulty);
        }

        model.addAttribute("Questions", difList);

        return "question/question_list";
    }
}

