package recipe.drugsenterprise.paymodeonlineshowdep;

import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.RecipeBussConstant;
import recipe.dao.HisRecipeDAO;
import recipe.service.RecipeOrderService;

import java.util.List;

/**
 * created by shiyuping on 2020/11/10
 * 线下转线上
 * @author shiyuping
 */
public class OfflineToOnlineShowDepService implements PayModeOnlineShowDepInterface {
    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(OfflineToOnlineShowDepService.class);

    @Override
    public void getPayModeOnlineShowDep(DrugsEnterprise dep, List<DepDetailBean> depDetailList, Recipe dbRecipe, List<Integer> recipeIdList) {
        DepDetailBean depDetailBean = new DepDetailBean();
        depDetailBean.setDepId(dep.getId());
        depDetailBean.setDepName(dep.getName());
        depDetailBean.setBelongDepName(dep.getName());
        depDetailBean.setOrderType(dep.getOrderType());
        depDetailBean.setMemo(dep.getMemo());
        if (RecipeBussConstant.PAYMODE_ONLINE.equals(dep.getPayModeSupport()) || RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS.equals(dep.getPayModeSupport())) {
            depDetailBean.setPayModeText("在线支付");
            depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_ONLINE);
        } else {
            depDetailBean.setPayModeText("货到付款");
            depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_COD);
        }
        HisRecipeDAO hisRecipeDAO = DAOFactory.getDAO(HisRecipeDAO.class);
        HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(dbRecipe.getClinicOrgan(), dbRecipe.getRecipeCode());
        //北京互联网线下转线上相关处理
        if (hisRecipe != null && StringUtils.isNotEmpty(hisRecipe.getDeliveryCode())) {
            if (new Integer(2).equals(hisRecipe.getMedicalType())) {
                depDetailBean.setPayModeText("货到付款");
                depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_COD);
            } else {
                depDetailBean.setPayModeText("在线支付");
                depDetailBean.setPayMode(RecipeBussConstant.PAYMODE_ONLINE);
            }
        }
        RecipeOrderService recipeOrderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        //重置药企处方价格
        depDetailBean.setRecipeFee(recipeOrderService.reCalculateRecipeFee(dep.getId(), recipeIdList, null));
        depDetailList.add(depDetailBean);
        LOG.info("BJIntShowDepService获取到的药店列表:{}.", JSONUtils.toString(depDetailList));
    }
}
