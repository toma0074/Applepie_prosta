package jp.co.sss.pr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.co.sss.pr.entity.Customer;


public interface CustomerRepository extends JpaRepository<Customer, Integer> {

		
		Customer findByUserNameAndUserPass(String userName, String userPass);
		
		Customer findByUserName(String userName);
		
		List<Customer> findByDeleteFlag(Integer deleteFlag);
		
		List<Customer> findByPermission(Integer permission);
		
		List<Customer> findByUserNameContaining(String userName);
		
		Customer findByUserId(Integer userId);
		
	
}
