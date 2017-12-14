package recipe.prescription.bean;

/**
 * 过敏源信息
 * Created by jiangtingfeng on 2017/11/2.
 */
public class AuditAllergy {
    // 过敏类型
    private String type;

    // 过敏源名称
    private String name;

    // 过敏源代码
    private String code;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
