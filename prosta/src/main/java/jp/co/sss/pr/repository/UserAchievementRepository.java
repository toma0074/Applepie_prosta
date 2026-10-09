package jp.co.sss.pr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.sss.pr.entity.Achievement;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.UserAchievement;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Integer> {

    List<UserAchievement> findByCustomerOrderByUnlockedDateDesc(Customer customer);

    Optional<UserAchievement> findByCustomerAndAchievement(Customer customer, Achievement achievement);

    boolean existsByCustomerAndAchievement(Customer customer, Achievement achievement);
}
