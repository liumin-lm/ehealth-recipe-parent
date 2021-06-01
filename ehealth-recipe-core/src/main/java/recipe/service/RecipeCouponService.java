package recipe.service;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import coupon.api.service.ICouponBaseService;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.RecipeBussConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;

/**
 * 处理处方优惠券
 * @author yinsheng
 * @date 2019\10\18 0018 13:58
 */
@RpcBean("recipeCouponService")
public class RecipeCouponService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeCouponService.class);
    /**
     * 在药师审核不通过、医生二次开具处方、三天定时任务
     * 返回优惠券给用户
     * @param recipeId  处方ID
     */
    @RpcService
    public void unuseCouponByRecipeId(Integer recipeId){
        LOGGER.info("RecipeCouponService-unuseCouponByRecipeId:{}.", recipeId);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (recipe != null) {
            if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
                RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
                RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                LOGGER.info("RecipeCouponService-unuseCouponByRecipeId recipeOrder:{}.", JSONUtils.toString(recipeOrder));
                if (recipeOrder.getCouponId() != null && recipeOrder.getCouponId() > 0 && RecipeBussConstant.PAYMODE_ONLINE.equals(recipeOrder.getPayMode())) {
                    //返还优惠券
                    ICouponBaseService couponService = AppContextHolder.getBean("voucher.couponBaseService",ICouponBaseService.class);
                    couponService.unuseCouponById(recipeOrder.getCouponId());
                }
            }
        }
    }
}
