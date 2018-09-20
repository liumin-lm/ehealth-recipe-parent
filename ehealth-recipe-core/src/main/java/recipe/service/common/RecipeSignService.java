package recipe.service.common;

import com.google.common.collect.Maps;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.common.RecipeStandardReqTO;
import com.ngari.recipe.common.RecipeStandardResTO;
import com.ngari.recipe.entity.Recipe;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.constant.PayConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.service.RecipeHisService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeOrderService;
import recipe.service.RecipeService;
import recipe.util.MapValueUtil;

import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 处方签名服务
 * @version： 1.0
 */
@RpcBean(value = "recipeSignService", mvc_authentication = false)
public class RecipeSignService {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RecipeSignService.class);

    @Autowired
    private RecipeDAO recipeDAO;


    @RpcService
    public RecipeStandardResTO<Map> sign(Integer recipeId, RecipeStandardReqTO request) {
        RecipeStandardResTO<Map> response = RecipeStandardResTO.getRequest(Map.class);
        response.setCode(RecipeCommonBaseTO.FAIL);
        //TODO 先校验处方是否有效
        if (null == recipeId) {
            response.setMsg("处方单ID为空");
            return response;
        }

        Recipe dbRecipe = recipeDAO.get(recipeId);
        if (null == dbRecipe) {
            response.setMsg("没有该处方单");
            return response;
        }

        if (RecipeStatusConstant.UNSIGN != dbRecipe.getStatus()) {
            response.setMsg("处方单已签名");
            return response;
        }

        //配送数据校验
        Map<String, Object> conditions = request.getConditions();
        Integer depId = MapValueUtil.getInteger(conditions, "depId");
        if (null == depId) {
            response.setMsg("缺少药企编码");
            return response;
        }
        Integer giveMode = MapValueUtil.getInteger(conditions, "giveMode");
        String pharmacyName = MapValueUtil.getString(conditions, "pharmacyName");
        String pharmacyCode = MapValueUtil.getString(conditions, "pharmacyCode");
        String patientAddress = MapValueUtil.getString(conditions, "patientAddress");
        String patientTel = MapValueUtil.getString(conditions, "patientTel");
        Integer payMode;
        if (null != giveMode) {
            if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode)) {
                //药店取药
                if (StringUtils.isEmpty(pharmacyCode)) {
                    response.setMsg("缺少药店编码");
                    return response;
                }
                payMode = RecipeBussConstant.PAYMODE_TFDS;
            } else if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)) {
                //配送到家
                if (StringUtils.isEmpty(patientAddress) || StringUtils.isEmpty(patientTel)) {
                    response.setMsg("配送信息不全");
                    return response;
                }
                payMode = RecipeBussConstant.PAYMODE_ONLINE;
            } else {
                response.setMsg("缺少取药方式");
                return response;
            }
        } else {
            response.setMsg("缺少取药方式");
            return response;
        }

        //签名
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        try {
            //写入his成功后，生成pdf并签名
            recipeService.generateRecipePdfAndSign(recipeId);
        } catch (Exception e) {
            LOG.warn("sign 签名服务异常，recipeId={}", recipeId, e);
        }

        //修改订单
        if (StringUtils.isEmpty(dbRecipe.getOrderCode())) {
            //订单在接收HIS处方时生成
            response.setMsg("处方订单不存在");
            return response;
        } else {
            // 修改订单一些参数
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            Map<String, Object> orderAttr = Maps.newHashMap();
            orderAttr.put("payMode", payMode.toString());
            orderAttr.put("depId", depId.toString());
            //未支付不知道支付方式
            orderAttr.put("payway", "-1");
            //使订单生效
            orderAttr.put("effective", 1);
            orderAttr.put("receiver", dbRecipe.getPatientName());
            orderAttr.put("address4", patientAddress);
            orderAttr.put("recMobile", patientTel);
            orderAttr.put("drugStoreName", pharmacyName);
            orderAttr.put("drugStoreAddr", patientAddress);
            RecipeResultBean resultBean = orderService.updateOrderInfo(dbRecipe.getOrderCode(), orderAttr, null);
            if (RecipeResultBean.SUCCESS.equals(resultBean.getCode())) {
                LOG.info("sign 订单更新成功 recipeId={}, orderCode={}", recipeId, dbRecipe.getOrderCode());
            } else {
                LOG.warn("sign 订单更新失败. recipeId={}, orderCode={}", recipeId, dbRecipe.getOrderCode());
                response.setMsg("处方订单更新错误");
                return response;
            }
        }

        //修改订单成功后再去更新处方状态及配送信息等，使接口可重复调用
        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("giveMode", giveMode);
        attrMap.put("payMode", payMode);
        attrMap.put("chooseFlag", 1);
        //不做失效前提醒
        attrMap.put("remindFlag", 1);
        recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.CHECK_PASS, attrMap);

        //HIS同步处理
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        RecipeResultBean hisResult = hisService.recipeDrugTake(recipeId, PayConstant.PAY_FLAG_NOT_PAY, null);
        if (RecipeResultBean.FAIL.equals(hisResult)) {
            LOG.warn("sign recipeId=[{}]更改取药方式失败，error={}", recipeId, hisResult.getError());
            response.setMsg("HIS更改取药方式失败");
            return response;
        }

        //日志记录
        RecipeLogService.saveRecipeLog(recipeId, dbRecipe.getStatus(), RecipeStatusConstant.CHECK_PASS, "sign 完成");
        response.setCode(RecipeCommonBaseTO.SUCCESS);
        return response;
    }

}
