package recipe.mq;

import com.ngari.platform.recipe.mode.NoticePlatRecipeMedicalInfoReq;
import com.ngari.recipe.entity.Recipe;
import ctd.net.broadcast.Observer;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.service.RecipeLogService;

/**
 * created by shiyuping on 2019/12/8
 * HIS处方医保上传信息回写平台
 * @author shiyuping
 */
public class RecipeMedicalInfoFromHisObserver implements Observer<NoticePlatRecipeMedicalInfoReq> {
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeStatusFromHisObserver.class);

    @Override
    public void onMessage(NoticePlatRecipeMedicalInfoReq req) {
        LOGGER.info("topic={}, tag={}, req={}", OnsConfig.hisCdrinfo, "recipeMedicalInfoFromHis"
                , JSONUtils.toString(req));
        if (null == req) {
            return;
        }
        String uploadStatus = req.getUploadStatus();
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe dbRecipe = recipeDAO.getByRecipeCode(req.getRecipeCode());
        if (null != dbRecipe) {
            //默认 医保上传确认中
            Integer status = RecipeStatusConstant.CHECKING_MEDICAL_INSURANCE;
            String memo = "";
            if ("1".equals(uploadStatus)){
                if (RecipeStatusConstant.READY_CHECK_YS != dbRecipe.getStatus()){
                    status = RecipeStatusConstant.READY_CHECK_YS;
                    memo = "His医保信息上传成功";
                }
            }else {
                //失败原因
                String failureInfo = req.getFailureInfo();
                status = RecipeStatusConstant.RECIPE_MEDICAL_FAIL;
                memo = StringUtils.isEmpty(failureInfo)?"His医保信息上传失败":"His医保信息上传失败,原因:"+failureInfo;
            }
            recipeDAO.updateRecipeInfoByRecipeId(dbRecipe.getRecipeId(), status, null);
            //日志记录
            RecipeLogService.saveRecipeLog(dbRecipe.getRecipeId(), dbRecipe.getStatus(), status, memo);
        }

    }
}
