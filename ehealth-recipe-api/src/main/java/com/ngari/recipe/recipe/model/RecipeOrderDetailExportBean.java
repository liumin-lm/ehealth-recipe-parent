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

    @ItemProperty(alias = "快捷购药处方标识： 0其他, 1快捷处方, 2方便门诊")
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

    @ItemProperty(alias = "收货人")
    private String receiver;

    @ItemProperty(alias = "联系方式")
    private String recMobile;

    @ItemProperty(alias = "省")
    private String address1;

    @ItemProperty(alias = "市")
    private String address2;

    @ItemProperty(alias = "区县")
    private String address3;

    @ItemProperty(alias = "街道")
    private String streetAddress;

    @ItemProperty(alias = "社区")
    private String address5Text;

    @ItemProperty(alias = "详细地址")
    private String address4;

    @ItemProperty(alias = "收货地址")
    private String completeAddress;

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

    @ItemProperty(alias = "优惠金额")
    private BigDecimal couponFee;

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

    @ItemProperty(alias = "帖数")
    private String copyNum;

    @ItemProperty(alias = "协定方名称")
    private String offlineRecipeName;

    @ItemProperty(alias = "开方科室")
    private String appointDepartName;

    @ItemProperty(alias = "开方医生")
    private String doctorName;

    @ItemProperty(alias = "就诊人姓名")
    private String patientName;

    @ItemProperty(alias = "开方时间")
    private Date createDate;

    @ItemProperty(alias = "单方药品费")
    private String singleRecipeFee;

    @ItemProperty(alias = "单方药事服务费")
    private String singleAuditFee;

    @ItemProperty(alias = "煎法")
    private String decoctionText;

    @ItemProperty(alias = "是否代煎")
    private String generationisOfDecoction;

    @ItemProperty(alias = "单方代煎费")
    private String singleDecoctionFee;

    @ItemProperty(alias = "退费金额")
    private BigDecimal refundAmount;

}
