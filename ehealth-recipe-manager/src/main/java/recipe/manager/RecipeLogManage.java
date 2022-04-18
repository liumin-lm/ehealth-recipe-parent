package recipe.manager;

import com.ngari.recipe.entity.RecipeLog;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeLogDAO;

import javax.annotation.Resource;

/**
 * 日志保存
 */
@Service
public class RecipeLogManage extends BaseManager{

    @Resource
    private RecipeLogDAO recipeLogDAO;

    /**
     * 日志保存
     * @param recipeId  处方ID
     * @param beforeStatus 之前状态
     * @param afterStatus  之后状态
     * @param memo 备注
     */
    public void saveRecipeLog(Integer recipeId, Integer beforeStatus, Integer afterStatus, String memo) {
        try {
            RecipeLog recipeLog = new RecipeLog();
            recipeLog.setRecipeId(recipeId);
            recipeLog.setModifyDate(DateTime.now().toDate());
            recipeLog.setBeforeStatus(beforeStatus);
            recipeLog.setAfterStatus(afterStatus);
            recipeLog.setMemo(memo);
            saveRecipeLog(recipeLog);
        } catch (Exception e) {
            logger.error("保存日志出错",e);
        }
    }

    /**
     * 保存处方操作记录
     *
     * @param log
     */
    public void saveRecipeLog(RecipeLog log) {
        if (null == log || null == log.getRecipeId()) {
            return;
        }
        if (null == log.getBeforeStatus()) {
            log.setBeforeStatus(RecipeStatusConstant.UNKNOW);
        }
        if (null == log.getAfterStatus()) {
            log.setAfterStatus(RecipeStatusConstant.UNKNOW);
        }
        log.setMemo(StringUtils.defaultString(log.getMemo(), ""));
        if (StringUtils.isNotEmpty(log.getMemo()) && log.getMemo().length() >250){
            log.setMemo(log.getMemo().substring(0,250));
        }
        log.setExpand(StringUtils.defaultString(log.getExpand(), ""));
        recipeLogDAO.saveRecipeLog(log);
    }
}
