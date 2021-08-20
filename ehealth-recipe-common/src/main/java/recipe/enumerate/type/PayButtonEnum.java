package recipe.enumerate.type;

/**
 * @description： 支付按钮类型
 * @author： whf
 * @date： 2021-08-19 15:39
 */
public enum PayButtonEnum {

    /**
     *  医保支付
     */
    MEDICAL_PAY(1,"medicalPay"),
    /**
     * 自费支付
     */
    MY_PAY(2,"myPay");

    private Integer type;
    private String text;

    PayButtonEnum(Integer type, String text) {
        this.type = type;
        this.text = text;
    }

    public Integer getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public static String getRecipeType(Integer type){
        for (PayButtonEnum value : PayButtonEnum.values()) {
            if (value.getType().equals(type)){
                return value.getText();
            }
        }
        return null;
    }
}
