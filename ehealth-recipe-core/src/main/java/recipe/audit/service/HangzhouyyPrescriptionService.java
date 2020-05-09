package recipe.audit.service;

import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.audit.bean.AutoAuditResult;

import java.util.List;

/**
 * 杭州逸曜合理用药
 */

@RpcBean
public class HangzhouyyPrescriptionService implements IntellectJudicialService {

    @Override
    @RpcService
    public AutoAuditResult analysis(RecipeBean recipe, List<RecipeDetailBean> recipedetails) {
        return null;
    }

    @Override
    public String getDrugSpecification(Integer drugId) {
        return null;
    }
}
