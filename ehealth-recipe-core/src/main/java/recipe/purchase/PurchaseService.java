package recipe.purchase;

import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.PltPurchaseResponse;
import recipe.dao.RecipeDAO;
import recipe.service.RecipeService;
import recipe.service.common.RecipeCacheService;
import recipe.util.MapValueUtil;

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
     * 获取可用购药方式
     *
     * @return
     */
    @RpcService
    public PltPurchaseResponse showPurchaseMode(Integer recipeId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        PltPurchaseResponse result = new PltPurchaseResponse();
        Recipe dbRecipe = recipeDAO.get(recipeId);
        if (null == dbRecipe) {
            return result;
        }
        //TODO 配送到家和药店取药默认可用
        result.setSendToHome(true);
        result.setTfds(true);
        //到院取药判断
        boolean hisStatus = false;
        try {
            IHisConfigService iHisConfigService = ApplicationUtils.getBaseService(IHisConfigService.class);
            hisStatus = iHisConfigService.isHisEnable(dbRecipe.getClinicOrgan());
        } catch (Exception e) {
            LOG.warn("showPurchaseMode his exception. recipeId={}, hisStatus={}", recipeId, hisStatus, e);
        }
        if (Integer.valueOf(0).equals(dbRecipe.getDistributionFlag())
                && hisStatus) {
            result.setToHos(true);
        }

        return result;
    }

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

    @RpcService
    public OrderCreateResult order(Integer recipeId, Map<String, String> extInfo){
        OrderCreateResult result = new OrderCreateResult(RecipeResultBean.SUCCESS);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        Recipe dbRecipe = recipeDAO.get(recipeId);
        if (null == dbRecipe) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("处方不存在");
            return result;
        }

        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        if (null == payMode) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("缺少支付方式");
            return result;
        }

        IPurchaseService purchaseService = getService(payMode);
        RecipeResultBean resultBean = purchaseService.order(dbRecipe, extInfo);
        

        return result;
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
