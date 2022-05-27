//package com.ngari.recipe.entity;
//
//import ctd.schema.annotation.ItemProperty;
//import ctd.schema.annotation.Schema;
//
//import javax.persistence.*;
//import java.io.Serializable;
//
//import static javax.persistence.GenerationType.IDENTITY;
//
///**
// * 药品销售策略表
// */
//@Entity
//@Schema
//@Table(name = "base_drug_sale_strategy")
//@Access(AccessType.PROPERTY)
//public class DrugSaleStrategy implements Serializable {
//    private static final long serialVersionUID = -2698550899257755L;
//
//    @ItemProperty(alias = "序号")
//    private Integer id;
//
//    @ItemProperty(alias = "药品ID")
//    private Integer drugId;
//
//    @ItemProperty(alias = "策略名称")
//    private String strategyTitle;
//
//    @ItemProperty(alias = "策略单位")
//    private String drugUnit;
//
//    @ItemProperty(alias = "销售系数比")
//    private Integer drugAmount;
//
//    @ItemProperty(alias = "状态 ，0：删除，1 正常")
//    private Integer status;
//
//    @Id
//    @GeneratedValue(strategy = IDENTITY)
//    @Column(name = "Id", unique = true, nullable = false)
//    public Integer getId() {
//        return id;
//    }
//
//    public void setId(Integer id) {
//        this.id = id;
//    }
//
//    @Column(name = "drug_id", length = 10)
//    public Integer getDrugId() {
//        return drugId;
//    }
//
//    public void setDrugId(Integer drugId) {
//        this.drugId = drugId;
//    }
//
//    @Column(name = "strategy_title", length = 50)
//    public String getStrategyTitle() {
//        return strategyTitle;
//    }
//
//    public void setStrategyTitle(String strategyTitle) {
//        this.strategyTitle = strategyTitle;
//    }
//
//    @Column(name = "drug_unit", length = 50)
//    public String getDrugUnit() {
//        return drugUnit;
//    }
//
//    public void setDrugUnit(String drugUnit) {
//        this.drugUnit = drugUnit;
//    }
//
//    @Column(name = "drug_amount", length = 11)
//    public Integer getDrugAmount() {
//        return drugAmount;
//    }
//
//    public void setDrugAmount(Integer drugAmount) {
//        this.drugAmount = drugAmount;
//    }
//
//    @Column(name = "status", length = 1)
//    public Integer getStatus() {
//        return status;
//    }
//
//    public void setStatus(Integer status) {
//        this.status = status;
//    }
//}
