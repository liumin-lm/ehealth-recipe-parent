package recipe.dao;

import com.google.common.collect.Maps;
import com.ngari.recipe.entity.Symptom;
import com.ngari.recipe.recipe.model.SymptomDTO;
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
import recipe.constant.ErrorCode;

import java.util.List;
import java.util.Map;

/**
 * 中医症候dao
 *
 * @author renfuhao
 */
@RpcSupportDAO
public abstract class SymptomDAO extends HibernateSupportDelegateDAO<Symptom> {

    private static Logger logger = Logger.getLogger(SymptomDAO.class);

    public SymptomDAO() {
        super();
        this.setEntityName(Symptom.class.getName());
        this.setKeyField("symptomId");
    }

    /**
     * 通过orgsnId获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from Symptom where organId=:organId ",limit =0)
    public abstract List<Symptom> findByOrganId(@DAOParam("organId") Integer organId);

    /**
     *
     *
     * @param symptomCode
     * @return
     */
    @DAOMethod(sql = "update  Symptom  set symptomName=:symptomName,pinYin=:pinYin   where symptomCode=:symptomCode and organId=:organId ")
    public abstract void updateBySymptomCode(@DAOParam("symptomCode") String symptomCode,@DAOParam("pinYin") String pinYin,@DAOParam("symptomName") String symptomName,@DAOParam("organId") Integer organId );

    /**
     * 通过orgsnId和症候Id获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from Symptom where organId=:organId and symptomId=:symptomId ")
    public abstract Symptom getByOrganIdAndSymptomId(@DAOParam("organId") Integer organId ,@DAOParam("symptomId") Integer symptomId);

    /**
     * 通过orgsnId获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from Symptom where organId=:organId and symptomCode=:symptomCode ")
    public abstract Symptom getByOrganIdAndSymptomCode(@DAOParam("organId") Integer organId,@DAOParam("symptomCode") String symptomCode);


    /**
     * 通过orgsnId获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from Symptom where organId=:organId and symptomName=:symptomName ")
    public abstract Symptom getByOrganIdAndSymptomName(@DAOParam("organId") Integer organId,@DAOParam("symptomName") String symptomName);

    /**
     * 通过orgsnId 和症候名称  模糊查询
     * @param organId
     * @param input
     * @param start
     * @param limit
     * @return
     */
    public QueryResult<SymptomDTO> queryTempByTimeAndName(Integer organId , String input, final int start, final int limit){
        HibernateStatelessResultAction<QueryResult<SymptomDTO>> action = new AbstractHibernateStatelessResultAction<QueryResult<SymptomDTO>>(){

            @Override
            public void execute(StatelessSession ss) throws Exception {
                if (null == organId) {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
                }
                Map<String,Object> param = Maps.newHashMap();
                StringBuffer sql = new StringBuffer(" from Symptom where organId =:organId ");
                param.put("organId",organId);
                if (!StringUtils.isEmpty(input)){
                    sql.append(" and ( symptomName like:name or pinYin like:name ) ");
                    param.put("name","%"+input+"%");
                }
                Query countQuery = ss.createQuery("select count(*) "+sql.toString());
                countQuery.setProperties(param);
                Long total = (Long) countQuery.uniqueResult();

                sql.append(" order by createDate DESC");
                Query query = ss.createQuery(sql.toString());
                query.setProperties(param);
                query.setFirstResult(start);
                query.setMaxResults(limit);
                List<SymptomDTO> temps = query.list();

                setResult(new QueryResult<>(total, query.getFirstResult(), query.getMaxResults(), temps));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

}
