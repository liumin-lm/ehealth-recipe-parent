package com.ngari.recipe.common;

import ctd.util.JSONUtils;

import java.io.Serializable;
import java.util.List;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/6.
 * @param <T>
 */
@SuppressWarnings("unchecked")
public class RecipeListResTO<T> extends RecipeCommonResTO implements Serializable {

    private static final long serialVersionUID = 4526538348861186263L;

    private List<T> data;

    public RecipeListResTO() {
    }

    public RecipeListResTO(List<T> data) {
        this.data = data;
        setCode(RecipeCommonBaseTO.SUCCESS);
    }

    public static RecipeListResTO getSuccessResponse(List data) {
        return new RecipeListResTO(data);
    }

    public static RecipeListResTO getFailResponse(String msg) {
        RecipeListResTO resTo = new RecipeListResTO();
        resTo.setCode(RecipeCommonBaseTO.FAIL);
        resTo.setMsg(msg);
        return resTo;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return JSONUtils.toString(this);
    }
}
