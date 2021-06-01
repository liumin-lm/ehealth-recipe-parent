package recipe.sign;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.ca.model.CaSignRequestTO;
import com.ngari.his.ca.model.CaSignResponseTO;
import com.ngari.his.ca.model.CertMsgBo;
import com.ngari.his.ca.service.ICaHisService;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.*;
import com.ngari.recipe.entity.sign.SignDoctorCaInfo;
import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
import com.ngari.recipe.logistics.model.RecipeLogisticsBean;
import com.ngari.recipe.sign.model.ParamToThirdDTO;
import ctd.account.thirdparty.ThirdPartyMapping;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import recipe.dao.sign.SignDoctorCaInfoDAO;
import recipe.dao.sign.SignDoctorRecipeInfoDAO;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 已迁移到CA 兼容老app 后续删除
 */
@Deprecated
@RpcBean
public class SignToThirdService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignToThirdService.class);

    private static ICaHisService iCaHisService = AppContextHolder.getBean("his.iCaHisService",ICaHisService.class);

    @RpcService
    public Map<String,Object> getCaSignToThird(ParamToThirdDTO paramToThird){
        Map<String,Object> returnMap = new HashMap<>();
        if(paramToThird != null){
            String jobNumber = paramToThird.getJobNumber();
            String thirdParty = paramToThird.getThirdParty();
            String signMsg = paramToThird.getSignMsg();
            String organizeCode = paramToThird.getOrganizeCode();
            //String cretMsg = paramToThird.getCretMsg();
            String bussNo = paramToThird.getBussNo();
            LOGGER.info("SignToThirdService.getCaSignToThird，jobNumber={},thirdParty={},signMsg={},bussNo={},organizeCode={}",jobNumber,thirdParty,signMsg,bussNo,organizeCode);
            if(StringUtils.isBlank(jobNumber)){
                throw new DAOException(DAOException.VALUE_NEEDED, "jobNumber is null");
            }
            if(StringUtils.isBlank(thirdParty)){
                throw new DAOException(DAOException.VALUE_NEEDED, "thirdParty is null");
            }
            if(StringUtils.isBlank(signMsg)){
                throw new DAOException(DAOException.VALUE_NEEDED, "signMsg is null");
            }
            if(StringUtils.isBlank(bussNo)){
                throw new DAOException(DAOException.VALUE_NEEDED, "bussNo is null");
            }
            //ThirdPartyMappingService ThirdPartyMappingService = BasicAPI.getService(ThirdPartyMappingService.class);
            //ThirdPartyMapping thirdPartyMapping =
            OrganService organService = BasicAPI.getService(OrganService.class);
            OrganDTO organDTO = organService.getOrganByOrganizeCode(organizeCode);
            EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
            EmploymentDTO employmentDTO = employmentService.getByJobNumberAndOrganId(jobNumber,organDTO.getOrganId());
            if(employmentDTO == null){
                throw new DAOException(DAOException.VALUE_NEEDED, "未在该机构下找到该医生");
            }
            DoctorService doctorService = BasicAPI.getService(DoctorService.class);
            DoctorDTO doctorDTO = doctorService.getByDoctorId(employmentDTO.getDoctorId());
            String userAccount = doctorDTO.getIdNumber();
            Integer organId = doctorDTO.getOrgan();

            CaSignRequestTO requestTO = new CaSignRequestTO();
            requestTO.setUserAccount(userAccount);
            requestTO.setCretMsg(null);
            requestTO.setSignMsg(signMsg);
            requestTO.setOrganId(organId);
            requestTO.setJobnumber(jobNumber);

            LOGGER.info("SignToThirdService getCaSignToThird toHis start requestTO={}",JSONUtils.toString(requestTO));
            HisResponseTO<CaSignResponseTO> responseTO = iCaHisService.caSignBusiness(requestTO);
            LOGGER.info("SignToThirdService getCaSignToThird toHis end responseTO={}",JSONUtils.toString(responseTO));
            String signValue = "";
            if (responseTO != null && "200".equals(responseTO.getMsgCode())) {
                signValue = responseTO.getData().getSignValue();
                saveCaSign(doctorDTO,signValue,paramToThird);
            }
            returnMap.put("signValue",signValue);

        }
        return returnMap;
    }

    private void saveCaSign(DoctorDTO doctorDTO,String signValue,ParamToThirdDTO paramToThird){
        /*SignDoctorCaInfo signDoctorCaInfo = new SignDoctorCaInfo();
        signDoctorCaInfo.setDoctorId(doctorDTO.getDoctorId());
        signDoctorCaInfo.setCaType("shanghaiCA");
        signDoctorCaInfo.setCert_voucher(signValue);
        signDoctorCaInfo.setCreateDate(new Date());
        signDoctorCaInfo.setLastmodify(new Date());
        signDoctorCaInfo.setName(doctorDTO.getName());
        signDoctorCaInfo.setIdcard(doctorDTO.getIdNumber());
        SignDoctorCaInfoDAO SignDoctorCaInfoDAO = DAOFactory.getDAO(SignDoctorCaInfoDAO.class);
        SignDoctorCaInfoDAO.save(signDoctorCaInfo);*/

        SignDoctorRecipeInfo signDoctorRecipeInfo = new SignDoctorRecipeInfo();
        signDoctorRecipeInfo.setCreateDate(new Date());
        signDoctorRecipeInfo.setLastmodify(new Date());
        signDoctorRecipeInfo.setRecipeId(Integer.parseInt(paramToThird.getBussNo()));
        signDoctorRecipeInfo.setServerType(1);
        signDoctorRecipeInfo.setType("shanghaiCA");
        signDoctorRecipeInfo.setSignBefText(paramToThird.getSignMsg());
        signDoctorRecipeInfo.setSignCodeDoc(signValue);
        SignDoctorRecipeInfoDAO signDoctorRecipeInfoDAO = DAOFactory.getDAO(SignDoctorRecipeInfoDAO.class);
        signDoctorRecipeInfoDAO.save(signDoctorRecipeInfo);

    }
}