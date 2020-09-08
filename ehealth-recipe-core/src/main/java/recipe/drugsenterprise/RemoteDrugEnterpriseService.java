package recipe.drugsenterprise;

import com.ngari.base.BaseAPI;
import com.ngari.base.currentuserinfo.model.SimpleWxAccountBean;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.ConsultAPI;
import com.ngari.his.recipe.mode.DrugInfoResponseTO;
import com.ngari.his.recipe.service.IRecipeEnterpriseService;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.*;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.*;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import ctd.account.thirdparty.entity.ThirdPartyMappingEntity;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.ErrorCode;
import recipe.constant.ParameterConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.hisservice.RecipeToHisService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeServiceSub;
import recipe.service.common.RecipeCacheService;
import recipe.third.IFileDownloadService;

import java.util.*;

import static ctd.util.AppContextHolder.getBean;

/**
 * 业务使用药企对接类，具体实现在CommonRemoteService
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/3/7.
 */
@RpcBean("remoteDrugEnterpriseService")
public class RemoteDrugEnterpriseService extends  AccessDrugEnterpriseService{

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteDrugEnterpriseService.class);

    private static final String COMMON_SERVICE = "commonRemoteService";

    //手动推送给第三方
    @RpcService
    public void pushRecipeInfoForThirdSd(Integer recipeId, Integer depId){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);

        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        pushRecipeInfoForThird(recipe, drugsEnterprise);

    }

    public void pushRecipeInfoForThird(Recipe recipe, DrugsEnterprise enterprise){
        //药企对应的service为空，则通过前置机进行推送
        IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService",IRecipeEnterpriseService.class);
        PushRecipeAndOrder pushRecipeAndOrder = getPushRecipeAndOrder(recipe, enterprise);
        HisResponseTO responseTO = recipeEnterpriseService.pushSingleRecipeInfo(pushRecipeAndOrder);
        LOGGER.info("pushRecipeInfoForThird responseTO:{}.", JSONUtils.toString(responseTO));
        if (responseTO != null && responseTO.isSuccess()) {
            //推送药企处方成功,判断是否为扁鹊平台
            if (RecipeServiceSub.isBQEnterprise(recipe.getClinicOrgan())) {
                DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getByAccount("bqEnterprise");
                if (drugsEnterprise != null) {
                    recipe.setEnterpriseId(drugsEnterprise.getId());
                    recipe.setPushFlag(1);
                    RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
                    recipeDAO.update(recipe);
                }
            } else {
                String prescId = (String)responseTO.getExtend().get("prescId");
                RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                if (StringUtils.isNotEmpty(prescId)) {
                    recipeExtend.setRxid(prescId);
                    recipeExtendDAO.update(recipeExtend);
                }
            }
        }
    }

    /**
     * 推送处方
     *
     * @param recipeId 处方ID集合
     * @return
     */
    @RpcService
    public DrugEnterpriseResult pushSingleRecipeInfo(Integer recipeId) {
        DrugEnterpriseResult result = getServiceByRecipeId(recipeId);
        DrugsEnterprise enterprise = result.getDrugsEnterprise();
        if (enterprise != null && new Integer(1).equals(enterprise.getOperationType())) {
            //药企对应的service为空，则通过前置机进行推送
            IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService",IRecipeEnterpriseService.class);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            PushRecipeAndOrder pushRecipeAndOrder = getPushRecipeAndOrder(recipe, enterprise);
            LOGGER.info("pushSingleRecipeInfo pushRecipeAndOrder:{}.", JSONUtils.toString(pushRecipeAndOrder));
            HisResponseTO responseTO = recipeEnterpriseService.pushSingleRecipeInfo(pushRecipeAndOrder);
            LOGGER.info("pushSingleRecipeInfo responseTO:{}.", JSONUtils.toString(responseTO));
            if (responseTO != null && responseTO.isSuccess()) {
                //推送药企处方成功
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "纳里给"+enterprise.getName()+"推送处方成功");
                result.setCode(1);
            } else {
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "纳里给"+enterprise.getName()+"推送处方失败");
                result.setCode(0);
            }
        } else {
            if (DrugEnterpriseResult.SUCCESS.equals(result.getCode()) && null != result.getAccessDrugEnterpriseService()) {
                result = result.getAccessDrugEnterpriseService().pushRecipeInfo(Collections.singletonList(recipeId), enterprise);
                if (DrugEnterpriseResult.SUCCESS.equals(result.getCode())) {
                    result.setDrugsEnterprise(enterprise);
                }
            }
        }
        LOGGER.info("pushSingleRecipeInfo recipeId:{}, result:{}", recipeId, JSONUtils.toString(result));
        return result;
    }

    private PushRecipeAndOrder getPushRecipeAndOrder(Recipe recipe, DrugsEnterprise enterprise) {
        PushRecipeAndOrder pushRecipeAndOrder = new PushRecipeAndOrder();
        pushRecipeAndOrder.setOrganId(recipe.getClinicOrgan());
        //设置处方信息
        pushRecipeAndOrder.setRecipeBean(ObjectCopyUtils.convert(recipe, RecipeBean.class));
        //设置订单信息
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            pushRecipeAndOrder.setRecipeOrderBean(ObjectCopyUtils.convert(recipeOrder, RecipeOrderBean.class));
            String province = getAddressDic(recipeOrder.getAddress1());
            String city = getAddressDic(recipeOrder.getAddress2());
            String district = getAddressDic(recipeOrder.getAddress3());
            AddressBean addressBean = new AddressBean();
            addressBean.setProvince(province);
            addressBean.setCity(city);
            addressBean.setDistrict(district);
            pushRecipeAndOrder.setAddressBean(addressBean);
        }
        //设置药品详情
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        List<PushDrugListBean> pushDrugListBeans = new ArrayList<>();
        //设置配送药品信息
        for (Recipedetail recipedetail : recipedetails) {
            PushDrugListBean pushDrugListBean = new PushDrugListBean();
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), enterprise.getId());
            if (saleDrugList != null) {
                pushDrugListBean.setSaleDrugListDTO(ObjectCopyUtils.convert(saleDrugList, SaleDrugListDTO.class));
            }
            OrganDrugList organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(), recipedetail.getOrganDrugCode(),recipedetail.getDrugId());
            if (organDrug != null) {
                pushDrugListBean.setOrganDrugListBean(ObjectCopyUtils.convert(organDrug, OrganDrugListBean.class));
            }
            pushDrugListBean.setRecipeDetailBean(ObjectCopyUtils.convert(recipedetail, RecipeDetailBean.class));
            pushDrugListBeans.add(pushDrugListBean);
        }
        pushRecipeAndOrder.setPushDrugListBeans(pushDrugListBeans);

        //设置医生信息
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getDoctor());
        //设置医生工号
        EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
        doctorDTO.setJobNumber(iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart()));
        pushRecipeAndOrder.setDoctorDTO(doctorDTO);
        //设置患者信息
        PatientService patientService = BasicAPI.getService(PatientService.class);
        PatientDTO patientDTO = patientService.get(recipe.getMpiid());
        try {
            // 从端获取患者渠道id
            ICurrentUserInfoService userInfoService = AppContextHolder.getBean("eh.remoteCurrentUserInfoService", ICurrentUserInfoService.class);
            SimpleWxAccountBean account = userInfoService.getSimpleWxAccount();
            String appKey = account.getAppId();
            String loginId = patientDTO.getLoginId();
            eh.account.api.ThirdPartyMappingService thirdService = AppContextHolder.getBean("eh.thirdPartyMappingService", eh.account.api.ThirdPartyMappingService.class);
            ThirdPartyMappingEntity thirdPartyEntity = thirdService.getOpenidByAppkeyAndUserId(appKey,loginId);
            // TODO thirdPartyEntity获取患者渠道id
            String patientChannelId = "";
            pushRecipeAndOrder.getRecipeBean().setPatientChannelId(patientChannelId);
        } catch (Exception e) {
            LOGGER.error("获取患者渠道id异常",e);
        }

        pushRecipeAndOrder.setPatientDTO(patientDTO);
        //设置用户信息
        if (StringUtils.isNotEmpty(recipe.getRequestMpiId())) {
            PatientDTO userDTO = patientService.get(recipe.getRequestMpiId());
            pushRecipeAndOrder.setUserDTO(userDTO);
        }
        //设置扩展信息
        ExpandDTO expandDTO = new ExpandDTO();
        String orgCode = RecipeServiceSub.getMinkeOrganCodeByOrganId(recipe.getClinicOrgan());
        if (StringUtils.isNotEmpty(orgCode)) {
            expandDTO.setOrgCode(orgCode);
        }
        if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
            expandDTO.setSignFile(recipe.getChemistSignFile());
        } else {
            if (StringUtils.isNotEmpty(recipe.getSignFile())) {
                expandDTO.setSignFile(recipe.getSignFile());
            }
        }
        //设置处方笺base
        String ossId = recipe.getSignImg();
        if(null != ossId){
            String imgHead = "data:image/jpeg;base64,";
            try {
                IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
                String imgStr = imgHead + fileDownloadService.downloadImg(ossId);
                if(org.springframework.util.ObjectUtils.isEmpty(imgStr)){
                    LOGGER.warn("getPushRecipeAndOrder:处方ID为{}的ossid为{}处方笺不存在", recipe.getRecipeId(), ossId);
                }
                LOGGER.warn("getPushRecipeAndOrder:{}处方", recipe.getRecipeId());
                expandDTO.setPrescriptionImg(imgStr);
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("getPushRecipeAndOrder:{}处方，获取处方图片服务异常：{}.", recipe.getRecipeId(), e.getMessage(),e );
            }
        }
        RecipeCheckDAO recipeCheckDAO = DAOFactory.getDAO(RecipeCheckDAO.class);
        RecipeCheck recipeCheck = recipeCheckDAO.getByRecipeId(recipe.getRecipeId());
        if (recipeCheck != null && StringUtils.isNotEmpty(recipeCheck.getCheckerName())) {
            expandDTO.setCheckerName(recipeCheck.getCheckerName());
        }
        pushRecipeAndOrder.setExpandDTO(expandDTO);
        //设置科室信息
        DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
        DepartmentDTO departmentDTO = departmentService.get(recipe.getDepart());
        pushRecipeAndOrder.setDepartmentDTO(departmentDTO);

        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        pushRecipeAndOrder.setRecipeExtendBean(ObjectCopyUtils.convert(recipeExtend, RecipeExtendBean.class));
        //制法Code 煎法Code 中医证候Code
        try{
            DrugDecoctionWayDao drugDecoctionWayDao=DAOFactory.getDAO(DrugDecoctionWayDao.class);
            DrugMakingMethodDao drugMakingMethodDao=DAOFactory.getDAO(DrugMakingMethodDao.class);
            SymptomDAO symptomDAO=DAOFactory.getDAO(SymptomDAO.class);
            if(StringUtils.isNotBlank(recipeExtend.getDecoctionId())){
                DecoctionWay decoctionWay=drugDecoctionWayDao.get(Integer.parseInt(recipeExtend.getDecoctionId()));
                pushRecipeAndOrder.getRecipeExtendBean().setDecoctionCode(decoctionWay.getDecoctionCode());
            }
            if(StringUtils.isNotBlank(recipeExtend.getMakeMethodId())){
                DrugMakingMethod drugMakingMethod=drugMakingMethodDao.get(Integer.parseInt(recipeExtend.getMakeMethodId()));
                pushRecipeAndOrder.getRecipeExtendBean().setMakeMethod(drugMakingMethod.getMethodCode());
            }
            if(StringUtils.isNotBlank(recipeExtend.getSymptomId())){
                Symptom symptom=symptomDAO.get(Integer.parseInt(recipeExtend.getSymptomId()));
                pushRecipeAndOrder.getRecipeExtendBean().setSymptomCode(symptom.getSymptomCode());
            }
        }catch(Exception e){
            LOGGER.error("getPushRecipeAndOrder recipe:{} error :{}",recipe.getRecipeId(),e );
        }

        LOGGER.info("getPushRecipeAndOrder pushRecipeAndOrder:{}.", JSONUtils.toString(pushRecipeAndOrder));
        return pushRecipeAndOrder;
    }

    /**
     * 根据药企推送处方
     *
     * @param drugsEnterprise 药企
     * @return
     */
    @RpcService
    public DrugEnterpriseResult pushSingleRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise drugsEnterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        result.setAccessDrugEnterpriseService(this.getServiceByDep(drugsEnterprise));
        if (DrugEnterpriseResult.SUCCESS.equals(result.getCode()) && null != result.getAccessDrugEnterpriseService()) {
            result = result.getAccessDrugEnterpriseService().pushRecipe(hospitalRecipeDTO, drugsEnterprise);
            if (DrugEnterpriseResult.SUCCESS.equals(result.getCode())) {
                result.setDrugsEnterprise(drugsEnterprise);
            }
        }
        LOGGER.info("pushSingleRecipeInfo drugsEnterpriseName:{}, result:{}", drugsEnterprise.getName(), JSONUtils.toString(result));
        return result;
    }

    /**
     * 带药企ID进行推送
     *
     * @param recipeId
     * @param depId
     * @return
     */
    @RpcService
    public DrugEnterpriseResult pushSingleRecipeInfoWithDepId(Integer recipeId, Integer depId) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise dep = null;
        if (null != depId) {
            dep = drugsEnterpriseDAO.get(depId);
            if (null != dep) {
                result.setAccessDrugEnterpriseService(this.getServiceByDep(dep));
            } else {
                result.setCode(DrugEnterpriseResult.FAIL);
                result.setMsg("药企" + depId + "未找到");
            }
        } else {
            result.setCode(DrugEnterpriseResult.FAIL);
            result.setMsg("处方单" + recipeId + "未分配药企");
        }

        if (DrugEnterpriseResult.SUCCESS.equals(result.getCode()) && null != result.getAccessDrugEnterpriseService()) {
            result = result.getAccessDrugEnterpriseService().pushRecipeInfo(Collections.singletonList(recipeId), dep);
        }
        LOGGER.info("pushSingleRecipeInfoWithDepId recipeId:{}, result:{}", recipeId, JSONUtils.toString(result));
        return result;
    }

    /**
     * 库存检验
     *
     * @param recipeId        处方ID
     * @param drugsEnterprise 药企
     * @return
     */
    @RpcService
    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("scanStock recipeId:{}, drugsEnterprise:{}", recipeId, JSONUtils.toString(drugsEnterprise));
        DrugEnterpriseResult result = DrugEnterpriseResult.getFail();
        if (drugsEnterprise != null && drugsEnterprise.getCheckInventoryFlag() != null && drugsEnterprise.getCheckInventoryFlag() == 0) {
            result.setCode(DrugEnterpriseResult.SUCCESS);
            return result;
        }
        if (drugsEnterprise != null && new Integer(1).equals(drugsEnterprise.getOperationType())) {
            //通过前置机调用
            IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService",IRecipeEnterpriseService.class);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            ScanRequestBean scanRequestBean = getScanRequestBean(recipe, drugsEnterprise);
            LOGGER.info("scanStock scanRequestBean:{}.", JSONUtils.toString(scanRequestBean));
            HisResponseTO responseTO = recipeEnterpriseService.scanStock(scanRequestBean);
            LOGGER.info("scanStock responseTO:{}.", JSONUtils.toString(responseTO));
            if (responseTO != null && responseTO.isSuccess()) {
                result.setCode(DrugEnterpriseResult.SUCCESS);
                return result;
            } else {
                result.setCode(DrugEnterpriseResult.FAIL);
                return result;
            }
        }
        AccessDrugEnterpriseService drugEnterpriseService = null;
        if (null == drugsEnterprise) {
            //药企对象为空，则通过处方id获取相应药企实现
            DrugEnterpriseResult result1 = this.getServiceByRecipeId(recipeId);
            if (DrugEnterpriseResult.SUCCESS.equals(result1.getCode())) {
                drugEnterpriseService = result1.getAccessDrugEnterpriseService();
                drugsEnterprise = result1.getDrugsEnterprise();
            }
        } else {
            drugEnterpriseService = this.getServiceByDep(drugsEnterprise);
        }

        if (null != drugEnterpriseService) {
            result = drugEnterpriseService.scanStock(recipeId, drugsEnterprise);
        }
        LOGGER.info("scanStock recipeId:{}, result:{}", recipeId, JSONUtils.toString(result));
        return result;
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return null;
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return null;
    }

    private ScanRequestBean getScanRequestBean(Recipe recipe, DrugsEnterprise drugsEnterprise) {
        ScanRequestBean scanRequestBean = new ScanRequestBean();
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        List<ScanDrugListBean> scanDrugListBeans = new ArrayList<>();
        for (Recipedetail recipedetail : recipedetails) {
            ScanDrugListBean scanDrugListBean = new ScanDrugListBean();
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), drugsEnterprise.getId());
            if (saleDrugList != null) {
                scanDrugListBean.setDrugCode(saleDrugList.getOrganDrugCode());
                scanDrugListBean.setTotal(recipedetail.getUseTotalDose().toString());
                scanDrugListBean.setUnit(recipedetail.getDrugUnit());
                scanDrugListBeans.add(scanDrugListBean);
            }
        }
        scanRequestBean.setDrugsEnterpriseBean(ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class));
        scanRequestBean.setScanDrugListBeans(scanDrugListBeans);
        scanRequestBean.setOrganId(recipe.getClinicOrgan());
        LOGGER.info("getScanRequestBean scanRequestBean:{}.", JSONUtils.toString(scanRequestBean));
        return scanRequestBean;
    }

    @RpcService
    public String getDrugInventory(Integer depId, Integer drugId, Integer organId){
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        LOGGER.info("getDrugInventory depId:{}, drugId:{}", depId, drugId);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        result.setAccessDrugEnterpriseService(this.getServiceByDep(drugsEnterprise));
        if (drugsEnterprise != null && new Integer(1).equals(drugsEnterprise.getOperationType())) {
            //通过前置机调用
            IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService",IRecipeEnterpriseService.class);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            ScanRequestBean scanRequestBean = getDrugInventoryRequestBean(organId, drugsEnterprise);
            LOGGER.info("getDrugInventory requestBean:{}.", JSONUtils.toString(scanRequestBean));
            HisResponseTO responseTO =  recipeEnterpriseService.scanStock(scanRequestBean);
            LOGGER.info("getDrugInventory responseTO:{}.", JSONUtils.toString(responseTO));
            if (responseTO != null && responseTO.isSuccess()) {
                return (String)responseTO.getExtend().get("inventor");
            } else {
                return "0";
            }
        }else{//通过平台调用
            if (DrugEnterpriseResult.SUCCESS.equals(result.getCode()) && null != result.getAccessDrugEnterpriseService()) {
                return  result.getAccessDrugEnterpriseService().getDrugInventory(drugId, drugsEnterprise, organId);
            } else {
                return "0";
            }
        }

    }

    @RpcService
    public Map<String, Object> test(){
        DrugsDataBean drugsDataBean = new DrugsDataBean();
        drugsDataBean.setOrganId(1003066);
        com.ngari.recipe.recipe.model.RecipeDetailBean recipeDetailBean = new com.ngari.recipe.recipe.model.RecipeDetailBean();
        recipeDetailBean.setDrugId(26);
        recipeDetailBean.setDrugName("拜新同");
        recipeDetailBean.setUseTotalDose(12.0);
        recipeDetailBean.setOrganDrugCode("212222");
        List<com.ngari.recipe.recipe.model.RecipeDetailBean> list = new ArrayList<>();
        list.add(recipeDetailBean);
        drugsDataBean.setRecipeDetailBeans(list);
        return getDrugsEnterpriseInventory(drugsDataBean);
    }


    /**
     * 医生端展示药品库存情况
     * @param drugsDataBean 药品数据
     * @return 药品数据
     */
    @RpcService
    public Map<String, Object> getDrugsEnterpriseInventory(DrugsDataBean drugsDataBean){
        LOGGER.info("getDrugsEnterpriseInventory drugsDataBean:{}.", JSONUtils.toString(drugsDataBean));
        Map<String, Object> result = new LinkedHashMap<>();
        DrugEnterpriseResult drugEnterpriseResult = DrugEnterpriseResult.getSuccess();

        OrganService organService = BasicAPI.getService(OrganService.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        OrganDTO organDTO = organService.getByOrganId(drugsDataBean.getOrganId());
        if (organDTO == null) {
            throw new DAOException("没有查询到机构信息");
        }
        //根据机构获取该机构配置的药企,需要查出药企支持的类型
        OrganAndDrugsepRelationDAO drugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        List<DrugsEnterprise> drugsEnterprises = drugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(drugsDataBean.getOrganId(), 1);
        //获取配置项
        IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);

        //获取机构配置的支持购药方式
        Object payModeDeploy = configService.getConfiguration(drugsDataBean.getOrganId(), "payModeDeploy");
        if(null == payModeDeploy){
            return new HashMap<>();
        }
        List<String> configurations = new ArrayList<>(Arrays.asList((String[])payModeDeploy));

        Map supportOnlineMap = new HashMap();
        List<String> haveInventoryList ;
        //查找非自建药企配送主体为药企的药企
        if (configurations.contains("supportOnline") || configurations.contains("supportTFDS")) {
            for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                drugEnterpriseResult.setAccessDrugEnterpriseService(this.getServiceByDep(drugsEnterprise));
                if (payModeSupport(drugsEnterprise , 1)) {
                    haveInventoryList = new ArrayList<>();
                    //该机构配制配送并且药企支持配送或者药店取药,校验该药企是否支持药品
                    for (com.ngari.recipe.recipe.model.RecipeDetailBean recipeDetailBean : drugsDataBean.getRecipeDetailBeans()) {
                        Integer drugId = recipeDetailBean.getDrugId();
                        SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, drugsEnterprise.getId());
                        if (saleDrugList != null) {
                            //该药企配置了这个药品,可以查询该药品在药企是否有库存了
                            if (new Integer(1).equals(drugsEnterprise.getOperationType())) {
                                //通过前置机调用
                                IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService",IRecipeEnterpriseService.class);
                                ScanRequestBean scanRequestBean = getDrugInventoryRequestBean(drugsDataBean.getOrganId(), drugsEnterprise);
                                LOGGER.info("getDrugInventory requestBean:{}.", JSONUtils.toString(scanRequestBean));
                                HisResponseTO responseTO =  recipeEnterpriseService.scanStock(scanRequestBean);
                                LOGGER.info("getDrugInventory responseTO:{}.", JSONUtils.toString(responseTO));
                                if (responseTO != null && responseTO.isSuccess()) {

                                }
                            }else{//通过平台调用
                                if (DrugEnterpriseResult.SUCCESS.equals(drugEnterpriseResult.getCode()) && null != drugEnterpriseResult.getAccessDrugEnterpriseService()) {
                                    boolean inventoryFlag = drugEnterpriseResult.getAccessDrugEnterpriseService().getDrugInventoryForApp(drugId, drugsEnterprise, drugsDataBean.getOrganId(), 1, recipeDetailBean.getUseTotalDose());
                                    if (inventoryFlag) {
                                        haveInventoryList.add(recipeDetailBean.getDrugName());
                                    }
                                }
                            }
                        }
                    }
                    supportOnlineMap.put(drugsEnterprise.getName(), haveInventoryList);
                }
                if (payModeSupport(drugsEnterprise , 3)) {
                    for (com.ngari.recipe.recipe.model.RecipeDetailBean recipeDetailBean : drugsDataBean.getRecipeDetailBeans()) {
                        Integer drugId = recipeDetailBean.getDrugId();
                        SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, drugsEnterprise.getId());
                        if (saleDrugList != null) {
                            //该药企配置了这个药品,可以查询该药品在药企是否有库存了
                            if (new Integer(1).equals(drugsEnterprise.getOperationType())) {

                            }else{//通过平台调用
                                if (DrugEnterpriseResult.SUCCESS.equals(drugEnterpriseResult.getCode()) && null != drugEnterpriseResult.getAccessDrugEnterpriseService()) {
                                    boolean inventoryFlag = drugEnterpriseResult.getAccessDrugEnterpriseService().getDrugInventoryForApp(drugId, drugsEnterprise, drugsDataBean.getOrganId(), 2, recipeDetailBean.getUseTotalDose());

                                }
                            }
                        }
                    }
                }
            }
            result.put("配送到家", supportOnlineMap);
        }
        if (configurations.contains("supportToHos")) {
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            IHisConfigService iHisConfigService = ApplicationUtils.getBaseService(IHisConfigService.class);
            if (iHisConfigService.isHisEnable(drugsDataBean.getOrganId())){
                //到院取药,需要验证HIS
                RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
                List<String> list = new ArrayList<>();
                for (com.ngari.recipe.recipe.model.RecipeDetailBean recipeDetailBean : drugsDataBean.getRecipeDetailBeans()) {
                    List<Recipedetail> recipedetails = new ArrayList<>();
                    Recipedetail recipedetail = ObjectCopyUtils.convert(recipeDetailBean, Recipedetail.class);
                    OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(drugsDataBean.getOrganId(), recipeDetailBean.getOrganDrugCode(), recipeDetailBean.getDrugId());
                    if (organDrugList != null) {
                        recipedetail.setPack(organDrugList.getPack());
                        recipedetail.setDrugUnit(organDrugList.getUnit());
                        recipedetail.setProducerCode(organDrugList.getProducerCode());
                    }
                    recipedetails.add(recipedetail);
                    DrugInfoResponseTO response = service.scanDrugStock(recipedetails, drugsDataBean.getOrganId());
                    if (response != null && Integer.valueOf(0).equals(response.getMsgCode())) {
                        //表示有库存
                        list.add(recipeDetailBean.getDrugName());
                    }
                }
                result.put("到院取药", list);
            }
        }
        if (configurations.contains("supportDownload")) {
            //下载处方,只要配置这个开关,默认都支持
            List<String> list = new ArrayList<>();
            for (com.ngari.recipe.recipe.model.RecipeDetailBean recipeDetailBean : drugsDataBean.getRecipeDetailBeans()) {
                list.add(recipeDetailBean.getDrugName());
            }
            result.put("下载处方", list);
        }
        return result;
    }

    public static void main(String[] args) {

    }

    private static boolean payModeSupport(DrugsEnterprise drugsEnterprise, Integer type){
        Integer[] online_pay = {RecipeBussConstant.DEP_SUPPORT_ONLINE,RecipeBussConstant.DEP_SUPPORT_COD,RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS,
                RecipeBussConstant.DEP_SUPPORT_COD_TFDS,RecipeBussConstant.DEP_SUPPORT_COD,RecipeBussConstant.DEP_SUPPORT_ALL};
        List<Integer> online_pay_list = Arrays.asList(online_pay);
        Integer[] to_tfds = {RecipeBussConstant.DEP_SUPPORT_TFDS,RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS,RecipeBussConstant.DEP_SUPPORT_COD_TFDS,
                RecipeBussConstant.DEP_SUPPORT_ALL};
        List<Integer> to_tfds_list = Arrays.asList(to_tfds);
        if (new Integer(1).equals(type)) {
            //支持配送
            return online_pay_list.contains(drugsEnterprise.getPayModeSupport());
        } else if (new Integer(2).equals(type)) {
            //支持到院取药
            if ("commonSelf".equals(drugsEnterprise.getCallSys())) {
                return true;
            } else {
                return false;
            }
        } else if (new Integer(3).equals(drugsEnterprise.getPayModeSupport())) {
            //支持药店取药
            return to_tfds_list.contains(drugsEnterprise.getPayModeSupport());
        } else {
            return true;
        }
    }

    /**
     * 封装前置机所需参数
     * @param organId
     * @param drugsEnterprise
     * @return
     */
    private ScanRequestBean getDrugInventoryRequestBean( Integer organId,DrugsEnterprise drugsEnterprise) {
        //TODO ScanRequestBean
        ScanRequestBean scanRequestBean = new ScanRequestBean();
        ThirdEnterpriseCallService thirdEnterpriseCallService =ApplicationUtils.getRecipeService(
                ThirdEnterpriseCallService.class, "takeDrugService");
        Map drugInventoryRequestMap=thirdEnterpriseCallService.drugInventoryRequestMap.get();
        LOGGER.info("getDrugInventoryRequestBean map: {}",JSONUtils.toString(drugInventoryRequestMap));
        List<ScanDrugListBean> scanDrugListBeans = new ArrayList<>();
        ScanDrugListBean scanDrugListBean = new ScanDrugListBean();
        scanDrugListBean.setDrugCode(drugInventoryRequestMap.get("organDurgList_drugCode")+"");
        scanDrugListBean.setTotal(drugInventoryRequestMap.get("total")+"");
        scanDrugListBean.setUnit(drugInventoryRequestMap.get("unit")+"");
        scanDrugListBeans.add(scanDrugListBean);
        scanRequestBean.setDrugsEnterpriseBean(ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class));
        scanRequestBean.setScanDrugListBeans(scanDrugListBeans);
        scanRequestBean.setOrganId(organId);
        LOGGER.info("getDrugInventoryRequestBean :{}.", JSONUtils.toString(scanRequestBean));
        return scanRequestBean;
    }

    /**
     * 药师审核通过通知消息
     *
     * @param recipeId  处方ID
     * @param checkFlag 审核结果
     * @return
     */
    @RpcService
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag) {
        DrugEnterpriseResult result = getServiceByRecipeId(recipeId);
        if (DrugEnterpriseResult.SUCCESS.equals(result.getCode()) && null != result.getAccessDrugEnterpriseService()) {
            result = result.getAccessDrugEnterpriseService().pushCheckResult(recipeId, checkFlag, result.getDrugsEnterprise());
        }
        LOGGER.info("pushCheckResult recipeId:{}, result:{}", recipeId, JSONUtils.toString(result));
        return result;
    }

    /**
     * 查找供应商
     *
     * @param recipeIds 处方列表
     * @param ext       额外信息
     * @return 供应商信息
     */
    @RpcService
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise drugsEnterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (drugsEnterprise != null && new Integer(1).equals(drugsEnterprise.getOperationType())) {
            //通过前置机调用
            IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService",IRecipeEnterpriseService.class);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeIds.get(0));
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            ScanRequestBean scanRequestBean = getScanRequestBean(recipe, drugsEnterprise);
            scanRequestBean.setExt(ext);
            if (recipeExtend != null && StringUtils.isNotEmpty(recipeExtend.getRxid())) {
                scanRequestBean.setRxid(recipeExtend.getRxid());
            }
            LOGGER.info("findSupportDep 发给前置机入参:{}.", JSONUtils.toString(scanRequestBean));
            List<DepDetailBean> depDetailBeans =  recipeEnterpriseService.findSupportDep(scanRequestBean);
            LOGGER.info("findSupportDep 前置机出参:{}.", JSONUtils.toString(depDetailBeans));
            result.setObject(ObjectCopyUtils.convert(depDetailBeans, com.ngari.recipe.drugsenterprise.model.DepDetailBean.class));
            return result;
        }
        if (CollectionUtils.isNotEmpty(recipeIds) && null != drugsEnterprise) {
            AccessDrugEnterpriseService drugEnterpriseService = this.getServiceByDep(drugsEnterprise);
            result = drugEnterpriseService.findSupportDep(recipeIds, ext, drugsEnterprise);
            LOGGER.info("findSupportDep recipeIds={}, DrugEnterpriseResult={}", JSONUtils.toString(recipeIds), JSONUtils.toString(result));
        } else {
            LOGGER.warn("findSupportDep param error. recipeIds={}, drugsEnterprise={}", JSONUtils.toString(recipeIds), JSONUtils.toString(drugsEnterprise));
        }

        return result;
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return null;
    }

    /**
     * 药品库存同步
     *
     * @return
     */
    @RpcService
    public DrugEnterpriseResult syncEnterpriseDrug() {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);

        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findAllDrugsEnterpriseByStatus(1);
        if (CollectionUtils.isNotEmpty(drugsEnterpriseList)) {
            AccessDrugEnterpriseService drugEnterpriseService;
            for (DrugsEnterprise drugsEnterprise : drugsEnterpriseList) {
                if (null != drugsEnterprise) {
                    List<Integer> drugIdList = saleDrugListDAO.findSynchroDrug(drugsEnterprise.getId());
                    if (CollectionUtils.isNotEmpty(drugIdList)) {
                        drugEnterpriseService = this.getServiceByDep(drugsEnterprise);
                        if (null != drugEnterpriseService) {
                            LOGGER.info("syncDrugTask 开始同步药企[{}]药品，药品数量[{}]", drugsEnterprise.getName(), drugIdList.size());
                            drugEnterpriseService.syncEnterpriseDrug(drugsEnterprise, drugIdList);
                        }
                    } else {
                        LOGGER.warn("syncDrugTask 药企[{}]无可同步药品.", drugsEnterprise.getName());
                    }
                }
            }
        }

        return result;
    }


    @RpcService
    public void updateAccessTokenById(Integer code, Integer depId) {
        AccessDrugEnterpriseService drugEnterpriseService = getBean(COMMON_SERVICE, AccessDrugEnterpriseService.class);
        drugEnterpriseService.updateAccessTokenById(code, depId);
    }

    public String updateAccessToken(List<Integer> drugsEnterpriseIds) {
        AccessDrugEnterpriseService drugEnterpriseService = getBean(COMMON_SERVICE, AccessDrugEnterpriseService.class);
        return drugEnterpriseService.updateAccessToken(drugsEnterpriseIds);
    }

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {

    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return null;
    }

    @RpcService
    public void updateAccessTokenByDep(DrugsEnterprise drugsEnterprise) {
        AccessDrugEnterpriseService service = getServiceByDep(drugsEnterprise);
        service.tokenUpdateImpl(drugsEnterprise);
    }

    /**
     * 根据单个处方ID获取具体药企实现
     *
     * @param recipeId
     * @return
     */
    public DrugEnterpriseResult getServiceByRecipeId(Integer recipeId) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (null == recipeId) {
            result.setCode(DrugEnterpriseResult.FAIL);
            result.setMsg("处方ID为空");
        }

        if (DrugEnterpriseResult.SUCCESS.equals(result.getCode())) {
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            //PS:药企ID取的是订单表的药企ID
            Integer depId = recipeOrderDAO.getEnterpriseIdByRecipeId(recipeId);
            if (depId==null){
                depId = recipeDAO.getByRecipeId(recipeId).getEnterpriseId();
            }
            if (null != depId) {
                DrugsEnterprise dep = drugsEnterpriseDAO.get(depId);
                if (null != dep) {
                    result.setAccessDrugEnterpriseService(this.getServiceByDep(dep));
                    result.setDrugsEnterprise(dep);
                } else {
                    result.setCode(DrugEnterpriseResult.FAIL);
                    result.setMsg("药企" + depId + "未找到");
                }
            } else {
                result.setCode(DrugEnterpriseResult.FAIL);
                result.setMsg("处方单" + recipeId + "未分配药企");
            }
        }

        LOGGER.info("getServiceByRecipeId recipeId:{}, result:{}", recipeId, result.toString());
        return result;
    }

    /**
     * 通过药企实例获取具体实现
     *
     * @param drugsEnterprise
     * @return
     */
    public AccessDrugEnterpriseService getServiceByDep(DrugsEnterprise drugsEnterprise) {
        AccessDrugEnterpriseService drugEnterpriseService = null;
        if (drugsEnterprise != null && new Integer(1).equals(drugsEnterprise.getOperationType())) {
            return ApplicationUtils.getService(RemoteDrugEnterpriseService.class, "remoteDrugEnterpriseService");
        }
        if (null != drugsEnterprise) {
            //先获取指定实现标识，没有指定则根据帐号名称来获取
            String callSys = StringUtils.isEmpty(drugsEnterprise.getCallSys()) ? drugsEnterprise.getAccount() : drugsEnterprise.getCallSys();
            String beanName = COMMON_SERVICE;
            if (StringUtils.isNotEmpty(callSys)) {
                beanName = callSys + "RemoteService";
            }
            try {
                LOGGER.info("getServiceByDep 获取[{}]协议实现.service=[{}]", drugsEnterprise.getName(), beanName);
                drugEnterpriseService = getBean(beanName, AccessDrugEnterpriseService.class);
            } catch (Exception e) {
                LOGGER.warn("getServiceByDep 未找到[{}]药企实现，使用通用协议处理. beanName={}", drugsEnterprise.getName(), beanName,e);
                drugEnterpriseService = getBean(COMMON_SERVICE, AccessDrugEnterpriseService.class);
            }
        }

        return drugEnterpriseService;
    }

    /**
     * 获取药企帐号
     *
     * @param depId
     * @return
     */
    public String getDepAccount(Integer depId) {
        if (null == depId) {
            return null;
        }
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        return drugsEnterpriseDAO.getAccountById(depId);
    }

    /**
     * 获取钥世圈订单详情URL
     *
     * @param recipe
     * @return
     */
    public String getYsqOrderInfoUrl(Recipe recipe) {
        RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);
        String backUrl = "";
        String ysqUrl = cacheService.getParam(ParameterConstant.KEY_YSQ_SKIP_URL);
        if (RecipeStatusConstant.FINISH != recipe.getStatus()) {
            backUrl = ysqUrl + "Order/Index?id=0&inbillno=" + recipe.getClinicOrgan() + YsqRemoteService.YSQ_SPLIT + recipe.getRecipeCode();
        }
        return backUrl;
    }

    /**
     *  获取运费
     * @return
     */
    @RpcService
    public Map<String, Object> getExpressFee(Map<String, Object> parames) {
        LOGGER.info("getExpressFee parames:{}.", JSONUtils.toString(parames));
        Map<String, Object> result = new HashMap<>();
        if (parames == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "获取运费参数不能为空");
        }
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        Integer depId = (Integer) parames.get("depId"); //获取药企ID
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        if (drugsEnterprise == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "获取药企失败");
        }
        if (new Integer(1).equals(drugsEnterprise.getExpressFeeType())) {
            //此时运费为从第三方获取
            Integer recipeId = (Integer) parames.get("recipeId"); //获取处方ID
            String province = (String) parames.get("province"); //获取省份
            String city = (String) parames.get("city"); //获取市
            String district = (String) parames.get("district"); //获取区县
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
            String rxid = recipeExtend.getRxid();
            //通过前置机调用
            IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService",IRecipeEnterpriseService.class);
            EnterpriseResTo enterpriseResTo = new EnterpriseResTo();
            enterpriseResTo.setOrganId(recipe.getClinicOrgan());
            enterpriseResTo.setDepId(depId.toString());
            enterpriseResTo.setRid(rxid);
            enterpriseResTo.setProvince(province);
            enterpriseResTo.setCity(city);
            enterpriseResTo.setDistrict(district);
            LOGGER.info("getExpressFee enterpriseResTo:{}.", JSONUtils.toString(enterpriseResTo));
            HisResponseTO hisResponseTO = recipeEnterpriseService.getEnterpriseExpress(enterpriseResTo);
            LOGGER.info("getExpressFee hisResponseTO:{}.", JSONUtils.toString(hisResponseTO));
            if (hisResponseTO != null && hisResponseTO.isSuccess()) {
                //表示获取第三方运费成功
                Map<String, Object> extend = hisResponseTO.getExtend();
                Boolean expressFeeFlag = (Boolean)extend.get("result");
                Object expressFee = extend.get("postagePrice");
                if (expressFeeFlag) {
                    result.put("expressFee", expressFee);
                } else {
                    result.put("expressFee", 0);
                }
                result.put("expressFeeType", 1);
            } else {
                //获取第三方失败 默认从平台获取
                LOGGER.info("getExpressFee 获取第三方运费失败,默认从平台获取");
                result.put("expressFeeType", 0);
            }

        } else {
            //此时运费从平台获取
            result.put("expressFeeType", 0);
        }
        return result;
    }

    private String getAddressDic(String area) {
        if (StringUtils.isNotEmpty(area)) {
            try {
                return DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(area);
            } catch (ControllerException e) {
                LOGGER.error("getAddressDic 获取地址数据类型失败*****area:" + area,e);
            }
        }
        return "";
    }

    /**
     * 通过机构分页查找药品库存
     * @param organId
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public List<Map<String,Object>> findEnterpriseStockByPage(String  organId, Integer start, Integer limit){
        LOGGER.info("syncEnterpriseStockByOrganIdForHis organId:{}, start:{}, limit:{}", organId, start,limit);
        IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService",IRecipeEnterpriseService.class);
        FindEnterpriseStockByPageTo findEnterpriseStockByPageTo = new FindEnterpriseStockByPageTo();
        findEnterpriseStockByPageTo.setOrgan(organId);
        findEnterpriseStockByPageTo.setStart(start);
        findEnterpriseStockByPageTo.setLimit(limit);
        LOGGER.info("findEnterpriseStockByPage requestBean:{}.", JSONUtils.toString(findEnterpriseStockByPageTo));
        HisResponseTO<List<Map<String,Object>>> responseTO =  recipeEnterpriseService.findEnterpriseStockByPage(findEnterpriseStockByPageTo);
        LOGGER.info("String responseTO:{}.", JSONUtils.toString(responseTO));
        if(responseTO.isSuccess()){
            return responseTO.getData();
        }
        return new ArrayList<>();
    }
}
