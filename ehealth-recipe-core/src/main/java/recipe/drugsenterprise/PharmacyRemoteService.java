package recipe.drugsenterprise;

import com.google.common.collect.Lists;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.util.JSONUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.CacheConstant;
import recipe.constant.DrugEnterpriseConstant;
import recipe.util.RedisClient;

import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/20
 * @description： TODO
 * @version： 1.0
 */
public class PharmacyRemoteService extends AccessDrugEnterpriseService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PharmacyRemoteService.class);

    @Autowired
    private RedisClient redisClient;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("PharmacyRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return "暂不支持库存查询";
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        return null;
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("PharmacyRemoteService pushRecipeInfo not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("PharmacyRemoteService scanStock not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        LOGGER.info("PharmacyRemoteService syncEnterpriseDrug not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        LOGGER.info("PharmacyRemoteService pushCheckResult not implement.");
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        String testData = redisClient.get(CacheConstant.KEY_PHARYACY_TEST_DATA);
        if (StringUtils.isNotEmpty(testData)) {
            List<DepDetailBean> backList = Lists.newArrayList();
            List<Map> list = JSONUtils.parse(testData, List.class);
            DepDetailBean bean;
            for (Map map : list) {
                bean = new DepDetailBean();
                try {
                    BeanUtils.populate(bean, map);
                } catch (Exception e) {

                }
                backList.add(bean);
            }
            result.setObject(backList);
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println((int) (Math.random() * 100));
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_PHARMACY;
    }
}
