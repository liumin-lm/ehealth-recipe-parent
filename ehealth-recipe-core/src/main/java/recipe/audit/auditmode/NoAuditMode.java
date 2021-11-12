package recipe.audit.auditmode;

import com.ngari.recipe.entity.Recipe;
import recipe.constant.ReviewTypeConstant;

/**
 * created by shiyuping on 2019/8/15
 * 不需要审方
 */
@AuditMode(ReviewTypeConstant.Not_AuditMode)
public class NoAuditMode extends AbstractAuidtMode {

    @Override
    public Boolean notifyPharAudit(Recipe recipe) {
        return null;
    }
}
