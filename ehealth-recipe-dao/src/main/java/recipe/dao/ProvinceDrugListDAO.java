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
    @DAOMethod
    public abstract List<ProvinceDrugList> findByProvinceIdAndStatus(String provinceId, int status);

    /**
     * 商品名匹配省平台药品
     * @param name
     * @return
     */
    public List<ProvinceDrugList> findByProvinceSaleNameLike(final String name) {
        HibernateStatelessResultAction<List<ProvinceDrugList>> action = new AbstractHibernateStatelessResultAction<List<ProvinceDrugList>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder("from ProvinceDrugList where status = 1 and saleName like :name");
                Query q = ss.createQuery(hql.toString());
                q.setParameter("name", "%" + name + "%");
                setResult(q.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /*根据关联省的省药品列表数据导入量*/
    @DAOMethod(sql = "select count(*) from ProvinceDrugList pd where pd.provinceId = :provinceId and pd.status = :status)", limit = 0)
    public abstract Long getCountByProvinceIdAndStatus(@DAOParam("provinceId")String provinceId, @DAOParam("status")int status);
}
