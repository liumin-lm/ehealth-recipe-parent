package com.ngari.recipe.recipe.model;

import java.util.List;

/**
 * @author zhongzx
 *
 */
public class HisSendResTO implements java.io.Serializable {

    private static final long serialVersionUID = -4584755552379166981L;

    /**
     * 处方ID
     */
    private String recipeId;

    /**
     * 交易成功标志 0-成功 1-失败
     */
    private Integer msgCode;

    /**
     * 返回信息 如果交易异常则返回异常信息
     */
    private String msg;

    /**
     * 返回医嘱列表数据
     */
    private List<OrderRepTO> data;

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public Integer getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(Integer msgCode) {
        this.msgCode = msgCode;
    }

    public List<OrderRepTO> getData() {
        return data;
    }

    public void setData(List<OrderRepTO> data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
