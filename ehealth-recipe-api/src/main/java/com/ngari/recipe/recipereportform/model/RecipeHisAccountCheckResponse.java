package com.ngari.recipe.recipereportform.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class RecipeHisAccountCheckResponse implements Serializable{
    private static final long serialVersionUID = -6587243463310827837L;

    private Long total; //总计

    private Integer recipeId; //处方单号

    private String patientName; //患者姓名

    private String buyMedicineWay; //购药方式

    private Date payDate; //支付时间

    private BigDecimal totalFee; //总费用

    private BigDecimal medicalInsurancePlanningFee; //医保统筹

    private BigDecimal selfPayFee; //自费金额

    private String tradeNo; //商户订单号

    //    private BigDecimal hiselfPayFee; //his医保金额
    //
    //    private BigDecimal hisMedicalInsuranceFee; //his医保金额
    //
    //    private BigDecimal hisTotalFee; //his总金额
    //
    private String hisRecipeId; //his处方号

    private Integer organId;

    private String mpiId;

    private Integer giverMode;

    //    private String reason; //产生原因
    //
    //    private String status; //状态

    //    private Date checkAccountDate; //对账时间
}
