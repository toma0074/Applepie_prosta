package jp.co.sss.pr.user_function;

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
public class UserController {

	@Autowired
	CustomerRepository userRepository;

	@Autowired
	HttpSession session;

	@RequestMapping(path = "/user/update/input", method = RequestMethod.GET)
	public String userUpdateInput(Model model, CustomerForm userForm) {
	    CustomerBean user = new CustomerBean();
	    
	    if(userForm.getUserId() == null) {
	    	 user=(CustomerBean)session.getAttribute("user");
	    }else {
	    	BeanUtils.copyProperties(userForm, user);
	    }
	   
	    model.addAttribute("user", user);

	    return "user/user_update_input"; 
	}

	@RequestMapping(path = "/user/update/check", method = RequestMethod.POST)
	public String userUpdateCheck(@Valid @ModelAttribute("user") CustomerForm user, BindingResult result, Model model) {
		Customer customer = userRepository.findByUserName(user.getUserName());

	    if(result.hasErrors()) {
	        return "user/user_update_input";
	        
	    }else if(customer != null && customer.getUserId() != user.getUserId()) {
	
	    	model.addAttribute("err","既に使用されているユーザー名です。");
	    	return "user/user_update_input";
	    }
	    

        return "user/user_update_check"; 
	}
	
	@RequestMapping(path = "/user/update/complete",method = RequestMethod.POST)
	public String userUpdateComplete(CustomerForm userForm, Model model) {

		CustomerBean user = (CustomerBean)session.getAttribute("user");	
		Customer user2 = new Customer();
		
		if(userForm.getUserId()==user.getUserId()) {
			
			BeanUtils.copyProperties(userForm,user);
			session.setAttribute("user", user);
	
		}
		
		BeanUtils.copyProperties(userForm,user2);
		userRepository.save(user2);
		
		model.addAttribute("pm",user.getPermission());

		return "user/user_update_complete";
	}
}