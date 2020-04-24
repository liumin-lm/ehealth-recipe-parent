package recipe.dao.sign;

import com.ngari.recipe.entity.sign.SignDoctorCaInfo;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

@RpcSupportDAO
public abstract class SignDoctorCaInfoDAO extends HibernateSupportDelegateDAO<SignDoctorCaInfo> {

    public SignDoctorCaInfoDAO(){
        super();
        this.setEntityName(SignDoctorCaInfo.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = " from SignDoctorCaInfo where doctorId=:doctorId and caType=:caType")
    public abstract SignDoctorCaInfo getDoctorSerCodeByDoctorIdAndType(@DAOParam("doctorId")Integer doctorId, @DAOParam("caType")String caType);
}

