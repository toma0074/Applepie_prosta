package jp.co.sss.pr.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name="customer")
public class Customer {

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE,generator="seq_userId")
	@SequenceGenerator(name="seq_userId",sequenceName="seq_user",allocationSize=1)	
	private Integer userId;

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

	public Integer getDeleteFlag() {
		return deleteFlag;
	}

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
	
}
