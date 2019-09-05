package recipe.mq;

import com.google.common.collect.Maps;
import com.ngari.platform.recipe.mode.NoticeNgariRecipeInfoReq;
import com.ngari.recipe.hisprescription.model.HosRecipeResult;
import com.ngari.recipe.hisprescription.model.HospitalStatusUpdateDTO;
import ctd.net.broadcast.Observer;
import ctd.util.JSONUtils;
import eh.msg.constant.MqConstant;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.HisBussConstant;
import recipe.constant.RecipeStatusConstant;
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
        String recipeStatus = notice.getRecipeStatus();
        Map<String, String> otherInfo = Maps.newHashMap();
        boolean pass = true;
        //处方状态 1 处方保存 2 处方收费 3 处方发药 4处方退费 5处方退药 6处方拒绝接收 7已申请配送 8已配送
        switch (recipeStatus) {
            case HisBussConstant.FROMHIS_RECIPE_STATUS_ADD:
                otherInfo.put("cardTypeName", notice.getCardTypeName());
                otherInfo.put("cardNo", notice.getCardNo());
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
}
