package com.ngari.recipe.hisprescription.service;

import com.ngari.recipe.hisprescription.model.*;
import ctd.util.annotation.RpcService;

/**
 * @author： 0184/yu_yun
 * @date： 2018/7/25
 * @description： 接收医院处方（存入 cdr_recipe）
 * @version： 1.0
 */
public interface IHosPrescriptionService {

    @RpcService
    HosRecipeResult createPrescription(HospitalRecipeDTO hospitalRecipeDTO);

    @RpcService
    HosRecipeResult createTransferPrescription(HospitalRecipeDTO hospitalRecipeDTO);

    /**
     * 接收his推送的处方 并推送用药指导模板消息
     * @param hosPatientRecipeDTO
     * @return
     */
    @RpcService
    HosRecipeResult sendMedicationGuideData(HosPatientRecipeDTO hosPatientRecipeDTO);

    /**
     * 用药指导
     * 前置机获取二维码信息
     * @param recipeQrCodeReqDTO
     * @return
     */
    @RpcService
    HosRecipeResult getQrUrlForRecipeRemind(RecipeQrCodeReqDTO recipeQrCodeReqDTO);

}
