package recipe.dao;

import com.google.common.collect.Maps;
import com.ngari.recipe.entity.TcmTreatment;
import com.ngari.recipe.recipe.model.TcmTreatmentDTO;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.springframework.util.ObjectUtils;
import recipe.constant.ErrorCode;

import java.util.List;
import java.util.Map;

/**
 * 中医治法dao
 *
 * @author renfuhao
 */
@RpcSupportDAO
public abstract class TcmTreatmentDAO extends HibernateSupportDelegateDAO<TcmTreatment> {

    private static Logger logger = Logger.getLogger(TcmTreatmentDAO.class);

    public TcmTreatmentDAO() {
        super();
        this.setEntityName(TcmTreatment.class.getName());
        this.setKeyField("id");
    }


    /**
     * 通过orgsnId获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from TcmTreatment where organId=:organId ", limit = 0)
    public abstract List<TcmTreatment> findByOrganId(@DAOParam("organId") Integer organId);


    /**
     * 根据机构id删除
     *
     * @param organId
     */
    @DAOMethod(sql = " delete from TcmTreatment where organId =:organId")
    public abstract void deleteByOrganId(@DAOParam("organId") Integer organId);

    /**
     * 根据机构id查询未关联监管数量
     *
     * @param organId
     */
    @DAOMethod(sql = " select count(*) from TcmTreatment where organId =:organId and ( regulationTreatmentCode is null or regulationTreatmentCode='')  ")
    public abstract Long getCountByOrganId(@DAOParam("organId") Integer organId);


    /**
     * 通过orgsnId获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from TcmTreatment where organId=:organId and treatmentCode=:treatmentCode ")
    public abstract TcmTreatment getByOrganIdAndTreatmentCode(@DAOParam("organId") Integer organId, @DAOParam("treatmentCode") String treatmentCode);


    /**
     * 通过orgsnId获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from TcmTreatment where organId=:organId and treatmentName=:treatmentName ")
    public abstract TcmTreatment getByOrganIdAndTreatmentName(@DAOParam("organId") Integer organId, @DAOParam("treatmentName") String treatmentName);


    /**
     * 通过orgsnId获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from TcmTreatment where organId=:organId and treatmentName=:treatmentName and treatmentCode=:treatmentCode ")
    public abstract TcmTreatment getByOrganIdAndTreatmentNameAndTreatmentCode(@DAOParam("organId") Integer organId, @DAOParam("treatmentName") String treatmentName, @DAOParam("treatmentCode") String treatmentCode);

    /**
     * 通过orgsnId 和症候名称  模糊查询
     *
     * @param organId
     * @param input
     * @param start
     * @param limit
     * @return
     */
    public QueryResult<TcmTreatmentDTO> queryTempByTimeAndName(Integer organId, String input, Boolean isRegulation, final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<TcmTreatmentDTO>> action = new AbstractHibernateStatelessResultAction<QueryResult<TcmTreatmentDTO>>() {

            @Override
            public void execute(StatelessSession ss) throws Exception {
                if (null == organId) {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
                }
                Map<String, Object> param = Maps.newHashMap();
                StringBuffer sql = new StringBuffer(" from TcmTreatment where organId =:organId ");
                param.put("organId", organId);
                if (!StringUtils.isEmpty(input)) {
                    sql.append(" and  treatmentName like:name  ");
                    param.put("name", "%" + input + "%");
                }
                if (!ObjectUtils.isEmpty(isRegulation)) {
                    if (isRegulation) {
                        sql.append(" and regulationTreatmentCode is not null and regulationTreatmentCode <> '' ");
                    } else {
                        sql.append(" and ( regulationTreatmentCode is  null or regulationTreatmentCode='' )");
                    }
                }
                Query countQuery = ss.createQuery("select count(*) " + sql.toString());
                countQuery.setProperties(param);
                Long total = (Long) countQuery.uniqueResult();

                sql.append(" order by createDate DESC");
                Query query = ss.createQuery(sql.toString());
                query.setProperties(param);
                query.setFirstResult(start);
                query.setMaxResults(limit);
                List<TcmTreatmentDTO> temps = query.list();

                setResult(new QueryResult<>(total, query.getFirstResult(), query.getMaxResults(), temps));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


}
