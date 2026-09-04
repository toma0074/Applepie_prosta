package jp.co.sss.pr.login_function;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.form.CustomerForm;
import jp.co.sss.pr.repository.CustomerRepository;

@Controller
public class LoginController {
	@Autowired
	CustomerRepository customerRepository;

	@Autowired
	HttpSession session;
	
    //最初画面
	@RequestMapping(path = "/", method = RequestMethod.GET)
	public String index(@ModelAttribute CustomerForm customerform) {
		session.invalidate();
		return "login/index";
	}
	
	// ログイン画面を表示する（GETリクエスト用）
		@RequestMapping(path = "/login", method = RequestMethod.GET)
		public String loginInput(@ModelAttribute CustomerForm customerForm) {
			// ログイン画面を表示するだけなので、対象のHTML（login/login）を返す
			return "login/login";
	}
		
	//login page
	@RequestMapping(path = "/login", method = RequestMethod.POST)
	public String login(@Valid @ModelAttribute CustomerForm customerForm,BindingResult result, HttpSession session, Model model) {
		
		if(result.hasErrors()) {
			return "login/login";
		}
		
		String userName = customerForm.getUserName();
		String userPass = customerForm.getUserPass();
		Customer customer = customerRepository.findByUserNameAndUserPass(userName,userPass);

		if (customer != null && customer.getDeleteFlag()==0) {
			CustomerBean customerbean = new CustomerBean();
			customerbean.setUserId(customer.getUserId());
			customerbean.setUserName(customer.getUserName());
			customerbean.setUserPass(customer.getUserPass());
			customerbean.setPermission(customer.getPermission());
			customerbean.setDeleteFlag(customer.getDeleteFlag());
			session.setAttribute("user", customerbean);
			// 一覧へリダイレクト
			return "redirect:/mypage";

		} else {
			model.addAttribute("errMessage", "ユーザー名、またはパスワードが間違っています。");
			return "login/login";
		}
	}
	
	//logout
	@RequestMapping(path = "/logout", method = RequestMethod.GET)
	public String logout() {
		// セッションの破棄
		session.invalidate();
		return "redirect:/";
	}

	//新規登録
	@RequestMapping(path="/regist",method=RequestMethod.GET)
	public String inputRegist(@ModelAttribute CustomerForm customerForm) {

		return "login/user_regist";
	}
	
	//入力チェック
	@RequestMapping(path="/regist/check",method=RequestMethod.POST)
	public String checkRegist(@Valid @ModelAttribute CustomerForm customerForm, BindingResult result,Model model) {
		
		Customer logCustomer = customerRepository.findByUserName(customerForm.getUserName());
		
		if(result.hasErrors()) {
			
			return "login/user_regist";
			
		}else if(logCustomer != null) {
			
			model.addAttribute("errMessage", "既に使用されているユーザー名です。");
			
			return "login/user_regist";
			
		}
		// エラーがなければ、Entityに詰め替え
			Customer customer = new Customer();
			BeanUtils.copyProperties(customerForm, customer);
			
			customer.setPermission(1); // 権限を一般ユーザーの「1」に設定
			customer.setDeleteFlag(0); // 削除フラグ（未削除=0）も入れる
			
		//初期値が入った状態でDB登録を実行する
			customer = customerRepository.save(customer);
					
		// セッションに保存する型をEntityからBeanに変換する
			CustomerBean customerbean = new CustomerBean();
			
		// コメントアウトを解除して、新しく発行されたuserIdもしっかりBeanに引き継ぎます
			customerbean.setUserId(customer.getUserId()); 
			customerbean.setUserName(customer.getUserName());
			customerbean.setUserPass(customer.getUserPass());	
			customerbean.setPermission(customer.getPermission());
        // セッション保存
        session.setAttribute("user", customerbean);
     			
		return "redirect:/mypage";		
	}
	
	@RequestMapping(path = "/login/error", method = RequestMethod.GET)
	public String loginError(@ModelAttribute CustomerForm customerform) {
		session.invalidate();
		return "login_error";
	}
	
	
	
}
