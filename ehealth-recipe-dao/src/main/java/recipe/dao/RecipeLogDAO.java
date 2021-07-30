package recipe.dao;

import com.ngari.recipe.entity.RecipeLog;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import java.util.List;

/**
 * 处方流程记录
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2016/4/29.
 */
@RpcSupportDAO
public abstract class RecipeLogDAO extends HibernateSupportDelegateDAO<RecipeLog> {

    private static final Log LOGGER = LogFactory.getLog(RecipeLogDAO.class);

    public RecipeLogDAO() {
        super();
        this.setEntityName(RecipeLog.class.getName());
        this.setKeyField("id");
    }

    public boolean saveRecipeLog(RecipeLog log) {
        log.setMemo(StringUtils.defaultString(log.getMemo(), ""));
        log.setExpand(StringUtils.defaultString(log.getExpand(), ""));
        log.setModifyDate(DateTime.now().toDate());
        LOGGER.info("saveRecipeLog : " + JSONUtils.toString(log));
        save(log);
        return true;
    }

    public void saveRecipeLog(Integer recipeId, Integer beforeStatus, Integer afterStatus, String memo) {
        RecipeLog recipeLog = new RecipeLog();
        recipeLog.setRecipeId(recipeId);
        recipeLog.setModifyDate(DateTime.now().toDate());
        recipeLog.setBeforeStatus(beforeStatus);
        recipeLog.setAfterStatus(afterStatus);
        recipeLog.setMemo(memo);
        saveRecipeLog(recipeLog);
    }

    /**
     * 根据处方id查询
     *
     * @param recipeId
     * @return
     */
    @DAOMethod(orderBy = " id asc")
    public abstract List<RecipeLog> findByRecipeId(Integer recipeId);

    /**
     * 根据处方id和后状态查询
     *
     * @param recipeId
     * @return
     */
    @DAOMethod(orderBy = " id asc")
    public abstract List<RecipeLog> findByRecipeIdAndAfterStatus(Integer recipeId, Integer afterStatus);

    /**
     * 根据处方id和后状态查询
     *
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "from RecipeLog where recipeId =:recipeId and afterStatus =:afterStatus order by id desc")
    public abstract List<RecipeLog> findByRecipeIdAndAfterStatusDesc(@DAOParam("recipeId") Integer recipeId, @DAOParam("afterStatus") Integer afterStatus);

}
