package com.ngari.recipe.hisprescription.model;

import java.io.Serializable;

/**
 * created by shiyuping on 2020/1/3
 * 既往史结构体数据集
 */
public class PastHistoryInfoDTO implements Serializable {
    private static final long serialVersionUID = -6022029602022542186L;
    private String hypertensio;//高血压
    private String hypertensionInfo;//高血压详细信息
    private String diabetes;//糖尿病史
    private String diabetesInfo;//糖尿病史详细信息
    private String operatio;//手术史
    private String operationInf;//手术史详细信息
    private String infectiousDiseases;//传染病史
    private String infectiousDiseasesInfo;//传染病史详细信息
    private String trauma;//外伤史
    private String traumaInfo;//外伤史详细信息
    private String allerg;//药物过敏史
    private String allergyInfo;//药物过敏史详细信息
    private String longTermDrug;//长期药物使用史
    private String longTermDrugsInfo;//长期药物使用史详细信息
    private String otherInf;//其他信息

    public String getHypertensio() {
        return hypertensio;
    }

    public void setHypertensio(String hypertensio) {
        this.hypertensio = hypertensio;
    }

    public String getHypertensionInfo() {
        return hypertensionInfo;
    }

    public void setHypertensionInfo(String hypertensionInfo) {
        this.hypertensionInfo = hypertensionInfo;
    }

    public String getDiabetes() {
        return diabetes;
    }

    public void setDiabetes(String diabetes) {
        this.diabetes = diabetes;
    }

    public String getDiabetesInfo() {
        return diabetesInfo;
    }

    public void setDiabetesInfo(String diabetesInfo) {
        this.diabetesInfo = diabetesInfo;
    }

    public String getOperatio() {
        return operatio;
    }

    public void setOperatio(String operatio) {
        this.operatio = operatio;
    }

    public String getOperationInf() {
        return operationInf;
    }

    public void setOperationInf(String operationInf) {
        this.operationInf = operationInf;
    }

    public String getInfectiousDiseases() {
        return infectiousDiseases;
    }

    public void setInfectiousDiseases(String infectiousDiseases) {
        this.infectiousDiseases = infectiousDiseases;
    }

    public String getInfectiousDiseasesInfo() {
        return infectiousDiseasesInfo;
    }

    public void setInfectiousDiseasesInfo(String infectiousDiseasesInfo) {
        this.infectiousDiseasesInfo = infectiousDiseasesInfo;
    }

    public String getTrauma() {
        return trauma;
    }

    public void setTrauma(String trauma) {
        this.trauma = trauma;
    }

    public String getTraumaInfo() {
        return traumaInfo;
    }

    public void setTraumaInfo(String traumaInfo) {
        this.traumaInfo = traumaInfo;
    }

    public String getAllerg() {
        return allerg;
    }

    public void setAllerg(String allerg) {
        this.allerg = allerg;
    }

    public String getAllergyInfo() {
        return allergyInfo;
    }

    public void setAllergyInfo(String allergyInfo) {
        this.allergyInfo = allergyInfo;
    }

    public String getLongTermDrug() {
        return longTermDrug;
    }

    public void setLongTermDrug(String longTermDrug) {
        this.longTermDrug = longTermDrug;
    }

    public String getLongTermDrugsInfo() {
        return longTermDrugsInfo;
    }

    public void setLongTermDrugsInfo(String longTermDrugsInfo) {
        this.longTermDrugsInfo = longTermDrugsInfo;
    }

    public String getOtherInf() {
        return otherInf;
    }

    public void setOtherInf(String otherInf) {
        this.otherInf = otherInf;
    }
}
