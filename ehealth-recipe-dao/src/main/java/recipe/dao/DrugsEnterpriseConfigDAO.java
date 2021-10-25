package recipe.dao;

import com.ngari.recipe.entity.DrugsEnterpriseConfig;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * @author renfuhao
 */
@RpcSupportDAO
public abstract class DrugsEnterpriseConfigDAO extends HibernateSupportDelegateDAO<DrugsEnterpriseConfig> {
    private static Logger logger = Logger.getLogger(DrugsEnterpriseConfigDAO.class);

    public DrugsEnterpriseConfigDAO() {
        super();
        this.setEntityName(DrugsEnterpriseConfig.class.getName());
        this.setKeyField("id");
    }

    /**
     * 根据药企id获取药企配置表数据
     *
     * @param drugsenterpriseId
     * @return
     */
    @DAOMethod(sql = "from DrugsEnterpriseConfig where drugsenterpriseId=:drugsenterpriseId")
    public abstract DrugsEnterpriseConfig getByDrugsenterpriseId(@DAOParam("drugsenterpriseId") int drugsenterpriseId);


}
