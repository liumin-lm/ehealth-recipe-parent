package com.ngari.recipe.entity;


import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author Created by liuxiaofeng on 2020/9/3.
 *         医生药品频次、用药途径使用次数统计表
 */
@Schema
@Entity
@Table(name = "cdr_doctor_drugusage_count")
@Access(AccessType.PROPERTY)
public class DoctorDrugUsageCount implements Serializable {
    private static final long serialVersionUID = -8367523326716664274L;

    @ItemProperty(alias = "主键")
    private Integer id;

    @ItemProperty(alias = "机构di")
    private Integer organId;

    @ItemProperty(alias = "医生id")
    private Integer doctorId;

    @ItemProperty(alias = "医生科室id")
    private String deptId;

    @ItemProperty(alias = "使用类型 1-用药频次 2-用药途径")
    private Integer usageType;

    @ItemProperty(alias = "使用id")
    private Integer usageId;

    @ItemProperty(alias = "使用次数")
    private Integer usageCount;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "修改时间")
    private Date updateTime;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false, length = 11)
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

    @Column(name = "doctor_id")
    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    @Column(name = "dept_id")
    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    @Column(name = "usage_type")
    public Integer getUsageType() {
        return usageType;
    }

    public void setUsageType(Integer usageType) {
        this.usageType = usageType;
    }

    @Column(name = "usage_id")
    public Integer getUsageId() {
        return usageId;
    }

    public void setUsageId(Integer usageId) {
        this.usageId = usageId;
    }

    @Column(name = "usage_count")
    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    @Column(name = "create_time")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "update_time")
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
