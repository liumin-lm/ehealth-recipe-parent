package com.ngari.recipe.common;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 处方标准请求对象
 * @version： 1.0
 */
public class RecipeStandardReqTO extends RecipeCommonReqTO {

    public boolean isNotEmpty(){
      return null != this.getConditions() && !this.getConditions().isEmpty();
    }
}
