package recipe.drugsenterprise;

import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2019\6\6 0006 14:16
 */
public class HddyfRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HddyfRemoteService.class);

    @Resource
    private RecipeDAO recipeDAO;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("HddyfRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        return DrugEnterpriseResult.getSuccess();
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
        return DrugEnterpriseConstant.COMPANY_HDDYF;
    }

    public DrugEnterpriseResult findChemistList(List<Integer> recipeIds, Integer range, String longitude, String latitude , DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (ObjectUtils.isEmpty(recipeIds)) {
            getDrugEnterpriseResult(result, "处方ID参数为空");
            return result;
        }
        Integer depId = enterprise.getId();

        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        if (!ObjectUtils.isEmpty(recipeList)) {
            PatientService patientService = BasicAPI.getService(PatientService.class);
            OrganService organService = BasicAPI.getService(OrganService.class);

            for (Recipe dbRecipe : recipeList) {
                //设置电子处方单号
                dbRecipe.getRecipeCode();
                //设置组织机构编码
                dbRecipe.getClinicOrgan();
                //设置处方单号
                dbRecipe.getRecipeId();
                //设置机构名称
                dbRecipe.getOrganName();
                //设置处方类型
                dbRecipe.getRecipeType();
                //设置患者证件类型
                PatientDTO patient = patientService.get(dbRecipe.getMpiid());
                if (patient == null) {
                    getDrugEnterpriseResult(result, "不存在的患者");
                    return result;
                }
                patient.getCertificateType();
                patient.getPatientName();
                patient.getMobile();
                patient.getCertificate();
                //获取查询范围

            }
        }
        return null;
    }

    /**
     * 返回调用信息
     * @param result DrugEnterpriseResult
     * @param msg     提示信息
     * @return DrugEnterpriseResult
     */
    private DrugEnterpriseResult getDrugEnterpriseResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
        LOGGER.info("HddyfRemoteService-getDrugEnterpriseResult提示信息：{}.", msg);
        return result;
    }
}
