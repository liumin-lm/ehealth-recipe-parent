package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 药企配置表
 */
@Entity
@Schema
@Table(name = "cdr_drugsenterpriseconfig")
public class DrugsEnterpriseConfig implements Serializable {

    @ItemProperty(alias = "id")
    private Integer id;

    @ItemProperty(alias = "药企ID")
    private Integer drugsenterpriseId;

    @ItemProperty(alias = "同步医院药品开关，默认为0（0：开关关闭；1：开关打开）")
    private Integer enable_drug_sync;

    @ItemProperty(alias = "同步数据来源 现阶段默认 1:关联机构 ")
    private Integer syncDataSource;

    @ItemProperty(alias = "数据同步类型  字典key用 ,隔开 eh.base.dictionary.SyncDrugType ")
    private String enable_drug_syncType;

    @ItemProperty(alias = "药企药品编码保存字段  字典  选择 默认1:机构药品编码")
    @Dictionary(id = "eh.base.dictionary.SyncSaleDrugCodeType")
    private Integer syncSaleDrugCodeType;

    @ItemProperty(alias = "同步数据范围  1配送药企  2 药品类型")
    private Integer syncDataRange;

    @ItemProperty(alias = "同步药品类型  字典key用 ，隔开  eh.base.dictionary.DrugType")
    private String syncDrugType;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "drugsenterpriseId")
    public Integer getDrugsenterpriseId() {
        return drugsenterpriseId;
    }

    public void setDrugsenterpriseId(Integer drugsenterpriseId) {
        this.drugsenterpriseId = drugsenterpriseId;
    }

    @Column(name = "enable_drug_sync")
    public Integer getEnable_drug_sync() {
        return enable_drug_sync;
    }

    public void setEnable_drug_sync(Integer enable_drug_sync) {
        this.enable_drug_sync = enable_drug_sync;
    }

    @Column(name = "syncDataSource")
    public Integer getSyncDataSource() {
        return syncDataSource;
    }

    public void setSyncDataSource(Integer syncDataSource) {
        this.syncDataSource = syncDataSource;
    }

    @Column(name = "enable_drug_syncType")
    public String getEnable_drug_syncType() {
        return enable_drug_syncType;
    }

    public void setEnable_drug_syncType(String enable_drug_syncType) {
        this.enable_drug_syncType = enable_drug_syncType;
    }

    @Column(name = "syncSaleDrugCodeType")
    public Integer getSyncSaleDrugCodeType() {
        return syncSaleDrugCodeType;
    }

    public void setSyncSaleDrugCodeType(Integer syncSaleDrugCodeType) {
        this.syncSaleDrugCodeType = syncSaleDrugCodeType;
    }

    @Column(name = "syncDataRange")
    public Integer getSyncDataRange() {
        return syncDataRange;
    }

    public void setSyncDataRange(Integer syncDataRange) {
        this.syncDataRange = syncDataRange;
    }

    @Column(name = "syncDrugType")
    public String getSyncDrugType() {
        return syncDrugType;
    }

    public void setSyncDrugType(String syncDrugType) {
        this.syncDrugType = syncDrugType;
    }
}
