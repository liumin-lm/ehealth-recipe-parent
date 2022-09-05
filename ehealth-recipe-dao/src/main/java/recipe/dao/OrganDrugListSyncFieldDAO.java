package recipe.dao;

import com.ngari.recipe.entity.OrganDrugListSyncField;
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
public abstract class OrganDrugListSyncFieldDAO extends HibernateSupportDelegateDAO<OrganDrugListSyncField>
        implements DBDictionaryItemLoader<OrganDrugListSyncField>, ExtendDao<OrganDrugListSyncField> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganDrugListSyncFieldDAO.class);
    private static final Integer ALL_DRUG_FLAG = 9;

    public OrganDrugListSyncFieldDAO() {
        super();
        this.setEntityName(OrganDrugListSyncField.class.getName());
        this.setKeyField("id");
    }

    @Override
    public boolean updateNonNullFieldByPrimaryKey(OrganDrugListSyncField organDrugListSyncField) {
        return updateNonNullFieldByPrimaryKey(organDrugListSyncField, "id");
    }

    @DAOMethod(sql = "from OrganDrugListSyncField where organId=:organId")
    public abstract List<OrganDrugListSyncField> findByOrganId(@DAOParam("organId") int organId);

    @DAOMethod(sql = "from OrganDrugListSyncField where organId=:organId and  type=:type")
    public abstract List<OrganDrugListSyncField>  findByOrganIdAndType(@DAOParam("organId") int organId,@DAOParam("type") String type);

    @DAOMethod(sql = "from OrganDrugListSyncField where organId=:organId and fieldCode=:fieldCode and type=:type")
    public abstract OrganDrugListSyncField getByOrganIdAndFieldCodeAndType(@DAOParam("organId") int organId,@DAOParam("fieldCode") String fieldCode,@DAOParam("type") String type);

    @DAOMethod(sql = "from OrganDrugListSyncField where organId=:organId and fieldCode=:fieldCode and type=:type order by updateTime desc")
    public abstract List<OrganDrugListSyncField>  findByOrganIdAndFieldCodeAndType(@DAOParam("organId") int organId,@DAOParam("fieldCode") String fieldCode,@DAOParam("type") String type);


}
