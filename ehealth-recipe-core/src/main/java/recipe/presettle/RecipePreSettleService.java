package recipe.presettle;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Maps;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.constant.RecipeBussConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.presettle.factory.PreSettleFactory;

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
        Integer depId = recipeOrder.getEnterpriseId();
        Integer orderType = recipeOrder.getOrderType() == null ? 0 : recipeOrder.getOrderType();
        String insuredArea = extend.getInsuredArea();
        Map<String, Object> param = Maps.newHashMap();
        param.put("depId", depId);
        param.put("insuredArea", insuredArea);
        param.put("recipeNoS", JSONUtils.toString(recipeNoS));
        param.put("payMode", recipeOrder.getPayMode());
        //获取对应预结算服务
        IRecipePreSettleService preSettleService = PreSettleFactory.getPreSettleService(recipe.getClinicOrgan(),orderType);
        if (preSettleService != null){
            return preSettleService.recipePreSettle(recipe.getRecipeId(), param);
        }

        /*if (orderType == 0 && RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())) {
            //平台自费预结算--仅少数机构用到
            //目前省中走自费预结算
            if ((dep != null && new Integer(1).equals(dep.getIsHosDep()))) {
                return recipeHisService.provincialCashPreSettle(recipe.getRecipeId(), recipe.getPayMode());
            }
        } else {
            //获取医保支付开关端配置----浙江省互联网没有预结算--浙江省不会打开医保端配置
            //杭州是互联网会打开医保端配置
            //杭州市互联网自费预结算不调预结算接口
            if (!RecipeBussConstant.ORDERTYPE_ZF.equals(orderType)) {
                ICommonService commonService = BaseAPI.getService(ICommonService.class);
                Boolean medicalPayConfig = (Boolean) commonService.getClientConfigByKey("medicalPayConfig");
                //杭州市医保预结算和自费预结算一样
                //平台省医保预结算
                String insuredArea = extend.getInsuredArea();
                Map<String, String> param = Maps.newHashMap();
                param.put("depId", String.valueOf(depId));
                param.put("insuredArea", insuredArea);
                param.put("recipeNoS", JSONUtils.toString(recipeNoS));
                LOGGER.info("unifyRecipePreSettle recipe={},param={},medicalPayConfig={}", recipe.getRecipeId(), JSONUtils.toString(param), medicalPayConfig);
                if (medicalPayConfig) {
                    return recipeHisService.provincialMedicalPreSettle(recipe.getRecipeId(), param);
                }
            }

        }*/
        result.put("code", "200");
        return result;
    }
}
