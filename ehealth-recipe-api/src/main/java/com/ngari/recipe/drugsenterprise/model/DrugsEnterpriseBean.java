package com.ngari.recipe.drugsenterprise.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import ctd.util.JSONUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 药企
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/6.
 */

@Schema
public class DrugsEnterpriseBean implements Serializable {

    private static final long serialVersionUID = 3811188626885371263L;

    @ItemProperty(alias = "药企序号")
    private Integer id;

    @ItemProperty(alias = "药企名称")
    private String name;

    @ItemProperty(alias = "药企在平台的账户")
    private String account;

    @ItemProperty(alias = "用户名")
    private String userId;

    @ItemProperty(alias = "密码")
    private String password;

    @ItemProperty(alias = "药企联系电话")
    private String tel;

    @ItemProperty(alias = "药企实现类简称，默认使用 common， 也就是国药的一套实现")
    private String callSys;

    @ItemProperty(alias = "调用接口标识")
    private String token;

    @ItemProperty(alias = "药企平台鉴权地址")
    private String authenUrl;

    @ItemProperty(alias = "药企平台业务处理地址")
    private String businessUrl;

    @ItemProperty(alias = "创建时间")
    private Date createDate;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "配送模式支持 0:不支持 1:线上付款 2:货到付款 3:药店取药 8:货到付款和药店取药 9:都支持")
    private Integer payModeSupport;

    @ItemProperty(alias = "状态标识")
    private Integer status;

    @ItemProperty(alias = "排序，1最前，越往后越小")
    private Integer sort;

    public DrugsEnterpriseBean() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getCallSys() {
        return callSys;
    }

    public void setCallSys(String callSys) {
        this.callSys = callSys;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAuthenUrl() {
        return authenUrl;
    }

    public void setAuthenUrl(String authenUrl) {
        this.authenUrl = authenUrl;
    }

    public String getBusinessUrl() {
        return businessUrl;
    }

    public void setBusinessUrl(String businessUrl) {
        this.businessUrl = businessUrl;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public Integer getPayModeSupport() {
        return payModeSupport;
    }

    public void setPayModeSupport(Integer payModeSupport) {
        this.payModeSupport = payModeSupport;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Override
    public String toString() {
        return JSONUtils.toString(this);
    }
}
