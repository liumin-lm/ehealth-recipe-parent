package recipe.dao;

import com.ngari.recipe.entity.DoctorCommonPharmacy;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.log4j.Logger;
import recipe.dao.comment.ExtendDao;

import java.util.List;

/**
 * 药房dao
 *
 * @author liumin
 */
@RpcSupportDAO
public abstract class DoctorCommonPharmacyDAO extends HibernateSupportDelegateDAO<DoctorCommonPharmacy> implements ExtendDao<DoctorCommonPharmacy> {

    private static Logger logger = Logger.getLogger(DoctorCommonPharmacyDAO.class);

    public DoctorCommonPharmacyDAO() {
        super();
        this.setEntityName(DoctorCommonPharmacy.class.getName());
        this.setKeyField("id");
    }

    @Override
    public boolean updateNonNullFieldByPrimaryKey(DoctorCommonPharmacy doctorCommonPharmacy) {
        return updateNonNullFieldByPrimaryKey(doctorCommonPharmacy, "id");
    }

    @DAOMethod(sql = "from DoctorCommonPharmacy where organId=:organId and doctorId=:doctorId order by updateTime desc " ,limit =0)
    public abstract List<DoctorCommonPharmacy> findByOrganIdAndDoctorId(@DAOParam("organId") Integer organId, @DAOParam("doctorId") Integer doctorId);

    @DAOMethod(sql = "from DoctorCommonPharmacy where organId=:organId " ,limit =0)
    public abstract List<DoctorCommonPharmacy> findByOrganIdAndDoctorId2(@DAOParam("organId")  Integer organId);


}
