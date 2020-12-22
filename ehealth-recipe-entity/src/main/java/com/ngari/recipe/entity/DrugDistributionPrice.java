package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 药品配送价格表
 *
 * @author jianghc
 *
 **/
@Entity
@Schema
@Table(name = "cdr_distribution_price")
@Access(AccessType.PROPERTY)
public class DrugDistributionPrice implements java.io.Serializable {

    private static final long serialVersionUID = 2672357769913680492L;

    @ItemProperty(alias = "主键")
    private Integer id;

    @ItemProperty(alias = "药企序号")
    private Integer enterpriseId;

    /**
     * 为null时表示全国价格
     */
    @ItemProperty(alias = "区域编码")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String addrArea;

    @ItemProperty(alias = "配送价格")
    private BigDecimal distributionPrice;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "最近修改时间")
    private Date lastModify;


    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "enterpriseId")
    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    @Column(name = "addrArea")
    public String getAddrArea() {
        return addrArea;
    }

    public void setAddrArea(String addrArea) {
        this.addrArea = addrArea;
    }

    @Column(name = "distributionPrice")
    public BigDecimal getDistributionPrice() {
        return distributionPrice;
    }

    public void setDistributionPrice(BigDecimal distributionPrice) {
        this.distributionPrice = distributionPrice;
    }

    @Column(name = "createTime")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "lastModify")
    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    @Override
    public String toString() {
        return new StringBuffer("药企配送价格：{")
                .append("内码：").append(id)
                .append(",药企ID：").append(enterpriseId)
                .append(",配送地域编码：").append(addrArea)
                .append(",配送价格").append(distributionPrice).append("元。").toString();
    }
}
