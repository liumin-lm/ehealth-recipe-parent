package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class PatientRecipeDetailReqDTO implements Serializable {
    private static final long serialVersionUID = -5990026917301549956L;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "处方序号")
    private Integer recipeId;

    @ItemProperty(alias = "处方号码，处方回写")
    private String recipeCode;

    @ItemProperty(alias = "处方业务查询来源 1 线上  2 线下 3 院内门诊")
    private Integer recipeBusType;

    @ItemProperty(alias = "处方父状态：0：默认 ， 1：待提交，2：待审核，3：待够药，4：待发药，5：配送中，6：待取药，7：已完成，8：已删除 ，9：已作废")
    private Integer processState;

    @ItemProperty(alias = "主索引（患者编号）")
    private String mpiid;

    @ItemProperty(alias = "开始时间")
    private Date startTime;

    @ItemProperty(alias = "结束时间")
    private Date endTime;
}
