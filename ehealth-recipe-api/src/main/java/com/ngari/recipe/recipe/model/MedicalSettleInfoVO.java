package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 医保预结算/结算查询信息
 * @author zgy
 * @date 2022/6/24 16:45
 */
@Data
public class MedicalSettleInfoVO implements Serializable {

    @ItemProperty(alias="机构ID")
    private Integer organId;

    @ItemProperty(alias="病历号")
    private String mrn;

    @ItemProperty(alias="就诊流水号")
    private String clinicNo;

    @ItemProperty(alias="his业务预结算单号")
    private String hisSettlementNo;

    @ItemProperty(alias="订单总金额")
    private BigDecimal totalAmount;

    @ItemProperty(alias="处方单号")
    private String recipeNos;

}
