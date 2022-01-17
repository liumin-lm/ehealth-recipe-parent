package recipe.dao;

import com.ngari.recipe.drugsenterprise.model.DrugEnterpriseLogisticsBean;
import com.ngari.recipe.entity.DrugEnterpriseLogistics;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.BeanUtils;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import recipe.dao.comment.ExtendDao;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author yuyun
 */
@RpcSupportDAO
public abstract class RecipeDetailDAO extends
        HibernateSupportDelegateDAO<Recipedetail> implements ExtendDao<Recipedetail> {

    public static final Logger LOGGER = Logger.getLogger(RecipeDetailDAO.class);

    public RecipeDetailDAO() {
        super();
        this.setEntityName(Recipedetail.class.getName());
        this.setKeyField("recipeDetailId");
    }

    @Override
    public boolean updateNonNullFieldByPrimaryKey(Recipedetail recipedetail) {
        return updateNonNullFieldByPrimaryKey(recipedetail, "recipeDetailId");
    }

    /**
     * 供 处方单详情服务 调用
     *
     * @param recipeId 处方序号
     * @return List<Recipedetail>
     * @author luf
     */
    @DAOMethod(sql = "from Recipedetail where recipeId=:recipeId and status=1", limit = 0)
    public abstract List<Recipedetail> findByRecipeId(@DAOParam("recipeId") int recipeId);

    /**
     * 根据处方id集合查询
     *
     * @param recipeIds
     * @return
     */
    @DAOMethod(sql = "from Recipedetail where recipeId in :recipeIds and status=1", limit = 0)
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
     * 新处方详情自定义字段 by recipeIds
     *
     * @param recipeIds
     * @param changeAttr
     * @return
     */
    public Boolean updateRecipeDetailByRecipeIdS(List<Integer> recipeIds, Map<String, Object> changeAttr) {
        return updateRecipeDetailByKeyS("recipeId", recipeIds, changeAttr);
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
                Iterator<Map.Entry<String, Object>> it = changeAttr.entrySet().iterator();
                while (it.hasNext()) {
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

    private Boolean updateRecipeDetailByKeyS(final String keyName, final Object keyValue, final Map<String, Object> changeAttr) {
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
                hql.append(keyHql.toString().substring(1)).append(" where " + keyName + " in (:" + keyName + ")");
                Query q = ss.createQuery(hql.toString());

                q.setParameterList(keyName, (List<Object>) keyValue);
                Iterator<Map.Entry<String, Object>> it = changeAttr.entrySet().iterator();
                while (it.hasNext()) {
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


    /**
     * 通过处方明细ID获取处方明细
     *
     * @param recipeDetailId
     * @return
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
        HibernateSessionTemplate.instance().execute(action);
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

    /**
     * 根据id及状态查询数量
     *
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "select count(*) from Recipedetail where recipeId=:recipeId and status=1")
    public abstract Long getCountByRecipeId(@DAOParam("recipeId") int recipeId);

    /**
     * 根据id删除无用的处方单关联的详情
     *
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "delete from Recipedetail where recipeId =:recipeId")
    public abstract void deleteByRecipeId(@DAOParam("recipeId") int recipeId);

    /**
     * 根据处方id批量删除
     *
     * @param recipeIds
     */
    @DAOMethod(sql = "delete from Recipedetail where recipeId in (:recipeIds)")
    public abstract void deleteByRecipeIds(@DAOParam("recipeIds") List<Integer> recipeIds);

    /**
     * 根据机构id和HIS结算单据号查询对应处方id
     *
     * @return
     */
    public Integer getRecipeIdByOrganIdAndInvoiceNo(final int organId, final String invoiceNo) {
        StringBuilder drugNames = new StringBuilder();
        HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder();
                hql.append("select cd.* From cdr_recipedetail cd left join cdr_recipe cr on cd.RecipeID = cr.RecipeID " +
                        "where cr.clinicOrgan =:organId and cd.status=1 and cd.invoiceNo = :invoiceNo");
                Query q = ss.createSQLQuery(hql.toString()).addEntity(Recipedetail.class);
                q.setParameter("organId", organId);
                q.setParameter("invoiceNo", invoiceNo);
                List<Recipedetail> list = q.list();
                Integer recipeId = null;
                if (null != list && 0 < list.size()) {
                    recipeId = list.get(0).getRecipeId();
                }
                setResult(recipeId);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 供 处方单详情服务 调用
     *
     * @param recipeIds 处方序号
     * @return List<Recipedetail>
     * @author luf
     */
    @DAOMethod(sql = "from Recipedetail where recipeId in (:recipeIds) and status=1", limit = 0)
    public abstract List<Recipedetail> findByRecipeIdList(@DAOParam("recipeIds") List<Integer> recipeIds);

    /**
     * 根据订单查询处方 详情
     *
     * @param orderCode
     * @return
     */
    @DAOMethod(sql = "from Recipedetail WHERE RecipeID IN ( SELECT recipeId FROM Recipe WHERE OrderCode = :orderCode ) and status=1 ")
    public abstract List<Recipedetail> findDetailByOrderCode(@DAOParam("orderCode") String orderCode);

    /**
     * 批量写入
     *
     * @param recipedetails
     * @return
     */
    public Boolean updateAllRecipeDetail(List<Recipedetail> recipedetails) {
        if (CollectionUtils.isEmpty(recipedetails)) {
            return true;
        }

        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                Transaction transaction = ss.beginTransaction();
                recipedetails.forEach(recipedetail -> {
                    ss.update(recipedetail);

                });
                transaction.commit();
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }
}
