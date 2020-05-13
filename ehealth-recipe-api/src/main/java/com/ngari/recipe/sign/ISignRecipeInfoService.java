package com.ngari.recipe.sign;

import ctd.util.annotation.RpcService;

public interface ISignRecipeInfoService {
    @RpcService
    void setMedicalSignInfoByRecipeId(Integer recipeId);
}
