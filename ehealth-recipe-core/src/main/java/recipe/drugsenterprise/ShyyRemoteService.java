package recipe.drugsenterprise;

import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.entity.SaleDrugList;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.SaleDrugListDAO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 上海益友药企
 * @author yinsheng
 * @date 2020\3\4 0004 11:24
 */
public class ShyyRemoteService  extends AccessDrugEnterpriseService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ShyyRemoteService.class);

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {

    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("ShyyRemoteService.pushRecipeInfo recipeIds:{}.", JSONUtils.toString(recipeIds));
        //上海益友此处进行减库存的操作
        updateEnterpriseInventory(recipeIds.get(0), enterprise);
        return DrugEnterpriseResult.getSuccess();
    }

    private void updateEnterpriseInventory(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        for (Recipedetail recipedetail : recipedetails) {
            Integer drugId = recipedetail.getDrugId();
            Double useTotalDose = recipedetail.getUseTotalDose();
            BigDecimal totalDose = new BigDecimal(useTotalDose);
            LOGGER.info("ShyyRemoteService.updateEnterpriseInventory 更新库存成功,更新药品:{},更新数量:{},处方单号：{}.", drugId, totalDose, recipeId);
            saleDrugListDAO.updateInventoryByOrganIdAndDrugId(drugsEnterprise.getId(), drugId, totalDose);
        }
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, drugsEnterprise.getId());
        return saleDrugList.getInventory()+"";
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("ShyyRemoteService.scanStock recipeId:{}.", recipeId);
        DrugEnterpriseResult drugEnterpriseResult = DrugEnterpriseResult.getSuccess();
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        for (Recipedetail recipedetail : recipedetails) {
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), drugsEnterprise.getId());
            if (saleDrugList.getInventory().doubleValue() < recipedetail.getUseTotalDose()) {
                //说明开药比较多
                drugEnterpriseResult.setCode(DrugEnterpriseResult.FAIL);
                return drugEnterpriseResult;
            }
        }
        return drugEnterpriseResult;
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_SHYY;
    }
}
