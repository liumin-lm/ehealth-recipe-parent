package com.ngari.recipe.recipereportform.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class EnterpriseRecipeDetailExcelRequest implements Serializable{

    private static final long serialVersionUID = -290309148717833732L;

    //机构id
    private Integer organId;

    //机构id集合
    private List<Integer> organIdList;

    //开始时间
    private Date startTime;

    //结束时间
    private Date endTime;

    //药企id
    private Integer enterpriseId;

    private String manageUnit;

    /**
     * 支付用户类型:0平台，1机构，2药企
     */
    private Integer payeeCode;
    /**
     * 1配送到家 2医院取药 3药店取药
     */
    private Integer giveMode;
}
