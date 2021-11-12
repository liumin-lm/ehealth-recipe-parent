package recipe.enumerate.type;

/**
 * 配送费支付方式
 * @author yinsheng
 * @date 2021\10\27 0007 13:58
 */
public enum ExpressFeePayWayEnum {
    ONLINE(1, "在线支付"),
    OFFLINE(2, "线下支付"),
    THIRDLINE(3, "第三方支付 "),
    SHOWFREEPIC(4, "运费不取设置的运费仅展示图片");

    private Integer type;
    private String name;

    ExpressFeePayWayEnum(Integer type, String name){
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
