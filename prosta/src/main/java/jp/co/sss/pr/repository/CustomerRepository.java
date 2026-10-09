package jp.co.sss.pr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.sss.pr.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Customer findByUserNameAndUserPass(String userName, String userPass);

    Customer findByUserName(String userName);

    List<Customer> findByDeleteFlag(Boolean deleteFlag);

    List<Customer> findByPermission(Integer permission);

    List<Customer> findByUserNameContaining(String userName);

    Customer findByUserId(Integer userId);
}
