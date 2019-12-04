package recipe.dao;

import com.ngari.recipe.entity.MedicationGuide;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

/**
 * created by shiyuping on 2019/10/25
 */
@RpcSupportDAO
public abstract class MedicationGuideDAO extends HibernateSupportDelegateDAO<MedicationGuide> {
    public MedicationGuideDAO() {
        super();
        this.setEntityName(MedicationGuide.class.getName());
        this.setKeyField("guideId");
    }

    @DAOMethod
    public abstract MedicationGuide getByGuideId(Integer id);

    @DAOMethod
    public abstract MedicationGuide getByCallSys(String callSys);
}
