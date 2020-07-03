package com.ngari.recipe.drugsenterprise.service;

import com.ngari.recipe.IBaseService;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.recipe.model.RecipeBean;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/11.
 */
public interface IDrugsEnterpriseService extends IBaseService<DrugsEnterpriseBean> {
    void pushRecipeInfoForThird(RecipeBean recipe,DrugsEnterpriseBean drugsEnterprise);

}
