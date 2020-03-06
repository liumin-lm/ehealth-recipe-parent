package com.ngari.recipe.recipe.constant;

/**
* @Description: RecipePayTextEnum 类（或接口）是 处方支付状态文案
* @Author: JRK
* @Date: 2020/3/5
*/
public enum RecipePayTextEnum {


    /**
     * 未支付
     */
    NO_PAY("未支付", 0),
    /**
     * 已支付
     */
    ALRAEDY_PAY("已支付", 1),
    /**
     * 退款中
     */
    REFUND("退款中", 2),
    /**
     * 退款成功
     */
    REFUND_SUCCESS("退款成功", 3),
    /**
     * 支付失败
     */
    FAIL_PAY("支付失败", 4),
    /**
     * 默认值
     */
    Default("未支付", 0);
    /**
     * 处方支付文案
     */
    private String payText;
    /**
     * 支付方式
     */
    private Integer payFlag;


    RecipePayTextEnum(String payText, Integer payFlag) {
        this.payText = payText;
        this.payFlag = payFlag;
    }

    public static RecipePayTextEnum getByPayFlag(Integer payFlag){
        for(RecipePayTextEnum ep : RecipePayTextEnum.values()){
            if(ep.getPayFlag().equals(payFlag)){
                return ep;
            }
        }
        return Default;
    }

    public Integer getPayFlag() {
        return payFlag;
    }

    public void setPayFlag(Integer payFlag) {
        this.payFlag = payFlag;
    }

    public String getPayText() {
        return payText;
    }

    public void setPayText(String payText) {
        this.payText = payText;
    }
}