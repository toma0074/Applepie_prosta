package jp.co.sss.pr.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "study_plan")
public class StudyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_study_plan_gen")
    @SequenceGenerator(name = "seq_study_plan_gen", sequenceName = "seq_study_plan", allocationSize = 1)
    @Column(name = "study_plan_id")
    private Integer studyPlanId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Customer customer;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "details", length = 500)
    private String details;

    public Integer getStudyPlanId() {
        return studyPlanId;
    }

    public void setStudyPlanId(Integer studyPlanId) {
        this.studyPlanId = studyPlanId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
