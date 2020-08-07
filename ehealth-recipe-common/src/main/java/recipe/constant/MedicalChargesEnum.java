package recipe.constant;

/**
 * 医疗费用meij
 *
 * @author fuzi
 */
public enum MedicalChargesEnum {
    REGISTRATION(10, "挂号费"),
    DISTRIBUTION(11, "配送费"),
    ;

    /**
     * 费用编码
     **/
    private Integer code;
    /**
     * 费用名称
     **/
    private String name;


    MedicalChargesEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
