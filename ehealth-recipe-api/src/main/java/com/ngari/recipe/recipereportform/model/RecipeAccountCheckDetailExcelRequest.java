package com.ngari.recipe.recipereportform.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class RecipeAccountCheckDetailExcelRequest implements Serializable{

    private static final long serialVersionUID = 5690478282503051625L;

    //机构id
    private Integer organId;

    //机构id集合
    private List<Integer> organIdList;

    //开始时间
    private Date bDate;

    //结束时间
    private Date eDate;

    //处方id
    private Integer recipeId;

    //用户mpiid
    private String mpiid;

    //商户订单号
    private String outTradeNo;

}
