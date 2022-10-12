package recipe.dao;

import com.ngari.recipe.entity.DrugCommon;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.StatelessSession;

import java.util.List;

/**
 * @author jiangtingfeng
 * date:2017/5/22.
 */
@RpcSupportDAO
public abstract class DrugCommonDAO extends HibernateSupportDelegateDAO<DrugCommon> {

    public DrugCommonDAO() {
        super();
        this.setEntityName(DrugCommon.class.getName());
        this.setKeyField("id");
    }

    /**
     * 获取常用列表
     *
     * @param organId   机构id
     * @param doctorId  医生id
     * @param drugTypes 药品类型
     * @return 常用药列表
     */
    @DAOMethod(sql = "from DrugCommon where organId=:organId and doctorId=:doctorId and  drugType in (:drugTypes) order by sort desc")
    public abstract List<DrugCommon> findByOrganIdAndDoctorIdAndTypes(@DAOParam("organId") Integer organId,
                                                                      @DAOParam("doctorId") Integer doctorId,
                                                                      @DAOParam("drugTypes") List<Integer> drugTypes,
                                                                      @DAOParam(pageStart = true) int start,
                                                                      @DAOParam(pageLimit = true) int limit);

    @DAOMethod(sql = "from DrugCommon where organId=:organId and doctorId=:doctorId and organDrugCode=:organDrugCode")
    public abstract DrugCommon getByOrganIdAndDoctorIdAndDrugCode(@DAOParam("organId") Integer organId,
                                                                  @DAOParam("doctorId") Integer doctorId,
                                                                  @DAOParam("organDrugCode") String organDrugCode);

    public List<DrugCommon> findByOrganIdAndDoctorIdAndDrugForm(final Integer organId, final Integer doctorId, final String drugForm, final int start, final int limit){
        HibernateStatelessResultAction<List<DrugCommon>> action = new AbstractHibernateStatelessResultAction<List<DrugCommon>>() {
                @Override
                public void execute(StatelessSession ss) throws Exception {
                    StringBuilder hql = new StringBuilder("select a from DrugCommon a,OrganDrugList b where a.drugId = b.drugId and a.organDrugCode = b.organDrugCode and a.organId = " +
                            " b.organId and a.organId=:organId and a.doctorId=:doctorId  and a.drugType = 3 ");
                    if (StringUtils.isNotEmpty(drugForm)) {
                        hql.append(" and b.drugForm=:drugForm ");
                    }
                    hql.append(" order by sort desc ");
                    Query q = ss.createQuery(hql.toString());
                    q.setParameter("organId", organId);
                    q.setParameter("doctorId", doctorId);
                    if (StringUtils.isNotEmpty(drugForm)) {
                        q.setParameter("drugForm", drugForm);
                    }
                    q.setFirstResult(start);
                    q.setMaxResults(limit);
                    setResult(q.list());
                }
            };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }
}