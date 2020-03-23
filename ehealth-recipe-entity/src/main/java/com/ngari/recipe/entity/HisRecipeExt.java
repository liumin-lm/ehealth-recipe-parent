package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author yinsheng
 * @date 2020\3\10 0010 19:35
 */
@Entity
@Schema
@Table(name = "cdr_his_recipe_ext")
@Access(AccessType.PROPERTY)
public class HisRecipeExt implements Serializable{
    private static final long serialVersionUID = 3634166336031318688L;

    @ItemProperty(alias = "处方扩展序号")
    private Integer hisRecipeExtID; // int(11) NOT NULL AUTO_INCREMENT,
    @ItemProperty(alias = "his处方序号")
    private Integer hisRecipeId; // int(11) NOT NULL COMMENT 'his处方序号',
    @ItemProperty(alias = "文本描述")
    private String  extendText; // varchar(50) DEFAULT NULL COMMENT '文本描述',
    @ItemProperty(alias = "文本值")
    private String  extendValue; // varchar(100) DEFAULT NULL COMMENT '文本值',
    @ItemProperty(alias = "文本值类型")
    private Integer valueType; // tinyint(1) DEFAULT NULL COMMENT '1 数值 2 链接 3 文本 4 图片',
    @ItemProperty(alias = "排序值")
    private Integer sort; // tinyint(1) DEFAULT NULL COMMENT '排序值',

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "hisRecipeExtID", unique = true, nullable = false)
    public Integer getHisRecipeExtID() {
        return hisRecipeExtID;
    }

    public void setHisRecipeExtID(Integer hisRecipeExtID) {
        this.hisRecipeExtID = hisRecipeExtID;
    }

    @Column(name = "hisRecipeId")
    public Integer getHisRecipeId() {
        return hisRecipeId;
    }

    public void setHisRecipeId(Integer hisRecipeId) {
        this.hisRecipeId = hisRecipeId;
    }

    @Column(name = "extendText")
    public String getExtendText() {
        return extendText;
    }

    public void setExtendText(String extendText) {
        this.extendText = extendText;
    }

    @Column(name = "extendValue")
    public String getExtendValue() {
        return extendValue;
    }

    public void setExtendValue(String extendValue) {
        this.extendValue = extendValue;
    }

    @Column(name = "valueType")
    public Integer getValueType() {
        return valueType;
    }

    public void setValueType(Integer valueType) {
        this.valueType = valueType;
    }

    @Column(name = "sort")
    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
