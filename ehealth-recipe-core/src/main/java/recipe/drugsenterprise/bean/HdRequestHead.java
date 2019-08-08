package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
* @Description: 华东token请求对象head头
* @Author: JRK
* @Date: 2019/7/24
*/
public class HdRequestHead implements Serializable{
    private static final long serialVersionUID = 8445155013826022717L;
    /**
     * 用户id
     */
    private String user;
    /**
     * 用户密码
     */
    private String password;
    /**
     * 用户组织
     */
    private String organization;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }
}