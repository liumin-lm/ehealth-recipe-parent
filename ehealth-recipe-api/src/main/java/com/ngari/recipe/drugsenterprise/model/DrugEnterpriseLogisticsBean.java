package com.ngari.recipe.drugsenterprise.model;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.Date;


/**
 * @description： 药企与物流关系
 * @author： whf
 * @date： 2021-03-30 11:31
 */
@Schema
public class DrugEnterpriseLogisticsBean implements Serializable {

    @ItemProperty(alias = "自增主键")
    private Integer id;

    @ItemProperty(alias = "药企ID")
    private Integer drugsEnterpriseId;

    @ItemProperty(alias = "是否默认物流公司 0 否 1是")
    private Integer isDefault;

    @ItemProperty(alias = "物流公司")
    @Dictionary(id = "eh.cdr.dictionary.LogisticsCode")
    private Integer logisticsCompany;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "最后修改时间")
    private Date updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDrugsEnterpriseId() {
        return drugsEnterpriseId;
    }

    public void setDrugsEnterpriseId(Integer drugsEnterpriseId) {
        this.drugsEnterpriseId = drugsEnterpriseId;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getLogisticsCompany() {
        return logisticsCompany;
    }

    public void setLogisticsCompany(Integer logisticsCompany) {
        this.logisticsCompany = logisticsCompany;
    }


    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }


    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

}
