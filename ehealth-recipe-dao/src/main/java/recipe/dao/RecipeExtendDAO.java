package recipe.dao;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.RecipeExtend;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.springframework.util.ObjectUtils;
import recipe.dao.comment.ExtendDao;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 处方扩展表
 * Created by yuzq on 2019/3/1.
 */
@RpcSupportDAO
public abstract class RecipeExtendDAO extends HibernateSupportDelegateDAO<RecipeExtend> implements ExtendDao<RecipeExtend> {

    private static final Log LOGGER = LogFactory.getLog(RecipeExtendDAO.class);

    public RecipeExtendDAO() {
        super();
        setEntityName(RecipeExtend.class.getName());
        setKeyField("recipeId");
    }

    @Override
    public boolean updateNonNullFieldByPrimaryKey(RecipeExtend recipeExtend) {
        return updateNonNullFieldByPrimaryKey(recipeExtend, "recipeId");
    }

    /**
     * 根据id获取
     *
     * @param recipeId
     * @return
     */
    @DAOMethod
    public abstract RecipeExtend getByRecipeId(int recipeId);


    public void saveRecipeExtend(RecipeExtend recipeExtend) {
        LOGGER.info("处方扩展表保存：" + JSONUtils.toString(recipeExtend));
        if (recipeExtend.getCanUrgentAuditRecipe() == null) {
            recipeExtend.setCanUrgentAuditRecipe(0);
        }
        super.save(recipeExtend);
    }


    /**
     * 保存OR更新
     * @param recipeExtend
     */
    public void saveOrUpdateRecipeExtend(RecipeExtend recipeExtend) {
        LOGGER.info("RecipeExtendDAO saveOrUpdateRecipeExtend recipeExtend：" + JSON.toJSONString(recipeExtend));
        if(null == recipeExtend.getRecipeId()){
            return;
        }
        if (recipeExtend.getCanUrgentAuditRecipe() == null) {
            recipeExtend.setCanUrgentAuditRecipe(0);
        }
        if (recipeExtend.getAppointEnterpriseType() == null) {
            recipeExtend.setAppointEnterpriseType(0);
        }
        if (ObjectUtils.isEmpty(getByRecipeId(recipeExtend.getRecipeId()))) {
            save(recipeExtend);
        } else {
            update(recipeExtend);
        }
    }

    /**
     * 更新处方自定义字段
     *
     * @param recipeId
     * @param changeAttr
     * @return
     */
    public Boolean updateRecipeExInfoByRecipeId(final int recipeId, final Map<String, ?> changeAttr) {
        if (null == changeAttr || changeAttr.isEmpty()) {
            return true;
        }

        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update RecipeExtend set ");
                StringBuilder keyHql = new StringBuilder();
                for (String key : changeAttr.keySet()) {
                    keyHql.append("," + key + "=:" + key);
                }
                hql.append(keyHql.toString().substring(1)).append(" where recipeId=:recipeId");
                Query q = ss.createQuery(hql.toString());

                q.setParameter("recipeId", recipeId);
                for (String key : changeAttr.keySet()) {
                    q.setParameter(key, changeAttr.get(key));
                }

                int flag = q.executeUpdate();
                setResult(flag == 1);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /*
     * @description 根据天猫回传处方编码获取处方id集合
     * @author gmw
     * @date 2019/9/23
     * @param rxNo
     * @return List<Integer>
     */
    @DAOMethod(sql = "select recipeId from RecipeExtend where rxNo=:rxNo")
    public abstract List<Integer> findRecipeIdsByRxNo(@DAOParam("rxNo") String rxNo);


    /**
     * 删除 电子病例处方关联
     *
     * @param docIndexId
     */
    @DAOMethod(sql = "update RecipeExtend set docIndexId=0 where docIndexId=:docIndexId")
    public abstract void updateDocIndexId(@DAOParam("docIndexId") int docIndexId);

    /**
     * 根据处方id批量删除
     *
     * @param recipeIds
     */
    @DAOMethod(sql = "delete from RecipeExtend where recipeId in (:recipeIds)")
    public abstract void deleteByRecipeIds(@DAOParam("recipeIds") List<Integer> recipeIds);


    /**
     * 根据处方id批量查询
     *
     * @param recipeIds
     */
    @DAOMethod(sql = "from RecipeExtend where recipeId in (:recipeIds)",limit = 0)
    public abstract List<RecipeExtend> queryRecipeExtendByRecipeIds(@DAOParam("recipeIds") List<Integer> recipeIds);


    /**
     * 新处方详情自定义字段 by recipeIds
     *
     * @param recipeIds
     * @param changeAttr
     * @return
     */
    public Boolean updateRecipeExtByRecipeIdS(List<Integer> recipeIds, Map<String, Object> changeAttr) {
        return updateRecipeExtByKeyS("recipeId", recipeIds, changeAttr);
    }

    private Boolean updateRecipeExtByKeyS(final String keyName, final Object keyValue, final Map<String, Object> changeAttr) {
        if (null == changeAttr || changeAttr.isEmpty()) {
            return true;
        }

        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update RecipeExtend set ");
                StringBuilder keyHql = new StringBuilder();
                for (String key : changeAttr.keySet()) {
                    keyHql.append("," + key + "=:" + key);
                }
                hql.append(keyHql.toString().substring(1)).append(" where " + keyName + " in (:" + keyName + ")");
                Query q = ss.createQuery(hql.toString());

                q.setParameterList(keyName, (List<Object>)keyValue);
                Iterator<Map.Entry<String, Object>> it = changeAttr.entrySet().iterator();
                while (it.hasNext()){
                    Map.Entry<String, Object> m = it.next();
                    q.setParameter(m.getKey(), m.getValue());
                }

                int flag = q.executeUpdate();
                setResult(flag == 1);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

}
