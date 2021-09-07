package recipe.enumerate.type;

/**
 * 诊疗作废类型
 * @author yinsheng
 * @date 2021\9\7 0007 13:42
 */
public enum TherapyCancellationTypeEnum {

    DOCTOR_CANCEL(1, "医生撤销"),
    HIS_ABOLISH(2, "HIS作废"),
    SYSTEM_CANCEL(3, "系统取消"),
    DOCTOR_ABOLISH(4, "医生作废");

    private Integer type;
    private String name;

    TherapyCancellationTypeEnum(Integer type, String name){
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
