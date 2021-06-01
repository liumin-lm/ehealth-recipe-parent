package recipe.dao;

import com.google.common.collect.Maps;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.DrugEntrust;
import com.ngari.recipe.entity.SyncDrugExc;
import com.ngari.recipe.recipe.model.DrugEntrustDTO;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.springframework.util.ObjectUtils;
import recipe.constant.ErrorCode;

import java.util.List;
import java.util.Map;

/**
 * 药品同步错误数据临时表dao
 *
 * @author renfuhao
 */
@RpcSupportDAO
public abstract class SyncDrugExcDAO extends HibernateSupportDelegateDAO<SyncDrugExc> {
    private static Logger logger = Logger.getLogger(SyncDrugExcDAO.class);

    public SyncDrugExcDAO() {
        super();
        this.setEntityName(SyncDrugExc.class.getName());
        this.setKeyField("id");
    }

    /**
     * 通过orgsnId获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from SyncDrugExc where organId=:organId ")
    public abstract List<SyncDrugExc> findByOrganId(@DAOParam("organId") Integer organId);

    /**
     * 通过orgsnId获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from SyncDrugExc where organId=:organId and syncType=:syncType ")
    public abstract List<SyncDrugExc> findByOrganIdAndSyncType(@DAOParam("organId") Integer organId,@DAOParam("syncType") Integer syncType);



    /**
     * 通过orgsnId删除
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = " delete from SyncDrugExc where organId=:organId and syncType=:syncType ")
    public abstract void deleteByOrganId(@DAOParam("organId") Integer organId,@DAOParam("syncType") Integer syncType );

    /**
     * 通过orgsnId 和症候名称  模糊查询
     * @param organId
     * @param input
     * @param start
     * @param limit
     * @return
     */
    public QueryResult<SyncDrugExc> querySyncDrugExcByOrganIdAndInput(Integer organId , String input,String type, Integer start, Integer limit){
        HibernateStatelessResultAction<QueryResult<SyncDrugExc>> action = new AbstractHibernateStatelessResultAction<QueryResult<SyncDrugExc>>(){

            @Override
            public void execute(StatelessSession ss) throws Exception {
                if (null == organId) {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
                }
                Map<String,Object> param = Maps.newHashMap();
                StringBuffer sql = new StringBuffer(" from SyncDrugExc d where d.organId =:organId ");
                param.put("organId",organId);
                if (!ObjectUtils.isEmpty(input)){
                    sql.append(" and ( d.organDrugCode like:name or d.drugName like:name or d.saleName like:name  or d.producer like:name )");
                    param.put("name","%"+input+"%");
                }

                if (!ObjectUtils.isEmpty(type)){
                    sql.append(" and  d.excType=:excType  ");
                    param.put("excType",type);

                }
                sql.append(" order by d.id ASC");
                Query countQuery = ss.createQuery("select count(*) "+sql.toString());
                countQuery.setProperties(param);
                Long total = (Long) countQuery.uniqueResult();
                Query query = ss.createQuery("select d "+sql.toString());
                query.setProperties(param);
                query.setFirstResult(start);
                query.setMaxResults(limit);
                List<SyncDrugExc> temps = query.list();
                setResult(new QueryResult<>(total, query.getFirstResult(), query.getMaxResults(), temps));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

}
