package com.ngari.recipe.offlinetoonline.model;


import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @Author liumin
 * @Date 2023/3/21 上午11:42
 * @Description 线下转线上
 */
@Data
public class BatchOfflineToOnlineReqVO implements Serializable {
    private static final long serialVersionUID = -7727248592234567484L;

    private List<SubBatchOfflineToOnlineReqVO> subParams;

    /**
     * 机构
     */
    private Integer organId;

    /**
     * mpiId
     */
    private String mpiId;

    /**
     * 卡片号
     */
    @Desensitizations(type = DesensitizationsType.HEALTHCARD)
    private String cardId;

    private String processState;


    @Data
    public class SubBatchOfflineToOnlineReqVO{
        /**
         * 处方cdr_his_recipe表的hisRecipeId
         */
//        private Integer hisRecipeId;

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

        public SubBatchOfflineToOnlineReqVO() {
        }

        public SubBatchOfflineToOnlineReqVO(String recipeCode, Date startTime, Date endTime) {
            this.recipeCode = recipeCode;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public BatchOfflineToOnlineReqVO() {
    }

    public BatchOfflineToOnlineReqVO(List<SubBatchOfflineToOnlineReqVO> subParams, Integer organId, String mpiId, String cardId, String processState) {
        this.subParams = subParams;
        this.organId = organId;
        this.mpiId = mpiId;
        this.cardId = cardId;
        this.processState = processState;
    }
}


