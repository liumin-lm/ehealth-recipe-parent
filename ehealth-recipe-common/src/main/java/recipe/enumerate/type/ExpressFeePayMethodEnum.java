package recipe.enumerate.type;

/**
 * 配送费付款方式
 * @author 刘敏
 * @date 2022\8\19
 */
public enum ExpressFeePayMethodEnum {
    ConsignONLINE(1, "寄付"),
    CASHONDELIVERYOFFLINE(2, "货到付款"),
    THIRDLINE(3, "寄付转第三方 "),
    SHOWFREEPIC(100, "全部");

    private Integer type;
    private String name;

    ExpressFeePayMethodEnum(Integer type, String name){
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
