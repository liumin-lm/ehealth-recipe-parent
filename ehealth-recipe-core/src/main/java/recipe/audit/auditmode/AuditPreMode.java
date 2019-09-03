package recipe.audit.auditmode;

import com.ngari.recipe.entity.Recipe;
import eh.cdr.constant.RecipeStatusConstant;
import eh.wxpay.constant.PayConstant;
import recipe.ApplicationUtils;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeMsgEnum;
import recipe.dao.RecipeDetailDAO;
import recipe.hisservice.syncdata.SyncExecutorService;
import recipe.service.RecipeHisService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeServiceSub;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * created by shiyuping on 2019/8/15
 * 审方前置
 */
@AuditMode(RecipeBussConstant.AUDIT_PRE)
public class AuditPreMode extends AbstractAuidtMode {
    @Override
    public void afterHisCallBackChange(Integer status,Recipe recipe) {
        if (status == RecipeStatusConstant.CHECK_PASS){
            status = RecipeStatusConstant.READY_CHECK_YS;
        }
    }

    @Override
    public void afterAuditRecipeChange(Integer status) {
        status = RecipeStatusConstant.CHECK_PASS;
    }

    @Override
    public void afterCheckPassYs(Recipe recipe) {
        RecipeDetailDAO detailDAO = getDAO(RecipeDetailDAO.class);
        Integer recipeId = recipe.getRecipeId();
        String recipeMode = recipe.getRecipeMode();
        //正常平台处方
        if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(recipe.getFromflag())) {
            RecipeServiceSub.sendRecipeTagToPatient(recipe, detailDAO.findByRecipeId(recipeId), null, true);
            //向患者推送处方消息
            RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_PASS);
            if(RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeMode)){
                //同步到互联网监管平台
                SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
                syncExecutorService.uploadRecipeIndicators(recipe);
            }
        }
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "审核通过处理完成");
    }

    @Override
    public void afterCheckNotPassYs(Recipe recipe) {
        //审方前置主要处理发送消息和his状态更新------没有处方订单相关处理
        if (null == recipe) {
            return;
        }
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        String recipeMode = recipe.getRecipeMode();
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeMode)) {
            RecipeMsgService.batchSendMsg(recipe.getRecipeId(), RecipeStatusConstant.CHECK_NOT_PASSYS_PAYONLINE);
        } else {
            //根据付款方式提示不同消息
            if (RecipeBussConstant.PAYMODE_ONLINE.equals(recipe.getPayMode()) && PayConstant.PAY_FLAG_PAY_SUCCESS == recipe.getPayFlag()) {
                if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(recipe.getFromflag())) {
                    RecipeMsgService.batchSendMsg(recipe.getRecipeId(), RecipeStatusConstant.CHECK_NOT_PASSYS_PAYONLINE);
                }
            } else if (RecipeBussConstant.PAYMODE_COD.equals(recipe.getPayMode()) || RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode())) {
                //货到付款 | 药店取药
                if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(recipe.getFromflag())) {
                    RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_NOT_PASSYS_REACHPAY);
                }
            }
        }
        if (RecipeBussConstant.FROMFLAG_HIS_USE.equals(recipe.getFromflag())) {
            //发送审核不成功消息
            //${sendOrgan}：抱歉，您的处方未通过药师审核。如有收取费用，款项将为您退回，预计1-5个工作日到账。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
            RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_YS_CHECKNOTPASS_4HIS, recipe);
        }
        //HIS消息发送
        //审核不通过 往his更新状态（已取消）
        hisService.recipeStatusUpdate(recipe.getRecipeId());
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "审核不通过处理完成");
    }
}
