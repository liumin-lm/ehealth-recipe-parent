package recipe.enumerate.type;

/**
 * 运费获取方式
 * @author yins
 */
public enum ExpressFeeTypeEnum {
    EXPRESS_FEE_ONLINE(0, "运费从平台获取"),
    EXPRESS_FEE_OFFLINE(1, "运费从线下获取");
    private Integer type;
    private String name;

    ExpressFeeTypeEnum (Integer type, String name) {
        this.type = type;
        this.name = name;
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
}
