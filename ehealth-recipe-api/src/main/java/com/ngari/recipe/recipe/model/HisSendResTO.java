package com.ngari.recipe.recipe.model;

import com.ngari.his.recipe.mode.RecipePreSettleDrugFeeDTO;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import java.math.BigDecimal;
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

    @ItemProperty(
            alias = "中药处方辩证论证费支付状态 0:待支付1:已支付"
    )
    private Integer visitPayFlag;
    @ItemProperty(
            alias = "中药处方辩证论证费"
    )
    private BigDecimal visitMoney;

    @ItemProperty(
            alias = "中药处方辩证论证费id"
    )
    private Integer visitId;

    @ItemProperty(alias = "处方总金额")
    private BigDecimal recipeFee;

    @ItemProperty(alias = "处方药品价格明细")
    private List<RecipePreSettleDrugFeeDTO> recipePreSettleDrugFeeDTOS;

    @ItemProperty(alias = "0：默认(未写入)，1：写入中，2：写入失败，3：写入成功")
    private Integer writeHisState;

    @ItemProperty(alias = "his订单编号(浙江医保小程序)")
    private String ybid;

    @ItemProperty(alias = "用于支付结算结果查询")
    private String hisBusId;

    public String getHisBusId() {
        return hisBusId;
    }

    public void setHisBusId(String hisBusId) {
        this.hisBusId = hisBusId;
    }

    public String getYbid() {
        return ybid;
    }

    public void setYbid(String ybid) {
        this.ybid = ybid;
    }

    public Integer getWriteHisState() {
        return writeHisState;
    }

    public void setWriteHisState(Integer writeHisState) {
        this.writeHisState = writeHisState;
    }

    public List<RecipePreSettleDrugFeeDTO> getRecipePreSettleDrugFeeDTOS() {
        return recipePreSettleDrugFeeDTOS;
    }

    public void setRecipePreSettleDrugFeeDTOS(List<RecipePreSettleDrugFeeDTO> recipePreSettleDrugFeeDTOS) {
        this.recipePreSettleDrugFeeDTOS = recipePreSettleDrugFeeDTOS;
    }

    public BigDecimal getRecipeFee() {
        return recipeFee;
    }

    public void setRecipeFee(BigDecimal recipeFee) {
        this.recipeFee = recipeFee;
    }

    public Integer getVisitPayFlag() {
        return visitPayFlag;
    }

    public void setVisitPayFlag(Integer visitPayFlag) {
        this.visitPayFlag = visitPayFlag;
    }

    public BigDecimal getVisitMoney() {
        return visitMoney;
    }

    public void setVisitMoney(BigDecimal visitMoney) {
        this.visitMoney = visitMoney;
    }

    public Integer getVisitId() {
        return visitId;
    }

    public void setVisitId(Integer visitId) {
        this.visitId = visitId;
    }

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
