// 【新規開始】2026/10/01

package jp.co.sss.pr.login_function;

import java.time.LocalDateTime;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.CustomerRepository;


@RestController
public class LfApiCtrl {
	
	@Autowired
    private CustomerRepository customerRepository;

    /**
     *  JavaScriptから5分おきに自動で呼ばれるフラグ判定API
     */
    @PostMapping("/api/login-status/heartbeat")
    public ResponseEntity<Void> handleHeartbeat(HttpSession session) {
        // セッションからログインユーザーを取得
        CustomerBean loginUser = (CustomerBean) session.getAttribute("user");
        
        if (loginUser != null) {
            // ユーザーが現在もブラウザを開いて操作している場合
            Customer customer = customerRepository.findById(loginUser.getUserId()).orElse(null);
            if (customer != null) {
                customer.setLoginDate(LocalDateTime.now()); // 現在時刻で更新
                customer.setLoginFlag(1); // ログイン中を維持
                customerRepository.save(customer);
            }
            return ResponseEntity.ok().build(); // 200 OK を返す
        }
        
        // ログインしていない（セッション切れ）なら 401 Unauthorized を返す
        return ResponseEntity.status(401).build();
    }
}

//【新規終了】2026/10/01