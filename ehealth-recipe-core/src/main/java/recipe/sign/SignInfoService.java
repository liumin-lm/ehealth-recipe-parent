package recipe.sign;

import com.alibaba.fastjson.JSONObject;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.ca.model.CaAccountRequestTO;
import com.ngari.his.ca.model.CaAccountResponseTO;
import com.ngari.his.ca.service.ICaHisService;
import com.ngari.his.regulation.entity.RegulationRecipeIndicatorsReq;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.sign.SignDoctorCaInfo;
import com.ngari.recipe.sign.ISignInfoService;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.params.ParamUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.ca.ICommonCAServcie;
import recipe.dao.RecipeDAO;
import recipe.dao.sign.SignDoctorCaInfoDAO;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.util.RedisClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RpcBean
public class SignInfoService implements ISignInfoService {

    private static final Logger logger = LoggerFactory.getLogger(SignInfoService.class);

    @Autowired
    private SignDoctorCaInfoDAO signDoctorCaInfoDAO;

    @Autowired
    private RecipeDAO recipeDAO;

    @Autowired
    private DoctorService doctorService;

    @RpcService
    @Override
    public void setSerCodeAndEndDateByDoctorId(Integer doctorId, String type, String serCode, Date caEndTime){
        SignDoctorCaInfo signDoctorCaInfo = signDoctorCaInfoDAO.getDoctorSerCodeByDoctorIdAndType(doctorId, type);
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
        if (null == signDoctorCaInfo) {
            SignDoctorCaInfo caInfo = new SignDoctorCaInfo();
            caInfo.setCaSerCode(serCode);
            caInfo.setDoctorId(doctorId);
            caInfo.setCaType(type);
            caInfo.setCreateDate(new Date());
            caInfo.setLastmodify(new Date());
            caInfo.setCaEndTime(caEndTime);
            caInfo.setName(doctorDTO.getName());
            caInfo.setIdcard(doctorDTO.getIdNumber());
            signDoctorCaInfoDAO.save(caInfo);
        } else {
            signDoctorCaInfo.setCaSerCode(serCode);
            signDoctorCaInfo.setLastmodify(new Date());
            signDoctorCaInfo.setCaEndTime(caEndTime);
            signDoctorCaInfo.setName(doctorDTO.getName());
            signDoctorCaInfo.setIdcard(doctorDTO.getIdNumber());
            signDoctorCaInfoDAO.update(signDoctorCaInfo);
        }
    }

    @RpcService
    public void setSerCodeByDoctorId(Integer doctorId, String type, String serCode){
        setSerCodeAndEndDateByDoctorId(doctorId, type, serCode, null);
    }

    @RpcService
    public SignDoctorCaInfo getSignInfoByDoctorIdAndType(Integer doctorId, String type){
        return signDoctorCaInfoDAO.getDoctorSerCodeByDoctorIdAndType(doctorId, type);
    }

    @RpcService
    public String getTaskCode(Integer recipeId,Integer doctorId,boolean isDoctor){
        logger.info("getTaskCode info recipeId={}=doctorId={}=isDoctor={}=", recipeId , doctorId,isDoctor);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);

        HisSyncSupervisionService service = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
        List<RegulationRecipeIndicatorsReq> request = new ArrayList<>();
        service.splicingBackRecipeData(Arrays.asList(recipe),request);

        CaAccountRequestTO caAccountRequestTO = new CaAccountRequestTO();
        caAccountRequestTO.setOrganId(doctorDTO.getOrgan());
        caAccountRequestTO.setRegulationRecipeIndicatorsReq(request);
        caAccountRequestTO.setBusType(isDoctor?4:5);
        ICaHisService iCaHisService = AppContextHolder.getBean("his.iCaHisService",ICaHisService.class);
        HisResponseTO<CaAccountResponseTO> responseTO = iCaHisService.caUserBusiness(caAccountRequestTO);
        logger.info("getTaskCode result info={}=", JSONObject.toJSONString(responseTO));
        if ("200".equals(responseTO.getMsgCode())) {
            return responseTO.getData().getMsg();
        }
        return null;
    }

    @RpcService
    public String getUserCode(Integer doctorId) {
        logger.info("getUserCode doctorId={}=", doctorId);
        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);

        CaAccountRequestTO caAccountRequestTO = new CaAccountRequestTO();
        caAccountRequestTO.setOrganId(doctorDTO.getOrgan());
        caAccountRequestTO.setUserName(doctorDTO.getName());
        caAccountRequestTO.setIdCard(doctorDTO.getIdNumber());
        caAccountRequestTO.setMobile(doctorDTO.getMobile());
        caAccountRequestTO.setBusType(6);
        ICaHisService iCaHisService = AppContextHolder.getBean("his.iCaHisService",ICaHisService.class);
        HisResponseTO<CaAccountResponseTO> responseTO = iCaHisService.caUserBusiness(caAccountRequestTO);
        logger.info("getUserCode result info={}=", JSONObject.toJSONString(responseTO));
        if ("200".equals(responseTO.getMsgCode())) {
            return responseTO.getData().getMsg();
        }
        return null;
    }

}
