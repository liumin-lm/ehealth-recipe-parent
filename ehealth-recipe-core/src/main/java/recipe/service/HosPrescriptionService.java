package recipe.service;

import com.google.common.collect.Maps;
import com.ngari.recipe.common.RecipeCommonResTO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.hisprescription.model.HosRecipeResult;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.hisprescription.model.HospitalStatusUpdateDTO;
import com.ngari.recipe.hisprescription.service.IHosPrescriptionService;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import recipe.ApplicationUtils;
import recipe.prescription.PrescribeService;

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
//        String recipeInfo = "{ \"clinicId\": \"1\", \"certificateType \": \"1\", \"certificate \": \"330624198808141671\",  \"patientTel \": \"17706521132\", \"patientName\": \"张三\",\"patientNumber \": \"P100\", \"clinicOrgan\": \"1\", \"recipeCode\": \"CF001001\", \"recipeType\": \"1\",  \"doctorNumber\": \"0020\", \"doctorName\": \"测试doc\",\"createDate\": \"2018-03-22 10:40:30\", \"recipeFee\": \"100.01\", \"actualFee\": \"105.02\", \"couponFee\": \"0.00\", \"expressFee\": \"5.01\",\"decoctionFee\": \"0.00\",\"medicalFee\": \"0.00\", \"orderTotalFee\": \"105.02\", \"organDiseaseName\": \"A8888\", \"organDiseaseId\": \"感冒\", \"payMode\": \"3\", \"giveMode\": \"2\",\"giveUser\": \"测试发药\",\"status\": \"2\", \"memo\": \"诊断备注\", \"medicalPayFlag\": \"0\", \"distributionFlag\": \"0\", \"recipeMemo\": \"处方备注\", \"tcmUsePathways\": \"\",\"tcmUsingRate\": \"\",  \"tcmNum \": \"\",  \"takeMedicine\": \"\",  \"drugList\": [{ \"drugCode\": \"111001402\",   \"drugName\": \"头孢\", \"total\": \"2\",  \"useDose\": \"0.1\",\"drugFee\": \"50.005\", \"medicalFee\": \"0\", \"drugTotalFee\": \"100.01\", \"uesDays\": \"3\",  \"pharmNo\": \"8\", \"usingRate\": \"qid\",\"usePathways\": \"po\", \"memo\": \"药品使用备注\"}]}";
        HosRecipeResult result = prescribeService.createPrescription(hospitalRecipeDTO);
        if (HosRecipeResult.SUCCESS.equals(result.getCode())) {
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
            //创建订单
            Map<String, String> orderMap = Maps.newHashMap();
            orderMap.put("operMpiId", recipe.getMpiid());
            //PayWayEnum.UNKNOW
            orderMap.put("payway", "-1");
            orderMap.put("payMode", recipe.getPayMode().toString());
            orderMap.put("decoctionFlag", "0");
            orderMap.put("gfFeeFlag", "0");
            orderMap.put("calculateFee", "0");
            OrderCreateResult orderCreateResult = orderService.createOrder(
                    Collections.singletonList(recipeId), orderMap, 1);
            if (null != orderCreateResult && OrderCreateResult.SUCCESS.equals(orderCreateResult.getCode())) {
                try {
                    //更新订单数据
                    Map<String, Object> orderAttr = Maps.newHashMap();
                    orderAttr.put("status", OrderStatusConstant.READY_PAY);
                    orderAttr.put("effective", 1);
                    orderAttr.put("payFlag", recipe.getPayFlag());
                    orderAttr.put("registerFee", BigDecimal.ZERO);
                    orderAttr.put("recipeFee", recipe.getTotalMoney());
                    orderAttr.put("expressFee", StringUtils.isEmpty(hospitalRecipe.getExpressFee()) ?
                            BigDecimal.ZERO : new BigDecimal(hospitalRecipe.getExpressFee()));
                    orderAttr.put("decoctionFee", StringUtils.isEmpty(hospitalRecipe.getDecoctionFee()) ?
                            BigDecimal.ZERO : new BigDecimal(hospitalRecipe.getDecoctionFee()));
                    orderAttr.put("couponFee", StringUtils.isEmpty(hospitalRecipe.getCouponFee()) ?
                            BigDecimal.ZERO : new BigDecimal(hospitalRecipe.getCouponFee()));
                    orderAttr.put("totalFee", StringUtils.isEmpty(hospitalRecipe.getOrderTotalFee()) ?
                            BigDecimal.ZERO : new BigDecimal(hospitalRecipe.getOrderTotalFee()));
                    orderAttr.put("actualPrice", StringUtils.isEmpty(hospitalRecipe.getActualFee()) ?
                            0d : new BigDecimal(hospitalRecipe.getActualFee()).doubleValue());

                    RecipeResultBean resultBean = orderService.updateOrderInfo(
                            orderCreateResult.getOrderCode(), orderAttr, null);
                    LOG.info("createPrescription 订单更新 orderCode={}, result={}",
                            orderCreateResult.getOrderCode(), JSONUtils.toString(resultBean));
                } catch (Exception e) {
                    LOG.warn("createPrescription 订单更新失败. recipeId={}, orderCode={}",
                            recipeId, orderCreateResult.getOrderCode(), e);
                    //删除处方
                    recipeService.delRecipeForce(recipeId);
                    result.setCode(RecipeCommonResTO.FAIL);
                    result.setMsg("处方[" + result.getRecipeCode() + "]订单更新失败");
                }
            } else {
                LOG.warn("createPrescription 创建订单失败. recipeId={}, result={}",
                        recipeId, JSONUtils.toString(orderCreateResult));
                //删除处方
                recipeService.delRecipeForce(recipeId);
                result.setCode(RecipeCommonResTO.FAIL);
                result.setMsg("处方[" + result.getRecipeCode() + "]订单创建失败, 原因：" + orderCreateResult.getMsg());
            }*/
        }

//        result.setRecipe(null);
//        result.setHospitalRecipe(null);
        return result;
    }

    @RpcService
    public HosRecipeResult updateRecipeStatus(HospitalStatusUpdateDTO request) {
        HosRecipeResult result = prescribeService.updateRecipeStatus(request);
        return result;
    }


}
