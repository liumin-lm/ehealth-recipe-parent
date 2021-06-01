package com.ngari.recipe.hisprescription.model;

import java.io.Serializable;

/**
 * created by shiyuping on 2020/1/3
 * 婚育史结构体数据集
 * @author shiyuping
 */
public class MCHistoryInfoDTO implements Serializable {
    private static final long serialVersionUID = -8121568476783806160L;
    private String maritalStatus;//婚姻状况-1已婚 0未婚 9未知 未能提供时默认为9
    private String pregnantNum;//怀孕次数
    private String prematureDelivery;//早产次数
    private String abortionTimes;//流产次数
    private String survivalNum;//现存个数

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getPregnantNum() {
        return pregnantNum;
    }

    public void setPregnantNum(String pregnantNum) {
        this.pregnantNum = pregnantNum;
    }

    public String getPrematureDelivery() {
        return prematureDelivery;
    }

    public void setPrematureDelivery(String prematureDelivery) {
        this.prematureDelivery = prematureDelivery;
    }

    public String getAbortionTimes() {
        return abortionTimes;
    }

    public void setAbortionTimes(String abortionTimes) {
        this.abortionTimes = abortionTimes;
    }

    public String getSurvivalNum() {
        return survivalNum;
    }

    public void setSurvivalNum(String survivalNum) {
        this.survivalNum = survivalNum;
    }
}
