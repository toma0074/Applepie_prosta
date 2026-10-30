package jp.co.sss.pr.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity

// heartbeatやプロフィール保存で、変更していないEXP・認証トークン等を古い値で上書きしない。
// 更新列の限定だけでは同時加算を守れないため、加算・認証には別途顧客行ロックも使用する。
@DynamicUpdate
@Table(name = "customer")

public class Customer {

    @Id

    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_user_gen")

    @SequenceGenerator(name = "seq_user_gen", sequenceName = "seq_user", allocationSize = 1)

    @Column(name = "user_id")

    private Integer userId;

    @Column(name = "user_name", nullable = false, length = 20)

    private String userName;

    @Column(name = "user_pass", nullable = false, length = 100)

    private String userPass;

    @Column(name = "permission", nullable = false)

    private Integer permission;

    @Column(name = "delete_flag", nullable = false)

    private Boolean deleteFlag;

    @Column(name = "login_date")

    private LocalDate loginDate;

    @Column(name = "login_count")

    private Integer loginCount;

    @Column(name = "login_flag")

    private Boolean loginFlag;

    @Column(name = "user_level")

    private Integer userLevel;
 
    @Column(name = "consecutive_login_days", nullable = false)

    private Integer consecutiveLoginDays = 0;
 
    @Column(name = "last_seen_at")

    private LocalDateTime lastSeenAt;
 
    @Column(name = "perfect_test_count", nullable = false)
    private Integer perfectTestCount = 0;

    @Column(name = "total_exp", nullable = false)
    private Long totalExp = 0L;

 
    

    @Column(name = "background_class", length = 32)

    private String backgroundClass;
 
    @Column(name = "background_color", length = 7)

    private String backgroundColor;
 
    @Column(name = "background_seen_level", nullable = false)
    private Integer backgroundSeenLevel = 1;

    @Column(name = "active_login_token", length = 36)
    private String activeLoginToken;

    public String getActiveLoginToken() { return activeLoginToken; }
    public void setActiveLoginToken(String activeLoginToken) { this.activeLoginToken = activeLoginToken; }

    public Integer getUserId() { return userId; }

    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserName() { return userName; }

    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPass() { return userPass; }

    public void setUserPass(String userPass) { this.userPass = userPass; }

    public Integer getPermission() { return permission; }

    public void setPermission(Integer permission) { this.permission = permission; }

    public Boolean getDeleteFlag() { return deleteFlag; }

    public void setDeleteFlag(Boolean deleteFlag) { this.deleteFlag = deleteFlag; }

    public LocalDate getLoginDate() { return loginDate; }

    public void setLoginDate(LocalDate loginDate) { this.loginDate = loginDate; }

    public Integer getLoginCount() { return loginCount == null ? 0 : loginCount; }

    public void setLoginCount(Integer loginCount) { this.loginCount = loginCount; }

    public Boolean getLoginFlag() { return loginFlag == null ? false : loginFlag; }

    public void setLoginFlag(Boolean loginFlag) { this.loginFlag = loginFlag; }

    public Integer getUserLevel() { return userLevel == null ? 1 : userLevel; }

    public void setUserLevel(Integer userLevel) { this.userLevel = userLevel; }

    public Integer getConsecutiveLoginDays() { return consecutiveLoginDays == null ? 0 : consecutiveLoginDays; }

    public void setConsecutiveLoginDays(Integer consecutiveLoginDays) { this.consecutiveLoginDays = consecutiveLoginDays; }

    public LocalDateTime getLastSeenAt() { return lastSeenAt; }

    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
 

    public Integer getPerfectTestCount() {

        return perfectTestCount == null ? 0 : perfectTestCount;}

    public void setPerfectTestCount(Integer perfectTestCount) {

        this.perfectTestCount = perfectTestCount;}

    public Long getTotalExp() {

        return totalExp == null ? 0L : totalExp;

    }

    public void setTotalExp(Long totalExp) {

        this.totalExp = totalExp;

    }
 

    public String getBackgroundClass() { return backgroundClass; }

    public void setBackgroundClass(String backgroundClass) { this.backgroundClass = backgroundClass; }

    public String getBackgroundColor() { return backgroundColor; }

    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }
 
    public Integer getBackgroundSeenLevel() { return backgroundSeenLevel; }
    public void setBackgroundSeenLevel(Integer backgroundSeenLevel) { this.backgroundSeenLevel = backgroundSeenLevel; }

}

 