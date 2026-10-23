package jp.co.sss.pr.mypage_function;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.service.ChatService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final int MAX_MESSAGE_LENGTH = 200;

    private final ChatService chatService;
    private final CustomerRepository customerRepository;

    public ChatController(ChatService chatService, CustomerRepository customerRepository) {
        this.chatService = chatService;
        this.customerRepository = customerRepository;
    }

    public record ChatRequest(String message) {}

    @PostMapping
    public ChatService.Reply chat(@RequestBody ChatRequest request,
                                  HttpSession session, HttpServletRequest http) {
        CustomerBean bean = (CustomerBean) session.getAttribute("user");
        if (bean == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ログインしてください");
        }
        String message = request == null || request.message() == null ? "" : request.message().trim();
        if (message.isEmpty() || message.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "メッセージは1〜" + MAX_MESSAGE_LENGTH + "文字で入力してください");
        }
        Customer user = customerRepository.findById(bean.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ユーザーが見つかりません"));
        return chatService.reply(user, message, http.getContextPath());
    }
}
