package com.ngari.recipe.offlinetoonline.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上获取详情参数
 */
@Data
@Builder
public class FindHisRecipeDetailReqVO implements Serializable {
    private static final long serialVersionUID = -7727248592234567484L;

    /**
     * 机构
     */
    private Integer organId;

    /**
     * mpiId
     */
    private String mpiId;

    /**
     * 处方cdr_his_recipe表的hisRecipeId
     */
    private Integer hisRecipeId;

    /**
     * 处方号
     */
    private String recipeCode;

    /**
     * 卡片号
     */
    private String cardId;

    /**
     * onready（待处理）ongoing（进行中）isover（已完成）
     */
    private String status;

    /**
     * timeQuantum 时间段  1 代表一个月  3 代表三个月 6 代表6个月
     * 23 代表3天
     */
    private Integer timeQuantum;

    public FindHisRecipeDetailReqVO(Integer organId, String mpiId, Integer hisRecipeId, String recipeCode, String cardId, String status) {
        this.organId = organId;
        this.mpiId = mpiId;
        this.hisRecipeId = hisRecipeId;
        this.recipeCode = recipeCode;
        this.cardId = cardId;
        this.status = status;
    }

    public FindHisRecipeDetailReqVO(Integer organId, String mpiId, Integer hisRecipeId, String recipeCode, String cardId, String status, Integer timeQuantum) {
        this.organId = organId;
        this.mpiId = mpiId;
        this.hisRecipeId = hisRecipeId;
        this.recipeCode = recipeCode;
        this.cardId = cardId;
        this.status = status;
        this.timeQuantum = timeQuantum;
    }

    public FindHisRecipeDetailReqVO() {
    }
}


