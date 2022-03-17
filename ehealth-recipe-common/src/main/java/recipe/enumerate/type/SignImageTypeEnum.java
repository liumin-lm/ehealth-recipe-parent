package recipe.enumerate.type;

/**
 * 签名图片类型
 */
public enum SignImageTypeEnum {
    SIGN_IMAGE_TYPE_DOCTOR(0, "医生签名图片"),
    SIGN_IMAGE_TYPE_CHEMIST(1, "药师签名图片");

    private Integer type;
    private String name;

    SignImageTypeEnum(Integer type, String name){
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
