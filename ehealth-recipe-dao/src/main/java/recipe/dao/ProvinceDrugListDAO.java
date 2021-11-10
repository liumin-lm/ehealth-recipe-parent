package recipe.dao;

import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.ProvinceDrugList;
import ctd.persistence.DAOFactory;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.persistence.support.impl.dictionary.DBDictionaryItemLoader;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import recipe.util.DateConversion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
* @Description: ProvinceDrugListDAO 类（或接口）是 省平台药品
* @Author: JRK
* @Date: 2019/10/25
*/
@RpcSupportDAO
public abstract class ProvinceDrugListDAO extends HibernateSupportDelegateDAO<ProvinceDrugList>
        implements DBDictionaryItemLoader<ProvinceDrugList> {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvinceDrugListDAO.class);


    public ProvinceDrugListDAO() {
        super();
        this.setEntityName(ProvinceDrugList.class.getName());
        this.setKeyField("provinceDrugId");
    }

    /*根据关联省的省药品列表*/
    @DAOMethod(limit = 0)
    public abstract List<ProvinceDrugList> findByProvinceIdAndStatus(String provinceId, int status);

    /**
     * 商品名匹配省平台药品
     * @param name
     * @return
     */
    public List<ProvinceDrugList> findByProvinceSaleNameLike(final String name, final String address, final int start, final int limit, final String seacrhString,String drugType) {
        HibernateStatelessResultAction<List<ProvinceDrugList>> action = new AbstractHibernateStatelessResultAction<List<ProvinceDrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from ProvinceDrugList where provinceId = :address and status = 1 and saleName like :name");
                if(null != seacrhString){
                    hql.append(" and (saleName like :seacrhString OR drugName like :seacrhString or producer like :seacrhString )");
                }
                if(null != drugType){
                    hql.append(" and drugType =:drugType ");
                }
                Query q = ss.createQuery(hql.toString());
                q.setParameter("name", "%" + name + "%");
                q.setParameter("address", address);
                if(null != seacrhString){
                    q.setParameter("seacrhString", "%" + seacrhString + "%");
                }
                if(null != drugType){
                    q.setParameter("drugType",drugType);
                }
                q.setMaxResults(limit);
                q.setFirstResult(start);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 商品名匹配省平台药品 搜索专用
     * @return
     */
    public List<ProvinceDrugList> findByProvinceSaleNameLikeSearch( final String address, final int start, final int limit,  String input, String producer,String drugType) {
        HibernateStatelessResultAction<List<ProvinceDrugList>> action = new AbstractHibernateStatelessResultAction<List<ProvinceDrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from ProvinceDrugList where provinceId = :address and status = 1 ");
                if(null != input){
                    hql.append(" and (saleName like :input OR drugName like :input or provinceDrugId like :input ) ");
                }
                if(null != producer){
                    hql.append(" and producer like :producer ");
                }
                if(null != drugType){
                    hql.append(" and drugType =:drugType ");
                }
                Query q = ss.createQuery(hql.toString());

                q.setParameter("address", address);
                if(null != input){
                    q.setParameter("input", "%" + input + "%");
                }
                if(null != producer){
                    q.setParameter("producer", "%" + producer + "%");
                }
                if(null != drugType){
                    q.setParameter("drugType",drugType);
                }
                q.setMaxResults(limit);
                q.setFirstResult(start);
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /*根据关联省的省药品列表数据导入量*/
    @DAOMethod(sql = "select count(*) from ProvinceDrugList pd where pd.provinceId = :provinceId and pd.status = :status)", limit = 0)
    public abstract Long getCountByProvinceIdAndStatus(@DAOParam("provinceId")String provinceId, @DAOParam("status")int status);

    @DAOMethod(sql = " delete from ProvinceDrugList where provinceId =:provinceId")
    public abstract void deleteByProvinceId(@DAOParam("provinceId")String id);

    @DAOMethod(sql = " delete from ProvinceDrugList where provinceDrugId =:provinceDrugId")
    public abstract void deleteByProvinceDrugId(@DAOParam("provinceDrugId")Integer provinceDrugId);

    @DAOMethod(sql = "from ProvinceDrugList where provinceId =:provinceId and provinceDrugCode =:provinceDrugCode and status=:status")
    public abstract ProvinceDrugList getByProvinceIdAndDrugId(@DAOParam("provinceId")String id,
                                                  @DAOParam("provinceDrugCode")String provinceDrugId,
                                                  @DAOParam("status")Integer status);
}
