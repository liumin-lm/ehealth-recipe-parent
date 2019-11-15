package com.ngari.recipe.hisprescription.model;

import java.io.Serializable;
import java.util.List;

/**
 * created by shiyuping on 2019/11/11
 * @author shiyuping
 */
public class HosRecipeDTO implements Serializable {
    private static final long serialVersionUID = 8949148045324702803L;
    private String registeredId;
    private String recipeCode;
    private String diseaseName;
    private String disease;
    private String recipeType;
    private String signTime;
    private String departCode;
    private String departName;
    private String doctorCode;
    private String doctorName;
    private List<HosRecipeDetailDTO> detailData;

    public String getRegisteredId() {
        return registeredId;
    }

    public void setRegisteredId(String registeredId) {
        this.registeredId = registeredId;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    public void setDiseaseName(String diseaseName) {
        this.diseaseName = diseaseName;
    }

    public String getDisease() {
        return disease;
    }

    public void setDisease(String disease) {
        this.disease = disease;
    }

    public String getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(String recipeType) {
        this.recipeType = recipeType;
    }

    public String getSignTime() {
        return signTime;
    }

    public void setSignTime(String signTime) {
        this.signTime = signTime;
    }

    public String getDepartCode() {
        return departCode;
    }

    public void setDepartCode(String departCode) {
        this.departCode = departCode;
    }

    public String getDepartName() {
        return departName;
    }

    public void setDepartName(String departName) {
        this.departName = departName;
    }

    public String getDoctorCode() {
        return doctorCode;
    }

    public void setDoctorCode(String doctorCode) {
        this.doctorCode = doctorCode;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public List<HosRecipeDetailDTO> getDetailData() {
        return detailData;
    }

    public void setDetailData(List<HosRecipeDetailDTO> detailData) {
        this.detailData = detailData;
    }
}
