package com.ngari.recipe.entity;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;

import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 药企配送地址
 * @company: Ngarihealth
 * @author: zhongzixuan
 * @date:2016/6/8.
 */

@Entity
@Schema
@Table(name = "cdr_enterprise_address")
@Access(AccessType.PROPERTY)
public class EnterpriseAddress implements java.io.Serializable{

    private static final long serialVersionUID = 6110497203150534282L;

    @ItemProperty(alias = "药企地址序号")
    private Integer id;

    @ItemProperty(alias = "药企序号")
    private Integer enterpriseId;

    @ItemProperty(alias = "药企配送地址")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address;

    @ItemProperty(alias = "配送地址状态")
    private Integer status;

    @ItemProperty(alias = "配送价格")
    private BigDecimal distributionPrice;

    @ItemProperty(alias = "金额满多少包邮")
    private BigDecimal buyFreeShipping;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "最近修改时间")
    private Date lastModify;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "Id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "EnterpriseId", nullable = false)
    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    @Column(name = "Address", nullable = false)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Column(name = "Status", nullable = false)
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    @Column(name = "distribution_price")
    public BigDecimal getDistributionPrice() {
        return distributionPrice;
    }

    public void setDistributionPrice(BigDecimal distributionPrice) {
        this.distributionPrice = distributionPrice;
    }

    @Column(name = "buy_free_shipping")
    public BigDecimal getBuyFreeShipping() {
        return buyFreeShipping;
    }

    public void setBuyFreeShipping(BigDecimal buyFreeShipping) {
        this.buyFreeShipping = buyFreeShipping;
    }
}
