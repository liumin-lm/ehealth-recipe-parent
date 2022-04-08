package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @description： 药企煎法配送地址
 * @author： whf
 * @date： 2022-04-07 10:40
 */
@Entity
@Schema
@Table(name = "cdr_enterprise_decoction_address")
@Access(AccessType.PROPERTY)
public class EnterpriseDecoctionAddress implements java.io.Serializable {
    @ItemProperty(alias = "药企地址序号")
    private Integer id;

    private Integer organId;

    @ItemProperty(alias = "药企序号")
    private Integer enterpriseId;

    @ItemProperty(alias = "药企序号")
    private Integer decoctionId;

    @ItemProperty(alias = "药企配送地址")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address;

    @ItemProperty(alias = "配送地址状态")
    private Integer status;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "最近修改时间")
    private Date modifyTime;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "Id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "organ_id", nullable = false)
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "enterprise_id", nullable = false)
    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    @Column(name = "decoction_id", nullable = false)
    public Integer getDecoctionId() {
        return decoctionId;
    }

    public void setDecoctionId(Integer decoctionId) {
        this.decoctionId = decoctionId;
    }

    @Column(name = "address", nullable = false)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Column(name = "status", nullable = false)
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Column(name = "create_time")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "modify_time")
    public Date getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Date modifyTime) {
        this.modifyTime = modifyTime;
    }
}
