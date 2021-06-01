package com.ngari.recipe.sign;

import com.ngari.recipe.ca.CaSignResultUpgradeBean;
import com.ngari.recipe.sign.model.SignDoctorRecipeInfoDTO;
import ctd.util.annotation.RpcService;

public interface ISignRecipeInfoService {
    @RpcService
    void setMedicalSignInfoByRecipeId(Integer recipeId);

    @RpcService
    void saveCaSignResult(CaSignResultUpgradeBean caSignResultBean);

    @RpcService
    SignDoctorRecipeInfoDTO getSignRecipeInfoByRecipeIdAndServerType(Integer recipeId, Integer serverType);
}
