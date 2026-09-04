package jp.co.sss.pr.login_function;

import java.io.IOException;

import org.springframework.stereotype.Component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component

	public class LoginCheck extends HttpFilter{
		@Override
		public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws IOException, ServletException{
	 
		//URLを取得
		String rqURL = request.getRequestURI();
	  
		//ログインページとログインページからのアクセスはフィルタ処理外
		if(rqURL.endsWith("/") || rqURL.endsWith("/login") || rqURL.contains("/regist/") || rqURL.endsWith(".css")|| rqURL.indexOf("/images/")!=1) {
	 
			chain.doFilter(request, response);
	 
		}else {
	 
			//セッションからログイン情報取得
			HttpSession session = request.getSession();
	 
			//ログインされてなかったらエラー画面に遷移
			if(session.getAttribute("user") == null) {
	 
				response.sendRedirect("/prosta/login/error");
	 
				return;
				
		}else {
	 
				chain.doFilter(request, response);
		
		}
	
		}
		}
	
}
