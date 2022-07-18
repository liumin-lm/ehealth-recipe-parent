package recipe.enumerate.status;

/**
 * 写入his状态枚举
 *
 * @author fuzi
 */

public enum WriteHisEnum {
    NONE(0, "默认", "默认(未写入)"),
    WRITE_HIS_STATE_SUBMIT(1, "写入中", ""),
    WRITE_HIS_STATE_AUDIT(2, "写入失败", ""),
    WRITE_HIS_STATE_ORDER(3, "写入成功", ""),
    ;

    private Integer type;
    private String name;
    private String desc;

    WriteHisEnum(Integer type, String name, String desc) {
        this.type = type;
        this.name = name;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
