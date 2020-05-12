package recipe.hisservice.syncdata;

import com.ngari.base.serviceconfig.mode.ServiceConfigResponseTO;
import com.ngari.base.serviceconfig.service.IHisServiceConfigService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.ConsultBean;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.model.QuestionnaireBean;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.consult.common.service.IConsultService;
import com.ngari.his.regulation.entity.*;
import com.ngari.his.regulation.service.IRegulationService;
import com.ngari.patient.dto.*;
import com.ngari.patient.dto.zjs.SubCodeDTO;
import com.ngari.patient.service.*;
import com.ngari.patient.service.zjs.SubCodeService;
import com.ngari.recipe.entity.*;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bussutil.RecipeUtil;
import recipe.common.CommonConstant;
import recipe.common.ResponseUtils;
import recipe.common.response.CommonResponse;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;
import recipe.util.RedisClient;

import java.util.*;
import java.util.stream.Collectors;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * created by shiyuping on 2019/6/3
 * 平台监管平台同步
 */
@RpcBean("hisSyncSupervisionService")
public class HisSyncSupervisionService implements ICommonSyncSupervisionService {
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HisSyncSupervisionService.class);

    private static String HIS_SUCCESS = "200";

    /**
     * 同步处方数据
     *
     * @param recipeList
     * @return
     */

    @RpcService
    @Override
    public CommonResponse uploadRecipeIndicators(List<Recipe> recipeList) {
        LOGGER.info("uploadRecipeIndicators recipeList length={} recipeId={}", recipeList.size(),recipeList.get(0).getRecipeId());
        CommonResponse commonResponse = ResponseUtils.getFailResponse(CommonResponse.class, "");
        if (CollectionUtils.isEmpty(recipeList)) {
            commonResponse.setMsg("处方列表为空");
            return commonResponse;
        }
        List<RegulationRecipeIndicatorsReq> request = new ArrayList<>(recipeList.size());
        splicingBackRecipeData(recipeList, request);

        try {
            IRegulationService hisService = AppDomainContext.getBean("his.regulationService", IRegulationService.class);
            LOGGER.info("uploadRecipeIndicators request={}", JSONUtils.toString(request));
            HisResponseTO response = hisService.uploadRecipeIndicators(recipeList.get(0).getClinicOrgan(), request);
            LOGGER.info("uploadRecipeIndicators response={}", JSONUtils.toString(response));
            if (HIS_SUCCESS.equals(response.getMsgCode())) {
                //成功
                commonResponse.setCode(CommonConstant.SUCCESS);
                LOGGER.info("uploadRecipeIndicators execute success.");
            } else {
                commonResponse.setMsg(response.getMsg());
            }
        } catch (Exception e) {
            LOGGER.warn("uploadRecipeIndicators HIS接口调用失败. request={}", JSONUtils.toString(request), e);
            commonResponse.setMsg("HIS接口调用异常");
        }

        LOGGER.info("uploadRecipeIndicators commonResponse={}", JSONUtils.toString(commonResponse));
        return commonResponse;
    }

    /**
     * 拼接监管平台所需处方数据
     *
     * @param recipeList
     * @param request
     */
    public void splicingBackRecipeData(List<Recipe> recipeList,List<RegulationRecipeIndicatorsReq> request) {

        AuditMedicinesDAO auditMedicinesDAO = DAOFactory.getDAO(AuditMedicinesDAO.class);
        DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
        EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        PatientService patientService = BasicAPI.getService(PatientService.class);
        SubCodeService subCodeService = BasicAPI.getService(SubCodeService.class);
        OrganService organService = BasicAPI.getService(OrganService.class);
        IConsultService iConsultService = ApplicationUtils.getConsultService(IConsultService.class);
        IConsultExService iConsultExService = ApplicationUtils.getConsultService(IConsultExService.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        DoctorExtendService doctorExtendService = BasicAPI.getService(DoctorExtendService.class);

        Map<Integer, OrganDTO> organMap = new HashMap<>(20);
        Map<Integer, DepartmentDTO> departMap = new HashMap<>(20);
        Map<Integer, DoctorDTO> doctorMap = new HashMap<>(20);

        Dictionary usingRateDic = null;
        Dictionary usePathwaysDic = null;
        try {
            usingRateDic = DictionaryController.instance().get("eh.cdr.dictionary.UsingRate");
            usePathwaysDic = DictionaryController.instance().get("eh.cdr.dictionary.UsePathways");
        } catch (ControllerException e) {
            LOGGER.warn("uploadRecipeIndicators dic error.");
        }

        //业务数据处理
        Date now = DateTime.now().toDate();
        RegulationRecipeIndicatorsReq req;
        OrganDTO organDTO;
        String organDiseaseName;
        DepartmentDTO departmentDTO;
        DoctorDTO doctorDTO;
        PatientDTO patientDTO;
        SubCodeDTO subCodeDTO;
        List<Recipedetail> detailList;
        List<AuditMedicines> medicineList;
        AuditMedicines medicine;
        RecipeExtend recipeExtend;
        RecipeOrder recipeOrder;
        DoctorExtendDTO doctorExtendDTO;
        ConsultExDTO consultExDTO;
        RedisClient redisClient = RedisClient.instance();
        String caSignature = null;
        for (Recipe recipe : recipeList) {
            req = new RegulationRecipeIndicatorsReq();
            //机构处理
            organDTO = organMap.get(recipe.getClinicOrgan());
            if (null == organDTO) {
                organDTO = organService.get(recipe.getClinicOrgan());
                organMap.put(recipe.getClinicOrgan(), organDTO);
            }
            if (null == organDTO) {
                LOGGER.warn("uploadRecipeIndicators organ is null. recipe.clinicOrgan={}", recipe.getClinicOrgan());
                continue;
            }
            req.setOrganID(LocalStringUtil.toString(organDTO.getOrganId()));
            //组织机构编码
            req.setOrganizeCode(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));
            req.setOrganName(organDTO.getName());

            //科室处理
            departmentDTO = departMap.get(recipe.getDepart());
            if (null == departmentDTO) {
                departmentDTO = departmentService.getById(recipe.getDepart());
                departMap.put(recipe.getDepart(), departmentDTO);
            }
            if (null == departmentDTO) {
                LOGGER.warn("uploadRecipeIndicators depart is null. recipe.depart={}", recipe.getDepart());
                continue;
            }
            req.setDeptID(departmentDTO.getCode());
            req.setDeptName(departmentDTO.getName());
            //设置专科编码等
            subCodeDTO = subCodeService.getByNgariProfessionCode(departmentDTO.getProfessionCode());
            if (null == subCodeDTO) {
                //专科编码没设置不应该导致推送不了处方到监管平台
                LOGGER.warn("uploadRecipeIndicators subCode is null. recipe.professionCode={}", departmentDTO.getProfessionCode());
            }else {
                req.setSubjectCode(subCodeDTO.getSubCode());
                req.setSubjectName(subCodeDTO.getSubName());
            }


            //医生处理
            req.setDoctorId(recipe.getDoctor().toString());
            doctorDTO = doctorMap.get(recipe.getDoctor());
            if (null == doctorDTO) {
                doctorDTO = doctorService.get(recipe.getDoctor());
                doctorMap.put(recipe.getDoctor(), doctorDTO);
            }
            if (null == doctorDTO) {
                LOGGER.warn("uploadRecipeIndicators doctor is null. recipe.doctor={}", recipe.getDoctor());
                continue;
            }
            if (Integer.valueOf(1).equals(doctorDTO.getTestPersonnel())) {
                LOGGER.warn("uploadRecipeIndicators doctor is testPersonnel. recipe.doctor={}", recipe.getDoctor());
                continue;
            }

            req.setDoctorCertID(doctorDTO.getIdNumber());
            req.setDoctorName(doctorDTO.getName());
            //医生签名---互联网医院用到
            req.setCAInfo(doctorDTO.getName());
            //设置医生工号
            req.setDoctorNo(iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart()));
            req.setDoctorProTitle(doctorDTO.getProTitle());
            //设置医生电子签名
            if (doctorDTO.getESignId() != null) {
                try {
                    caSignature = redisClient.get(doctorDTO.getESignId() + "_signature");
                } catch (Exception e) {
                    LOGGER.error("get caSignature error. doctorId={}", doctorDTO.getDoctorId(), e);
                }
                req.setDoctorSign(StringUtils.isNotEmpty(caSignature) ? caSignature : "");
            }
            //药师处理
            if (recipe.getChecker() != null) {
                doctorDTO = doctorMap.get(recipe.getChecker());
                if (null == doctorDTO) {
                    doctorDTO = doctorService.get(recipe.getChecker());
                    doctorMap.put(recipe.getChecker(), doctorDTO);
                }
                if (null == doctorDTO) {
                    LOGGER.warn("uploadRecipeIndicators checker is null. recipe.checker={}", recipe.getChecker());
                    continue;
                }
                req.setAuditDoctorCertID(doctorDTO.getIdNumber());
                req.setAuditDoctor(doctorDTO.getName());
                req.setAuditDoctorId(recipe.getChecker().toString());
                req.setAuditProTitle(doctorDTO.getProTitle());
            }
            //设置药师电子签名
            if (doctorDTO.getESignId() != null) {
                try {
                    caSignature = redisClient.get(doctorDTO.getESignId() + "_signature");
                } catch (Exception e) {
                    LOGGER.error("get caSignature error. doctorId={}", doctorDTO.getDoctorId(), e);
                }
                req.setAuditDoctorSign(StringUtils.isNotEmpty(caSignature) ? caSignature : "");
            }

            //患者处理
            patientDTO = patientService.get(recipe.getMpiid());
            if (null == patientDTO) {
                LOGGER.warn("uploadRecipeIndicators patient is null. recipe.patient={}", recipe.getMpiid());
                continue;
            }
            req.setMpiId(patientDTO.getMpiId());
            organDiseaseName = recipe.getOrganDiseaseName().replaceAll("；", "|");
            req.setOriginalDiagnosis(organDiseaseName);
            req.setPatientCardType(LocalStringUtil.toString(patientDTO.getCertificateType()));
            req.setPatientCertID(LocalStringUtil.toString(patientDTO.getCertificate()));
            req.setPatientName(patientDTO.getPatientName());
            req.setNation(patientDTO.getNation());
            req.setMobile(LocalStringUtil.toString(patientDTO.getMobile()));
            req.setSex(patientDTO.getPatientSex());
            req.setAge(DateConversion.calculateAge(patientDTO.getBirthday()));
            req.setBirthDay(patientDTO.getBirthday());
            //陪诊人信息
            req.setGuardianName(patientDTO.getGuardianName());
            req.setGuardianCertID(patientDTO.getGuardianCertificate());
            //其他信息
            //监管接收方现在使用recipeId去重
            req.setRecipeID(recipe.getRecipeId().toString());
            //处方唯一编号
            req.setRecipeUniqueID(recipe.getRecipeCode());
            //审方时间
            req.setCheckDate(recipe.getCheckDateYs());
            //互联网医院处方都是经过合理用药审查
            req.setRationalFlag("1");
            medicineList = auditMedicinesDAO.findMedicinesByRecipeId(recipe.getRecipeId());
            if (CollectionUtils.isEmpty(medicineList)) {
                req.setRationalFlag("0");
            } else if (1 == medicineList.size()) {
                medicine = medicineList.get(0);
                //问题药品编码为空，可能是没问题，可能是审核出错
                if (StringUtils.isEmpty(medicine.getCode())) {
                    //TODO 此处由于无法判断审核是否完成，只能通过该关键字判断
                    if (-1 != "无预审结果".indexOf(medicine.getRemark())) {
                        req.setRationalFlag("0");
                    } else {
                        //其他情况为 系统预审未发现处方问题
                        req.setRationalDrug("系统预审未发现处方问题");
                    }
                } else {
                    req.setRationalDrug(setRationalDrug(recipe.getRecipeId()));
                }
            } else {
                req.setRationalDrug(setRationalDrug(recipe.getRecipeId()));
            }

            req.setIcdCode(recipe.getOrganDiseaseId().replaceAll("；", "|"));
            req.setIcdName(organDiseaseName);
            //诊断备注
            req.setMemo(recipe.getMemo());
            req.setRecipeType(recipe.getRecipeType().toString());
            req.setPacketsNum(recipe.getCopyNum());
            req.setDatein(recipe.getSignDate());
            req.setEffectivePeriod(recipe.getValueDays());
            req.setStartDate(recipe.getSignDate());
            req.setEndDate(DateConversion.getDateAftXDays(recipe.getSignDate(), recipe.getValueDays()));
            req.setUpdateTime(now);
            req.setTotalFee(recipe.getTotalMoney().doubleValue());
            req.setIsPay(recipe.getPayFlag().toString());

            //主诉
           /* consultIds = iConsultService.findApplyingConsultByRequestMpiAndDoctorId(recipe.getRequestMpiId(),
                    recipe.getDoctor(), RecipeSystemConstant.CONSULT_TYPE_RECIPE);
            if (CollectionUtils.isNotEmpty(consultIds)) {
                consultId = consultIds.get(0);
            }*/
            if (recipe.getClinicId() != null) {
                req.setBussID(recipe.getClinicId().toString());
                //处方来源 1-问诊 4复诊
                if (!RecipeBussConstant.BUSS_SOURCE_NONE.equals(recipe.getBussSource())) {
                    if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource())) {
                        req.setBussSource("4");
                    } else {
                        req.setBussSource("1");
                    }
                }
                ConsultBean consultBean = iConsultService.getById(recipe.getClinicId());
                QuestionnaireBean questionnaire = iConsultService.getConsultQuestionnaireByConsultId(recipe.getClinicId());
                if (consultBean != null) {
                    req.setMainDieaseDescribe(consultBean.getLeaveMess());
                    //咨询开始时间
                    req.setConsultStartDate(consultBean.getStartDate());
                }
                if (questionnaire != null) {
                    //过敏史标记 有无过敏史 0:无 1:有
                    req.setAllergyFlag(questionnaire.getAlleric().toString());
                    //过敏史详情
                    req.setAllergyInfo(questionnaire.getAllericMemo());
                    //现病史
                    req.setCurrentMedical(questionnaire.getDisease());
                    //既往史
                    req.setHistroyMedical(questionnaire.getDisease());
                }
            }
            //门诊号处理
            recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if (recipeExtend != null) {
                req.setPatientNumber(recipeExtend.getRegisterID());
                req.setCardNo(recipeExtend.getCardNo());
                req.setCardType(recipeExtend.getCardType());
            }
            //处方状态
            req.setRecipeStatus(recipe.getStatus());

            //撤销标记
            req.setCancelFlag(getVerificationRevokeStatus(recipe));
            //核销标记
            req.setVerificationStatus(getVerificationStatus(recipe));
            req.setPayFlag(null == recipe.getPayFlag() ? "" : String.valueOf(recipe.getPayFlag()));
            doctorExtendDTO = doctorExtendService.getByDoctorId(recipe.getDoctor());
            if(null != doctorExtendDTO){
                req.setSerialNumCA(doctorExtendDTO.getSerialNumCA()); //医护人员证件序列号
            }
            //医生处方签名生成时间戳
            req.setSignCADate(recipe.getSignCADate()); //医生处方数字签名值
            req.setSignRecipeCode(recipe.getSignRecipeCode());
            recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            if(null != recipeOrder){
                //配送方式
                req.setDeliveryType(null == recipe.getGiveMode() ? "" : recipe.getGiveMode().toString());
                //配送开始时间
                req.setSendTime(recipeOrder.getSendTime());
                //配送结束时间
                req.setFinishTime(recipeOrder.getFinishTime());
                //配送状态
                req.setDeliveryStatus(recipeOrder.getStatus());
            }

            //卡号，卡类型
            if(req.getCardNo()==null){
                //取复诊单里的卡号卡信息
                if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource())) {
                    consultExDTO = iConsultExService.getByConsultId(recipe.getClinicId());
                    if(null != consultExDTO){
                        req.setCardNo(consultExDTO.getCardId());
                        req.setCardType(consultExDTO.getCardType());
                    }
                }
            }

            //详情处理
            detailList = detailDAO.findByRecipeId(recipe.getRecipeId());
            if (CollectionUtils.isEmpty(detailList)) {
                LOGGER.warn("uploadRecipeIndicators detail is null. recipe.id={}", recipe.getRecipeId());
                continue;
            }
            setDetail(req, detailList, usingRateDic, usePathwaysDic, recipe);

            request.add(req);
        }
    }

    /**
     * 核销处方同步方法
     *
     * @param recipeList
     * @return
     */
    @Override
    @RpcService
    public CommonResponse uploadRecipeVerificationIndicators(List<Recipe> recipeList) {
        LOGGER.info("uploadRecipeVerificationIndicators recipeList length={}", Integer.valueOf(recipeList.size()));
        CommonResponse commonResponse = ResponseUtils.getFailResponse(CommonResponse.class, "");
        if (CollectionUtils.isEmpty(recipeList)) {
            commonResponse.setMsg("处方列表为空");
            return commonResponse;
        }
        /*IHisServiceConfigService configService = AppDomainContext.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
        List<ServiceConfigResponseTO> list = configService.findAllRegulationOrgan();
        if (CollectionUtils.isEmpty(list)) {
            LOGGER.warn("uploadRecipeIndicators provUploadOrgan list is null.");
            commonResponse.setMsg("需要同步机构列表为空");
            return commonResponse;
        }*/
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        OrganService organService = BasicAPI.getService(OrganService.class);

        Map organMap = new HashMap(20);
        List<RegulationRecipeVerificationIndicatorsReq> request = new ArrayList<>(recipeList.size());
        Date now = DateTime.now().toDate();

        RecipeOrder order = null;

        for (Recipe recipe : recipeList) {
            RegulationRecipeVerificationIndicatorsReq req = new RegulationRecipeVerificationIndicatorsReq();
            //12.23由原来的recipeId修改成clinicId
            req.setBussID(LocalStringUtil.toString(recipe.getClinicId()));
            req.setRecipeID(recipe.getRecipeId().toString());
            req.setRecipeUniqueID(recipe.getRecipeCode());

            if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
                order = orderDAO.getByOrderCode(recipe.getOrderCode());
            }

            if (null == order) {
                LOGGER.warn("uploadRecipeVerificationIndicators order is null. recipe.orderCode={}", recipe.getOrderCode());
                continue;
            }

            //机构处理
            OrganDTO organDTO = (OrganDTO) organMap.get(recipe.getClinicOrgan());
            if (null == organDTO) {
                organDTO = organService.get(recipe.getClinicOrgan());
                organMap.put(recipe.getClinicOrgan(), organDTO);
            }
            if (null == organDTO) {
                LOGGER.warn("uploadRecipeVerificationIndicators organ is null. recipe.clinicOrgan={}", recipe.getClinicOrgan());
                continue;
            }
            req.setOrganID(LocalStringUtil.toString(organDTO.getOrganId()));
            req.setOrganName(organDTO.getName());

            if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode())) {
                req.setDeliveryType("1");
                req.setDeliverySTDate(recipe.getStartSendDate());
                req.setDeliveryFee(order.getExpressFee().toPlainString());
                DrugsEnterprise enterprise = enterpriseDAO.get(order.getEnterpriseId());
                if (null != enterprise) {
                    req.setDeliveryFirm(enterprise.getName());
                }
            } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(recipe.getGiveMode())) {
                req.setDeliveryType("0");
                req.setDeliveryFirm("医院药房");
            } else if (RecipeBussConstant.GIVEMODE_TFDS.equals(recipe.getGiveMode())) {
                req.setDeliveryType("3");
                req.setDeliveryFirm(order.getDrugStoreName());
            }

            req.setVerificationTime(now);
            req.setTotalFee(Double.valueOf(recipe.getTotalMoney().doubleValue()));
            if (1 == order.getPayFlag().intValue()) {
                req.setIsPay("1");
            } else {
                req.setIsPay("0");
            }
            req.setUpdateTime(now);

            //从his返回的挂号序号
            RecipeExtendDAO recipeExtendDAO = getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend=recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if(recipeExtend!=null){
                req.setRegisterNo(recipeExtend.getRegisterNo());
            }

            request.add(req);
        }
        try {
            IRegulationService hisService = AppDomainContext.getBean("his.regulationService", IRegulationService.class);
            LOGGER.info("uploadRecipeVerificationIndicators request={}", JSONUtils.toString(request));
            HisResponseTO response = hisService.uploadRecipeVerificationIndicators((recipeList.get(0)).getClinicOrgan(), request);
            LOGGER.info("uploadRecipeCirculationIndicators response={}", JSONUtils.toString(response));
            if (HIS_SUCCESS.equals(response.getMsgCode())) {
                commonResponse.setCode("000");
                LOGGER.info("uploadRecipeCirculationIndicators execute success.");
            } else {
                commonResponse.setMsg(response.getMsg());
            }
        } catch (Exception e) {
            LOGGER.warn("uploadRecipeCirculationIndicators HIS接口调用失败. request={}", JSONUtils.toString(request), e);
            commonResponse.setMsg("HIS接口调用异常");
        }
        LOGGER.info("uploadRecipeVerificationIndicators commonResponse={}", JSONUtils.toString(commonResponse));
        return commonResponse;
    }

    /**
     * 处方审核数据同步
     *
     * @param recipeList
     * @return
     */
    @Deprecated
    public CommonResponse uploadRecipeAuditIndicators(List<Recipe> recipeList) {
        LOGGER.info("uploadRecipeAuditIndicators recipeList length={}", recipeList.size());
        CommonResponse commonResponse = ResponseUtils.getFailResponse(CommonResponse.class, "");
        if (CollectionUtils.isEmpty(recipeList)) {
            commonResponse.setMsg("处方列表为空");
            return commonResponse;
        }
        IHisServiceConfigService configService = AppDomainContext.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
        //获取所有监管平台机构列表
        List<ServiceConfigResponseTO> list = configService.findAllRegulationOrgan();
        if (CollectionUtils.isEmpty(list)) {
            LOGGER.warn("uploadRecipeIndicators provUploadOrgan list is null.");
            commonResponse.setMsg("需要同步机构列表为空");
            return commonResponse;
        }
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        OrganService organService = BasicAPI.getService(OrganService.class);
        List<RegulationRecipeAuditIndicatorsReq> request = new ArrayList<>(recipeList.size());
        Map<Integer, DoctorDTO> doctorMap = new HashMap<>(20);
        RegulationRecipeAuditIndicatorsReq req;
        DoctorDTO doctorDTO;
        RedisClient redisClient = RedisClient.instance();
        String caSignature = null;
        for (Recipe recipe : recipeList) {
            req = new RegulationRecipeAuditIndicatorsReq();
            req.setOrganId(recipe.getClinicOrgan());
            //组织机构编码
            req.setOrganizeCode(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));
            req.setOrganName(recipe.getOrganName());
            //审核药师处理
            if (recipe.getChecker() != null) {
                doctorDTO = doctorMap.get(recipe.getChecker());
                if (null == doctorDTO) {
                    doctorDTO = doctorService.get(recipe.getChecker());
                    doctorMap.put(recipe.getChecker(), doctorDTO);
                }
                if (null == doctorDTO) {
                    LOGGER.warn("uploadRecipeIndicators checker is null. recipe.checker={}", recipe.getChecker());
                    continue;
                }
                //设置药师电子签名
                if (doctorDTO.getESignId() != null) {
                    try {
                        caSignature = redisClient.get(doctorDTO.getESignId() + "_signature");
                    } catch (Exception e) {
                        LOGGER.error("get caSignature error. doctorId={}", doctorDTO.getDoctorId(), e);
                    }
                    req.setAuditDoctorSign(StringUtils.isNotEmpty(caSignature) ? caSignature : "");
                }
                req.setAuditDoctorIdCard(doctorDTO.getIdNumber());
                req.setAuditDoctorName(doctorDTO.getName());
            }
            req.setAuditStatus(RecipeStatusConstant.CHECK_PASS_YS == recipe.getStatus() ? "1" : "2");
            req.setRecipeCode(recipe.getRecipeCode());
            request.add(req);
        }
        try {
            IRegulationService hisService = AppDomainContext.getBean("his.regulationService", IRegulationService.class);
            LOGGER.info("uploadRecipeAuditIndicators request={}", JSONUtils.toString(request));
            HisResponseTO response = hisService.uploadRecipeAuditIndicators(recipeList.get(0).getClinicOrgan(), request);
            LOGGER.info("uploadRecipeAuditIndicators response={}", JSONUtils.toString(response));
            if (HIS_SUCCESS.equals(response.getMsgCode())) {
                //成功
                commonResponse.setCode(CommonConstant.SUCCESS);
                LOGGER.info("uploadRecipeAuditIndicators execute success.");
            } else {
                commonResponse.setMsg(response.getMsg());
            }
        } catch (Exception e) {
            LOGGER.warn("uploadRecipeAuditIndicators HIS接口调用失败. request={}", JSONUtils.toString(request), e);
            commonResponse.setMsg("HIS接口调用异常");
        }

        LOGGER.info("uploadRecipeAuditIndicators commonResponse={}", JSONUtils.toString(commonResponse));
        return commonResponse;
    }

    /**
     * 处方流转数据同步
     *
     * @param recipeList
     * @return
     */
    @Deprecated
    public CommonResponse uploadRecipeCirculationIndicators(List<Recipe> recipeList) {
        LOGGER.info("uploadRecipeCirculationIndicators recipeList length={}", recipeList.size());
        CommonResponse commonResponse = ResponseUtils.getFailResponse(CommonResponse.class, "");
        if (CollectionUtils.isEmpty(recipeList)) {
            commonResponse.setMsg("处方列表为空");
            return commonResponse;
        }
        IHisServiceConfigService configService = AppDomainContext.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
        //获取所有监管平台机构列表
        List<ServiceConfigResponseTO> list = configService.findAllRegulationOrgan();
        if (CollectionUtils.isEmpty(list)) {
            LOGGER.warn("uploadRecipeIndicators provUploadOrgan list is null.");
            commonResponse.setMsg("需要同步机构列表为空");
            return commonResponse;
        }
        OrganService organService = BasicAPI.getService(OrganService.class);
        List<RegulationRecipeCirculationIndicatorsReq> request = new ArrayList<>(recipeList.size());
        RegulationRecipeCirculationIndicatorsReq req;
        for (Recipe recipe : recipeList) {
            req = new RegulationRecipeCirculationIndicatorsReq();
            req.setRecipeCode(recipe.getRecipeCode());
            req.setOrganId(recipe.getClinicOrgan());
            req.setOrganName(recipe.getOrganName());
            //组织机构编码
            req.setOrganizeCode(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));
            //0-配送1-自提
            req.setGiveMode("1");
            //0、在线支付，1、货到付款、2、到店支付
            req.setPayMode("0");
            request.add(req);
        }
        try {
            IRegulationService hisService = AppDomainContext.getBean("his.regulationService", IRegulationService.class);
            LOGGER.info("uploadRecipeCirculationIndicators request={}", JSONUtils.toString(request));
            HisResponseTO response = hisService.uploadRecipeCirculationIndicators(recipeList.get(0).getClinicOrgan(), request);
            LOGGER.info("uploadRecipeCirculationIndicators response={}", JSONUtils.toString(response));
            if (HIS_SUCCESS.equals(response.getMsgCode())) {
                //成功
                commonResponse.setCode(CommonConstant.SUCCESS);
                LOGGER.info("uploadRecipeCirculationIndicators execute success.");
            } else {
                commonResponse.setMsg(response.getMsg());
            }
        } catch (Exception e) {
            LOGGER.warn("uploadRecipeCirculationIndicators HIS接口调用失败. request={}", JSONUtils.toString(request), e);
            commonResponse.setMsg("HIS接口调用异常");
        }

        LOGGER.info("uploadRecipeCirculationIndicators commonResponse={}", JSONUtils.toString(commonResponse));
        return commonResponse;
    }

    /**
     * 设置处方详情数据
     *
     * @param req
     * @param detailList
     */
    private void setDetail(RegulationRecipeIndicatorsReq req, List<Recipedetail> detailList, Dictionary usingRateDic, Dictionary usePathwaysDic, Recipe recipe) {
        RegulationRecipeDetailIndicatorsReq reqDetail;
        DrugListDAO drugListDao = DAOFactory.getDAO(DrugListDAO.class);
        OrganDrugListDAO organDrugDao = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<RegulationRecipeDetailIndicatorsReq> list = new ArrayList<>(detailList.size());
        /*double dosageDay;*/
        DrugList drugList;
        OrganDrugList organDrugList;
        String useDose;
        for (Recipedetail detail : detailList) {
            reqDetail = new RegulationRecipeDetailIndicatorsReq();
            organDrugList = organDrugDao.getByOrganIdAndDrugId(recipe.getClinicOrgan(), detail.getDrugId());
            if (organDrugList == null) {
                reqDetail.setDrcode(detail.getOrganDrugCode());
            } else {
                reqDetail.setDrcode(StringUtils.isNotEmpty(organDrugList.getRegulationDrugCode()) ? organDrugList.getRegulationDrugCode() : organDrugList.getOrganDrugCode());
                reqDetail.setLicenseNumber(organDrugList.getLicenseNumber());
                reqDetail.setDosageFormCode(organDrugList.getDrugFormCode());
                reqDetail.setMedicalDrugCode(organDrugList.getMedicalDrugCode());
                reqDetail.setDrugFormCode(organDrugList.getDrugFormCode());
                reqDetail.setMedicalDrugFormCode(organDrugList.getMedicalDrugFormCode());
            }

            reqDetail.setDrname(detail.getDrugName());
            reqDetail.setDrmodel(detail.getDrugSpec());
            reqDetail.setPack(detail.getPack());
            reqDetail.setPackUnit(detail.getDrugUnit());
            //频次
            reqDetail.setFrequency(detail.getUsingRate());
            //药品频次名称
            if (null != usingRateDic) {
                reqDetail.setFrequencyName(usingRateDic.getText(detail.getUsingRate()));
            }
            //用法
            reqDetail.setAdmission(detail.getUsePathways());
            //药品用法名称
            if (null != usePathwaysDic) {
                reqDetail.setAdmissionName(usePathwaysDic.getText(detail.getUsePathways()));
            }
            useDose = detail.getUseDose() == null?detail.getUseDoseStr():String.valueOf(detail.getUseDose());
            reqDetail.setDosage(useDose);
            reqDetail.setDrunit(detail.getUseDoseUnit());
            reqDetail.setDosageTotal(detail.getUseTotalDose().toString());
            reqDetail.setUseDays(detail.getUseDays());
            reqDetail.setRemark(detail.getMemo());
            drugList = drugListDao.getById(detail.getDrugId());
            if (drugList != null) {
                //药物剂型代码
                reqDetail.setDosageForm(drugList.getDrugForm());
                //厂商
                reqDetail.setDrugManf(drugList.getProducer());
            }
            //药物使用总剂量
            reqDetail.setUseDosage("0");
            //药物日药量/DDD值
            /*dosageDay = (detail.getUseDose())*(UsingRateFilter.transDailyTimes(detail.getUsingRate()));*/
            reqDetail.setDosageDay("0");
            //中药处方详细描述
            if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
                reqDetail.setTcmDescribe(detail.getUsingRate() + detail.getUsePathways());
            }
            //处方明细Id
            reqDetail.setRecipeDetailId(detail.getRecipeDetailId());
            //单价
            reqDetail.setPrice(detail.getSalePrice());
            //总价
            reqDetail.setTotalPrice(detail.getDrugCost());

            list.add(reqDetail);
        }

        req.setOrderList(list);
    }

    /**
     * 处方撤销状态判断
     *
     * @param recipe 1正常 2撤销
     * @return
     */
    private String getVerificationRevokeStatus(Recipe recipe) {
        if (RecipeStatusConstant.REVOKE == recipe.getStatus() || RecipeStatusConstant.HIS_FAIL == recipe.getStatus() || RecipeStatusConstant.NO_DRUG == recipe.getStatus() || RecipeStatusConstant.NO_PAY == recipe.getStatus() || RecipeStatusConstant.NO_OPERATOR == recipe.getStatus()) {
            return "2";
        }
        if (RecipeStatusConstant.CHECK_PASS == recipe.getStatus()) {
            return "1";//仅处方开立上传
        }
        if (RecipeStatusConstant.CHECK_PASS_YS == recipe.getStatus() || RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
            return "3";//处方审核后上传（包含通过和不通过）
        }
        return "1";
    }

    /**
     * 处方核销状态判断，处方完成及开始配送都当做已核销处理
     *
     * @param recipe status 0未核销 1已核销
     * @return
     */
    private String getVerificationStatus(Recipe recipe) {
        if (RecipeStatusConstant.FINISH == recipe.getStatus() || RecipeStatusConstant.WAIT_SEND == recipe.getStatus() || RecipeStatusConstant.IN_SEND == recipe.getStatus() || RecipeStatusConstant.REVOKE == recipe.getStatus() || RecipeStatusConstant.HIS_FAIL == recipe.getStatus() || RecipeStatusConstant.NO_DRUG == recipe.getStatus() || RecipeStatusConstant.NO_PAY == recipe.getStatus() || RecipeStatusConstant.NO_OPERATOR == recipe.getStatus() || RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
            return "1";
        }

        return "0";
    }

    public void uploadRecipePayToRegulation(String orderCode, int payFlag) {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
        RecipeExtendDAO recipeExtendDAO = getDAO(RecipeExtendDAO.class);

        List<Integer> recipeIds = recipeDAO.findRecipeIdsByOrderCode(orderCode);
        if (null != recipeIds) {
            Recipe recipe = recipeDAO.get(recipeIds.get(0));
            RecipeOrder order = recipeOrderDAO.getByOrderCode(orderCode);
            try {
                if (null != recipe && order != null) {
                    IHisServiceConfigService configService = AppDomainContext.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
                    //获取所有监管平台机构列表
                    List<ServiceConfigResponseTO> serviceConfigResponseTOS = configService.findAllRegulationOrgan();
                    if (CollectionUtils.isEmpty(serviceConfigResponseTOS)) {
                        LOGGER.warn("uploadRecipePayToRegulation  regulationOrganList is null.");
                        return;
                    }
                    List<Integer> organList = serviceConfigResponseTOS.stream().map(ServiceConfigResponseTO::getOrganid).collect(Collectors.toList());
                    if (!organList.contains(recipe.getClinicOrgan())) {
                        LOGGER.warn("uploadRecipePayToRegulation organId={},没有关联监管平台", recipe.getClinicOrgan());
                        return;
                    }
                    RegulationOutpatientPayReq req = new RegulationOutpatientPayReq();
                    req.setRecipeId(recipe.getRecipeId());

                    PatientService patientService = BasicAPI.getService(PatientService.class);
                    PatientDTO patientDTO = patientService.getPatientDTOByMpiId(recipe.getMpiid());
                    if (patientDTO != null) {
                        req.setIdcardTypeCode("01");
                        req.setIdcardNo(patientDTO.getIdcard());
                        req.setName(patientDTO.getPatientName());
                        req.setGenderCode(patientDTO.getPatientSex());
                        req.setBirthdate(patientDTO.getBirthday());
                    }
                    req.setVisitNo(String.valueOf(recipe.getClinicId()));
                    req.setAccountNo(order.getTradeNo());
                    req.setTotalFee(recipe.getTotalMoney() != null ? recipe.getTotalMoney().doubleValue() : 0);
                    req.setIndividualPay(recipe.getActualPrice() != null ? recipe.getActualPrice().doubleValue() : 0);
                    req.setChargeRefundCode(String.valueOf(payFlag));
                    req.setOrganId(String.valueOf(recipe.getClinicOrgan()));
                    req.setOrgName(recipe.getOrganName());
                    req.setRcdDatetime(new Date());
                    req.setPayTypeCode("07"); //详见医疗费用分类代码表

                    List<RegulationCostDetailReq> items = new ArrayList<>();
                    //取处方单明细
                    RecipeDetailDAO recipeDetailDAO = getDAO(RecipeDetailDAO.class);
                    List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
                    for (Recipedetail item : recipedetails) {
                        RegulationCostDetailReq costDetailReq = new RegulationCostDetailReq();
                        costDetailReq.setProjDeno(String.valueOf(item.getRecipeDetailId()));
                        costDetailReq.setProjName(item.getDrugName());
                        costDetailReq.setChargeRefundCode(String.valueOf(payFlag));
                        costDetailReq.setStatCatCode("010100"); //监管分类代码 his未返回该字段，不知道传什么，默认传  010100 一般医疗服务
                        costDetailReq.setPinCatCode("9900"); // 财务分类代码 ，his未返回  9900 其他
                        costDetailReq.setIfOutMedIns("0");
                        costDetailReq.setProjUnitPrice(item.getSalePrice() != null ? item.getSalePrice().doubleValue() : 0);
                        costDetailReq.setProjCnt(item.getUseTotalDose());
                        costDetailReq.setProjAmount(item.getDrugCost() != null ? item.getDrugCost().doubleValue() : 0);
                        items.add(costDetailReq);
                    }
                    req.setItems(items);
                    // 科室相关
                    DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
                    DepartmentDTO departmentDTO = departmentService.getById(recipe.getDepart());
                    if (departmentDTO != null) {
                        req.setDeptCode(departmentDTO.getCode());
                        req.setDeptName(departmentDTO.getName());
                    }
                    req.setDeptClassCode("A99");
                    req.setDeptClassName("其他业务科室");
                    /*req.setOriginalAccountNo(outPatient.getRefundNo());*/
                    req.setOrderNo(order.getOutTradeNo());

                    //从his返回的挂号序号
                    RecipeExtend recipeExtend=recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                    if(recipeExtend!=null){
                        req.setRegisterNo(recipeExtend.getRegisterNo());
                    }

                    LOGGER.info("调用regulation接口，上传处方缴费信息，req = {}，payFlag = {}", JSONUtils.toString(req), payFlag);
                    IRegulationService regulationService = AppDomainContext.getBean("his.regulationService", IRegulationService.class);
                    HisResponseTO hisResponseTO = regulationService.uploadOutpatientPay(recipe.getClinicOrgan(), req);
                    LOGGER.info("调用regulation接口，上传处方缴费信息，res = {}，payFlag = {}", JSONUtils.toString(hisResponseTO), payFlag);
                }
            } catch (Exception e) {
                LOGGER.error("调用regulation接口，上传处方缴费信息失败，busId = {}，payFlag = {}", recipe.getRecipeId(), payFlag, e);
            }
        }
    }

    /**
     * 设置合理用药审核结果
     *
     * @param recipeId
     */
    private String setRationalDrug(Integer recipeId) {
        AuditMedicineIssueDAO issueDAO = DAOFactory.getDAO(AuditMedicineIssueDAO.class);
        List<AuditMedicineIssue> issueList = issueDAO.findIssueByRecipeId(recipeId);
        StringBuilder sb = new StringBuilder();
        for (AuditMedicineIssue issue : issueList) {
            sb.append(issue.getDetail());
        }

        return sb.toString();
    }
}
