package recipe.constant;

/**
 * @author： 0184/yu_yun
 * @date： 2018/7/27
 * @description： MQ消息类型
 * @version： 1.0
 */
public enum MsgTypeEnum {

    DELETE_PATIENT("90", "删除就诊人", "删除就诊人");

    private String id;
    private String name;
    private String desc;

    private MsgTypeEnum(String id, String name, String desc) {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
