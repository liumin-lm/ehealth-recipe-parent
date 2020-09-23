package com.ngari.recipe.sign;

import com.ngari.recipe.ca.CaSignResultUpgradeBean;
import ctd.util.annotation.RpcService;

public interface ISignRecipeInfoService {
    @RpcService
    void setMedicalSignInfoByRecipeId(Integer recipeId);

    @RpcService
    void saveCaSignResult(CaSignResultUpgradeBean caSignResultBean);
}
