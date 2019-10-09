package recipe.dao;

import com.ngari.recipe.entity.JudicialOrgan;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

/**
 * @author yinsheng
 * @date 2019\10\9 0009 11:10
 */
@RpcSupportDAO
public abstract class JudicialOrganDAO extends HibernateSupportDelegateDAO<JudicialOrgan> {

    public JudicialOrganDAO() {
        super();
        this.setEntityName(JudicialOrgan.class.getName());
        this.setKeyField("judicialorganId");
    }

    @DAOMethod
    public abstract JudicialOrgan getByJudicialorganId(Integer id);
}
