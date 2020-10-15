package com.ngari.recipe.common;

import com.ngari.recipe.common.anno.Verify;
import ctd.schema.annotation.Dictionary;

import java.io.Serializable;
import java.util.Date;

/**
 *@author Created by liuxiaofeng on 2020/10/12.
 * 基础服务修改处方物流状态入参
 */
public class RecipeTrannckingReqTO implements Serializable{
    private static final long serialVersionUID = 7678705372967824586L;

    @Verify(desc = "物流单号")
    private String trackingNumber;

    @Verify(desc = "物流公司")
    @Dictionary(id = "eh.cdr.dictionary.LogisticsCompany")
    private String logisticsCompany;

    @Verify(desc = "物流状态")
    private Integer trackingStatus;

    @Verify(isNotNull = false, desc = "配送日期")
    private Date sendDate;

    @Verify(isNotNull = false, desc = "配送人")
    private String sender;

    @Verify(isNotNull = false, desc = "配送完成日期")
    private Date finishDate;

    public Date getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getLogisticsCompany() {
        return logisticsCompany;
    }

    public void setLogisticsCompany(String logisticsCompany) {
        this.logisticsCompany = logisticsCompany;
    }

    public Integer getTrackingStatus() {
        return trackingStatus;
    }

    public void setTrackingStatus(Integer trackingStatus) {
        this.trackingStatus = trackingStatus;
    }

    public Date getSendDate() {
        return sendDate;
    }

    public void setSendDate(Date sendDate) {
        this.sendDate = sendDate;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}
