package recipe.dao;

import com.ngari.recipe.entity.MedicationSyncConfig;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import recipe.dao.comment.ExtendDao;

/**
 * @Author zgy
 * @Date 2022-10-11
 */

@RpcSupportDAO
public abstract class MedicationSyncConfigDAO extends HibernateSupportDelegateDAO<MedicationSyncConfig> implements ExtendDao<MedicationSyncConfig> {

    public MedicationSyncConfigDAO() {
        super();
        this.setEntityName(MedicationSyncConfig.class.getName());
        this.setKeyField("id");
    }

    @Override
    public boolean updateNonNullFieldByPrimaryKey(MedicationSyncConfig medicationSyncConfig) {
        return updateNonNullFieldByPrimaryKey(medicationSyncConfig, "id");
    }

    @DAOMethod(sql = "from MedicationSyncConfig where organId = :organId and dataType = :dataType")
    public abstract MedicationSyncConfig getMedicationSyncConfigByOrganIdAndDataType(@DAOParam("organId") Integer organId, @DAOParam("dataType") Integer dataType);
}
