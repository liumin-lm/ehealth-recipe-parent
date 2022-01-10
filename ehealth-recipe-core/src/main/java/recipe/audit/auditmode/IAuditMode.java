package recipe.audit.auditmode;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;

import java.util.Map;

/**
 * created by shiyuping on 2019/8/15
 * 审方方式接口 审方前置 审方后置 不需要审方
 */
public interface IAuditMode {
    /**
     * his回调后的变更
     */
    void afterHisCallBackChange(Integer status,Recipe recipe,String memo);

    /**
     * 审核处方后的状态变更
     */
    int afterAuditRecipeChange();

    /**
     * 审核通过后逻辑处理
     * @param recipe
     */
    void afterCheckPassYs(Recipe recipe);

    /**
     * 审核不通过后逻辑处理
     *
     * @param recipe
     */
    void afterCheckNotPassYs(Recipe recipe);

    /**
     * 支付后的变更
     */
    void afterPayChange(Boolean saveFlag, Recipe recipe, RecipeResultBean result, Map<String, Object> attrMap);
}
