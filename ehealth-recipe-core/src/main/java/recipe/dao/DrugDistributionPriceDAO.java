package recipe.dao;

import com.ngari.recipe.entity.DrugDistributionPrice;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * @author jianghc
 * @create 2017-02-07 14:41
 **/
@RpcSupportDAO
public abstract class DrugDistributionPriceDAO extends HibernateSupportDelegateDAO<DrugDistributionPrice> {
    private static final Log logger = LogFactory.getLog(DrugDistributionPriceDAO.class);

    public DrugDistributionPriceDAO() {
        super();
        setEntityName(DrugDistributionPrice.class.getName());
        setKeyField("id");
    }

    @DAOMethod(sql = " from DrugDistributionPrice where enterpriseId =:enterpriseId order by addrArea desc")
    public abstract List<DrugDistributionPrice> findByEnterpriseId(@DAOParam("enterpriseId") Integer enterpriseId);

    @DAOMethod(sql = " delete from DrugDistributionPrice where id =:id")
    public abstract void deleteById(@DAOParam("id") Integer id);

    @DAOMethod(sql = " delete from DrugDistributionPrice where enterpriseId =:enterpriseId")
    public abstract void deleteByEnterpriseId(@DAOParam("enterpriseId") Integer enterpriseId);

    @DAOMethod
    public abstract DrugDistributionPrice getByEnterpriseIdAndAddrArea(Integer enterpriseId, String addrArea);


}
