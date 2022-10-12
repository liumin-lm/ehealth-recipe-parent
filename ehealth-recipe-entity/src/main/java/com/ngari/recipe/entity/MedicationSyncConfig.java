package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Time;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @Description 用药频次、用药途径同步配置表
 * @Author zgy
 * @Date 2022-10-11
 */

@Entity
@Schema
@DynamicInsert
@DynamicUpdate
@Table(name = "cdr_medication_sync_config")
@Access(AccessType.PROPERTY)
public class MedicationSyncConfig implements Serializable {

    private static final long serialVersionUID = 8154374325419128688L;

    @ItemProperty(alias = "主键id")
    private Integer id;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "定时时间")
    private Time regularTime;

    @ItemProperty(alias = "同步开关")
    private Boolean enableSync;

    @ItemProperty(alias = "接口对接模式 1 自主查询  2 主动推送   默认 1")
    private Integer dockingMode;

    @ItemProperty(alias = "数据类型 1 用药途径 2 用药频次")
    private Integer dataType;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
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
    @Column(name = "regular_time")
    public Time getRegularTime() {
        return regularTime;
    }

    public void setRegularTime(Time regularTime) {
        this.regularTime = regularTime;
    }

    @Column(name = "enable_sync")
    public Boolean getEnableSync() {
        return enableSync;
    }

    public void setEnableSync(Boolean enableSync) {
        this.enableSync = enableSync;
    }

    @Column(name = "docking_mode")
    public Integer getDockingMode() {
        return dockingMode;
    }

    public void setDockingMode(Integer dockingMode) {
        this.dockingMode = dockingMode;
    }

    @Column(name = "data_type")
    public Integer getDataType() {
        return dataType;
    }

    public void setDataType(Integer dataType) {
        this.dataType = dataType;
    }

}
