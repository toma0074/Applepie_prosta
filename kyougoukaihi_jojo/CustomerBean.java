package jp.co.sss.pr.bean;

import java.time.LocalDateTime;

public class CustomerBean {

<<<<<<< HEAD
    private Integer userId;
    private String userName;
    private String userPass;
    private Integer permission;
    private Boolean deleteFlag;
=======
	private Integer userId;
	
	private String userName;
	 
	private String userPass;
	
	private Integer permission;
	
	private Integer deleteFlag;
	
//	【追加開始】2026/10/01 大塚
	
	private LocalDateTime loginDate;
	
	private Integer loginCount;

	private Integer loginFlag;

	private Integer userLevel;
	
//	【追加終了】2026/10/01 大塚
	
	public Integer getUserId() {
		return userId;
	}
>>>>>>> dbbeeff4a6dc45b31000f5bdad3215498d51e015

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPass() { return userPass; }
    public void setUserPass(String userPass) { this.userPass = userPass; }

    public Integer getPermission() { return permission; }
    public void setPermission(Integer permission) { this.permission = permission; }

<<<<<<< HEAD
    public Boolean getDeleteFlag() { return deleteFlag; }
    public void setDeleteFlag(Boolean deleteFlag) { this.deleteFlag = deleteFlag; }
=======
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
	
>>>>>>> dbbeeff4a6dc45b31000f5bdad3215498d51e015
}
