package recipe.sign;

import com.alibaba.fastjson.JSONObject;
import com.ngari.base.BaseAPI;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.his.ca.model.CaAccountRequestTO;
import com.ngari.his.ca.model.CaAccountResponseTO;
import com.ngari.his.ca.service.ICaHisService;
import com.ngari.his.regulation.entity.RegulationRecipeDetailIndicatorsReq;
import com.ngari.his.regulation.entity.RegulationRecipeIndicatorsReq;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.sign.SignDoctorCaInfo;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import com.ngari.recipe.sign.ISignInfoService;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.ca.impl.BeijingYwxCAImpl;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.sign.SignDoctorCaInfoDAO;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.service.RecipeCAService;
import recipe.service.RecipeService;
import recipe.service.manager.EmrRecipeManager;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RpcBean
public class SignInfoService implements ISignInfoService {

    private static final Logger logger = LoggerFactory.getLogger(SignInfoService.class);

    @Autowired
    private SignDoctorCaInfoDAO signDoctorCaInfoDAO;

    @Autowired
    private RecipeDAO recipeDAO;

    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    @Autowired
    private DoctorService doctorService;
    @Autowired
    private EmrRecipeManager emrRecipeManager;

    @RpcService
    @Override
    public void setSerCodeAndEndDateByDoctorId(Integer doctorId, String type, String serCode, Date caEndTime) {
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
    public String getTaskCode(Integer recipeId,Integer doctorId, boolean isDoctor){
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
    public String getTaskCode2(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList, boolean isDoctor){
//        logger.info("getTaskCode2 info RecipeBean={}=detailBeanList={}=", JSONUtils.toString(recipeBean) , JSONUtils.toString(detailBeanList));
//        BeijingYwxCAImpl beijingYwxCA = AppContextHolder.getBean("BeijingYCA", BeijingYwxCAImpl.class);
//        RecipeCAService recipeCAService = ApplicationUtils.getRecipeService(RecipeCAService.class);
//        RegulationRecipeIndicatorsReq request = null;
//        request = recipeCAService.getCATaskRecipeReq(recipeBean, detailBeanList);
//        logger.info("getTaskCode2 组装的处方对象{}", JSONUtils.toString(request));
//        CaAccountRequestTO caAccountRequestTO = new CaAccountRequestTO();
//        IConfigurationCenterUtilsService configurationService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
//        String thirdCASign = (String) configurationService.getConfiguration(recipeBean.getClinicOrgan(), "thirdCASign");
//        if ("bjYwxCA".equals(thirdCASign)) {
//            String token = beijingYwxCA.caTokenBussiness(recipeBean.getClinicOrgan());
//            String openId = beijingYwxCA.getDocStatusForPC(recipeBean.getClinicOrgan(),recipeBean.getDoctor()).getUserAccount();
//            caAccountRequestTO.setUserName(token);
//            caAccountRequestTO.setUserAccount(StringUtils.isNotEmpty(recipeBean.getCaPassword()) ? recipeBean.getCaPassword():openId);
//        }
//        else {
//            caAccountRequestTO.setUserAccount(recipeBean.getCaPassword());
//        }
//        caAccountRequestTO.setOrganId(recipeBean.getClinicOrgan());
//        caAccountRequestTO.setBusType(isDoctor?4:5);
//        caAccountRequestTO.setRegulationRecipeIndicatorsReq(Arrays.asList(request));
//        logger.info("getTaskCode2 request info={}=", JSONUtils.toString(caAccountRequestTO));
//        ICaHisService iCaHisService = AppContextHolder.getBean("his.iCaHisService",ICaHisService.class);
//        HisResponseTO<CaAccountResponseTO> responseTO = iCaHisService.caUserBusiness(caAccountRequestTO);
//        logger.info("getTaskCode2 result info={}=", JSONObject.toJSONString(responseTO));
//        if ("-1".equals(responseTO.getMsgCode()) && null != responseTO.getData()){
//
//            throw new DAOException(609,responseTO.getData().getMsg());
//        }
        CaAccountResponseTO result = synBussData(recipeBean, detailBeanList, isDoctor);
        if (result != null){
            return result.getMsg();
        }
        else {
            logger.error("前置机未返回数据");
        }
        return null;
    }

    /**
     * 同步数据得到签名码 新增是否开启自动签名返回
     * @param recipeBean
     * @param detailBeanList
     * @param isDoctor
     * @return
     */
    @RpcService
    public Map<String,Object> getTaskCodeNew(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList, boolean isDoctor){
        CaAccountResponseTO response = synBussData(recipeBean, detailBeanList, isDoctor);
        Map<String,Object> result = new HashMap<>();
        if (response != null){
            result.put("code",response.getMsg());
            result.put("selfSignStatus",response.getUserAccount());
        }
        else {
            logger.error("前置机未返回数据");
        }
        return result;
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

    /**
     * 同步医生数据 北京信步云和医网信
     * @param recipeBean
     * @param detailBeanList
     * @param isDoctor
     * @return
     */
    public CaAccountResponseTO synBussData(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList, boolean isDoctor){
        logger.info("getTaskCode2 info RecipeBean={}=detailBeanList={}=", JSONUtils.toString(recipeBean) , JSONUtils.toString(detailBeanList));
        BeijingYwxCAImpl beijingYwxCA = AppContextHolder.getBean("BeijingYCA", BeijingYwxCAImpl.class);
        RecipeCAService recipeCAService = ApplicationUtils.getRecipeService(RecipeCAService.class);
        RegulationRecipeIndicatorsReq request = null;
        request = recipeCAService.getCATaskRecipeReq(recipeBean, detailBeanList);
        logger.info("getTaskCode2 组装的处方对象{}", JSONUtils.toString(request));
        CaAccountRequestTO caAccountRequestTO = new CaAccountRequestTO();
        IConfigurationCenterUtilsService configurationService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        String thirdCASign = (String) configurationService.getConfiguration(recipeBean.getClinicOrgan(), "thirdCASign");
        if ("bjYwxCA".equals(thirdCASign)) {
            String token = beijingYwxCA.caTokenBussiness(recipeBean.getClinicOrgan());
            String openId = beijingYwxCA.getDocStatusForPC(recipeBean.getClinicOrgan(),recipeBean.getDoctor()).getUserAccount();
            caAccountRequestTO.setUserName(token);
            caAccountRequestTO.setUserAccount(StringUtils.isNotEmpty(recipeBean.getCaPassword()) ? recipeBean.getCaPassword():openId);
        }
        else {
            caAccountRequestTO.setUserAccount(recipeBean.getCaPassword());
        }
        caAccountRequestTO.setOrganId(recipeBean.getClinicOrgan());
        caAccountRequestTO.setBusType(isDoctor?4:5);
        caAccountRequestTO.setRegulationRecipeIndicatorsReq(Arrays.asList(request));
        logger.info("getTaskCode2 request info={}=", JSONUtils.toString(caAccountRequestTO));
        ICaHisService iCaHisService = AppContextHolder.getBean("his.iCaHisService",ICaHisService.class);
        HisResponseTO<CaAccountResponseTO> responseTO = iCaHisService.caUserBusiness(caAccountRequestTO);
        logger.info("getTaskCode2 result info={}=", JSONObject.toJSONString(responseTO));
        if ("-1".equals(responseTO.getMsgCode()) && null != responseTO.getData()){

            throw new DAOException(609,responseTO.getData().getMsg());
        }
        CaAccountResponseTO response = new CaAccountResponseTO();
        if (null != responseTO && "200".equals(responseTO.getMsgCode())){
            response = responseTO.getData();
        }
        return response;
    }

}
