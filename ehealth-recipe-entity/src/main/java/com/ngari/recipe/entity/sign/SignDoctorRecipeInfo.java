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
    private String signCaDateDoc;

    /**医生签名值*/
    private String signCodeDoc;

    /**医生签名文件*/
    private String signFileDoc;

    /**医生签名时间*/
    private Date signDate;

    /** 药师审方时间戳*/
    private String signCaDatePha;

    /**药师审方签名值*/
    private String signCodePha;

    /**药师签名文件*/
    private String signFilePha;

    /**药师审方时间*/
    private Date checkDatePha;

    /**医生手签图片*/
    private String sealDataDoc;

    /**药师手签图片*/
    private String sealDataPha;

    /**医生签名摘要*/
    private String signRemarkDoc;

    /**药师签名摘要*/
    private String signRemarkPha;

    /**签名原文*/
    private String signBefText;

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
    public Date getSignDate() {
        return signDate;
    }

    public void setSignDate(Date signDate) {
        this.signDate = signDate;
    }

    @Column
    public String getSignCaDateDoc() {
        return signCaDateDoc;
    }

    public void setSignCaDateDoc(String signCaDateDoc) {
        this.signCaDateDoc = signCaDateDoc;
    }

    @Column
    public String getSignCodeDoc() {
        return signCodeDoc;
    }

    public void setSignCodeDoc(String signCodeDoc) {
        this.signCodeDoc = signCodeDoc;
    }

    @Column
    public String getSignFileDoc() {
        return signFileDoc;
    }

    public void setSignFileDoc(String signFileDoc) {
        this.signFileDoc = signFileDoc;
    }

    @Column
    public String getSignCaDatePha() {
        return signCaDatePha;
    }

    public void setSignCaDatePha(String signCaDatePha) {
        this.signCaDatePha = signCaDatePha;
    }

    @Column
    public String getSignCodePha() {
        return signCodePha;
    }

    public void setSignCodePha(String signCodePha) {
        this.signCodePha = signCodePha;
    }

    @Column
    public String getSignFilePha() {
        return signFilePha;
    }

    public void setSignFilePha(String signFilePha) {
        this.signFilePha = signFilePha;
    }

    @Column
    public Date getCheckDatePha() {
        return checkDatePha;
    }

    public void setCheckDatePha(Date checkDatePha) {
        this.checkDatePha = checkDatePha;
    }

    @Transient
    public String getSealDataDoc() {
        return sealDataDoc;
    }

    public void setSealDataDoc(String sealDataDoc) {
        this.sealDataDoc = sealDataDoc;
    }

    @Transient
    public String getSealDataPha() {
        return sealDataPha;
    }

    public void setSealDataPha(String sealDataPha) {
        this.sealDataPha = sealDataPha;
    }

    @Column
    public String getSignRemarkDoc() {
        return signRemarkDoc;
    }

    public void setSignRemarkDoc(String signRemarkDoc) {
        this.signRemarkDoc = signRemarkDoc;
    }

    @Column
    public String getSignRemarkPha() {
        return signRemarkPha;
    }

    public void setSignRemarkPha(String signRemarkPha) {
        this.signRemarkPha = signRemarkPha;
    }

    @Column
    public String getSignBefText() {
        return signBefText;
    }

    public void setSignBefText(String signBefText) {
        this.signBefText = signBefText;
    }
}
