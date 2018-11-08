package recipe.service;

import com.google.common.collect.Maps;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.hisprescription.model.HosRecipeResult;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.hisprescription.model.HospitalStatusUpdateDTO;
import com.ngari.recipe.hisprescription.service.IHosPrescriptionService;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import recipe.ApplicationUtils;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.service.hospitalrecipe.PrescribeService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * 对接第三方医院服务
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/4/17.
 */
@RpcBean("hosPrescriptionService")
public class HosPrescriptionService implements IHosPrescriptionService {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(HosPrescriptionService.class);

    @Autowired
    @Qualifier("remotePrescribeService")
    private PrescribeService prescribeService;

    /**
     * 接收第三方处方
     *
     * @param hospitalRecipeList 医院处方
     * @return 结果
     */
    @Override
    @RpcService
    public HosRecipeResult createPrescription(HospitalRecipeDTO hospitalRecipeDTO) {
        HosRecipeResult<RecipeBean> result = prescribeService.createPrescription(hospitalRecipeDTO);
        Integer recipeId = null;
        if (HosRecipeResult.SUCCESS.equals(result.getCode())) {
            RecipeBean recipe = result.getData();
            recipeId = recipe.getRecipeId();
            HosRecipeResult orderResult = createBlankOrderForHos(recipe, hospitalRecipeDTO);
            if (HosRecipeResult.FAIL.equals(orderResult.getCode())) {
                result.setCode(HosRecipeResult.FAIL);
                result.setMsg(orderResult.getMsg());
            }
        }

        if (HosRecipeResult.DUPLICATION.equals(result.getCode())) {
            result.setCode(HosRecipeResult.SUCCESS);
        }
        RecipeBean backNew = new RecipeBean();
        backNew.setRecipeId(recipeId);
        result.setData(backNew);
        return result;
    }

    @RpcService
    public HosRecipeResult updateRecipeStatus(HospitalStatusUpdateDTO request) {
        HosRecipeResult result = prescribeService.updateRecipeStatus(request);
        return result;
    }

    public HosRecipeResult createBlankOrderForHos(RecipeBean recipe, HospitalRecipeDTO hospitalRecipeDTO) {
        HosRecipeResult result = new HosRecipeResult();
        result.setCode(HosRecipeResult.FAIL);

        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);

        //创建订单
        //待煎费或者膏方制作费，存在该值说明需要待煎
        String decoctionFeeStr = hospitalRecipeDTO.getDecoctionFee();
        boolean decoctionFlag = StringUtils.isNotEmpty(decoctionFeeStr)
                && RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType()) ? true : false;
        boolean gfFeeFlag = StringUtils.isNotEmpty(decoctionFeeStr)
                && RecipeBussConstant.RECIPETYPE_HP.equals(recipe.getRecipeType()) ? true : false;
        Map<String, String> orderMap = Maps.newHashMap();
        orderMap.put("operMpiId", recipe.getMpiid());
        //PayWayEnum.UNKNOW
        orderMap.put("payway", "-1");
        orderMap.put("payMode", null != recipe.getPayMode() ? recipe.getPayMode().toString() : "0");
        orderMap.put("decoctionFlag", decoctionFlag ? "1" : "0");
        orderMap.put("gfFeeFlag", gfFeeFlag ? "1" : "0");
        orderMap.put("calculateFee", "0");
        OrderCreateResult orderCreateResult = orderService.createOrder(
                Collections.singletonList(recipe.getRecipeId()), orderMap, 1);
        if (null != orderCreateResult && OrderCreateResult.SUCCESS.equals(orderCreateResult.getCode())) {
            try {
                //更新订单数据
                Map<String, Object> orderAttr = Maps.newHashMap();
                orderAttr.put("status", OrderStatusConstant.READY_PAY);
                orderAttr.put("effective", 0);
                orderAttr.put("payFlag", recipe.getPayFlag());
                //接收患者手机信息
                orderAttr.put("recMobile", hospitalRecipeDTO.getPatientTel());
                //服务费为0
                orderAttr.put("registerFee", BigDecimal.ZERO);
                orderAttr.put("recipeFee", recipe.getTotalMoney());
                orderAttr.put("expressFee", StringUtils.isEmpty(hospitalRecipeDTO.getExpressFee()) ?
                        BigDecimal.ZERO : new BigDecimal(hospitalRecipeDTO.getExpressFee()));
                orderAttr.put("decoctionFee", StringUtils.isEmpty(decoctionFeeStr) ?
                        BigDecimal.ZERO : new BigDecimal(decoctionFeeStr));
                orderAttr.put("couponFee", StringUtils.isEmpty(hospitalRecipeDTO.getCouponFee()) ?
                        BigDecimal.ZERO : new BigDecimal(hospitalRecipeDTO.getCouponFee()));
                orderAttr.put("totalFee", new BigDecimal(hospitalRecipeDTO.getOrderTotalFee()));
                orderAttr.put("actualPrice", new BigDecimal(hospitalRecipeDTO.getActualFee()).doubleValue());

                RecipeResultBean resultBean = orderService.updateOrderInfo(
                        orderCreateResult.getOrderCode(), orderAttr, null);
                if (RecipeResultBean.SUCCESS.equals(resultBean.getCode())) {
                    LOG.info("createPrescription 订单更新成功 orderCode={}", orderCreateResult.getOrderCode());
                    result.setCode(HosRecipeResult.SUCCESS);
                } else {
                    LOG.warn("createPrescription 订单更新失败. recipeCode={}, orderCode={}",
                            hospitalRecipeDTO.getRecipeCode(), orderCreateResult.getOrderCode());
                    updateOrderError(recipe.getRecipeId(), hospitalRecipeDTO.getRecipeCode(), result);
                }
            } catch (Exception e) {
                LOG.warn("createPrescription 订单更新异常. recipeCode={}, orderCode={}",
                        hospitalRecipeDTO.getRecipeCode(), orderCreateResult.getOrderCode(), e);
                updateOrderError(recipe.getRecipeId(), hospitalRecipeDTO.getRecipeCode(), result);
            }
        } else {
            LOG.warn("createPrescription 创建订单失败. recipeCode={}, result={}",
                    hospitalRecipeDTO.getRecipeCode(), JSONUtils.toString(orderCreateResult));
            //删除处方
            recipeService.delRecipeForce(recipe.getRecipeId());
            result.setMsg("处方[" + hospitalRecipeDTO.getRecipeCode() + "]订单创建失败, 原因：" + orderCreateResult.getMsg());
        }

        return result;
    }

    private void updateOrderError(Integer recipeId, String orderCode, HosRecipeResult result) {
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        //删除订单

        //删除处方
        recipeService.delRecipeForce(recipeId);
        result.setMsg("处方[" + orderCode + "]订单更新失败");
    }

}
