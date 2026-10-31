package jp.co.sss.pr.question_function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Favorite;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.repository.FavoriteRepository;
import jp.co.sss.pr.repository.QuestionRepository;

/**
 * 問題のブックマーク(後で見返す印)を付け外しするコントローラー。
 * 画面表記は「ブックマーク」だが、DBテーブル・URL・クラス名は互換性のため favorite のまま。
 */
@Controller
public class FavoriteController {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private HttpSession session;

    /** 登録済みなら解除、未登録なら追加する(☆⇔★の切り替え)。 */
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

        // 同じユーザーの連打で「存在確認→追加」が並行し一意制約違反(500)にならないよう、ユーザー行をロックして直列化する
        Customer customer = entityManager.find(
            Customer.class, userBean.getUserId(), LockModeType.PESSIMISTIC_WRITE);

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