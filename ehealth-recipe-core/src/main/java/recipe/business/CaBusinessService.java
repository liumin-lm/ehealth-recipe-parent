package recipe.business;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.client.RevisitClient;
import recipe.core.api.doctor.ICaBusinessService;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.status.SignEnum;
import recipe.enumerate.status.WriteHisEnum;
import recipe.manager.StateManager;
import recipe.service.RecipeLogService;

import java.util.List;

/**
 * ca核心逻辑处理类
 *
 * @author liumin
 * @date 2022\3\15 0016 17:30
 */
@Service
public class CaBusinessService extends BaseService implements ICaBusinessService {
    @Autowired
    private StateManager stateManager;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private RevisitClient revisitClient;

    @Override
    public void signRecipeCAInterruptForStandard(Integer recipeId) {
        signRecipeCAInterrupt(recipeId, RecipeStatusEnum.RECIPE_STATUS_SIGN_ERROR_CODE_DOC, SignEnum.SIGN_STATE_AUDIT);
    }

    /**
     * @param recipeId
     * @param status
     * @param sign
     */
    @LogRecord
    public void signRecipeCAInterrupt(Integer recipeId, RecipeStatusEnum status, SignEnum sign) {
        //首先判断处方的装填是不是可以设置成需要重新中断的
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            logger.error("当前处方{}不存在", recipeId);
            return;
        }
        if (status.getType().equals(recipe.getStatus())) {
            return;
        }
        Recipe updateRecipe = new Recipe();
        updateRecipe.setRecipeId(recipeId);
        updateRecipe.setStatus(status.getType());
        updateRecipe.setDoctorSignState(sign.getType());
        if (RecipeStatusEnum.RECIPE_STATUS_UNSIGNED == status) {
            updateRecipe.setWriteHisState(WriteHisEnum.NONE.getType());
        }
        recipeDAO.updateNonNullFieldByPrimaryKey(updateRecipe);
        stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_SUBMIT, RecipeStateEnum.NONE);
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), status.getType(), sign.getName() + "设医生签名！");
    }


    @Override
    public void checkRecipeCAInterruptForStandard(Integer recipeId) {
        //首先判断处方的装填是不是可以设置成需要重新中断的
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            logger.error("当前处方{}不存在", recipeId);
            return;
        }
        //将处方设置成药师签名失败
        Integer beforeStatus = recipe.getStatus();
        if (!RecipeStatusEnum.RECIPE_STATUS_SIGN_NO_CODE_PHA.getType().equals(recipe.getStatus())) {
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("status", RecipeStatusEnum.RECIPE_STATUS_SIGN_ERROR_CODE_PHA.getType()));
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), beforeStatus, RecipeStatusEnum.RECIPE_STATUS_SIGN_ERROR_CODE_PHA.getType(), "签名失败，设置药师未签名！");
        }
    }

    /**
     * ca签名失败修改状态
     *
     * @param recipe
     * @param msg
     */
    public void updateSignFailState(Recipe recipe, String msg, RecipeStatusEnum status, boolean isDoctor) {
        Integer recipeId = recipe.getRecipeId();
        logger.info("CaBusinessService updateSignFailState recipeId={},msg={},status={},isDoctor={}", recipeId, msg, status, isDoctor);
        if (isDoctor) {
            stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_DOCTOR);
            stateManager.updateStatus(recipeId, status, SignEnum.SIGN_STATE_AUDIT);
            if (StringUtils.isNotEmpty(msg)) {
                RecipeExtend recipeExtend = new RecipeExtend();
                recipeExtend.setRecipeId(recipeId);
                recipeExtend.setSignFailReason(msg);
                recipeExtendDAO.updateNonNullFieldByPrimaryKey(recipeExtend);
            }
        } else {
            stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_AUDIT_NOT_PASS);
            stateManager.updateStatus(recipeId, status, null);
            stateManager.updateCheckerSignState(recipeId, SignEnum.SIGN_STATE_AUDIT);
        }
        if (Integer.valueOf(2).equals(recipe.getBussSource())) {
            List<Recipe> recipeList = recipeDAO.findTempRecipeByClinicId(recipe.getClinicOrgan(), recipe.getClinicId());
            if (CollectionUtils.isEmpty(recipeList)) {
                logger.info("failedToPrescribeFastDrug interrupt 该复诊下有暂存处方单未开方 recipeList={}", JSON.toJSONString(recipeList));
            } else {
                revisitClient.failedToPrescribeFastDrug(recipe, false);
            }
        }
    }

}

