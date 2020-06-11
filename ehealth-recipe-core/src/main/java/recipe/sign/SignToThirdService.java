package recipe.sign;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.ca.model.CaSignRequestTO;
import com.ngari.his.ca.model.CaSignResponseTO;
import com.ngari.his.ca.model.CertMsgBo;
import com.ngari.his.ca.service.ICaHisService;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.service.ThirdPartyMappingService;
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
 * @ClassName SignToThirdService
 * @Description
 * @Author maoLy
 * @Date 2020/6/4
 **/
@RpcBean
public class SignToThirdService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignToThirdService.class);

    private static ICaHisService iCaHisService = AppContextHolder.getBean("his.iCaHisService",ICaHisService.class);

    @RpcService
    public Map<String,Object> getCaSignToThird(ParamToThirdDTO paramToThird){
        Map<String,Object> returnMap = new HashMap<>();
        if(paramToThird != null){
            String tid = paramToThird.getTid();
            String thirdParty = paramToThird.getThirdParty();
            String signMsg = paramToThird.getSignMsg();
            //String cretMsg = paramToThird.getCretMsg();
            String bussNo = paramToThird.getBussNo();
            LOGGER.info("SignToThirdService.getCaSignToThird，tid={},thirdParty={},signMsg={},bussNo={}",tid,thirdParty,signMsg,bussNo);
            if(StringUtils.isBlank(tid)){
                throw new DAOException(DAOException.VALUE_NEEDED, "tid is null");
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
            ThirdPartyMappingService ThirdPartyMappingService = BasicAPI.getService(ThirdPartyMappingService.class);
            //ThirdPartyMapping thirdPartyMapping = ThirdPartyMappingService.getByThirdpartyAndTid(thirdParty,tid);
            DoctorDTO doctorDTO = ThirdPartyMappingService.getDoctorByThirdpartyAndTid(thirdParty,tid);
            String userAccount = doctorDTO.getLoginId();
            Integer organId = doctorDTO.getOrgan();
            EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
            List<String> jobNumbers = employmentService.findJobNumberByDoctorIdAndOrganId(doctorDTO.getDoctorId(), organId);
            CaSignRequestTO requestTO = new CaSignRequestTO();
            requestTO.setUserAccount(userAccount);
            requestTO.setCretMsg(null);
            requestTO.setSignMsg(signMsg);
            requestTO.setOrganId(organId);
            if (!CollectionUtils.isEmpty(jobNumbers)) {
                requestTO.setJobnumber(jobNumbers.get(0));
            }
            LOGGER.info("SignToThirdService getCaSignToThird toHis start requestTO={}",JSONUtils.toString(requestTO));
            HisResponseTO<CaSignResponseTO> responseTO = iCaHisService.caSignBusiness(requestTO);
            LOGGER.info("SignToThirdService getCaSignToThird toHis end responseTO={}",JSONUtils.toString(responseTO));
            String signValue = "";
            if ("200".equals(responseTO.getMsgCode())) {
                signValue = responseTO.getData().getSignValue();
                saveCaSign(doctorDTO,signValue,paramToThird);
            }
            returnMap.put("signValue",signValue);
        }
        return returnMap;
    }

    private void saveCaSign(DoctorDTO doctorDTO,String signValue,ParamToThirdDTO paramToThird){
        SignDoctorCaInfo signDoctorCaInfo = new SignDoctorCaInfo();
        signDoctorCaInfo.setDoctorId(doctorDTO.getDoctorId());
        signDoctorCaInfo.setCaType("shanghaiCA");
        signDoctorCaInfo.setCert_voucher(signValue);
        signDoctorCaInfo.setCreateDate(new Date());
        signDoctorCaInfo.setLastmodify(new Date());
        signDoctorCaInfo.setName(doctorDTO.getName());
        signDoctorCaInfo.setIdcard(doctorDTO.getIdNumber());
        SignDoctorCaInfoDAO SignDoctorCaInfoDAO = DAOFactory.getDAO(SignDoctorCaInfoDAO.class);
        SignDoctorCaInfoDAO.save(signDoctorCaInfo);

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