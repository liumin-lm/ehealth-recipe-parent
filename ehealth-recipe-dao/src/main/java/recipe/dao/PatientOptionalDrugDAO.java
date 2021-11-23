package recipe.dao;

import com.ngari.recipe.entity.PatientOptionalDrug;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;


/**
 * @description： 患者自选药品 dao
 * @author： whf
 * @date： 2021-11-22 20:00
 */
@RpcSupportDAO
public abstract class PatientOptionalDrugDAO extends HibernateSupportDelegateDAO<PatientOptionalDrug> {

    /**
     * 根据复诊id获取患者自选药品
     *
     * @param clinicId
     * @return
     */
    @DAOMethod(sql = "from PatientOptionalDrug where clinicId= :clinicId ", limit = 0)
    public abstract List<PatientOptionalDrug> findPatientOptionalDrugByClinicId(@DAOParam("clinicId") Integer clinicId);
}
