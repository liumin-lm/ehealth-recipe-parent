package recipe.givemode.business;

/**
 * @author yinsheng
 * @date 2021\5\25 0025 13:59
 */
public enum GiveModeTypeEnum {

    COMMON_GIVEMODE(0, "commonGiveModeService", "通用标准获取购药方式"),
    BEIJING_GIVEMODE(1, "bjGiveModeService", "北京互联网模式"),
    HANGZHOU_GIVEMODE(2, "fromHisGiveModeService", "浙江省互联网模式");


    private Integer type;
    private String name;
    private String desc;

    GiveModeTypeEnum(Integer type, String name, String desc) {
        this.type = type;
        this.name = name;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
