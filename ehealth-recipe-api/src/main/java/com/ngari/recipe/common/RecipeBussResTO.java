package com.ngari.recipe.common;

import java.io.Serializable;

/**
 * @param <T>
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/6.
 */
public class RecipeBussResTO<T> extends RecipeCommonResTO implements Serializable {

    private static final long serialVersionUID = -5327145704045983963L;

    private T data;

    private T user;

    public RecipeBussResTO() {
    }

    public RecipeBussResTO(T data) {
        this.data = data;
        setCode(RecipeCommonBaseTO.SUCCESS);
    }

    public static RecipeBussResTO getSuccessResponse(Object data) {
        return new RecipeBussResTO(data);
    }

    public static RecipeBussResTO getFailResponse(String msg) {
        RecipeBussResTO resTo = new RecipeBussResTO();
        resTo.setCode(RecipeCommonBaseTO.FAIL);
        resTo.setMsg(msg);
        return resTo;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public T getUser() {
        return user;
    }

    public void setUser(T user) {
        this.user = user;
    }
}
