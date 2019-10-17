package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author yinsheng
 * @date 2019\10\9 0009 10:44
 */
@Entity
@Schema
@Table(name = "cdr_judicialorgan")
@Access(AccessType.PROPERTY)
public class JudicialOrgan implements Serializable{
    private static final long serialVersionUID = -4463027090280870885L;

    @ItemProperty(alias = "审方机构序号")
    private Integer judicialorganId;

    @ItemProperty(alias = "审方机构名称")
    private String name;

    @ItemProperty(alias = "审方机构")
    private String account;

    @ItemProperty(alias = "用户名")
    private String userId;

    @ItemProperty(alias = "密码")
    private String password;

    @ItemProperty(alias = "审方机构联系电话")
    private String tel;

    @ItemProperty(alias = "审方机构实现层")
    private String callSys;

    @ItemProperty(alias = "调用接口标识")
    private String token;

    @ItemProperty(alias = "鉴权地址")
    private String authenUrl;

    @ItemProperty(alias = "业务处理地址")
    private String businessUrl;

    @ItemProperty(alias = "创建时间")
    private Date createDate;

    @ItemProperty(alias = "修改时间")
    private Date lastModify;

    @ItemProperty(alias = "状态 0:不启用，1:启用")
    private Integer status;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "JudicialorganId", unique = true, nullable = false)
    public Integer getJudicialorganId() {
        return judicialorganId;
    }

    public void setJudicialorganId(Integer judicialorganId) {
        this.judicialorganId = judicialorganId;
    }

    @Column(name = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "Account", length = 20)
    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    @Column(name = "UserId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Column(name = "Password")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Column(name = "Tel")
    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    @Column(name = "CallSys")
    public String getCallSys() {
        return callSys;
    }

    public void setCallSys(String callSys) {
        this.callSys = callSys;
    }

    @Column(name = "Token")
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Column(name = "AuthenUrl")
    public String getAuthenUrl() {
        return authenUrl;
    }

    public void setAuthenUrl(String authenUrl) {
        this.authenUrl = authenUrl;
    }

    @Column(name = "BusinessUrl")
    public String getBusinessUrl() {
        return businessUrl;
    }

    public void setBusinessUrl(String businessUrl) {
        this.businessUrl = businessUrl;
    }

    @Column(name = "CreateDate")
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column(name = "LastModify")
    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    @Column(name = "Status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
