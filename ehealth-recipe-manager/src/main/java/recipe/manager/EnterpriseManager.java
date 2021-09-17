package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.AppointDepartService;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.*;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.SkipThirdDTO;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import eh.recipeaudit.api.IRecipeCheckService;
import eh.recipeaudit.model.RecipeCheckBean;
import eh.recipeaudit.util.RecipeAuditAPI;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DoctorClient;
import recipe.client.EnterpriseClient;
import recipe.client.IConfigurationClient;
import recipe.client.PatientClient;
import recipe.constant.ErrorCode;
import recipe.dao.*;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.GiveModeTextEnum;
import recipe.third.IFileDownloadService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * 药企 功能处理类
 * @author fuzi
 */
@Service
public class EnterpriseManager extends BaseManager  {
    /**
     * 扁鹊
     */
    private static String ENTERPRISE_BAN_QUE = "bqEnterprise";
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private EnterpriseClient enterpriseClient;
    @Autowired
    private IFileDownloadService fileDownloadService;
    @Autowired
    private EmploymentService iEmploymentService;
    @Autowired
    private AppointDepartService appointDepartService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private DoctorClient doctorClient;

    /**
     * 上传处方pdf给第三方
     *
     * @param recipeId
     */
    public void uploadRecipePdfToHis(Integer recipeId) {
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        enterpriseClient.uploadRecipePdfToHis(recipe);
    }

    /**
     * @param organId
     * @param giveMode
     * @param recipeIds
     */
    public SkipThirdDTO uploadRecipeInfoToThird(Integer organId, String giveMode, List<Integer> recipeIds) {
        logger.info("EnterpriseManager uploadRecipeInfoToThird organId:{},giveMode:{},recipeIds:{}", organId, giveMode, JSONUtils.toString(recipeIds));
        Boolean pushToHisAfterChoose = configurationClient.getValueBooleanCatch(organId, "pushToHisAfterChoose", false);
        if (!pushToHisAfterChoose) {
            return null;
        }
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIds);
        //将处方上传到第三方
        SkipThirdDTO result = null;
        for (Recipe recipe : recipes) {
            recipe.setGiveMode(GiveModeTextEnum.getGiveMode(giveMode));
            result = pushRecipeForThird(recipe, 1);
            //todo 一个报错就都不推送了？
            if (0 == result.getCode()) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, result.getMsg());
            }
        }
        return result;
    }

    public SkipThirdDTO pushRecipeForThird(Recipe recipe, Integer node) {
        logger.info("EnterpriseManager pushRecipeForThird recipeId:{}, node:{}.", recipe.getRecipeId(), node);
        SkipThirdDTO result = new SkipThirdDTO();
        result.setCode(1);
        List<DrugsEnterprise> drugsEnterpriseList = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
        if(CollectionUtils.isEmpty(drugsEnterpriseList)){
            return result;
        }
        for (DrugsEnterprise drugsEnterprise : drugsEnterpriseList) {
            try {
                //todo 只用最后一个返回？
                if (1 == drugsEnterprise.getOperationType() && ENTERPRISE_BAN_QUE.equals(drugsEnterprise.getAccount())) {
                    result = pushRecipeInfoForThird(recipe, drugsEnterprise, node);
                }
            } catch (Exception e) {
                logger.error("EnterpriseManager pushRecipeForThird error ", e);
            }
        }
        logger.info("EnterpriseManager pushRecipeForThird result:{}", JSONUtils.toString(result));
        return result;
    }


    public SkipThirdDTO pushRecipeInfoForThird(Recipe recipe, DrugsEnterprise enterprise, Integer node) {
        logger.info("RemoteDrugEnterpriseService pushRecipeInfoForThird recipeId:{},enterprise:{},node:{}.", recipe.getRecipeId(), JSONUtils.toString(enterprise), node);
        //传过来的处方不是最新的需要重新从数据库获取
        Recipe recipeNew = recipeDAO.getByRecipeId(recipe.getRecipeId());
        recipeNew.setGiveMode(recipe.getGiveMode());
        //通过前置机进行推送
        PushRecipeAndOrder pushRecipeAndOrder = getPushRecipeAndOrder(recipeNew, enterprise);
        SkipThirdDTO skipThirdDTO = enterpriseClient.pushRecipeInfoForThird(pushRecipeAndOrder, node);
        if (0 == skipThirdDTO.getCode()) {
            saveRecipeLog(recipe.getRecipeId(), RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType(), RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType(), "购药按钮药企推送失败:" + skipThirdDTO.getMsg());
            return skipThirdDTO;
        }
        //推送药企处方成功,判断是否为扁鹊平台
        if (null != enterprise && ENTERPRISE_BAN_QUE.equals(enterprise.getAccount())) {
            Recipe recipeUpdate = new Recipe();
            recipeUpdate.setRecipeId(recipeNew.getRecipeId());
            recipeUpdate.setEnterpriseId(enterprise.getId());
            recipeUpdate.setPushFlag(1);
            recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        } else if (StringUtils.isNotEmpty(skipThirdDTO.getPrescId())) {
            recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeNew.getRecipeId(), ImmutableMap.of("rxid", skipThirdDTO.getPrescId()));
        }
        Executors.newSingleThreadExecutor().execute(() -> enterpriseClient.uploadRecipePdfToHis(recipeNew));
        return skipThirdDTO;
    }


    public PushRecipeAndOrder getPushRecipeAndOrder(Recipe recipe, DrugsEnterprise enterprise) {
        PushRecipeAndOrder pushRecipeAndOrder = new PushRecipeAndOrder();
        pushRecipeAndOrder.setOrganId(recipe.getClinicOrgan());
        //设置订单信息
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            pushRecipeAndOrder.setRecipeOrderBean(ObjectCopyUtils.convert(recipeOrder, RecipeOrderBean.class));
            AddressBean addressBean = new AddressBean();
            addressBean.setProvince(getAddress(recipeOrder.getAddress1()));
            addressBean.setCity(getAddress(recipeOrder.getAddress2()));
            addressBean.setDistrict(getAddress(recipeOrder.getAddress3()));
            addressBean.setStreetAddress(getAddress(recipeOrder.getStreetAddress()));
            pushRecipeAndOrder.setAddressBean(addressBean);
        }
        //设置医生信息
        DoctorDTO doctorDTO = doctorClient.getDoctor(recipe.getDoctor());
        doctorDTO.setJobNumber(iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart()));
        pushRecipeAndOrder.setDoctorDTO(doctorDTO);
        //设置审方药师信息
        AppointDepartDTO appointDepart = appointDepartService.findByOrganIDAndDepartID(recipe.getClinicOrgan(), recipe.getDepart());
        RecipeAuditReq recipeAuditReq = new RecipeAuditReq();
        //科室代码
        recipeAuditReq.setDepartCode((null != appointDepart) ? appointDepart.getAppointDepartCode() : "");
        //科室名称
        recipeAuditReq.setDepartName((null != appointDepart) ? appointDepart.getAppointDepartName() : "");
        if (null != recipe.getChecker() && 0 != recipe.getChecker()) {
            DoctorDTO doctor = doctorClient.getDoctor(recipe.getChecker());
            recipeAuditReq.setAuditDoctorNo(iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), doctor.getDepartment()));
            recipeAuditReq.setAuditDoctorName(doctor.getName());
        }
        pushRecipeAndOrder.setRecipeAuditReq(recipeAuditReq);
        //设置患者信息
        PatientDTO patientDTO = patientClient.getPatientDTO(recipe.getMpiid());
        pushRecipeAndOrder.setPatientDTO(ObjectCopyUtils.convert(patientDTO, com.ngari.patient.dto.PatientDTO.class));
        //设置用户信息
        if (StringUtils.isNotEmpty(recipe.getRequestMpiId())) {
            PatientDTO userDTO = patientClient.getPatientDTO(recipe.getRequestMpiId());
            pushRecipeAndOrder.setUserDTO(ObjectCopyUtils.convert(userDTO, com.ngari.patient.dto.PatientDTO.class));
        }
        //设置科室信息
        DepartmentDTO departmentDTO = departmentService.get(recipe.getDepart());
        pushRecipeAndOrder.setDepartmentDTO(departmentDTO);
        List<Recipe> recipes = Arrays.asList(recipe);
        //多处方处理
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
            recipes = recipeDAO.findByRecipeIds(recipeIdList);
        }
        if (recipes.size() > 1) {
            //说明为多处方
            pushRecipeAndOrder.setMergeRecipeFlag(1);
        } else {
            pushRecipeAndOrder.setMergeRecipeFlag(0);
        }
        List<MargeRecipeBean> margeRecipeBeans = new ArrayList<>();
        for (Recipe rec : recipes) {
            setSingleRecipeInfo(rec, enterprise, pushRecipeAndOrder, margeRecipeBeans);
        }
        pushRecipeAndOrder.setMargeRecipeBeans(margeRecipeBeans);
        logger.info("getPushRecipeAndOrder pushRecipeAndOrder:{}.", JSONUtils.toString(pushRecipeAndOrder));
        return pushRecipeAndOrder;
    }

    /**
     * 设置单个处方订单相关信息
     * @param recipe      处方信息
     * @param pushRecipeAndOrder 包装信息
     */
    private void setSingleRecipeInfo(Recipe recipe, DrugsEnterprise enterprise, PushRecipeAndOrder pushRecipeAndOrder, List<MargeRecipeBean> margeRecipeBeans){
        //设置处方信息
        pushRecipeAndOrder.setRecipeBean(ObjectCopyUtils.convert(recipe, RecipeBean.class));
        // 从复诊获取患者渠道id
        try {
            if (null != recipe.getClinicId()) {
                IRevisitExService exService = RevisitAPI.getService(IRevisitExService.class);
                logger.info("queryPatientChannelId req={}", recipe.getClinicId());
                RevisitExDTO revisitExDTO = exService.getByConsultId(recipe.getClinicId());
                if (revisitExDTO != null) {
                    logger.info("queryPatientChannelId res={}", JSONObject.toJSONString(revisitExDTO));
                    pushRecipeAndOrder.getRecipeBean().setPatientChannelId(revisitExDTO.getProjectChannel());
                }
            }
        } catch (Exception e) {
            logger.error("queryPatientChannelId error:",e);
        }
        //设置药品详情
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        List<PushDrugListBean> pushDrugListBeans = new ArrayList<>();
        //设置配送药品信息
        for (Recipedetail recipedetail : recipedetails) {
            PushDrugListBean pushDrugListBean = new PushDrugListBean();
            if (enterprise != null) {
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), enterprise.getId());
                if (saleDrugList != null) {
                    pushDrugListBean.setSaleDrugListDTO(ObjectCopyUtils.convert(saleDrugList, SaleDrugListDTO.class));
                }
            }
            OrganDrugList organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(), recipedetail.getOrganDrugCode(),recipedetail.getDrugId());
            if (organDrug != null) {
                pushDrugListBean.setOrganDrugListBean(ObjectCopyUtils.convert(organDrug, OrganDrugListBean.class));
            }
            pushDrugListBean.setRecipeDetailBean(ObjectCopyUtils.convert(recipedetail, RecipeDetailBean.class));
            pushDrugListBeans.add(pushDrugListBean);
        }
        pushRecipeAndOrder.setPushDrugListBeans(pushDrugListBeans);

        //设置扩展信息
        ExpandDTO expandDTO = new ExpandDTO();
        String orgCode = patientClient.getMinkeOrganCodeByOrganId(recipe.getClinicOrgan());
        if (StringUtils.isNotEmpty(orgCode)) {
            expandDTO.setOrgCode(orgCode);
        }
        if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
            expandDTO.setSignFile(recipe.getChemistSignFile());
        } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
            expandDTO.setSignFile(recipe.getSignFile());
        }
        //设置pdf base 64内容
        String signFileOssId = expandDTO.getSignFile();
        String imgHead = "data:image/jpeg;base64,";
        if (StringUtils.isNotBlank(signFileOssId)) {
            try {
                String imgStr = imgHead + fileDownloadService.downloadImg(signFileOssId);
                expandDTO.setPdfContent(imgStr);
            } catch (Exception e) {
                logger.error("getPushRecipeAndOrder:{}处方，获取处方pdf:{},服务异常：", recipe.getRecipeId(), recipe.getSignFile(), e);
            }
        }
        if (null != enterprise && null != enterprise.getDownSignImgType() && 1 == enterprise.getDownSignImgType()) {
            //获取处方签链接
            RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
            String signImgFile = recipeParameterDao.getByName("fileImgUrl");
            expandDTO.setPrescriptionImg(signImgFile + expandDTO.getSignFile());
        } else {
            //设置处方笺base
            String ossId = recipe.getSignImg();
            if (null != ossId) {
                try {
                    String imgStr = imgHead + fileDownloadService.downloadImg(ossId);
                    expandDTO.setPrescriptionImg(imgStr);
                } catch (Exception e) {
                    logger.error("getPushRecipeAndOrder 获取处方图片服务异常 recipeId:{}，", recipe.getRecipeId(), e);
                }
            }
        }
        IRecipeCheckService recipeCheckService = RecipeAuditAPI.getService(IRecipeCheckService.class, "recipeCheckServiceImpl");
        RecipeCheckBean recipeCheckBean = recipeCheckService.getByRecipeId(recipe.getRecipeId());
        if (recipeCheckBean != null && StringUtils.isNotEmpty(recipeCheckBean.getCheckerName())) {
            expandDTO.setCheckerName(recipeCheckBean.getCheckerName());
        }
        pushRecipeAndOrder.setExpandDTO(expandDTO);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        pushRecipeAndOrder.setRecipeExtendBean(ObjectCopyUtils.convert(recipeExtend, RecipeExtendBean.class));
        //制法Code 煎法Code 中医证候Code
        try {
            DrugDecoctionWayDao drugDecoctionWayDao = DAOFactory.getDAO(DrugDecoctionWayDao.class);
            DrugMakingMethodDao drugMakingMethodDao = DAOFactory.getDAO(DrugMakingMethodDao.class);
            if (StringUtils.isNotBlank(recipeExtend.getDecoctionId())) {
                DecoctionWay decoctionWay = drugDecoctionWayDao.get(Integer.parseInt(recipeExtend.getDecoctionId()));
                pushRecipeAndOrder.getRecipeExtendBean().setDecoctionCode(decoctionWay.getDecoctionCode());
            }
            if (StringUtils.isNotBlank(recipeExtend.getMakeMethodId())) {
                DrugMakingMethod drugMakingMethod = drugMakingMethodDao.get(Integer.parseInt(recipeExtend.getMakeMethodId()));
                pushRecipeAndOrder.getRecipeExtendBean().setMakeMethod(drugMakingMethod.getMethodCode());
            }
            if (StringUtils.isNotBlank(recipeExtend.getSymptomId())) {
                SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
                Symptom symptom = symptomDAO.get(recipeExtend.getSymptomId());
                pushRecipeAndOrder.getRecipeExtendBean().setSymptomCode(symptom.getSymptomCode());
            }
        } catch (Exception e) {
            logger.error("getPushRecipeAndOrder recipe:{} error :{}", recipe.getRecipeId(), e);
        }
        if (1 == pushRecipeAndOrder.getMergeRecipeFlag()) {
            MargeRecipeBean margeRecipeBean = new MargeRecipeBean();
            margeRecipeBean.setRecipeBean(pushRecipeAndOrder.getRecipeBean());
            margeRecipeBean.setPushDrugListBeans(pushDrugListBeans);
            margeRecipeBean.setExpandDTO(pushRecipeAndOrder.getExpandDTO());
            margeRecipeBean.setRecipeExtendBean(pushRecipeAndOrder.getRecipeExtendBean());
            margeRecipeBeans.add(margeRecipeBean);
        }
        logger.info("getPushRecipeAndOrder pushRecipeAndOrder:{}", JSON.toJSONString(pushRecipeAndOrder));
    }

}
