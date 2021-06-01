package recipe.dao;

import com.ngari.recipe.entity.OrganMedicationGuideRelation;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

/**
 * created by shiyuping on 2019/10/25
 */
@RpcSupportDAO
public abstract class OrganMedicationGuideRelationDAO extends HibernateSupportDelegateDAO<OrganMedicationGuideRelation> {
    public OrganMedicationGuideRelationDAO() {
        super();
        this.setEntityName(OrganMedicationGuideRelation.class.getName());
        this.setKeyField("organGuideRelationId");
    }

    @DAOMethod(sql = "from OrganMedicationGuideRelation where organId=:organId ")
    public abstract OrganMedicationGuideRelation getOrganMedicationGuideRelationByOrganId(@DAOParam("organId") int organId);
}
