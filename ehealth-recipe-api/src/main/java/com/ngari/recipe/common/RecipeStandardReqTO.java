package com.ngari.recipe.common;

import java.io.Serializable;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 处方标准请求对象
 * @version： 1.0
 */
public class RecipeStandardReqTO implements Serializable{

    private static final long serialVersionUID = -8096243539536077281L;

    private Map<String, Object> conditions;

    public Map<String, Object> getConditions() {
        return conditions;
    }

    public void setConditions(Map<String, Object> conditions) {
        this.conditions = conditions;
    }

    public boolean isNotEmpty(){
      return null != this.getConditions() && !this.getConditions().isEmpty();
    }
}
