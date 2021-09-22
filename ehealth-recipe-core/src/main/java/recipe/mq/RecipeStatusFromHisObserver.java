package recipe.mq;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.ngari.platform.recipe.mode.NoticeNgariRecipeInfoReq;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.hisprescription.model.HosRecipeResult;
import com.ngari.recipe.hisprescription.model.HospitalStatusUpdateDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.net.broadcast.Observer;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import eh.msg.constant.MqConstant;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.common.OnsConfig;
import recipe.constant.HisBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.manager.EmrRecipeManager;
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
        LOGGER.info("RecipeStatusFromHisObserver onMessage topic={}, tag={}, notice={}", OnsConfig.hisCdrinfo, MqConstant.HIS_CDRINFO_TAG_TO_PLATFORM
                , JSONUtils.toString(notice));
        if (null == notice) {
            return;
        }
        HospitalStatusUpdateDTO hospitalStatusUpdateDTO = new HospitalStatusUpdateDTO();
        hospitalStatusUpdateDTO.setRecipeCode(notice.getRecipeID());
        hospitalStatusUpdateDTO.setClinicOrgan(LocalStringUtil.toString(notice.getOrganId()));
        hospitalStatusUpdateDTO.setOrganId(notice.getOrganizeCode());
        hospitalStatusUpdateDTO.setPlatRecipeID(notice.getPlatRecipeID());
        hospitalStatusUpdateDTO.setUpdateRecipeCodeFlag(notice.getUpdateRecipeCodeFlag());
        String recipeStatus = notice.getRecipeStatus();
        Map<String, String> otherInfo = Maps.newHashMap();
        boolean pass = true;
        //处方状态 1 处方保存 2 处方收费 3 处方发药 4处方退费 5处方退药 6处方拒绝接收 7已申请配送 8已配送
        switch (recipeStatus) {
            case HisBussConstant.FROMHIS_RECIPE_STATUS_ADD:
                //TODO liu
                //设置健康卡
                try {
                    RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
                    Recipe recipe = recipeDAO.getByRecipeId(Integer.parseInt(notice.getPlatRecipeID()));
                    if (recipe!=null&&recipe.getClinicId() != null) {
                        IRevisitExService iRevisitExService = RevisitAPI.getService(IRevisitExService.class);
                        RevisitExDTO consultExDTO = iRevisitExService.getByConsultId(recipe.getClinicId());
                        LOGGER.info("recipeId:{},consultExDTO:{}",notice.getPlatRecipeID(),JSONUtils.toString(consultExDTO));
                        if (consultExDTO != null) {
                            otherInfo.put("cardNo", consultExDTO.getCardId());
                            otherInfo.put("cardType", consultExDTO.getCardType());
                            try {
                                otherInfo.put("cardTypeName", DictionaryController.instance().get("eh.mpi.dictionary.CardType").getText(consultExDTO.getCardType()));
                            } catch (ControllerException e) {
                                LOGGER.error("recipeId:{},DictionaryController 字典转化异常,{}",notice.getPlatRecipeID(), e);
                            }
                        }
                    }
                }catch (Exception e){
                    LOGGER.error("recipeId:{},onMessage His回调异常,{}",notice.getPlatRecipeID(), e);
                }
                //设置卡
                if(null != notice.getCardTypeName()){
                    otherInfo.put("cardTypeName", getCardTypeName(notice.getCardTypeName()));
                }
                if(null != notice.getCardNo()){
                    otherInfo.put("cardNo", notice.getCardNo());
                }
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
                RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
                if(StringUtils.isNotEmpty(notice.getPlatRecipeID())){
                    RecipeBean recipe = recipeService.getByRecipeId(Integer.parseInt(notice.getPlatRecipeID()));
                    if(null != recipe){
                        RecipeLogService.saveRecipeLog(Integer.parseInt(notice.getPlatRecipeID()), recipe.getStatus(), recipe.getStatus(), "his进行线下退费");
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
        try {
            if (StringUtils.isEmpty(notice.getPlatRecipeID())) {
                LOGGER.info("RecipeStatusFromHisObserver notice={}", JSON.toJSONString(notice));
                return;
            }
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(Integer.parseInt(notice.getPlatRecipeID()));
            EmrRecipeManager emrRecipeManager = AppContextHolder.getBean("emrRecipeManager", EmrRecipeManager.class);
            //将药品信息加入病历中
            emrRecipeManager.upDocIndex(recipeExtend.getRecipeId(), recipeExtend.getDocIndexId());
        } catch (Exception e) {
            LOGGER.error("修改电子病例使用状态失败 ", e);
        }
        LOGGER.info("RecipeStatusFromHisObserver onMessage otherInfo={}", JSON.toJSONString(otherInfo));
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
            case "7":
                return "杭州健康卡";
            default:
                return cardTypeName;
        }
    }
}
