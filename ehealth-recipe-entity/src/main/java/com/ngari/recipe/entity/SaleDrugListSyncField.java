package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;

import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 药企药品目录同步字段
 *
 *
 */
@Entity
@Schema
@Table(name = "saledruglist_sync_field")
@Access(AccessType.PROPERTY)
public class SaleDrugListSyncField implements java.io.Serializable {
    private static final long serialVersionUID = -7090271704460035622L;

    @ItemProperty(alias = "id")
    private Integer id;

    @ItemProperty(alias = "药企id")
    private Integer drugsenterpriseId;

    @ItemProperty(alias = "<dic>\n" +
            "\t<item key=\"1\" text=\"新增药品\"/>\n" +
            "\t<item key=\"2\" text=\"更新药品\"/>\n" +
            "\t<item key=\"3\" text=\"删除药品\"/>\n" +
            "</dic>")
    private String type;

    @ItemProperty(alias = "字段名称")
    private String fieldName;

    @ItemProperty(alias = "字段编码")
    private String fieldCode;

    @ItemProperty(alias = "是否同步 同步勾选 0否 1是")
    private String isSync;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "更新时间")
    private Date updateTime;

    @Column(name = "create_time", length = 19)
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "update_time", length = 19)
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "drugsenterprise_id")
    public Integer getDrugsenterpriseId() {
        return drugsenterpriseId;
    }

    public void setDrugsenterpriseId(Integer drugsenterpriseId) {
        this.drugsenterpriseId = drugsenterpriseId;
    }

    @Column(name = "type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "field_name")
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Column(name = "field_code")
    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    @Column(name = "is_sync")
    public String getIsSync() {
        return isSync;
    }

    public void setIsSync(String isSync) {
        this.isSync = isSync;
    }
    //    @ItemProperty(alias = "药企药品编码 同药企药品目录organDrugCode organDrugCode命名不合适")
//    private Integer saleDrugCode;
//
//    @ItemProperty(alias = "机构药品名称")
//    private String drugName;
//
//    @ItemProperty(alias = "商品名称")
//    private String saleName;
//
//    @ItemProperty(alias = "机构药品规格")
//    private String drugSpec;
//
//    @ItemProperty(alias = "无税单价")
//    private BigDecimal price;
//
//    @ItemProperty(alias = "使用状态")
//    @Dictionary(id = "eh.base.dictionary.OrganDrugStatus")
//    private Integer status;




}