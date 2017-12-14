package com.ngari.recipe.common;

import ctd.util.JSONUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/6.
 */
public class RecipeBussReqTO extends RecipeCommonReqTO implements Serializable {

    private static final long serialVersionUID = -7643738136821741809L;

    private Object data;


    public RecipeBussReqTO() {
    }

    public RecipeBussReqTO(Object data, Map<String, Object> conditions) {
        this.data = data;
        setConditions(conditions);
    }


    public static RecipeBussReqTO getNewRequest(Object data) {
        return new RecipeBussReqTO(data, null);
    }

    public static RecipeBussReqTO getNewRequest(Map<String, Object> conditions) {
        return new RecipeBussReqTO(null, conditions);
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return JSONUtils.toString(this);
    }
}
