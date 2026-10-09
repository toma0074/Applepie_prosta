package jp.co.sss.pr.entity;

<<<<<<< HEAD
import java.time.LocalDate;
=======
import java.time.LocalDateTime;
>>>>>>> dbbeeff4a6dc45b31000f5bdad3215498d51e015

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

<<<<<<< HEAD
    @Column(name = "user_name", nullable = false, length = 20)
    private String userName;
=======
	@Column
	private String userName;
	
	@Column
	private String userPass;
	
	@Column
	private Integer permission;
	
	@Column 
	private Integer deleteFlag;
	
//	【追加開始】2026/10/01 大塚
	
	@Column
	private LocalDateTime loginDate; // DATE型に対応
	
	@Column
	private Integer loginCount; // NUMBER(10)に対応

	@Column
	private Integer loginFlag; // NUMBER(1)に対応

	@Column
	private Integer userLevel; // NUMBER(10)に対応
	
//	【追加終了】2026/10/01 大塚
	
	
	public Integer getUserId() {
		return userId;
	}
>>>>>>> dbbeeff4a6dc45b31000f5bdad3215498d51e015

    @Column(name = "user_pass", nullable = false, length = 16)
    private String userPass;

    @Column(name = "permission", nullable = false)
    private Integer permission;

    @Column(name = "delete_flag", nullable = false)
    private Boolean deleteFlag;

    /** 最終ログイン日 */
    @Column(name = "login_date")
    private LocalDate loginDate;

    /** 累計ログイン日数 */
    @Column(name = "login_count")
    private Integer loginCount;

    /** 当日ログイン済みフラグ（true=当日ログイン済み） */
    @Column(name = "login_flag")
    private Boolean loginFlag;

    /** 現在のレベル */
    @Column(name = "user_level")
    private Integer userLevel;

    // --- getters / setters ---

<<<<<<< HEAD
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPass() {
        return userPass;
    }

    public void setUserPass(String userPass) {
        this.userPass = userPass;
    }

    public Integer getPermission() {
        return permission;
    }

    public void setPermission(Integer permission) {
        this.permission = permission;
    }

    public Boolean getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Boolean deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public LocalDate getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(LocalDate loginDate) {
        this.loginDate = loginDate;
    }

    public Integer getLoginCount() {
        return loginCount == null ? 0 : loginCount;
    }

    public void setLoginCount(Integer loginCount) {
        this.loginCount = loginCount;
    }

    public Boolean getLoginFlag() {
        return loginFlag == null ? false : loginFlag;
    }

    public void setLoginFlag(Boolean loginFlag) {
        this.loginFlag = loginFlag;
    }

    public Integer getUserLevel() {
        return userLevel == null ? 1 : userLevel;
    }

    public void setUserLevel(Integer userLevel) {
        this.userLevel = userLevel;
    }
=======
	public void setDeleteFlag(Integer deleteFlag) {
		this.deleteFlag = deleteFlag;
	}
	
//	【追加開始】2026/10/01 大塚

	public LocalDateTime getLoginDate() {
		return loginDate;
	}

	public void setLoginDate(LocalDateTime loginDate) {
		this.loginDate = loginDate;
	}

	public Integer getLoginCount() {
		return loginCount;
	}

	public void setLoginCount(Integer loginCount) {
		this.loginCount = loginCount;
	}

	public Integer getLoginFlag() {
		return loginFlag;
	}

	public void setLoginFlag(Integer loginFlag) {
		this.loginFlag = loginFlag;
	}

	public Integer getUserLevel() {
		return userLevel;
	}

	public void setUserLevel(Integer userLevel) {
		this.userLevel = userLevel;
	}
	
//	【追加終了】2026/10/01 大塚
	
>>>>>>> dbbeeff4a6dc45b31000f5bdad3215498d51e015
}
