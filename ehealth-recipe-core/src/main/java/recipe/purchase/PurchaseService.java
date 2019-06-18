package recipe.purchase;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.dao.RecipeDAO;
import recipe.service.RecipeService;
import recipe.service.common.RecipeCacheService;

import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 购药入口类
 * @version： 1.0
 */
@RpcBean(value = "purchaseService", mvc_authentication = false)
public class PurchaseService {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PurchaseService.class);

    /**
     * 根据对应的购药方式展示对应药企
     *
     * @param recipeId
     * @param payModes
     */
    @RpcService
    public RecipeResultBean filterSupportDepList(Integer recipeId, List<Integer> payModes, Map ext) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);

        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Recipe dbRecipe = recipeDAO.get(recipeId);
        if (null == dbRecipe) {
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("处方不存在");
            return resultBean;
        }

        for (Integer i : payModes) {
            IPurchaseService purchaseService = getService(i);
            //如果涉及到多种购药方式合并成一个列表，此处需要进行合并
            resultBean = purchaseService.findSupportDepList(dbRecipe, ext);

        }

        return resultBean;
    }

    public IPurchaseService getService(Integer payMode) {
        PurchaseEnum[] list = PurchaseEnum.values();
        String serviceName = null;
        for (PurchaseEnum e : list) {
            if (e.getPayMode().equals(payMode)) {
                serviceName = e.getServiceName();
                break;
            }
        }

        IPurchaseService purchaseService = null;
        if (StringUtils.isNotEmpty(serviceName)) {
            purchaseService = AppContextHolder.getBean(serviceName, IPurchaseService.class);
        }

        return purchaseService;
    }
}
