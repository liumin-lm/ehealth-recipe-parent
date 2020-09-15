package recipe.drugsenterprise;

import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.drugsenterprise.model.Position;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.SaleDrugList;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.SaleDrugListDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return "20.0";
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        List<String> result = new ArrayList<>();
        for (RecipeDetailBean recipeDetailBean : drugsDataBean.getRecipeDetailBeans()) {
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganIdAndStatus(recipeDetailBean.getDrugId(), drugsEnterprise.getId());
            if (saleDrugList != null) {
                result.add(recipeDetailBean.getDrugName());
            }
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("TestRemoteService pushRecipeInfo not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("TestRemoteService scanStock not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        LOGGER.info("TestRemoteService syncEnterpriseDrug not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        LOGGER.info("TestRemoteService pushCheckResult not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        LOGGER.info("TestRemoteService findSupportDep not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_TEST;
    }
}
