package jp.co.sss.pr.question_function;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Favorite;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FavoriteRepository;
import jp.co.sss.pr.repository.QuestionRepository;

@Controller
public class QuestionController {

    @Autowired
    QuestionRepository qRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    HttpSession session;
    
    @Autowired
    FavoriteRepository favoriteRepository;

    @Autowired
    CustomerRepository customerRepository;

    @GetMapping("/question/bookmarks")
    public String filteredBookmarks(Model model, @RequestParam(required = false) Integer category,
            @RequestParam(required = false) Integer difficulty) {
        String view = bookmarks(model);
        if (view.startsWith("redirect:")) return view;
        @SuppressWarnings("unchecked")
        List<Question> all = (List<Question>) model.getAttribute("Questions");
        model.addAttribute("Questions", all.stream()
                .filter(q -> category == null || q.getCategory() != null && category.equals(q.getCategory().getCategoryId()))
                .filter(q -> difficulty == null || difficulty.equals(q.getDifficulty())).toList());
        model.addAttribute("categoryList", categoryRepository.findAll());
        model.addAttribute("bookmarkCategory", category);
        model.addAttribute("bookmarkDifficulty", difficulty);
        return view;
    }

    public String bookmarks(Model model) {
        CustomerBean user = (CustomerBean) session.getAttribute("user");
        if (user == null) {
            return "redirect:/";
        }
        List<Question> questions = favoriteRepository.findBookmarkedQuestions(user.getUserId());
        model.addAttribute("Questions", questions);
        model.addAttribute("QuestionResult", "ブックマーク一覧");
        model.addAttribute("isBookmarkList", true);
        Set<Integer> favoriteIds = new HashSet<>();
        for (Question question : questions) {
            favoriteIds.add(question.getQuestionId());
        }
        model.addAttribute("favoriteIds", favoriteIds);
        return "question/question_list";
    }

    // 問題一覧（全件検索）
    @RequestMapping(path = "/question")
    public String questionFindAll(Model model) {

        List<Question> questionList =
                qRepository.findAllByOrderByDifficultyAscCategoryAsc();

        model.addAttribute("Questions", questionList);
        model.addAttribute("QuestionResult", "全件検索");

        // ブックマーク情報を画面に渡す
        addFavoriteIds(model);

        return "question/question_list";
    }

    // カテゴリ別検索
    @RequestMapping(path = "/question/list/category")
    public String questionList(Model model, Integer category) {

        Category categoryEntity = category == null ? null : categoryRepository.findById(category).orElse(null);

        if (categoryEntity == null) {
            model.addAttribute("QuestionResult", "全件検索");
            model.addAttribute("Questions", qRepository.findAllByOrderByDifficultyAscCategoryAsc());
        } else {
            model.addAttribute("QuestionResult", categoryEntity.getCategoryName());
            model.addAttribute("Questions",
                    qRepository.findByCategoryOrderByDifficultyAscCategoryAsc(categoryEntity));
        }

        // ブックマーク情報を画面に渡す
        addFavoriteIds(model);

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

     // ブックマーク情報を画面に渡す
     addFavoriteIds(model);

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
        
     // ブックマーク情報を画面に渡す
        addFavoriteIds(model);
        
        return "question/question_list";
    }
    private void addFavoriteIds(Model model) {

        Set<Integer> favoriteIds = new HashSet<>();

        CustomerBean userBean =
            (CustomerBean) session.getAttribute("user");

        if (userBean != null) {

            Customer customer = customerRepository
                .findById(userBean.getUserId())
                .orElse(null);

            if (customer != null) {

                for (Favorite favorite :
                        favoriteRepository.findByCustomer(customer)) {

                    if (favorite.getQuestion() != null) {
                        favoriteIds.add(
                            favorite.getQuestion().getQuestionId()
                        );
                    }
                }
            }
        }

        model.addAttribute("favoriteIds", favoriteIds);
    }
}
