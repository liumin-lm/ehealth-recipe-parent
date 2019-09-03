package recipe.audit.auditmode;

/**
 * created by shiyuping on 2019/8/16
 * 审方模式枚举
 */
public enum AuditModeEnum {
    AUDITPOST(2, "auditPost"),
    AUDITPRE(1, "auditPre"),
    NOAUDIT(0, "noAuditMode");


    private Integer auditMode;
    private String serviceName;

    AuditModeEnum(Integer auditMode, String serviceName) {
        this.auditMode = auditMode;
        this.serviceName = serviceName;
    }

    public Integer getAuditMode() {
        return auditMode;
    }

    public String getServiceName() {
        return serviceName;
    }
}
