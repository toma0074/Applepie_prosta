package jp.co.sss.pr.form;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CustomerForm {

    private Integer userId;

    @NotBlank
    @Size(min = 1, max = 8)
    private String userName;

<<<<<<< HEAD
    @NotBlank
    @Size(min = 4, max = 16)
    @Pattern(regexp = "^[a-zA-Z0-9]+$")
    private String userPass;

    private Integer permission;

    /** deleteFlag は Boolean 型（customer テーブルの BOOLEAN 列に対応） */
    private Boolean deleteFlag;

    // --- getters / setters ---

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
=======
	  private Integer permission;
	  
	  private Integer deleteFlag;
	  
//		【追加開始】2026/10/01 大塚
		
		private LocalDateTime loginDate;
		
		private Integer loginCount;

		private Integer loginFlag;

		private Integer userLevel;
		
//		【追加終了】2026/10/01 大塚
	
	  //getter setter
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
	  
//		【追加開始】2026/10/01 大塚
		
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
		
//		【追加終了】2026/10/01 大塚
>>>>>>> dbbeeff4a6dc45b31000f5bdad3215498d51e015
}
