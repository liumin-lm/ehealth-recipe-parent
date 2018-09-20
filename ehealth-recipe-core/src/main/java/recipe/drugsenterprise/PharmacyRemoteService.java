package recipe.drugsenterprise;

import com.ngari.recipe.entity.DrugsEnterprise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;

import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/20
 * @description： TODO
 * @version： 1.0
 */
public class PharmacyRemoteService extends AccessDrugEnterpriseService{

    /** logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(PharmacyRemoteService.class);

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("PharmacyRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("PharmacyRemoteService pushRecipeInfo not implement.");
        return null;
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("PharmacyRemoteService scanStock not implement.");
        return null;
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        LOGGER.info("PharmacyRemoteService syncEnterpriseDrug not implement.");
        return null;
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        LOGGER.info("PharmacyRemoteService pushCheckResult not implement.");
        return null;
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("PharmacyRemoteService findSupportDep not implement.");


        return null;
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_PHARMACY;
    }
}
