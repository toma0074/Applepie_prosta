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

/**
 * ユーザーが解除した実績を記録するEntity
 */
/**
 * ユーザーが解除した実績を記録するEntity
 */
@Entity
@Table(name = "user_achievement")
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_user_ach_gen")
    @SequenceGenerator(name = "seq_user_ach_gen", sequenceName = "seq_user_achievement", allocationSize = 1)
    private Integer userAchId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "achievement_id")
    private Achievement achievement;

    @Column(nullable = false)
    private LocalDate unlockedDate;

    // --- getters / setters ---

    public Integer getUserAchId() {
        return userAchId;
    }

    public void setUserAchId(Integer userAchId) {
        this.userAchId = userAchId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Achievement getAchievement() {
        return achievement;
    }

    public void setAchievement(Achievement achievement) {
        this.achievement = achievement;
    }

    public LocalDate getUnlockedDate() {
        return unlockedDate;
    }

    public void setUnlockedDate(LocalDate unlockedDate) {
        this.unlockedDate = unlockedDate;
    }
}
