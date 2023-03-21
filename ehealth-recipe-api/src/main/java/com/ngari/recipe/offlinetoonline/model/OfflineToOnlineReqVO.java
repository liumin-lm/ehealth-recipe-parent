package com.ngari.recipe.offlinetoonline.model;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import ctd.schema.annotation.ItemProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上获取详情参数
 */
@Data
@Builder
public class OfflineToOnlineReqVO implements Serializable {
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
    @Desensitizations(type = DesensitizationsType.HEALTHCARD)
    private String cardId;

    /**
     * onready（待处理）ongoing（进行中）isover（已完成）
     */
    private String status;

    @ItemProperty(alias = "开始时间")
    private Date startTime;

    @ItemProperty(alias = "结束时间")
    private Date endTime;


    public OfflineToOnlineReqVO() {
    }
}


