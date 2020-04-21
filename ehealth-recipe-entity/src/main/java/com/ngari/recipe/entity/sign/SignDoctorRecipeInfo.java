package com.ngari.recipe.entity.sign;

import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Schema
@Entity
@Table(name = "sign_doctor_recipe_info")
public class SignDoctorRecipeInfo {

    private Integer id;

    /** 处方订单号*/
    private Integer recipeId;

    /** 医生序列号*/
    private  String caSerCodeDoc;

    /** 药师序列号*/
    private String caSerCodePha;

    /** 医生签名时间戳*/
    private String signCADate;

    /**医生签名值*/
    private String signRecipeCode;

    /**医生签名文件*/
    private String signFile;

    /**医生签名时间*/
    private Date signDate;

    /** 药师审方时间戳*/
    private String signPharmacistCADate;

    /**药师审方签名值*/
    private String signPharmacistCode;

    /**药师签名文件*/
    private String chemistSignFile;

    /**药师审方时间*/
    private Date CheckDateYs;

    private Date createDate;

    private Date lastmodify;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column
    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    @Column
    public String getCaSerCodeDoc() {
        return caSerCodeDoc;
    }

    public void setCaSerCodeDoc(String caSerCodeDoc) {
        this.caSerCodeDoc = caSerCodeDoc;
    }

    @Column
    public String getCaSerCodePha() {
        return caSerCodePha;
    }

    public void setCaSerCodePha(String caSerCodePha) {
        this.caSerCodePha = caSerCodePha;
    }

    @Column
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column
    public Date getLastmodify() {
        return lastmodify;
    }

    public void setLastmodify(Date lastmodify) {
        this.lastmodify = lastmodify;
    }

    @Column
    public String getSignCADate() {
        return signCADate;
    }

    public void setSignCADate(String signCADate) {
        this.signCADate = signCADate;
    }

    @Column
    public String getSignRecipeCode() {
        return signRecipeCode;
    }

    public void setSignRecipeCode(String signRecipeCode) {
        this.signRecipeCode = signRecipeCode;
    }

    @Column
    public String getSignFile() {
        return signFile;
    }

    public void setSignFile(String signFile) {
        this.signFile = signFile;
    }

    @Column
    public Date getSignDate() {
        return signDate;
    }

    public void setSignDate(Date signDate) {
        this.signDate = signDate;
    }

    @Column
    public String getSignPharmacistCADate() {
        return signPharmacistCADate;
    }

    public void setSignPharmacistCADate(String signPharmacistCADate) {
        this.signPharmacistCADate = signPharmacistCADate;
    }

    @Column
    public String getSignPharmacistCode() {
        return signPharmacistCode;
    }

    public void setSignPharmacistCode(String signPharmacistCode) {
        this.signPharmacistCode = signPharmacistCode;
    }

    @Column
    public String getChemistSignFile() {
        return chemistSignFile;
    }

    public void setChemistSignFile(String chemistSignFile) {
        this.chemistSignFile = chemistSignFile;
    }

    @Column
    public Date getCheckDateYs() {
        return CheckDateYs;
    }

    public void setCheckDateYs(Date checkDateYs) {
        CheckDateYs = checkDateYs;
    }
}
