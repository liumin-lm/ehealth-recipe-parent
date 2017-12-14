package recipe.prescription.bean;

/**
 * 诊断信息
 * Created by jiangtingfeng on 2017/11/2.
 */
public class AuditDiagnose {
    // 诊断类型
    private String type;

    // 诊断名称
    private String name;

    // 诊断代码
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
