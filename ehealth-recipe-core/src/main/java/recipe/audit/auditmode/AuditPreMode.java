package recipe.audit.auditmode;

import com.ngari.recipe.entity.Recipe;
import eh.cdr.constant.RecipeStatusConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.RecipeBussConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.dao.RecipeDetailDAO;
import recipe.hisservice.syncdata.SyncExecutorService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeServiceSub;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * created by shiyuping on 2019/8/15
 * 审方前置
 */
@AuditMode(ReviewTypeConstant.Preposition_Check)
public class AuditPreMode extends AbstractAuidtMode {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditPreMode.class);
    @Override
    public void afterHisCallBackChange(Integer status,Recipe recipe,String memo) {
        if (status == RecipeStatusConstant.CHECK_PASS){
            status = RecipeStatusConstant.READY_CHECK_YS;
        }
        super.saveStatusAndSendMsg(status,recipe,memo);
    }

    @Override
    public int afterAuditRecipeChange() {
        return RecipeStatusConstant.CHECK_PASS;
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
            //处方通知您有一张处方单需要处理，请及时查看。
            RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_PASS);
            if(RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeMode)){
                //同步到互联网监管平台
                SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
                syncExecutorService.uploadRecipeIndicators(recipe);
            }
        }
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "审核通过处理完成");
    }
}
