package com.ngari.recipe.common;

import java.util.Map;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/11.
 */
public class RecipeCommonReqTO extends RecipeCommonBaseTO{

    public Map<String, Object> conditions;

    public Map<String, Object> getConditions() {
        return conditions;
    }

    public void setConditions(Map<String, Object> conditions) {
        this.conditions = conditions;
    }
}
