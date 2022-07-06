package recipe.audit.auditmode;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.client.DocIndexClient;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.enumerate.status.RecipeAuditStateEnum;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.DocIndexShowEnum;
import recipe.manager.EnterpriseManager;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeServiceSub;
import recipe.thread.RecipeBusiThreadPool;
import recipe.thread.UpdateWaterPrintRecipePdfRunnable;

import java.util.List;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * created by shiyuping on 2019/8/15
 * 审方前置
 */
@AuditMode(ReviewTypeConstant.Pre_AuditMode)
public class AuditPreMode extends AbstractAuditMode {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditPreMode.class);

    @Override
    public int afterAuditRecipeChange() {
        return RecipeStatusConstant.CHECK_PASS;
    }

    @Override
    public void afterCheckPassYs(Recipe recipe) {
        LOGGER.info("AuditPreMode afterCheckPassYs recipeId:{}.", recipe.getRecipeId());
        super.afterCheckPassYs(recipe);
        RecipeDetailDAO detailDAO = getDAO(RecipeDetailDAO.class);
        Integer recipeId = recipe.getRecipeId();
        String recipeMode = recipe.getRecipeMode();
        EnterpriseManager enterpriseManager = AppContextHolder.getBean("enterpriseManager", EnterpriseManager.class);
        //药师审方后推送给前置机（扁鹊）
        enterpriseManager.pushRecipeForThird(recipe, 0, "");
        //正常平台处方
        if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(recipe.getFromflag())) {
            //审核通过只有互联网发
            if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeMode)) {
                RecipeServiceSub.sendRecipeTagToPatient(recipe, detailDAO.findByRecipeId(recipeId), null, true);
                //向患者推送处方消息
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_PASS);
            } else {
                //平台前置发送审核通过消息 /向患者推送处方消息 处方通知您有一张处方单需要处理，请及时查看。
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_PASS_YS);
            }
        }
        // 病历处方-状态修改成显示
        DocIndexClient docIndexClient = AppContextHolder.getBean("docIndexClient", DocIndexClient.class);
        docIndexClient.updateStatusByBussIdBussType(recipe.getRecipeId(), DocIndexShowEnum.SHOW.getCode());
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
        currentRecipe.setSubState(RecipeStateEnum.NONE.getType());
        currentRecipe.setProcessState(RecipeStateEnum.PROCESS_STATE_AUDIT.getType());
        currentRecipe.setAuditState(RecipeAuditStateEnum.PENDING_REVIEW.getType());
        recipeDAO.updateNonNullFieldByPrimaryKey(currentRecipe);
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
        super.recipeAudit(recipe.getRecipeId());
        //异步添加水印
        RecipeBusiThreadPool.execute(new UpdateWaterPrintRecipePdfRunnable(recipe.getRecipeId()));
    }
}
