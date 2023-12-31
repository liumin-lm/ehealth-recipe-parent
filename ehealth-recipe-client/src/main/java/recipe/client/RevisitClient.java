package recipe.client;

import com.alibaba.fastjson.JSON;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.WriteDrugRecipeTO;
import com.ngari.his.visit.mode.WriteDrugRecipeReqTO;
import com.ngari.his.visit.service.IVisitService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.model.DrugFaileToRevisitDTO;
import com.ngari.revisit.common.model.HosRecordDTO;
import com.ngari.revisit.common.model.RevisitBussNoticeDTO;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.request.RevisitEntrustRequest;
import com.ngari.revisit.common.request.ValidRevisitRequest;
import com.ngari.revisit.common.service.*;
import com.ngari.revisit.process.service.IRecipeOnLineRevisitService;
import com.ngari.revisit.traces.service.IRevisitTracesSortService;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.RecipeBussConstant;
import recipe.enumerate.type.BussSourceTypeEnum;
import recipe.enumerate.type.FastRecipeFlagEnum;
import recipe.util.ValidateUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 复诊相关服务
 *
 * @Author liumin
 * @Date 2021/7/22 下午2:26
 * @Description
 */
@Service
public class RevisitClient extends BaseClient {

    @Autowired
    private IRevisitExService revisitExService;

    @Autowired
    private IRevisitTracesSortService revisitTracesSortService;

    @Autowired
    private IRevisitService revisitService;

    @Autowired
    private IVisitService iVisitService;

    @Autowired
    private IRevisitBusNoticeService revisitBusNoticeService;

    @Autowired
    private IRecipeOnLineRevisitService recipeOnLineRevisitService;

    @Autowired
    private IRevisitHosRecordService iRevisitHosRecordService;

    @Autowired
    private RevisitPayService revisitPayService;

    /**
     * 类加载排序
     *
     * @return
     */
    @Override
    public Integer getSort() {
        return 12;
    }

    /**
     * 根据挂号序号获取复诊信息
     *
     * @param registeredId 挂号序号
     * @return 复诊信息
     */
    public RevisitExDTO getByRegisterId(String registeredId) {
        logger.info("RevisitClient getByRegisterId param registeredId:{}", registeredId);
        try {
            RevisitExDTO consultExDTO = revisitExService.getByRegisterId(registeredId);
            logger.info("RevisitClient res consultExDTO:{} ", JSONUtils.toString(consultExDTO));
            return consultExDTO;
        } catch (Exception e) {
            logger.error("RevisitClient res consultExDTO error", e);
        }
        return null;
    }

    public RevisitBean getRevisitByClinicId(Integer clinicId) {
        logger.info("RevisitClient getRevisitByClinicId param clinicId:{}", clinicId);
        RevisitBean revisitBean = revisitService.getById(clinicId);
        logger.info("RevisitClient getRevisitByClinicId param revisitBean:{}", JSONUtils.toString(revisitBean));
        return revisitBean;
    }

    /**
     * 支付成后通知复诊
     * @param revisitEntrustRequest
     */
    public void doHandleAfterPayForEntrust(RevisitEntrustRequest revisitEntrustRequest) {
        logger.info("RevisitClient doHandleAfterPayForEntrust param revisitEntrustRequest:{}", revisitEntrustRequest);
        revisitPayService.doHandleAfterPayForEntrust(revisitEntrustRequest);
    }

    /**
     * 根据复诊单号获取复诊信息
     *
     * @param clinicId 复诊单号
     * @return 复诊信息
     */
    public RevisitExDTO getByClinicId(Integer clinicId) {
        if (ValidateUtil.integerIsEmpty(clinicId)) {
            return null;
        }
        logger.info("RevisitClient getByClinicId param clinicId:{}", clinicId);
        RevisitExDTO consultExDTO = revisitExService.getByConsultId(clinicId);
        logger.info("RevisitClient getByClinicId res consultExDTO:{} ", JSONUtils.toString(consultExDTO));
        return consultExDTO;
    }

    /**
     * 重试获取复诊
     * @param clinicId
     * @return
     */
    public RevisitExDTO retryGetByClinicId(Integer clinicId) {
        if (ValidateUtil.integerIsEmpty(clinicId)) {
            return null;
        }
        Retryer<RevisitExDTO> retry = RetryerBuilder.<RevisitExDTO>newBuilder()
                //抛出指定异常重试
                .retryIfExceptionOfType(Exception.class)
                .retryIfResult(e -> StringUtils.isEmpty(e.getPatId()))
                //停止重试策略
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                //每次等待重试时间间隔
                .withWaitStrategy(WaitStrategies.fixedWait(300, TimeUnit.MILLISECONDS))
                .build();
        try {
            logger.info("retryGetByClinicId clinicId1:{}", clinicId);
            retry.call(() -> {logger.info("retryGetByClinicId clinicId:{}", clinicId);return revisitExService.getByConsultId(clinicId);});
        } catch (Exception e) {
            return revisitExService.getByConsultId(clinicId);
        }
        return revisitExService.getByConsultId(clinicId);
    }

    /**
     * 通知复诊——删除处方追溯数据
     *
     * @param recipeId
     */
    public void deleteByBusIdAndBusNumOrder(Integer recipeId) {
        try {
            if (recipeId == null) {
                return;
            }
            logger.info("RevisitClient deleteByBusIdAndBusNumOrder request recipeId:{}", recipeId);
            revisitTracesSortService.deleteByBusIdAndBusNumOrder(recipeId + "", 10);
            logger.info("RevisitClient deleteByBusIdAndBusNumOrder response recipeId:{}", recipeId);
            //TODO  复诊的接口返回没有成功或失败 无法加标志 无法失败重试或批量处理失败数据
        } catch (Exception e) {
            logger.error("RevisitClient deleteByBusIdAndBusNumOrder error recipeId:{},{}", recipeId, e);
            e.printStackTrace();
        }
    }


    /**
     * 获取医生下同一个患者 最新 复诊的id
     *
     * @param mpiId        患者id
     * @param doctorId     医生id
     * @param isRegisterNo 是否存在挂号序号 获取存在挂号序号的复诊id
     * @return 复诊id
     */
    public Integer getRevisitIdByRegisterNo(String mpiId, Integer doctorId, Integer consultType, Boolean isRegisterNo) {
        ValidRevisitRequest revisitRequest = new ValidRevisitRequest();
        revisitRequest.setMpiId(mpiId);
        revisitRequest.setDoctorID(doctorId);
        revisitRequest.setRequestMode(consultType);
        revisitRequest.setRegisterNo(isRegisterNo);
        Integer revisitId = revisitService.findValidRevisitByMpiIdAndDoctorId(revisitRequest);
        logger.info("RevisitClient getRevisitId revisitRequest = {},revisitId={}", JSON.toJSONString(revisitRequest), revisitId);
        return revisitId;
    }

    /**
     * 获取复诊列表
     *
     * @param consultIds
     * @return
     */
    public List<RevisitBean> findByConsultIds(List<Integer> consultIds) {
        logger.info("RevisitClient findByConsultIds consultIds:{}.", JSONUtils.toString(consultIds));
        List<RevisitBean> revisitBeans = revisitService.findByConsultIds(consultIds);
        logger.info("RevisitClient findByConsultIds revisitBeans:{}.", JSONUtils.toString(revisitBeans));
        return revisitBeans;
    }

    /**
     * @param writeDrugRecipeReqTO 获取院内门诊请求入参
     * @return 院内门诊
     */
    public HisResponseTO<List<WriteDrugRecipeTO>> findWriteDrugRecipeByRevisitFromHis(WriteDrugRecipeReqTO writeDrugRecipeReqTO) {
        HisResponseTO<List<WriteDrugRecipeTO>> hisResponseTOList = new HisResponseTO<>();
        try {
            logger.info("RevisitClient findWriteDrugRecipeByRevisitFromHis writeDrugRecipeReqTO={}", JSONUtils.toString(writeDrugRecipeReqTO));
            hisResponseTOList = iVisitService.findWriteDrugRecipeByRevisitFromHis(writeDrugRecipeReqTO);
            logger.info("RevisitClient findWriteDrugRecipeByRevisitFromHis hisResponseTOList={}", JSON.toJSONString(hisResponseTOList));
        } catch (Exception e) {
            logger.error("RevisitClient findWriteDrugRecipeByRevisitFromHis error ", e);
        }
        return hisResponseTOList;
    }

    /**
     * 处方开成功回写复诊更改处方id
     *
     * @param recipeId
     * @param clinicId
     */
    public void updateRecipeIdByConsultId(Integer recipeId, Integer clinicId) {
        revisitExService.updateRecipeIdByConsultId(clinicId, recipeId);
    }

    /**
     * 用药提醒复诊
     *
     * @param revisitBussNoticeDTO
     */
    public void remindDrugRevisit(RevisitBussNoticeDTO revisitBussNoticeDTO) {
        revisitBusNoticeService.saveSendBussNotice(revisitBussNoticeDTO);
    }

    /**
     * 发送环信消息
     *
     * @param recipe
     */
    public void sendRecipeDefeat(Recipe recipe) {
        logger.info("RevisitClient sendRecipeDefeat recipe={}", JSONUtils.toString(recipe));
        if (!Integer.valueOf(2).equals(recipe.getBussSource())) {
            return;
        }
        recipeOnLineRevisitService.sendRecipeDefeat(recipe.getRecipeId(), recipe.getClinicId());
    }

    public void sendRecipeMsg(Integer consultId, Integer bussSource) {
        logger.info("RevisitClient sendRecipeMsg consultId={},bussSource={}", consultId, bussSource);
        if (!RecipeBussConstant.BUSS_SOURCE_FZ.equals(bussSource)) {
            return;
        }
        try {
            recipeOnLineRevisitService.sendRecipeMsg(consultId, 2);
        } catch (Exception e) {
            logger.error("RevisitClient sendRecipeMsg error", e);
        }
    }

    /**
     * 便捷够药药师签名完成通知复诊
     * true：签名成功，复诊状态改为已结束
     * false：签名失败，复诊状态改为已取消
     *
     * @param recipe
     * @param failFlag
     */
    public void failedToPrescribeFastDrug(Recipe recipe, boolean failFlag) {
        logger.info("RevisitClient failedToPrescribeFastDrug recipeId={}, successFlag={}, revisitId={}",
                recipe.getRecipeId(), failFlag, recipe.getClinicId());
        DrugFaileToRevisitDTO daileToRevisitDTO = new DrugFaileToRevisitDTO();
        daileToRevisitDTO.setConsultId(recipe.getClinicId());
        daileToRevisitDTO.setFailure(failFlag);
        logger.info("RevisitClient failedToPrescribeFastDrug daileToRevisitDTO={}", JSONUtils.toString(daileToRevisitDTO));
        revisitService.failedToPrescribeFastDrug(daileToRevisitDTO);
    }


    /**
     * 设置处方默认数据
     *
     * @param recipe 处方头对象
     */
    @Override
    public void setRecipe(Recipe recipe) {
        if (ValidateUtil.integerIsEmpty(recipe.getClinicId())) {
            return;
        }

        if (!BussSourceTypeEnum.BUSSSOURCE_REVISIT.getType().equals(recipe.getBussSource())) {
            return;
        }
        RevisitExDTO revisitExDTO = this.getByClinicId(recipe.getClinicId());
        if (null != revisitExDTO) {
            recipe.setPatientID(revisitExDTO.getPatId());
            recipe.setMedicalFlag(revisitExDTO.getMedicalFlag());
        }
        RevisitBean revisitBean = getRevisitByClinicId(recipe.getClinicId());
        if (null != revisitBean) {
            recipe.setFastRecipeFlag(FastRecipeFlagEnum.getFastRecipeFlag(revisitBean.getSourceTag()));
            if (Integer.valueOf(1).equals(revisitBean.getConsultSource())) {
                recipe.setRecipeSource(1);
            }
        }
    }

    /**
     * 设置处方默认数据
     *
     * @param recipe 处方头对象
     */
    @Override
    public void setRecipeExt(Recipe recipe, RecipeExtend extend) {
        if (!BussSourceTypeEnum.BUSSSOURCE_REVISIT.getType().equals(recipe.getBussSource())) {
            if (Integer.valueOf(6).equals(extend.getRecipeChooseChronicDisease())) {
                extend.setRecipeChooseChronicDisease(null);
                extend.setChronicDiseaseCode("");
                extend.setChronicDiseaseName("");
            }
            return;
        }

        RevisitExDTO revisitExDTO = this.getByClinicId(recipe.getClinicId());
        if (null != revisitExDTO) {
            extend.setCardNo(revisitExDTO.getCardId());
            extend.setCardType(revisitExDTO.getCardType());
            extend.setRegisterID(revisitExDTO.getRegisterNo());
            extend.setWeight(revisitExDTO.getWeight());
            extend.setMedicalRecordNumber(revisitExDTO.getMedicalRecordNo());
            extend.setIllnessType(revisitExDTO.getDbType());
            extend.setIllnessName(revisitExDTO.getDbTypeName());
            extend.setTerminalId(revisitExDTO.getSelfServiceMachineNo());
            extend.setCardNo(StringUtils.isNotEmpty(revisitExDTO.getCardId()) ? revisitExDTO.getCardId() : extend.getCardNo());
            //从复诊获取病种编码和名称
            if (Integer.valueOf(6).equals(extend.getRecipeChooseChronicDisease()) && "4".equals(revisitExDTO.getInsureTypeCode())) {
                extend.setChronicDiseaseCode(revisitExDTO.getMtTypeCode());
                extend.setChronicDiseaseName(revisitExDTO.getMtTypeName());
            }
        }
        HosRecordDTO hosRecord = iRevisitHosRecordService.getByConsultId(recipe.getClinicId());
        if (null != hosRecord) {
            extend.setSideCourtYardType(hosRecord.getType());
        }
        revisitExService.updateRecipeIdByConsultId(recipe.getClinicId(), recipe.getRecipeId());
    }
}

