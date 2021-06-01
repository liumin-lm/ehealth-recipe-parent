package com.ngari.recipe.recipereportform.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class RecipeReportFormsRequest implements Serializable{
    private static final long serialVersionUID = -2063260885851045109L;

    private List<Integer> organIdList;

    private Integer organId; //机构id

    private Date startTime; //开始时间

    private Date endTime; //结束时间

    private String year; //年份

    private String month; //月份

    private String recipeId; //处方单号

    private String patientName; //患者姓名

    private String mpiId; //患者id

    private String tradeNo; //商户订单号

    private Integer enterpriseId; //药企

    private String buyMedicWay; //购药方式

    private Integer start;

    private Integer limit;

    private String manageUnit;

    /**
     * 支付用户类型:0平台，1机构，2药企
     */
    private Integer payeeCode;
    /**
     * 1配送到家 2医院取药 3药店取药
     */
    private Integer giveMode;
    /**
     * 订单退款标识 0未退费 1已退费
     */
    private Integer refundFlag;
}
