package recipe.hisservice;

import com.google.common.collect.Lists;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.HisSendResTO;
import com.ngari.recipe.recipe.model.OrderRepTO;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.process.service.IRecipeOnLineRevisitService;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.aop.LogRecord;
import recipe.bean.RecipeCheckPassResult;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeMsgEnum;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.status.WriteHisEnum;
import recipe.manager.EmrRecipeManager;
import recipe.manager.RecipeDetailManager;
import recipe.manager.StateManager;
import recipe.service.DrugsEnterpriseService;
import recipe.service.HisCallBackService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.util.LocalStringUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * @author yuyun
 * 接收处方与HIS交互回调消息
 * 写入HIS处方服务类，由于老版本HIS只会通过BASE调用接口，回调消息也只能通过BASE中转
 */
@RpcBean("recipeToHisCallbackService")
public class RecipeToHisCallbackService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeToHisCallbackService.class);
    @Autowired
    private EmrRecipeManager emrRecipeManager;
    @Autowired
    private DrugsEnterpriseService drugsEnterpriseService;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeDetailManager recipeDetailManager;
    @Autowired
    private StateManager stateManager;

    /**
     * 上海六院的模式是在患者选择完购药方式后推送处方，所以这里有调用两次
     * 一次是跳过前置机后调用保证流程正常下去，二次是真正推送处方给his之后，如果成功则不需要处理，失败需要标记
     */

    /**
     * @param response
     * @return his返回结果消息
     */
    @RpcService
    @Deprecated
    public void sendSuccess(HisSendResTO response) {
        long start = System.currentTimeMillis();
        LOGGER.info("recipeSend recive success. recipeId={}, response={}", response.getRecipeId(), JSONUtils.toString(response));
        List<OrderRepTO> repList = response.getData();
        if (CollectionUtils.isNotEmpty(repList)) {
            RecipeCheckPassResult result = new RecipeCheckPassResult();
            //以下数据取第一个数据是因为这些数据是处方相关的
            String recipeNo = repList.get(0).getRecipeNo();
            String hisOrderCode = response.getYbid();
            String patientId = repList.get(0).getPatientID();
            String amount = repList.get(0).getAmount();
            String registerId = repList.get(0).getRegisterID();
            String sendFlag = repList.get(0).getSendFlag();
            String medicalType = repList.get(0).getMedicalType();
            String medicalTypeText = repList.get(0).getMedicalTypeText();
            boolean isWuChang = false;
            //是否武昌模式
            if (StringUtils.isNotEmpty(repList.get(0).getIsDrugStock())) {
                isWuChang = true;
            }

            Recipedetail detail;
            List<Recipedetail> list = Lists.newArrayList();
            boolean isDrugStock = true;

            String pharmNo = null;
            for (OrderRepTO rep : repList) {
                detail = new Recipedetail();
                //是否有库存
                if (StringUtils.isNotEmpty(rep.getIsDrugStock())
                        && "0".equals(rep.getIsDrugStock())) {
                    isDrugStock = false;
                }
                if (StringUtils.isNotEmpty(rep.getPrice())) {
                    detail.setDrugCost(new BigDecimal(rep.getPrice()));
                }
                if (StringUtils.isNotEmpty(rep.getOrderID())) {
                    detail.setRecipeDetailId(Integer.valueOf(rep.getOrderID()));
                }
                detail.setOrderNo(LocalStringUtil.toString(rep.getOrderNo()));
                detail.setDrugGroup(LocalStringUtil.toString(rep.getSetNo()));
                //取药窗口是否都是返回同一窗口
                if (StringUtils.isNotEmpty(rep.getPharmNo())) {
                    pharmNo = LocalStringUtil.toString(rep.getPharmNo());
                }
                detail.setMemo(LocalStringUtil.toString(rep.getRemark()));

                detail.setDrugSpec(rep.getDrugSpec());
                detail.setMedicalDrugCode(rep.getMedicalDrugCode());
                detail.setPack(rep.getPack());
                list.add(detail);
            }
            result.setPharmNo(pharmNo);
            if (!StringUtils.isEmpty(amount)) {
                BigDecimal total = new BigDecimal(amount);
                result.setTotalMoney(total);
            }
            if (Objects.nonNull(response.getRecipeFee())) {
                result.setTotalMoney(response.getRecipeFee());
            }
            result.setWriteHisState(null == response.getWriteHisState() ? WriteHisEnum.WRITE_HIS_STATE_ORDER.getType() : response.getWriteHisState());
            String recipeCostNumber = StringUtils.isNotBlank(response.getRecipeCostNumber()) ? response.getRecipeCostNumber() : recipeNo;
            result.setRecipeCostNumber(recipeCostNumber);
            result.setRecipeId(Integer.valueOf(response.getRecipeId()));
            result.setRecipeCode(recipeNo);
            result.setHisOrderCode(hisOrderCode);
            result.setPatientID(patientId);
            result.setRegisterID(registerId);
            result.setMedicalType(medicalType);
            result.setMedicalTypeText(medicalTypeText);
            result.setHisDiseaseSerial(repList.get(0).getHisDiseaseSerial());
            result.setDetailList(list);
            result.setVisitMoney(response.getVisitMoney());
            result.setVisitPayFlag(response.getVisitPayFlag());
            result.setVisitCode(null == response.getVisitId() ? null : response.getVisitId().toString());
            LOGGER.info("recipeSend recive success. recipeId={}, checkPassSuccess result={}", response.getRecipeId(), JSONUtils.toString(result));
            //没库存操作----推送九州通
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(Integer.valueOf(response.getRecipeId()));
            try {
                HisCallBackService.checkPassSuccess(result, true);
                if (CollectionUtils.isNotEmpty(response.getRecipePreSettleDrugFeeDTOS())) {
                    recipeDetailManager.saveRecipeDetailBySendSuccess(response.getRecipePreSettleDrugFeeDTOS(), Integer.valueOf(response.getRecipeId()));
                }
                String memo;
                if (StringUtils.isNotEmpty(sendFlag) && "1".equals(sendFlag)) {
                    LOGGER.info("岳阳模式，不对接HIS直接推送到药企");
                    //岳阳模式，不对接HIS直接推送到药企
                    drugsEnterpriseService.pushHosInteriorSupport(recipe.getRecipeId(), recipe.getClinicOrgan());
                    memo = "岳阳处方,直接推送钥世圈";
                    //日志记录
                    RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), memo);
                    return;
                }
                if (!isDrugStock) {
                    //没库存操作----推送九州通
                    drugsEnterpriseService.pushHosInteriorSupport(recipe.getRecipeId(), recipe.getClinicOrgan());
                    //发送患者没库存消息
                    RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_HOSSUPPORT_NOINVENTORY, recipe);
                    memo = "药品没库存,推送九州通";
                    //日志记录
                    RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), memo);
                } else if (isWuChang) {
                    //有库存操作----发送患者消息
                    RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_HOSSUPPORT_INVENTORY, recipe);
                    memo = "药品有库存,发生患者取药消息";
                    //日志记录
                    RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), memo);
                }
            } catch (Exception e) {
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "推送处方失败");
                LOGGER.error("recipeSend recive error recipeId={}, data is empty. ", recipe.getRecipeId(), e);
                throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
            }
        }
        try {
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(Integer.parseInt(response.getRecipeId()));
            //将药品信息加入电子病历中
            emrRecipeManager.upDocIndex(recipeExtend.getRecipeId(), recipeExtend.getDocIndexId());
        } catch (Exception e) {
            LOGGER.error("修改电子病例使用状态失败 ", e);
        }
        long elapsedTime = System.currentTimeMillis() - start;
        LOGGER.info("RecipeToHisCallbackService sendSuccess 推送处方成功 执行时间:{}ms.", elapsedTime);
    }


    /**
     * 该处方单发送his系统失败并给医生发送推送和系统消息通知。
     *
     * @param response
     */
    @RpcService
    @LogRecord
    public void sendFail(HisSendResTO response) {
        LOGGER.error("recipeSend recive fail. recipeId={}, response={}", response.getRecipeId(), JSONUtils.toString(response));
        if (StringUtils.isEmpty(response.getRecipeId())) {
            return;
        }
        Integer recipeId = Integer.valueOf(response.getRecipeId());
        //日志记录
        RecipeLogService.saveRecipeLog(recipeId, RecipeStatusEnum.RECIPE_STATUS_CHECKING_HOS.getType(), RecipeStatusEnum.RECIPE_STATUS_HIS_FAIL.getType()
                , "HIS审核返回：写入his失败[" + response.getMsgCode() + ":|" + response.getMsg() + "]");
        Recipe updateRecipe = new Recipe();
        updateRecipe.setRecipeId(recipeId);
        updateRecipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_HIS_FAIL.getType());
        updateRecipe.setWriteHisState(WriteHisEnum.WRITE_HIS_STATE_AUDIT.getType());
        recipeDAO.updateNonNullFieldByPrimaryKey(updateRecipe);
        RecipeExtend recipeExtend = new RecipeExtend();
        recipeExtend.setRecipeId(recipeId);
        recipeExtend.setCancellation(response.getMsg());
        recipeExtendDAO.updateNonNullFieldByPrimaryKey(recipeExtend);
        stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_WRITE_HIS_NOT_ORDER);
        //发送消息
        RecipeMsgService.batchSendMsg(recipeId, RecipeStatusEnum.RECIPE_STATUS_HIS_FAIL.getType());
        //复诊开方HIS确认失败 发送环信消息
        Recipe recipe = recipeDAO.get(recipeId);
        if (null != recipe && new Integer(2).equals(recipe.getBussSource())) {
            LOGGER.info("checkPassFail 复诊开方HIS确认失败 发送环信消息 recipeId:{}", recipeId);
            IRecipeOnLineRevisitService recipeOnLineRevisitService = RevisitAPI.getService(IRecipeOnLineRevisitService.class);
            recipeOnLineRevisitService.sendRecipeDefeat(recipe.getRecipeId(), recipe.getClinicId());
        }
    }
}
