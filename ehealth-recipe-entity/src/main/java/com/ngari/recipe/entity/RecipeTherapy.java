package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


/**
 * 诊疗处方对象
 *
 * @author fuzi
 */
@Schema
@Entity
@Table(name = "cdr_recipe_therapy")
@Access(AccessType.PROPERTY)
public class RecipeTherapy implements Serializable {
    private static final long serialVersionUID = -7119210639963847034L;
    @ItemProperty(alias = "诊疗id")
    private Integer id;
    @ItemProperty(alias = "机构id")
    private Integer organId;
    @ItemProperty(alias = "处方id")
    private Integer recipeId;
    @ItemProperty(alias = "来源Id，默认复诊")
    private Integer clinicId;
    @ItemProperty(alias = "医生id")
    private Integer doctorId;
    @ItemProperty(alias = "患者id")
    private String mpiId;
    @ItemProperty(alias = "医院诊疗提示")
    private String therapyNotice;
    @ItemProperty(alias = "医院诊疗时间")
    private String therapyTime;
    @ItemProperty(alias = "诊疗执行科室")
    private String therapyExecuteDepart;
    @ItemProperty(alias = "诊疗缴费时间")
    private Date therapyPayTime;
    @ItemProperty(alias = "诊疗作废类型，1:医生撤销，2:HIS作废，3:系统取消")
    private Integer therapyCancellationType;
    @ItemProperty(alias = "诊疗作废信息")
    private String therapyCancellation;
    @ItemProperty(alias = "诊疗状态: 1：待提交，2:待缴费，3:已交费，4：已作废")
    private Integer status;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "organ_id")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "recipe_id")
    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    @Column(name = "clinic_id")
    public Integer getClinicId() {
        return clinicId;
    }

    public void setClinicId(Integer clinicId) {
        this.clinicId = clinicId;
    }

    @Column(name = "doctor_id")
    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    @Column(name = "mpi_id")
    public String getMpiId() {
        return mpiId;
    }

    public void setMpiId(String mpiId) {
        this.mpiId = mpiId;
    }

    @Column(name = "therapy_notice")
    public String getTherapyNotice() {
        return therapyNotice;
    }

    public void setTherapyNotice(String therapyNotice) {
        this.therapyNotice = therapyNotice;
    }

    @Column(name = "therapy_time")
    public String getTherapyTime() {
        return therapyTime;
    }

    public void setTherapyTime(String therapyTime) {
        this.therapyTime = therapyTime;
    }

    @Column(name = "therapy_execute_depart")
    public String getTherapyExecuteDepart() {
        return therapyExecuteDepart;
    }

    public void setTherapyExecuteDepart(String therapyExecuteDepart) {
        this.therapyExecuteDepart = therapyExecuteDepart;
    }

    @Column(name = "therapy_pay_time")
    public Date getTherapyPayTime() {
        return therapyPayTime;
    }

    public void setTherapyPayTime(Date therapyPayTime) {
        this.therapyPayTime = therapyPayTime;
    }

    @Column(name = "therapy_cancellation_type")
    public Integer getTherapyCancellationType() {
        return therapyCancellationType;
    }

    public void setTherapyCancellationType(Integer therapyCancellationType) {
        this.therapyCancellationType = therapyCancellationType;
    }

    @Column(name = "therapy_cancellation")
    public String getTherapyCancellation() {
        return therapyCancellation;
    }

    public void setTherapyCancellation(String therapyCancellation) {
        this.therapyCancellation = therapyCancellation;
    }

    @Column(name = "status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

}
