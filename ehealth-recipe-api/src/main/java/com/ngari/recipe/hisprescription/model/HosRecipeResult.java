package com.ngari.recipe.hisprescription.model;

import ctd.schema.annotation.Schema;
import ctd.util.JSONUtils;

import java.io.Serializable;

/**
 * 对接医院HIS结果对象
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2017/4/18.
 */
@Schema
public class HosRecipeResult implements Serializable {

    private static final long serialVersionUID = 4055757161601929525L;

    public final static String SUCCESS = "000";

    public final static String FAIL = "001";

    private String code;

    private String msg;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return JSONUtils.toString(this);
    }
}
