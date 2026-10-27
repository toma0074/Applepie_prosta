package jp.co.sss.pr.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.StudyPlan;

@Repository
public interface StudyPlanRepository extends JpaRepository<StudyPlan, Integer> {

    List<StudyPlan> findAllByCustomerAndPlanDateBetweenOrderByPlanDateAscStudyPlanIdAsc(
            Customer customer, LocalDate from, LocalDate to);

    List<StudyPlan> findAllByCustomerAndPlanDateOrderByStudyPlanIdAsc(
            Customer customer, LocalDate planDate);

    Optional<StudyPlan> findByStudyPlanIdAndCustomer(Integer studyPlanId, Customer customer);
}
