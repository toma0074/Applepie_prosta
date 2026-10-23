package jp.co.sss.pr.mypage_function;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;

/** サイドバーやマイページで使う、届いている友達申請の件数を全画面に渡す。 */
@ControllerAdvice
public class FriendNotificationAdvice {

    private final FriendConnectionRepository friendConnectionRepository;
    private final CustomerRepository customerRepository;

    public FriendNotificationAdvice(FriendConnectionRepository friendConnectionRepository,
                                    CustomerRepository customerRepository) {
        this.friendConnectionRepository = friendConnectionRepository;
        this.customerRepository = customerRepository;
    }

    @ModelAttribute("pendingFriendRequestCount")
    public long pendingFriendRequestCount(HttpSession session, HttpServletRequest request) {
        if (request.getRequestURI().contains("/api/")) {
            return 0;
        }
        Object user = session.getAttribute("user");
        if (!(user instanceof CustomerBean bean) || !Integer.valueOf(1).equals(bean.getPermission())) {
            return 0;
        }
        return customerRepository.findById(bean.getUserId())
                .map(friendConnectionRepository::countIncomingPending)
                .orElse(0L);
    }
}
