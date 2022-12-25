package recipe.enumerate.type;

/**
 * 库存校验来源类型
 */
public enum StockCheckSourceTypeEnum {

    DOCTOR_STOCK(1, "医生端"),
    PATIENT_STOCK(2, "患者端"),
    GREENROOM_STOCK(3, "运营平台");
    private Integer type;
    private String name;

    StockCheckSourceTypeEnum(Integer type, String name){
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
