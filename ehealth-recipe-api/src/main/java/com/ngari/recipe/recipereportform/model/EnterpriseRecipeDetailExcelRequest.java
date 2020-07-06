package com.ngari.recipe.recipereportform.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
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
}
