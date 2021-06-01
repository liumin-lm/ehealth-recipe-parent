package com.ngari.recipe.common;

import com.ngari.recipe.drugsenterprise.model.DepStyleBean;

import java.io.Serializable;
import java.util.Map;

/**
 * 一般业务返回结果bean
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2017/2/15.
 */
public class RecipeResultBean implements Serializable {

    public static final Integer SUCCESS = 1;

    public static final Integer FAIL = 0;

    public static final Integer NO_ADDRESS = -1;

    public static final Integer CHECKFAIL = 5;

    public static final Integer PUSHSUCCESS = 6;

    private static final long serialVersionUID = 8407331602023924130L;

    private Integer code;

    private String msg;

    private Integer busId;

    private String error;

    private Object object;

    private String extendValue;

    private Map<String, Object> ext;

    private DepStyleBean style;

    public RecipeResultBean() {
    }

    public RecipeResultBean(Integer code) {
        this.code = code;
    }

    public static RecipeResultBean getSuccess() {
        return new RecipeResultBean(SUCCESS);
    }

    public static RecipeResultBean getFail() {
        return new RecipeResultBean(FAIL);
    }

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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Integer getBusId() {
        return busId;
    }

    public void setBusId(Integer busId) {
        this.busId = busId;
    }

    public String getExtendValue() {
        return extendValue;
    }

    public void setExtendValue(String extendValue) {
        this.extendValue = extendValue;
    }

    public Map<String, Object> getExt() {
        return ext;
    }

    public void setExt(Map<String, Object> ext) {
        this.ext = ext;
    }

    public DepStyleBean getStyle() {
        return style;
    }

    public void setStyle(DepStyleBean style) {
        this.style = style;
    }
}
