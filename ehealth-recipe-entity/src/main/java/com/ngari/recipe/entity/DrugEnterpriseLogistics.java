package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @description：药企物流关联表
 * @author： whf
 * @date： 2021-03-30 11:09
 */
//@Entity
//@Schema
//@Table(name = "drug_enterprise_logistics")
//@Access(AccessType.PROPERTY)
@Data
public class DrugEnterpriseLogistics {

    @ItemProperty(alias = "自增主键")
    private Integer id;

    @ItemProperty(alias = "药企ID")
    private Integer drugsEnterpriseId;

    @ItemProperty(alias = "是否默认物流公司 0 否 1是")
    private Integer isDefault;

    @ItemProperty(alias = "物流公司")
    private Integer logisticsCompany;

    @ItemProperty(alias = "物流公司名称")
    private String logisticsCompanyName;

    @ItemProperty(alias = "是否同城快递")
    private Integer isExpressDelivery;

    @ItemProperty(alias = "发件城市编码")
    private String consignorCityCode;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "最后修改时间")
    private Date updateTime;

}
