package com.ngari.recipe.regulation.model;


import java.io.Serializable;

public class RegulationDrugDeliveryDTO implements Serializable{
    private static final long serialVersionUID = 3933294480288077602L;

    private Integer organId;
    private String recipeId; //处方号
    private String clinicId; //复诊流水号
    private String giveMode; //配送方式
    private String startSendDate; //配送开始时间
    private String endSendDate; //配送结束时间
    private String sendStatus; //配送状态 非必填
    private String modifyFlag; //修改标志

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getClinicId() {
        return clinicId;
    }

    public void setClinicId(String clinicId) {
        this.clinicId = clinicId;
    }

    public String getGiveMode() {
        return giveMode;
    }

    public void setGiveMode(String giveMode) {
        this.giveMode = giveMode;
    }

    public String getStartSendDate() {
        return startSendDate;
    }

    public void setStartSendDate(String startSendDate) {
        this.startSendDate = startSendDate;
    }

    public String getEndSendDate() {
        return endSendDate;
    }

    public void setEndSendDate(String endSendDate) {
        this.endSendDate = endSendDate;
    }

    public String getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(String sendStatus) {
        this.sendStatus = sendStatus;
    }

    public String getModifyFlag() {
        return modifyFlag;
    }

    public void setModifyFlag(String modifyFlag) {
        this.modifyFlag = modifyFlag;
    }
}
