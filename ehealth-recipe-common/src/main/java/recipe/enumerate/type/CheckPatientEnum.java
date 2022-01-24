package recipe.enumerate.type;

/**
 * @author yinsheng
 * @date 2021\7\29 0029 19:29
 */
public enum CheckPatientEnum {
    CHECK_PATIENT_NORMAL(1, "正常"),
    CHECK_PATIENT_PATIENT(2, "患者无效"),
    CHECK_PATIENT_NOAUTH(3, "患者未实名认证"),
    CHECK_PATIENT_CARDDEL(4, "卡删除");

    CheckPatientEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }
    private Integer type;
    private String name;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getDesc() {
        return name;
    }

    public void setDesc(String desc) {
        this.name = desc;
    }
}
