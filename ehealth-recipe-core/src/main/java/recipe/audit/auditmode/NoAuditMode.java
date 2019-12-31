package recipe.audit.auditmode;

import recipe.constant.ReviewTypeConstant;

/**
 * created by shiyuping on 2019/8/15
 * 不需要审方
 */
@AuditMode(ReviewTypeConstant.Not_AuditMode)
public class NoAuditMode extends AbstractAuidtMode {

}
