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
 * 2017-02-07 14:41
 **/
@RpcSupportDAO
public abstract class DrugDistributionPriceDAO extends HibernateSupportDelegateDAO<DrugDistributionPrice> {
    private static final Log LOGGER = LogFactory.getLog(DrugDistributionPriceDAO.class);

    public DrugDistributionPriceDAO() {
        super();
        setEntityName(DrugDistributionPrice.class.getName());
        setKeyField("id");
    }

    /**
     * 根据药品序号获取
     * @param enterpriseId
     * @return
     */
    @DAOMethod(sql = " from DrugDistributionPrice where enterpriseId =:enterpriseId order by addrArea desc", limit=0)
    public abstract List<DrugDistributionPrice> findByEnterpriseId(@DAOParam("enterpriseId") Integer enterpriseId);

    /**
     * 根据id删除
     * @param id
     */
    @DAOMethod(sql = " delete from DrugDistributionPrice where id =:id")
    public abstract void deleteById(@DAOParam("id") Integer id);

    /**
     * 根据药品序号删除
     * @param enterpriseId
     */
    @DAOMethod(sql = " delete from DrugDistributionPrice where enterpriseId =:enterpriseId")
    public abstract void deleteByEnterpriseId(@DAOParam("enterpriseId") Integer enterpriseId);

    /**
     * 根据地区和药品序号获取
     *
     * @param enterpriseId
     * @param addrArea
     * @return
     */
    @DAOMethod
    public abstract DrugDistributionPrice getByEnterpriseIdAndAddrArea(Integer enterpriseId, String addrArea);

    /**
     * 根据药品序号删除
     *
     * @param enterpriseId
     */
    @DAOMethod(sql = " delete from DrugDistributionPrice where enterpriseId =:enterpriseId and addrArea=:addrArea")
    public abstract void deleteByEnterpriseIdAddr(@DAOParam("enterpriseId") Integer enterpriseId, @DAOParam("addrArea") String addrArea);
}
