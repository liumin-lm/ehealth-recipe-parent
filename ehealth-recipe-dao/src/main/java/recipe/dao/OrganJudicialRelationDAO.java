package recipe.dao;

import com.ngari.recipe.entity.OrganJudicialRelation;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

/**
 * @author yinsheng
 * @date 2019\10\9 0009 11:12
 */
@RpcSupportDAO
public abstract class OrganJudicialRelationDAO extends HibernateSupportDelegateDAO<OrganJudicialRelation> {

    public OrganJudicialRelationDAO() {
        super();
        this.setEntityName(OrganJudicialRelation.class.getName());
        this.setKeyField("organJudRelationId");
    }

    @DAOMethod(sql = "from OrganJudicialRelation where organId=:organId ")
    public abstract OrganJudicialRelation getOrganJudicialRelationByOrganId(@DAOParam("organId") int organId);
}
