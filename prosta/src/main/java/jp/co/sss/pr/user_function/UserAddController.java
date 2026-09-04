package jp.co.sss.pr.user_function;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.form.CustomerForm;
import jp.co.sss.pr.repository.CustomerRepository;

@Controller
public class UserAddController {

	@Autowired
	HttpSession session;

	@Autowired
	CustomerRepository userRepository;

	//ユーザー一覧表示
	@RequestMapping(path="/user/list", method=RequestMethod.GET)
	public String userListshow(Model model) {

		List<Customer>userList = userRepository.findAll();

		model.addAttribute("customerForm",userList);

		return "user/user_list";
	}

	//ユーザー名あいまい検索
	@RequestMapping(path="/user/list/text", method=RequestMethod.POST)
	public String userTextSearch(Model model,String userName) {

		List<Customer>userList = userRepository.findByUserNameContaining(userName);

		model.addAttribute("customerForm",userList);

		return "user/user_list";
	}

	//権限検索
	@RequestMapping(path="/user/list/permission", method=RequestMethod.POST)
	public String userPermissionSearch(Model model,Integer permission) {

		List<Customer>userList = userRepository.findByPermission(permission);

		model.addAttribute("customerForm",userList);

		return "user/user_list";
	}


	//削除済みユーザー検索
	@RequestMapping(path="/user/list/delete", method=RequestMethod.POST)
	public String userDeleteSearch(Model model,Integer deleteFlag) {

		List<Customer>userList = userRepository.findByDeleteFlag(deleteFlag);

		model.addAttribute("customerForm",userList);

		return "user/user_list";
	}

	//ユーザー削除確認
	@RequestMapping(path="/user/delete/check", method=RequestMethod.POST)
	public String userDeleteCheck(@ModelAttribute CustomerForm customerForm,Model model) {

		CustomerBean loginUser = (CustomerBean)session.getAttribute("user");

		if(customerForm.getUserId() == loginUser.getUserId()) {
			
			model.addAttribute("errorMessage","ログイン中のユーザーは操作できません");
			model.addAttribute("back","一覧に戻る");

			return "user/user_list";
			
		}else if(customerForm.getDeleteFlag() == 1) {

			model.addAttribute("errorMessage","選択したユーザーは削除済みです");
			model.addAttribute("back","一覧に戻る");

			return "user/user_list";
		}
		
		model.addAttribute("flag", "このユーザーを削除しますか?");
		return "user/user_delete_check";
	}
	
	//ユーザー復元確認
	@RequestMapping(path="/user/restore/check", method=RequestMethod.POST)
	public String userRestoreCheck(@ModelAttribute CustomerForm customerForm,Model model) {

		CustomerBean loginUser = (CustomerBean)session.getAttribute("user");

		if(customerForm.getUserId() == loginUser.getUserId()) {
			
			model.addAttribute("errorMessage","ログイン中のユーザーは操作できません");
			model.addAttribute("back","一覧に戻る");

			return "user/user_list";
			
		}else if(customerForm.getDeleteFlag() == 0) {

			model.addAttribute("errorMessage","選択したユーザーはアクティブです");
			model.addAttribute("back","一覧に戻る");

			return "user/user_list";
		}
		
		model.addAttribute("flag", "このユーザーを復元しますか?");

		return "user/user_delete_check";
	}

	//ユーザー削除/復元完了
	@RequestMapping(path="/user/delete/complete",method = RequestMethod.POST)
	public String userDeleteComp(Model model, Integer userId) {

		Customer user = userRepository.findByUserId(userId);
		String m="";
		
		switch(user.getDeleteFlag()) {
			
		case 0:
			user.setDeleteFlag(1);
			m = "さんを削除しました";
			break;
		
		case 1:
			user.setDeleteFlag(0);
			m = "さんを復元しました";
			break;
		}

		userRepository.save(user);

		model.addAttribute("userName",user.getUserName() + m);

		return "user/user_delete_complete";
	}

}
