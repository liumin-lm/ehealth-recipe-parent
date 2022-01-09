package recipe.enumerate.type;

/**
 * 取药方式
 *
 * @author yinsheng
 */
public enum TakeMedicineWayEnum {

    DEFAULT_WAY(0, "默认"),
    TAKE_MEDICINE_STORE(1, "药店取药"),
    TAKE_MEDICINE_STATION(2, "站点取药");

    TakeMedicineWayEnum(Integer type, String name){
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
