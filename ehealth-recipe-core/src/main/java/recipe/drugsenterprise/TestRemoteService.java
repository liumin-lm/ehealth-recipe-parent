package recipe.drugsenterprise;

import com.ngari.recipe.entity.DrugsEnterprise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;

import java.util.List;

/**
 * 测试药企，测试某些场景下药企不支持的功能，如 中药库存校验等
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/6/22.
 */
public class TestRemoteService extends AccessDrugEnterpriseService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TestRemoteService.class);

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("TestRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("TestRemoteService pushRecipeInfo not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("TestRemoteService pushRecipeInfo not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        LOGGER.info("TestRemoteService pushRecipeInfo not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        LOGGER.info("TestRemoteService pushRecipeInfo not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("TestRemoteService findSupportDep not implement.");
        return null;
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_TEST;
    }
}
