package recipe.dao;

import com.ngari.recipe.entity.DrugSaleStrategy;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.impl.dictionary.DBDictionaryItemLoader;
import ctd.util.annotation.RpcSupportDAO;
import recipe.dao.comment.ExtendDao;

import java.util.List;

/**
 * 药品销售策略
 */
@RpcSupportDAO
public abstract class DrugSaleStrategyDAO extends HibernateSupportDelegateDAO<DrugSaleStrategy> implements DBDictionaryItemLoader<DrugSaleStrategy>, ExtendDao<DrugSaleStrategy> {

    public DrugSaleStrategyDAO() {
        super();
        this.setEntityName(DrugSaleStrategy.class.getName());
        this.setKeyField("id");
    }
    @Override
    public boolean updateNonNullFieldByPrimaryKey(DrugSaleStrategy drugSaleStrategy) {
        return updateNonNullFieldByPrimaryKey(drugSaleStrategy, "id");
    }

    @DAOMethod(sql = "from DrugSaleStrategy where status=1 and drugId =:drugId ")
    public abstract List<DrugSaleStrategy> findByDrugId(@DAOParam("drugId") Integer drugId);

    @DAOMethod(sql = "from DrugSaleStrategy where status=1 and id=:id")
    public abstract DrugSaleStrategy getDrugSaleStrategyById(@DAOParam("id") Integer id);

    @DAOMethod(sql = "from DrugSaleStrategy where status=1 and id in (:ids)")
    public abstract List<DrugSaleStrategy> findBysaleStrategyIds(@DAOParam("ids")List<Integer> saleStrategyIds);

    @DAOMethod(sql = "from DrugSaleStrategy where status=1 ", limit = 0)
    public abstract List<DrugSaleStrategy> findAllDrugSaleStrategy();

}
