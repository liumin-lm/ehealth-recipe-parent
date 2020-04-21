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
}
