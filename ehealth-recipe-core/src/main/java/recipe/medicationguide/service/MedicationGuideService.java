package recipe.medicationguide.service;

import com.google.common.collect.Lists;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.MedicationGuide;
import com.ngari.recipe.entity.OrganMedicationGuideRelation;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.hisprescription.model.HospitalDrugDTO;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.schema.exception.ValidateException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.ErrorCode;
import recipe.dao.MedicationGuideDAO;
import recipe.dao.OrganMedicationGuideRelationDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.hisservice.RecipeToHisService;
import recipe.medicationguide.bean.PatientInfoDTO;
import recipe.service.RecipeMsgService;
import recipe.util.ChinaIDNumberUtil;
import recipe.util.DateConversion;

import java.util.List;
import java.util.Map;


/**
 * 用药指导服务入口
 * created by shiyuping on 2019/10/25
 * @author shiyuping
 */
@RpcBean("medicationGuideService")
public class MedicationGuideService {

    /** logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(MedicationGuideService.class);
    /**微信事件推送模板id*/
    private static final String WX_TEMPLATE_ID = "";

    /**
     * 扫码后--接收weixin-service扫码后的信息并获取跳转url再推送消息
     * @param appId
     * @param openId
     * @param qrInfo 加密后的解码二维码信息
     */
    @RpcService
    public void pushMedicationGuideMsgByQrCode(String appId, String openId,String organId,String qrInfo){
        LOGGER.info("pushMedicationGuideMsgByQrCode appId={},openId={},organId={},qrInfo={}",appId,openId,organId,qrInfo);
        if (StringUtils.isEmpty(appId)&&StringUtils.isEmpty(openId)){
            return;
        }
        //根据扫描出来的二维码信息去查his处方信息
        RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
        HospitalRecipeDTO hospitalRecipeDTO = service.queryHisPatientRecipeInfo(organId,qrInfo);
        //reqType 请求类型（1：二维码扫码推送详情 2：自动推送详情链接跳转请求 ）
        hospitalRecipeDTO.setReqType("1");
        //发送模板消息
        sendMedicationGuideMsg(appId,openId,hospitalRecipeDTO);
    }

    /**
     * 用药指导
     * 场景一-扫码后触发-微信事件消息--WXCallbackListenerImpl》onEvent
     * 场景三-线下开处方线上推送消息--前提患者已在公众号注册过
     * @param hospitalRecipeDTO
     */
    public void sendMedicationGuideMsg(String appId, String openId,HospitalRecipeDTO hospitalRecipeDTO){
        verifyParam(hospitalRecipeDTO);
        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        PatientInfoDTO patient = new PatientInfoDTO();
        RecipeBean recipe = new RecipeBean();
        List<RecipeDetailBean> recipeDetails = Lists.newArrayList();
        try {
            patient.setPatientName(hospitalRecipeDTO.getPatientName());
            //科室名
            patient.setDeptName(hospitalRecipeDTO.getDepartId());
            patient.setGender(hospitalRecipeDTO.getPatientSex());
            patient.setDocDate(hospitalRecipeDTO.getCreateDate());
            patient.setPatientAge(String.valueOf(ChinaIDNumberUtil.getStringAgeFromIDNumber(hospitalRecipeDTO.getCertificate())));
            //患者编号
            patient.setPatientCode(hospitalRecipeDTO.getPatientNumber());
            //就诊号
            patient.setAdminNo(hospitalRecipeDTO.getClinicId());
            recipe.setClinicOrgan(Integer.valueOf(hospitalRecipeDTO.getClinicOrgan()));
            recipe.setOrganName(organService.getShortNameById(recipe.getClinicOrgan()));
            recipe.setOrganDiseaseName(hospitalRecipeDTO.getOrganDiseaseName());
            recipe.setOrganDiseaseId(hospitalRecipeDTO.getOrganDiseaseId());
            RecipeDetailBean detailBean;
            for (HospitalDrugDTO drugDTO : hospitalRecipeDTO.getDrugList()){
                detailBean = new RecipeDetailBean();
                detailBean.setUsingRate(drugDTO.getUsingRate());
                detailBean.setUsePathways(drugDTO.getUsePathways());
                detailBean.setUseDose(Double.valueOf(drugDTO.getUseDose()));
                detailBean.setUseDoseUnit(drugDTO.getUseDoseUnit());
                detailBean.setDrugName(drugDTO.getDrugName());
                detailBean.setDrugCode(drugDTO.getDrugCode());
            }
        } catch (Exception e) {
            LOGGER.error("sendMedicationGuideData set param error",e);
        }
        //获取用药指导链接
        Map<String, Object> map = getHtml5Link(patient, recipe, recipeDetails, hospitalRecipeDTO.getReqType());
        sendMedicationGuideMsg(appId,openId,map);
    }

    private void sendMedicationGuideMsg(String appId, String openId,Map<String,Object> map) {
        String url = (String) map.get("url");
        if (StringUtils.isEmpty(appId)&& StringUtils.isEmpty(openId)){
            //发送微信模板消息
            RecipeMsgService.sendMedicationGuideMsg(map);
        }else {
            //发送微信模板事件消息
            RecipeMsgService.sendMedicationGuideMsg(appId,WX_TEMPLATE_ID,openId,url,map);
        }
    }

    private void verifyParam(HospitalRecipeDTO hospitalRecipeDTO) {
        if (StringUtils.isEmpty(hospitalRecipeDTO.getCertificate())){
            throw new DAOException(DAOException.VALUE_NEEDED, "certificate is required!");
        }
        if (StringUtils.isEmpty(hospitalRecipeDTO.getPatientName())){
            throw new DAOException(DAOException.VALUE_NEEDED, "patientName is required!");
        }
        if (StringUtils.isEmpty(hospitalRecipeDTO.getClinicOrgan())){
            throw new DAOException(DAOException.VALUE_NEEDED, "clinicOrgan is required!");
        }
        if (StringUtils.isEmpty(hospitalRecipeDTO.getPatientSex())){
            throw new DAOException(DAOException.VALUE_NEEDED, "patientSex is required!");
        }
        if (StringUtils.isEmpty(hospitalRecipeDTO.getPatientNumber())){
            throw new DAOException(DAOException.VALUE_NEEDED, "patientNumber is required!");
        }
        if (StringUtils.isEmpty(hospitalRecipeDTO.getClinicId())){
            throw new DAOException(DAOException.VALUE_NEEDED, "clinicId is required!");
        }
        if (StringUtils.isEmpty(hospitalRecipeDTO.getCreateDate())){
            throw new DAOException(DAOException.VALUE_NEEDED, "createDate is required!");
        }
        if (StringUtils.isEmpty(hospitalRecipeDTO.getDepartId())){
            throw new DAOException(DAOException.VALUE_NEEDED, "departId is required!");
        }
        if (StringUtils.isEmpty(hospitalRecipeDTO.getOrganDiseaseName())){
            throw new DAOException(DAOException.VALUE_NEEDED, "organDiseaseName is required!");
        }
        if (StringUtils.isEmpty(hospitalRecipeDTO.getOrganDiseaseId())){
            throw new DAOException(DAOException.VALUE_NEEDED, "organDiseaseId is required!");
        }
        if (hospitalRecipeDTO.getDrugList() == null){
            throw new DAOException(DAOException.VALUE_NEEDED, "drugList is required!");
        }
        for (HospitalDrugDTO drugDTO : hospitalRecipeDTO.getDrugList()){
            if (StringUtils.isEmpty(drugDTO.getDrugCode())){
                throw new DAOException(DAOException.VALUE_NEEDED, "drugCode is required!");
            }
            if (StringUtils.isEmpty(drugDTO.getDrugName())){
                throw new DAOException(DAOException.VALUE_NEEDED, "drugName is required!");
            }
            if (StringUtils.isEmpty(drugDTO.getUsingRate())){
                throw new DAOException(DAOException.VALUE_NEEDED, "usingRate is required!");
            }
            if (StringUtils.isEmpty(drugDTO.getUsePathways())){
                throw new DAOException(DAOException.VALUE_NEEDED, "usePathways is required!");
            }
        }
    }

    /**
     * 场景二-平台处方--获取药品说明页面URL
     */
    @RpcService
    public String getHtml5Link(Integer recipeId){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (recipe == null){
            throw new DAOException(ErrorCode.SERVICE_ERROR,"处方不存在");
        }
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
        if (recipedetails == null){
            throw new DAOException(ErrorCode.SERVICE_ERROR,"处方明细不存在");
        }
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        PatientDTO patient = patientService.getByMpiId(recipe.getMpiid());
        PatientInfoDTO patientParam = new PatientInfoDTO();
        //患者编号
        patientParam.setPatientCode(recipe.getPatientID());
        patientParam.setPatientName(patient.getPatientName());

        try {
            Dictionary departDic = DictionaryController.instance().get("eh.base.dictionary.Depart");
            patientParam.setDeptName(departDic.getText(recipe.getDepart()));
        } catch (ControllerException e) {
            LOGGER.error("getHtml5Link error",e);
        }
        //就诊号
        patientParam.setAdminNo(recipe.getPatientID());
        patientParam.setCardType("1");
        patientParam.setCard(patient.getCertificate());
        try {
            patientParam.setPatientAge(String.valueOf(ChinaIDNumberUtil.getStringAgeFromIDNumber(patient.getCertificate())));
        } catch (ValidateException e) {
            LOGGER.error("getHtml5Link error",e);
        } patientParam.setGender(patient.getPatientSex());
        patientParam.setDocDate(DateConversion.formatDateTimeWithSec(recipe.getSignDate()));
        patientParam.setFlag("0");
        RecipeBean recipeBean = ObjectCopyUtils.convert(recipe, RecipeBean.class);
        List<RecipeDetailBean> recipeDetailBeans = ObjectCopyUtils.convert(recipedetails, RecipeDetailBean.class);
        //reqType 请求类型（1：二维码扫码推送详情 2：自动推送详情链接跳转请求 ）
        Map<String, Object> map = getHtml5Link(patientParam, recipeBean, recipeDetailBeans, "2");
        return (String) map.get("url");
    }

    /**
     * 获取药品说明页面URL
     */
    public Map<String,Object> getHtml5Link(PatientInfoDTO patient, RecipeBean recipe, List<RecipeDetailBean> recipeDetail, String reqType){
        LOGGER.info("medicationGuideService getHtml5Link start");
        IMedicationGuideService medicationGuideService = getGuideService(recipe.getClinicOrgan());
        return medicationGuideService.getHtml5LinkInfo(patient,recipe,recipeDetail,reqType);
    }

    /**
     * 获取用药指导开关配置
     * @param organId  机构id
     * @return         0 关闭 1 打开
     */
    @RpcService
    public Integer getMedicationGuideFlag(Integer organId){
        IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        return (Integer)configurationCenterUtilsService.getConfiguration(organId, "medicationGuideFlag");
    }

    private IMedicationGuideService getGuideService(Integer organId){
        OrganMedicationGuideRelationDAO organMedicationGuideRelationDAO = DAOFactory.getDAO(OrganMedicationGuideRelationDAO.class);
        OrganMedicationGuideRelation organMedicationGuideRelation = organMedicationGuideRelationDAO.getOrganMedicationGuideRelationByOrganId(organId);
        MedicationGuideDAO medicationGuideDAO = DAOFactory.getDAO(MedicationGuideDAO.class);
        if (organMedicationGuideRelation == null) {
            LOGGER.info("medicationGuideService getService 没有维护用药指导机构关系");
            organMedicationGuideRelation = new OrganMedicationGuideRelation();
            organMedicationGuideRelation.setOrganId(organId);
            //默认配置卫宁智能审方
            MedicationGuide medicationGuide = medicationGuideDAO.getByCallSys("winning");
            if (medicationGuide == null){
                throw new DAOException(ErrorCode.SERVICE_ERROR,"未配置默认用于指导第三方信息");
            }
            organMedicationGuideRelation.setGuideId(medicationGuide.getGuideId());
            organMedicationGuideRelationDAO.save(organMedicationGuideRelation);
            organMedicationGuideRelation = organMedicationGuideRelationDAO.getOrganMedicationGuideRelationByOrganId(organId);
        }
        LOGGER.info("medicationGuideService getService organMedicationGuideRelation:{}.", JSONUtils.toString(organMedicationGuideRelation));
        MedicationGuide medicationGuide = medicationGuideDAO.getByGuideId(organMedicationGuideRelation.getGuideId());
        if (medicationGuide == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR,"用药指导第三方不存在");
        }
        String serviceName = medicationGuide.getCallSys() + "MedicationGuideService";
        LOGGER.info("medicationGuideService getService serviceName:{}.", serviceName);
        return AppContextHolder.getBean(serviceName, IMedicationGuideService.class);
    }
}
