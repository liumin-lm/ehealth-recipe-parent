package com.ngari.recipe.offlinetoonline.model;


import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author liumin
 * @Date 2023/3/21 上午11:42
 * @Description 线下转线上
 */
@Data
public class SubBatchOfflineToOnlineReqVO implements Serializable {

    private static final long serialVersionUID = -7727248592234567484L;

    /**
     * 处方cdr_his_recipe表的hisRecipeId
     */
//    private Integer hisRecipeId;

    /**
     * 处方号
     */
    public String recipeCode;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

}




