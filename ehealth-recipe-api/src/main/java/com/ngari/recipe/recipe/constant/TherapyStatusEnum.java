package com.ngari.recipe.recipe.constant;

/**
 * 诊疗处方状态
 * @author yinsheng
 * @date 2021\8\20 0020 09:30
 */
public enum TherapyStatusEnum {

    READYSUBMIT(1, "待提交"),
    READYPAY(2, "待缴费"),
    HADEPAY(3, "已缴费"),
    HADECANCEL(4, "已作废");

    private Integer status;
    private String desc;

    TherapyStatusEnum(Integer status, String desc){
        this.status = status;
        this.desc = desc;
    }

    public String getDescByStatus(Integer status){
        TherapyStatusEnum[] therapyStatusEnums = TherapyStatusEnum.values();
        for (TherapyStatusEnum value : therapyStatusEnums) {
            if (value.getStatus().equals(status)) {
                return value.getDesc();
            }
        }
        return "";
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
