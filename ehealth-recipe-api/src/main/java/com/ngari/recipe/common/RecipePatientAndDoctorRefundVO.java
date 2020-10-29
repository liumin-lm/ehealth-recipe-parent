package com.ngari.recipe.common;

import lombok.Data;

import java.io.Serializable;

/**
 * Copyright (C) 2009-2020 by ngarihealth,Inc.All rights Reserved
 *
 * @author dingxx
 * @Description:退款信息详情VO
 * @date 2020/10/24 14:24
 */
@Data
public class RecipePatientAndDoctorRefundVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 医生姓名
     */
    private String doctorName;
    /**
     * 第三方审核状态
     */
    private Integer recipeRefundStatus;
    /**
     * 第三方审核状态文案
     */
    private String recipeRefundStatusThirdMsg;
    /**
     * 第三方审核不通过原因
     */
    private String reasonForNoPass;
    private RecipePatientRefundVO recipePatientRefundVO;

    public RecipePatientAndDoctorRefundVO(String doctorName, RecipePatientRefundVO recipePatientRefundVO) {
        this.recipePatientRefundVO = recipePatientRefundVO;
        this.doctorName = doctorName;
    }
}
