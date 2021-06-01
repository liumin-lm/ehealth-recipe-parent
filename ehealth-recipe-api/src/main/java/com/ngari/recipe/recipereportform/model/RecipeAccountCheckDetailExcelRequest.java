package com.ngari.recipe.recipereportform.model;

import lombok.Data;

import java.io.Serializable;
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
    private Date startTime;

    //结束时间
    private Date endTime;

    //处方id
    private String recipeId;

    //用户mpiid
    private String mpiid;

    //商户订单号
    private String tradeNo;

    private String manageUnit;

    /**
     * 订单退款标识 0未退费 1已退费
     */
    private Integer refundFlag;

}
