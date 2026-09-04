package jp.co.sss.pr.mypage_function;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CustomerRepository;

@Controller
public class MypageController {
	
	@Autowired
	AnswerRepository answerRepository;
	@Autowired
	CustomerRepository customerRepository;
	
	
	@RequestMapping(path = "/mypage",method = RequestMethod.GET )
	public String mypage(Model model,HttpSession session, @RequestParam(name="sort", required=false)Integer sort) {
		
		//セッションからは「CustomerBean」型としてデータを取り出す
		CustomerBean userBean=(CustomerBean)session.getAttribute("user");
		
		if (userBean == null) {
			
			return "redirect:/";
			
		}
		//AnswerRepositoryの検索用として、BeanのIDからEntity（Customer）を取得する
		Customer user = customerRepository.getReferenceById(userBean.getUserId());
		
		// 履歴を取得
		if(sort == null|| sort == 1) {
			sort = 1;
			List<Answer> ansList = answerRepository.findAllByCustomerOrderByAnsIdDesc(user);
			model.addAttribute("answerlist", ansList);
			
		}else if(sort == 2) {
			
			List<Answer> ansList = answerRepository.findAllByCustomerOrderByAnsIdAsc(user);
			model.addAttribute("answerlist", ansList);
			model.addAttribute("sort",2);
		}
		
		
		return "mypage/mypage";
	}
}
