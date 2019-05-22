package recipe.dao;

import com.alibaba.druid.util.StringUtils;
import com.ngari.recipe.entity.AuditDrugList;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author yinsheng
 * @date 2019\5\15 0015 21:06
 */
@RpcSupportDAO
public abstract class AuditDrugListDAO extends HibernateSupportDelegateDAO<AuditDrugList> {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditDrugListDAO.class);

    public AuditDrugListDAO() {
        super();
        this.setEntityName(AuditDrugList.class.getName());
        this.setKeyField("AuditDrugId");
    }

    @Override
    public AuditDrugList update(AuditDrugList auditDrugList) {
        auditDrugList.setLastModify(new Date());
        return super.update(auditDrugList);
    }

    @Override
    public AuditDrugList save(AuditDrugList auditDrugList) throws DAOException {
        auditDrugList.setLastModify(new Date());
        return super.save(auditDrugList);
    }

    /**
     * [平台使用]查询所有审核药品,药品未进行匹配
     * @param start  起始页
     * @param limit  限制页
     * @return       药品列表
     */
    public QueryResult<AuditDrugList> findAllDrugList(final String drugClass, final String keyword, final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<AuditDrugList>> action =
                new AbstractHibernateStatelessResultAction<QueryResult<AuditDrugList>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void execute(StatelessSession ss) throws DAOException {
                        StringBuilder hql = new StringBuilder(" from AuditDrugList where Type = 0 and Status = 0  ");
                        if (!StringUtils.isEmpty(drugClass)) {
                            hql.append(" and drugClass like :drugClass ");
                        }
                        if (!StringUtils.isEmpty(keyword)) {
                            hql.append(" and (");
                            hql.append(" drugName like :keyword or producer like :keyword or saleName like :keyword or approvalNumber like :keyword ");
                            hql.append(")");
                        }
                        hql.append(" order by Status asc, CreateDt desc ");

                        Query countQuery = ss.createQuery("select count(*) " + hql.toString());

                        Long total = (Long) countQuery.uniqueResult();

                        Query query = ss.createQuery(hql.toString());

                        query.setFirstResult(start);
                        query.setMaxResults(limit);
                        List<AuditDrugList> list = query.list();
                        setResult(new QueryResult<AuditDrugList>(total, query.getFirstResult(), query.getMaxResults(), list));
                    }
                };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * [医院使用]根据机构查询审核药品,药品已经匹配成功
     * @param organId  机构ID
     * @param start    起始页
     * @param limit    限制页
     * @return         药品列表
     */
    public QueryResult<AuditDrugList> findAllDrugListByOrganId(final Integer organId, final String drugClass, final String keyword, final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<AuditDrugList>> action =
                new AbstractHibernateStatelessResultAction<QueryResult<AuditDrugList>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void execute(StatelessSession ss) throws DAOException {
                        StringBuilder hql = new StringBuilder(" from AuditDrugList where OrganId=:organId and Type = 1 and Status = 0 ");
                        if (!StringUtils.isEmpty(drugClass)) {
                            hql.append(" and drugClass like :drugClass");
                        }
                        if (!StringUtils.isEmpty(keyword)) {
                            hql.append(" and (");
                            hql.append(" drugName like :keyword or producer like :keyword or saleName like :keyword or approvalNumber like :keyword ");
                            hql.append(")");
                        }
                        hql.append(" order by Status asc, CreateDt desc ");
                        Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                        countQuery.setParameter("organId", organId);
                        Long total = (Long) countQuery.uniqueResult();

                        Query query = ss.createQuery(hql.toString());
                        query.setParameter("organId", organId);
                        query.setFirstResult(start);
                        query.setMaxResults(limit);
                        List<AuditDrugList> list = query.list();

                        setResult(new QueryResult<AuditDrugList>(total, query.getFirstResult(), query.getMaxResults(), list));
                    }
                };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 更新审核信息
     * @param auditDrugId      主键
     * @param status           状态
     * @param rejectReason     拒绝原因
     */
    @DAOMethod(sql = "update AuditDrugList set Status=:status ,RejectReason=:rejectReason where AuditDrugId=:auditDrugId")
    public abstract void updateAuditDrugListStatus(@DAOParam("auditDrugId") Integer auditDrugId,
                                                   @DAOParam("status") Integer status,
                                                   @DAOParam("rejectReason") String rejectReason);
}
