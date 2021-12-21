package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author yins
 * 处方字典对照表
 */
@Schema
@Entity
@Table(name = "base_recipe_dictionary")
@Access(AccessType.PROPERTY)
public class RecipeDictionary implements Serializable {
    private static final long serialVersionUID = -7151292954468997258L;
    @ItemProperty(alias = "字典id")
    private Integer id;
    @ItemProperty(alias = "机构id")
    private Integer organId;
    @ItemProperty(alias = "字典类型 1 超量原因")
    private Integer dictionaryType;
    @ItemProperty(alias = "字典编码")
    private String dictionaryCode;
    @ItemProperty(alias = "字典名称")
    private String dictionaryName;
    @ItemProperty(alias = "项目价格")
    private BigDecimal itemPrice;
    @ItemProperty(alias = "字典拼音")
    private String dictionaryPingying;
    @ItemProperty(alias = "1 表示删除，0 表示未删除")
    private Integer isDelete;
    @ItemProperty(alias = "字典排序")
    private Integer dictionarySort;
    @ItemProperty(alias = "创建时间")
    private Date gmtCreate;

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

    @Column(name = "dictionary_type")
    public Integer getDictionaryType() {
        return dictionaryType;
    }

    public void setDictionaryType(Integer dictionaryType) {
        this.dictionaryType = dictionaryType;
    }

    @Column(name = "dictionary_code")
    public String getDictionaryCode() {
        return dictionaryCode;
    }

    public void setDictionaryCode(String dictionaryCode) {
        this.dictionaryCode = dictionaryCode;
    }

    @Column(name = "dictionary_name")
    public String getDictionaryName() {
        return dictionaryName;
    }

    public void setDictionaryName(String dictionaryName) {
        this.dictionaryName = dictionaryName;
    }

    @Column(name = "item_price")
    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }

    @Column(name = "dictionary_pingying")
    public String getDictionaryPingying() {
        return dictionaryPingying;
    }

    public void setDictionaryPingying(String dictionaryPingying) {
        this.dictionaryPingying = dictionaryPingying;
    }

    @Column(name = "is_deleted")
    public Integer getIsDelete() {
        return isDelete;
    }

    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }

    @Column(name = "dictionary_sort")
    public Integer getDictionarySort() {
        return dictionarySort;
    }

    public void setDictionarySort(Integer dictionarySort) {
        this.dictionarySort = dictionarySort;
    }

    @Column(name = "gmt_create")
    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }
}
