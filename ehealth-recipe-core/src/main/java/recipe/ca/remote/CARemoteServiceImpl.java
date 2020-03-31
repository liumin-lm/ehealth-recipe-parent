package recipe.ca.remote;

import com.alibaba.fastjson.JSONObject;
import com.ngari.his.ca.model.CaPasswordRequestTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.DoctorService;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.ca.CAInterface;
import recipe.ca.factory.CommonCAFactory;

import java.util.Date;

@RpcBean(value="iCARemoteService", mvc_authentication = false)
public class CARemoteServiceImpl implements ICARemoteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CARemoteServiceImpl.class);

    private DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);

    /**
     * CA用户注册、申请证书接口
     * @param doctorId
     * @return
     */
    @Override
    @RpcService
    public boolean caUserLoginAndGetCertificate(Integer doctorId) {
        LOGGER.info("CARemoteServiceImpl caUserLoginAndGetCertificate start in doctorId={}", doctorId);
        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
        CommonCAFactory caFactory = new CommonCAFactory();
        //通过工厂获取对应的实现CA类
        CAInterface caInterface = caFactory.useCAFunction(doctorDTO.getOrgan());
        if (caInterface != null) {
            return caInterface.caUserLoginAndGetCertificate(doctorId);
        }
        return false;
    }

    /**
     * CA密码接口
     * @return
     */
    @Override
    @RpcService
    public boolean caPasswordBusiness(Integer doctorId,String password,String newPassword,int busType) {
        LOGGER.info("CARemoteServiceImpl caPasswordBusiness start in doctorId={},password={},newPassword={},busType={}", doctorId);
        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
        CaPasswordRequestTO requestTO = new CaPasswordRequestTO();
        requestTO.setBusType(busType);
        requestTO.setNewPassword(newPassword);
        requestTO.setOrganId(doctorDTO.getOrgan());
        requestTO.setUserAccount(doctorDTO.getIdNumber());
        requestTO.setPassword(password);
        CommonCAFactory caFactory = new CommonCAFactory();
        CAInterface caInterface = caFactory.useCAFunction(doctorDTO.getOrgan());
        if (caInterface != null) {
            return caInterface.caPasswordBusiness(requestTO);
        }
        return false;
    }

    @RpcService
    public Date getSystemTime() {
        LOGGER.info("getSystemTime start");
        Date date = new Date();
        LOGGER.info("getSystemTime end  date={}",date);
        return date;
    }

    /**
     * 仅供肺科医院 测评使用
     * @param name
     * @return
     */
    @Deprecated
    @RpcService
    public String getDoctorInfo(String name) {
        JSONObject json = new JSONObject();
        switch (name){
            case "便民门诊":
                json.put("certSubject","李爱武");
                json.put("certIssuer","Mkey Root CA");
                json.put("startDate","2020-03-24 17:07:35");
                json.put("endDate","2020-06-24 17:07:35");
                break;
            case "陈玮俊":
                json.put("certSubject","陈玮俊");
                json.put("certIssuer","Mkey Root CA");
                json.put("startDate","2020-03-31 15:50:17");
                json.put("endDate","2020-06-31 15:50:17");
                break;
            case "李爱武":
                json.put("certSubject","李爱武");
                json.put("certIssuer","Mkey Root CA");
                json.put("startDate","2020-03-24 17:07:35");
                json.put("endDate","2020-06-24 17:07:35");
                break;
        }
        return json.toJSONString();
    }
}
