package jp.co.sss.pr.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CustomerForm {
	
	  private Integer userId;

	  @NotBlank
	  @Size(min=1,max=8)
	  //@Pattern(regexp="^[^ -~｡-ﾟ]+$")
	  private String userName;

	  @NotBlank
	  @Size(min=4,max=16)
	  @Pattern(regexp="^[a-zA-Z0-9]+$")
	  private String userPass;

	  private Integer permission;
	  private Integer deleteFlag;
	
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
	  
}
