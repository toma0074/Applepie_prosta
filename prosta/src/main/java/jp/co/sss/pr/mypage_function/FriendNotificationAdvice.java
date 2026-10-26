package jp.co.sss.pr.mypage_function;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;
import jp.co.sss.pr.repository.UserAchievementRepository;

/** サイドバーやマイページで使う、届いている友達申請の件数を全画面に渡す。 */
@ControllerAdvice
public class FriendNotificationAdvice {

    private final FriendConnectionRepository friendConnectionRepository;
    private final CustomerRepository customerRepository;
    private final ObjectProvider<UserAchievementRepository> userAchievementRepository;

    public FriendNotificationAdvice(FriendConnectionRepository friendConnectionRepository,
                                    CustomerRepository customerRepository,
                                    ObjectProvider<UserAchievementRepository> userAchievementRepository) {
        this.friendConnectionRepository = friendConnectionRepository;
        this.customerRepository = customerRepository;
        this.userAchievementRepository = userAchievementRepository;
    }

    /** 実績一覧をまだ開いていない新規バッジの件数。 */
    @ModelAttribute("unreadAchievementCount")
    public long unreadAchievementCount(HttpSession session, HttpServletRequest request) {
        UserAchievementRepository repo = userAchievementRepository.getIfAvailable();
        if (repo == null || request.getRequestURI().contains("/api/")
                || !(session.getAttribute("user") instanceof CustomerBean bean)) {
            return 0;
        }
        return customerRepository.findById(bean.getUserId())
                .map(repo::countByCustomerAndSeenFalse)
                .orElse(0L);
    }

    @ModelAttribute("pendingFriendRequestCount")
    public long pendingFriendRequestCount(HttpSession session, HttpServletRequest request) {
        if (request.getRequestURI().contains("/api/")) {
            return 0;
        }
        Object user = session.getAttribute("user");
        if (!(user instanceof CustomerBean bean) || !Integer.valueOf(0).equals(bean.getPermission())) {
            return 0;
        }
        return customerRepository.findById(bean.getUserId())
                .map(friendConnectionRepository::countIncomingPending)
                .orElse(0L);
    }
}
