package com.ngari.recipe.offlinetoonline.model;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import ctd.schema.annotation.ItemProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上获取详情参数
 */
@Data
@Builder
//@AllArgsConstructor
public class OfflineToOnlineReqVO implements Serializable {
    private static final long serialVersionUID = -7727248592234567484L;

    /**
     * 机构
     */
    @NonNull
    private Integer organId;

    /**
     * mpiId
     */
    @NonNull
    private String mpiid;

//    /**
//     * 处方cdr_his_recipe表的hisRecipeId
//     */
//    private Integer hisRecipeId;

    /**
     * 处方号
     */
    @NonNull
    private String recipeCode;

    /**
     * 卡片号
     */
    @Desensitizations(type = DesensitizationsType.HEALTHCARD)
    private String cardId;


    private String processState;

    @ItemProperty(alias = "开始时间")
    private Date startTime;

    @ItemProperty(alias = "结束时间")
    private Date endTime;


    public OfflineToOnlineReqVO(@NonNull Integer organId, @NonNull String mpiid, @NonNull String recipeCode, String cardId, String processState, Date startTime, Date endTime) {
        this.organId = organId;
        this.mpiid = mpiid;
        this.recipeCode = recipeCode;
        this.cardId = cardId;
        this.processState = processState;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public OfflineToOnlineReqVO() {
    }
}


