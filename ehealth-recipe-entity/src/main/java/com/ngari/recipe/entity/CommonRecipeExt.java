package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;


/**
 * 常用方扩展对象
 *
 * @author fuzi
 */
@Schema
@Entity
@Table(name = "cdr_common_recipe_ext")
@Access(AccessType.PROPERTY)
public class CommonRecipeExt implements Serializable {
    @ItemProperty(alias = "自增id")
    private Integer id;
    @ItemProperty(alias = "常用方Id")
    private Integer commonRecipeId;
    @ItemProperty(alias = "制法")
    private String makeMethodId;
    @ItemProperty(alias = "制法text")
    private String makeMethodText;
    @ItemProperty(alias = "每付取汁")
    private String juice;
    @ItemProperty(alias = "每付取汁单位")
    private String juiceUnit;
    @ItemProperty(alias = "次量")
    private String minor;
    @ItemProperty(alias = "次量单位")
    private String minorUnit;
    @ItemProperty(alias = "煎法")
    private String decoctionId;
    @ItemProperty(alias = "煎法text")
    private String decoctionText;
    @ItemProperty(alias = "剂数")
    private Integer copyNum;
    @ItemProperty(alias = "嘱托")
    private String entrust;
    @ItemProperty(alias = "是否启用")
    private Integer status;
//    @ItemProperty(alias = "创建时间")
//    private Date createDt;
//    @ItemProperty(alias = "最后修改时间")
//    private Date lastModify;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "common_recipe_id")
    public Integer getCommonRecipeId() {
        return commonRecipeId;
    }

    public void setCommonRecipeId(Integer commonRecipeId) {
        this.commonRecipeId = commonRecipeId;
    }

    @Column(name = "make_method_id")
    public String getMakeMethodId() {
        return makeMethodId;
    }

    public void setMakeMethodId(String makeMethodId) {
        this.makeMethodId = makeMethodId;
    }

    @Column(name = "make_method_text")
    public String getMakeMethodText() {
        return makeMethodText;
    }

    public void setMakeMethodText(String makeMethodText) {
        this.makeMethodText = makeMethodText;
    }

    @Column(name = "juice")
    public String getJuice() {
        return juice;
    }

    public void setJuice(String juice) {
        this.juice = juice;
    }

    @Column(name = "juice_unit")
    public String getJuiceUnit() {
        return juiceUnit;
    }

    public void setJuiceUnit(String juiceUnit) {
        this.juiceUnit = juiceUnit;
    }

    @Column(name = "minor")
    public String getMinor() {
        return minor;
    }

    public void setMinor(String minor) {
        this.minor = minor;
    }

    @Column(name = "minor_unit")
    public String getMinorUnit() {
        return minorUnit;
    }

    public void setMinorUnit(String minorUnit) {
        this.minorUnit = minorUnit;
    }

    @Column(name = "decoction_id")
    public String getDecoctionId() {
        return decoctionId;
    }

    public void setDecoctionId(String decoctionId) {
        this.decoctionId = decoctionId;
    }

    @Column(name = "decoction_text")
    public String getDecoctionText() {
        return decoctionText;
    }

    public void setDecoctionText(String decoctionText) {
        this.decoctionText = decoctionText;
    }

    @Column(name = "copy_num")
    public Integer getCopyNum() {
        return copyNum;
    }

    public void setCopyNum(Integer copyNum) {
        this.copyNum = copyNum;
    }

    @Column(name = "entrust")
    public String getEntrust() {
        return entrust;
    }

    public void setEntrust(String entrust) {
        this.entrust = entrust;
    }

    @Column(name = "status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

//    @Column(name = "gmt_create")
//    public Date getCreateDt() {
//        return createDt;
//    }
//
//    public void setCreateDt(Date createDt) {
//        this.createDt = createDt;
//    }
//
//    @Column(name = "gmt_modified")
//    public Date getLastModify() {
//        return lastModify;
//    }
//
//    public void setLastModify(Date lastModify) {
//        this.lastModify = lastModify;
//    }
}
