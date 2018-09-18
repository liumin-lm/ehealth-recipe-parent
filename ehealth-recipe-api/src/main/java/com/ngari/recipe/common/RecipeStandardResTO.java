package com.ngari.recipe.common;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 处方标准返回对象
 * @version： 1.0
 */
public class RecipeStandardResTO<T> extends RecipeCommonResTO {

    private T data;

    public static <T> RecipeStandardResTO getRequest(Class<T> clazz){
        return new RecipeStandardResTO<T>();
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
