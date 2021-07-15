package recipe.hisservice;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.ngari.base.cdr.service.IDiseaseService;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.base.patient.model.HealthCardBean;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.his.regulation.entity.RegulationRecipeIndicatorsReq;
import com.ngari.patient.dto.*;
import com.ngari.patient.service.*;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.OrganDrugChangeBean;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.*;
import com.ngari.recipe.hisprescription.service.IQueryRecipeService;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.controller.exception.ControllerException;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.base.constant.ErrorCode;
import eh.cdr.api.service.IDocIndexService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.bussutil.UsePathwaysFilter;
import recipe.bussutil.UsingRateFilter;
import recipe.dao.*;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.manager.EmrRecipeManager;
import recipe.service.OrganDrugListService;
import recipe.service.RecipeServiceSub;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.DateConversion;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static recipe.dao.DrugMakingMethodDao.log;


/**
 * 浙江互联网医院处方查询接口
 * created by shiyuping on 2018/11/30
 */
@RpcBean("remoteQueryRecipeService")
public class QueryRecipeService implements IQueryRecipeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryRecipeService.class);

    @Resource
    private RecipeExtendDAO recipeExtendDAO;
    @Resource
    private IDocIndexService docIndexService;

    /**
     * 用于sendRecipeToHIS 推送处方mq后 查询接口
     *
     * @param queryRecipeReqDTO
     * @return
     */
    @Override
    @RpcService
    public QueryRecipeResultDTO queryRecipeInfo(QueryRecipeReqDTO queryRecipeReqDTO) {
        LOGGER.info("queryRecipeInfo入參：{}", JSONUtils.toString(queryRecipeReqDTO));
        QueryRecipeResultDTO resultDTO = new QueryRecipeResultDTO();
        if (StringUtils.isEmpty(queryRecipeReqDTO.getOrganId())) {
            resultDTO.setMsgCode(-1);
            resultDTO.setMsg("缺少组织机构编码");
            return resultDTO;
        }
        String recipeCode = queryRecipeReqDTO.getRecipeID();
        if (StringUtils.isEmpty(recipeCode)) {
            resultDTO.setMsgCode(-1);
            resultDTO.setMsg("缺少处方编码");
            return resultDTO;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);
        //转换机构组织编码
        Integer clinicOrgan = RecipeServiceSub.transformOrganIdToClinicOrgan(queryRecipeReqDTO.getOrganId());
        if (null == clinicOrgan) {
            resultDTO.setMsgCode(-1);
            resultDTO.setMsg("平台未匹配到该组织机构编码");
            return resultDTO;
        }
        Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode, clinicOrgan);
        if (null == recipe) {
            resultDTO.setMsgCode(-1);
            resultDTO.setMsg("找不到处方");
            return resultDTO;
        }
        List<Recipedetail> details = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        PatientBean patientBean = iPatientService.get(recipe.getMpiid());
        HealthCardBean cardBean = iPatientService.getHealthCard(recipe.getMpiid(), recipe.getClinicOrgan(), "2");
        //拼接返回数据
        QueryRecipeInfoDTO infoDTO = splicingBackData(details, recipe, patientBean, cardBean);
        resultDTO.setMsgCode(0);
        resultDTO.setData(infoDTO);
        LOGGER.info("queryRecipeInfo res={}", JSONUtils.toString(resultDTO));
        return resultDTO;
    }

    /**
     * 用于sendRecipeToHIS 推送处方mq后 查询接口
     *
     * @param req
     * @return
     */
    @Override
    @RpcService
    public QueryRecipeResultDTO queryPlatRecipeByRecipeId(QueryPlatRecipeInfoByDateDTO req) {
        LOGGER.info("queryPlatRecipeByPatientNameAndDate req={}", JSONUtils.toString(req));
        QueryRecipeResultDTO resultDTO = new QueryRecipeResultDTO();
        if (StringUtils.isEmpty(req.getRecipeId())) {
            resultDTO.setMsgCode(-1);
            resultDTO.setMsg("处方序号不能为空");
            return resultDTO;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);
        /*//根据患者姓名和时间查询未支付处方
        List<Recipe> recipes = recipeDAO.findNoPayRecipeListByPatientNameAndDate(req.getPatientName(), req.getOrganId(), req.getStartDate(), req.getEndDate());*/
        Recipe recipe = recipeDAO.getByRecipeId(Integer.valueOf(req.getRecipeId()));
        if (null == recipe) {
            resultDTO.setMsgCode(-1);
            resultDTO.setMsg("找不到处方");
            return resultDTO;
        }
        List<Recipedetail> details = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        PatientBean patientBean = iPatientService.get(recipe.getMpiid());
        //拼接返回数据
        QueryRecipeInfoDTO infoDTO = splicingBackData(details, recipe, patientBean, null);
        resultDTO.setMsgCode(0);
        resultDTO.setData(infoDTO);
        LOGGER.info("queryPlatRecipeByPatientNameAndDate res={}", JSONUtils.toString(resultDTO));
        return resultDTO;
    }

    @Override
    @RpcService
    public List<RegulationRecipeIndicatorsDTO> queryRegulationRecipeData(Integer organId, Date startDate, Date endDate, Boolean checkFlag) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        String start = DateConversion.formatDateTimeWithSec(startDate);
        String end = DateConversion.formatDateTimeWithSec(endDate);
        List<Recipe> recipeList = recipeDAO.findSyncRecipeListByOrganId(organId, start, end, checkFlag);
        if (CollectionUtils.isEmpty(recipeList)) {
            return new ArrayList<>();
        }
        HisSyncSupervisionService service = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
        List<RegulationRecipeIndicatorsReq> request = new ArrayList<>(recipeList.size());
        LOGGER.info("queryRegulationRecipeData start");
        service.splicingBackRecipeData(recipeList, request);
        List<RegulationRecipeIndicatorsDTO> result = ObjectCopyUtils.convert(request, RegulationRecipeIndicatorsDTO.class);
        LOGGER.info("queryRegulationRecipeData data={}", JSONUtils.toString(result));
        return result;
    }

    @Override
    @RpcService
    public List<RegulationRecipeIndicatorsDTO> queryRegulationRecipeDataForSH(Integer organId, Date startDate, Date endDate, Boolean updateFlag) {
        updateFlag = updateFlag == null ? Boolean.TRUE : updateFlag;

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        String start = DateConversion.formatDateTimeWithSec(startDate);
        String end = DateConversion.formatDateTimeWithSec(endDate);
        List<Recipe> recipeList = recipeDAO.findSyncRecipeListByOrganIdForSH(organId, start, end, updateFlag);
        if (CollectionUtils.isEmpty(recipeList)) {
            return new ArrayList<>();
        }
        HisSyncSupervisionService service = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
        List<RegulationRecipeIndicatorsReq> request = new ArrayList<>(recipeList.size());
        LOGGER.info("queryRegulationRecipeDataForSH start:organId={},startDate={},endDate={},updateFlag={}", organId, startDate, endDate, updateFlag);
        service.splicingBackRecipeData(recipeList, request);
        List<RegulationRecipeIndicatorsDTO> result = ObjectCopyUtils.convert(request, RegulationRecipeIndicatorsDTO.class);
        LOGGER.info("queryRegulationRecipeDataForSH data={}", JSONUtils.toString(result));
        return result;
    }

    /**
     * 拼接处方返回信息数据
     *
     * @param details
     * @param recipe
     * @param patient
     * @param card
     */
    private QueryRecipeInfoDTO splicingBackData(List<Recipedetail> details, Recipe recipe, PatientBean patient, HealthCardBean card) {
        QueryRecipeInfoDTO recipeDTO = null;
        try {
            Integer recipeId = recipe.getRecipeId();
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
            try {
                if (!ValidateUtil.integerIsEmpty(recipeExtend.getDocIndexId())) {
                    Map<String, Object> medicalInfoBean = docIndexService.getMedicalInfoByDocIndexId(recipeExtend.getDocIndexId());
                    recipeDTO.setMedicalInfoBean(medicalInfoBean);
                }
            } catch (Exception e) {
                LOGGER.error("RecipeHisService sendRecipe  medicalInfoBean error", e);
            }

            EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
            recipeDTO = new QueryRecipeInfoDTO();
            //拼接处方信息
            //处方号
            recipeDTO.setRecipeID(recipe.getRecipeCode());
            //机构id
            recipeDTO.setOrganId(String.valueOf(recipe.getClinicOrgan()));
            //处方id
            recipeDTO.setPlatRecipeID(String.valueOf(recipe.getRecipeId()));
            //挂号序号
            //recipeDTO.setRegisterId(String.valueOf(recipe.getClinicId()));
            if (recipe.getClinicId() != null) {
                IRevisitExService iRevisitExService = RevisitAPI.getService(IRevisitExService.class);
                RevisitExDTO consultExDTO = iRevisitExService.getByConsultId(recipe.getClinicId());
                if (consultExDTO != null) {
                    recipeDTO.setRegisterId(consultExDTO.getRegisterNo());
                }
            }
            //签名日期
            recipeDTO.setDatein(recipe.getSignDate());
            //是否支付
            recipeDTO.setIsPay((null != recipe.getPayFlag()) ? Integer.toString(recipe.getPayFlag()) : null);
            //返回部门code
            DepartmentService service = BasicAPI.getService(DepartmentService.class);
            DepartmentDTO departmentDTO = service.getById(recipe.getDepart());
            if (departmentDTO != null) {
                recipeDTO.setDeptID(departmentDTO.getCode());
                //科室名
                recipeDTO.setDeptName(departmentDTO.getName());
                AppointDepartService appointDepartService = ApplicationUtils.getBasicService(AppointDepartService.class);
                AppointDepartDTO appointDepart = appointDepartService.findByOrganIDAndDepartID(recipe.getClinicOrgan(), recipe.getDepart());
                recipeDTO.setDeptCode((null != appointDepart) ? appointDepart.getAppointDepartCode() : "");
            }
            //处方类型
            recipeDTO.setRecipeType((null != recipe.getRecipeType()) ? recipe.getRecipeType().toString() : null);
            //复诊id
            recipeDTO.setClinicID((null != recipe.getClinicId()) ? Integer.toString(recipe.getClinicId()) : null);
            //转换平台医生id为工号返回his
            EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
            if (recipe.getDoctor() != null) {
                String jobNumber = iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart());
                //医生工号
                recipeDTO.setDoctorID(jobNumber);
                //医生姓名
                recipeDTO.setDoctorName(recipe.getDoctorName());
                //医生身份证
                DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
                DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getDoctor());
                if (doctorDTO != null) {
                    recipeDTO.setDoctorIDCard(doctorDTO.getIdNumber());
                }
            }
            //审核医生
            if (recipe.getChecker() != null) {
                String jobNumberChecker = iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getChecker(), recipe.getClinicOrgan(), recipe.getDepart());
                recipeDTO.setAuditDoctor(jobNumberChecker);
                //审核状态
                recipeDTO.setAuditCheckStatus("1");
                //date 20200225 审方时间
                recipeDTO.setCheckDate(recipe.getCheckDate());
            } else {
                recipeDTO.setAuditDoctor(recipeDTO.getDoctorID());
                recipeDTO.setAuditCheckStatus("0");
                //date 20200225 审方时间
                recipeDTO.setCheckDate(new Date());
            }
            //本处方收费类型 1市医保 2省医保 3自费---杭州市互联网-市医保
            recipeDTO.setMedicalPayFlag(getMedicalType(recipe.getMpiid(), recipe.getClinicOrgan()));
            //处方金额
            recipeDTO.setRecipeFee(String.valueOf(recipe.getActualPrice()));
            //自付比例
            /*recipeDTO.setPayScale("");*/
            //主诉等等四个字段

            if (recipeExtend != null) {
                EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
                if (StringUtils.isNotEmpty(recipeExtend.getMainDieaseDescribe())) {
                    //主诉
                    recipeDTO.setBRZS(recipeExtend.getMainDieaseDescribe());
                }
                if (StringUtils.isNotEmpty(recipeExtend.getPhysicalCheck())) {
                    //体格检查
                    recipeDTO.setTGJC(recipeExtend.getPhysicalCheck());
                }
                if (StringUtils.isNotEmpty(recipeExtend.getHistoryOfPresentIllness())) {
                    //现病史
                    recipeDTO.setXBS(recipeExtend.getHistoryOfPresentIllness());
                }
                if (StringUtils.isNotEmpty(recipeExtend.getHandleMethod())) {
                    //处理方法
                    recipeDTO.setCLFF(recipeExtend.getHandleMethod());
                }
                recipeDTO.setRecipeExtendBean(ObjectCopyUtils.convert(recipeExtend, RecipeExtendBean.class));
            }
            //获取医院诊断内码
            recipeDTO.setIcdRdn(getIcdRdn(recipe.getClinicOrgan(), recipe.getOrganDiseaseId(), recipe.getOrganDiseaseName()));
            //icd诊断码
            recipeDTO.setIcdCode(getCode(recipe.getOrganDiseaseId()));
            //icd诊断名称
            recipeDTO.setIcdName(getCode(recipe.getOrganDiseaseName()));

            if (null != patient) {
                // 患者信息
                String idCard = patient.getCertificate();
                if (StringUtils.isNotEmpty(idCard)) {
                    //没有身份证儿童的证件处理
                    String childFlag = "-";
                    if (idCard.contains(childFlag)) {
                        idCard = idCard.split(childFlag)[0];
                    }
                }
                recipeDTO.setCertID(idCard);
                recipeDTO.setPatientName(patient.getPatientName());
                recipeDTO.setMobile(patient.getMobile());
                recipeDTO.setPatientSex(patient.getPatientSex());
                // 简要病史
                recipeDTO.setDiseasesHistory(recipe.getOrganDiseaseName());
                // 患者年龄
                recipeDTO.setPatinetAge(getAge(patient.getBirthday()));
                // 就诊人手机号
                if (StringUtils.isNotBlank(patient.getLoginId())) {
                    PatientService patientService = BasicAPI.getService(PatientService.class);
                    List<PatientDTO> patientList = patientService.findOwnPatient(patient.getLoginId());
                    if (null != patientList && patientList.size() > 0) {
                        PatientDTO userInfo = patientList.get(0);
                        UserInfoDTO infoDTO = new UserInfoDTO();
                        infoDTO.setUserMobile(userInfo.getMobile());
                        infoDTO.setUserBirthDay(userInfo.getBirthday());
                        infoDTO.setUserSex(userInfo.getPatientSex());
                        infoDTO.setUsernName(userInfo.getPatientName());
                        recipeDTO.setUserInfo(infoDTO);
                    }
                }
            }
            //设置健康卡
            if (recipe.getClinicId() != null) {
                IRevisitExService iRevisitExService = RevisitAPI.getService(IRevisitExService.class);
                RevisitExDTO consultExDTO = iRevisitExService.getByConsultId(recipe.getClinicId());
                if (consultExDTO != null) {
                    recipeDTO.setCardType(consultExDTO.getCardType());
                    recipeDTO.setCardNo(consultExDTO.getCardId());
                }
            }
            //设置卡
            if (null != card && StringUtils.isEmpty(recipeDTO.getCardNo())) {
                recipeDTO.setCardType(card.getCardType());
                recipeDTO.setCardNo(card.getCardId());
            }


            if (recipe.getGiveMode() == null) {
                //如果为nul则默认为医院取药
                recipeDTO.setDeliveryType("0");
            } else {
                //根据处方单设置配送方式
                switch (recipe.getGiveMode()) {
                    //配送到家
                    case 1:
                        recipeDTO.setDeliveryType("1");
                        break;
                    //医院取药
                    case 2:
                        recipeDTO.setDeliveryType("0");
                        break;
                    //药店取药
                    case 3:
                        recipeDTO.setDeliveryType("2");
                        break;
                    default:
                        LOGGER.warn("queryRecipe splicingBackData GiveMode = {}", recipe.getGiveMode());
                }
            }

            splicingBackDataForRecipeDetails(recipe.getClinicOrgan(), details, recipeDTO);
            LOGGER.info("queryRecipe splicingBackData recipeDTO:{}", JSONUtils.toString(recipeDTO));
        } catch (Exception e) {
            LOGGER.error("queryRecipe splicingBackData error", e);
        }

        return recipeDTO;
    }

    private int getAge(Date birthDay) {
        int age = 0;
        if (null == birthDay) {
            return age;
        }
        try {
            Calendar cal = Calendar.getInstance();
            if (cal.before(birthDay)) {
                return age;
            }
            int yearNow = cal.get(Calendar.YEAR);
            int monthNow = cal.get(Calendar.MONTH);
            int dayOfMonthNow = cal.get(Calendar.DAY_OF_MONTH);
            cal.setTime(birthDay);

            int yearBirth = cal.get(Calendar.YEAR);
            int monthBirth = cal.get(Calendar.MONTH);
            int dayOfMonthBirth = cal.get(Calendar.DAY_OF_MONTH);

            age = yearNow - yearBirth;

            if (monthNow <= monthBirth) {
                if (monthNow == monthBirth) {
                    if (dayOfMonthNow < dayOfMonthBirth) {
                        age--;
                    }
                } else {
                    age--;
                }
            }
        } catch (Exception e) {
            LOGGER.error("根据出生日期或者年龄异常,birthday={}", birthDay, e);
        }
        return age;
    }


    private void splicingBackDataForRecipeDetails(Integer clinicOrgan, List<Recipedetail> details, QueryRecipeInfoDTO recipeDTO) throws ControllerException {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
        Double drugTotalNumber = new Double(0);
        BigDecimal drugTotalAmount = new BigDecimal(0);

        //拼接处方明细
        if (CollectionUtils.isNotEmpty(details)) {
            List<OrderItemDTO> orderList = new ArrayList<>();
            for (Recipedetail detail : details) {
                OrderItemDTO orderItem = new OrderItemDTO();
                //药品Id
                orderItem.setDrugId(detail.getDrugId());
                //处方明细id
                orderItem.setOrderID(Integer.toString(detail.getRecipeDetailId()));
                //医院药品编码  药品唯一标识
                orderItem.setDrcode(detail.getOrganDrugCode());
                //医院药品名
                orderItem.setDrname(detail.getDrugName());
                //药品规格
                orderItem.setDrmodel(detail.getDrugSpec());
                //包装单位
                orderItem.setPackUnit(detail.getDrugUnit());
                //用药天数
                //date 20200526
                //修改推送给医院的用药天数big转String
                orderItem.setUseDays(Integer.toString(detail.getUseDays()));
                //设置
                //用法
                orderItem.setAdmission(UsePathwaysFilter.filterNgari(clinicOrgan, detail.getUsePathways()));
                //频次
                orderItem.setFrequency(UsingRateFilter.filterNgari(clinicOrgan, detail.getUsingRate()));
                //机构的频次代码
                orderItem.setOrganUsingRate(detail.getOrganUsingRate());
                //机构的用药代码
                orderItem.setOrganUsePathways(detail.getOrganUsePathways());
                //医保频次
                orderItem.setMedicalFrequency(UsingRateFilter.filterNgariByMedical(clinicOrgan, detail.getUsingRate()));
                //单次剂量
                if (StringUtils.isNotEmpty(detail.getUseDoseStr())) {
                    orderItem.setDosage(detail.getUseDoseStr());
                } else {
                    orderItem.setDosage((null != detail.getUseDose()) ? Double.toString(detail.getUseDose()) : null);
                }
                //剂量单位
                orderItem.setDrunit(detail.getUseDoseUnit());

                OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(clinicOrgan, detail.getOrganDrugCode(), detail.getDrugId());
                if (null != organDrugList) {
                    //药品产地名称
                    orderItem.setDrugManf(organDrugList.getProducer());
                    //药品产地编码
                    orderItem.setDrugManfCode(organDrugList.getProducerCode());
                    //药品单价
                    orderItem.setPrice(String.valueOf(organDrugList.getSalePrice()));
                    // 订单药品总价
                    try {
                        BigDecimal num = null != detail.getUseTotalDose() ? new BigDecimal(detail.getUseTotalDose()) : new BigDecimal(0);
                        BigDecimal price = null != organDrugList.getSalePrice() ? organDrugList.getSalePrice() : new BigDecimal(0);
                        BigDecimal totalPrice = num.multiply(price);
                        drugTotalAmount = drugTotalAmount.add(totalPrice);
                    } catch (Exception e) {
                        LOGGER.error("计算处方订单药品总价异常=", e);
                    }
                    //医保对应代码
                    orderItem.setMedicalDrcode(organDrugList.getMedicalDrugCode());
                    //剂型代码 --
                    orderItem.setDrugFormCode(organDrugList.getDrugFormCode());
                    //医保剂型代码--
                    orderItem.setMedicalDrugFormCode(organDrugList.getMedicalDrugFormCode());
                    //剂型名称
                    orderItem.setDrugFormName(organDrugList.getDrugForm());
                }
                /*
                 * //每日剂量 转换成两位小数 DecimalFormat df = new DecimalFormat("0.00");
                 * String dosageDay =
                 * df.format(getFrequency(detail.getUsingRate(
                 * ))*detail.getUseDose()
                 * );
                 */
                // 开药数量
                orderItem.setTotalDose((null != detail.getUseTotalDose()) ? Double.toString(detail.getUseTotalDose()) : null);
                // 药品总数量
                Double drugNumber = null != detail.getUseTotalDose() ? detail.getUseTotalDose() : 0;
                drugTotalNumber += drugNumber;
                //备注
                orderItem.setRemark(detail.getMemo());
                //药品包装
                orderItem.setPack(detail.getPack());
                //药品单位
                orderItem.setUnit(detail.getDrugUnit());
                //放最后
                //用法名称
                orderItem.setAdmissionName(detail.getUsePathwaysTextFromHis());
                //频次名称
                orderItem.setFrequencyName(detail.getUsingRateTextFromHis());
                //药房
                if (detail.getPharmacyId() != null) {
                    PharmacyTcm pharmacyTcm = pharmacyTcmDAO.get(detail.getPharmacyId());
                    if (pharmacyTcm != null) {
                        orderItem.setPharmacyCode(pharmacyTcm.getPharmacyCode());
                        orderItem.setPharmacy(pharmacyTcm.getPharmacyName());
                    }
                }
                LOGGER.info("处方明细数据：JSONUtils.toString(orderList)={}", JSONUtils.toString(orderList));
                orderList.add(orderItem);
            }
            recipeDTO.setOrderList(orderList);
        } else {
            recipeDTO.setOrderList(null);
        }
        recipeDTO.setDrugTotalNumber(drugTotalNumber);
        recipeDTO.setDrugTotalAmount(drugTotalAmount);
    }

    /**
     * 获取杭州市互联网的医保类型
     */
    private String getMedicalType(String mpiid, Integer clinicOrgan) {
        return RecipeServiceSub.isMedicalPatient(mpiid, clinicOrgan) ? "1" : "3";
    }

    //将；用|代替
    private String getCode(String code) {
        return code.replace("；", "|");
    }

    //获取医院诊断内码
    private String getIcdRdn(Integer clinicOrgan, String organDiseaseId, String organDiseaseName) {
        IDiseaseService diseaseService = AppContextHolder.getBean("eh.diseasService", IDiseaseService.class);
        List<String> icd10Lists = Splitter.on("；").splitToList(organDiseaseId);
        List<String> nameLists = Splitter.on("；").splitToList(organDiseaseName);
        List<String> icdRdnList = Lists.newArrayList();
        if (icd10Lists.size() == nameLists.size()) {
            for (int i = 0; i < icd10Lists.size(); i++) {
                String innerCode = diseaseService.getInnerCodeByNameOrCode(clinicOrgan, nameLists.get(i), icd10Lists.get(i));
                if (StringUtils.isEmpty(innerCode)) {
                    //如果取不到就有icd10
                    innerCode = icd10Lists.get(i);
                }
                icdRdnList.add(innerCode);
            }
        }
        //若没匹配的医院诊断内码则返回空字符串
        return StringUtils.join(icdRdnList, "|");
    }


    public static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<String>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    @Override
    @RpcService
    public RecipeResultBean updateOrSaveOrganDrug(OrganDrugChangeBean organDrugChange) {
        LOGGER.info("updateOrSaveOrganDrug 更新药品信息入参{}", JSONUtils.toString(organDrugChange));
        if (organDrugChange.getOnlyOrganDrug()) {
            //his药品同步更新平台机构药品库处理
            return dealWithforOnlyOrganDrug(organDrugChange);
        } else {
            //杭州互联网his药品同步处理
            return dealWithforHZInternet(organDrugChange);
        }
    }

    @Override
    @RpcService
    public Boolean updateSuperviseRecipecodeToRecipe(Integer recipeId, String superviseRecipecode) {
        LOGGER.info("更新电子处方监管平台流水号入参：recipeId：{}，superviseRecipecode：{}", recipeId, superviseRecipecode);
        if (null == recipeId) {
            return false;
        }
        Boolean result = recipeExtendDAO.updateRecipeExInfoByRecipeId(recipeId, ImmutableMap.of("superviseRecipecode", superviseRecipecode));
        LOGGER.info("更新电子处方监管平台流水号结果：{}", result);
        return result;
    }

    @Override
    @RpcService
    public RecipeOrderBillDTO getRecipeOrderBill(Integer recipeId) {
        RecipeOrderBillDTO billDTO = new RecipeOrderBillDTO();
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            LOGGER.error("查询订单电子票据，根据处方号查询处方信息为空，recipeId={}", recipeId);
            return billDTO;
        }
        RecipeOrderBillDAO orderBillDAO = DAOFactory.getDAO(RecipeOrderBillDAO.class);
        RecipeOrderBill orderBill = orderBillDAO.getRecipeOrderBillByOrderCode(recipe.getOrderCode());
        if (null == orderBill) {
            LOGGER.error("查询订单电子票据，根据订单号查询票据信息为空，orderCode={}", recipe.getOrderCode());
            return billDTO;
        }
        billDTO.setBillBathCode(orderBill.getBillBathCode());
        billDTO.setBillNumber(orderBill.getBillNumber());
        billDTO.setBillPictureUrl(orderBill.getBillPictureUrl());
        billDTO.setBillQrCode(orderBill.getBillQrCode());
        billDTO.setRecipeOrderCode(orderBill.getRecipeOrderCode());
        LOGGER.info("查询订单电子票据,结果={}", JSONObject.toJSONString(billDTO));
        return billDTO;
    }

    private RecipeResultBean dealWithforOnlyOrganDrug(OrganDrugChangeBean organDrugChange) {
        RecipeResultBean result = RecipeResultBean.getFail();
        if (StringUtils.isEmpty(organDrugChange.getOrganDrugCode())) {
            result.setMsg("his药品唯一编码不能为空");
            return result;
        }
        if (organDrugChange.getOrganId() == null) {
            result.setMsg("机构id不能为空");
            return result;
        }
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        OrganDrugList organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCode(organDrugChange.getOrganId(), organDrugChange.getOrganDrugCode());
        if (organDrug != null) {
            Date now = DateTime.now().toDate();
            com.ngari.recipe.common.OrganDrugChangeBean organDrugChangeBean = transFormOrganDrugChangeBean(organDrugChange);
            BeanUtils.copyProperties(organDrugChangeBean, organDrug, getNullPropertyNames(organDrugChangeBean));
            organDrug.setLastModify(now);
            OrganDrugList nowOrganDrugList = organDrugListDAO.update(organDrug);
            LOGGER.info("updateOrSaveOrganDrug 更新机构药品信息成功{}", JSONUtils.toString(nowOrganDrugList));
            result = RecipeResultBean.getSuccess();
        } else {
            LOGGER.info("updateOrSaveOrganDrug 当前OrganDrugCode:{}当前机构id:{}药品不存在!", organDrugChange.getOrganDrugCode(), organDrugChange.getOrganId());
        }
        return result;
    }


    private List<String> check(OrganDrugChangeBean organDrugChange) {
        List<String> list = Lists.newArrayList();
        if (StringUtils.isEmpty(organDrugChange.getDrugId())) {
            list.add("DrugId");
        }
        if (StringUtils.isEmpty(organDrugChange.getPack())) {
            list.add("Pack");
        }
        if (StringUtils.isEmpty(organDrugChange.getUseDose())) {
            list.add("UseDose");
        }
        if (StringUtils.isEmpty(organDrugChange.getSalePrice())) {
            list.add("SalePrice");
        }
        if (StringUtils.isEmpty(organDrugChange.getBaseDrug())) {
            list.add("BaseDrug");
        }
        if (StringUtils.isEmpty(organDrugChange.getOperationCode())) {
            list.add("OperationCode");
        }
        if (StringUtils.isEmpty(organDrugChange.getMedicalDrugType())) {
            list.add("MedicalDrugType");
        }
        if (StringUtils.isEmpty(organDrugChange.getDrugType())) {
            list.add("DrugType");
        }
        if (StringUtils.isEmpty(organDrugChange.getDrugName())) {
            list.add("DrugName");
        }
        if (StringUtils.isEmpty(organDrugChange.getSaleName())) {
            list.add("SaleName");
        }
        return list;
    }

    private RecipeResultBean dealWithforHZInternet(OrganDrugChangeBean organDrugChange) {
        RecipeResultBean result = RecipeResultBean.getFail();
        List<String> check = check(organDrugChange);
        if (!ObjectUtils.isEmpty(check)) {
            LOGGER.info("updateOrSaveOrganDrug 当前新增药品信息,信息缺失{}", JSONUtils.toString(check));
            result.setMsg("当前新增药品信息,信息缺失(包括:" + check.toString() + "),无法操作!");
            return result;
        }
       /* if(StringUtils.isEmpty(organDrugChange.getDrugId()) || StringUtils.isEmpty(organDrugChange.getPack()) ||
                StringUtils.isEmpty(organDrugChange.getUseDose()) || StringUtils.isEmpty(organDrugChange.getSalePrice()) ||
                StringUtils.isEmpty(organDrugChange.getBaseDrug()) || StringUtils.isEmpty(organDrugChange.getOperationCode()) ||
                StringUtils.isEmpty(organDrugChange.getMedicalDrugType()) ||  StringUtils.isEmpty(organDrugChange.getDrugType())
                || StringUtils.isEmpty(organDrugChange.getDrugName()) || StringUtils.isEmpty(organDrugChange.getSaleName())){
            result.setMsg("当前请求参数不全，有必填字段为空");
            return result;
        }*/
        //his-api转换成recipe-bean
        com.ngari.recipe.common.OrganDrugChangeBean organDrugChangeBean = transFormOrganDrugChangeBean(organDrugChange);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        SaleDrugList saleDrugList = DAOFactory.getDAO(SaleDrugList.class);
        //根据你操作的方式，判断药品修改的方式（机构药品目录）
        //根据省监管药品代码，关联到对应的organDrugList,saleDrugList
        Integer operationCode = organDrugChangeBean.getOperationCode();
        //1新增 2修改 3停用
        OrganDrugList organDrugList = new OrganDrugList();
        BeanUtils.copyProperties(organDrugChangeBean, organDrugList);

        if (!validDrugMsg(organDrugChangeBean, result)) {
            return result;
        }
        OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(organDrugChangeBean.getOrganId(), 1);
        if (CollectionUtils.isEmpty(drugsEnterprises)) {
            result.setMsg("当前医院" + organDrugChangeBean.getOrganId() + "没有关联药企，无法操作关联的配送药品！");
            return result;
        }
        Integer drugsEnterpriseId = drugsEnterprises.get(0).getId();
        Date now = DateTime.now().toDate();

        switch (operationCode) {
            //新增
            case 1:
                //校验是否可以新增
                if (!validDrugAdd(organDrugChangeBean, result)) {
                    return result;
                }
                //新增1条organDrugList
                //新增1条saleDrugList
                List<OrganDrugList> organDrugsNo = organDrugListDAO.findByOrganIdAndDrugIdAndOrganDrugCodeAndStatus(organDrugChangeBean.getOrganId(), organDrugChangeBean.getDrugId(), organDrugChangeBean.getOrganDrugCode(), 0);
                //判断有没有失效的
                if (CollectionUtils.isNotEmpty(organDrugsNo)) {
                    //查询对应的配送药品设置为启用
                    List<SaleDrugList> saleDrugLists = saleDrugListDAO.
                            findByDrugIdAndOrganIdAndOrganDrugCodeAndStatus(drugsEnterpriseId, organDrugsNo.get(0).getDrugId(), organDrugChangeBean.getCloudPharmDrugCode(), 0);
                    if (CollectionUtils.isEmpty(saleDrugLists)) {
                        result.setMsg("当前没有停用配送药品");
                        return result;
                    }

                    //将设置为启用
                    OrganDrugList organDrugListAdd = organDrugsNo.get(0);
                    BeanUtils.copyProperties(organDrugList, organDrugListAdd, getNullPropertyNames(organDrugList));
                    organDrugListAdd.setStatus(1);
                    organDrugListAdd.setLastModify(now);
                    LOGGER.info("updateOrSaveOrganDrug 更新机构药品信息{}", JSONUtils.toString(organDrugListAdd));
                    OrganDrugList nowOrganDrugList = organDrugListDAO.update(organDrugListAdd);

                    SaleDrugList nowSaleDrugList = saleDrugLists.get(0);
                    nowSaleDrugList.setStatus(1);
                    nowSaleDrugList.setDrugId(organDrugChangeBean.getDrugId());
                    nowSaleDrugList.setOrganDrugCode(organDrugChangeBean.getCloudPharmDrugCode());
                    nowSaleDrugList.setOrganId(drugsEnterpriseId);
                    nowSaleDrugList.setPrice(organDrugChangeBean.getSalePrice());
                    nowSaleDrugList.setLastModify(now);
                    LOGGER.info("updateOrSaveOrganDrug 更新配送药品信息{}", JSONUtils.toString(nowSaleDrugList));
                    saleDrugListDAO.update(nowSaleDrugList);

                } else {
                    //没有失效的新增
                    organDrugList.setStatus(1);
                    organDrugList.setCreateDt(now);
                    LOGGER.info("updateOrSaveOrganDrug 添加机构药品信息{}", JSONUtils.toString(organDrugList));
                    OrganDrugList nowOrganDrugList = organDrugListDAO.save(organDrugList);
                    OrganDrugListService organDrugListService = AppContextHolder.getBean("organDrugListService", OrganDrugListService.class);
                    //同步药品到监管备案
                    RecipeBusiThreadPool.submit(() -> {
                        organDrugListService.uploadDrugToRegulation(organDrugList);
                        return null;
                    });
                    //填充配送药品信息
                    SaleDrugList newSaleDrugList = new SaleDrugList();
                    newSaleDrugList.setDrugId(organDrugChangeBean.getDrugId());
                    newSaleDrugList.setOrganDrugCode(organDrugChangeBean.getCloudPharmDrugCode());
                    newSaleDrugList.setOrganId(drugsEnterpriseId);
                    newSaleDrugList.setPrice(organDrugChangeBean.getSalePrice());
                    newSaleDrugList.setStatus(1);
                    newSaleDrugList.setCreateDt(now);
                    LOGGER.info("updateOrSaveOrganDrug 添加配送药品信息{}", JSONUtils.toString(newSaleDrugList));
                    saleDrugListDAO.save(newSaleDrugList);

                }

                result = RecipeResultBean.getSuccess();
                break;
            //修改
            case 2:
                //校验是否可以修改
                if (!validDrugChange(organDrugChangeBean, result)) {
                    return result;
                }
                //修改1条organDrugList
                //修改1条saleDrugList
                List<OrganDrugList> organDrugs = organDrugListDAO.findByOrganIdAndDrugIdAndOrganDrugCodeAndStatus(organDrugChangeBean.getOrganId(), organDrugChangeBean.getDrugId(), organDrugChangeBean.getOrganDrugCode(), 1);
                List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByDrugIdAndOrganIdAndOrganDrugCodeAndStatus(drugsEnterpriseId, organDrugs.get(0).getDrugId(), organDrugChangeBean.getCloudPharmDrugCode(), 1);
                if (CollectionUtils.isEmpty(saleDrugLists)) {
                    result.setMsg("当前没有启用配送药品");
                    return result;
                }
                //将设置为启用

                OrganDrugList organDrugListChange = organDrugs.get(0);
                BeanUtils.copyProperties(organDrugList, organDrugListChange, getNullPropertyNames(organDrugList));
                organDrugListChange.setStatus(1);
                organDrugListChange.setLastModify(now);
                LOGGER.info("updateOrSaveOrganDrug 更新机构药品信息{}", JSONUtils.toString(organDrugListChange));
                OrganDrugList nowOrganDrugList = organDrugListDAO.update(organDrugListChange);

                SaleDrugList nowSaleDrugList = saleDrugLists.get(0);
                nowSaleDrugList.setStatus(1);
                nowSaleDrugList.setDrugId(organDrugChangeBean.getDrugId());
                nowSaleDrugList.setOrganDrugCode(organDrugChangeBean.getCloudPharmDrugCode());
                nowSaleDrugList.setOrganId(drugsEnterpriseId);
                nowSaleDrugList.setPrice(organDrugChangeBean.getSalePrice());
                nowSaleDrugList.setLastModify(now);
                LOGGER.info("updateOrSaveOrganDrug 更新配送药品信息{}", JSONUtils.toString(nowSaleDrugList));
                saleDrugListDAO.update(nowSaleDrugList);

                result = RecipeResultBean.getSuccess();
                break;
            //停用
            case 3:
                //校验是否可以停用
                if (!validDrugDown(organDrugChangeBean, result)) {
                    return result;
                }
                //停用1条organDrugList
                //停用1条saleDrugList
                List<OrganDrugList> organDrugsDown = organDrugListDAO.findByOrganIdAndDrugIdAndOrganDrugCodeAndStatus(organDrugChangeBean.getOrganId(), organDrugChangeBean.getDrugId(), organDrugChangeBean.getOrganDrugCode(), 1);
                List<SaleDrugList> saleDrugListsDown = saleDrugListDAO.findByDrugIdAndOrganIdAndOrganDrugCodeAndStatus(drugsEnterpriseId, organDrugsDown.get(0).getDrugId(), organDrugChangeBean.getCloudPharmDrugCode(), 1);
                if (CollectionUtils.isEmpty(saleDrugListsDown)) {
                    result.setMsg("当前没有启用配送药品");
                    return result;
                }

                OrganDrugList organDrugListDown = organDrugsDown.get(0);
                organDrugListDown.setStatus(0);
                organDrugListDown.setLastModify(now);
                LOGGER.info("updateOrSaveOrganDrug 停用机构药品信息{}", JSONUtils.toString(organDrugListDown));
                organDrugListDAO.update(organDrugListDown);

                SaleDrugList saleDrugListDown = saleDrugListsDown.get(0);
                saleDrugListDown.setLastModify(now);
                saleDrugListDown.setStatus(0);
                LOGGER.info("updateOrSaveOrganDrug 停用配送药品信息{}", JSONUtils.toString(saleDrugListDown));
                saleDrugListDAO.update(saleDrugListDown);

                result = RecipeResultBean.getSuccess();

                break;
            default:
        }
        return result;
    }

    private com.ngari.recipe.common.OrganDrugChangeBean transFormOrganDrugChangeBean(OrganDrugChangeBean organDrugChangeBean) {
        com.ngari.recipe.common.OrganDrugChangeBean request = new com.ngari.recipe.common.OrganDrugChangeBean();
        try {

            request.setBaseDrug(StringUtils.isEmpty(organDrugChangeBean.getBaseDrug()) ? null : Integer.parseInt(organDrugChangeBean.getBaseDrug()));
            request.setCloudPharmDrugCode(organDrugChangeBean.getCloudPharmDrugCode());
            request.setDrugId(StringUtils.isEmpty(organDrugChangeBean.getDrugId()) ? null : Integer.parseInt(organDrugChangeBean.getDrugId()));
            request.setDrugName(organDrugChangeBean.getDrugName());
            request.setDrugSpec(organDrugChangeBean.getDrugSpec());
            request.setDrugSpec(organDrugChangeBean.getDrugSpec());
            request.setDrugType(StringUtils.isEmpty(organDrugChangeBean.getDrugType()) ? null : Integer.parseInt(organDrugChangeBean.getDrugType()));
            request.setLicenseNumber(organDrugChangeBean.getLicenseNumber());
            request.setMedicalDrugCode(organDrugChangeBean.getMedicalDrugCode());
            request.setMedicalDrugType(StringUtils.isEmpty(organDrugChangeBean.getMedicalDrugType()) ? null : Integer.parseInt(organDrugChangeBean.getMedicalDrugType()));
            request.setOperationCode(StringUtils.isEmpty(organDrugChangeBean.getOperationCode()) ? null : Integer.parseInt(organDrugChangeBean.getOperationCode()));
            request.setOrganDrugCode(organDrugChangeBean.getOrganDrugCode());
            request.setOrganId(organDrugChangeBean.getOrganId());
            request.setOrganName(organDrugChangeBean.getOrganName());
            request.setPack(StringUtils.isEmpty(organDrugChangeBean.getPack()) ? null : Integer.parseInt(organDrugChangeBean.getPack()));
            request.setProducer(organDrugChangeBean.getProducer());
            request.setProducerCode(organDrugChangeBean.getProducerCode());

            String regEx = "(\\d+\\.?\\d+)";
            Pattern p = Pattern.compile(regEx);
            Matcher m;
            //截取数字部分
            if (StringUtils.isNotEmpty(organDrugChangeBean.getRecommendedUseDose())) {
                StringBuilder builder = new StringBuilder();
                m = p.matcher(organDrugChangeBean.getRecommendedUseDose());
                if (m.find()) {//当符合正则表达式定义的条件时
                    builder.append(m.group());
                    request.setRecommendedUseDose(Double.parseDouble(builder.toString()));
                }
            }

            request.setSaleName(organDrugChangeBean.getSaleName());
            request.setSalePrice(StringUtils.isEmpty(organDrugChangeBean.getSalePrice()) ? null : new BigDecimal(organDrugChangeBean.getSalePrice()));
            request.setUnit(organDrugChangeBean.getUnit());
            //截取数字部分
            StringBuilder builder = new StringBuilder();
            if (StringUtils.isNotEmpty(organDrugChangeBean.getUseDose())) {
                m = p.matcher(organDrugChangeBean.getUseDose());
                if (m.find()) {//当符合正则表达式定义的条件时
                    builder.append(m.group());
                    request.setUseDose(Double.parseDouble(builder.toString()));
                }
            }
            request.setUseDoseUnit(organDrugChangeBean.getUseDoseUnit());
            request.setUsePathways(organDrugChangeBean.getUsePathways());
            request.setUsingRate(organDrugChangeBean.getUsingRate());
        } catch (Exception e) {
            //抛出异常信息，返回空数组
            LOGGER.error("updateOrSaveOrganDrug 当前更新操作异常：", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "当前药品修改请求数据异常！");
        }
        return request;
    }

    private boolean validDrugMsg(com.ngari.recipe.common.OrganDrugChangeBean organDrugChangeBean, RecipeResultBean result) {
        if (null == organDrugChangeBean.getDrugId() || null == organDrugChangeBean.getOrganId() || null == organDrugChangeBean.getOrganDrugCode() || null == organDrugChangeBean.getCloudPharmDrugCode()) {
            result.setMsg("当前新增药品信息，信息缺失,无法操作！");
            return false;
        }

        return true;
    }


    private boolean validDrugAdd(com.ngari.recipe.common.OrganDrugChangeBean organDrugChangeBean, RecipeResultBean result) {

        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        //判断当前机构药品是否已经存在
        List<OrganDrugList> organDrugs = organDrugListDAO.findByOrganIdAndDrugIdAndOrganDrugCodeAndStatus(organDrugChangeBean.getOrganId(), organDrugChangeBean.getDrugId(), organDrugChangeBean.getOrganDrugCode(), 1);
        //如果已经有了启用的
        if (CollectionUtils.isNotEmpty(organDrugs)) {
            result.setMsg("当前药品信息系统已存在，无法重复新增！");
            return false;
        }

        return true;
    }

    private boolean validDrugChange(com.ngari.recipe.common.OrganDrugChangeBean organDrugChangeBean, RecipeResultBean result) {

        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        //判断当前机构药品是否已经存在
        List<OrganDrugList> organDrugs = organDrugListDAO.findByOrganIdAndDrugIdAndOrganDrugCodeAndStatus(organDrugChangeBean.getOrganId(), organDrugChangeBean.getDrugId(), organDrugChangeBean.getOrganDrugCode(), 1);
        //如果没有启用的
        if (CollectionUtils.isEmpty(organDrugs)) {
            result.setMsg("当前药品信息系统没有启用的,无法修改药品信息！");
            return false;
        }

        return true;
    }

    private boolean validDrugDown(com.ngari.recipe.common.OrganDrugChangeBean organDrugChangeBean, RecipeResultBean result) {

        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        //判断当前机构药品是否已经存在
        List<OrganDrugList> organDrugs = organDrugListDAO.findByOrganIdAndDrugIdAndOrganDrugCodeAndStatus(organDrugChangeBean.getOrganId(), organDrugChangeBean.getDrugId(), organDrugChangeBean.getOrganDrugCode(), 1);
        //如果没有启用的
        if (CollectionUtils.isEmpty(organDrugs)) {
            result.setMsg("当前药品信息系统没有启用的,无法停用药品信息！");
            return false;
        }
        return true;
    }

    //杭州市互联网医院查询基础药品目录
    @Override
    @RpcService
    public List<DrugListBean> getDrugList(String organId, String organName, Integer start, Integer limit) {
        LOGGER.info("当前请求参数：{},{},{},{}", organId, organName, start, limit);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> drugList = drugListDAO.findAllForPage(start, limit);
        if (CollectionUtils.isEmpty(drugList)) {
            return new ArrayList<DrugListBean>();
        }
        LOGGER.info("当前返回结果", JSONUtils.toString(drugList));
        return ObjectCopyUtils.convert(drugList, DrugListBean.class);

    }

    /**
     * 处方数据上传医院数据中心 处方数据，处方明细数据
     *
     * @param organId
     * @param startDate
     * @param endDate
     * @return QueryRecipeInfoDTO
     */
    @Override
    @RpcService
    public List<QueryRecipeInfoDTO> queryRecipeDataForHisDataCenter(Integer organId, Date startDate, Date endDate) {

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

        String start = DateConversion.formatDateTimeWithSec(startDate);
        String end = DateConversion.formatDateTimeWithSec(endDate);
        LOGGER.info("处方数据上传医院数据中心入参:organId,startDate,endDate={},{},{}", organId, startDate, endDate);

        int daysBetween = DateConversion.getDaysBetween(startDate, endDate);
        if (daysBetween > 30) {
            throw new DAOException("当前最多仅支持查询最近一个月内数据。");
        }

        //通过机构Id和时间查询处方信息
        List<Recipe> recipeList = recipeDAO.findRecipeListByOrganIdAndTime(organId, start, end);
        List<QueryRecipeInfoDTO> list = new ArrayList<>(recipeList.size());

        if (CollectionUtils.isNotEmpty(recipeList)) {
            LOGGER.info("当前查询返回结果，recipeList.size()={}", recipeList.size());
            for (Recipe r : recipeList) {
                List<Recipedetail> details = recipeDetailDAO.findByRecipeId(r.getRecipeId());
                list.add(splicingBackData(details, r));
            }
            LOGGER.info("当前查询返回结果，list.size()={}", list.size());
        }
        return list;
    }

    /**
     * 拼接医院数据中心需要处方业务
     *
     * @param details
     * @param recipe
     * @return
     */
    @RpcService
    private QueryRecipeInfoDTO splicingBackData(List<Recipedetail> details, Recipe recipe) {
        QueryRecipeInfoDTO recipeDTO = new QueryRecipeInfoDTO();
        try {
            //处方号 his返回
            recipeDTO.setRecipeID(recipe.getRecipeCode());
            //机构id
            recipeDTO.setOrganId(String.valueOf(recipe.getClinicOrgan()));
            //处方号  处方唯一标识 收费码
            recipeDTO.setPlatRecipeID(String.valueOf(recipe.getRecipeId()));
            //患者编号  门诊患者标识
            recipeDTO.setPatientID(recipe.getPatientID());
            //处方备注 医嘱正文
            recipeDTO.setRecipeMemo(recipe.getRecipeMemo());
            //开医嘱时间  开方时间
            recipeDTO.setCreateDate(recipe.getCreateDate());
            //诊断备注  医嘱备注
            recipeDTO.setMemo(recipe.getMemo());
            //处方状态
            recipeDTO.setStatus(recipe.getStatus());
            //支付状态  是否支付
            recipeDTO.setIsPay((null != recipe.getPayFlag()) ? Integer.toString(recipe.getPayFlag()) : null);
            //返回开方部门code
            DepartmentService service = BasicAPI.getService(DepartmentService.class);
            DepartmentDTO departmentDTO = service.getById(recipe.getDepart());
            if (departmentDTO != null) {
                recipeDTO.setDeptID(departmentDTO.getCode());
                //科室名
                recipeDTO.setDeptName(departmentDTO.getName());
                AppointDepartService appointDepartService = ApplicationUtils.getBasicService(AppointDepartService.class);
                AppointDepartDTO appointDepart = appointDepartService.findByOrganIDAndDepartID(recipe.getClinicOrgan(), recipe.getDepart());
                //开单挂号科室代号
                recipeDTO.setDeptCode((null != appointDepart) ? appointDepart.getAppointDepartCode() : "");
            }
            //处方类型
            recipeDTO.setRecipeType((null != recipe.getRecipeType()) ? recipe.getRecipeType().toString() : null);
            //复诊id
            recipeDTO.setClinicID((null != recipe.getClinicId()) ? Integer.toString(recipe.getClinicId()) : null);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if (recipeExtend != null) {
                recipeDTO.setRegisterId(recipeExtend.getRegisterID());
            }
            //转换平台医生id为工号返回his
            EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
            if (recipe.getDoctor() != null) {
                String jobNumber = iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart());
                //医生工号
                recipeDTO.setDoctorID(jobNumber);
                //医生姓名
                recipeDTO.setDoctorName(recipe.getDoctorName());
                //医生身份证
                DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
                DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getDoctor());
                if (doctorDTO != null) {
                    recipeDTO.setDoctorIDCard(doctorDTO.getIdNumber());
                }
            }
            //审核医生
            if (recipe.getChecker() != null) {
                String jobNumberChecker = iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getChecker(), recipe.getClinicOrgan(), recipe.getDepart());
                recipeDTO.setAuditDoctor(jobNumberChecker);
                //审核状态
                recipeDTO.setAuditCheckStatus("1");
                //审方时间
                recipeDTO.setCheckDate(recipe.getCheckDate());
            } else {
                recipeDTO.setAuditDoctor(recipeDTO.getDoctorID());
                recipeDTO.setAuditCheckStatus("0");
                //审方时间
                recipeDTO.setCheckDate(new Date());
            }
            recipeDTO.setMedicalPayFlag(getMedicalType(recipe.getMpiid(), recipe.getClinicOrgan()));
            //处方总金额
            recipeDTO.setRecipeFee(String.valueOf(recipe.getActualPrice()));
            /*//获取医院诊断内码
            recipeDTO.setIcdRdn(getIcdRdn(recipe.getClinicOrgan(), recipe.getOrganDiseaseId(), recipe.getOrganDiseaseName()));*/
            //icd诊断码
            recipeDTO.setIcdCode(getCode(recipe.getOrganDiseaseId()));
            //icd诊断名称
            recipeDTO.setIcdName(getCode(recipe.getOrganDiseaseName()));
            splicingBackDataForRecipeDetails(recipe.getClinicOrgan(), details, recipeDTO);
            LOGGER.info("数据中心获取处方业务信息 recipeDTO={}", JSONUtils.toString(recipeDTO));
        } catch (Exception e) {
            LOGGER.error("数据中心获取处方业务信息 recipeDTO error", e);
        }
        return recipeDTO;
    }

    /**
     * 通过区域公众号查询当前支持线下处方查询的机构
     * @return
     */
    @RpcService
    public List<Integer> getOrganForWeb(){
        ICurrentUserInfoService currentUserInfoService = AppDomainContext.getBean("eh.remoteCurrentUserInfoService", ICurrentUserInfoService.class);
        //查询当前区域公众号下所有归属机构
        List<Integer> organIds = currentUserInfoService.getCurrentOrganIds();
        List<Integer> oganList = new ArrayList<>();
        //获取运营平台配置--是否开启查询线下处方
        IConfigurationCenterUtilsService utilsService = AppDomainContext.getBean("eh.configurationCenterUtils", IConfigurationCenterUtilsService.class);
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(organIds)){
            //取反操作，获取所以未打开配置的机构  []--所以机构都打开
            List<Integer> organIdList= utilsService.findOrganByPropertyKeyAndValue("queryGetToHisRecipe","false");
            log.info("queryOrganService.getOrganByConfig.oganListBefore={}",JSONUtils.toString(organIdList));
            if (CollectionUtils.isNotEmpty(organIdList)){
                organIds.removeAll(organIdList);
                log.info("queryOrganService.getOrganByConfig.organIds.size={}",JSONUtils.toString(organIds.size()));
                return organIds;
            }
            log.info("queryOrganService.getOrganByConfig.organIds={}",JSONUtils.toString(organIds));
            if (organIds.contains(-1)){
                organIds.remove(new Integer(-1));
            }
            return organIds;
        }

        //查询全国机构 organService
        if (organIds==null){
            OrganService organService = BasicAPI.getService(OrganService.class);
            List<OrganDTO> organs = organService.findOrgans();
            log.info("queryOrganService.organs={}",JSONUtils.toString(organs));
            if (CollectionUtils.isNotEmpty(organs)){
                for (OrganDTO o:organs){
                    oganList.add(o.getOrganId());
                }
                log.info("queryOrganService.oganList={}",JSONUtils.toString(oganList));
                return oganList;
            }
        }
        return oganList;
    }
}
