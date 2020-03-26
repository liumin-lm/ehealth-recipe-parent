package recipe.dao;

import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.OrganAndDrugsepRelation;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 *
 * @author yu_yun
 */
@RpcSupportDAO
public abstract class OrganAndDrugsepRelationDAO extends HibernateSupportDelegateDAO<OrganAndDrugsepRelation> {
    private static final Log LOGGER = LogFactory.getLog(OrganAndDrugsepRelationDAO.class);

    public OrganAndDrugsepRelationDAO() {
        super();
        this.setEntityName(OrganAndDrugsepRelation.class.getName());
        this.setKeyField("id");
    }

    /**
     * 根据id获取
     * @param id
     * @return
     */
    @DAOMethod
    public abstract OrganAndDrugsepRelation getById(int id);

    /**
     * 根据机构id和药企id获取组织与药企间关系
     * @param organId
     * @param entId
     * @return
     */
    @DAOMethod(sql = "from OrganAndDrugsepRelation where organId = :organId and drugsEnterpriseId=:entId")
    public abstract OrganAndDrugsepRelation getOrganAndDrugsepByOrganIdAndEntId(@DAOParam("organId") Integer organId, @DAOParam("entId") Integer entId);

    /**
     * 根据机构id和状态获取
     * @param organId
     * @param status
     * @return
     */
    @DAOMethod(sql = "select t from DrugsEnterprise t, OrganAndDrugsepRelation s where t.id=s.drugsEnterpriseId " +
            "and s.organId=:organId and t.status=:status ")
    public abstract List<DrugsEnterprise> findDrugsEnterpriseByOrganIdAndStatus(@DAOParam("organId") Integer organId, @DAOParam("status") Integer status);

    /**
     * 根据机构id和状态获取药企id
     * @param organId
     * @param status
     * @return
     */
    @DAOMethod(sql = "select t.id from DrugsEnterprise t, OrganAndDrugsepRelation s where t.id=s.drugsEnterpriseId " +
            "and s.organId=:organId and t.status=:status ")
    public abstract List<Integer> findDrugsEnterpriseIdByOrganIdAndStatus(@DAOParam("organId") Integer organId, @DAOParam("status") Integer status);

}
