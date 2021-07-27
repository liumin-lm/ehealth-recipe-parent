package recipe.medicationguide.service;

import com.google.common.collect.ImmutableMap;
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
import com.ngari.recipe.hisprescription.model.HosPatientDTO;
import com.ngari.recipe.hisprescription.model.HosPatientRecipeDTO;
import com.ngari.recipe.hisprescription.model.HosRecipeDTO;
import com.ngari.recipe.hisprescription.model.HosRecipeDetailDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.dictionary.Dictionary;
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
import recipe.constant.ErrorCode;
import recipe.constant.RecipeBussConstant;
import recipe.dao.MedicationGuideDAO;
import recipe.dao.OrganMedicationGuideRelationDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.hisservice.RecipeToHisService;
import recipe.medicationguide.bean.PatientInfoDTO;
import recipe.service.RecipeMsgService;
import recipe.util.ChinaIDNumberUtil;
import recipe.util.DateConversion;
import recipe.util.RedisClient;

import java.util.List;
import java.util.Map;


/**
 * 用药指导服务入口
 * created by shiyuping on 2019/10/25
 * @author shiyuping
 */
@RpcBean(value = "medicationGuideService", mvc_authentication = false)
public class MedicationGuideService {

    /** logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(MedicationGuideService.class);

    @Autowired
    private RedisClient redisClient;

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
        HosPatientRecipeDTO hosPatientRecipeDTO = service.queryHisPatientRecipeInfo(organId,qrInfo);
        //reqType 请求类型（1：二维码扫码推送详情 2：自动推送详情链接跳转请求 ）
        hosPatientRecipeDTO.setReqType(RecipeBussConstant.REQ_TYPE_QRCODE);
        boolean flag = true;
        try {
            verifyParam(hosPatientRecipeDTO);
        } catch (Exception e) {
            LOGGER.error("pushMedicationGuideMsgByQrCode verifyParam error",e);
            flag = false;
            RecipeMsgService.sendMedicationGuideMsg(ImmutableMap.of("appId",appId,"openId",openId,"url","","organId",0));
        }
        //发送模板消息
        if (flag){
            sendMedicationGuideMsg(appId,openId,hosPatientRecipeDTO);
        }
    }

    /**
     * 用药指导
     * 场景一-扫码后触发-微信事件消息--WXCallbackListenerImpl》onEvent
     * 场景三-线下开处方线上推送消息--前提患者已在公众号注册过
     * @param hosPatientRecipeDTO
     */
    public void sendMedicationGuideMsg(String appId, String openId, HosPatientRecipeDTO hosPatientRecipeDTO){
        verifyParam(hosPatientRecipeDTO);
        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        PatientInfoDTO patient = new PatientInfoDTO();
        RecipeBean recipe = new RecipeBean();
        List<RecipeDetailBean> recipeDetails = Lists.newArrayList();
        HosPatientDTO hosPatient = hosPatientRecipeDTO.getPatient();
        HosRecipeDTO hosRecipe = hosPatientRecipeDTO.getRecipe();
        try {
            patient.setPatientName(hosPatient.getPatientName());
            //科室名
            patient.setDeptName(hosRecipe.getDepartName());
            patient.setGender(Integer.valueOf(hosPatient.getPatientSex()));
            patient.setDocDate(hosRecipe.getSignTime());
            patient.setPatientAge(String.valueOf(ChinaIDNumberUtil.getStringAgeFromIDNumber(hosPatient.getCertificate())));
            //患者编号
            patient.setPatientCode(hosPatient.getPatientID());
            //就诊号
            patient.setAdminNo(hosPatientRecipeDTO.getClinicNo());
            recipe.setClinicOrgan(Integer.valueOf(hosPatientRecipeDTO.getOrganId()));
            recipe.setOrganName(organService.getShortNameById(recipe.getClinicOrgan()));
            recipe.setOrganDiseaseName(hosRecipe.getDiseaseName());
            recipe.setOrganDiseaseId(hosRecipe.getDisease());
            RecipeDetailBean detailBean;
            for (HosRecipeDetailDTO drugDTO : hosRecipe.getDetailData()){
                detailBean = new RecipeDetailBean();
                detailBean.setUsingRate(drugDTO.getUsingRate());
                detailBean.setUsePathways(drugDTO.getUsePathWays());
                if (StringUtils.isNotEmpty(drugDTO.getUseDose())){
                    detailBean.setUseDose(Double.valueOf(drugDTO.getUseDose()));
                }
                detailBean.setUseDoseUnit(drugDTO.getUseDoseUnit());
                detailBean.setDrugName(drugDTO.getDrugName());
                detailBean.setOrganDrugCode(drugDTO.getDrugCode());
                recipeDetails.add(detailBean);
            }
        } catch (Exception e) {
            LOGGER.error("sendMedicationGuideData set param error",e);
        }
        //获取用药指导链接
        Map<String, Object> map = getHtml5Link(patient, recipe, recipeDetails, hosPatientRecipeDTO.getReqType());
        map.put("recipeDoctor",hosRecipe.getDoctorName());
        map.put("recipeType",hosRecipe.getRecipeType());
        map.put("organId",recipe.getClinicOrgan());
        map.put("idCard",hosPatient.getCertificate());
        map.put("patientName",hosPatient.getPatientName());
        map.put("recipeTime",hosRecipe.getSignTime());
        map.put("appId",appId);
        map.put("openId",openId);
        RecipeMsgService.sendMedicationGuideMsg(map);
    }

    private void verifyParam(HosPatientRecipeDTO hosPatientRecipeDTO) {
        if (null != hosPatientRecipeDTO) {
            HosPatientDTO hosPatient = hosPatientRecipeDTO.getPatient();
            HosRecipeDTO hosRecipe = hosPatientRecipeDTO.getRecipe();
            if (hosPatient == null){
                throw new DAOException(DAOException.VALUE_NEEDED, "patient is required!");
            }
            if (hosRecipe == null){
                throw new DAOException(DAOException.VALUE_NEEDED, "recipe is required!");
            }
            if (StringUtils.isEmpty(hosPatientRecipeDTO.getClinicNo())){
                throw new DAOException(DAOException.VALUE_NEEDED, "clinicNo is required!");
            }
            if (StringUtils.isEmpty(hosPatient.getCertificate())){
                throw new DAOException(DAOException.VALUE_NEEDED, "certificate is required!");
            }
            if (StringUtils.isEmpty(hosPatient.getPatientName())){
                throw new DAOException(DAOException.VALUE_NEEDED, "patientName is required!");
            }
            if (StringUtils.isEmpty(hosPatientRecipeDTO.getOrganId())){
                throw new DAOException(DAOException.VALUE_NEEDED, "organId is required!");
            }
            if (StringUtils.isEmpty(hosPatient.getPatientSex())){
                throw new DAOException(DAOException.VALUE_NEEDED, "patientSex is required!");
            }
            if (StringUtils.isEmpty(hosPatient.getPatientID())){
                throw new DAOException(DAOException.VALUE_NEEDED, "patientID is required!");
            }
            if (StringUtils.isEmpty(hosPatientRecipeDTO.getClinicNo())){
                throw new DAOException(DAOException.VALUE_NEEDED, "clinicNo is required!");
            }
            if (StringUtils.isEmpty(hosRecipe.getSignTime())){
                throw new DAOException(DAOException.VALUE_NEEDED, "signTime is required!");
            }
            if (StringUtils.isEmpty(hosRecipe.getDepartName())){
                throw new DAOException(DAOException.VALUE_NEEDED, "departId is required!");
            }
            if (StringUtils.isEmpty(hosRecipe.getDiseaseName())){
                throw new DAOException(DAOException.VALUE_NEEDED, "diseaseName is required!");
            }
            if (StringUtils.isEmpty(hosRecipe.getDisease())){
                throw new DAOException(DAOException.VALUE_NEEDED, "disease is required!");
            }
            if (hosRecipe.getDetailData() == null){
                throw new DAOException(DAOException.VALUE_NEEDED, "detailData is required!");
            }
            for (HosRecipeDetailDTO drugDTO : hosRecipe.getDetailData()){
                if (StringUtils.isEmpty(drugDTO.getDrugCode())){
                    throw new DAOException(DAOException.VALUE_NEEDED, "drugCode is required!");
                }
                if (StringUtils.isEmpty(drugDTO.getDrugName())){
                    throw new DAOException(DAOException.VALUE_NEEDED, "drugName is required!");
                }
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
        PatientDTO patient = patientService.getPatientByMpiId(recipe.getMpiid());
        PatientInfoDTO patientParam = new PatientInfoDTO();


        try {
            //患者编号
            patientParam.setPatientCode(recipe.getPatientID());
            patientParam.setPatientName(patient.getPatientName());
            Dictionary departDic = DictionaryController.instance().get("eh.base.dictionary.Depart");
            patientParam.setDeptName(departDic.getText(recipe.getDepart()));
            //就诊号
            patientParam.setAdminNo(recipe.getPatientID());
            patientParam.setCardType(1);
            patientParam.setCard(patient.getCertificate());
            patientParam.setPatientAge(String.valueOf(ChinaIDNumberUtil.getStringAgeFromIDNumber(patient.getCertificate())));
            patientParam.setGender(Integer.valueOf(patient.getPatientSex()));
            patientParam.setDocDate(DateConversion.formatDateTimeWithSec(recipe.getSignDate()));
            patientParam.setFlag(0);
        } catch (Exception e) {
            LOGGER.error("getHtml5Link error",e);
        }

        RecipeBean recipeBean = ObjectCopyUtils.convert(recipe, RecipeBean.class);
        List<RecipeDetailBean> recipeDetailBeans = ObjectCopyUtils.convert(recipedetails, RecipeDetailBean.class);
        //reqType 请求类型（1：二维码扫码推送详情 2：自动推送详情链接跳转请求 ）
        Map<String, Object> map = getHtml5Link(patientParam, recipeBean, recipeDetailBeans, RecipeBussConstant.REQ_TYPE_AUTO);
        return (String) map.get("url");
    }

    /**
     * 获取药品说明页面URL
     */
    public Map<String,Object> getHtml5Link(PatientInfoDTO patient, RecipeBean recipe, List<RecipeDetailBean> recipeDetail, Integer reqType){
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
    public Boolean getMedicationGuideFlag(Integer organId){
        IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        return (Boolean)configurationCenterUtilsService.getConfiguration(organId, "medicationGuideFlag");
    }

    private IMedicationGuideService getGuideService(Integer organId){
        OrganMedicationGuideRelationDAO organMedicationGuideRelationDAO = DAOFactory.getDAO(OrganMedicationGuideRelationDAO.class);
        MedicationGuideDAO medicationGuideDAO = DAOFactory.getDAO(MedicationGuideDAO.class);
        Integer guideId = redisClient.get("MedicationGuide_" + organId);
        if (guideId == null){
            OrganMedicationGuideRelation organMedicationGuideRelation = organMedicationGuideRelationDAO.getOrganMedicationGuideRelationByOrganId(organId);
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
            guideId =organMedicationGuideRelation.getGuideId();
            //缓存一周
            redisClient.setEX("MedicationGuide_"+organId,7 * 24 * 3600L,guideId);
        }
        MedicationGuide medicationGuide = medicationGuideDAO.getByGuideId(guideId);
        if (medicationGuide == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR,"用药指导第三方不存在");
        }
        String serviceName = medicationGuide.getCallSys() + "MedicationGuideService";
        LOGGER.info("medicationGuideService getService serviceName:{}.", serviceName);
        return AppContextHolder.getBean(serviceName, IMedicationGuideService.class);
    }
}
