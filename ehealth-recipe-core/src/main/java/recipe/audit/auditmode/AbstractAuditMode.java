package recipe.audit.auditmode;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.service.IRevisitService;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import eh.cdr.constant.RecipeStatusConstant;
import eh.wxpay.constant.PayConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.audit.IAuditMode;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.RecipeAuditClient;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeSystemConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.enumerate.status.RecipeAuditStateEnum;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.SignImageTypeEnum;
import recipe.manager.StateManager;
import recipe.mq.Buss2SessionProducer;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeServiceSub;
import recipe.thread.RecipeBusiThreadPool;
import recipe.thread.UpdateWaterPrintRecipePdfRunnable;
import recipe.util.MapValueUtil;

import java.util.List;
import java.util.Map;

import static ctd.persistence.DAOFactory.getDAO;
import static recipe.service.afterpay.IAfterPayBussService.REVISIT_STATUS_IN;

/**
 * created by shiyuping on 2019/9/3
 */
public abstract class AbstractAuditMode implements IAuditMode {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAuditMode.class);

    @Override
    public void afterHisCallBackChange(Integer status, Recipe recipe, String memo) {
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        //发送卡片
        RecipeServiceSub.sendRecipeTagToPatient(recipe, detailDAO.findByRecipeId(recipe.getRecipeId()), null, true);
        saveStatusAndSendMsg(recipe, memo);
        //todo 已经在回调接口中的代码没必要异步
        //异步添加水印
        RecipeBusiThreadPool.execute(new UpdateWaterPrintRecipePdfRunnable(recipe.getRecipeId()));
    }

    private void saveStatusAndSendMsg(Recipe recipe, String memo) {
        //生成文件成功后再去更新处方状态
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe currentRecipe = recipeDAO.getByRecipeId(recipe.getRecipeId());
        //处方签名中 点击撤销按钮 如果处方单状态处于已取消 则不走下面逻辑
        if (RecipeStatusEnum.RECIPE_STATUS_REVOKE.getType().equals(currentRecipe.getStatus())) {
            LOGGER.info("saveStatusAndSendMsg 处方单已经撤销,recipeId:{}", recipe.getRecipeId());
            return;
        }
        currentRecipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType());
        recipeDAO.updateNonNullFieldByPrimaryKey(currentRecipe);
        StateManager stateManager = AppContextHolder.getBean("stateManager", StateManager.class);
        stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_ORDER, RecipeStateEnum.SUB_ORDER_READY_SUBMIT_ORDER);
        //日志记录
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), currentRecipe.getStatus(), memo);
        //发送消息--待审核或者待处理消息
        RecipeMsgService.batchSendMsg(recipe.getRecipeId(), RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType());
        //处方审核
        startRecipeAuditProcess(recipe.getRecipeId());
    }

    @Override
    public int afterAuditRecipeChange() {
        return RecipeStatusConstant.CHECK_PASS_YS;
    }

    @Override
    public void afterCheckPassYs(Recipe recipe) {
        LOGGER.info("AbstractAuditMode afterCheckPassYs recipe:{}", JSON.toJSONString(recipe));
        try {
            //药师审核通过后，重新根据药师的pdf生成签名图片
            CreatePdfFactory createPdfFactory = AppContextHolder.getBean("createPdfFactory", CreatePdfFactory.class);
            createPdfFactory.updatePdfToImg(recipe.getRecipeId(), SignImageTypeEnum.SIGN_IMAGE_TYPE_CHEMIST.getType());
        } catch (Exception e) {
            LOGGER.error("AbstractAuditMode afterCheckPassYs error", e);
        }
    }

    @Override
    public void afterCheckNotPassYs(Recipe recipe) {
        LOGGER.info("AbstractAuditMode afterCheckPassYs recipeId :{}", recipe.getRecipeId());
        StateManager stateManager = AppContextHolder.getBean("stateManager", StateManager.class);
        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode()) && RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource()) && recipe.getClinicId() != null) {
            IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);
            RevisitBean revisitBean = iRevisitService.getById(recipe.getClinicId());
            if (revisitBean != null && REVISIT_STATUS_IN.equals(revisitBean.getStatus())) {
                Buss2SessionProducer.sendMsgToMq(recipe, "recipeCheckNotPass", revisitBean.getSessionID());
            }
        }
        stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_AUDIT_NOT_PASS);
    }

    @Override
    public void afterPayChange(Boolean saveFlag, Recipe recipe, RecipeResultBean result, Map<String, Object> attrMap) {
        //默认待处理
        Integer status = RecipeStatusConstant.CHECK_PASS;
        Integer giveMode = null == MapValueUtil.getInteger(attrMap, "giveMode") ? recipe.getGiveMode() : MapValueUtil.getInteger(attrMap, "giveMode");
        Integer payFlag = MapValueUtil.getInteger(attrMap, "payFlag");
        // 获取paymode
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder byOrderCode = orderDAO.getByOrderCode(recipe.getOrderCode());
        Integer payMode = byOrderCode.getPayMode();
        //根据传入的方式来处理, 因为供应商列表，钥世圈提供的有可能是多种方式都支持，当时这2个值是保存为null的
        if (saveFlag) {
            attrMap.put("chooseFlag", 1);
            String memo = "";
            if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)) {
                if (RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
                    //线上支付
                    if (new Integer(PayConstant.PAY_FLAG_PAY_SUCCESS).equals(payFlag)) {
                        //配送到家-线上支付
                        memo = "配送到家-线上支付成功";
                    } else {
                        memo = "配送到家-线上支付失败";
                    }
                } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode)) {
                    memo = "货到付款-待配送";
                }
            } else if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode)) {
                memo = "药店取药-待取药";
            }
            //记录日志
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), RecipeStatusConstant.CHECK_PASS, status, memo);
        } else {
            attrMap.put("chooseFlag", 0);
            if (RecipeBussConstant.FROMFLAG_HIS_USE.equals(recipe.getFromflag())) {
                status = recipe.getStatus();
            }
        }
        updateRecipeInfoByRecipeId(recipe.getRecipeId(), status, attrMap, result);
        LOGGER.info("AbstractAuditMode.afterPayChange saveFlag:{}, payFlag:{}.", saveFlag, payFlag);
        if (saveFlag && new Integer(PayConstant.PAY_FLAG_PAY_SUCCESS).equals(payFlag)) {
            //处方推送到药企
            RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            remoteDrugEnterpriseService.pushSingleRecipeInfo(recipe.getRecipeId());
        }

    }

    protected void updateRecipeInfoByRecipeId(Integer recipeId, Integer status, Map<String, Object> attrMap, RecipeResultBean result) {
        try {
            RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
            boolean flag = recipeDAO.updateRecipeInfoByRecipeId(recipeId, status, attrMap);
            if (flag) {
                result.setMsg(RecipeSystemConstant.SUCCESS);
            } else {
                result.setCode(RecipeResultBean.FAIL);
                result.setError("更新处方失败");
            }
        } catch (Exception e) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("更新处方失败，" + e.getMessage());
            LOGGER.error("更新处方失败", e);
        }
    }

    /**
     * 处方审核
     * @param recipeId
     */
    protected void startRecipeAuditProcess(Integer recipeId){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        RecipeAuditClient recipeAuditClient = AppContextHolder.getBean("recipeAuditClient", RecipeAuditClient.class);
        recipeAuditClient.startRecipeAuditProcess(recipe, recipeExtend, recipeDetailList);
    }

    protected void setAuditStateToPendingReview(Recipe recipe,Integer status) {
        if (status == RecipeStatusEnum.RECIPE_STATUS_READY_CHECK_YS.getType()) {
            StateManager stateManager = AppContextHolder.getBean("stateManager", StateManager.class);
            stateManager.updateAuditState(recipe.getRecipeId(), RecipeAuditStateEnum.PENDING_REVIEW);
            stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_AUDIT, RecipeStateEnum.SUB_AUDIT_READY_SUPPORT);
        }
    }
}
