package recipe.service.common;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.common.RecipeStandardReqTO;
import com.ngari.recipe.common.RecipeStandardResTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.CheckYsInfoBean;
import recipe.constant.*;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.service.*;
import recipe.util.MapValueUtil;
import recipe.util.RedisClient;
import recipe.util.RegexUtils;

import java.util.Map;
import java.util.Set;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 处方签名服务
 * @version： 1.0
 */
@RpcBean("recipeSignService")
public class RecipeSignService {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RecipeSignService.class);

    @Autowired
    private RecipeDAO recipeDAO;

    @Autowired
    private RedisClient redisClient;

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

        //查询订单
        RecipeOrder order = DAOFactory.getDAO(RecipeOrderDAO.class).getByOrderCode(dbRecipe.getOrderCode());
        if (null == order) {
            response.setMsg("订单不存在");
            return response;
        }

        //配送数据校验
        Map<String, Object> conditions = request.getConditions();
        Integer giveMode = MapValueUtil.getInteger(conditions, "giveMode");
        Integer depId = MapValueUtil.getInteger(conditions, "depId");
        if (null == depId && !RecipeBussConstant.GIVEMODE_FREEDOM.equals(giveMode)) {
            response.setMsg("缺少药企编码");
            return response;
        }

        String depName = MapValueUtil.getString(conditions, "depName");
        String pharmacyCode = MapValueUtil.getString(conditions, "pharmacyCode");
        String pharmacyAddress = MapValueUtil.getString(conditions, "pharmacyAddress");
        String patientAddress = MapValueUtil.getString(conditions, "patientAddress");
        String patientTel = MapValueUtil.getString(conditions, "patientTel");
        Integer payMode = null;
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
                //校验参数准确性
                if (!RegexUtils.regular(patientTel, RegexEnum.MOBILE)) {
                    response.setMsg("请输入有效手机号码");
                    return response;
                }
                if (StringUtils.length(patientAddress) > 100) {
                    response.setMsg("地址不能超过100个字");
                    return response;
                }
                payMode = RecipeBussConstant.PAYMODE_ONLINE;
            } else if (RecipeBussConstant.GIVEMODE_FREEDOM.equals(giveMode)) {
                //患者自由选择
                depId = null;
                payMode = RecipeBussConstant.PAYMODE_COMPLEX;
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
            //为确保通知能送达用户手机需要重置下手机信息
            if (StringUtils.isEmpty(patientTel)) {
                PatientService patientService = BasicAPI.getService(PatientService.class);
                PatientDTO patient = patientService.get(dbRecipe.getMpiid());
                if (null != patient) {
                    patientTel = patient.getMobile();
                    patientAddress = patient.getAddress();
                } else {
                    LOG.warn("sign 患者不存在，可能导致短信无法通知. recipeId={}, mpiId={}", recipeId, dbRecipe.getMpiid());
                }
            }

            // 修改订单一些参数
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            Map<String, Object> orderAttr = Maps.newHashMap();
            orderAttr.put("enterpriseId", depId);
            //未支付不知道支付方式
            orderAttr.put("wxPayWay", "-1");
            //使订单生效
            orderAttr.put("effective", 1);
            orderAttr.put("receiver", dbRecipe.getPatientName());
            orderAttr.put("address4", patientAddress);
            orderAttr.put("recMobile", patientTel);
            orderAttr.put("drugStoreName", depName);
            orderAttr.put("drugStoreAddr", pharmacyAddress);
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
        attrMap.put("enterpriseId", depId);
        attrMap.put("chooseFlag", 1);
        //不做失效前提醒
        attrMap.put("remindFlag", 1);

        /**
         * 药店取药和自由选择都流转到药师审核，审核完成推送给药企
         */
        boolean sendYsCheck = false;
        Integer status = RecipeStatusConstant.CHECK_PASS;
        if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode) || RecipeBussConstant.GIVEMODE_FREEDOM.equals(giveMode)) {
            status = RecipeStatusConstant.READY_CHECK_YS;
            sendYsCheck = true;
        }
        recipeDAO.updateRecipeInfoByRecipeId(recipeId, status, attrMap);

        //HIS同步处理
        if (!RecipeBussConstant.GIVEMODE_FREEDOM.equals(giveMode)) {
            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
            RecipeResultBean hisResult = hisService.recipeDrugTake(recipeId, PayConstant.PAY_FLAG_NOT_PAY, null);
            //TODO HIS处理失败暂时略过
//        if (RecipeResultBean.FAIL.equals(hisResult.getCode())) {
//            LOG.warn("sign recipeId=[{}]更改取药方式失败，error={}", recipeId, hisResult.getError());
//            response.setMsg("HIS更改取药方式失败");
//            return response;
//        }
        }

        //根据配置判断是否需要人工审核, 配送到家处理在支付完成后回调 RecipeOrderService finishOrderPay
        if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode) || RecipeBussConstant.GIVEMODE_FREEDOM.equals(giveMode)) {
            Set<String> organIdList = redisClient.sMembers(CacheConstant.KEY_SKIP_YSCHECK_LIST);
            if (CollectionUtils.isNotEmpty(organIdList) && organIdList.contains(dbRecipe.getClinicOrgan().toString())) {
                RecipeCheckService checkService = ApplicationUtils.getRecipeService(RecipeCheckService.class);
                //不用发药师消息
                sendYsCheck = false;
                //跳过人工审核
                CheckYsInfoBean checkResult = new CheckYsInfoBean();
                checkResult.setRecipeId(recipeId);
                checkResult.setCheckDoctorId(dbRecipe.getDoctor());
                checkResult.setCheckOrganId(dbRecipe.getClinicOrgan());
                try {
                    checkService.autoPassForCheckYs(checkResult);
                } catch (Exception e) {
                    LOG.error("sign 药师自动审核失败. recipeId={}", recipeId);
                    RecipeLogService.saveRecipeLog(recipeId, dbRecipe.getStatus(), status,
                            "sign 药师自动审核失败:" + e.getMessage());
                }
            }
        }

        //设置其他参数
        response.setData(ImmutableMap.of("orderId", order.getOrderId()));

        //日志记录
        RecipeLogService.saveRecipeLog(recipeId, dbRecipe.getStatus(), status, "sign 完成 giveMode=" + giveMode);
        response.setCode(RecipeCommonBaseTO.SUCCESS);

        //推送身边医生消息
        if (sendYsCheck) {
            RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_YS_READYCHECK_4HIS, dbRecipe);
        }
        return response;
    }

}
