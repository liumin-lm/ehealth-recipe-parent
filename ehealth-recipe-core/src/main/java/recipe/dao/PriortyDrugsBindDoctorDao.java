package recipe.dao;

import com.ngari.recipe.entity.PriortyDrugBindDoctor;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * 重点药品表
 * Created by jiangtingfeng on 2017/10/23.
 */
@RpcSupportDAO
public abstract class PriortyDrugsBindDoctorDao extends HibernateSupportDelegateDAO<PriortyDrugBindDoctor>{

    public PriortyDrugsBindDoctorDao() {
        super();
        this.setEntityName(PriortyDrugBindDoctor.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = "select doctorId from PriortyDrugBindDoctor where drugId=:drugId")
    public abstract List<Integer> findPriortyDrugBindDoctors(@DAOParam("drugId") Integer drugId);
}



