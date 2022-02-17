package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author zgy
 * @date 2022/1/13 15:28
 */
@Entity
public class RecipeTherapyOpBean implements Serializable {
    private static final long serialVersionUID = 6342441820192332167L;

    @ItemProperty(alias = "处方id")
    private Integer recipeId;

    @ItemProperty(alias = "处方单号")
    private String recipeCode;

    @ItemProperty(alias = "患者姓名")
    private String patientName;

    @ItemProperty(alias = "患者唯一标识")
    private String mpiId;

    @ItemProperty(alias = "医生姓名")
    private String doctorName;

    @ItemProperty(alias = "挂号科室名称")
    private String appointDepartName;

    @ItemProperty(alias = "开方机构名称")
    private String organName;

    @ItemProperty(alias = "诊疗处方状态")
    private Integer status;

    @ItemProperty(alias = "开具时间")
    private String createTime;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "RecipeID", unique = true, nullable = false)
    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }
    @Column(name = "RecipeCode")
    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }
    @Column(name = "patientName")
    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }
    @Column(name = "MPIID")
    public String getMpiId() {
        return mpiId;
    }

    public void setMpiId(String mpiId) {
        this.mpiId = mpiId;
    }
    @Column(name = "doctorName")
    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }
    @Column(name = "appoint_depart_name")
    public String getAppointDepartName() {
        return appointDepartName;
    }

    public void setAppointDepartName(String appointDepartName) {
        this.appointDepartName = appointDepartName;
    }
    @Column(name = "organName")
    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }
    @Column(name = "Status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
    @Column(name = "gmt_create")
    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
