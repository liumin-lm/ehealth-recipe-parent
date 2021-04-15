package recipe.dao;

import com.google.common.collect.Maps;
import com.ngari.recipe.drug.model.DecoctionWayBean;
import com.ngari.recipe.drug.model.DrugMakingMethodBean;
import com.ngari.recipe.entity.DrugMakingMethod;
import ctd.dictionary.DictionaryItem;
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
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RpcSupportDAO
public abstract class DrugMakingMethodDao extends HibernateSupportDelegateDAO<DrugMakingMethod> {
    public static final Logger log = LoggerFactory.getLogger(DrugMakingMethod.class);
    public DrugMakingMethodDao() {
        super();
        this.setEntityName(DrugMakingMethod.class.getName());
        this.setKeyField("methodId");
    }

    @DAOMethod(sql = "from DrugMakingMethod where organId =:organId order by sort", limit = 0)
    public abstract List<DrugMakingMethodBean> findAllDrugMakingMethodByOrganId(@DAOParam("organId")Integer organId);

    @DAOMethod(sql = "from DrugMakingMethod where organId =:organId and methodCode = :methodCode")
    public abstract DrugMakingMethod getDrugMakingMethodByOrganIdAndCode(@DAOParam("organId")Integer organId,
                                                                    @DAOParam("methodCode")String methodCode);

    @DAOMethod(sql = "from DrugMakingMethod where organId =:organId and methodText = :methodText")
    public abstract DrugMakingMethod getDrugMakingMethodByOrganIdAndText(@DAOParam("organId")Integer organId,
                                                                    @DAOParam("methodText")String methodText);

    @DAOMethod(sql = "delete from DrugMakingMethod where methodId =:methodId ")
    public abstract void deleteDrugMakingMethodByMethodId(@DAOParam("methodId")Integer methodId);

    public QueryResult<DrugMakingMethodBean> findDrugMakingMethodByOrganIdAndName(Integer organId, String methodText, Integer start, Integer limit) {
        HibernateStatelessResultAction<QueryResult<DrugMakingMethodBean>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugMakingMethodBean>>() {

            @Override public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder("from DrugMakingMethod where 1=1");
                if (organId != null) {
                    hql.append(" and organId =:organId");
                }
                if (!StringUtils.isEmpty(methodText)) {
                    hql.append(" and methodText like :methodText");
                }
                hql.append(" order by sort");
                Query query = ss.createQuery(hql.toString());
                if (organId != null) {
                    query.setParameter("organId", organId);
                }
                if (!StringUtils.isEmpty(methodText)) {
                    query.setParameter("methodText", "%" + methodText + "%");
                }
                query.setFirstResult(start);
                query.setMaxResults(limit);
                List<DrugMakingMethodBean> lists = query.list();

                Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                if (organId != null) {
                    countQuery.setParameter("organId", organId);
                }
                if (!StringUtils.isEmpty(methodText)) {
                    countQuery.setParameter("methodText", "%" + methodText + "%");
                }
                Long total = (Long) countQuery.uniqueResult();
                setResult(new QueryResult<DrugMakingMethodBean>(total, query.getFirstResult(), query.getMaxResults(), lists));

            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = "select count(*) from DrugMakingMethod where organId=:organId")
    public abstract Long getCountOfOrgan(@DAOParam("organId") Integer organId);
}
