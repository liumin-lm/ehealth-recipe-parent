//package com.ngari.recipe.drugdistributionprice.model;
//
//import ctd.schema.annotation.Dictionary;
//import ctd.schema.annotation.ItemProperty;
//import ctd.schema.annotation.Schema;
//
//import javax.persistence.*;
//import java.math.BigDecimal;
//
//import static javax.persistence.GenerationType.IDENTITY;
//
///**
// * 药品配送价格表
// *
// * @author jianghc
// *
// **/
//@Schema
//public class DrugDistributionPriceBean implements java.io.Serializable {
//
//    private static final long serialVersionUID = 2672357769913680492L;
//
//    @ItemProperty(alias = "主键")
//    private Integer id;
//
//    @ItemProperty(alias = "药企序号")
//    private Integer enterpriseId;
//
//    /**
//     * 为null时表示全国价格
//     */
//    @ItemProperty(alias = "区域编码")
//    @Dictionary(id = "eh.base.dictionary.AddrArea")
//    private String addrArea;
//
//    @ItemProperty(alias = "配送价格")
//    private BigDecimal distributionPrice;
//
//
//    @ItemProperty(alias = "金额满多少包邮")
//    private BigDecimal buyFreeShipping;
//
//    public DrugDistributionPriceBean() {
//    }
//
//    public Integer getId() {
//        return id;
//    }
//
//    public void setId(Integer id) {
//        this.id = id;
//    }
//
//    public Integer getEnterpriseId() {
//        return enterpriseId;
//    }
//
//    public void setEnterpriseId(Integer enterpriseId) {
//        this.enterpriseId = enterpriseId;
//    }
//
//    public String getAddrArea() {
//        return addrArea;
//    }
//
//    public void setAddrArea(String addrArea) {
//        this.addrArea = addrArea;
//    }
//
//    public BigDecimal getDistributionPrice() {
//        return distributionPrice;
//    }
//
//    public void setDistributionPrice(BigDecimal distributionPrice) {
//        this.distributionPrice = distributionPrice;
//    }
//
//    public BigDecimal getBuyFreeShipping() {
//        return buyFreeShipping;
//    }
//
//    public void setBuyFreeShipping(BigDecimal buyFreeShipping) {
//        this.buyFreeShipping = buyFreeShipping;
//    }
//
//    @Override
//    public String toString() {
//        return new StringBuffer("药企配送价格：{")
//                .append("内码：").append(id)
//                .append(",药企ID：").append(enterpriseId)
//                .append(",配送地域编码：").append(addrArea)
//                .append(",配送价格").append(distributionPrice).append("元。").toString();
//    }
//}
