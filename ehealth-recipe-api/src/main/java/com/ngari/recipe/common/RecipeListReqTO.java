package com.ngari.recipe.common;

import ctd.util.JSONUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * 列表查询请求
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/6.
 */
public class RecipeListReqTO extends RecipeCommonReqTO implements Serializable {

    private static final long serialVersionUID = -3284816229796221254L;

    private int start;

    private int limit;

    public RecipeListReqTO() {
    }

    public RecipeListReqTO(int start, int limit, Map<String, Object> conditions) {
        setConditions(conditions);
        this.start = start;
        this.limit = limit;
    }

    public static RecipeListReqTO getNewRequest(int start, int limit, Map<String, Object> conditions) {
        return new RecipeListReqTO(start, limit, conditions);
    }

    public static RecipeListReqTO getNewRequest(Map<String, Object> conditions) {
        return new RecipeListReqTO(0, 0, conditions);
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public String toString() {
        return JSONUtils.toString(this);
    }
}
