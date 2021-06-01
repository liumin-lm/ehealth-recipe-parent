package recipe.dao;

import com.ngari.recipe.drug.model.DecoctionWayBean;
import com.ngari.recipe.entity.DecoctionWay;
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

/**
 * @company: ngarihealth
 * @author: gaomw
 * @date:2020/8/5.
 */
@RpcSupportDAO
public abstract class DrugDecoctionWayDao extends HibernateSupportDelegateDAO<DecoctionWay> {
    public static final Logger log = LoggerFactory.getLogger(DecoctionWay.class);

    public DrugDecoctionWayDao() {
        super();
        this.setEntityName(DecoctionWay.class.getName());
        this.setKeyField("decoctionId");
    }


    @DAOMethod(sql = "from DecoctionWay where organId =:organId order by sort", limit = 0)
    public abstract List<DecoctionWayBean> findAllDecoctionWayByOrganId(@DAOParam("organId") Integer organId);

    @DAOMethod
    public abstract List<DecoctionWay> findByOrganId(Integer organId);

    @DAOMethod(sql = "from DecoctionWay where organId =:organId and decoctionCode = :decoctionCode")
    public abstract DecoctionWay getDecoctionWayByOrganIdAndCode(@DAOParam("organId") Integer organId
            , @DAOParam("decoctionCode") String decoctionCode);

    @DAOMethod(sql = "from DecoctionWay where organId =:organId and decoctionText = :decoctionText")
    public abstract DecoctionWay getDecoctionWayByOrganIdAndText(@DAOParam("organId") Integer organId
            , @DAOParam("decoctionText") String decoctionText);

    @DAOMethod(sql = "delete from DecoctionWay where decoctionId =:decoctionId ")
    public abstract void deleteDecoctionWayByDecoctionId(@DAOParam("decoctionId")Integer decoctionId);

    public QueryResult<DecoctionWayBean> findDecoctionWayByOrganIdAndName(Integer organId, String decoctionText, Integer start, Integer limit) {
        HibernateStatelessResultAction<QueryResult<DecoctionWayBean>> action = new AbstractHibernateStatelessResultAction<QueryResult<DecoctionWayBean>>() {

            @Override public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder("from DecoctionWay where 1=1");
                if (organId != null) {
                    hql.append(" and organId =:organId");
                }
                if (!StringUtils.isEmpty(decoctionText)) {
                    hql.append(" and decoctionText like :decoctionText");
                }
                hql.append(" order by sort");
                Query query = ss.createQuery(hql.toString());
                if (organId != null) {
                    query.setParameter("organId", organId);
                }
                if (!StringUtils.isEmpty(decoctionText)) {
                    query.setParameter("decoctionText", "%" + decoctionText + "%");
                }
                query.setFirstResult(start);
                query.setMaxResults(limit);
                List<DecoctionWayBean> lists = query.list();

                Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                if (organId != null) {
                    countQuery.setParameter("organId", organId);
                }
                if (!StringUtils.isEmpty(decoctionText)) {
                    countQuery.setParameter("decoctionText", "%" + decoctionText + "%");
                }
                Long total = (Long) countQuery.uniqueResult();
                setResult(new QueryResult<DecoctionWayBean>(total, query.getFirstResult(), query.getMaxResults(), lists));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }


    @DAOMethod(sql = "select count(*) from DecoctionWay where organId=:organId")
    public abstract Long getCountOfOrgan(@DAOParam("organId") Integer organId);

}
