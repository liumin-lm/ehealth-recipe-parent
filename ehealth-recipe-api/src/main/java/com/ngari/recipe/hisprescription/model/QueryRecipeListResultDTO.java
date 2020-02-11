package com.ngari.recipe.hisprescription.model;

import java.util.List;

/**
 * created by shiyuping on 2020/2/10
 */
public class QueryRecipeListResultDTO implements java.io.Serializable{
    private static final long serialVersionUID = 3514018925816075858L;
    //交易成功标志 0-成功 -1-失败
    private Integer msgCode;

    //返回信息 如果交易异常则返回异常信息
    private String msg;

    private List<QueryRecipeInfoDTO> data;

    public Integer getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(Integer msgCode) {
        this.msgCode = msgCode;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<QueryRecipeInfoDTO> getData() {
        return data;
    }

    public void setData(List<QueryRecipeInfoDTO> data) {
        this.data = data;
    }
}
