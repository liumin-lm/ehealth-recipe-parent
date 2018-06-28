package com.ngari.recipe.hisprescription.service;

import com.ngari.recipe.IBaseService;
import com.ngari.recipe.common.RecipeCommonResTO;
import com.ngari.recipe.hisprescription.model.HisprescriptionTO;
import ctd.util.annotation.RpcService;

/**
 * @author： 0184/yu_yun
 * @date： 2018/6/28
 * @description： 医院处方相关接口
 * @version： 1.0
 */
public interface IHisprescriptionService extends IBaseService<HisprescriptionTO> {

    @RpcService
    RecipeCommonResTO createPrescription(HisprescriptionTO hisprescription);
}
