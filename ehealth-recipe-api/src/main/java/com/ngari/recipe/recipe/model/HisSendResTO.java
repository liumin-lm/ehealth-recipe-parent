package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Schema;

import java.util.List;

/**
 * @author zhongzx
 *
 */
@Schema
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
     * 患者类型（1自费 2医保）
     */
    private String patientType;

    /**
     * 返回医嘱列表数据
     */
    private List<OrderRepTO> data;

    /**
     * 处方收费序号合集（多个用逗号拼接）
     */
    private String recipeCostNumber;

    public String getRecipeCostNumber() {
        return recipeCostNumber;
    }

    public void setRecipeCostNumber(String recipeCostNumber) {
        this.recipeCostNumber = recipeCostNumber;
    }

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

    public String getPatientType() {
        return patientType;
    }

    public void setPatientType(String patientType) {
        this.patientType = patientType;
    }
}
