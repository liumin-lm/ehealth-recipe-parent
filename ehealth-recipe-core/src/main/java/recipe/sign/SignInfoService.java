package recipe.sign;

import com.ngari.recipe.entity.sign.SignDoctorCaInfo;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.sign.SignDoctorCaInfoDAO;

import java.util.Date;

@RpcBean
public class SignInfoService {

    @Autowired
    private SignDoctorCaInfoDAO signDoctorCaInfoDAO;

    @RpcService
    public void setSerCodeByDoctorId(Integer doctorId, String type, String serCode){
        SignDoctorCaInfo signDoctorCaInfo = signDoctorCaInfoDAO.getDoctorSerCodeByDoctorIdAndType(doctorId, type);

        if (null == signDoctorCaInfo) {
            SignDoctorCaInfo caInfo = new SignDoctorCaInfo();
            caInfo.setCaSerCode(serCode);
            caInfo.setDoctorId(doctorId);
            caInfo.setCaType(type);
            caInfo.setCreateDate(new Date());
            caInfo.setLastmodify(new Date());
            signDoctorCaInfoDAO.save(caInfo);
        } else {
            signDoctorCaInfo.setCaSerCode(serCode);
            signDoctorCaInfo.setLastmodify(new Date());
            signDoctorCaInfoDAO.update(signDoctorCaInfo);
        }
    }

    @RpcService
    public SignDoctorCaInfo getSignInfoByDoctorIdAndType(Integer doctorId, String type){
        return signDoctorCaInfoDAO.getDoctorSerCodeByDoctorIdAndType(doctorId, type);
    }
}
