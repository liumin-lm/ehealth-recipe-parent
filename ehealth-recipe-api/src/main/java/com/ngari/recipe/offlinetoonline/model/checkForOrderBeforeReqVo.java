package com.ngari.recipe.offlinetoonline.model;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上获取详情参数
 */
@Data
//@Builder
public class checkForOrderBeforeReqVo implements Serializable {
    private static final long serialVersionUID = -7727248592234567484L;
    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "处方号码，处方回写")
    private String recipeCode;

    @ItemProperty(alias = "主索引（患者编号）")
    private String mpiid;

    /**
     * 卡片号
     */
    @Desensitizations(type = DesensitizationsType.HEALTHCARD)
    private String cardId;
    private String cardType;

    @ItemProperty(alias = "挂号序号")
    private String registerId;

    @ItemProperty(alias = "签名时间")
    private Date signDate;

}


