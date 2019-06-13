package com.ngari.recipe.hisprescription.model;

/**
 * 浙江互联网医院查询处方接口返回dto
 * created by shiyuping on 2018/11/30
 */
public class QueryRecipeResultDTO implements java.io.Serializable{

    private static final long serialVersionUID = -2350679729758975781L;
    //交易成功标志 0-成功 -1-失败
    private Integer msgCode;

    //返回信息 如果交易异常则返回异常信息
    private String msg;

    private QueryRecipeInfoDTO data;

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

    public QueryRecipeInfoDTO getData() {
        return data;
    }

    public void setData(QueryRecipeInfoDTO data) {
        this.data = data;
    }
}
