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
 * Created by houxr on 2016/8/3.
 */
@RpcSupportDAO
public abstract class OrganAndDrugsepRelationDAO extends HibernateSupportDelegateDAO<OrganAndDrugsepRelation> {
    private static final Log logger = LogFactory.getLog(OrganAndDrugsepRelationDAO.class);

    public OrganAndDrugsepRelationDAO() {
        super();
        this.setEntityName(OrganAndDrugsepRelation.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod
    public abstract OrganAndDrugsepRelation getById(int id);

    @DAOMethod(sql = "from OrganAndDrugsepRelation where organId = :organId and drugsEnterpriseId=:entId")
    public abstract OrganAndDrugsepRelation getOrganAndDrugsepByOrganIdAndEntId(@DAOParam("organId") Integer organId, @DAOParam("entId") Integer entId);

    @DAOMethod(sql = "select t from DrugsEnterprise t, OrganAndDrugsepRelation s where t.id=s.drugsEnterpriseId " +
            "and s.organId=:organId and t.status=:status ")
    public abstract List<DrugsEnterprise> findDrugsEnterpriseByOrganIdAndStatus(@DAOParam("organId") Integer organId, @DAOParam("status") Integer status);

}
