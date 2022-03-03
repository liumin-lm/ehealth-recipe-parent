package com.ngari.recipe.dto;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author zgy
 * @date 2022/3/2 14:28
 */
@Data
public class OutPatientRecordResDTO implements Serializable {

    private static final long serialVersionUID = -5817152924841458185L;

    @ItemProperty(alias = "挂号科室编码")
    private String appointDepartCode;

    @ItemProperty(alias = "挂号科室名称")
    private String appointDepartName;

    @ItemProperty(alias = "挂号科室ID")
    private Integer appointDepartId;

    @ItemProperty(alias = "挂号科室对应的行政科室ID")
    private Integer appointDepartInDepartId;

    @ItemProperty(alias = "处方业务类型  1 门诊处方  2  复诊处方  3 其他处方")
    private Integer recipeBusinessType;

    @ItemProperty(alias = "就诊人卡号")
    private String cardId;

    @ItemProperty(alias = "就诊人卡类型")
    private String cardType;

    @ItemProperty(alias = "挂号序号")
    private String registerNo;

    @ItemProperty(alias = "门诊医生科室")
    private Integer consultDepart;

    @ItemProperty(alias = "门诊医生科室名称")
    private String consultDepartText;

    @ItemProperty(alias = "门诊方式")
    private Integer requestMode;

    @ItemProperty(alias = "默认200成功，返回609为未建档")
    private Integer msgCode;

    @ItemProperty(alias = "错误原因（返回错误时必填）")
    private String msg;
}
