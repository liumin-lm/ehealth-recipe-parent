package recipe.dao;

import com.ngari.recipe.entity.MedicationSyncConfig;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import recipe.dao.comment.ExtendDao;

import java.sql.Time;
import java.util.List;

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

    /**
     * 根据定时时间范围 查找 同步开关开启的 配置机构ID集合（同步配置调用）
     * @param startTime
     * @param endTime
     * @return
     */
    @DAOMethod(sql="from MedicationSyncConfig where enableSync=1 and dockingMode=1 and regularTime >=:startTime and  regularTime <=:endTime ",limit = 0)
    public abstract List<MedicationSyncConfig> findByEnableSyncAndTime(@DAOParam("startTime") Time startTime , @DAOParam("endTime") Time endTime  );

    @DAOMethod(sql="from MedicationSyncConfig where enableSync=1 and dockingMode=1 ",limit = 0)
    public abstract List<MedicationSyncConfig> findByEnableSync();

    @DAOMethod
    public abstract List<MedicationSyncConfig> getByOrganId(Integer organId);
}
