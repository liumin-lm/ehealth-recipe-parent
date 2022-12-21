package recipe.audit.auditmode;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.service.IRevisitService;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.client.DocIndexClient;
import recipe.constant.RecipeBussConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.enumerate.status.RecipeAuditStateEnum;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.DocIndexShowEnum;
import recipe.manager.EnterpriseManager;
import recipe.manager.RecipeManager;
import recipe.manager.StateManager;
import recipe.mq.Buss2SessionProducer;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeServiceSub;
import recipe.thread.RecipeBusiThreadPool;
import recipe.thread.UpdateWaterPrintRecipePdfRunnable;

import java.util.List;

import static ctd.persistence.DAOFactory.getDAO;
import static recipe.service.afterpay.IAfterPayBussService.REVISIT_STATUS_IN;

/**
 * created by shiyuping on 2019/8/15
 * 审方前置
 */
@AuditMode(ReviewTypeConstant.Pre_AuditMode)
public class AuditPreMode extends AbstractAuditMode {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditPreMode.class);

    @Autowired
    RecipeManager recipeManager;

    @Override
    public int afterAuditRecipeChange() {
        return RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType();
    }

    @Override
    public void afterCheckPassYs(Recipe recipe) {
        LOGGER.info("AuditPreMode afterCheckPassYs recipeId:{}.", recipe.getRecipeId());
        super.afterCheckPassYs(recipe);
        RecipeDetailDAO detailDAO = getDAO(RecipeDetailDAO.class);
        Integer recipeId = recipe.getRecipeId();
        String recipeMode = recipe.getRecipeMode();
        try {
            EnterpriseManager enterpriseManager = AppContextHolder.getBean("enterpriseManager", EnterpriseManager.class);
            //药师审方后推送给前置机
            enterpriseManager.pushRecipeForThird(recipe, 0, "");
            //药师审方后推送给扁鹊流转平台
            enterpriseManager.pushRecipeInfoToBq(recipe, 0);
            //正常平台处方
            if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(recipe.getFromflag())) {
                //审核通过只有互联网发
                if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeMode)) {
                    RecipeServiceSub.sendRecipeTagToPatient(recipe, detailDAO.findByRecipeId(recipeId), null, true);
                    //向患者推送处方消息
                    RecipeMsgService.batchSendMsg(recipe, RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType());
                } else {
                    //平台前置发送审核通过消息 /向患者推送处方消息 处方通知您有一张处方单需要处理，请及时查看。
                    RecipeMsgService.batchSendMsg(recipe, RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS_YS.getType());
                }
            }
            if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource()) && recipe.getClinicId() != null) {
                IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);
                RevisitBean revisitBean = iRevisitService.getById(recipe.getClinicId());
                if (revisitBean != null && REVISIT_STATUS_IN.equals(revisitBean.getStatus())) {
                    Buss2SessionProducer.sendMsgToMq(recipe, "recipeCheckPass", revisitBean.getSessionID());
                    if (Integer.valueOf(1).equals(recipe.getFastRecipeFlag())) {
                        recipeManager.doctorJoinFastRecipeNoticeRevisit(recipe);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("afterCheckPassYs error", e);
        }
        StateManager stateManager = AppContextHolder.getBean("stateManager", StateManager.class);
        stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_ORDER, RecipeStateEnum.SUB_ORDER_READY_SUBMIT_ORDER);
        // 病历处方-状态修改成显示
        DocIndexClient docIndexClient = AppContextHolder.getBean("docIndexClient", DocIndexClient.class);
//        docIndexClient.updateStatusByBussIdBussType(recipe.getRecipeId(), DocIndexShowEnum.SHOW.getCode());
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "审核通过处理完成");
    }

    @Override
    public void afterHisCallBackChange(Integer status, Recipe recipe, String memo) {
        //处方签名中 点击撤销按钮 如果处方单状态处于已取消 则不走下面逻辑
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        Recipe currentRecipe = recipeDAO.getByRecipeId(recipe.getRecipeId());
        if (RecipeStatusEnum.RECIPE_STATUS_REVOKE.getType().equals(currentRecipe.getStatus())) {
            LOGGER.info("afterHisCallBackChange 处方单已经撤销,recipeId:{}", recipe.getRecipeId());
            return;
        }
        status = RecipeStatusEnum.RECIPE_STATUS_READY_CHECK_YS.getType();
        // 设置新的审方状态
        currentRecipe.setStatus(status);
        currentRecipe.setCheckFlag(0);
        currentRecipe.setAuditState(RecipeAuditStateEnum.PENDING_REVIEW.getType());
        recipeDAO.updateNonNullFieldByPrimaryKey(currentRecipe);
        StateManager stateManager = AppContextHolder.getBean("stateManager", StateManager.class);
        stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_AUDIT, RecipeStateEnum.SUB_AUDIT_READY_SUPPORT);
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        // 平台模式前置需要发送卡片 待审核只有平台发
        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())) {
            RecipeServiceSub.sendRecipeTagToPatient(recipe, recipeDetailList, null, true);
        }
        //日志记录
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), status, memo);
        //发送消息
        RecipeMsgService.batchSendMsg(recipe.getRecipeId(), status);
        //处方审核
        super.startRecipeAuditProcess(recipe.getRecipeId());
        //异步添加水印
        RecipeBusiThreadPool.execute(new UpdateWaterPrintRecipePdfRunnable(recipe.getRecipeId()));
    }
}
