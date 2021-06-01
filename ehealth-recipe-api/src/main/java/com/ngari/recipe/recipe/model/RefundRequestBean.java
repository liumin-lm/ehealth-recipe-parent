package com.ngari.recipe.recipe.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\10\26 0026 18:55
 */
@Data
public class RefundRequestBean implements Serializable{
    private static final long serialVersionUID = -5961047302400890570L;

    private Integer organId;
    private String hospitalCode;
    private Integer recipeId;
    private String recipeCode;
    private Boolean refundFlag;
    private Boolean otherRefundFlag;
    private String remark;
    //来源 1 运营平台药师 2 前置机药企回调 3 前置机HIS
    private Integer source;

}