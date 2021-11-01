package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @description： 支付流水 邵逸夫两次支付专用
 * @author： whf
 * @date： 2021-09-17 18:33
 */
@Entity
@Schema
@Table(name = "cdr_recipe_order_pay_flow")
@Access(AccessType.PROPERTY)
@ToString
public class RecipeOrderPayFlow implements Serializable {


    @ItemProperty(alias = "自增id")
    private Integer id;


    @ItemProperty(alias = "订单主键id")
    private Integer orderId;


    @ItemProperty(alias = "支付流水类型 1.药品费用 2审方费用")
    private Integer payFlowType;


    @ItemProperty(alias = "支付总费用")
    private Double totalFee;


    @ItemProperty(alias = "交易流水号")
    private String tradeNo;


    @ItemProperty(alias = "支付方式")
    private String wxPayWay;


    @ItemProperty(alias = "卫宁付下的支付方式")
    private String wnPayWay;


    @ItemProperty(alias = "商户订单号")
    private String outTradeNo;


    @ItemProperty(alias = "支付平台分配的机构id")
    private String payOrganId;


    @ItemProperty(alias = "支付状态 1.已支付  3.退款成功")
    private Integer payFlag;


    @ItemProperty(alias = "创建时间")
    private Date createTime;


    @ItemProperty(alias = "修改时间")
    private Date modifiedTime;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "order_id")
    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    @Column(name = "pay_flow_type")
    public Integer getPayFlowType() {
        return payFlowType;
    }

    public void setPayFlowType(Integer payFlowType) {
        this.payFlowType = payFlowType;
    }

    @Column(name = "total_fee")
    public Double getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(Double totalFee) {
        this.totalFee = totalFee;
    }

    @Column(name = "trade_no")
    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    @Column(name = "wx_pay_way")
    public String getWxPayWay() {
        return wxPayWay;
    }

    public void setWxPayWay(String wxPayWay) {
        this.wxPayWay = wxPayWay;
    }

    @Column(name = "wn_pay_way")
    public String getWnPayWay() {
        return wnPayWay;
    }

    public void setWnPayWay(String wnPayWay) {
        this.wnPayWay = wnPayWay;
    }

    @Column(name = "out_trade_no")
    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    @Column(name = "pay_organ_id")
    public String getPayOrganId() {
        return payOrganId;
    }

    public void setPayOrganId(String payOrganId) {
        this.payOrganId = payOrganId;
    }

    @Column(name = "pay_flag")
    public Integer getPayFlag() {
        return payFlag;
    }

    public void setPayFlag(Integer payFlag) {
        this.payFlag = payFlag;
    }

    @Column(name = "create_time")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "modified_time")
    public Date getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }
}
