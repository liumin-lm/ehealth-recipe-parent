package recipe.presettle;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Maps;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import jdk.nashorn.internal.ir.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.business.StockBusinessService;
import recipe.constant.RecipeBussConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.presettle.factory.PreSettleFactory;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * created by shiyuping on 2020/9/22
 * @author shiyuping
 */
@RpcBean
public class RecipePreSettleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipePreSettleService.class);

    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Resource
    private StockBusinessService stockBusinessService;

    /**
     * 统一处方预结算接口
     * 整合平台医保预结算/自费预结算以及杭州市互联网医保和自费预结算接口
     * 提交订单后支付前调用
     * <p>
     * 省医保小程序没有走这个接口还是走的原有流程recipeMedicInsurPreSettle
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public Map<String, Object> unifyRecipePreSettle(List<Integer> recipeId) {
        LOGGER.info("unifyRecipePreSettle recipeId={}", JSONArray.toJSONString(recipeId));
        Map<String, Object> result = Maps.newHashMap();
        result.put("code", "-1");
        if(recipeId == null || recipeId.size() == 0){
            result.put("msg", "查不到该处方扩展信息");
            return result;
        }
        Recipe recipe = null;
        List<String> recipeNoS = new ArrayList<>();
        for (int i = 0; i < recipeId.size(); i++) {
            recipe = recipeDAO.getByRecipeId(recipeId.get(i));
            if (recipe == null) {
                result.put("msg", "查不到该处方");
                return result;
            }
            recipeNoS.add(recipe.getRecipeCode());
        }
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        if (recipeOrder == null) {
            result.put("msg", "查不到该处方订单");
            return result;
        }
        RecipeExtend extend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if (extend == null) {
            result.put("msg", "查不到该处方扩展信息");
            return result;
        }
        if (!RecipeBussConstant.PAYMODE_ONLINE.equals(recipeOrder.getPayMode())) {
            LOGGER.info("unifyRecipePreSettle no support. recipeId={}", JSONUtils.toString(recipeId));
            result.put("code", "200");
            return result;
        }
        // 库存查询
        Boolean stockFlag = stockBusinessService.getStockFlag(recipeId,recipe,recipeOrder.getEnterpriseId());
        if(!stockFlag){
            result.put("msg", "库存不足");
            return result;
        }

        Integer depId = recipeOrder.getEnterpriseId();
        Integer orderType = recipeOrder.getOrderType() == null ? 0 : recipeOrder.getOrderType();
        String insuredArea = extend.getInsuredArea();
        Map<String, Object> param = Maps.newHashMap();
        param.put("depId", depId);
        param.put("insuredArea", insuredArea);
        param.put("recipeNoS", JSONUtils.toString(recipeNoS));
        param.put("payMode", recipeOrder.getPayMode());
        param.put("recipeIds", recipeId);
        //获取对应预结算服务
        IRecipePreSettleService preSettleService = PreSettleFactory.getPreSettleService(recipe.getClinicOrgan(),orderType);
        if (preSettleService != null){
            return preSettleService.recipePreSettle(recipe.getRecipeId(), param);
        }
        result.put("code", "200");
        return result;
    }
}
