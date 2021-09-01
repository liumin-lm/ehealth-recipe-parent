package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class RecipeTherapyDTO implements Serializable {
    @ItemProperty(alias = "诊疗id")
    private Integer id;
    @ItemProperty(alias = "处方id")
    private Integer recipeId;
    @ItemProperty(alias = "医生id")
    private Integer doctorId;
    @ItemProperty(alias = "患者id")
    private String mpiId;
    @ItemProperty(alias = "医院诊疗提示")
    private String therapyNotice;
    @ItemProperty(alias = "医院诊疗时间")
    private String therapyTime;
    @ItemProperty(alias = "诊疗执行科室")
    private String therapyExecuteDepart;
    @ItemProperty(alias = "诊疗缴费时间")
    private Date therapyPayTime;
    @ItemProperty(alias = "诊疗作废类型，1:医生撤销，2:HIS作废，3:系统取消 4:医生作废")
    private String therapyCancellationType;
    @ItemProperty(alias = "诊疗作废信息")
    private String therapyCancellation;
    @ItemProperty(alias = "诊疗状态: 1：待提交，2:待缴费，3:已交费，4：已作废")
    private Integer status;

}