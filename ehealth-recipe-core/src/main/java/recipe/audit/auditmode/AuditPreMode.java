package recipe.audit.auditmode;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.his.recipe.mode.NoticeHisRecipeInfoReq;
import com.ngari.patient.service.HealthCardService;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.dao.RecipeDetailDAO;
import recipe.hisservice.RecipeToHisMqService;
import recipe.hisservice.syncdata.SyncExecutorService;
import recipe.service.HisCheckRecipeService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeServiceSub;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * created by shiyuping on 2019/8/15
 * 审方前置
 */
@AuditMode(ReviewTypeConstant.Pre_AuditMode)
public class AuditPreMode extends AbstractAuidtMode {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditPreMode.class);
    @Override
    public void afterHisCallBackChange(Integer status,Recipe recipe,String memo) {
        if (status == RecipeStatusConstant.CHECK_PASS){
            //暂时去掉，没有用到
            /*//todo 判断是否是杭州市医保患者，医保患者得医保信息回传后才能设置待审核
            if (RecipeServiceSub.isMedicalPatient(recipe.getMpiid(),recipe.getClinicOrgan())){
                //医保上传确认中----三天后没回传就设置成已取消
                status = RecipeStatusConstant.CHECKING_MEDICAL_INSURANCE;
            }else {
                status = RecipeStatusConstant.READY_CHECK_YS;
            }*/
            status = RecipeStatusConstant.READY_CHECK_YS;
        }
        // 平台模式前置需要发送卡片
        //if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(recipe.getFromflag())){
        //待审核只有平台发
        if(RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())){
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            RecipeServiceSub.sendRecipeTagToPatient(recipe, detailDAO.findByRecipeId(recipe.getRecipeId()), null, true);
        }
        //}
        super.saveStatusAndSendMsg(status,recipe,memo);
        //针对his审方的模式,先在此处处理,推送消息给前置机,让前置机取轮询HIS获取审方结果
        HisCheckRecipeService hisCheckRecipeService = ApplicationUtils.getRecipeService(HisCheckRecipeService.class);
        hisCheckRecipeService.sendCheckRecipeInfo(recipe);
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
            //审核通过只有互联网发
            if(RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeMode)){
                RecipeServiceSub.sendRecipeTagToPatient(recipe, detailDAO.findByRecipeId(recipeId), null, true);
                //向患者推送处方消息
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_PASS);
                /*//同步到互联网监管平台
                SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
                syncExecutorService.uploadRecipeIndicators(recipe);*/
            }else {
                //平台前置发送审核通过消息
                //向患者推送处方消息
                //处方通知您有一张处方单需要处理，请及时查看。
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_PASS_YS);
            }
        }
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "审核通过处理完成");
    }
}
