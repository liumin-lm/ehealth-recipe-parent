package com.ngari.recipe.recipe.model;

import java.io.Serializable;

/**
 * created by shiyuping on 2019/10/28
 */
public class NoticePlatRecipeFlowInfoDTO implements Serializable {
    private static final long serialVersionUID = -2723306637157750520L;
    private Integer organId;
    private String  organizeCode;
    //医院处方号
    private String  recipeID;
    //备案结果状态
    private String putOnRecordStatus;
    //医保备案号
    private String putOnRecordID;
    //原因备注
    private String resultMark;

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getOrganizeCode() {
        return organizeCode;
    }

    public void setOrganizeCode(String organizeCode) {
        this.organizeCode = organizeCode;
    }

    public String getRecipeID() {
        return recipeID;
    }

    public void setRecipeID(String recipeID) {
        this.recipeID = recipeID;
    }

    public String getPutOnRecordStatus() {
        return putOnRecordStatus;
    }

    public void setPutOnRecordStatus(String putOnRecordStatus) {
        this.putOnRecordStatus = putOnRecordStatus;
    }

    public String getPutOnRecordID() {
        return putOnRecordID;
    }

    public void setPutOnRecordID(String putOnRecordID) {
        this.putOnRecordID = putOnRecordID;
    }

    public String getResultMark() {
        return resultMark;
    }

    public void setResultMark(String resultMark) {
        this.resultMark = resultMark;
    }
}
