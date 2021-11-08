package recipe.dao;

import com.ngari.recipe.entity.DrugListMatch;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.persistence.support.impl.dictionary.DBDictionaryItemLoader;
import ctd.util.BeanUtils;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RpcSupportDAO
@Repository
public abstract class DrugListMatchDAO extends HibernateSupportDelegateDAO<DrugListMatch>
        implements DBDictionaryItemLoader<DrugListMatch> {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DrugListMatchDAO.class);


    public DrugListMatchDAO() {
        super();
        this.setEntityName(DrugListMatch.class.getName());
        this.setKeyField("drugId");
    }

    /**
     * 药品查询服务
     *
     * @param status    药品状态
     * @param keyword   查询关键字:药品名称 or 生产厂家 or 商品名称 or 批准文号 or drugId
     * @param start     分页起始位置
     * @param limit     每页限制条数
     * @return QueryResult<DrugList>
     * @author houxr
     */
    public QueryResult<DrugListMatch> queryDrugListsByDrugNameAndStartAndLimit(final Integer organId,final String keyword,
                                                                               final Integer status,
                                                                               final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<DrugListMatch>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugListMatch>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder("From DrugListMatch where sourceOrgan=:sourceOrgan");
                if (!StringUtils.isEmpty(keyword)) {
                    hql.append(" and (");
                    hql.append(" drugName like :keyword or producer like :keyword or saleName like :keyword or organDrugCode like :keyword ");
                    hql.append(")");
                }

                if (!ObjectUtils.isEmpty(status)) {
                    if(status == -1){
                        hql.append(" and status !=2");
                    }else{
                        hql.append(" and status =:status");
                    }
                }
                /*hql.append(" order by createDt desc");*/
                Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                if (!ObjectUtils.isEmpty(status) && status != -1) {
                    countQuery.setParameter("status", status);
                }
                if (!StringUtils.isEmpty(keyword)) {
                    countQuery.setParameter("keyword", "%" + keyword + "%");
                }
                if (!ObjectUtils.isEmpty(organId)) {
                    countQuery.setParameter("sourceOrgan", organId);
                }

                Long total = (Long) countQuery.uniqueResult();

                Query query = ss.createQuery(hql.toString());
                if (!ObjectUtils.isEmpty(status) && status != -1) {
                    query.setParameter("status", status);
                }
                if (!StringUtils.isEmpty(keyword)) {
                    query.setParameter("keyword", "%" + keyword + "%");
                }
                if (!ObjectUtils.isEmpty(organId)) {
                    query.setParameter("sourceOrgan", organId);
                }

                query.setFirstResult(start);
                query.setMaxResults(limit);
                List<DrugListMatch> lists = query.list();
                setResult(new QueryResult<DrugListMatch>(total, query.getFirstResult(), query.getMaxResults(), lists));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 药品查询服务
     *
     * @param keyword   查询关键字:药品名称 or 生产厂家 or 商品名称 or 批准文号 or drugId
     * @param start     分页起始位置
     * @param limit     每页限制条数
     * @return QueryResult<DrugList>
     * @author houxr
     */
    public QueryResult<DrugListMatch> queryDrugListsCheckByDrugNameAndStartAndLimit(final Date startTime,final Date endTime, final Integer organId, final String keyword,
                                                                                    final String drugForm,
                                                                                    final int start, final int limit) {
        HibernateStatelessResultAction<QueryResult<DrugListMatch>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugListMatch>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                DateTime dt = new DateTime(endTime);
                StringBuilder hql = new StringBuilder("From DrugListMatch where sourceOrgan=:sourceOrgan and status = 1 and drugSource=1 ");
                if (!ObjectUtils.isEmpty(startTime) && !ObjectUtils.isEmpty(endTime)) {
                    hql.append(" and createDt>=:startTime and createDt<=:endTime ");
                }
                if (!StringUtils.isEmpty(keyword)) {
                    hql.append(" and (");
                    hql.append(" drugName like :keyword or producer like :keyword or saleName like :keyword or organDrugCode like :keyword ");
                    hql.append(")");
                }

                if (!ObjectUtils.isEmpty(drugForm)) {
                    hql.append(" and drugForm =:drugForm");
                }
                /*hql.append(" order by createDt desc");*/
                Query countQuery = ss.createQuery("select count(*) " + hql.toString());

                if (!ObjectUtils.isEmpty(startTime)) {
                    countQuery.setParameter("startTime", startTime);
                }
                if (!ObjectUtils.isEmpty(endTime)) {
                    countQuery.setParameter("endTime", dt.plusDays(1).toDate());
                }
                if (!ObjectUtils.isEmpty(drugForm)) {
                    countQuery.setParameter("drugForm", drugForm);
                }
                if (!StringUtils.isEmpty(keyword)) {
                    countQuery.setParameter("keyword", "%" + keyword + "%");
                }
                if (!ObjectUtils.isEmpty(organId)) {
                    countQuery.setParameter("sourceOrgan", organId);
                }

                Long total = (Long) countQuery.uniqueResult();

                Query query = ss.createQuery(hql.toString());

                if (!ObjectUtils.isEmpty(startTime)) {
                    query.setParameter("startTime", startTime);
                }
                if (!ObjectUtils.isEmpty(endTime)) {
                    query.setParameter("endTime", dt.plusDays(1).toDate());
                }
                if (!ObjectUtils.isEmpty(drugForm) ) {
                    query.setParameter("drugForm", drugForm);
                }
                if (!StringUtils.isEmpty(keyword)) {
                    query.setParameter("keyword", "%" + keyword + "%");
                }
                if (!ObjectUtils.isEmpty(organId)) {
                    query.setParameter("sourceOrgan", organId);
                }

                query.setFirstResult(start);
                query.setMaxResults(limit);
                List<DrugListMatch> lists = query.list();
                setResult(new QueryResult<DrugListMatch>(total, query.getFirstResult(), query.getMaxResults(), lists));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * @param drugId
     * @param changeAttr
     * @return
     */
    public Boolean updateDrugListMatchInfoById(final int drugId, final Map<String, ?> changeAttr) {
        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update DrugListMatch set lastModify=current_timestamp() ");
                if (null != changeAttr && !changeAttr.isEmpty()) {
                    for (String key : changeAttr.keySet()) {
                        if (key.equals("status")&&changeAttr.get(key).equals(0)){
                            hql.append(" ,matchDrugId = null");
                            hql.append(" ,regulationDrugCode = null");
                        }
                        hql.append("," + key + "=:" + key);
                    }
                }
                hql.append(" where drugId=:drugId");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("drugId", drugId);
                if (null != changeAttr && !changeAttr.isEmpty()) {
                    for (String key : changeAttr.keySet()) {
                        q.setParameter(key, changeAttr.get(key));
                    }
                }
                int flag = q.executeUpdate();
                setResult(flag == 1);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 查询机构 以匹配数据
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from DrugListMatch where sourceOrgan =:organId and status=1 and drugSource=0  ",limit = 0)
    public abstract List<DrugListMatch> findMatchDataByOrganAndStatus(@DAOParam("organId") int organId);

    /**
     * 查询机构 以匹配数据
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from DrugListMatch where sourceOrgan =:organId and status=1 and drugSource=1  ",limit = 0)
    public abstract List<DrugListMatch> findMatchDataByOrganAndStatusAndrugSource(@DAOParam("organId") int organId);

    @DAOMethod(sql = "from DrugListMatch where sourceOrgan =:organId",limit = 0)
    public abstract List<DrugListMatch> findMatchDataByOrgan(@DAOParam("organId") int organId);

    public QueryResult<DrugListMatch> findMatchDataByOrgan(final int organId, final int start, final int limit,final int status){
        HibernateStatelessResultAction<QueryResult<DrugListMatch>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugListMatch>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from DrugListMatch where sourceOrgan =:organId and status !=:status");
                Query query = ss.createQuery(hql.toString());
                query.setParameter("organId", organId);
                query.setParameter("status", status);
                query.setFirstResult(start);
                query.setMaxResults(limit);

                Query countQuery = ss.createQuery("select count(*) " + hql.toString());
                countQuery.setParameter("organId", organId);
                countQuery.setParameter("status", status);
                Long total = (Long) countQuery.uniqueResult();

                List<DrugListMatch> lists = query.list();
                setResult(new QueryResult<DrugListMatch>(total, query.getFirstResult(), query.getMaxResults(), lists));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    public boolean updateData(final DrugListMatch drug){
        final HashMap<String,Object> map = BeanUtils.map(drug, HashMap.class);
        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("update DrugListMatch set lastModify=current_timestamp() ");
                for (String key : map.keySet()) {
                    if (key.equals("status")||key.equals("isNew")||key.equals("matchDrugId")){
                        continue;
                    }
                    if (!key.endsWith("Text")){
                        hql.append("," + key + "=:" + key);
                    }
                }
                hql.append(" where organDrugCode=:organDrugCode and sourceOrgan=:sourceOrgan");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("organDrugCode", drug.getOrganDrugCode());
                q.setParameter("sourceOrgan", drug.getSourceOrgan());
                for (String key : map.keySet()) {
                    if (key.equals("status")||key.equals("isNew")||key.equals("matchDrugId")){
                        continue;
                    }
                    if (!key.endsWith("Text")){
                        q.setParameter(key, map.get(key));
                    }
                }
                int flag = q.executeUpdate();
                setResult(flag == 1);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = "from DrugListMatch where sourceOrgan =:organId and status =:status and drugSource=0 ",limit = 0)
    public abstract List<DrugListMatch> findDataByOrganAndStatus(@DAOParam("organId")int organId,@DAOParam("status")int status);

    @DAOMethod(sql = "from DrugListMatch where organDrugCode =:organDrugCode and sourceOrgan=:sourceOrgan ")
    public abstract List<DrugListMatch> findDataByOrganDrugCode(@DAOParam("organDrugCode")String organDrugCode,@DAOParam("sourceOrgan") Integer sourceOrgan);

    @DAOMethod(sql = "from DrugListMatch where organDrugCode =:organDrugCode and sourceOrgan=:sourceOrgan and status !=2 ")
    public abstract List<DrugListMatch> findDataByOrganDrugCodenew(@DAOParam("organDrugCode")String organDrugCode,@DAOParam("sourceOrgan") Integer sourceOrgan);

    @DAOMethod(sql = "SELECT COUNT(*) from DrugListMatch where sourceOrgan =:sourceOrgan and status !=2 ")
    public abstract Long getNumBySourceOrgan(@DAOParam("sourceOrgan") Integer organDrugCode);


    /**
     * 根据id删除
     * @param id
     */
    @DAOMethod(sql = " delete from DrugListMatch where id =:id")
    public abstract void deleteById(@DAOParam("id") Integer id);

    /**
     * 根据机构id删除
     * @param organId
     */
    @DAOMethod(sql = " delete from DrugListMatch where sourceOrgan =:sourceOrgan")
    public abstract void deleteByOrganId(@DAOParam("sourceOrgan") Integer organId);


    /**
     * 根据机构id删除  已提交数据
     * @param organId
     */
    @DAOMethod(sql = " delete from DrugListMatch where sourceOrgan =:sourceOrgan  and status=2  ")
    public abstract void deleteByOrganIdAndStatus(@DAOParam("sourceOrgan") Integer organId);

    @DAOMethod(sql = "update DrugListMatch set status = :resultStatus where sourceOrgan = :organId and regulationDrugCode Is Null and status in :statusList")
    public abstract void updateStatusListToStatus(@DAOParam("statusList") List<Integer> statusList, @DAOParam("organId") Integer organId, @DAOParam("resultStatus") Integer resultStatus);

    @DAOMethod(sql = "select count(*) from DrugListMatch where sourceOrgan =:organId and status not in :noStatusList")
    public abstract long getCountByNoStatus(@DAOParam("organId")int organId, @DAOParam("noStatusList")List<Integer> noStatusList);


    @DAOMethod(sql = "update DrugListMatch set platformDrugId=:platformDrugId where drugId = :drugId")
    public abstract void updatePlatformDrugIdByDrugId(@DAOParam("platformDrugId") Integer platformDrugId, @DAOParam("drugId") Integer drugId);

}
