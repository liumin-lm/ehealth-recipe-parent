package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

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

    //作废
//    @ItemProperty(alias = "同步数据范围  1配送药企  2 药品类型")
//    private Integer syncDataRange;
//
//    //作废
//    @ItemProperty(alias = "同步药品类型  字典key用 ，隔开  eh.base.dictionary.DrugType")
//    private String syncDrugType;

    @ItemProperty(alias = "药企药品同步关联机构")
    private String organId;

    @ItemProperty(alias = "新增 同步数据范围  1配送药企  2 药品类型")
    private Integer addSyncDataRange;

    @ItemProperty(alias = "修改 同步数据范围  1配送药企  2 药品类型")
    private Integer updateSyncDataRange;

    @ItemProperty(alias = "删除 同步数据范围  1配送药企  2 药品类型")
    private Integer delSyncDataRange;

    @ItemProperty(alias = "新增 同步药品类型  字典key用 ，隔开  eh.base.dictionary.DrugType")
    private String addSyncDrugType;

    @ItemProperty(alias = "修改 同步药品类型  字典key用 ，隔开  eh.base.dictionary.DrugType")
    private String updateSyncDrugType;

    @ItemProperty(alias = "删除 同步药品类型  字典key用 ，隔开  eh.base.dictionary.DrugType")
    private String delSyncDrugType;

    @ItemProperty(alias = "药企药品目录同步字段")
    private List<SaleDrugListSyncField> saleDrugListSyncFieldList;

    @Transient
    public List<SaleDrugListSyncField> getSaleDrugListSyncFieldList() {
        return saleDrugListSyncFieldList;
    }

    public void setSaleDrugListSyncFieldList(List<SaleDrugListSyncField> saleDrugListSyncFieldList) {
        this.saleDrugListSyncFieldList = saleDrugListSyncFieldList;
    }

    @Column(name = "add_sync_drug_type")
    public String getAddSyncDrugType() {
        return addSyncDrugType;
    }

    public void setAddSyncDrugType(String addSyncDrugType) {
        this.addSyncDrugType = addSyncDrugType;
    }

    @Column(name = "update_sync_drug_type")
    public String getUpdateSyncDrugType() {
        return updateSyncDrugType;
    }

    public void setUpdateSyncDrugType(String updateSyncDrugType) {
        this.updateSyncDrugType = updateSyncDrugType;
    }

    @Column(name = "del_sync_drug_type")
    public String getDelSyncDrugType() {
        return delSyncDrugType;
    }

    public void setDelSyncDrugType(String delSyncDrugType) {
        this.delSyncDrugType = delSyncDrugType;
    }

    @Column(name = "add_sync_data_range")
    public Integer getAddSyncDataRange() {
        return addSyncDataRange;
    }

    public void setAddSyncDataRange(Integer addSyncDataRange) {
        this.addSyncDataRange = addSyncDataRange;
    }

    @Column(name = "update_sync_data_range")
    public Integer getUpdateSyncDataRange() {
        return updateSyncDataRange;
    }

    public void setUpdateSyncDataRange(Integer updateSyncDataRange) {
        this.updateSyncDataRange = updateSyncDataRange;
    }

    @Column(name = "del_sync_data_range")
    public Integer getDelSyncDataRange() {
        return delSyncDataRange;
    }

    public void setDelSyncDataRange(Integer delSyncDataRange) {
        this.delSyncDataRange = delSyncDataRange;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }

    @Column(name = "organ_id")
    public String getOrganId() {
        return organId;
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

//    @Column(name = "syncDataRange")
//    public Integer getSyncDataRange() {
//        return syncDataRange;
//    }
//
//    public void setSyncDataRange(Integer syncDataRange) {
//        this.syncDataRange = syncDataRange;
//    }
//
//    @Column(name = "syncDrugType")
//    public String getSyncDrugType() {
//        return syncDrugType;
//    }
//
//    public void setSyncDrugType(String syncDrugType) {
//        this.syncDrugType = syncDrugType;
//    }
}
