package com.ngari.recipe.recipe.model;
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
     * 科室代码
     */
    private String departId;
    /**
     * 科室名称
     */
    private String departName;

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
     * 膏方费
     */
    private BigDecimal pasteMedFee;

    /**
     *  医保 自费
     */
    private BigDecimal medicalAmount;
    private BigDecimal personalAmount;
    /**
     * 医疗费
     */
    private BigDecimal totalAmount;

    //处方单类型
    private Integer recipeType;
    //处方id
    private Integer recipeId;
    //处方药物类型  支付金额
    private  BigDecimal recipePayMoney;

}
