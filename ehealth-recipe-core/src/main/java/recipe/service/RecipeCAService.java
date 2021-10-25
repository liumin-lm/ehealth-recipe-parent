package recipe.service;

import ca.vo.CommonSignRequest;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.push.model.SmsInfoBean;
import com.ngari.base.push.service.ISmsPushService;
import com.ngari.his.ca.model.CaAccountRequestTO;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.his.regulation.entity.RegulationRecipeDetailIndicatorsReq;
import com.ngari.his.regulation.entity.RegulationRecipeIndicatorsReq;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.cqJgptBussData.AdditionalDiagnosis;
import recipe.bean.cqJgptBussData.Drug;
import recipe.bean.cqJgptBussData.RecipeDocSignatureXML;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.XstreamUtil;
import recipe.ca.vo.CaSignResultVo;
import recipe.caNew.AbstractCaProcessType;
import recipe.caNew.CaAfterProcessType;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.constant.CARecipeTypeConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.factoryManager.button.DrugStockBusinessService;
import recipe.manager.EmrRecipeManager;
import recipe.service.common.RecipeSignService;
import recipe.util.ByteUtils;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;
import recipe.util.RedisClient;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static ctd.persistence.DAOFactory.getDAO;

@RpcBean("recipeCAService")
public class RecipeCAService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeCAService.class);

    private static final Integer CA_OLD_TYPE = new Integer(0);

    private static final Integer CA_NEW_TYPE = new Integer(1);

    @Autowired
    private RedisClient redisClient;
    @Autowired
    private CaAfterProcessType caAfterProcessType;
    @Autowired
    private IConfigurationCenterUtilsService configService;

    private DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);

    private RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);

    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    @Autowired
    PatientService patientService;

    @Autowired
    RecipeDetailDAO recipeDetailDAO;

    @Autowired
    OrganDrugListDAO organDrugDao;
    @Autowired
    private CreatePdfFactory createPdfFactory;

    @Resource
    private DrugStockBusinessService drugStockBusinessService;

    @RpcService
    public CommonSignRequest packageCAFromRecipe(Integer recipeId, Integer doctorId, Boolean isDoctor) {
        LOGGER.info("packageCAFromRecipe recipeId：{},doctorId:{},isDoctor:{}", recipeId, doctorId, isDoctor);
        CommonSignRequest caRequest = new CommonSignRequest();
        Map<String, Object> esignMap = new HashMap<>();
        try {
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            if (null == recipe) {
                LOGGER.info("packageCAFromRecipe 当前处方{}信息为空！", recipe);
                return null;
            }
            //1.判断是药师还是医生组装ca请求数据
            //原来方法是通过isDoctor来判断是否是医生的，现在统一请求组装，设置CA类型（药师/医生）
            //2.首先组装易签保用的签名签章数据
            List<Recipedetail> details = detailDAO.findByRecipeId(recipeId);
            //1.判断是药师还是医生组装ca请求数据
            //原来方法是通过isDoctor来判断是否是医生的，现在统一请求组装，设置CA类型（药师/医生）
            caRequest.setOrganId(recipe.getClinicOrgan());
            caRequest.setDoctorId(doctorId);
            caRequest.setBussId(recipeId);
            caRequest.setBusstype(isDoctor ? CARecipeTypeConstant.CA_RECIPE_DOC : CARecipeTypeConstant.CA_RECIPE_PHA);
            //2.首先组装易签保用的签名签章数据
            esignMap.put("isDoctor", isDoctor);
            esignMap.put("checker", doctorId);

            if (isDoctor) {
                //医生的组装
                String fileName = "recipe_" + recipeId + ".pdf";
                recipe.setSignDate(DateTime.now().toDate());
                if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
                    //中药pdf参数
                    esignMap = RecipeServiceSub.createParamMapForChineseMedicine(recipe, details, fileName);
                } else {
                    esignMap = RecipeServiceSub.createParamMap(recipe, details, fileName);
                    esignMap.put("recipeImgId", recipeId);
                }
            } else {
                //药师的组装
                esignMap.put("fileName", "recipecheck_" + recipeId + ".pdf");
                esignMap.put("recipeSignFileId", recipe.getSignFile());
                if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
                    esignMap.put("templateType", "tcm");
                } else {
                    esignMap.put("templateType", "wm");
                }
                // 添加机构id
                esignMap.put("organId", recipe.getClinicOrgan());
            }

            caRequest.setEsignMap(esignMap);

            //3.在组装通用的签名签章数据
            //获取签章pdf数据。签名原文
            CaSealRequestTO requestSealTO = createPdfFactory.queryPdfByte(recipeId);
            if (null == requestSealTO) {
                LOGGER.warn("当前CA组装【pdf】和【签章数据】信息返回空，中断当前CA");
                return null;
            }

            /*****添加pdf覆盖逻辑*****/
            /*之前设置在CA组装请求的时候将处方pdf更新上去，现在将生成的时机放置在CA结果回调上*/
            RecipeServiceEsignExt.updateInitRecipePDF(isDoctor, recipe, requestSealTO.getPdfBase64Str());
            //4.最后组装业务单独请求的扩展数据
            /*** 这个taskCode是SDK签名的时候的签名原文，之后对接的时候需要根据业务组装成对应业务的签名对象****/
            //如果是重庆监管平台，按照固定要求格式上传签名原文
            caRequest.setBussData(JSONUtils.toString(recipe));
            if (RecipeServiceSub.isCQOrgan(recipe.getClinicOrgan())) {
                caRequest.setBussData(getBussDataFromCQ(recipeId, isDoctor));
            }
            caRequest.setExtendMap(obtainExtendMap(recipe));
        } catch (Exception e) {
            LOGGER.warn("当前处方CA数据组装失败返回空，{}", e);
        }
        LOGGER.info("packageCAFromRecipe caRequest：{}", JSONUtils.toString(caRequest));
        return caRequest;
    }

    /**
     * 获取ExtendMap
     *
     * @param recipe
     * @return
     */
    private Map<String, Object> obtainExtendMap(Recipe recipe) {
        Map<String, Object> extendMap = new HashMap<>();
        try {
            extendMap.put("recipeBean", JSONUtils.toString(recipe));
            extendMap.put("detailBeanList", JSONUtils.toString(recipeDetailDAO.findByRecipeId(recipe.getRecipeId())));
            extendMap.put("caMixData", JSONUtils.toString(obtainCaMixData(recipe.getRecipeId())));

        } catch (Exception e) {
            LOGGER.info("obtainExtendMap error ", e);
            e.printStackTrace();
        }
        return extendMap;
    }

    private String getBussDataFromCQ(Integer recipeId, Boolean isDoctor) {
        RecipeBean recipeBean = recipeService.getByRecipeId(recipeId);
        if (null == recipeBean) {
            LOGGER.warn("当前处方{}信息为空", recipeId);
            return null;
        }
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
        PatientDTO patientDTO = patientService.getPatientBeanByMpiId(recipeBean.getMpiid());

        RecipeDocSignatureXML xml = new RecipeDocSignatureXML();
        List<AdditionalDiagnosis> DiagnosisList = new ArrayList();
        xml.setMedicalRecordNo(recipeExtend.getRegisterID());
        LOGGER.info("Q310***** 开方xml挂号序号" + recipeExtend.getRegisterID());
        xml.setDoctorCode(recipeBean.getDoctor() + "");
        xml.setDoctorName(recipeBean.getDoctorName());
        //如果是药师，添加药师信息
        if (!isDoctor) {
            xml.setTrialPharmCode(recipeBean.getChecker() + "");
            xml.setTrialPharmName(recipeBean.getCheckerText());
        }
        xml.setPatientName(recipeBean.getPatientName());
        xml.setPatientSFZH(patientDTO.getIdcard());
        String organDiseaseId = "";
        if (null != recipeBean.getOrganDiseaseId()) {
            organDiseaseId = recipeBean.getOrganDiseaseId().replaceAll(ByteUtils.SEMI_COLON_EN, "|");
        }
        String diagnosisName = "";

        if (null != recipeBean.getOrganDiseaseId()) {
            diagnosisName = recipeBean.getOrganDiseaseName().replaceAll(ByteUtils.SEMI_COLON_EN, "|");
        }
        AdditionalDiagnosis add = new AdditionalDiagnosis();
        add.setDiagnosisCode(organDiseaseId);
        add.setDiagnosisName(diagnosisName);
        DiagnosisList.add(add);
        xml.setDiagnosisList(DiagnosisList);

        try {
            List<Drug> drugs = new ArrayList<>();
            for (Recipedetail recipeDetail : recipedetails) {
                try {
                    OrganDrugList organDrugList = organDrugDao.getByOrganIdAndOrganDrugCodeAndDrugId(recipeBean.getClinicOrgan(), recipeDetail.getOrganDrugCode(), recipeDetail.getDrugId());

                    String drCode = "";
                    if (organDrugList == null) {
                        drCode = recipeDetail.getOrganDrugCode();
                    } else {
                        drCode = StringUtils.isNotEmpty(organDrugList.getRegulationDrugCode()) ? organDrugList.getRegulationDrugCode() : organDrugList.getOrganDrugCode();
                    }

                    Drug drug = new Drug();
                    drug.setHospitalDrugCode(drCode);
                    drug.setDrugCommonName(organDrugList.getDrugName());
                    drug.setPrice(organDrugList.getSalePrice().setScale(4).toString());
                    drug.setDeliverNumUnit(organDrugList.getUnit());
                    drug.setMoney(new BigDecimal(recipeDetail.getUseTotalDose()).setScale(4).toString());
                    drugs.add(drug);

                } catch (Exception e) {
                }
            }
            xml.setDrugList(drugs);
        } catch (Exception e) {
        }
        String xmlStr = XstreamUtil.objectToXml(xml);
        if (StringUtils.isNotEmpty(xmlStr)) {
            xmlStr = xmlStr.replaceAll("\n", "").replaceAll(" ", "");
        }
        LOGGER.info("RecipeCAService getBussDataFromCQ response:{}", xmlStr);
        return xmlStr;
    }

    /**
     * 获取重庆签名原文（需跟重庆监管平台数据格式保持严格一致）
     * null时不要标签名 ""需要标签名[时间紧急，先写死标签]
     *
     * @param recipeId
     * @param isDoctor
     * @return
     * @Author liumin
     */
    private String getBussDataFromCQ2(Integer recipeId, Boolean isDoctor) {
        RecipeBean recipeBean = recipeService.getByRecipeId(recipeId);
        if (null == recipeBean) {
            LOGGER.warn("当前处方{}信息为空", recipeId);
            return null;
        }
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
        PatientDTO patientDTO = patientService.getPatientBeanByMpiId(recipeBean.getMpiid());

        StringBuffer cqCABussData = new StringBuffer();
        cqCABussData.append("<CA>");
        LOGGER.info("Q310***** 开方xml挂号序号" + recipeExtend.getRegisterID());
        if (null != recipeExtend.getRegisterID()) {
            cqCABussData.append("<MedicalRecordNo>" + recipeExtend.getRegisterID() + "</MedicalRecordNo>");
        }
        if (null != recipeExtend.getRegisterID()) {

        }
        if (null != recipeBean.getDoctor()) {
            cqCABussData.append("<DoctorCode>" + recipeBean.getDoctor() + "</DoctorCode>");
        }
        if (null != recipeBean.getDoctorName()) {
            cqCABussData.append("<DoctorName>" + recipeBean.getDoctorName() + "</DoctorName>");
        }
        //如果是药师，添加药师信息
        if (!isDoctor) {
            if (null != recipeBean.getChecker()) {
                cqCABussData.append("<TrialPharmCode>" + recipeBean.getChecker() + "</TrialPharmCode>");
            }
            if (null != recipeBean.getCheckerText()) {
                cqCABussData.append("<TrialPharmName>" + recipeBean.getCheckerText() + "</TrialPharmName>");
            }
        }
        if (null != recipeBean.getPatientName()) {
            cqCABussData.append("<PatientName>" + recipeBean.getPatientName() + "</PatientName>");
        }
        if (null != patientDTO.getIdcard()) {
            cqCABussData.append("<PatientSFZH>" + patientDTO.getIdcard() + "</PatientSFZH>");
        }

        cqCABussData.append("<DiagnosisList>");
        cqCABussData.append("<additionaldiagnosis>");
        String organDiseaseId = "";
        if (null != recipeBean.getOrganDiseaseId()) {
            organDiseaseId = recipeBean.getOrganDiseaseId().replaceAll(ByteUtils.SEMI_COLON_EN, "|");
        }
        if (null != organDiseaseId) {
            cqCABussData.append("<diagnosisCode>" + organDiseaseId + "</diagnosisCode>");
        }
        String diagnosisName = "";

        if (null != recipeBean.getOrganDiseaseId()) {
            diagnosisName = recipeBean.getOrganDiseaseName().replaceAll(ByteUtils.SEMI_COLON_EN, "|");
        }
        if (null != diagnosisName) {
            cqCABussData.append("<diagnosisName>" + diagnosisName + "</diagnosisName>");
        }
        cqCABussData.append("</additionaldiagnosis>");
        cqCABussData.append("</DiagnosisList>");

        cqCABussData.append("<DrugList>");
        recipedetails.forEach(recipeDetail -> {
            OrganDrugList organDrugList = organDrugDao.getByOrganIdAndOrganDrugCodeAndDrugId(recipeBean.getClinicOrgan(), recipeDetail.getOrganDrugCode(), recipeDetail.getDrugId());

            cqCABussData.append("<drug>");
            String drCode = "";
            if (organDrugList == null) {
                drCode = recipeDetail.getOrganDrugCode();
            } else {
                drCode = StringUtils.isNotEmpty(organDrugList.getRegulationDrugCode()) ? organDrugList.getRegulationDrugCode() : organDrugList.getOrganDrugCode();
                if (null != drCode) {
                    cqCABussData.append("<hospitalDrugCode>" + drCode + "</hospitalDrugCode>");
                }
                if (null != organDrugList.getDrugName()) {
                    cqCABussData.append("<drugCommonName>" + organDrugList.getDrugName() + "</drugCommonName>");
                }
                if (null != organDrugList.getSalePrice().setScale(4).toString()) {
                    cqCABussData.append("<price>" + organDrugList.getSalePrice().setScale(4).toString() + "</price>");
                }
                if (null != organDrugList.getUnit()) {
                    cqCABussData.append("<deliverNumUnit>" + organDrugList.getUnit() + "</deliverNumUnit>");
                }
            }
            if (null != new BigDecimal(recipeDetail.getUseTotalDose()).setScale(4).toString()) {
                cqCABussData.append("<money>" + new BigDecimal(recipeDetail.getUseTotalDose()).setScale(4).toString() + "</money>");
            }
            cqCABussData.append("</drug>");
        });
        cqCABussData.append("</DrugList>");
        cqCABussData.append("</CA>");
        return cqCABussData.toString();
    }


    public CaAccountRequestTO packageCAFromBus(Integer recipeId) {
        RecipeBean recipeBean = recipeService.getByRecipeId(recipeId);
        if (null == recipeBean) {
            LOGGER.warn("当前处方{}信息为空", recipeId);
            return null;
        }
        RecipeDetailDAO recipeDetailDAO = getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);

        List<RecipeDetailBean> detailBeanList = ObjectCopyUtils.convert(recipedetails, RecipeDetailBean.class);

        CaAccountRequestTO caAccountRequestTO = new CaAccountRequestTO();
        caAccountRequestTO.setOrganId(recipeBean.getClinicOrgan());
        /** 当前没有设置CA签名中的业务端签名对象，原计划根据签名医生的类型设置请求【BusType】***/
        caAccountRequestTO.setBusType(null == recipeBean.getChecker() ? 4 : 5);
        caAccountRequestTO.setRegulationRecipeIndicatorsReq(Arrays.asList(getCATaskRecipeReq(recipeBean, detailBeanList)));

        //caAccountRequestTO.setSignOriginal(Arrays.asList(getCATaskRecipeReq(recipeBean, detailBeanList)));
        return caAccountRequestTO;
    }

    public RegulationRecipeIndicatorsReq obtainCaMixData(Integer recipeId) {
        RecipeBean recipeBean = recipeService.getByRecipeId(recipeId);
        if (null == recipeBean) {
            LOGGER.warn("当前处方{}信息为空", recipeId);
            return null;
        }
        RecipeDetailDAO recipeDetailDAO = getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);

        List<RecipeDetailBean> detailBeanList = ObjectCopyUtils.convert(recipedetails, RecipeDetailBean.class);

        return getCATaskRecipeReq(recipeBean, detailBeanList);
    }

    public RegulationRecipeIndicatorsReq getCATaskRecipeReq(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList) {
        EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
        OrganDrugListDAO organDrugDao = DAOFactory.getDAO(OrganDrugListDAO.class);
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        PatientService patientService = BasicAPI.getService(PatientService.class);
        RegulationRecipeIndicatorsReq request = new RegulationRecipeIndicatorsReq();

        try {
            String registerId = "";
            Integer recipeId = recipeBean.getRecipeId();
            RecipeExtendBean extend = recipeBean.getRecipeExtend();
            //调整电子病历扩展表改造逻辑
            RecipeExtend recipeExtend = null;
            if (null != extend) {
                recipeExtend = ObjectCopyUtils.convert(extend, RecipeExtend.class);
            } else {
                if (null != recipeId) {
                    recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
                }
            }
            if (null != recipeExtend) {
                Recipe recipeNew = new Recipe();
                ctd.util.BeanUtils.copy(recipeBean, recipeNew);
                EmrRecipeManager.getMedicalInfo(recipeNew, recipeExtend);
                recipeBean.setOrganDiseaseName(recipeNew.getOrganDiseaseName());
                recipeBean.setOrganDiseaseId(recipeNew.getOrganDiseaseId());
                registerId = recipeExtend.getRegisterID();
                request.setMainDieaseDescribe(recipeExtend.getMainDieaseDescribe());
            } else {
                LOGGER.warn("当前处方{}电子病历改造后，没有正常的数据填充", recipeId);
            }
            //date  20200820
            //当处方id为空时设置临时的处方id，产生签名的id在和处方关联
            request.setRecipeID(UUID.randomUUID().toString());

            request.setStartDate(null != recipeBean.getSignDate() ? recipeBean.getSignDate() : new Date());
            request.setEffectivePeriod(3);

            if (StringUtils.isEmpty(registerId) && recipeBean.getClinicId() != null && recipeBean.getBussSource() != null) {
                //在线复诊
                if (new Integer(2).equals(recipeBean.getBussSource())) {
                    IRevisitExService exService = RevisitAPI.getService(IRevisitExService.class);
                    RevisitExDTO consultExDTO = exService.getByConsultId(recipeBean.getClinicId());
                    if (null != consultExDTO) {
                        registerId = consultExDTO.getRegisterNo();
                    }
                }

            }

            request.setRegisterId(registerId);
            request.setRegisterNo(registerId);

            if (null != recipeBean.getDoctor()) {
                DoctorDTO doctorDTO = doctorService.get(recipeBean.getDoctor());
                request.setDoctorId(recipeBean.getDoctor().toString());
                request.setDoctorName(doctorDTO.getName());
                EmploymentDTO employment = iEmploymentService.getPrimaryEmpByDoctorId(recipeBean.getDoctor());
                if (employment != null) {
                    request.setDoctorNo(employment.getJobNumber());
                }
            } else {
                LOGGER.warn("getTaskCode2 RecipeBean doctor is null.");
            }

            if (recipeBean.getChecker() != null) {
                DoctorDTO doctorDTO = doctorService.get(recipeBean.getChecker());
                if (null == doctorDTO) {
                    LOGGER.warn("getTaskCode2 RecipeBean checker is null. recipe.checker={}", recipeBean.getChecker());
                } else {
                    request.setAuditDoctorCertID(doctorDTO.getIdNumber());
                    request.setAuditDoctor(doctorDTO.getName());
                    request.setAuditDoctorId(recipeBean.getChecker().toString());
                    request.setAuditProTitle(doctorDTO.getProTitle());
                    //工号：医生取开方机构的工号，药师取第一职业点的工号
                    EmploymentDTO employment = iEmploymentService.getPrimaryEmpByDoctorId(recipeBean.getChecker());
                    if (employment != null) {
                        request.setAuditDoctorNo(employment.getJobNumber());
                    }
                }
            } else {
                LOGGER.warn("getTaskCode2 RecipeBean checker is null");
            }
            if (StringUtils.isNotEmpty(recipeBean.getOrganDiseaseId())) {
                request.setIcdCode(recipeBean.getOrganDiseaseId().replaceAll(ByteUtils.SEMI_COLON_EN, "|"));
            }
            request.setIcdName(recipeBean.getOrganDiseaseName());

            // 患者信息
            PatientDTO patientDTO = patientService.get(recipeBean.getMpiid());
            if (null == patientDTO) {
                LOGGER.warn("getTaskCode2 patient is null. recipe.patient={}", recipeBean.getMpiid());
            } else {
                request.setMpiId(patientDTO.getMpiId());
                String organDiseaseName = recipeBean.getOrganDiseaseName().replaceAll(ByteUtils.SEMI_COLON_EN, "|");
                request.setOriginalDiagnosis(organDiseaseName);
                request.setPatientCardType(LocalStringUtil.toString(patientDTO.getCertificateType()));
                request.setPatientCertID(LocalStringUtil.toString(patientDTO.getCertificate()));
                request.setPatientName(patientDTO.getPatientName());
                request.setNation(patientDTO.getNation());
                request.setMobile(LocalStringUtil.toString(patientDTO.getMobile()));
                request.setSex(patientDTO.getPatientSex());
                request.setAge(DateConversion.calculateAge(patientDTO.getBirthday()));
                request.setBirthDay(patientDTO.getBirthday());
                //陪诊人信息
                request.setGuardianName(patientDTO.getGuardianName());
                request.setGuardianCertID(patientDTO.getGuardianCertificate());
                request.setGuardianMobile(patientDTO.getMobile());
            }

            List<RegulationRecipeDetailIndicatorsReq> list = new ArrayList(detailBeanList.size());
            for (RecipeDetailBean detail : detailBeanList) {
                RegulationRecipeDetailIndicatorsReq reqDetail = new RegulationRecipeDetailIndicatorsReq();
                OrganDrugList organDrugList = organDrugDao.getByOrganIdAndOrganDrugCodeAndDrugId(recipeBean.getClinicOrgan(), detail.getOrganDrugCode(), detail.getDrugId());
                if (organDrugList == null) {
                    reqDetail.setDrcode(detail.getOrganDrugCode());
                } else {
                    reqDetail.setDrcode(StringUtils.isNotEmpty(organDrugList.getRegulationDrugCode()) ? organDrugList.getRegulationDrugCode() : organDrugList.getOrganDrugCode());
                    reqDetail.setLicenseNumber(organDrugList.getLicenseNumber());
                    reqDetail.setDosageFormCode(organDrugList.getDrugFormCode());
                    reqDetail.setMedicalDrugCode(organDrugList.getMedicalDrugCode());
                    reqDetail.setMedicalDrugFormCode(organDrugList.getMedicalDrugFormCode());
                    reqDetail.setDrugFormCode(organDrugList.getDrugFormCode());
                    //处方保存之前少三个字段
                    reqDetail.setPackUnit(StringUtils.isEmpty(detail.getDrugUnit()) ? organDrugList.getUnit() : detail.getDrugUnit());
                    //设置药品价格
                    BigDecimal price = organDrugList.getSalePrice();
                    //单价
                    reqDetail.setPrice(price);
                    //保留3位小数
                    BigDecimal drugCost = price.multiply(new BigDecimal(detail.getUseTotalDose())).divide(BigDecimal.ONE, 3, RoundingMode.UP);
                    //总价
                    reqDetail.setTotalPrice(drugCost);
                }

                reqDetail.setDrmodel(detail.getDrugSpec());
                reqDetail.setPack(detail.getPack());
                reqDetail.setDrname(detail.getDrugName());
                reqDetail.setDrunit(detail.getUseDoseUnit());
                //date 20200821
                //添加CA同步字段
                reqDetail.setUseDaysB(null != detail.getUseDays() ? detail.getUseDays().toString() : detail.getUseDaysB());
                reqDetail.setDosageTotal(null != detail.getUseTotalDose() ? detail.getUseTotalDose().toString() : null);
                ctd.dictionary.Dictionary usingRateDic = null;
                ctd.dictionary.Dictionary usePathwaysDic = null;
                try {
                    usingRateDic = DictionaryController.instance().get("eh.cdr.dictionary.UsingRate");
                    usePathwaysDic = DictionaryController.instance().get("eh.cdr.dictionary.UsePathways");
                } catch (ControllerException e) {
                    LOGGER.error("search dic error.", e);
                }
                if (null != usePathwaysDic) {
                    reqDetail.setAdmissionName(detail.getUsePathwaysTextFromHis() != null ? detail.getUsePathwaysTextFromHis() : usePathwaysDic.getText(detail.getUsePathways()));
                }
                if (null != usingRateDic) {
                    reqDetail.setFrequencyName(detail.getUsingRateTextFromHis() != null ? detail.getUsingRateTextFromHis() : usingRateDic.getText(detail.getUsingRate()));
                }
                reqDetail.setDosage(detail.getUseDose() == null ? detail.getUseDoseStr() : String.valueOf(detail.getUseDose()));
                list.add(reqDetail);
            }
            request.setOrderList(list);
        } catch (Exception e) {
            LOGGER.error("当前处方{}CA数据组装异常", recipeBean.getRecipeId(), e);
            throw new DAOException(609, "当前CA数据组装异常,返回空");
        }
        LOGGER.info("getCATaskRecipeReq request:{}", JSONUtils.toString(request));
        return request;
    }

    @RpcService
    public Map<String, Object> doSignRecipeCABefore(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList, int continueFlag) {
        if (null == recipeBean) {
            LOGGER.warn("当前签名处方信息为空！");
        }
        //当前方法提供CA改造后的签名方法
        //改造点：1.根据CA配置项，前置=》流程：新增；后置=》流程：新增+推his
        //2.新增后处方状态为：医生签名中

        //先定义一个CA配置
        String caStatus = "before";

        LOGGER.info("doSignRecipeCABefore param: recipeBean={} detailBean={}", JSONUtils.toString(recipeBean), JSONUtils.toString(detailBeanList));
        //将密码放到redis中
        redisClient.set("caPassword", recipeBean.getCaPassword());
        Map<String, Object> rMap = new HashMap<String, Object>();
        rMap.put("signResult", true);
        try {
            recipeBean.setDistributionFlag(continueFlag);
            //上海肺科个性化处理--智能审方重要警示弹窗处理
            recipeService.doforShangHaiFeiKe(recipeBean, detailBeanList);
            //第一步暂存处方（处方状态未签名）
            recipeService.doSignRecipeSave(recipeBean, detailBeanList);

            //第二步预校验
            if (continueFlag == 0) {
                //his处方预检查
                RecipeSignService recipeSignService = AppContextHolder.getBean("eh.recipeSignService", RecipeSignService.class);
                boolean b = recipeSignService.hisRecipeCheck(rMap, recipeBean);
                if (!b) {
                    rMap.put("signResult", false);
                    rMap.put("recipeId", recipeBean.getRecipeId());
                    rMap.put("errorFlag", true);
                    return rMap;
                }
            }
            //第三步校验库存
            if (continueFlag == 0 || continueFlag == 4) {
                rMap = drugStockBusinessService.doSignRecipeCheckAndGetGiveMode(recipeBean);
                Boolean signResult = Boolean.valueOf(rMap.get("signResult").toString());
                if (signResult != null && false == signResult) {
                    return rMap;
                }
            }
            //跳转所需要的复诊信息
            Integer consultId = recipeBean.getClinicId();
            Integer bussSource = recipeBean.getBussSource();
            if (consultId != null) {
                if (null != rMap && null == rMap.get("consultId")) {
                    rMap.put("consultId", consultId);
                    rMap.put("bussSource", bussSource);
                }
            }
            Integer CANewOldWay = CA_OLD_TYPE;
            Object caProcessType = configService.getConfiguration(recipeBean.getClinicOrgan(), "CAProcessType");
            if (null != caProcessType) {
                CANewOldWay = Integer.parseInt(caProcessType.toString());
            }
            //触发CA前置操作
            if (CA_NEW_TYPE.equals(CANewOldWay)) {
                AbstractCaProcessType.getCaProcessFactory(recipeBean.getClinicOrgan()).signCABeforeRecipeFunction(recipeBean, detailBeanList);
            } else {
                //老版默认走后置的逻辑，直接将处方推his
                caAfterProcessType.signCABeforeRecipeFunction(recipeBean, detailBeanList);
            }

        } catch (Exception e) {
            LOGGER.error("doSignRecipeCABefore error", e);
            throw new DAOException(recipe.constant.ErrorCode.SERVICE_ERROR, e.getMessage());
        }

        rMap.put("signResult", true);
        rMap.put("recipeId", recipeBean.getRecipeId());
        rMap.put("consultId", recipeBean.getClinicId());
        rMap.put("errorFlag", false);
        rMap.put("canContinueFlag", "0");
        LOGGER.info("doSignRecipeCABefore execute ok! rMap:" + JSONUtils.toString(rMap));
        return rMap;
    }

    @RpcService
    //这里因为是签名的回调函数，前端暂时不捕捉回调的返 回，就算捕捉了是否要添加交互
    public void signRecipeCAAfterCallBack(CaSignResultVo resultVo) {
        //当前方法提供CA改造后的签名成功后的回调方法
        //改造点：1.根据CA配置项，前置=》流程：推his+保存ca结果和相关数据；后置=》保存ca结果和相关数据+处方下流：
        //2.新增后处方状态为：医生签名中

        //ca完成签名签章后，将和返回的结果给平台
        //平台根据结果设置处方业务的跳转
        if (null == resultVo) {
            LOGGER.warn("signRecipeCAAfterCallBack 当期医生ca签名异步调用接口返回参数为空，无法设置相关信息");
            return;
        }
        LOGGER.info("signRecipeCAAfterCallBack 当前ca异步接口返回：{}", JSONUtils.toString(resultVo));
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = getDAO(RecipeDetailDAO.class);
        RecipeLogDAO recipeLogDAO = getDAO(RecipeLogDAO.class);

        Integer recipeId = resultVo.getRecipeId();
        if (null == recipeId) {
            LOGGER.warn("signRecipeCAAfterCallBack 当前CA业务端id为空，不能进行业务流程");
            return;
        }
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        RecipeBean recipeBean = recipeService.getByRecipeId(recipeId);
        List<Recipedetail> details = recipeDetailDAO.findByRecipeId(recipeId);
        List<RecipeDetailBean> detailBeanList = ObjectCopyUtils.convert(details, RecipeDetailBean.class);

        RecipeResultBean result = new RecipeResultBean();

        Integer organId = recipe.getClinicOrgan();
        DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getDoctor());

        try {
            String fileId = null;
            result.setMsg(resultVo.getMsg());
            if (resultVo != null && new Integer(200).equals(resultVo.getCode())) {
                result.setCode(RecipeResultBean.SUCCESS);
                /****处方保存相关签名信息****/
                //保存签名值、时间戳、电子签章文件
                RecipeServiceEsignExt.saveSignRecipePDF(resultVo.getPdfBase64(), recipeId, null, resultVo.getSignCADate(), resultVo.getSignRecipeCode(), true, fileId);
                resultVo.setFileId(fileId);
            } else {
                ISmsPushService smsPushService = AppContextHolder.getBean("eh.smsPushService", ISmsPushService.class);
                SmsInfoBean smsInfo = new SmsInfoBean();
                smsInfo.setBusId(0);
                smsInfo.setOrganId(0);
                smsInfo.setBusType("DocSignNotify");
                smsInfo.setSmsType("DocSignNotify");
                smsInfo.setExtendValue(doctorDTO.getUrt() + "|" + recipeId + "|" + doctorDTO.getLoginId());
                smsPushService.pushMsgData2OnsExtendValue(smsInfo);
                result.setCode(RecipeResultBean.FAIL);
            }
        } catch (Exception e) {
            LOGGER.error("signRecipeCAAfterCallBack 标准化CA签章报错 recipeId={} ,doctor={} ,e==============", recipeId, recipe.getDoctor(), e);
        }

        //首先判断当前ca是否是有结束结果的
        if (-1 == resultVo.getResultCode()) {
            LOGGER.info("signRecipeCAAfterCallBack 当期处方{}医生ca签名异步调用接口返回：未触发处方业务结果", recipeId);
            return;
        }

        //重试签名，首先设置处方的状态为签名中，根据签名的结果
        Integer code = result.getCode();
        String msg = result.getMsg();
        try {
            if (RecipeResultBean.FAIL == code) {
                //说明处方签名失败
                LOGGER.info("signRecipeCAAfterCallBack 当前签名处方{}签名失败！", recipeId);
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.SIGN_ERROR_CODE_DOC, null);
                recipeLogDAO.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), msg);
                return;
            } else {
                //说明处方签名成功，记录日志，走签名成功逻辑
                LOGGER.info("signRecipeCAAfterCallBack 当前签名处方{}签名成功！", recipeId);
                //更新审方checkFlag为待审核
                Map<String, Object> attrMap = Maps.newHashMap();
                attrMap.put("checkFlag", 0);
                recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), attrMap);
                LOGGER.info("checkFlag {} 更新为待审核", recipe.getRecipeId());
                recipeLogDAO.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "当前签名处方签名成功");
            }
        } catch (Exception e) {
            LOGGER.error("signRecipeCAAfterCallBack 签名服务或者发送卡片异常. ", e);
        }

        //设置处方的状态，如果失败不走下面逻辑
        /**************/
        //触发CA操作
        //兼容新老版本,根据配置项判断CA的新老流程走向
        Integer CANewOldWay = CA_OLD_TYPE;
        Object caProcessType = configService.getConfiguration(organId, "CAProcessType");
        if (null != caProcessType) {
            CANewOldWay = Integer.parseInt(caProcessType.toString());
        }
        if (CA_NEW_TYPE.equals(CANewOldWay)) {
            AbstractCaProcessType.getCaProcessFactory(recipeBean.getClinicOrgan()).signCAAfterRecipeCallBackFunction(recipeBean, detailBeanList);
        } else {
            //老版默认走后置的逻辑，直接将处方向下流
            caAfterProcessType.signCAAfterRecipeCallBackFunction(recipeBean, detailBeanList);
        }
    }

    @RpcService
    public void signRecipeCAInterrupt(Integer recipeId) {
        //首先判断处方的装填是不是可以设置成需要重新中断的
        //暂时不用加判断筛选是否可以设置【失败】
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            LOGGER.warn("当前处方{}不存在", recipeId);
            return;
        }
        //将处方设置成医生签名失败
        Integer beforeStatus = recipe.getStatus();
        if (RecipeStatusConstant.UNSIGN != recipe.getStatus()) {
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("status", RecipeStatusConstant.UNSIGN));
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), beforeStatus, RecipeStatusConstant.UNSIGN, "签名失败，设医生未签名！");
        }
    }

    @RpcService
    public void checkRecipeCAInterrupt(Integer recipeId) {
        //首先判断处方的装填是不是可以设置成需要重新中断的
        //暂时不用加判断筛选是否可以设置【失败】
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            LOGGER.warn("当前处方{}不存在", recipeId);
            return;
        }
        //将处方设置成药师签名失败
        Integer beforeStatus = recipe.getStatus();
        if (RecipeStatusConstant.SIGN_NO_CODE_PHA != recipe.getStatus()) {
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("status", RecipeStatusConstant.SIGN_NO_CODE_PHA));
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), beforeStatus, RecipeStatusConstant.SIGN_NO_CODE_PHA, "签名失败，设置药师未签名！");
        }
    }

    @RpcService
    //医生签名失败，操作重新签名设置处方状态【医生签名中】
    public void signRecipeCAAgain(Integer recipeId) {
        //签名失败后，重新进行签名，先将处方重新设置成【医生签名中】
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            LOGGER.warn("当前处方{}不存在", recipeId);
            return;
        }
        //将处方设置成【医生签名中】
        Integer beforeStatus = recipe.getStatus();
        if (RecipeStatusConstant.SIGN_ING_CODE_DOC != recipe.getStatus()) {
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("status", RecipeStatusConstant.SIGN_ING_CODE_DOC));
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), beforeStatus, RecipeStatusConstant.SIGN_ING_CODE_DOC, "重新签名，医生签名中！");
        }
    }


    @RpcService
    //药师CA操作重新签名设置处方状态
    public void checkRecipeCAAgain(Integer recipeId) {
        //签名失败后，重新进行签名，先将处方重新设置成【药师签名中】
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            LOGGER.warn("当前处方{}不存在", recipeId);
            return;
        }
        //将处方设置成【药师签名中】
        Integer beforeStatus = recipe.getStatus();
        if (RecipeStatusConstant.SIGN_ING_CODE_PHA != recipe.getStatus()) {
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("status", RecipeStatusConstant.SIGN_ING_CODE_PHA));
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), beforeStatus, RecipeStatusConstant.SIGN_ING_CODE_PHA, "重新签名，药师签名中！");
        }
    }

    /**
     * 测试类
     *
     * @param recipeId
     */
    @RpcService
    public void testGetBussDataFromCQ(Integer recipeId, boolean isDoctor) {
        getBussDataFromCQ(recipeId, isDoctor);
    }
}