package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author zgy
 * @date 2022/7/21 16:53
 */
@Data
public class RecipeOrderDetailExportBean implements Serializable {
    @ItemProperty(alias = "处方发起者id,用户标识")
    private String requestMpiId;

    @ItemProperty(alias = "快捷购药处方标识 0 非快捷处方 1 快捷处方")
    private String fastRecipeFlag;

    @ItemProperty(alias = "订单号")
    private String orderCode;

    @ItemProperty(alias = "订单状态")
    private String processState;

    @ItemProperty(alias = "退款状态")
    private String refundNodeStatus;

    @ItemProperty(alias = "购药方式")
    private String giveModeText;

    @ItemProperty(alias = "供药药企")
    private String name;

    @ItemProperty(alias = "供药药店")
    private String drugStoreName;

    @ItemProperty(alias = "下单人")
    private String requestPatientName;

    @ItemProperty(alias = "下单手机号")
    private String mobile;

    @ItemProperty(alias = "下单时间")
    private Date orderTime;

    @ItemProperty(alias = "支付时间")
    private Date payTime;

    @ItemProperty(alias = "支付流水号")
    private String tradeNo;

    @ItemProperty(alias = "订单金额")
    private BigDecimal totalFee;

    @ItemProperty(alias = "结算类型")
    private String orderTypeText;

    @ItemProperty(alias = "医保金额")
    private Double fundAmount;

    @ItemProperty(alias = "自费金额")
    private Double cashAmount;

    @ItemProperty(alias = "药品费")
    private BigDecimal recipeFee;

    @ItemProperty(alias = "运费")
    private BigDecimal expressFee;

    @ItemProperty(alias = "代煎费")
    private BigDecimal decoctionFee;

    @ItemProperty(alias = "中医辨证论治费")
    private BigDecimal tcmFee;

    @ItemProperty(alias = "药事服务费")
    private BigDecimal auditFee;

    @ItemProperty(alias = "挂号费")
    private BigDecimal registerFee;

    @ItemProperty(alias = "平台处方单号")
    private Integer recipeId;

    @ItemProperty(alias = "HIS处方编码")
    private String recipeCode;

    @ItemProperty(alias = "处方类型")
    private String recipeType;

    @ItemProperty(alias = "开方科室")
    private String appointDepartName;

    @ItemProperty(alias = "开方医生")
    private String doctorName;

    @ItemProperty(alias = "就诊人姓名")
    private String patientName;

    @ItemProperty(alias = "开方时间")
    private Date createDate;

    @ItemProperty(alias = "药品名称")
    private String drugName;

    @ItemProperty(alias = "机构药品ID")
    private String organDrugCode;

    @ItemProperty(alias = "药企药品ID")
    private String saleDrugCode;

    @ItemProperty(alias = "药品单价")
    private BigDecimal salePrice;

    @ItemProperty(alias = "出售数量")
    private Double useTotalDose;

    @ItemProperty(alias = "出售单位")
    private String drugUnit;

    @ItemProperty(alias = "煎法")
    private String decoctionText;

    @ItemProperty(alias = "是否代煎")
    private String generationisOfDecoction;


}
