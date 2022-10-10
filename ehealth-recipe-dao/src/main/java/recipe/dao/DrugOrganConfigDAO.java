package recipe.dao;

import com.ngari.recipe.entity.DrugOrganConfig;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.SaleDrugListSyncField;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.impl.dictionary.DBDictionaryItemLoader;
import ctd.util.annotation.RpcSupportDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.comment.ExtendDao;

import java.util.List;

/**
 * @author 刘敏
 */
@RpcSupportDAO
public abstract class DrugOrganConfigDAO extends HibernateSupportDelegateDAO<DrugOrganConfig>
        implements DBDictionaryItemLoader<DrugOrganConfig>, ExtendDao<DrugOrganConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DrugOrganConfigDAO.class);

    public DrugOrganConfigDAO() {
        super();
        this.setEntityName(DrugOrganConfig.class.getName());
        this.setKeyField("id");
    }

    @Override
    public boolean updateNonNullFieldByPrimaryKey(DrugOrganConfig obj) {
        return updateNonNullFieldByPrimaryKey(obj, "id");
    }

    @DAOMethod
    public abstract DrugOrganConfig getByOrganId(Integer organId);

}
