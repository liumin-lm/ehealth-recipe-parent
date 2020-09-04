package recipe.dao;

import com.ngari.recipe.entity.DoctorDrugUsageCount;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * @author Created by liuxiaofeng on 2020/9/3.
 */
@RpcSupportDAO
public abstract class DoctorDrugUsageCountDAO extends HibernateSupportDelegateDAO<DoctorDrugUsageCount> {

    public DoctorDrugUsageCountDAO() {
        super();
        this.setEntityName(DoctorDrugUsageCount.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = "from DoctorDrugUsageCount where organ_id =:organId and doctor_id = :doctorId and usage_type = :usageType order by usage_count desc")
    public abstract List<DoctorDrugUsageCount> findByUsageTypeForDoctor(@DAOParam("organId") Integer organId, @DAOParam("doctorId") Integer doctorId, @DAOParam("usageType") Integer usageType);

    @DAOMethod(sql = "from DoctorDrugUsageCount where organ_id =:organId and doctor_id = :doctorId and usage_type = :usageType and usage_id = :usageId")
    public abstract DoctorDrugUsageCount getDoctorUsage(@DAOParam("organId") Integer organId, @DAOParam("doctorId") Integer doctorId, @DAOParam("usageType") Integer usageType, @DAOParam("usageId") Integer usageId);

    @DAOMethod(sql = "update DoctorDrugUsageCount set usage_count = :usageCount where id = :id")
    public abstract void updateUsageCountById(@DAOParam("id") Integer id, @DAOParam("usageCount") Integer usageCount);
}
