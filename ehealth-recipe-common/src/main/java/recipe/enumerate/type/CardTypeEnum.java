package recipe.enumerate.type;

/**
 * 卡类型
 * @author yinsheng
 * @date 2022\1\24 0029 19:29
 */
public enum CardTypeEnum {
    HOSPITAL_CARD("1", "医院就诊卡"),
    MEDICARE_CARD("2", "医保卡"),
    MEDICAL_RECORD_CARD("3", "病例号"),
    HEALTH_CARD("4", "健康卡"),
    SOCIAL_SECURITY_CARD("5", "社保卡");
    CardTypeEnum(String type, String name){
        this.type = type;
        this.name = name;
    }
    private String type;
    private String name;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
