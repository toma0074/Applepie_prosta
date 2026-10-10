package jp.co.sss.pr.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_user_gen")
    @SequenceGenerator(name = "seq_user_gen", sequenceName = "seq_user", allocationSize = 1)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "user_name", nullable = false, length = 20)
    private String userName;

    @Column(name = "user_pass", nullable = false, length = 16)
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
}
