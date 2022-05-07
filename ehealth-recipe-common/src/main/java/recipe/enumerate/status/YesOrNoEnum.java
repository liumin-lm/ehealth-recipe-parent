package recipe.enumerate.status;

/**
 * @description： 是否枚举
 * @author： whf
 * @date： 2022-05-07 16:34
 */
public enum YesOrNoEnum {
    NO(0),
    YES(1);
    private Integer type;

    YesOrNoEnum(Integer type) {
        this.type = type;
    }

    public Integer getType() {
        return type;
    }
}
