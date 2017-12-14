package com.ngari.recipe.common;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/11.
 */
public class RecipeCommonResTO extends RecipeCommonBaseTO{

    private Integer code;

    private String msg;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
