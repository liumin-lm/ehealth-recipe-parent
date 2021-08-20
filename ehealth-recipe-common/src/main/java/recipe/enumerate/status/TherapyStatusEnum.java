package recipe.enumerate.status;

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
    private Integer type;
    private String name;

    TherapyStatusEnum(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }

}
