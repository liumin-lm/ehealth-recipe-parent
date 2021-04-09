package recipe.dao;

import com.google.common.collect.Maps;
import com.ngari.recipe.drugsenterprise.model.DrugEnterpriseLogisticsBean;
import com.ngari.recipe.entity.DrugEnterpriseLogistics;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.BeanUtils;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @description： 药企物流关系dao
 * @author： whf
 * @date： 2021-03-31 10:04
 */
@RpcSupportDAO
public abstract class DrugEnterpriseLogisticsDAO extends HibernateSupportDelegateDAO<DrugEnterpriseLogistics> {
    private static final Log LOGGER = LogFactory.getLog(DrugEnterpriseLogisticsDAO.class);

    public DrugEnterpriseLogisticsDAO() {
        super();
        this.setEntityName(DrugEnterpriseLogistics.class.getName());
        this.setKeyField("id");
    }

    /**
     * 根据药企ID删除该药企下的所有物流关系
     *
     * @param drugsEnterpriseId
     */
    @DAOMethod(sql = "DELETE FROM DrugEnterpriseLogistics WHERE drugsEnterpriseId = :drugsEnterpriseId")
    public abstract void deleteByDrugsEnterpriseId(@DAOParam("drugsEnterpriseId") Integer drugsEnterpriseId);

    /**
     * 批量插入
     *
     * @param drugEnterpriseLogistics
     * @return
     */
    public Boolean saveAll(List<DrugEnterpriseLogisticsBean> drugEnterpriseLogistics,Integer drugsEnterpriseId) {
        return insertAll(drugEnterpriseLogistics,drugsEnterpriseId);
    }


    /**
     * 批量写入
     *
     * @param drugEnterpriseLogistics
     * @return
     */
    private Boolean insertAll(List<DrugEnterpriseLogisticsBean> drugEnterpriseLogistics,Integer drugsEnterpriseId) {
        if (CollectionUtils.isEmpty(drugEnterpriseLogistics)) {
            return true;
        }

        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                Transaction transaction = ss.beginTransaction();
                drugEnterpriseLogistics.forEach(drugEnterpriseLogisticsBean -> {

                    DrugEnterpriseLogistics drugEnterpriseLogistic = new DrugEnterpriseLogistics();
                    drugEnterpriseLogistic.setDrugsEnterpriseId(drugsEnterpriseId);
                    BeanUtils.copy(drugEnterpriseLogisticsBean, drugEnterpriseLogistic);
                    drugEnterpriseLogistic.setCreateTime(new Date());
                    ss.insert(drugEnterpriseLogistic);

                });
                transaction.commit();
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据药企ID获取所有物流对接信息
     *
     * @param drugsEnterpriseId
     * @return
     */
    public List<DrugEnterpriseLogistics> getByDrugsEnterpriseId(Integer drugsEnterpriseId) {
        HibernateStatelessResultAction<List<DrugEnterpriseLogistics>> action = new AbstractHibernateStatelessResultAction<List<DrugEnterpriseLogistics>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                String hql = "FROM DrugEnterpriseLogistics WHERE drugsEnterpriseId = :drugsEnterpriseId";
                Map<String, Object> param = Maps.newHashMap();
                param.put("drugsEnterpriseId", drugsEnterpriseId);
                Query query = ss.createQuery(hql);
                query.setProperties(param);
                List list = query.list();
                setResult(list);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }
}
