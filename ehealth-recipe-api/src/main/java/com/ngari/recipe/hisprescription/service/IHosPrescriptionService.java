package com.ngari.recipe.hisprescription.service;

import com.ngari.recipe.hisprescription.model.HosRecipeResult;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
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
}
