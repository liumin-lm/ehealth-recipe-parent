package recipe.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.RecipeLog;
import com.ngari.recipe.recipelog.model.RecipeLogBean;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeLogDAO;

import java.util.List;

/**
 * 处方状态记录
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2016/5/27.
 */
@RpcBean("recipeLogService")
public class RecipeLogService {

    private static final Log LOGGER = LogFactory.getLog(RecipeLogService.class);

    /**
     * 保存处方操作记录
     *
     * @param log
     */
    public static void saveRecipeLog(RecipeLog log) {
        if (null == log || null == log.getRecipeId()) {
            return;
        }

        RecipeLogDAO dao = DAOFactory.getDAO(RecipeLogDAO.class);

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
//        LOGGER.info("saveRecipeLog : " + JSONUtils.toString(log));
        dao.saveRecipeLog(log);
    }

    public static void saveRecipeLog(Integer recipeId, Integer beforeStatus, Integer afterStatus, String memo) {
        try {
            RecipeLog recipeLog = new RecipeLog();
            recipeLog.setRecipeId(recipeId);
            recipeLog.setModifyDate(DateTime.now().toDate());
            recipeLog.setBeforeStatus(beforeStatus);
            recipeLog.setAfterStatus(afterStatus);
            recipeLog.setMemo(memo);
            saveRecipeLog(recipeLog);
        } catch (Exception e) {
            LOGGER.error("保存日志出错",e);
        }
    }

    @RpcService
    public List<RecipeLogBean> findByRecipeId(Integer recipeId) {
        if (recipeId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "recipeId is require");
        }
        RecipeLogDAO recipeLogDAO = DAOFactory.getDAO(RecipeLogDAO.class);
        List<RecipeLog> list = recipeLogDAO.findByRecipeId(recipeId);
        return ObjectCopyUtils.convert(list, RecipeLogBean.class);
    }


}
