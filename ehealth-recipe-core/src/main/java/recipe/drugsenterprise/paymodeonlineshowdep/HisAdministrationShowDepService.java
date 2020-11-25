package recipe.drugsenterprise.paymodeonlineshowdep;

import com.google.common.collect.ImmutableMap;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;

import java.util.Arrays;
import java.util.List;

/**
 * created by shiyuping on 2020/11/10
 * @author shiyuping
 * his管理的药企
 */
public class HisAdministrationShowDepService implements PayModeOnlineShowDepInterface {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(HisAdministrationShowDepService.class);

    @Override
    public void getPayModeOnlineShowDep(DrugsEnterprise dep, List<DepDetailBean> depDetailList, Recipe dbRecipe, List<Integer> recipeIdList) {
        RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        //需要从接口获取药店列表
        DrugEnterpriseResult drugEnterpriseResult = remoteDrugService.findSupportDep(Arrays.asList(dbRecipe.getRecipeId()), ImmutableMap.of("recipeIds", recipeIdList), dep);
        if (DrugEnterpriseResult.SUCCESS.equals(drugEnterpriseResult.getCode())) {
            Object result = drugEnterpriseResult.getObject();
            if (result != null && result instanceof List) {
                List<DepDetailBean> hrList = (List) result;
                depDetailList.addAll(hrList);
            }
            LOG.info("HisAdministrationShowDepService获取到的药店列表:{}.", JSONUtils.toString(depDetailList));
        }
    }
}
