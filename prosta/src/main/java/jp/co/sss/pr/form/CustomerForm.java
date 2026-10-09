package jp.co.sss.pr.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CustomerForm {

    private Integer userId;

    @NotBlank
    @Size(min = 1, max = 8)
    private String userName;

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
}
