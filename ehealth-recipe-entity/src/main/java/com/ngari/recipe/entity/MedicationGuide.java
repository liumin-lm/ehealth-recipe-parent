package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * created by shiyuping on 2019/10/25
 * @author shiyuping
 */
@Entity
@Schema
@Table(name = "cdr_medicationguide")
@Access(AccessType.PROPERTY)
public class MedicationGuide implements Serializable {
    private static final long serialVersionUID = 970751328483807160L;

    @ItemProperty(alias = "用药指导第三方id")
    private Integer guideId;

    @ItemProperty(alias = "用药指导第三方名称")
    private String name;

    @ItemProperty(alias = "用药指导第三方实现层")
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
    @Column(name = "guideId", unique = true, nullable = false)
    public Integer getGuideId() {
        return guideId;
    }

    public void setGuideId(Integer guideId) {
        this.guideId = guideId;
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "callSys")
    public String getCallSys() {
        return callSys;
    }

    public void setCallSys(String callSys) {
        this.callSys = callSys;
    }

    @Column(name = "token")
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Column(name = "authenUrl")
    public String getAuthenUrl() {
        return authenUrl;
    }

    public void setAuthenUrl(String authenUrl) {
        this.authenUrl = authenUrl;
    }

    @Column(name = "businessUrl")
    public String getBusinessUrl() {
        return businessUrl;
    }

    public void setBusinessUrl(String businessUrl) {
        this.businessUrl = businessUrl;
    }

    @Column(name = "createDate")
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column(name = "lastModify")
    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    @Column(name = "status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
