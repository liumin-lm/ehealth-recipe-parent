package recipe.enumerate.type;

/**
 * @description： 库存校验枚举
 * @author： whf
 * @date： 2021-07-29 10:20
 */
public enum DrugStockCheckEnum {
    /**
     * 不校验库存
     */
    NO_CHECK_STOCK(0, "不校验库存"),
    /**
     * 仅校验医院库存
     */
    HOS_CHECK_STOCK(1, "仅校验医院库存"),
    /**
     * 仅校验药企库存
     */
    ENT_CHECK_STOCK(2, "仅校验药企库存"),
    /**
     * 医院药企库存都校验
     */
    ALL_CHECK_STOCK(3, "医院药企库存都校验"),
    ;

    private Integer type;
    private String name;

    DrugStockCheckEnum(Integer type, String name) {
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
