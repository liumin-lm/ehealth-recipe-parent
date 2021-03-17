package com.ngari.recipe.recipe.model;

import com.ngari.opbase.base.mode.HosBusFundsReportResult;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author yepeng
 * @version 1.0
 * @date 2021/3/16 11:31
 */
@Data
public class RecipeOrderFeeVO {

    /**
     * 西药费
     */
    private BigDecimal westMedFee;
    /**
     * 中成药
     */
    private BigDecimal chinesePatentMedFee;
    /**
     * 中药费
     */
    private BigDecimal chineseMedFee;

    /**
     * 医疗费  医保+自费
     */
    private HosBusFundsReportResult.MedFundsDetail medFee;
    //处方单类型
    private Integer recipeType;
    //处方id
    private Integer recipeId;
    //处方支付金额
    private  BigDecimal recipePayMoney;
    //自费
    private  BigDecimal cashMoney;
    //医保
    private  BigDecimal MedicalMoney;

}
