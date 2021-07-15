package recipe.sign;

import com.alibaba.fastjson.JSONObject;
import com.ngari.base.BaseAPI;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.ca.api.service.ICaRemoteService;
import com.ngari.ca.api.vo.CaSignResultBean;
import com.ngari.ca.api.vo.CommonSignRequest;
import com.ngari.ca.api.vo.SealRequestBean;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.ca.model.CaAccountRequestTO;
import com.ngari.his.ca.model.CaAccountResponseTO;
import com.ngari.his.ca.model.CaSignRequestTO;
import com.ngari.his.ca.model.CaSignResponseTO;
import com.ngari.his.ca.service.ICaHisService;
import com.ngari.his.regulation.entity.RegulationRecipeIndicatorsReq;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.sign.SignDoctorCaInfo;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.sign.ISignInfoService;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
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
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.sign.SignDoctorCaInfoDAO;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.service.RecipeCAService;
import recipe.manager.EmrRecipeManager;

import java.util.*;

/**
 * 已迁移到CA 兼容老app 后续删除
 */
@Deprecated
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
            result.put("uniqueId",response.getMsg());
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

    /**
     * 信步云根据签名id获取医生签名状态
     * WAITING_USER_SIGN：任务尚未签署
     * USER_SIGN_FINISH：任务签署完成，完成状态才能获取到签名结果
     * TIMEOUT：任务超期，已经无法签署
     * @param signId
     * @param organId
     * @return
     */
    @RpcService
    public String getSignStatus(String signId, Integer organId) {
        IConfigurationCenterUtilsService configurationService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        String thirdCASign = (String) configurationService.getConfiguration(organId, "thirdCASign");
        if("bjNingxiaCA".equals(thirdCASign)){
            ICaRemoteService caRemoteService = AppDomainContext.getBean("mi.caRemoteService", ICaRemoteService.class);
            CommonSignRequest commonSignRequest = new CommonSignRequest();
            SealRequestBean sealRequestBean= new SealRequestBean();
            sealRequestBean.setSignId(signId);
            commonSignRequest.setOrganId(organId);
            commonSignRequest.setSealRequestBean(sealRequestBean);
            CaSignResultBean caSignResultBean = caRemoteService.commonCaSignAndSeal(commonSignRequest);
            if (caSignResultBean != null && StringUtils.isNotBlank(caSignResultBean.getSignCode())){
                return "USER_SIGN_FINISH";
            }else {
                return "WAITING_USER_SIGN";
            }
        }else {


            CaSignRequestTO request = new CaSignRequestTO();
            request.setOrganId(organId);
            request.setSignId(signId);
            logger.info("getSignStatus request info=[{}]", JSONUtils.toString(request));
            ICaHisService iCaHisService = AppContextHolder.getBean("his.iCaHisService", ICaHisService.class);
            HisResponseTO<CaSignResponseTO> response = iCaHisService.caSignBusiness(request);
            logger.info("getSignStatus respose info=[{}]", JSONUtils.toString(response));
            if (null != response && "200".equals(response.getMsgCode())) {
                return response.getData().getSignStatus();
            }
            else {
                throw new DAOException(609,response.getMsg());
            }
        }



    }
}
