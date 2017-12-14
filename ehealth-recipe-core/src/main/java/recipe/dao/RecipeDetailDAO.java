package recipe.dao;

import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.StatelessSession;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RpcSupportDAO
public abstract class RecipeDetailDAO extends
        HibernateSupportDelegateDAO<Recipedetail> {

    public static final Logger log = Logger.getLogger(RecipeDetailDAO.class);

    public RecipeDetailDAO() {
        super();
        this.setEntityName(Recipedetail.class.getName());
        this.setKeyField("recipeDetailId");
    }

    /**
     * 保持从his导入的处方详情数据
     */
    public void saveRecipeDetail(Recipedetail recipedetail) {
        log.info("保存服务:" + JSONUtils.toString(recipedetail));

        if (recipedetail.getRecipeId() == null) {
//			log.error("RecipeId is required!");
            throw new DAOException("RecipeId is required!");
        }
        if (recipedetail.getCreateDt() == null) {
            recipedetail.setCreateDt(new Date());
        }
        recipedetail.setLastModify(new Date());
        recipedetail.setStatus(1);

        save(recipedetail);
    }

    /**
     * 供 处方单详情服务 调用
     *
     * @param recipeId 处方序号
     * @return List<Recipedetail>
     * @author luf
     */
    @DAOMethod(sql = "from Recipedetail where recipeId=:recipeId and status=1")
    public abstract List<Recipedetail> findByRecipeId(@DAOParam("recipeId") int recipeId);

    @DAOMethod(sql = "from Recipedetail where recipeId in :recipeIds and status=1")
    public abstract List<Recipedetail> findByRecipeIds(@DAOParam("recipeIds") List<Integer> recipeIds);

    /**
     * 根据处方ID获取药品ID
     *
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "select drugId from Recipedetail where recipeId=:recipeId and status=1")
    public abstract List<Integer> findDrugIdByRecipeId(@DAOParam("recipeId") int recipeId);

    /**
     * 根据处方ID获取 药物使用总数量
     *
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "select useTotalDose from Recipedetail where recipeId=:recipeId and status=1")
    public abstract List<Double> findUseTotalDoseByRecipeId(@DAOParam("recipeId") int recipeId);

    @DAOMethod(sql = "from Recipedetail where recipeDetailId=:recipeDetailId")
    public abstract Recipedetail getByRecipeDetailId(@DAOParam("recipeDetailId") int recipeDetailId);

    /**
     * 获取处方总剂量
     *
     * @param recipeIds
     * @return
     */
    @DAOMethod(sql = "select sum(useTotalDose) from Recipedetail where recipeId in :recipeIds and status=1")
    public abstract Double getUseTotalDoseByRecipeIds(@DAOParam("recipeIds") List<Integer> recipeIds);

    /**
     * 新处方详情自定义字段 by recipeId
     *
     * @param recipeId
     * @param changeAttr
     * @return
     */
    public Boolean updateRecipeDetailByRecipeId(int recipeId, Map<String, Object> changeAttr) {
        return updateRecipeDetailByKey("recipeId", recipeId, changeAttr);
    }

    /**
     * 更新处方详情自定义字段  by detailId
     *
     * @param detailId
     * @param changeAttr
     * @return
     */
    public Boolean updateRecipeDetailByRecipeDetailId(int detailId, Map<String, Object> changeAttr) {
        return updateRecipeDetailByKey("recipeDetailId", detailId, changeAttr);
    }

    private Boolean updateRecipeDetailByKey(final String keyName, final Object keyValue, final Map<String, Object> changeAttr) {
        if (null == changeAttr || changeAttr.isEmpty()) {
            return true;
        }

        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update Recipedetail set ");
                StringBuilder keyHql = new StringBuilder();
                for (String key : changeAttr.keySet()) {
                    keyHql.append("," + key + "=:" + key);
                }
                hql.append(keyHql.toString().substring(1)).append(" where " + keyName + "=:" + keyName);
                Query q = ss.createQuery(hql.toString());

                q.setParameter(keyName, keyValue);
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

    /**
     * 通过处方明细ID获取处方明细
     *
     * @param recipeDetailId 处方明细id
     * @author xiebz
     * @date 2015-12-23 下午2:20:00
     */
    @DAOMethod
    public abstract Recipedetail getByRecipeDetailId(Integer recipeDetailId);

    /**
     * 获取处方的药物名称集合
     *
     * @param recipeId
     * @return
     */
    public String getDrugNamesByRecipeId(final int recipeId) {
        StringBuilder drugNames = new StringBuilder();
        HibernateStatelessResultAction<List<String>> action = new AbstractHibernateStatelessResultAction<List<String>>() {
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder();
                hql.append("select new java.lang.String(drugName) From Recipedetail where recipeId=:recipeId and status=1");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("recipeId", recipeId);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        List<String> drugNameList = action.getResult();
        for (String drugName : drugNameList) {
            drugNames.append("、" + drugName);
        }

        return (drugNames.length() > 0) ? drugNames.toString().substring(1) : "";
    }

    /**
     * 更新处方单详情记录为无效
     *
     * @param recipeId
     */
    @DAOMethod(sql = "update Recipedetail set status=0 where recipeId=:recipeId")
    public abstract void updateDetailInvalidByRecipeId(@DAOParam("recipeId") int recipeId);

    @DAOMethod(sql = "select count(*) from Recipedetail where recipeId=:recipeId and status=1")
    public abstract Long getCountByRecipeId(@DAOParam("recipeId") int recipeId);

}
