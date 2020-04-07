package recipe.mq;

import com.google.common.collect.Maps;
import com.ngari.platform.recipe.mode.NoticeNgariRecipeInfoReq;
import com.ngari.recipe.hisprescription.model.HosRecipeResult;
import com.ngari.recipe.hisprescription.model.HospitalStatusUpdateDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.net.broadcast.Observer;
import ctd.util.JSONUtils;
import eh.msg.constant.MqConstant;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.HisBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.service.RecipeLogService;
import recipe.service.RecipeService;
import recipe.service.hospitalrecipe.PrescribeService;
import recipe.util.LocalStringUtil;

import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/12/3
 * @description： 接收从HIS发来的处方状态变更消息
 * @version： 1.0
 */
public class RecipeStatusFromHisObserver implements Observer<NoticeNgariRecipeInfoReq> {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeStatusFromHisObserver.class);

    @Override
    public void onMessage(NoticeNgariRecipeInfoReq notice) {
        LOGGER.info("topic={}, tag={}, notice={}", OnsConfig.hisCdrinfo, MqConstant.HIS_CDRINFO_TAG_TO_PLATFORM
                , JSONUtils.toString(notice));
        if (null == notice) {
            return;
        }
        HospitalStatusUpdateDTO hospitalStatusUpdateDTO = new HospitalStatusUpdateDTO();
        hospitalStatusUpdateDTO.setRecipeCode(notice.getRecipeID());
//        hospitalStatusUpdateDTO.setClinicOrgan(LocalStringUtil.toString(notice.getOrganId()));
        hospitalStatusUpdateDTO.setOrganId(notice.getOrganizeCode());
        hospitalStatusUpdateDTO.setPlatRecipeID(notice.getPlatRecipeID());
        hospitalStatusUpdateDTO.setUpdateRecipeCodeFlag(notice.getUpdateRecipeCodeFlag());
        String recipeStatus = notice.getRecipeStatus();
        Map<String, String> otherInfo = Maps.newHashMap();
        boolean pass = true;
        //处方状态 1 处方保存 2 处方收费 3 处方发药 4处方退费 5处方退药 6处方拒绝接收 7已申请配送 8已配送
        switch (recipeStatus) {
            case HisBussConstant.FROMHIS_RECIPE_STATUS_ADD:
                if(null != notice.getCardTypeName()){
                    otherInfo.put("cardTypeName", getCardTypeName(notice.getCardTypeName()));
                }
                otherInfo.put("cardNo", notice.getCardNo());
                //自费 0 商保 1 省医保33 杭州市医保3301 衢州市医保3308 巨化医保3308A
                otherInfo.put("patientType", notice.getPatientType());
                //医院所属区域代码(结算发生地区域代码)
                otherInfo.put("areaCode", notice.getAreaCode());
                //参保地统筹区
                otherInfo.put("insuredArea", notice.getInsuredArea());
                //医保备案号
                otherInfo.put("putOnRecordID", notice.getPutOnRecordID());
                hospitalStatusUpdateDTO.setStatus(LocalStringUtil.toString(RecipeStatusConstant.CHECK_PASS));
                if (StringUtils.isEmpty(notice.getCardNo())){
                    otherInfo.put("distributionFlag", "1");
                }
                break;

            case HisBussConstant.FROMHIS_RECIPE_STATUS_PAY:
                otherInfo.put("originRecipeCode", notice.getInvoiceNo());
                otherInfo.put("patientInvoiceNo", notice.getChargeID());
                hospitalStatusUpdateDTO.setStatus(LocalStringUtil.toString(RecipeStatusConstant.HAVE_PAY));
                break;

            case HisBussConstant.FROMHIS_RECIPE_STATUS_FINISH:
                hospitalStatusUpdateDTO.setStatus(LocalStringUtil.toString(RecipeStatusConstant.FINISH));
                break;

            case HisBussConstant.FROMHIS_RECIPE_STATUS_REFUND:
                //date 20200221 更新处方推送日志
                RecipeLogService service = ApplicationUtils.getRecipeService(RecipeLogService.class);
                RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
                if(StringUtils.isNotEmpty(notice.getPlatRecipeID())){

                    RecipeBean recipe = recipeService.getByRecipeId(Integer.parseInt(notice.getPlatRecipeID()));
                    if(null != recipe){
                        service.saveRecipeLog(Integer.parseInt(notice.getPlatRecipeID()), recipe.getStatus(), recipe.getStatus(), "his进行线下退费");
                    }else{
                        LOGGER.error("线下退费当前处方id{}没有对应处方信息", notice.getPlatRecipeID());
                    }
                }else{
                    LOGGER.error("线下退费当前处方信息不足");
                }

                hospitalStatusUpdateDTO.setStatus(LocalStringUtil.toString(RecipeStatusConstant.REVOKE));
                break;

            case HisBussConstant.FROMHIS_RECIPE_STATUS_REFUND_EX:
                hospitalStatusUpdateDTO.setStatus(LocalStringUtil.toString(RecipeStatusConstant.REVOKE));
                break;

            case HisBussConstant.FROMHIS_RECIPE_STATUS_REJECT:
                otherInfo.put("distributionFlag", "1");
                hospitalStatusUpdateDTO.setStatus(LocalStringUtil.toString(RecipeStatusConstant.CHECK_PASS));
                break;
            case HisBussConstant.FROMHIS_RECIPE_STATUS_SENDING:
                otherInfo.put("trackingNo", notice.getTrackingNo());
                otherInfo.put("companyId", notice.getCompanyId());
                hospitalStatusUpdateDTO.setStatus(LocalStringUtil.toString(RecipeStatusConstant.IN_SEND));
                break;
            case HisBussConstant.FROMHIS_RECIPE_STATUS_SENDED:
                otherInfo.put("trackingNo", notice.getTrackingNo());
                otherInfo.put("companyId", notice.getCompanyId());
                hospitalStatusUpdateDTO.setStatus(LocalStringUtil.toString(RecipeStatusConstant.FINISH));
                break;

            default:
                pass = false;
                LOGGER.warn("recipeStatusFromHis notice recipeStatus warning. ");
        }

        if (pass) {
            PrescribeService prescribeService = ApplicationUtils.getRecipeService(
                    PrescribeService.class, "remotePrescribeService");
            HosRecipeResult result = prescribeService.updateRecipeStatus(hospitalStatusUpdateDTO, otherInfo);
            LOGGER.info("tag={}, result={}", MqConstant.HIS_CDRINFO_TAG_TO_PLATFORM, JSONUtils.toString(result));
        }
    }

    private String getCardTypeName(String cardTypeName) {
        switch (cardTypeName){
            case "0":
                return "身份证";
            case "1":
                return "就诊卡";
            case "2":
                return "医保卡";
            case "3":
                return "病历号";
            case "4":
                return "医保电子凭证";
            case "5":
                return "居民健康卡";
            default:
                return cardTypeName;
        }
    }
}
