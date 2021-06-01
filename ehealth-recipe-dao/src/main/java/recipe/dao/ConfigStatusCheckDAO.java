package recipe.dao;

import com.ngari.recipe.entity.ConfigStatusCheck;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.comment.ExtendDao;

import java.util.List;


/**
 * 状态校验表
 *
 * @author fuzi
 */
@RpcSupportDAO
public abstract class ConfigStatusCheckDAO extends HibernateSupportDelegateDAO<ConfigStatusCheck> implements ExtendDao<ConfigStatusCheck> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public ConfigStatusCheckDAO() {
        super();
        this.setEntityName(ConfigStatusCheck.class.getName());
        this.setKeyField("id");
    }

    @Override
    public boolean updateNonNullFieldByPrimaryKey(ConfigStatusCheck configStatusCheck) {
        return updateNonNullFieldByPrimaryKey(configStatusCheck, "id");
    }

    /**
     * 根据位置获取状态数据
     *
     * @param location 位置
     * @return
     */
    @DAOMethod
    public abstract List<ConfigStatusCheck> findByLocation(Integer location);

    /**
     * 根据位置 与 源状态 获取状态数据
     *
     * @param location 位置
     * @param source   源
     * @return
     */
    @DAOMethod
    public abstract List<ConfigStatusCheck> findByLocationAndSource(Integer location, Integer source);
}
