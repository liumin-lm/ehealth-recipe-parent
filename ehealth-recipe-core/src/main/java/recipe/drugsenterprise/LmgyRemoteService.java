package recipe.drugsenterprise;

import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;

import java.util.List;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2020\5\8 0008 15:08
 */
public class LmgyRemoteService extends AccessDrugEnterpriseService {
    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {

    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return null;
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        return null;
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return null;
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_LMGY;
    }
}
