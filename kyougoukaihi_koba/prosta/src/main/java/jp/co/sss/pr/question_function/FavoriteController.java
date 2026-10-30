package jp.co.sss.pr.question_function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Favorite;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FavoriteRepository;
import jp.co.sss.pr.repository.QuestionRepository;

@Controller
public class FavoriteController {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private HttpSession session;

    @Transactional
    @PostMapping("/favorite/toggle")
    public String toggleFavorite(
            @RequestParam("questionId") Integer questionId,
            RedirectAttributes redirectAttributes) {

        CustomerBean userBean =
            (CustomerBean) session.getAttribute("user");

        if (userBean == null) {
            return "redirect:/";
        }

        Customer customer = customerRepository
            .findById(userBean.getUserId())
            .orElse(null);

        Question question = questionRepository
            .findById(questionId)
            .orElse(null);

        if (customer == null || question == null) {
            redirectAttributes.addFlashAttribute(
                "error", "対象の問題が見つかりません。");
            return "redirect:/question";
        }

        boolean exists = favoriteRepository
            .existsByCustomerAndQuestion(customer, question);

        if (exists) {
            favoriteRepository
                .deleteByCustomerAndQuestion(customer, question);
        } else {
            Favorite favorite = new Favorite();
            favorite.setCustomer(customer);
            favorite.setQuestion(question);
            favoriteRepository.save(favorite);
        }

        return "redirect:/question";
    }
}