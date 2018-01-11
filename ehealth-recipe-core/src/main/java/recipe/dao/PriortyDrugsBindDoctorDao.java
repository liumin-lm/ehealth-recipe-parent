package recipe.dao;

import com.ngari.recipe.entity.PriortyDrugBindDoctor;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * 重点药品表
 * @author jiangtingfeng
 */
@RpcSupportDAO
public abstract class PriortyDrugsBindDoctorDao extends HibernateSupportDelegateDAO<PriortyDrugBindDoctor>{

    public PriortyDrugsBindDoctorDao() {
        super();
        this.setEntityName(PriortyDrugBindDoctor.class.getName());
        this.setKeyField("id");
    }

    /**
     * 根据药品id查询医生id列表
     * @param drugId
     * @return
     */
    @DAOMethod(sql = "select doctorId from PriortyDrugBindDoctor where drugId=:drugId")
    public abstract List<Integer> findPriortyDrugBindDoctors(@DAOParam("drugId") Integer drugId);
}



