package recipe.dao;

import com.ngari.recipe.entity.DrugOrganConfig;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.impl.dictionary.DBDictionaryItemLoader;
import ctd.util.annotation.RpcSupportDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.comment.ExtendDao;

import java.sql.Time;
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

    public boolean updateNonNullFieldByOrganId(DrugOrganConfig obj) {
        return updateNonNullFieldByPrimaryKey(obj, "organId");
    }

    @DAOMethod(sql = "update DrugOrganConfig set toOrganIds=:toOrganIds where organId =:organId")
    public abstract void updateToOrganIdsByOrganId(@DAOParam("toOrganIds") String toOrganIds, @DAOParam("organId") Integer organId);

    @DAOMethod
    public abstract DrugOrganConfig getByOrganId(Integer organId);

    /**
     * 根据定时时间范围 查找 同步开关开启的 配置机构ID集合（药品同步调用）
     *
     * @param startTime
     * @param endTime
     * @return
     */
    @DAOMethod(sql = "select organId  from DrugOrganConfig where enableDrugSync=1 and dockingMode=1 and regularTime >:startTime and  regularTime <=:endTime ", limit = 0)
    public abstract List<Integer> findOrganIdByEnableDrugSyncAndTime(@DAOParam("startTime") Time startTime, @DAOParam("endTime") Time endTime);

    @DAOMethod(sql = "select organId from DrugOrganConfig where enable_drug_sync=1", limit = 0)
    public abstract List<Integer> findEnableDrugSync();

}
