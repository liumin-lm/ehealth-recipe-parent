package recipe.dao;

import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.OrganAndDrugsepRelation;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.StatelessSession;
import recipe.dao.comment.ExtendDao;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yu_yun
 */
@RpcSupportDAO
public abstract class OrganAndDrugsepRelationDAO extends HibernateSupportDelegateDAO<OrganAndDrugsepRelation> implements ExtendDao<OrganAndDrugsepRelation> {
    private static final Log LOGGER = LogFactory.getLog(OrganAndDrugsepRelationDAO.class);

    public OrganAndDrugsepRelationDAO() {
        super();
        this.setEntityName(OrganAndDrugsepRelation.class.getName());
        this.setKeyField("id");
    }


    @Override
    public boolean updateNonNullFieldByPrimaryKey(OrganAndDrugsepRelation relation) {
        return updateNonNullFieldByPrimaryKey(relation, "id");
    }

    /**
     * 根据id获取
     *
     * @param id
     * @return
     */
    @DAOMethod
    public abstract OrganAndDrugsepRelation getById(int id);

    /**
     * 根据机构id和药企id获取组织与药企间关系
     *
     * @param organId
     * @param entId
     * @return
     */
    @DAOMethod(sql = "from OrganAndDrugsepRelation where organId = :organId and drugsEnterpriseId=:entId")
    public abstract OrganAndDrugsepRelation getOrganAndDrugsepByOrganIdAndEntId(@DAOParam("organId") Integer organId, @DAOParam("entId") Integer entId);


    /**
     * 根据药企id获取组织与药企间关系
     *
     * @param entId
     * @return
     */
    @DAOMethod(sql = "from OrganAndDrugsepRelation where  drugsEnterpriseId=:entId")
    public abstract List<OrganAndDrugsepRelation> findByEntId(@DAOParam("entId") Integer entId);

    /**
     * 根据机构id和状态获取
     *
     * @param organId
     * @param status
     * @return
     */
    @DAOMethod(sql = "select t from DrugsEnterprise t, OrganAndDrugsepRelation s where t.id=s.drugsEnterpriseId and s.organId=:organId and t.status=:status")
    public abstract List<DrugsEnterprise> findDrugsEnterpriseByOrganIdAndStatus(@DAOParam("organId") Integer organId, @DAOParam("status") Integer status);

    /**
     * 根据机构id和状态获取药企id
     *
     * @param organId
     * @param status
     * @return
     */
    @DAOMethod(sql = "select t.id from DrugsEnterprise t, OrganAndDrugsepRelation s where t.id=s.drugsEnterpriseId and s.organId=:organId and t.status=:status")
    public abstract List<Integer> findDrugsEnterpriseIdByOrganIdAndStatus(@DAOParam("organId") Integer organId, @DAOParam("status") Integer status);

    /**
     * 根据机构id 与 类型获取关联关系
     *
     * @param clinicOrgan
     * @param type
     * @return
     */
    public List<OrganAndDrugsepRelation> getRelationByOrganIdAndGiveMode(@DAOParam("clinicOrgan") Integer clinicOrgan, @DAOParam("type") Integer type) {
        HibernateStatelessResultAction<List<OrganAndDrugsepRelation>> action = new AbstractHibernateStatelessResultAction<List<OrganAndDrugsepRelation>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder sql = new StringBuilder("SELECT id,organid,DrugsEnterpriseId,drug_enterprise_support_give_mode FROM cdr_organ_drugsep_relation WHERE OrganId = ");
                sql.append(clinicOrgan).append(" and drug_enterprise_support_give_mode LIKE '%").append(type).append("%'");
                Query query = ss.createSQLQuery(String.valueOf(sql));

                List<Object[]> result = query.list();
                List<OrganAndDrugsepRelation> vo = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(result)) {
                    for (Object[] objects : result) {
                        OrganAndDrugsepRelation organAndDrugsepRelation = new OrganAndDrugsepRelation();
                        organAndDrugsepRelation.setDrugsEnterpriseId(Integer.valueOf(objects[2].toString()));
                        organAndDrugsepRelation.setDrugsEnterpriseSupportGiveMode(objects[3].toString());
                        organAndDrugsepRelation.setOrganId(Integer.valueOf(objects[1].toString()));
                        organAndDrugsepRelation.setId(Integer.valueOf(objects[0].toString()));
                        vo.add(organAndDrugsepRelation);
                    }
                }
                setResult(vo);
            }
        };
        HibernateSessionTemplate.instance().executeReadOnly(action);
        return action.getResult();
    }
}
