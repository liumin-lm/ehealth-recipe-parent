package recipe.ca.remote;

import com.alibaba.fastjson.JSONObject;
import com.ngari.his.ca.model.CaPasswordRequestTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.EmploymentService;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.ca.CAInterface;
import recipe.ca.factory.CommonCAFactory;
import recipe.ca.impl.ShenzhenImp;
import recipe.ca.vo.CaSignResultVo;
import recipe.dao.RecipeDAO;

import java.util.Date;

@RpcBean(value="iCARemoteService", mvc_authentication = false)
public class CARemoteServiceImpl implements ICARemoteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CARemoteServiceImpl.class);

    private DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);

    private EmploymentService employmentService = ApplicationUtils.getBasicService(EmploymentService.class);

    @Autowired
    private CommonCAFactory commonCAFactory;

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
//        CommonCAFactory caFactory = new CommonCAFactory();
        //通过工厂获取对应的实现CA类
        CAInterface caInterface = commonCAFactory.useCAFunction(doctorDTO.getOrgan());
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
        LOGGER.info("CARemoteServiceImpl caPasswordBusiness start in doctorId={},password={},newPassword={},busType={}", doctorId,password,newPassword,busType);
        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
        CaPasswordRequestTO requestTO = new CaPasswordRequestTO();
        requestTO.setBusType(busType);
        requestTO.setNewPassword(newPassword);
        requestTO.setOrganId(doctorDTO.getOrgan());
        requestTO.setUserAccount(doctorDTO.getIdNumber());
        requestTO.setPassword(password);
//        CommonCAFactory caFactory = new CommonCAFactory();
        CAInterface caInterface = commonCAFactory.useCAFunction(doctorDTO.getOrgan());
        if(caInterface instanceof ShenzhenImp){
            EmploymentDTO employmentDTO =employmentService.getByDoctorIdAndOrganId(doctorId,doctorDTO.getOrgan());
            requestTO.setUserAccount(employmentDTO.getJobNumber());
        }
        if (caInterface != null) {
            return caInterface.caPasswordBusiness(requestTO);
        }
        return false;
    }


//    @Override
//    public CaSignResultVo commonCASignAndSeal(Integer doctorId, Integer bussId, Integer bussType) {
//        LOGGER.info("CARemoteServiceImpl caPasswordBusiness start in doctorId={},bussId={}，bussType={}", doctorId, bussId, bussType);
//        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
//        // 目前先支持recipe 后期加入其他业务
//        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//        Recipe recipe = recipeDAO.get(bussId);
//        EmploymentDTO employmentDTO =employmentService.getByDoctorIdAndOrganId(doctorId,doctorDTO.getOrgan());
//        CAInterface caInterface = commonCAFactory.useCAFunction(doctorDTO.getOrgan());
//        if (caInterface != null) {
//            return caInterface.commonCASignAndSeal(null,recipe,doctorDTO.getOrgan(),employmentDTO.getJobNumber(),null);
//        }
//        return null;
//    }

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
