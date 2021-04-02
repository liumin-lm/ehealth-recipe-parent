package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @description：药企物流关联表
 * @author： whf
 * @date： 2021-03-30 11:09
 */
@Entity
@Schema
@Table(name = "drug_enterprise_logistics")
@Access(AccessType.PROPERTY)
public class DrugEnterpriseLogistics {

    @ItemProperty(alias = "自增主键")
    private Integer id;

    @ItemProperty(alias = "药企ID")
    private Integer drugsEnterpriseId;

    @ItemProperty(alias = "是否默认物流公司 0 否 1是")
    private Integer isDefault;

    @ItemProperty(alias = "物流公司")
//    @Dictionary(id = "eh.cdr.dictionary.LogisticsCode")
    private Integer logisticsCompany;
    @ItemProperty(alias = "物流公司名称")
    private String logisticsCompanyName;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "最后修改时间")
    private Date updateTime;


    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "drugs_enterprise_id")
    public Integer getDrugsEnterpriseId() {
        return drugsEnterpriseId;
    }

    public void setDrugsEnterpriseId(Integer drugsEnterpriseId) {
        this.drugsEnterpriseId = drugsEnterpriseId;
    }

    @Column(name = "is_default")
    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }

    @Column(name = "logistics_company")
    public Integer getLogisticsCompany() {
        return logisticsCompany;
    }

    public void setLogisticsCompany(Integer logisticsCompany) {
        this.logisticsCompany = logisticsCompany;
    }

    @Column(name = "logistics_company_name")
    public String getLogisticsCompanyName() {
        return logisticsCompanyName;
    }

    public void setLogisticsCompanyName(String logisticsCompanyName) {
        this.logisticsCompanyName = logisticsCompanyName;
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
