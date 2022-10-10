package recipe.dao;

import com.ngari.recipe.entity.DoctorDefault;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.log4j.Logger;
import recipe.dao.comment.ExtendDao;

import java.util.List;

/**
 * 医生默认数据
 *
 * @author fuzi
 */
@RpcSupportDAO
public abstract class DoctorDefaultDAO extends HibernateSupportDelegateDAO<DoctorDefault> implements ExtendDao<DoctorDefault> {

    private static Logger logger = Logger.getLogger(DoctorDefaultDAO.class);

    public DoctorDefaultDAO() {
        super();
        this.setEntityName(DoctorDefault.class.getName());
        this.setKeyField("id");
    }

    @Override
    public boolean updateNonNullFieldByPrimaryKey(DoctorDefault doctorDefault) {
        return updateNonNullFieldByPrimaryKey(doctorDefault, "id");
    }

    /**
     * 获取医生 默认数据
     * @param organId
     * @param doctorId
     * @return
     */
    @DAOMethod(sql = "from DoctorDefault where organId=:organId and doctorId=:doctorId and status = 0 order by id desc ")
    public abstract List<DoctorDefault> findByOrganAndDoctor(@DAOParam("organId") Integer organId, @DAOParam("doctorId") Integer doctorId);

    /**
     * 获取医生-类别默认数据
     * @param organId
     * @param doctorId
     * @param category
     * @return
     */
    @DAOMethod(sql = "from DoctorDefault where organId=:organId and doctorId=:doctorId and category=:category and status = 0 order by id desc ")
    public abstract List<DoctorDefault> findByOrganAndDoctorAndCategory(@DAOParam("organId") Integer organId, @DAOParam("doctorId") Integer doctorId, @DAOParam("category") Integer category);

}
