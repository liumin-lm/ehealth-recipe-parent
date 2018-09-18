package com.ngari.recipe.common.thirdpart;

import com.ngari.recipe.common.anno.Verify;

import java.io.Serializable;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 处方标准推送请求
 * @version： 1.0
 */
public class RecipePushReqTO implements Serializable {

    @Verify(desc = "患者证件类型", isInt = true)
    private String certificateType;

    @Verify(desc = "患者证件号")
    private String certificate;

    @Verify(desc = "患者姓名")
    private String patientName;

    private String patientTel;

    private String patientNumber;

    private String recipeCode;

    private String clinicOrgan;

    private String recipeType;



}
