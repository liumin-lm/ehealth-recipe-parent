package com.ngari.recipe.recipe.model;

import java.io.Serializable;

/**
* @Description: UpdateMatchStatusFormBean 类（或接口）是更新对照药品状态的前端提交对象
* @Author: JRK
* @Date: 2019/10/31
*/
public class UpdateMatchStatusFormBean implements Serializable {

    private static final long serialVersionUID = -8906862460261923051L;

    /* 对照的药品id*/
    private Integer drugId;

    /*匹配的平台药品id*/
    private Integer matchDrugId;

    /*匹配的省平台药品code*/
    private String matchDrugInfo;

    /*操作的用户名*/
    private String operator;

    /*操作类型 -> 平台：0，省平台：1*/
    private int makeType;

    /*有机构对应的省平台药品*/
    private Boolean haveProvinceDrug;

    public Integer getDrugId() {
        return drugId;
    }

    public void setDrugId(Integer drugId) {
        this.drugId = drugId;
    }

    public Integer getMatchDrugId() {
        return matchDrugId;
    }

    public void setMatchDrugId(Integer matchDrugId) {
        this.matchDrugId = matchDrugId;
    }

    public String getMatchDrugInfo() {
        return matchDrugInfo;
    }

    public void setMatchDrugInfo(String matchDrugInfo) {
        this.matchDrugInfo = matchDrugInfo;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public int getMakeType() {
        return makeType;
    }

    public void setMakeType(int makeType) {
        this.makeType = makeType;
    }

    public Boolean getHaveProvinceDrug() {
        return haveProvinceDrug;
    }

    public void setHaveProvinceDrug(Boolean haveProvinceDrug) {
        this.haveProvinceDrug = haveProvinceDrug;
    }
}