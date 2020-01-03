package com.ngari.recipe.hisprescription.model;

import java.io.Serializable;

/**
 * created by shiyuping on 2020/1/3
 * 家族史结构体数据集
 * @author shiyuping
 */
public class FamilyHistoryInfoDTO implements Serializable {
    private static final long serialVersionUID = -5514839908178063254L;
    private String familyDisease;//家族遗传相关性疾病史 1是 0否 未能提供时默认为0
    private String familyDiseaseInfo;//家族史详细信息

    public String getFamilyDisease() {
        return familyDisease;
    }

    public void setFamilyDisease(String familyDisease) {
        this.familyDisease = familyDisease;
    }

    public String getFamilyDiseaseInfo() {
        return familyDiseaseInfo;
    }

    public void setFamilyDiseaseInfo(String familyDiseaseInfo) {
        this.familyDiseaseInfo = familyDiseaseInfo;
    }
}
