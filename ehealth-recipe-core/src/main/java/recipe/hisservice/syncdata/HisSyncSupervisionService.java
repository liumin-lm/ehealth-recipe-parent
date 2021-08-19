package recipe.hisservice.syncdata;

import ca.service.ISignRecipeInfoService;
import ca.vo.model.SignDoctorRecipeInfoDTO;
import com.alibaba.fastjson.JSON;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.serviceconfig.mode.ServiceConfigResponseTO;
import com.ngari.base.serviceconfig.service.IHisServiceConfigService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.ConsultBean;
import com.ngari.consult.common.model.QuestionnaireBean;
import com.ngari.consult.common.service.IConsultService;
import com.ngari.his.regulation.entity.*;
import com.ngari.his.regulation.service.IRegulationService;
import com.ngari.patient.dto.*;
import com.ngari.patient.dto.zjs.SubCodeDTO;
import com.ngari.patient.service.*;
import com.ngari.patient.service.zjs.SubCodeService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.base.mode.PatientTO;
import com.ngari.platform.recipe.mode.RecipeExtendBean;
import com.ngari.recipe.dto.ApothecaryDTO;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.model.RevisitQuestionnaireBean;
import com.ngari.revisit.common.service.IRevisitExService;
import com.ngari.revisit.common.service.IRevisitService;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.cdr.api.service.IEmrPdfService;
import eh.recipeaudit.api.IAuditMedicinesService;
import eh.recipeaudit.model.AuditMedicineIssueBean;
import eh.recipeaudit.model.AuditMedicinesBean;
import eh.utils.ValidateUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.EleInvoiceDTO;
import recipe.bussutil.RecipeUtil;
import recipe.client.DocIndexClient;
import recipe.client.DoctorClient;
import recipe.common.CommonConstant;
import recipe.common.ResponseUtils;
import recipe.common.response.CommonResponse;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.hisservice.EleInvoiceService;
import recipe.manager.EmrRecipeManager;
import recipe.service.RecipeExtendService;
import recipe.util.ByteUtils;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;
import recipe.util.RedisClient;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * created by shiyuping on 2019/6/3
 * 平台监管平台同步
 */
@RpcBean("hisSyncSupervisionService")
public class HisSyncSupervisionService implements ICommonSyncSupervisionService {

    @Autowired
    private IAuditMedicinesService iAuditMedicinesService;
    @Autowired
    private DoctorClient doctorClient;
    @Autowired
    private DocIndexClient docIndexClient;
    @Autowired
    private ISignRecipeInfoService signRecipeInfoService;
    @Resource
    private IRegulationService regulationService;
    @Autowired
    private RecipeDAO recipeDAO;
    @Resource
    private IEmrPdfService emrPdfService;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private EmploymentService employmentService;
    @Autowired
    private OrganDrugListDAO organDrugDao;
    @Autowired
    private DoctorService doctorService;
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HisSyncSupervisionService.class);

    private static String HIS_SUCCESS = "200";

    public void uploadRecipePrepareCheck(Integer recipeId) {
        LOGGER.info("HisSyncSupervisionService uploadRecipePrepareCheck recipeId={}", recipeId);
        if (null == recipeId) {
            return;
        }
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        List<RegulationRecipeIndicatorsReq> request = new LinkedList<>();
        splicingBackRecipeData(Collections.singletonList(recipe), request);
        try {
            LOGGER.info("HisSyncSupervisionService uploadRecipePrepareCheck request={}", JSONUtils.toString(request));
            HisResponseTO response = regulationService.uploadRecipePrepareCheck(recipe.getClinicOrgan(), request);
            LOGGER.info("HisSyncSupervisionService uploadRecipePrepareCheck response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            LOGGER.error("HisSyncSupervisionService uploadRecipePrepareCheck HIS接口调用失败", e);
        }
    }

    /**
     * 同步处方数据
     *
     * @param recipeList
     * @return
     */
    @RpcService
    @Override
    public CommonResponse uploadRecipeIndicators(List<Recipe> recipeList) {
        LOGGER.info("uploadRecipeIndicators recipeList length={} recipeId={}", recipeList.size(), recipeList.get (0).getRecipeId());
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
            LOGGER.error("uploadRecipeIndicators HIS接口调用失败. request={}", JSONUtils.toString(request), e);
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
    public void splicingBackRecipeData(List<Recipe> recipeList, List<RegulationRecipeIndicatorsReq> request) {

        DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
        EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        PatientService patientService = BasicAPI.getService(PatientService.class);
        SubCodeService subCodeService = BasicAPI.getService(SubCodeService.class);
        OrganService organService = BasicAPI.getService(OrganService.class);
        IConsultService iConsultService = ApplicationUtils.getConsultService(IConsultService.class);
        IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);
        IRevisitExService iRevisitExService = RevisitAPI.getService(IRevisitExService.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        DoctorExtendService doctorExtendService = BasicAPI.getService(DoctorExtendService.class);
        CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);

        Map<Integer, OrganDTO> organMap = new HashMap<>(20);
        Map<Integer, DepartmentDTO> departMap = new HashMap<>(20);
        Map<Integer, DoctorDTO> doctorMap = new HashMap<>(20);

        Dictionary usingRateDic = null;
        Dictionary usePathwaysDic = null;
        try {
            usingRateDic = DictionaryController.instance().get("eh.cdr.dictionary.UsingRate");
            usePathwaysDic = DictionaryController.instance().get("eh.cdr.dictionary.UsePathways");
        } catch (ControllerException e) {
            LOGGER.error("uploadRecipeIndicators dic error.", e);
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
        List<AuditMedicinesBean> medicineList;
        AuditMedicinesBean medicine;
        RecipeExtend recipeExtend;
        RecipeOrder recipeOrder;
        DoctorExtendDTO doctorExtendDTO;
        RevisitExDTO consultExDTO;
        SignDoctorRecipeInfoDTO caInfo;
        RedisClient redisClient = RedisClient.instance();
        String caSignature = null;
        for (Recipe recipe : recipeList) {
            recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
            req = new RegulationRecipeIndicatorsReq();
            // 电子病历PDF id
            Map<String, Object> docIndex = emrPdfService.generateEmrPdf(recipeExtend.getDocIndexId());
            LOGGER.info("电子病历 PDF 返回信息 recipe:{}, recipeExtend: {} ,docIndex:{}", recipe.getRecipeId(), JSON.toJSONString(recipeExtend), JSONUtils.toString(docIndex));
            if (MapUtils.isNotEmpty(docIndex) && Objects.nonNull(docIndex.get("fileId"))) {
                req.setMedicalFileId(docIndex.get("fileId").toString());
            }

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
            } else {
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
            //江苏ca用到
            caInfo=signRecipeInfoService.getSignRecipeInfoByRecipeIdAndServerType(recipe.getRecipeId(),1);
            if (caInfo != null) {
                //医生签名值
                req.setDoctorSign(caInfo.getSignCodeDoc());
                //药师签名值
                req.setAuditDoctorSign(caInfo.getSignCodePha());
            }
            //如果其他ca里取不到默认用e签宝的
            if (StringUtils.isEmpty(req.getDoctorSign()) && doctorDTO.getESignId() != null) {
                //设置医生电子签名
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
                //工号：医生取开方机构的工号，药师取第一职业点的工号
                EmploymentDTO employment = iEmploymentService.getPrimaryEmpByDoctorId(recipe.getChecker());
                if (employment != null) {
                    req.setAuditDoctorNo(employment.getJobNumber());

                    //科室处理
                    DepartmentDTO deptDTO = departmentService.get(employment.getDepartment());
                    if (null != deptDTO) {
                        req.setAuditDeptID(deptDTO.getCode());
                        req.setAuditDeptName(deptDTO.getName());
                        //设置专科编码等
                        subCodeDTO = subCodeService.getByNgariProfessionCode(deptDTO.getProfessionCode());
                        if (null == subCodeDTO) {
                            //专科编码没设置不应该导致推送不了处方到监管平台
                            LOGGER.warn("uploadRecipeIndicators subCode is null. recipe.professionCode={}", deptDTO.getProfessionCode());
                        } else {
                            req.setAuditSubjectCode(subCodeDTO.getSubCode());
                            req.setAuditSubjectName(subCodeDTO.getSubName());
                        }
                    }
                }
                //设置药师电子签名
                //如果其他ca里取不到默认用e签宝的
                if (StringUtils.isEmpty(req.getAuditDoctorSign()) && doctorDTO.getESignId() != null) {
                    try {
                        caSignature = redisClient.get(doctorDTO.getESignId() + "_signature");
                    } catch (Exception e) {
                        LOGGER.error("get caSignature error. doctorId={}", doctorDTO.getDoctorId(), e);
                    }
                    req.setAuditDoctorSign(StringUtils.isNotEmpty(caSignature) ? caSignature : "");
                }
            }

            //患者处理
            patientDTO = patientService.get(recipe.getMpiid());
            if (null == patientDTO) {
                LOGGER.warn("uploadRecipeIndicators patient is null. recipe.patient={}", recipe.getMpiid());
                continue;
            }
            req.setMpiId(patientDTO.getMpiId());
            req.setPatientCardType(LocalStringUtil.toString(patientDTO.getCertificateType()));
            if (new Integer(2).equals(patientDTO.getPatientUserType())) {
                //无证身份证儿童包含特殊字符
                req.setPatientCertID(patientDTO.getGuardianCertificate());
            } else {
                req.setPatientCertID(LocalStringUtil.toString(patientDTO.getCertificate()));
            }
            req.setPatientName(patientDTO.getPatientName());
            req.setNation(patientDTO.getNation());
            req.setMobile(LocalStringUtil.toString(patientDTO.getMobile()));
            req.setSex(patientDTO.getPatientSex());
            req.setAge(DateConversion.calculateAge(patientDTO.getBirthday()));
            req.setBirthDay(patientDTO.getBirthday());
            //陪诊人信息
            req.setGuardianName(patientDTO.getGuardianName());
            req.setGuardianCertID(patientDTO.getGuardianCertificate());
            //儿童的手机号就是陪诊人的手机号
            if (StringUtils.isNotEmpty(patientDTO.getGuardianCertificate())) {
                req.setGuardianMobile(patientDTO.getMobile());
            }
            //其他信息
            //监管接收方现在使用recipeId去重
            req.setRecipeID(recipe.getRecipeId().toString());
            //处方唯一编号
            req.setRecipeUniqueID(recipe.getRecipeCode());
            //审方时间
            req.setCheckDate(recipe.getCheckDateYs());
            //互联网医院处方都是经过合理用药审查
            req.setRationalFlag("1");
            //medicineList = auditMedicinesDAO.findMedicinesByRecipeId(recipe.getRecipeId());
            medicineList = iAuditMedicinesService.findMedicinesByRecipeId(recipe.getRecipeId());
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
            //诊断备注
            req.setMemo(recipe.getMemo());
            req.setRecipeType(recipe.getRecipeType().toString());
            req.setPacketsNum(recipe.getCopyNum());
            req.setDatein(recipe.getSignDate());
            req.setEffectivePeriod(recipe.getValueDays());
            req.setStartDate(recipe.getSignDate());
            if (recipe.getSignDate() != null) {
                req.setEndDate(DateConversion.getDateAftXDays(recipe.getSignDate(), recipe.getValueDays()));
            }
            req.setUpdateTime(now);
            req.setTotalFee(recipe.getTotalMoney().doubleValue());
            req.setIsPay(recipe.getPayFlag().toString());

            if (recipe.getClinicId() != null) {
                req.setBussID(recipe.getClinicId().toString());
                //处方来源 1-问诊 4复诊
                if (!RecipeBussConstant.BUSS_SOURCE_NONE.equals(recipe.getBussSource())) {
                    if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource())) {
                        req.setBussSource("4");
                        RevisitBean revisitBean = iRevisitService.getById(recipe.getClinicId());
                        RevisitQuestionnaireBean questionnaire = iRevisitService.getConsultQuestionnaireByConsultId(recipe.getClinicId());
                        if (revisitBean != null) {
                            req.setMainDieaseDescribe(revisitBean.getLeaveMess());
                            //咨询开始时间
                            // bug=60819 不从复诊开处方时复诊开始时间为空导致前置机上传监管平台失败
                            req.setConsultStartDate(revisitBean.getStartDate() != null ? revisitBean.getStartDate() : revisitBean.getRequestTime());
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
                    } else {
                        req.setBussSource("1");
                        ConsultBean consultBean = iConsultService.getById(recipe.getClinicId());
                        QuestionnaireBean questionnaire = iConsultService.getConsultQuestionnaireByConsultId(recipe.getClinicId());
                        if (consultBean != null) {
                            req.setMainDieaseDescribe(consultBean.getLeaveMess());
                            //咨询开始时间
                            // bug=60819 不从复诊开处方时复诊开始时间为空导致前置机上传监管平台失败
                            req.setConsultStartDate(consultBean.getStartDate() != null ? consultBean.getStartDate() : consultBean.getRequestTime());
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
                }

            }
            organDiseaseName = recipe.getOrganDiseaseName().replaceAll(ByteUtils.SEMI_COLON_EN, "|");
            req.setOriginalDiagnosis(organDiseaseName);
            req.setIcdCode(recipe.getOrganDiseaseId().replaceAll(ByteUtils.SEMI_COLON_EN, "|"));
            req.setIcdName(organDiseaseName);
            //门诊号处理
            if (recipeExtend != null) {
                req.setPatientNumber(recipeExtend.getRegisterID());
                req.setCardNo(recipeExtend.getCardNo());
                req.setCardType(recipeExtend.getCardType());
                req.setRegisterNo(recipeExtend.getRegisterID());
                req.setRegisterId(recipeExtend.getRegisterID());
            }
            //处方状态
            req.setRecipeStatus(recipe.getStatus());

            //撤销标记
            req.setCancelFlag(getVerificationRevokeStatus(recipe));
            //核销标记
            req.setVerificationStatus(getVerificationStatus(recipe));
            req.setPayFlag(null == recipe.getPayFlag() ? "" : String.valueOf(recipe.getPayFlag()));
            doctorExtendDTO = doctorExtendService.getByDoctorId(recipe.getDoctor());
            if (null != doctorExtendDTO) {
                req.setSerialNumCA(doctorExtendDTO.getSerialNumCA()); //医护人员证件序列号
            }
            //医生处方签名生成时间戳
            req.setSignCADate(recipe.getSignCADate());
            //医生处方数字签名值
            req.setSignRecipeCode(recipe.getSignRecipeCode());
            //药师处方数字签名值
            req.setSignPharmacistCode(recipe.getSignPharmacistCode());
            recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            if (null != recipeOrder) {
                //配送方式
                req.setDeliveryType(null == recipe.getGiveMode() ? "" : recipe.getGiveMode().toString());
                //配送开始时间
                req.setSendTime(recipeOrder.getSendTime());
                //配送结束时间
                req.setFinishTime(recipeOrder.getFinishTime());
                //配送状态
                req.setDeliveryStatus(recipeOrder.getStatus());
                //商户订单号
                req.setOutTradeNo(recipeOrder.getOutTradeNo());
                //支付时间
                req.setPayTime(recipeOrder.getPayTime());
                String address = commonRemoteService.getCompleteAddress(recipeOrder);
                req.setAddress(address);
            }

            //卡号，卡类型
            if (req.getCardNo() == null) {
                //取复诊单里的卡号卡信息
                if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource())) {
                    consultExDTO = iRevisitExService.getByConsultId(recipe.getClinicId());
                    if (null != consultExDTO) {
                        req.setCardNo(consultExDTO.getCardId());
                        req.setCardType(consultExDTO.getCardType());
                    }
                }
            }
            //处方pdfId
            if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                req.setRecipeFileId(recipe.getChemistSignFile());
            } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
                req.setRecipeFileId(recipe.getSignFile());
            } else {
                LOGGER.warn("recipeId file is null  recipeId={}", recipe.getRecipeId());
            }
            //详情处理
            detailList = detailDAO.findByRecipeId(recipe.getRecipeId());
            if (CollectionUtils.isEmpty(detailList)) {
                LOGGER.warn("uploadRecipeIndicators detail is null. recipe.id={}", recipe.getRecipeId());
                continue;
            }
            setRecipeExtend(req, recipeExtend);
            setDetail(req, detailList, usingRateDic, usePathwaysDic, recipe);

            // 发票号
            String invoiceNumber = getInvoiceNumber(recipeExtend, recipe);
            req.setEinvoiceNumber(invoiceNumber);

            //优先取运营平台处方详情设置的发药药师，如果没有取机构默认发药药师，都没有就为空
            IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            String doctorId = (String) configurationService.getConfiguration(recipe.getClinicOrgan(), "oragnDefaultDispensingApothecary");
            //获取运营平台发药药师
            ApothecaryDTO apothecaryDTO = doctorClient.getGiveUser(recipe);
            if (StringUtils.isNotEmpty(apothecaryDTO.getGiveUserName())) {
                req.setDispensingApothecaryName(apothecaryDTO.getGiveUserName());
            } else if (doctorId != null) {
                //获取默认发药药师
                DoctorDTO dispensingApothecary = doctorService.get(Integer.valueOf(doctorId));
                req.setDispensingApothecaryName(dispensingApothecary.getName());
            }
            if (StringUtils.isNotEmpty(apothecaryDTO.getGiveUserIdCard())) {
                req.setDispensingApothecaryIdCard(apothecaryDTO.getGiveUserIdCard());
            } else if (doctorId != null) {
                //获取默认发药药师
                DoctorDTO dispensingApothecary = doctorService.get(Integer.valueOf(doctorId));
                req.setDispensingApothecaryIdCard(dispensingApothecary.getIdNumber());
            }
            request.add(req);
        }
    }

    /**
     * 设置处方扩展数据
     *
     * @param req
     * @param recipeExtend
     */
    private void setRecipeExtend(RegulationRecipeIndicatorsReq req, RecipeExtend recipeExtend) {
        //处方扩展信息
        req.setRecipeExtend(ObjectCopyUtils.convert(recipeExtend, RecipeExtendBean.class));
        try {
            //制法Code 煎法Code 中医证候Code
            DrugDecoctionWayDao drugDecoctionWayDao = DAOFactory.getDAO(DrugDecoctionWayDao.class);
            DrugMakingMethodDao drugMakingMethodDao = DAOFactory.getDAO(DrugMakingMethodDao.class);
            SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
            if (StringUtils.isNotBlank(recipeExtend.getDecoctionId())) {
                DecoctionWay decoctionWay = drugDecoctionWayDao.get(Integer.parseInt(recipeExtend.getDecoctionId()));
                req.getRecipeExtend().setDecoctionCode(decoctionWay.getDecoctionCode());
            }
            if (StringUtils.isNotBlank(recipeExtend.getMakeMethodId())) {
                DrugMakingMethod drugMakingMethod = drugMakingMethodDao.get(Integer.parseInt(recipeExtend.getMakeMethodId()));
                req.getRecipeExtend().setMakeMethod(drugMakingMethod.getMethodCode());

            }
            if (StringUtils.isNotBlank(recipeExtend.getSymptomId())) {
                Symptom symptom = symptomDAO.get(Integer.parseInt(recipeExtend.getSymptomId()));
                req.getRecipeExtend().setSymptomCode(symptom.getSymptomCode());
            }
        } catch (Exception e) {
            LOGGER.error("setRecipeExtend recipeid:{} error :{}", recipeExtend.getRecipeId(), e);
        }

    }

    /**
     * 获取发票号
     *
     * @param recipeExtend
     * @param recipe
     * @return
     */
    private String getInvoiceNumber(RecipeExtend recipeExtend, Recipe recipe) {
        String invoiceNumber = null;
        try {
            RecipeExtendService extendService = AppContextHolder.getBean("recipeExtendService", RecipeExtendService.class);
            invoiceNumber = extendService.queryEinvoiceNumberByRecipeId(recipe.getRecipeId());
            if (StringUtils.isBlank(invoiceNumber)) {
                EleInvoiceService invoiceService = AppContextHolder.getBean("eleInvoiceService", EleInvoiceService.class);
                EleInvoiceDTO invoiceDTO = new EleInvoiceDTO();
                invoiceDTO.setId(recipe.getRecipeId());
                if (null != recipeExtend) {
                    invoiceDTO.setCardId(recipeExtend.getCardNo());
                    invoiceDTO.setCardType(recipeExtend.getCardType());
                    invoiceDTO.setGhxh(recipeExtend.getRegisterID());
                }
                invoiceDTO.setMpiid(recipe.getMpiid());
                invoiceDTO.setOrganId(recipe.getClinicOrgan());
                invoiceDTO.setType("1");
                invoiceService.findEleInvoice(invoiceDTO);
            }
            invoiceNumber = extendService.queryEinvoiceNumberByRecipeId(recipe.getRecipeId());
        } catch (Exception e) {
            LOGGER.error("上传监管平台获取发票号异常，recipeId={},", recipe.getRecipeId(), e);
        }
        return invoiceNumber;
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
            req.setRecipeCode(recipe.getRecipeCode());
            req.setRecipeType(recipe.getRecipeType());
            req.setBussSource(recipe.getBussSource());

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
            req.setPayFlag(order.getPayFlag());
            req.setOutTradeNo(order.getOutTradeNo());

            RecipeExtendDAO recipeExtendDAO = getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if (recipeExtend != null) {
                //从his返回的挂号序号
                req.setRegisterNo(recipeExtend.getRegisterID());
                req.setRegisterId(recipeExtend.getRegisterID());
                //从监管平台返回监管平台流水号
                req.setSuperviseRecipecode(recipeExtend.getSuperviseRecipecode());
            }

            //处方药品明细
            List<RegulationCostDetailReq> items = pakRegulationCostDetailReq(recipe);
            req.setDetails(items);

            //审方医生信息
            req.setCheckDoctor(getRegulationBusDocReq(recipe.getChecker(), null, null));

            //开方医生信息
            req.setDoctor(getRegulationBusDocReq(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart()));

            //就诊人信息
            req.setPetient(pakRegulationBusPatientReq(recipe.getMpiid()));
            req.setCardNo(recipeExtend.getCardNo());

            //主诉
            req.setMainDiseaseDescribe(StringUtils.isNotEmpty(recipeExtend.getMainDieaseDescribe()) ? recipeExtend.getMainDieaseDescribe() : "无");

            //现病史
            req.setHistoryOfPresentIllness(StringUtils.isNotEmpty(recipeExtend.getCurrentMedical()) ? recipeExtend.getCurrentMedical() : "无");
            //时间
            IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);
            if(null!=recipe.getClinicId()){
                RevisitBean revisitBean = iRevisitService.getById(recipe.getClinicId());
                if (revisitBean != null) {
                    //咨询开始时间
                    req.setConsultStartDate(revisitBean.getStartDate() != null ? revisitBean.getStartDate() : revisitBean.getRequestTime());
                }
            }

            // 科室相关
            DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
            DepartmentDTO departmentDTO = departmentService.getById(recipe.getDepart());
            if (departmentDTO != null) {
                req.setDeptCode(departmentDTO.getCode());
                req.setDeptName(departmentDTO.getName());
            }
            //患者处理
            PatientService patientService = BasicAPI.getService(PatientService.class);
            PatientDTO patientDTO = patientService.get(recipe.getMpiid());
            if (null == patientDTO) {
                LOGGER.warn("uploadRecipeIndicators patient is null. recipe.patient={}", recipe.getMpiid());
                continue;
            }
            //陪诊人信息
            req.setGuardianName(patientDTO.getGuardianName());
            req.setGuardianCertID(patientDTO.getGuardianCertificate());
            //支付时间
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            req.setPayTime(recipeOrder.getPayTime());
            req.setRcdDatetime(new Date()); //退费时间取当前时间
            req.setMedicalPayFlag(recipe.getMedicalPayFlag());
            req.setTotalFee(recipe.getTotalMoney() != null ? recipe.getTotalMoney().doubleValue() : 0);

            //获取发药药师工号
            if (recipe.getChecker() != null) {
                EmploymentDTO employment = employmentService.getPrimaryEmpByDoctorId(recipe.getChecker());
                if (employment != null) {
                    req.setDispensingCheckerId(employment.getJobNumber());
                }
            }
            //优先取运营平台处方详情设置的发药药师，如果没有取机构默认发药药师，都没有就为空
            IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            String doctorId = (String) configurationService.getConfiguration(recipe.getClinicOrgan(), "oragnDefaultDispensingApothecary");
            ApothecaryDTO apothecaryDTO = doctorClient.getGiveUser(recipe);
            if (StringUtils.isNotEmpty(apothecaryDTO.getGiveUserName())) {
                req.setDispensingCheckerName(apothecaryDTO.getGiveUserName());
            } else if (doctorId != null) {
                //获取机构发药药师
                DoctorDTO dispensingApothecary = doctorService.get(Integer.valueOf(doctorId));
                req.setDispensingCheckerName(dispensingApothecary.getName());
            }
            //获取发药时间  订单表中进行获取
            req.setDispensingTime(order.getDispensingTime());
            //诊断编码
            req.setOrganDiseaseId(recipe.getOrganDiseaseId());
            //诊断名称
            req.setOrganDiseaseName(recipe.getOrganDiseaseName());
            //开方时间
            req.setCreateDate(recipe.getCreateDate());
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
            LOGGER.error("uploadRecipeCirculationIndicators HIS接口调用失败. request={}", JSONUtils.toString(request), e);
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
            LOGGER.error("uploadRecipeAuditIndicators HIS接口调用失败. request={}", JSONUtils.toString(request), e);
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
            LOGGER.error("uploadRecipeCirculationIndicators HIS接口调用失败. request={}", JSONUtils.toString(request), e);
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
            organDrugList = organDrugDao.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(), detail.getOrganDrugCode(), detail.getDrugId());
            if (organDrugList == null) {
                reqDetail.setDrcode(detail.getOrganDrugCode());
            } else {
                reqDetail.setOrganDrugCode(organDrugList.getOrganDrugCode());
                reqDetail.setDrcode(StringUtils.isNotEmpty(organDrugList.getRegulationDrugCode()) ? organDrugList.getRegulationDrugCode() : organDrugList.getOrganDrugCode());
                reqDetail.setLicenseNumber(organDrugList.getLicenseNumber());
                reqDetail.setDosageFormCode(organDrugList.getDrugFormCode());
                reqDetail.setMedicalDrugCode(organDrugList.getMedicalDrugCode());
                reqDetail.setDrugFormCode(organDrugList.getDrugFormCode());
                reqDetail.setMedicalDrugFormCode(organDrugList.getMedicalDrugFormCode());
                reqDetail.setRegulationDrugCode(organDrugList.getRegulationDrugCode());
            }

            reqDetail.setDrname(detail.getDrugName());
            reqDetail.setDrmodel(detail.getDrugSpec());
            reqDetail.setPack(detail.getPack());
            reqDetail.setPackUnit(detail.getDrugUnit());
            //频次
            reqDetail.setFrequency(detail.getUsingRate());
            //机构频次
            reqDetail.setOrganUsingRate(detail.getOrganUsingRate());
            //机构频次名称
            reqDetail.setUsingRateTextFromHis(detail.getUsingRateTextFromHis());
            //药品频次名称
            if (null != usingRateDic) {
                reqDetail.setFrequencyName(detail.getUsingRateTextFromHis() != null ? detail.getUsingRateTextFromHis() : usingRateDic.getText(detail.getUsingRate()));
            }
            //用法
            reqDetail.setAdmission(detail.getUsePathways());
            //机构用法
            reqDetail.setOrganUsePathways(detail.getOrganUsePathways());
            //机构用法名称
            reqDetail.setUsePathwaysTextFromHis(detail.getUsePathwaysTextFromHis());
            //药品用法名称
            if (null != usePathwaysDic) {
                reqDetail.setAdmissionName(detail.getUsePathwaysTextFromHis() != null ? detail.getUsePathwaysTextFromHis() : usePathwaysDic.getText(detail.getUsePathways()));
            }
            useDose = detail.getUseDose() == null ? detail.getUseDoseStr() : String.valueOf(detail.getUseDose());
            reqDetail.setDosage(useDose);
            reqDetail.setDrunit(detail.getUseDoseUnit());
            reqDetail.setDosageTotal(detail.getUseTotalDose().toString());
            //date 202000526
            //添加字段UseDaysB用药天数的小数类型数据
            reqDetail.setUseDays(detail.getUseDays());
            reqDetail.setUseDaysB(detail.getUseDaysB());
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
        LOGGER.info("uploadRecipePayToRegulation param orderCode:{} ,payFlag:{}",orderCode,payFlag);
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
        RecipeExtendDAO recipeExtendDAO = getDAO(RecipeExtendDAO.class);

        List<Integer> recipeIds = recipeDAO.findRecipeIdsByOrderCode(orderCode);
        if (null != recipeIds) {
            RecipeOrder order = recipeOrderDAO.getByOrderCode(orderCode);
            Recipe recipe = recipeDAO.get(recipeIds.get(0));
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
                    //留着兼容
                    req.setRecipeId(recipe.getRecipeId());
                    //前置机会根据处方id列表反查
                    req.setRecipeIds(recipeIds);
                    PatientService patientService = BasicAPI.getService(PatientService.class);
                    PatientDTO patientDTO = patientService.getPatientDTOByMpiId(recipe.getMpiid());
                    if (patientDTO != null) {
                        req.setIdcardTypeCode("01");
                        req.setIdcardNo(patientDTO.getIdcard());
                        req.setName(patientDTO.getPatientName());
                        req.setGenderCode(patientDTO.getPatientSex());
                        req.setBirthdate(patientDTO.getBirthday());
                        req.setNation(patientDTO.getNation());
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
                    List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeIds(recipeIds);
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
                    RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                    if (recipeExtend != null) {
                        req.setRegisterNo(recipeExtend.getRegisterID());
                        req.setRegisterId(recipeExtend.getRegisterID());
                    }
                    //开方医生信息
                    req.setDoctor(getRegulationBusDocReq(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart()));
                    //复诊id
                    req.setBussID(recipe.getClinicId()!=null?recipe.getClinicId().toString():null);
                    //医保金额
                    req.setFundAmount(order.getFundAmount());
                    //自费金额
                    req.setCashAmount(order.getCashAmount());
                    req.setRecipeCode(recipe.getRecipeCode());

                    // 购药方式
                    req.setGiveMode(recipe.getGiveMode());
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
//        AuditMedicineIssueDAO issueDAO = DAOFactory.getDAO(AuditMedicineIssueDAO.class);
        List<AuditMedicineIssueBean> issueList = iAuditMedicinesService.findIssueByRecipeId(recipeId);
        StringBuilder sb = new StringBuilder();
        for (AuditMedicineIssueBean issue : issueList) {
            sb.append(issue.getDetail());
        }

        return sb.toString();
    }


    /**
     * 派药接口
     *
     * @return
     **/
    @RpcService
    public CommonResponse uploadSendMedicine(Integer recipeId) {
        CommonResponse commonResponse = ResponseUtils.getFailResponse(CommonResponse.class, "");
        try {
            RegulationSendMedicineReq req = pakRegulationSendMedicineReq(recipeId);
            if (null == req) {
                return commonResponse;
            }

            LOGGER.info("调用regulation接口，上传派药信息，req = {}", JSONUtils.toString(req));
            IRegulationService regulationService = AppDomainContext.getBean("his.regulationService", IRegulationService.class);
            //监管平台进行数据上传
            HisResponseTO hisResponseTO = regulationService.uploadSendMedicine(Integer.valueOf(req.getOrganId()), req);
            LOGGER.info("调用regulation接口，上传派药信息，res = {}", JSONUtils.toString(hisResponseTO));


            if (hisResponseTO.isSuccess()) {
                commonResponse.setCode(CommonConstant.SUCCESS);
            } else {
                commonResponse.setMsg(hisResponseTO.getMsg());
            }

        } catch (Exception e) {
            LOGGER.error("调用regulation接口，上传派药信息，busId = {}", recipeId, e);
        }
        return commonResponse;
    }

    /**
     * 拼接配送到家-配送完成调用监管平台参数、以及调用
     *
     * @return
     **/
    @RpcService
    public CommonResponse uploadFinishMedicine(Integer recipeId) {
        CommonResponse commonResponse = ResponseUtils.getFailResponse(CommonResponse.class, "");
        try {
            RegulationSendMedicineReq req = pakRegulationSendMedicineReq(recipeId);
            if (null == req) {
                return commonResponse;
            }
            LOGGER.info("调用regulation接口，上传[配送到家-处方完成]信息，req = {}", JSONUtils.toString(req));
            IRegulationService regulationService = AppDomainContext.getBean("his.regulationService", IRegulationService.class);
            HisResponseTO hisResponseTO = regulationService.uploadFinishMedicine(Integer.valueOf(req.getOrganId()), req);
            LOGGER.info("调用regulation接口，上传[配送到家-处方完成]信息，res = {}", JSONUtils.toString(hisResponseTO));


            if (hisResponseTO.isSuccess()) {
                commonResponse.setCode(CommonConstant.SUCCESS);
            } else {
                commonResponse.setMsg(hisResponseTO.getMsg());
            }
        } catch (Exception e) {
            LOGGER.error("组装参数pakRegulationSendMedicineReq报错，busId = {}", recipeId, e);
        }
        return commonResponse;
    }

    /**
     * 医生信息
     **/
    private RegulationBusDocReq getRegulationBusDocReq(Integer doctorId, Integer organId, Integer deptId) {
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        RegulationBusDocReq regulationBusDocReq = new RegulationBusDocReq();

        if (null == doctorId) {
            return regulationBusDocReq;
        }

        DoctorDTO doctor = doctorService.getByDoctorId(doctorId);
        if (null == doctor) {
            return regulationBusDocReq;
        }

        //医生基础信息
        regulationBusDocReq = ObjectCopyUtils.convert(doctor, RegulationBusDocReq.class);

        //工号：医生取开方机构的工号，药师取第一职业点的工号
        EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
        if (null == organId) {
            EmploymentDTO employment = employmentService.getPrimaryEmpByDoctorId(doctorId);
            if (null != employment) {
                regulationBusDocReq.setJobNum(employment.getJobNumber());
            }
        } else {
            regulationBusDocReq.setJobNum(employmentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(doctorId, organId, deptId));
        }

        return regulationBusDocReq;
    }

    /**
     * 患者信息
     **/
    private PatientTO pakRegulationBusPatientReq(String mpiId) {
        PatientTO patient = null;
        if (StringUtils.isEmpty(mpiId)) {
            return patient;
        }


        PatientService patientService = BasicAPI.getService(PatientService.class);
        PatientDTO patientDto = patientService.getPatientDTOByMpiId(mpiId);
        if (patientDto != null) {
            patient = new PatientTO();
            patient.setPatientName(patientDto.getPatientName());
            patient.setPatientSex(patientDto.getPatientSex());
            patient.setBirthday(patientDto.getBirthday());
            patient.setMobile(patientDto.getMobile());
            patient.setIdcard(patientDto.getIdcard());
        }

        return patient;
    }

    /**
     * 组装物流信息
     **/
    private RegulationLogisticsReq pakRegulationLogisticsReq(RecipeOrder order) {

        if (null == order) {
            return null;
        }

        //物流信息
        RegulationLogisticsReq LogisticsInfo = ObjectCopyUtils.convert(order, RegulationLogisticsReq.class);
        try {
            String company = DictionaryController.instance().get("eh.cdr.dictionary.LogisticsCompany").getText(LogisticsInfo.getLogisticsCompany());
            LogisticsInfo.setLogisticsCompanyName(company);

            String address1 = DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(order.getAddress1());
            String address2 = DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(order.getAddress2());
            String address3 = DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(order.getAddress3());
            String streetAddress = DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(order.getStreetAddress());
            StringBuffer streetAddressBuffer = new StringBuffer().append(null == address1 ? "" : address1).append(null == address2 ? "" : address2).append(null == address3 ? "" : address3).append(null == streetAddress ? "" : streetAddress).append(null == order.getAddress4() ? "" : order.getAddress4());
            LogisticsInfo.setAddressInfo(streetAddressBuffer.toString());

        } catch (ControllerException e) {
            LOGGER.error("toSend get logisticsCompany error. logisticsCompany={}", LogisticsInfo.getLogisticsCompany(), e);
        }

        return LogisticsInfo;
    }

    /**
     * 组装处方明细数据
     **/
    private List<RegulationCostDetailReq> pakRegulationCostDetailReq(Recipe recipe) {
        if (null == recipe) {
            return null;
        }

        List<RegulationCostDetailReq> items = new ArrayList<>();
        //取处方单明细
        RecipeDetailDAO recipeDetailDAO = getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        for (Recipedetail item : recipedetails) {
            RegulationCostDetailReq costDetailReq = new RegulationCostDetailReq();
            costDetailReq.setProjDeno(String.valueOf(item.getRecipeDetailId()));
            costDetailReq.setProjName(item.getDrugName());
            costDetailReq.setChargeRefundCode(String.valueOf(recipe.getPayFlag()));
            costDetailReq.setStatCatCode("010100"); //监管分类代码 his未返回该字段，不知道传什么，默认传  010100 一般医疗服务
            costDetailReq.setPinCatCode("9900"); // 财务分类代码 ，his未返回  9900 其他
            costDetailReq.setIfOutMedIns("0");
            costDetailReq.setProjUnitPrice(item.getSalePrice() != null ? item.getSalePrice().doubleValue() : 0);
            costDetailReq.setProjCnt(item.getUseTotalDose());
            costDetailReq.setProjAmount(item.getDrugCost() != null ? item.getDrugCost().doubleValue() : 0);
            costDetailReq.setDetailId(item.getRecipeDetailId());

            //用药频次
            costDetailReq.setUsingRate(item.getOrganUsingRate());
            //用药途径
            costDetailReq.setUsePathways(item.getOrganUsePathways());
            //用药剂量
            costDetailReq.setUseDose(item.getUseDose());
            //剂量单位1
            costDetailReq.setDosageUnit(item.getDosageUnit());
            //剂量单位2
            costDetailReq.setUseDoseUnit(item.getUseDoseUnit());
            //包装单位
            costDetailReq.setPack(item.getDrugUnit());
            //用药途径名
            costDetailReq.setUsingRateTextFromHis(item.getUsingRateTextFromHis());
            //给药途径名称
            costDetailReq.setUsingRateText(item.getUsePathwaysTextFromHis());

            //医保编码
            OrganDrugListDAO organDrugListDAO = getDAO(OrganDrugListDAO.class);
            List<OrganDrugList> list = organDrugListDAO.findByDrugIdAndOrganId(item.getDrugId(), recipe.getClinicOrgan());
            if (!list.isEmpty()) {
                costDetailReq.setMedicalDrugCode(list.get(0).getMedicalDrugCode());
                costDetailReq.setOrganDrugCode(list.get(0).getOrganDrugCode());
            }
            OrganDrugList organDrugList = organDrugDao.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(), item.getOrganDrugCode(), item.getDrugId());
            if (organDrugList == null) {
                costDetailReq.setDrcode(item.getOrganDrugCode());
            } else {
                costDetailReq.setDrcode(StringUtils.isNotEmpty(organDrugList.getRegulationDrugCode()) ? organDrugList.getRegulationDrugCode() : organDrugList.getOrganDrugCode());

            }
            costDetailReq.setDrname(item.getDrugName());
            Dictionary usingRateDic = null;
            Dictionary usePathwaysDic = null;
            try {
                usingRateDic = DictionaryController.instance().get("eh.cdr.dictionary.UsingRate");
                usePathwaysDic = DictionaryController.instance().get("eh.cdr.dictionary.UsePathways");
            } catch (ControllerException e) {
                LOGGER.error("uploadRecipeIndicators dic error.", e);
            }
            //药品频次名称
            if (null != usingRateDic) {
                costDetailReq.setFrequencyName(item.getUsingRateTextFromHis() != null ? item.getUsingRateTextFromHis() : usingRateDic.getText(item.getUsingRate()));
            }
            items.add(costDetailReq);
        }
        return items;
    }


    /**
     * 组装派药/配送完成接口
     *
     * @param recipeId
     * @return
     * @author zhangx
     * @create 2020-10-13 20:00
     **/
    private RegulationSendMedicineReq pakRegulationSendMedicineReq(Integer recipeId) {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
        RecipeExtendDAO recipeExtendDAO = getDAO(RecipeExtendDAO.class);
        RegulationSendMedicineReq req = null;
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);

        EmploymentService employmentService = AppContextHolder.getBean("basic.employmentService",EmploymentService.class);
        //EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);


        if (ValidateUtil.nullOrZeroInteger(recipeId)) {
            LOGGER.warn("pakRegulationSendMedicineReq  recipeId is null.");
            return null;
        }

        //处方单
        Recipe recipe = recipeDAO.get(recipeId);
        if (null == recipe) {
            LOGGER.warn("pakRegulationSendMedicineReq  recipe is null,recipeid={}", recipeId);
            return null;
        }
        //生成了订单的处方
        String orderCode = recipe.getOrderCode();
        if (!StringUtils.isEmpty(orderCode)) {

            RecipeOrder order = recipeOrderDAO.getByOrderCode(orderCode);
            if (null != recipe && order != null) {
                IHisServiceConfigService configService = AppDomainContext.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
                //获取所有监管平台机构列表
                List<ServiceConfigResponseTO> serviceConfigResponseTOS = configService.findAllRegulationOrgan();
                if (CollectionUtils.isEmpty(serviceConfigResponseTOS)) {
                    LOGGER.warn("pakRegulationSendMedicineReq  regulationOrganList is null.");
                    return null;
                }
                List<Integer> organList = serviceConfigResponseTOS.stream().map(ServiceConfigResponseTO::getOrganid).collect(Collectors.toList());
                if (!organList.contains(recipe.getClinicOrgan())) {
                    LOGGER.warn("pakRegulationSendMedicineReq organId={},没有关联监管平台", recipe.getClinicOrgan());
                    return null;
                }

                req = new RegulationSendMedicineReq();
                req.setRecipeId(recipe.getRecipeId());
                req.setGiveMode(recipe.getGiveMode());
                req.setRecipeCode(recipe.getRecipeCode());
                req.setRecipeType(recipe.getRecipeType());
                req.setBussSource(recipe.getBussSource());


                //门诊号
                req.setPatientID(recipe.getPatientID());
                //计费时间
                req.setPayDate(order.getPayTime());

                //患者Id
                req.setMpiid(recipe.getMpiid());
                //开方时间
                req.setCreateDate(recipe.getCreateDate());

                //优先取运营平台处方详情设置的发药药师，如果没有取机构默认发药药师，都没有就为空
                IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
                String doctorId = (String) configurationService.getConfiguration(recipe.getClinicOrgan(), "oragnDefaultDispensingApothecary");
                ApothecaryDTO apothecaryDTO = doctorClient.getGiveUser(recipe);
                //获取发药药师姓名  默认平台配置
                if (StringUtils.isNotEmpty(apothecaryDTO.getGiveUserName())) {
                    req.setDispensingCheckerName(apothecaryDTO.getGiveUserName());
                } else if (doctorId != null) {
                    //获取机构发药药师
                    DoctorDTO dispensingApothecary = doctorService.get(Integer.valueOf(doctorId));
                    req.setDispensingCheckerName(dispensingApothecary.getName());
                }

                //获取发药药师工号
                if (recipe.getChecker() != null) {
                    EmploymentDTO employment = employmentService.getPrimaryEmpByDoctorId(recipe.getChecker());
                    if (employment != null) {
                       req.setDispensingCheckerId(employment.getJobNumber());
                   }
               }

                //获取发药时间  订单表中进行获取
                req.setDispensingTime(order.getDispensingTime());


                PatientService patientService = BasicAPI.getService(PatientService.class);
                PatientDTO patientDTO = patientService.getPatientDTOByMpiId(recipe.getMpiid());
                if (patientDTO != null) {
                    req.setIdcardTypeCode("01");
                    req.setIdcardNo(patientDTO.getIdcard());
                    req.setName(patientDTO.getPatientName());
                    req.setGenderCode(patientDTO.getPatientSex());
                    req.setBirthdate(patientDTO.getBirthday());
                    req.setMobile(patientDTO.getMobile());
                }
                req.setVisitNo(String.valueOf(recipe.getClinicId()));
                req.setAccountNo(order.getTradeNo());
                req.setTotalFee(recipe.getTotalMoney() != null ? recipe.getTotalMoney().doubleValue() : 0);
                req.setIndividualPay(recipe.getActualPrice() != null ? recipe.getActualPrice().doubleValue() : 0);
                req.setChargeRefundCode(String.valueOf(recipe.getPayFlag()));
                req.setOrganId(String.valueOf(recipe.getClinicOrgan()));
                req.setOrgName(recipe.getOrganName());
                req.setRcdDatetime(new Date());
                req.setPayTypeCode("07"); //详见医疗费用分类代码表

                //处方药品明细
                List<RegulationCostDetailReq> items = pakRegulationCostDetailReq(recipe);
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

                //医保金额
                req.setFundAmount(order.getFundAmount());
                //自费金额
                req.setCashAmount(order.getCashAmount());

                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                if (recipeExtend != null) {
                    //获取电子病历,优先从电子病历取
                    EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
                    //从his返回的挂号序号
                    req.setRegisterNo(recipeExtend.getRegisterID());
                    req.setRegisterId(recipeExtend.getRegisterID());
                    //诊断编码
                    req.setOrganDiseaseId(recipe.getOrganDiseaseId());
                    //诊断名称
                    req.setOrganDiseaseName(recipe.getOrganDiseaseName());
                    //从监管平台上传保存的序号
                    req.setSuperviseRecipecode(recipeExtend.getSuperviseRecipecode());
                    //主诉
                    req.setMainDieaseDescribe(recipeExtend.getMainDieaseDescribe());
                    //现病史
                    req.setCurrentMedical(recipeExtend.getCurrentMedical());
                    //既往史
                    req.setHistroyMedical(recipeExtend.getHistroyMedical());
                }

                //开方医生信息
                req.setDoctor(getRegulationBusDocReq(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart()));
                //审方医生信息
                req.setCheckDoctor(getRegulationBusDocReq(recipe.getChecker(), null, null));

                //物流信息
                RegulationLogisticsReq LogisticsInfo = pakRegulationLogisticsReq(order);
                req.setLogisticsInfo(LogisticsInfo);

                //复诊id
                req.setBussID(recipe.getClinicId()!=null?recipe.getClinicId().toString():null);

            }

        }
        LOGGER.warn("pakRegulationSendMedicineReq  req={}",JSONUtils.toString(req));
        return req;
    }
}
