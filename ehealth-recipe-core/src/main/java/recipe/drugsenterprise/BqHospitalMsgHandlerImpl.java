package recipe.drugsenterprise;

import com.alijk.bqhospital.alijk.message.BqHospitalMsgException;
import com.alijk.bqhospital.alijk.message.BqHospitalMsgHandler;
import com.alijk.bqhospital.alijk.message.BqHospitalPrescriptionStatus;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.UpdateTakeDrugWayReqTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.entity.Recipe;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.constant.AlDyfRecipeStatusConstant;
import recipe.constant.HisBussConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.hisservice.HisMqRequestInit;
import recipe.hisservice.RecipeToHisMqService;
import recipe.hisservice.RecipeToHisService;
import recipe.hisservice.syncdata.SyncExecutorService;
import recipe.service.RecipeHisService;
import recipe.service.RecipeMsgService;

import java.util.Date;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2019\3\7 0007 11:46
 */
public class BqHospitalMsgHandlerImpl implements BqHospitalMsgHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BqHospitalMsgHandlerImpl.class);

    @Autowired
    private RecipeDAO recipeDAO;

    @Override
    public void handle(BqHospitalPrescriptionStatus bqHospitalPrescriptionStatus) throws BqHospitalMsgException {
        String rxId = bqHospitalPrescriptionStatus.getRxId();
        Recipe recipe = recipeDAO.getByRecipeCode(rxId);
        LOGGER.info("接收到核销消息{}", rxId);
        if (bqHospitalPrescriptionStatus.getStatus() == AlDyfRecipeStatusConstant.VERIFICATION && recipe.getStatus() != RecipeStatusConstant.FINISH) {
            //将处方状态更新为已完成
            recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("status", RecipeStatusConstant.FINISH));
            LOGGER.info("处方状态已更新{}", rxId);
            finishRecipe(rxId);
        } else {
            LOGGER.info("未更新状态，接收到核销消息{},{}", bqHospitalPrescriptionStatus.getStatus(),rxId);
        }
    }

    public void finishRecipe(String rxId) {
        String deliveryType = "3";
        //取药方式进行HIS推送
        Recipe recipe = recipeDAO.getByRecipeCode(rxId);

        RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
        OrganService organService = BasicAPI.getService(OrganService.class);
        UpdateTakeDrugWayReqTO updateTakeDrugWayReqTO = new UpdateTakeDrugWayReqTO();

        if (!ObjectUtils.isEmpty(recipe)) {
            updateTakeDrugWayReqTO.setClinicOrgan(recipe.getClinicOrgan());
            updateTakeDrugWayReqTO.setRecipeID(recipe.getRecipeCode());
            updateTakeDrugWayReqTO.setOrganID(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));
            if (recipe.getClinicId() != null) {
                updateTakeDrugWayReqTO.setClinicID(recipe.getClinicId().toString());
            }
            updateTakeDrugWayReqTO.setDeliveryType(deliveryType);
            HisResponseTO hisResult = service.updateTakeDrugWay(updateTakeDrugWayReqTO);
            //更新平台处方
            recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("giveMode", 1, "chooseFlag", 1));

            LOGGER.info("取药方式更新通知his. param={},result={}", JSONUtils.toString(updateTakeDrugWayReqTO), JSONUtils.toString(hisResult));


            Map<String, Object> attrMap = Maps.newHashMap();
            attrMap.put("giveFlag", 1);
            attrMap.put("payFlag", 1);
            attrMap.put("giveMode", RecipeBussConstant.GIVEMODE_SEND_TO_HOME);
            attrMap.put("chooseFlag", 1);
            attrMap.put("payDate", new Date());
            //更新处方信息
            Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), RecipeStatusConstant.FINISH, attrMap);
            if (rs) {
                /*RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);*/
                //HIS消息发送
                LOGGER.info("HIS消息发送{}.", recipe.getRecipeId());
                /*hisService.recipeFinish(recipe.getRecipeId());*/
                RecipeToHisMqService hisMqService = ApplicationUtils.getRecipeService(RecipeToHisMqService.class);
                hisMqService.recipeStatusToHis(HisMqRequestInit.initRecipeStatusToHisReq(recipe, HisBussConstant.TOHIS_RECIPE_STATUS_FINISH,"阿里健康平台处方配送"));
                //配送到家
                LOGGER.info("配送到家消息发送{}.",recipe.getRecipeId());
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.PATIENT_REACHPAY_FINISH);
                //监管平台核销上传
                SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
                syncExecutorService.uploadRecipeVerificationIndicators(recipe.getRecipeId());
            }
        }

    }


}
