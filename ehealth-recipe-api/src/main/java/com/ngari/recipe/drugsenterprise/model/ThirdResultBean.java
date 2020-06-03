package com.ngari.recipe.drugsenterprise.model;

import com.ngari.recipe.common.RecipeResultBean;

public class ThirdResultBean extends RecipeResultBean {

    public static final Integer SUCCESS = 1;

    public static final Integer FAIL = 0;

    private Integer code;

    private String msg;

    private Integer busId;

    private String error;

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public Integer getBusId() {
        return busId;
    }

    @Override
    public void setBusId(Integer busId) {
        this.busId = busId;
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public void setError(String error) {
        this.error = error;
    }
}