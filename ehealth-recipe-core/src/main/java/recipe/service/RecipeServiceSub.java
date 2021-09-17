package recipe.service;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.operationrecords.model.OperationRecordsBean;
import com.ngari.base.operationrecords.service.IOperationRecordsService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.serviceconfig.mode.ServiceConfigResponseTO;
import com.ngari.base.serviceconfig.service.IHisServiceConfigService;
import com.ngari.common.dto.RecipeTagMsgBean;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.ConsultBean;
import com.ngari.consult.common.service.IConsultService;
import com.ngari.follow.service.IRelationLabelService;
import com.ngari.follow.service.IRelationPatientService;
import com.ngari.follow.utils.ObjectCopyUtil;
import com.ngari.follow.vo.RelationDoctorVO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.his.recipe.mode.RecipeDetailTO;
import com.ngari.home.asyn.model.BussCancelEvent;
import com.ngari.home.asyn.service.IAsynDoBussService;
import com.ngari.message.api.MessageAPI;
import com.ngari.message.api.service.ConsultMessageService;
import com.ngari.message.api.service.INetworkclinicMsgService;
import com.ngari.message.api.service.IRevisitMessageService;
import com.ngari.patient.dto.*;
import com.ngari.patient.service.*;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.RecipeAPI;
import com.ngari.recipe.basic.ds.PatientVO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.dto.AttachSealPicDTO;
import com.ngari.recipe.dto.GroupRecipeConfDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.constant.RecipeDistributionFlagEnum;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.recipe.service.IRecipeService;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import com.ngari.revisit.common.service.IRevisitService;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.schema.exception.ValidateException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.FileAuth;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcService;
import eh.recipeaudit.api.IAuditMedicinesService;
import eh.recipeaudit.api.IRecipeAuditService;
import eh.recipeaudit.api.IRecipeCheckService;
import eh.recipeaudit.model.AuditMedicineIssueBean;
import eh.recipeaudit.model.AuditMedicinesBean;
import eh.recipeaudit.model.RecipeCheckBean;
import eh.recipeaudit.util.RecipeAuditAPI;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.audit.bean.PAWebRecipeDanger;
import recipe.audit.service.PrescriptionService;
import recipe.bean.DrugEnterpriseResult;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.RecipeValidateUtil;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.constant.*;
import recipe.dao.*;
import recipe.drugsenterprise.AldyfRemoteService;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.givemode.business.GiveModeFactory;
import recipe.givemode.business.IGiveModeBase;
import recipe.hisservice.HisMqRequestInit;
import recipe.hisservice.RecipeToHisMqService;
import recipe.manager.*;
import recipe.purchase.PurchaseService;
import recipe.service.common.RecipeCacheService;
import recipe.service.recipecancel.RecipeCancelService;
import recipe.thread.PushRecipeToRegulationCallable;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 供recipeService调用
 *
 * @author liuya
 */
@Service
public class RecipeServiceSub {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeServiceSub.class);

    private static final String UNSIGN = "unsign";

    private static final String UNCHECK = "uncheck";

    private static SignManager signManager = AppContextHolder.getBean("signManager", SignManager.class);

    private static HisRecipeManager hisRecipeManager = AppContextHolder.getBean("hisRecipeManager", HisRecipeManager.class);

    private static GroupRecipeManager groupRecipeManager = AppContextHolder.getBean("groupRecipeManager", GroupRecipeManager.class);

    private static PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);

    private static DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);

    private static OrganService organService = ApplicationUtils.getBasicService(OrganService.class);

    private static RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);

    private static DepartmentService departmentService = ApplicationUtils.getBasicService(DepartmentService.class);

    private static IConfigurationCenterUtilsService configService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);

    private static Integer[] showRecipeStatus = new Integer[]{RecipeStatusConstant.CHECK_PASS_YS, RecipeStatusConstant.IN_SEND, RecipeStatusConstant.WAIT_SEND, RecipeStatusConstant.FINISH};

    private static Integer[] showDownloadRecipeStatus = new Integer[]{RecipeStatusConstant.CHECK_PASS_YS, RecipeStatusConstant.RECIPE_DOWNLOADED};

    private static RecipeListService recipeListService = ApplicationUtils.getRecipeService(RecipeListService.class);

    private static IAuditMedicinesService iAuditMedicinesService = AppContextHolder.getBean("recipeaudit.remoteAuditMedicinesService", IAuditMedicinesService.class);

    private static RecipeManager recipeManager = AppContextHolder.getBean("recipeManager", RecipeManager.class);

    private static RecipeDetailManager recipeDetailManager = AppContextHolder.getBean("recipeDetailManager", RecipeDetailManager.class);

    private static List<String> specitalOrganList = Lists.newArrayList("1005790", "1005217", "1005789");


    /**
     * @param recipeBean
     * @param detailBeanList
     * @param flag(recipe的fromflag) 0：HIS处方  1：平台处方
     * @return
     */
    public Integer saveRecipeDataImpl(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList, Integer flag) {
        LOGGER.info("RecipeServiceSub saveRecipeDataImpl recipeBean:{},detailBeanList:{},flag:{}"
                , JSONUtils.toString(recipeBean), JSONUtils.toString(detailBeanList), flag);
        if (null != recipeBean && recipeBean.getRecipeId() != null && recipeBean.getRecipeId() > 0) {
            RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
            return recipeService.updateRecipeAndDetail(recipeBean, detailBeanList);
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        if (null == recipeBean) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipe is required!");
        }
        Recipe recipe = ObjectCopyUtils.convert(recipeBean, Recipe.class);
        List<Recipedetail> details = ObjectCopyUtils.convert(detailBeanList, Recipedetail.class);

        setRecipeMoreInfo(recipe, details, recipeBean, flag);

        Integer recipeId = recipeDAO.updateOrSaveRecipeAndDetail(recipe, details, false);
        recipe.setRecipeId(recipeId);
        PatientDTO patient = patientService.get(recipe.getMpiid());

        //加入处方扩展信息---扩展信息处理
        doWithRecipeExtend(patient, recipeBean, recipeId);

        //加入历史患者
        saveOperationRecordsForRecipe(patient, recipe);
        RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "暂存处方单");
        return recipeId;
    }

    private void doWithRecipeExtend(PatientDTO patient, RecipeBean recipeBean, Integer recipeId) {
        RecipeExtendBean recipeExt = recipeBean.getRecipeExtend();
        if (null != recipeExt && null != recipeId) {
            RecipeExtend recipeExtend = ObjectCopyUtils.convert(recipeExt, RecipeExtend.class);
            recipeExtend.setRecipeId(recipeId);
            //老的字段兼容处理
            if (StringUtils.isNotEmpty(recipeExtend.getPatientType())) {
                recipeExtend.setMedicalType(recipeExtend.getPatientType());
                switch (recipeExtend.getPatientType()) {
                    case "2":
                        recipeExtend.setMedicalTypeText(("普通医保"));
                        break;
                    case "3":
                        recipeExtend.setMedicalTypeText(("慢病医保"));
                        break;
                    default:
                }
            }
            //慢病开关
            if (recipeExtend.getRecipeChooseChronicDisease() == null) {
                try {
                    IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
                    Integer recipeChooseChronicDisease = (Integer) configurationService.getConfiguration(recipeBean.getClinicOrgan(), "recipeChooseChronicDisease");
                    recipeExtend.setRecipeChooseChronicDisease(recipeChooseChronicDisease);
                } catch (Exception e) {
                    LOGGER.error("doWithRecipeExtend 获取开关异常", e);
                }
            }

            if (null != patient) {
                recipeExtend.setGuardianName(patient.getGuardianName());
                recipeExtend.setGuardianCertificate(patient.getGuardianCertificate());
                recipeExtend.setGuardianMobile(patient.getMobile());
            }
            //根据复诊id 保存就诊卡号和就诊卡类型
            Integer consultId = recipeBean.getClinicId();
            if (consultId != null) {
                IRevisitExService exService = RevisitAPI.getService(IRevisitExService.class);
                RevisitExDTO consultExDTO = exService.getByConsultId(consultId);
                if (consultExDTO != null) {
                    recipeExtend.setCardNo(consultExDTO.getCardId());
                    recipeExtend.setCardType(consultExDTO.getCardType());
                }
            }
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            recipeExtendDAO.saveOrUpdateRecipeExtend(recipeExtend);
        }
    }

    public static void setRecipeMoreInfo(Recipe recipe, List<Recipedetail> details, RecipeBean recipeBean, Integer flag) {
        //校验处方和明细保存数据
        validateRecipeAndDetailData(recipe, details);

        //设置处方默认数据
        RecipeUtil.setDefaultData(recipe);
        //设置处方明细数据
        if (details != null && details.size() > 0) {
            setReciepeDetailsInfo(flag, recipeBean, recipe, details);
        }

        //患者数据前面已校验--设置患者姓名医生姓名机构名
        PatientDTO patient = patientService.get(recipe.getMpiid());
        recipe.setPatientName(patient.getPatientName());
        recipe.setDoctorName(doctorService.getNameById(recipe.getDoctor()));
        OrganDTO organBean = organService.get(recipe.getClinicOrgan());
        recipe.setOrganName(organBean.getShortName());

        //武昌机构recipeCode平台生成
        getRecipeCodeForWuChang(recipeBean, patient, recipe);

        // 根据咨询单特殊来源标识设置处方单特殊来源标识
        if (null != recipe.getClinicId()) {
            if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource())) {
                IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);
                RevisitBean consultBean = iRevisitService.getById(recipe.getClinicId());
                if ((null != consultBean) && (Integer.valueOf(1).equals(consultBean.getConsultSource()))) {
                    recipe.setRecipeSource(consultBean.getConsultSource());
                }
            } else if (RecipeBussConstant.BUSS_SOURCE_WZ.equals(recipe.getBussSource())) {
                IConsultService consultService = ConsultAPI.getService(IConsultService.class);
                ConsultBean consultBean = consultService.getById(recipe.getClinicId());
                if ((null != consultBean) && (Integer.valueOf(1).equals(consultBean.getConsultSource()))) {
                    recipe.setRecipeSource(consultBean.getConsultSource());
                }
            }
        }
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        recipeService.setMergeDrugType(details, recipe);
    }

    private static void saveOperationRecordsForRecipe(PatientDTO patient, Recipe recipe) {
        IOperationRecordsService iOperationRecordsService = ApplicationUtils.getBaseService(IOperationRecordsService.class);
        OperationRecordsBean record = new OperationRecordsBean();
        record.setMpiId(patient.getMpiId());
        record.setRequestMpiId(patient.getMpiId());
        record.setPatientName(patient.getPatientName());
        record.setBussType(BussTypeConstant.RECIPE);
        record.setBussId(recipe.getRecipeId());
        record.setRequestDoctor(recipe.getDoctor());
        record.setExeDoctor(recipe.getDoctor());
        record.setRequestTime(recipe.getCreateDate());
        iOperationRecordsService.saveOperationRecordsForRecipe(record);
    }

    private static void getRecipeCodeForWuChang(RecipeBean recipeBean, PatientDTO patient, Recipe recipe) {
        RedisClient redisClient = RedisClient.instance();
        Set<String> organIdList = redisClient.sMembers(CacheConstant.KEY_WUCHANG_ORGAN_LIST);
        //武昌机构recipeCode平台生成
        if (RecipeBussConstant.FROMFLAG_HIS_USE.equals(recipeBean.getFromflag()) || (CollectionUtils.isNotEmpty(organIdList) && organIdList.contains(recipe.getClinicOrgan().toString()))) {
            //在 doSignRecipe 生成的一些数据在此生成
            PatientDTO requestPatient = patientService.getOwnPatientForOtherProject(patient.getLoginId());
            if (null != requestPatient && null != requestPatient.getMpiId()) {
                recipe.setRequestMpiId(requestPatient.getMpiId());
                // urt用于系统消息推送
                recipe.setRequestUrt(requestPatient.getUrt());
            }
            //生成处方编号，不需要通过HIS去产生
            String recipeCodeStr = "ngari" + DigestUtil.md5For16(recipeBean.getClinicOrgan() + recipeBean.getMpiid() + Calendar.getInstance().getTimeInMillis());
            recipe.setRecipeCode(recipeCodeStr);
            recipeBean.setRecipeCode(recipeCodeStr);
        }
    }

    private static void setReciepeDetailsInfo(Integer flag, RecipeBean recipeBean, Recipe recipe, List<Recipedetail> details) {
        //设置药品详情数据
        boolean isSucc = setDetailsInfo(recipe, details);
        if (!isSucc) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品详情数据有误");
        }
        recipeBean.setTotalMoney(recipe.getTotalMoney());
        recipeBean.setActualPrice(recipe.getActualPrice());

        //保存开处方时的单位剂量【规格单位】，单位【规格单位】，单位剂量【最小单位】，单位【最小单位】,以json对象的方式存储
        LOGGER.info("setReciepeDetailsInfo recipedetails:{}", JSONUtils.toString(details));
        if (details != null && details.size() > 0) {
            for (Recipedetail detail : details) {
                OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
                OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(), detail.getOrganDrugCode(), detail.getDrugId());
                String unitDoseForSpecificationUnit = "";
                String unitForSpecificationUnit = "";
                String unitDoseForSmallUnit = "";
                String unitForSmallUnit = "";
                Map<String, String> drugUnitdoseAndUnitMap = new HashMap<>();
                if (organDrugList != null) {
                    unitDoseForSpecificationUnit = organDrugList.getUseDose() == null ? "" : organDrugList.getUseDose().toString();
                    unitForSpecificationUnit = organDrugList.getUseDoseUnit() == null ? "" : organDrugList.getUseDoseUnit();
                    unitDoseForSmallUnit = organDrugList.getSmallestUnitUseDose() == null ? "" : organDrugList.getSmallestUnitUseDose().toString();
                    unitForSmallUnit = organDrugList.getUseDoseSmallestUnit() == null ? "" : organDrugList.getUseDoseSmallestUnit();
                }
                drugUnitdoseAndUnitMap.put("unitDoseForSpecificationUnit", unitDoseForSpecificationUnit);
                drugUnitdoseAndUnitMap.put("unitForSpecificationUnit", unitForSpecificationUnit);
                drugUnitdoseAndUnitMap.put("unitDoseForSmallUnit", unitDoseForSmallUnit);
                drugUnitdoseAndUnitMap.put("unitForSmallUnit", unitForSmallUnit);
                LOGGER.info("setReciepeDetailsInfo drugUnitdoseAndUnitMap:{}", JSONUtils.writeValueAsString(drugUnitdoseAndUnitMap));
                detail.setDrugUnitdoseAndUnit(JSONUtils.toString(drugUnitdoseAndUnitMap));
            }
        }
        LOGGER.info("setReciepeDetailsInfo recipedetails:{}", JSONUtils.toString(details));
    }

    private static void validateRecipeAndDetailData(Recipe recipe, List<Recipedetail> details) {
        RecipeValidateUtil.validateSaveRecipeData(recipe);
        if (null != details) {
            for (Recipedetail recipeDetail : details) {
                RecipeValidateUtil.validateRecipeDetailData(recipeDetail, recipe);
            }
        }
    }


    /**
     * 设置药品详情数据
     *
     * @param recipe        处方
     * @param recipedetails 处方ID
     */
    public static boolean setDetailsInfo(Recipe recipe, List<Recipedetail> recipedetails) {
        getMedicalInfo(recipe);
        boolean success = false;
        int organId = recipe.getClinicOrgan();
        //药品总金额
        BigDecimal totalMoney = new BigDecimal(0d);
        List<Integer> drugIds = new ArrayList<>(recipedetails.size());
        List<String> organDrugCodes = new ArrayList<>(recipedetails.size());
        Date nowDate = DateTime.now().toDate();
        String recipeMode = recipe.getRecipeMode();

        for (Recipedetail detail : recipedetails) {
            //设置药品详情基础数据
            detail.setStatus(1);
            detail.setRecipeId(recipe.getRecipeId());
            detail.setCreateDt(nowDate);
            detail.setLastModify(nowDate);
            if (null != detail.getDrugId()) {
                drugIds.add(detail.getDrugId());
            }
            if (StringUtils.isNotEmpty(detail.getOrganDrugCode())) {
                organDrugCodes.add(detail.getOrganDrugCode());
            }
        }
        if (CollectionUtils.isNotEmpty(drugIds)) {
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
            //判断平台基础库药品是否删除
            List<DrugList> drugLists = drugListDAO.findByDrugIdsforDel(drugIds);
            if (CollectionUtils.isNotEmpty(drugLists)) {
                List<String> delDrugName = Lists.newArrayList();
                for (DrugList drugList : drugLists) {
                    delDrugName.add(drugList.getDrugName());
                }
                if (CollectionUtils.isNotEmpty(delDrugName)) {
                    String errorDrugName = Joiner.on(",").join(delDrugName);
                    throw new DAOException(ErrorCode.SERVICE_ERROR, errorDrugName + "药品已失效，请重新选择药品");
                }
            }
            //是否为老的药品兼容方式，老的药品传入方式没有organDrugCode
            boolean oldFlag = organDrugCodes.isEmpty() ? true : false;
            Map<String, OrganDrugList> organDrugListMap = Maps.newHashMap();
            Map<Integer, OrganDrugList> organDrugListIdMap = Maps.newHashMap();
            List<OrganDrugList> organDrugList = Lists.newArrayList();
            if (oldFlag) {
                organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(organId, drugIds);
            } else {
                organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(organId, organDrugCodes);
            }

            if (CollectionUtils.isNotEmpty(organDrugList)) {
                if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipeMode)) {
                    //药品有混非外带药和外带药的不能一起开
                    int takeMedicineSize = 0;
                    List<String> takeOutDrugName = Lists.newArrayList();
                    for (OrganDrugList obj : organDrugList) {
                        //检验是否都为外带药
                        if (Integer.valueOf(1).equals(obj.getTakeMedicine())) {
                            takeMedicineSize++;
                            takeOutDrugName.add(obj.getSaleName());
                        }
                        organDrugListMap.put(obj.getOrganDrugCode() + obj.getDrugId(), obj);
                        organDrugListIdMap.put(obj.getDrugId(), obj);
                    }

                    if (takeMedicineSize > 0) {
                        if (takeMedicineSize != organDrugList.size()) {
                            String errorDrugName = Joiner.on(",").join(takeOutDrugName);
                            //外带药和处方药混合开具是不允许的
                            LOGGER.warn("setDetailsInfo 存在外带药且混合开具. recipeId=[{}], drugIds={}, 外带药={}", recipe.getRecipeId(), JSONUtils.toString(drugIds), errorDrugName);
                            throw new DAOException(ErrorCode.SERVICE_ERROR, errorDrugName + "为外带药,不能与其他药品开在同一张处方单上");
                        } else {
                            //外带处方， 同时也设置成只能配送处方
                            recipe.setTakeMedicine(1);
                            recipe.setDistributionFlag(1);
                        }
                    }
                    //判断某诊断下某药品能否开具
                    if (recipe != null && recipe.getOrganDiseaseName() != null) {
                        canOpenRecipeDrugsAndDisease(recipe, drugIds);
                    }
                } else {
                    for (OrganDrugList obj : organDrugList) {
                        organDrugListMap.put(obj.getOrganDrugCode() + obj.getDrugId(), obj);
                        organDrugListIdMap.put(obj.getDrugId(), obj);
                    }
                }


                OrganDrugList organDrug;
                List<String> delOrganDrugName = Lists.newArrayList();
                PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
                com.ngari.patient.service.IUsingRateService usingRateService = AppDomainContext.getBean("basic.usingRateService", IUsingRateService.class);
                com.ngari.patient.service.IUsePathwaysService usePathwaysService = AppDomainContext.getBean("basic.usePathwaysService", IUsePathwaysService.class);
                for (Recipedetail detail : recipedetails) {
                    //设置药品基础数据
                    if (oldFlag) {
                        organDrug = organDrugListIdMap.get(detail.getDrugId());
                    } else {
                        organDrug = organDrugListMap.get(detail.getOrganDrugCode() + detail.getDrugId());
                    }
                    if (null != organDrug) {
                        detail.setOrganDrugCode(organDrug.getOrganDrugCode());
                        detail.setDrugName(organDrug.getDrugName());
                        detail.setDrugSpec(organDrug.getDrugSpec());
                        detail.setDrugUnit(organDrug.getUnit());
                        detail.setDefaultUseDose(organDrug.getUseDose());
                        detail.setSaleName(organDrug.getSaleName());
                        //如果前端传了剂量单位优先用医生选择的剂量单位
                        //医生端剂量单位可以选择规格单位还是最小单位
                        if (StringUtils.isNotEmpty(detail.getUseDoseUnit())) {
                            detail.setUseDoseUnit(detail.getUseDoseUnit());
                        } else {
                            detail.setUseDoseUnit(organDrug.getUseDoseUnit());
                        }
                        detail.setDosageUnit(organDrug.getUseDoseUnit());
                        //设置药品包装数量
                        detail.setPack(organDrug.getPack());
                        //频次处理
                        if (StringUtils.isNotEmpty(detail.getUsingRateId())) {
                            UsingRateDTO usingRateDTO = usingRateService.getById(Integer.valueOf(detail.getUsingRateId()));
                            if (usingRateDTO != null) {
                                detail.setUsingRateTextFromHis(usingRateDTO.getText());
                                detail.setOrganUsingRate(usingRateDTO.getUsingRateKey());
                                if (usingRateDTO.getRelatedPlatformKey() != null) {
                                    detail.setUsingRate(usingRateDTO.getRelatedPlatformKey());
                                } else {
                                    detail.setUsingRate(usingRateDTO.getUsingRateKey());
                                }

                            }
                        }
                        //用法处理
                        if (StringUtils.isNotEmpty(detail.getUsePathwaysId())) {
                            UsePathwaysDTO usePathwaysDTO = usePathwaysService.getById(Integer.valueOf(detail.getUsePathwaysId()));
                            if (usePathwaysDTO != null) {
                                detail.setUsePathwaysTextFromHis(usePathwaysDTO.getText());
                                detail.setOrganUsePathways(usePathwaysDTO.getPathwaysKey());
                                if (usePathwaysDTO.getRelatedPlatformKey() != null) {
                                    detail.setUsePathways(usePathwaysDTO.getRelatedPlatformKey());
                                } else {
                                    detail.setUsePathways(usePathwaysDTO.getPathwaysKey());
                                }
                            }
                        }
                        //中药基础数据处理
                        if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType())) {
                            if (StringUtils.isBlank(detail.getUsePathways())) {
                                detail.setUsePathways(recipe.getTcmUsePathways());
                            }
                            if (StringUtils.isBlank(detail.getUsingRate())) {
                                detail.setUsingRate(recipe.getTcmUsingRate());
                            }

//                            if (detail.getUseDays() == null) {
//                                detail.setUseDays(recipe.getCopyNum());
//                            }
                            if (detail.getUseDose() != null) {
                                detail.setUseTotalDose(BigDecimal.valueOf(recipe.getCopyNum()).multiply(BigDecimal.valueOf(detail.getUseDose())).doubleValue());
                            }
                            //中药药品显示名称处理---固定--中药药品名暂时前端拼接写死
                            //detail.setDrugDisplaySplicedName(DrugNameDisplayUtil.dealwithRecipedetailName(null, detail,RecipeBussConstant.RECIPETYPE_TCM));
                        } else if (RecipeBussConstant.RECIPETYPE_HP.equals(recipe.getRecipeType())) {

//                            if (detail.getUseDays() == null) {
//                                detail.setUseDays(recipe.getCopyNum());
//                            }
                            if (detail.getUseDose() != null) {
                                detail.setUseTotalDose(BigDecimal.valueOf(recipe.getCopyNum()).multiply(BigDecimal.valueOf(detail.getUseDose())).doubleValue());
                            }
                        }
                        //添加机构药品信息
                        //date 20200225
                        detail.setProducer(organDrug.getProducer());
                        detail.setProducerCode(organDrug.getProducerCode());
                        detail.setLicenseNumber(organDrug.getLicenseNumber());

                        //设置药品价格
                        BigDecimal price = organDrug.getSalePrice();
                        if (null == price) {
                            LOGGER.warn("setDetailsInfo 药品ID：" + organDrug.getDrugId() + " 在医院(ID为" + organId + ")的价格为NULL！");
                            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品数据异常！");
                        }
                        detail.setSalePrice(price);
                        //保留3位小数
                        BigDecimal drugCost = price.multiply(new BigDecimal(detail.getUseTotalDose())).divide(BigDecimal.ONE, 3, RoundingMode.UP);
                        detail.setDrugCost(drugCost);
                        totalMoney = totalMoney.add(drugCost);
                        //药房处理
                        if (detail.getPharmacyId() != null && StringUtils.isEmpty(detail.getPharmacyName())) {
                            PharmacyTcm pharmacyTcm = pharmacyTcmDAO.get(detail.getPharmacyId());
                            if (pharmacyTcm != null) {
                                detail.setPharmacyName(pharmacyTcm.getPharmacyName());
                            }
                        }
                    } else {
                        if (StringUtils.isNotEmpty(detail.getDrugName())) {
                            delOrganDrugName.add(detail.getDrugName());
                        }
                    }

                    //date 202000601
                    //设置处方用药天数字符类型
                    if (StringUtils.isEmpty(detail.getUseDaysB())) {

                        detail.setUseDaysB(null != detail.getUseDays() ? detail.getUseDays().toString() : "0");

                    }
                }
                if (CollectionUtils.isNotEmpty(delOrganDrugName)) {
                    String errorDrugName = Joiner.on(",").join(delOrganDrugName);
                    throw new DAOException(ErrorCode.SERVICE_ERROR, errorDrugName + "药品已失效，请重新选择药品");
                }
                success = true;
            } else {
                LOGGER.warn("setDetailsInfo organDrugList. recipeId=[{}], drugIds={}", recipe.getRecipeId(), JSONUtils.toString(drugIds));
                throw new DAOException(ErrorCode.SERVICE_ERROR, "药品已失效，请重新选择药品");
            }
        } else {
            LOGGER.warn("setDetailsInfo 详情里没有药品ID. recipeId=[{}]", recipe.getRecipeId());
        }

        recipe.setTotalMoney(totalMoney);
        recipe.setActualPrice(totalMoney);
        return success;
    }

    public static RecipeResultBean validateRecipeSendDrugMsg(RecipeBean recipe) {
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Integer> drugIds = detailDAO.findDrugIdByRecipeId(recipe.getRecipeId());
        try {
            //date 20200921 修改【his管理的药企】不用校验配送药品，由预校验结果
            if (new Integer(1).equals(RecipeServiceSub.getOrganEnterprisesDockType(recipe.getClinicOrgan()))) {
                return resultBean;
            } else {
                //处方药品能否配送以及能否开具同一张处方上
                canOpenRecipeDrugs(recipe.getClinicOrgan(), recipe.getRecipeId(), drugIds);
            }
        } catch (Exception e) {
            LOGGER.error("canOpenRecipeDrugs error", e);
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg(e.getMessage());
            return resultBean;
        }
        return resultBean;
    }

    private static void canOpenRecipeDrugsAndDisease(Recipe recipe, List<Integer> drugIds) {
        List<String> nameLists = Splitter.on(ByteUtils.SEMI_COLON_EN).splitToList(recipe.getOrganDiseaseName());
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        for (String organDiseaseName : nameLists) {
            Set<String> drugIdSet = cacheService.findDrugByDiseaseName(recipe.getClinicOrgan() + "_" + organDiseaseName);
            if (CollectionUtils.isEmpty(drugIdSet)) {
                continue;
            }
            for (String drugId : drugIdSet) {
                if (drugIds.contains(Integer.valueOf(drugId))) {
                    DrugList drugList = drugListDAO.getById(Integer.valueOf(drugId));
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "本处方中:" + drugList.getDrugName() + "对诊断为[" + organDiseaseName + "]的患者禁用,请修改处方,如确认无误请联系管理员");
                }
            }
        }
    }

    public static void canOpenRecipeDrugs(Integer clinicOrgan, Integer recipeId, List<Integer> drugIds) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        List<DrugList> drugList = drugListDAO.findByDrugIds(drugIds);
        //list转map
        Map<Integer, DrugList> drugListMap = drugList.stream().collect(Collectors.toMap(DrugList::getDrugId, a -> a));


        //供应商一致性校验，取第一个药品能配送的药企作为标准
        //应该按照机构配置了的药企作为条件来查找是否能配送
        //获取该机构下配置的药企
        OrganAndDrugsepRelationDAO relationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        List<DrugsEnterprise> enterprises = relationDAO.findDrugsEnterpriseByOrganIdAndStatus(clinicOrgan, 1);
        List<Integer> deps = enterprises.stream().map(e -> e.getId()).collect(Collectors.toList());
        //找到每一个药能支持的药企关系
        Map<Integer, List<String>> drugDepRel = saleDrugListDAO.findDrugDepRelation(drugIds, deps);

        //无法配送药品校验------有一个药企能支持就不会提示
        List<String> noFilterDrugName = new ArrayList<>();
        for (Integer drugId : drugIds) {
            if (CollectionUtils.isEmpty(drugDepRel.get(drugId))) {
                noFilterDrugName.add(drugListMap.get(drugId).getDrugName());
            }
        }
        if (CollectionUtils.isNotEmpty(noFilterDrugName)) {
            LOGGER.warn("setDetailsInfo 存在无法配送的药品. recipeId=[{}], drugIds={}, noFilterDrugName={}", recipeId, JSONUtils.toString(drugIds), JSONUtils.toString(noFilterDrugName));
            throw new DAOException(ErrorCode.SERVICE_ERROR, Joiner.on(",").join(noFilterDrugName) + "不在该机构可配送药企的药品目录里面，无法进行配送");
        }

        noFilterDrugName.clear();
        //取第一个药能支持的药企做标准来判断
        List<String> firstDrugDepIds = drugDepRel.get(drugIds.get(0));
        for (Integer drugId : drugDepRel.keySet()) {
            List<String> depIds = drugDepRel.get(drugId);
            boolean filterFlag = false;
            for (String depId : depIds) {
                //匹配到一个药企相同则可跳过
                if (firstDrugDepIds.contains(depId)) {
                    filterFlag = true;
                    break;
                }
            }
            if (!filterFlag) {
                noFilterDrugName.add(drugListMap.get(drugId).getDrugName());
            } else {
                //取交集
                firstDrugDepIds.retainAll(depIds);
            }
        }

        if (CollectionUtils.isNotEmpty(noFilterDrugName)) {
            List<DrugList> drugLists = new ArrayList<DrugList>(drugListMap.values());
            List<String> drugNames = drugLists.stream().map(e -> e.getDrugName()).collect(Collectors.toList());
            LOGGER.error("setDetailsInfo 存在无法一起配送的药品. recipeId=[{}], drugIds={}, noFilterDrugName={}", recipeId, JSONUtils.toString(drugIds), JSONUtils.toString(noFilterDrugName));
            //一张处方单上的药品不能同时支持同一家药企配送
            throw new DAOException(ErrorCode.SERVICE_ERROR, Joiner.on(",").join(drugNames) + "不支持同一家药企配送");
        }
    }

    /**
     * 组装生成pdf的参数集合
     * zhongzx
     *
     * @param recipe  处方对象
     * @param details 处方详情
     * @return Map<String, Object>
     */
    public static Map<String, Object> createParamMap(Recipe recipe, List<Recipedetail> details, String fileName) {
        Map<String, Object> paramMap = Maps.newHashMap();
        try {
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend extend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            EmrRecipeManager.getMedicalInfo(recipe, extend);
            PatientDTO p = patientService.get(recipe.getMpiid());
            if (null == p) {
                LOGGER.error("createParamMap 病人不存在. recipeId={}, mpiId={}", recipe.getRecipeId(), recipe.getMpiid());
                return paramMap;
            }
            //模板类型，西药模板
            paramMap.put("templateType", "wm");
            //生成pdf文件的入参
            paramMap.put("fileName", fileName);
            paramMap.put("recipeType", recipe.getRecipeType());
            String recipeType = DictionaryController.instance().get("eh.cdr.dictionary.RecipeType").getText(recipe.getRecipeType());
            paramMap.put("title", recipeType + "处方笺");
            paramMap.put("pName", p.getPatientName());
            paramMap.put("pGender", DictionaryController.instance().get("eh.base.dictionary.Gender").getText(p.getPatientSex()));
            paramMap.put("pAge", DateConversion.getAge(p.getBirthday()) + "岁");
            //date 20200908 添加体重字段，住院病历号，就诊卡号
            if (StringUtils.isNotEmpty(p.getWeight())) {
                paramMap.put("pWeight", p.getWeight() + "kg");
            }
            paramMap.put("pHisID", recipe.getPatientID());
            if (null != extend) {
                paramMap.put("pCardNo", extend.getCardNo());
            }

            paramMap.put("pType", DictionaryController.instance().get("eh.mpi.dictionary.PatientType").getText(p.getPatientType()));
            paramMap.put("doctor", DictionaryController.instance().get("eh.base.dictionary.Doctor").getText(recipe.getDoctor()));
            String organ = DictionaryController.instance().get("eh.base.dictionary.Organ").getText(recipe.getClinicOrgan());
            String depart = DictionaryController.instance().get("eh.base.dictionary.Depart").getText(recipe.getDepart());
            paramMap.put("organInfo", organ);
            // 添加机构id
            paramMap.put("organId", recipe.getClinicOrgan());
            paramMap.put("departInfo", depart);
            paramMap.put("disease", recipe.getOrganDiseaseName());
            paramMap.put("cDate", DateConversion.getDateFormatter(recipe.getSignDate(), "yyyy-MM-dd HH:mm"));
            paramMap.put("diseaseMemo", recipe.getMemo());
            paramMap.put("recipeCode", recipe.getRecipeCode() == null ? "" : recipe.getRecipeCode().startsWith("ngari") ? "" : recipe.getRecipeCode());
            paramMap.put("patientId", recipe.getPatientID());
            paramMap.put("mobile", p.getMobile());
            paramMap.put("loginId", p.getLoginId());
            paramMap.put("label", recipeType + "处方");
            int i = 0;
            ctd.dictionary.Dictionary usingRateDic = DictionaryController.instance().get("eh.cdr.dictionary.UsingRate");
            Dictionary usePathwaysDic = DictionaryController.instance().get("eh.cdr.dictionary.UsePathways");
            String useDose;
            for (Recipedetail d : details) {
                String dName = (i + 1) + "、" + d.getDrugName();
                //规格+药品单位
                String dSpec = d.getDrugSpec() + "/" + d.getDrugUnit();
                //使用天数
                String useDay = d.getUseDays() + "天";
                if (StringUtils.isNotEmpty(d.getUseDoseStr())) {
                    useDose = d.getUseDoseStr();
                } else {
                    useDose = d.getUseDose() != null ? String.valueOf(d.getUseDose()) : d.getUseDoseStr();
                }
                //每次剂量+剂量单位
                String uDose = "Sig: " + "每次" + useDose + (StringUtils.isEmpty(d.getUseDoseUnit()) ? "" : (StringUtils.isNotEmpty(d.getUseDoseStr()) ? "" : d.getUseDoseUnit()));
                //开药总量+药品单位
                String dTotal = "X" + d.getUseTotalDose() + d.getDrugUnit();
                //用药频次
                String dRateName = d.getUsingRateTextFromHis() != null ? d.getUsingRateTextFromHis() : usingRateDic.getText(d.getUsingRate());
                //用法
                String dWay = d.getUsePathwaysTextFromHis() != null ? d.getUsePathwaysTextFromHis() : usePathwaysDic.getText(d.getUsePathways());
                paramMap.put("drugInfo" + i, dName + dSpec);
                paramMap.put("dTotal" + i, dTotal);
                paramMap.put("useInfo" + i, uDose + "    " + dRateName + "    " + dWay + "    " + useDay);
                if (!StringUtils.isEmpty(d.getMemo())) {
                    //备注
                    paramMap.put("dMemo" + i, "备注:" + d.getMemo());
                }
                Object canShowDrugCost = configService.getConfiguration(recipe.getClinicOrgan(), "canShowDrugCost");
                LOGGER.info("createParamMap recipeId:{} canShowDrugCost:{}", recipe.getRecipeId(), canShowDrugCost);
                if ((boolean) canShowDrugCost) {
                    paramMap.put("drugCost" + i, d.getDrugCost().divide(BigDecimal.ONE, 2, RoundingMode.UP) + "元");
                }
                i++;
            }
            paramMap.put("recipeFee", recipe.getTotalMoney() + "元");
            paramMap.put("drugNum", i);

        } catch (Exception e) {
            LOGGER.error("createParamMap 组装参数错误. recipeId={}, error ", recipe.getRecipeId(), e);
        }
        LOGGER.info("createParamMap 组装参数. recipeId={},paramMap={}  ", recipe.getRecipeId(), JSONUtils.toString(paramMap));
        return paramMap;
    }

    /**
     * 中药处方pdf模板
     *
     * @param recipe
     * @param details
     * @param fileName
     * @return
     * @Author liuya
     */
    public static Map<String, Object> createParamMapForChineseMedicine(Recipe recipe, List<Recipedetail> details, String fileName) {
        Map<String, Object> paramMap = Maps.newHashMap();
        try {
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend extend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            EmrRecipeManager.getMedicalInfo(recipe, extend);
            PatientDTO p = patientService.get(recipe.getMpiid());
            if (null == p) {
                LOGGER.error("createParamMapForChineseMedicine 病人不存在. recipeId={}, mpiId={}", recipe.getRecipeId(), recipe.getMpiid());
                return paramMap;
            }
            //模板类型，中药类模板
            paramMap.put("templateType", "tcm");
            //生成pdf文件的入参
            paramMap.put("fileName", fileName);
            paramMap.put("recipeType", recipe.getRecipeType());
            String recipeType = DictionaryController.instance().get("eh.cdr.dictionary.RecipeType").getText(recipe.getRecipeType());
            paramMap.put("title", recipeType + "处方笺");
            paramMap.put("pName", p.getPatientName());
            paramMap.put("pGender", DictionaryController.instance().get("eh.base.dictionary.Gender").getText(p.getPatientSex()));
            paramMap.put("pAge", DateConversion.getAge(p.getBirthday()) + "岁");
            //date 20200908 添加体重字段，住院病历号，就诊卡号
            if (StringUtils.isNotEmpty(p.getWeight())) {
                paramMap.put("pWeight", p.getWeight() + "kg");
            }
            paramMap.put("pHisID", recipe.getPatientID());
            //date 20200909 添加字段，嘱托,煎法,制法,次量,每付取汁,天数
            paramMap.put("tcmRecipeMemo", recipe.getRecipeMemo());

            if (null != extend) {
                paramMap.put("pCardNo", extend.getCardNo());
                paramMap.put("tcmDecoction", extend.getDecoctionText());
                paramMap.put("tcmJuice", extend.getJuice() + extend.getJuiceUnit());
                paramMap.put("tcmMinor", extend.getMinor() + extend.getMinorUnit());
                paramMap.put("tcmMakeMethod", extend.getMakeMethodText());
            }

            paramMap.put("pType", DictionaryController.instance().get("eh.mpi.dictionary.PatientType").getText(p.getPatientType()));
            paramMap.put("doctor", DictionaryController.instance().get("eh.base.dictionary.Doctor").getText(recipe.getDoctor()));
            String organ = DictionaryController.instance().get("eh.base.dictionary.Organ").getText(recipe.getClinicOrgan());
            String depart = DictionaryController.instance().get("eh.base.dictionary.Depart").getText(recipe.getDepart());
            paramMap.put("organInfo", organ);
            // 添加机构id
            paramMap.put("organId", recipe.getClinicOrgan());
            paramMap.put("departInfo", depart);
            paramMap.put("disease", recipe.getOrganDiseaseName());
            paramMap.put("cDate", DateConversion.getDateFormatter(recipe.getSignDate(), "yyyy-MM-dd HH:mm"));
            paramMap.put("diseaseMemo", recipe.getMemo());
            paramMap.put("recipeCode", recipe.getRecipeCode() == null ? "" : recipe.getRecipeCode().startsWith("ngari") ? "" : recipe.getRecipeCode());
            paramMap.put("patientId", recipe.getPatientID());
            paramMap.put("mobile", p.getMobile());
            paramMap.put("loginId", p.getLoginId());
            paramMap.put("label", recipeType + "处方");
            paramMap.put("copyNum", recipe.getCopyNum() + "贴");
            paramMap.put("recipeMemo", recipe.getRecipeMemo());
            int i = 0;
            for (Recipedetail d : details) {
                String dName = d.getDrugName();
                //开药总量+药品单位
                String dTotal = "";
                if (StringUtils.isNotEmpty(d.getUseDoseStr())) {
                    dTotal = d.getUseDoseStr() + d.getUseDoseUnit();
                } else {
                    if (d.getUseDose() != null) {
                        //增加判断条件  如果用量小数位为零，则不显示小数点
                        if ((d.getUseDose() - d.getUseDose().intValue()) == 0d) {
                            dTotal = d.getUseDose().intValue() + d.getUseDoseUnit();
                        } else {
                            dTotal = d.getUseDose() + d.getUseDoseUnit();
                        }
                    }
                }

                if (!StringUtils.isEmpty(d.getMemo())) {
                    //备注
                    dTotal = dTotal + "*" + d.getMemo();
                }
                paramMap.put("drugInfo" + i, dName + "：" + dTotal);
                if (StringUtils.isNotEmpty(DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(d.getUsePathways()))) {
                    paramMap.put("tcmUsePathways", DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(d.getUsePathways()));
                } else {
                    paramMap.put("tcmUsePathways", d.getUsePathways());
                }
                if (StringUtils.isNotEmpty(DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(d.getUsingRate()))) {
                    paramMap.put("tcmUsingRate", DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(d.getUsingRate()));
                } else {
                    paramMap.put("tcmUsingRate", d.getUsingRate());
                }

                paramMap.put("tcmUseDay", null != d.getUseDaysB() ? d.getUseDaysB() : d.getUseDays());
                i++;
            }
            paramMap.put("recipeFee", recipe.getTotalMoney() + "元");
            paramMap.put("drugNum", i);

        } catch (Exception e) {
            LOGGER.error("createParamMapForChineseMedicine 组装参数错误. recipeId={}, error ", recipe.getRecipeId(), e);
        }
        LOGGER.info("createParamMapForChineseMedicine 组装参数. recipeId={},paramMap={}  ", recipe.getRecipeId(), JSONUtils.toString(paramMap));
        return paramMap;
    }

    /**
     * 处方列表服务
     *
     * @param doctorId 开方医生
     * @param start    分页开始位置
     * @param limit    每页限制条数
     * @param mark     标志 --0新处方1历史处方
     * @return List
     */
    public static List<HashMap<String, Object>> findRecipesAndPatientsByDoctor(final int doctorId, final int start, final int limit, final int mark) {
        if (0 == limit) {
            return null;
        }

        List<Recipe> recipes;
        // 是否含有未签名的数据
        boolean hasUnsignRecipe = false;
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);

        if (0 == mark) {
            recipes = new ArrayList<>(0);
            int endIndex = start + limit;
            //先查询未签名处方的数量
            int unsignCount = recipeDAO.getCountByDoctorIdAndStatus(doctorId, Arrays.asList(RecipeStatusConstant.CHECK_NOT_PASS, RecipeStatusConstant.UNSIGN), ConditionOperator.IN, false);
            //查询未签名的处方数据
            if (unsignCount > start) {
                hasUnsignRecipe = true;
                List<Recipe> unsignRecipes = recipeDAO.findByDoctorIdAndStatus(doctorId, Arrays.asList(RecipeStatusConstant.CHECK_NOT_PASS, RecipeStatusConstant.UNSIGN), ConditionOperator.IN, false, start, limit, mark);
                if (null != unsignRecipes && !unsignRecipes.isEmpty()) {
                    recipes.addAll(unsignRecipes);
                }

                //当前页的数据未签名的数据无法充满则需要查询未审核的数据
                if (unsignCount < endIndex) {
                    List<Recipe> uncheckRecipes = recipeDAO.findByDoctorIdAndStatus(doctorId, Collections.singletonList(RecipeStatusConstant.UNCHECK), ConditionOperator.EQUAL, false, 0, limit - recipes.size(), mark);
                    if (null != uncheckRecipes && !uncheckRecipes.isEmpty()) {
                        recipes.addAll(uncheckRecipes);
                    }
                }
            } else {
                //未签名的数据已经全部显示
                int startIndex = start - unsignCount;
                List<Recipe> uncheckRecipes = recipeDAO.findByDoctorIdAndStatus(doctorId, Collections.singletonList(RecipeStatusConstant.UNCHECK), ConditionOperator.EQUAL, false, startIndex, limit, mark);
                if (null != uncheckRecipes && !uncheckRecipes.isEmpty()) {
                    recipes.addAll(uncheckRecipes);
                }
            }
        } else {
            //历史处方数据
            recipes = recipeDAO.findByDoctorIdAndStatus(doctorId, Collections.singletonList(RecipeStatusConstant.CHECK_PASS), ConditionOperator.GREAT_EQUAL, false, start, limit, mark);
        }

        List<String> patientIds = new ArrayList<>(0);
        Map<Integer, RecipeBean> recipeMap = Maps.newHashMap();
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        //date 20200506
        //获取处方对应的订单信息
        Map<String, Integer> orderStatus = new HashMap<>();
        List<String> recipeCodes = recipes.stream().map(recipe -> recipe.getOrderCode()).filter(code -> StringUtils.isNotEmpty(code)).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(recipeCodes)) {
            List<RecipeOrder> recipeOrders = orderDAO.findValidListbyCodes(recipeCodes);
            orderStatus = recipeOrders.stream().collect(Collectors.toMap(RecipeOrder::getOrderCode, RecipeOrder::getStatus));
        }
        for (Recipe recipe : recipes) {
            if (StringUtils.isNotEmpty(recipe.getMpiid())) {
                patientIds.add(recipe.getMpiid());
            }
            //设置处方具体药品名称
            recipe.setRecipeDrugName(recipeDetailDAO.getDrugNamesByRecipeId(recipe.getRecipeId()));
            //前台页面展示的时间源不同
            if (0 == mark) {
                if (null != recipe.getLastModify()) {
                    recipe.setRecipeShowTime(recipe.getLastModify());
                }
            } else {
                if (null != recipe.getSignDate()) {
                    recipe.setRecipeShowTime(recipe.getSignDate());
                }
            }
            boolean effective = false;
            //只有审核未通过的情况需要看订单状态
            if (RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
                effective = orderDAO.isEffectiveOrder(recipe.getOrderCode());
            }
            Map<String, String> tipMap = getTipsByStatusCopy(recipe.getStatus(), recipe, effective, (orderStatus == null || 0 >= orderStatus.size()) ? null : orderStatus.get(recipe.getOrderCode()));
            recipe.setShowTip(MapValueUtil.getString(tipMap, "listTips"));
            recipeMap.put(recipe.getRecipeId(), convertRecipeForRAP(recipe));
        }

        List<PatientDTO> patientList = null;
        if (!patientIds.isEmpty()) {
            patientList = patientService.findByMpiIdIn(patientIds);
        }
        Map<String, PatientVO> patientMap = Maps.newHashMap();
        if (null != patientList && !patientList.isEmpty()) {
            for (PatientDTO patient : patientList) {
                //设置患者数据
                setPatientMoreInfo(patient, doctorId);
                patientMap.put(patient.getMpiId(), convertSensitivePatientForRAP(patient));
            }
        }

        List<HashMap<String, Object>> list = new ArrayList<>(0);
        List<HashMap<String, Object>> unsignMapList = new ArrayList<>(0);
        List<HashMap<String, Object>> uncheckMapList = new ArrayList<>(0);
        for (Recipe recipe : recipes) {
            //对处方数据进行分类
            String mpiid = recipe.getMpiid();
            HashMap<String, Object> map = Maps.newHashMap();
            map.put("recipe", recipeMap.get(recipe.getRecipeId()));
            map.put("patient", patientMap.get(mpiid));

            //新开处方与历史处方JSON结构不同
            if (0 == mark) {
                if (hasUnsignRecipe) {
                    if (recipe.getStatus() <= RecipeStatusConstant.UNSIGN) {
                        //未签名处方
                        unsignMapList.add(map);
                    } else if (RecipeStatusConstant.UNCHECK == recipe.getStatus()) {
                        //未审核处方
                        uncheckMapList.add(map);
                    }
                } else {
                    uncheckMapList.add(map);
                }
            } else {
                list.add(map);
            }
        }

        if (!unsignMapList.isEmpty()) {
            HashMap<String, Object> map = Maps.newHashMap();
            map.put(UNSIGN, unsignMapList);
            list.add(map);
        }

        if (!uncheckMapList.isEmpty()) {
            HashMap<String, Object> map = Maps.newHashMap();
            map.put(UNCHECK, uncheckMapList);
            list.add(map);
        }

        return list;
    }

    /**
     * 状态文字提示（医生端）
     *
     * @param status
     * @param recipe
     * @param effective
     * @return
     */
    public static Map<String, String> getTipsByStatusCopy(int status, Recipe recipe, Boolean effective, Integer orderStatus) {
        RecipeLogDAO recipeLogDAO = DAOFactory.getDAO(RecipeLogDAO.class);
        RecipeRefundDAO recipeRefundDAO = DAOFactory.getDAO(RecipeRefundDAO.class);
        String cancelReason = "";
        String tips = "";
        String listTips = "";
        List<RecipeLog> recipeLog;
        //修改展示状态的方式，有订单的状态现优先展示订单的状态再展示处方的状态
        if (null != orderStatus && RecipeOrderStatusEnum.DOCTOR_SHOW_ORDER_STATUS.contains(orderStatus)) {
            if (RecipeOrderStatusEnum.ORDER_STATUS_NO_DRUG.getType().equals(orderStatus)
                    && RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(recipe.getGiveMode())) {
                tips = "待下载";
            } else if (RecipeOrderStatusEnum.ORDER_STATUS_NO_DRUG.getType().equals(orderStatus)
                    || RecipeOrderStatusEnum.ORDER_STATUS_HAS_DRUG.getType().equals(orderStatus)) {
                tips = "待取药";
            } else {
                tips = RecipeOrderStatusEnum.getOrderStatus(orderStatus);
            }
        }
        //判断当订单的状态不存在的时候用处方的状态
        if (StringUtils.isEmpty(tips)) {
            switch (status) {
                case RecipeStatusConstant.REVOKE:
                    if (CollectionUtils.isNotEmpty(recipeRefundDAO.findRefundListByRecipeIdAndNode(recipe.getRecipeId()))) {
                        cancelReason = "由于患者申请退费成功，该处方已取消。";
                        tips = "已取消";
                    } else {
                        tips = "已撤销";
                        cancelReason = "由于您已撤销，该处方单已失效";
                        List<RecipeLog> recipeLogs = recipeLogDAO.findByRecipeIdAndAfterStatus(recipe.getRecipeId(), status);
                        if (CollectionUtils.isNotEmpty(recipeLogs)) {
                            cancelReason = recipeLogs.get(0).getMemo();
                        }
                    }
                    break;
                case RecipeStatusConstant.HAVE_PAY:
                    tips = "待取药";
                    break;
                case RecipeStatusConstant.CHECK_PASS_YS:
                    if (StringUtils.isNotEmpty(recipe.getSupplementaryMemo())) {
                        tips = "医生再次确认处方";
                    } else {
                        tips = "审核通过";
                    }
                    listTips = "审核通过";
                    break;
                case RecipeStatusConstant.HIS_FAIL:
                    tips = "已取消";
                    //date 20200507
                    //判断当前处方调用医院接口是否有异常信息，有的话展示异常信息抹油获取默认信息
                    List<RecipeLog> recipeFailLogs = recipeLogDAO.findByRecipeIdAndAfterStatusDesc(recipe.getRecipeId(), RecipeStatusConstant.HIS_FAIL);
                    if (CollectionUtils.isNotEmpty(recipeFailLogs)) {
                        cancelReason = recipeFailLogs.get(0).getMemo().substring(recipeFailLogs.get(0).getMemo().indexOf("|") + 1, recipeFailLogs.get(0).getMemo().length() - 1);
                    } else {
                        cancelReason = "可能由于医院接口异常，处方单已取消，请稍后重试！";
                    }
                    break;
                case RecipeStatusConstant.NO_DRUG:
                    tips = "已取消";
                    cancelReason = "由于患者未及时取药，该处方单已失效";
                    break;
                case RecipeStatusConstant.NO_PAY:
                    //修改文案
                    tips = "已取消";
                    cancelReason = "由于患者未及时支付，该处方单已取消。";
                    break;
                case RecipeStatusConstant.NO_OPERATOR:
                    //修改文案
                    tips = "已取消";
                    cancelReason = "由于患者未及时处理，该处方已取消。";
                    break;
                case RecipeStatusConstant.NO_MEDICAL_INSURANCE_RETURN:
                    tips = "已取消";
                    cancelReason = "处方超时医保上传失败，处方单已取消！";
                    break;
                case RecipeStatusConstant.RECIPE_MEDICAL_FAIL:
                    tips = "已取消";
                    cancelReason = "医保上传失败，处方单已取消！";
                    break;
                case RecipeStatusConstant.SIGN_ERROR_CODE_DOC:
                    recipeLog = recipeLogDAO.findByRecipeIdAndAfterStatus(recipe.getRecipeId(), status);
                    tips = "处方签名失败";
                    if (recipeLog != null && recipeLog.size() > 0) {
                        cancelReason = "处方签名失败:" + recipeLog.get(0).getMemo();
                    } else {
                        cancelReason = "处方签名失败！";
                    }
                    break;
                case RecipeStatusConstant.SIGN_ERROR_CODE_PHA:
                    recipeLog = recipeLogDAO.findByRecipeIdAndAfterStatus(recipe.getRecipeId(), status);
                    tips = "审方签名失败";
                    if (recipeLog != null && recipeLog.size() > 0) {
                        cancelReason = "审方签名失败:" + recipeLog.get(0).getMemo();
                    } else {
                        cancelReason = "审方签名失败！";
                    }
                    break;
                case RecipeStatusConstant.SIGN_ING_CODE_PHA:
                    tips = "审方签名中";
                    break;
                case RecipeStatusConstant.REVIEW_DRUG_FAIL:
                    tips = "已取消";
                    cancelReason = "由于审方平台接口异常，处方单已取消，请稍后重试";
                    break;
                default:
                    tips = RecipeStatusEnum.getRecipeStatus(status);
            }
        }
        if (StringUtils.isEmpty(listTips)) {
            listTips = tips;
        }
        Map<String, String> map = Maps.newHashMap();
        map.put("tips", tips);
        map.put("listTips", listTips);
        map.put("cancelReason", cancelReason);
        return map;
    }

    /**
     * 状态文字提示（医生端）
     *
     * @param status
     * @param recipe
     * @param effective
     * @return
     */
    public static Map<String, String> getTipsByStatusCopy2(int status, Recipe recipe, Boolean effective, Integer orderStatus, Integer recipeRefundId) {
        String tips = "";
        String listTips = "";
        //修改展示状态的方式，有订单的状态现优先展示订单的状态再展示处方的状态
        if (null != orderStatus && RecipeOrderStatusEnum.DOCTOR_SHOW_ORDER_STATUS.contains(orderStatus)) {
            if (RecipeOrderStatusEnum.ORDER_STATUS_NO_DRUG.getType().equals(orderStatus)
                    && RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(recipe.getGiveMode())) {
                tips = "待下载";
            } else if (RecipeOrderStatusEnum.ORDER_STATUS_NO_DRUG.getType().equals(orderStatus)
                    || RecipeOrderStatusEnum.ORDER_STATUS_HAS_DRUG.getType().equals(orderStatus)) {
                tips = "待取药";
            } else {
                tips = RecipeOrderStatusEnum.getOrderStatus(orderStatus);
            }
        }
        //判断当订单的状态不存在的时候用处方的状态
        if (StringUtils.isEmpty(tips)) {
            switch (status) {
                case RecipeStatusConstant.REVOKE:
                    if (recipeRefundId != null) {
                        tips = "已取消";
                    } else {
                        tips = "已撤销";
                    }
                    break;
                case RecipeStatusConstant.HAVE_PAY:
                    tips = "待取药";
                    break;
                case RecipeStatusConstant.CHECK_PASS_YS:
                    if (StringUtils.isNotEmpty(recipe.getSupplementaryMemo())) {
                        tips = "医生再次确认处方";
                    } else {
                        tips = "审核通过";
                    }
                    listTips = "审核通过";
                    break;
                case RecipeStatusConstant.HIS_FAIL:
                    tips = "已取消";
                    break;
                case RecipeStatusConstant.NO_DRUG:
                    tips = "已取消";
                    break;
                case RecipeStatusConstant.NO_PAY:
                    //修改文案
                    tips = "已取消";
                    break;
                case RecipeStatusConstant.NO_OPERATOR:
                    //修改文案
                    tips = "已取消";
                    break;
                case RecipeStatusConstant.NO_MEDICAL_INSURANCE_RETURN:
                    tips = "已取消";
                    break;
                case RecipeStatusConstant.RECIPE_MEDICAL_FAIL:
                    tips = "已取消";
                    break;
                case RecipeStatusConstant.SIGN_ERROR_CODE_DOC:
                    tips = "处方签名失败";
                    break;
                case RecipeStatusConstant.SIGN_ERROR_CODE_PHA:
                    tips = "审方签名失败";
                    break;
                case RecipeStatusConstant.SIGN_ING_CODE_PHA:
                    tips = "审方签名中";
                    break;
                default:
                    tips = RecipeStatusEnum.getRecipeStatus(status);
            }
        }
        if (StringUtils.isEmpty(listTips)) {
            listTips = tips;
        }
        Map<String, String> map = Maps.newHashMap();
        map.put("listTips", listTips);
        return map;
    }

    public static void setPatientMoreInfo(PatientDTO patient, int doctorId) {
        try {
            IRelationPatientService iRelationPatientService = AppContextHolder.getBean("pm.remoteRelationPatientService", IRelationPatientService.class);
            RelationDoctorVO relationDoctor = iRelationPatientService.getByMpiidAndDoctorId(patient.getMpiId(), doctorId);
            //是否关注
            Boolean relationFlag = false;
            //是否签约
            Boolean signFlag = false;
            List<String> labelNames = Lists.newArrayList();
            if (relationDoctor != null) {
                relationFlag = true;
                if (relationDoctor.getFamilyDoctorFlag()) {
                    signFlag = true;
                }
                IRelationLabelService iRelationLabelService = AppContextHolder.getBean("pm.remoteRelationLabelService", IRelationLabelService.class);
                labelNames = iRelationLabelService.findLabelNamesByRPId(relationDoctor.getRelationDoctorId());

            }
            patient.setRelationFlag(relationFlag);
            patient.setSignFlag(signFlag);
            patient.setLabelNames(labelNames);
        } catch (Exception e) {
            LOGGER.error("setPatientMoreInfo error. patient={},doctorId={}", JSONUtils.toString(patient), doctorId, e);
        }
    }

    /**
     * 兼容脱敏，做预备方案,左手到右手 可以去除一些不希望前端展示的字段
     *
     * @param patient
     * @return
     */
    public static PatientDTO patientDesensitization(PatientDTO patient) {
        LOGGER.info("patientDesensitization patient={}", JSONUtils.toString(patient));
        PatientVO p = new PatientVO();
        BeanUtils.copyProperties(patient, p);
        if (StringUtils.isNotEmpty(patient.getMobile())) {
            p.setMobile(LocalStringUtil.coverMobile((patient.getMobile())));
        }
        if (StringUtils.isNotEmpty(patient.getIdcard())) {
            p.setIdcard(ChinaIDNumberUtil.hideIdCard((patient.getIdcard())));
        }
        p.setAge(null == patient.getBirthday() ? 0 : DateConversion.getAge(patient.getBirthday()));
        p.setIdcard2(null);
        p.setCertificate(null);
        PatientDTO patientDTO = new PatientDTO();
        BeanUtils.copyProperties(p, patientDTO);
        return patientDTO;
    }

    /**
     * 医生端信息脱敏：对身份证，手机号进行脱敏，返回前端对象使用PatientVO
     * 患者端脱敏需求（脱敏身份证，手机号，姓名）：返回前端对象使用PatientDS，医生app4.1.7、健康端5.1、医生PC5.2、运营平台4.9、健康app2.8,
     *
     * @param patient
     * @return
     * @author zhangx
     * @create 14:43
     **/
    public static PatientVO convertSensitivePatientForRAP(PatientDTO patient) {
        PatientVO p = new PatientVO();
        p.setPatientName(patient.getPatientName());
        p.setPatientSex(patient.getPatientSex());
        p.setBirthday(patient.getBirthday());
        p.setPatientType(patient.getPatientType());
        p.setIdcard(patient.getCertificate());
        p.setStatus(patient.getStatus());
        p.setMpiId(patient.getMpiId());
        p.setPhoto(patient.getPhoto());
        p.setSignFlag(patient.getSignFlag());
        p.setRelationFlag(patient.getRelationFlag());
        p.setLabelNames(patient.getLabelNames());
        p.setGuardianFlag(patient.getGuardianFlag());
        p.setGuardianCertificate(patient.getGuardianCertificate());
        p.setGuardianName(patient.getGuardianName());
        p.setAge(null == patient.getBirthday() ? 0 : DateConversion.getAge(patient.getBirthday()));
        return p;
    }

    public static RecipeBean convertRecipeForRAP(Recipe recipe) {
        RecipeBean r = new RecipeBean();
        r.setRecipeId(recipe.getRecipeId());
        r.setCreateDate(recipe.getCreateDate());
        r.setRecipeType(recipe.getRecipeType());
        r.setStatus(recipe.getStatus());
        r.setOrganDiseaseName(recipe.getOrganDiseaseName());
        r.setRecipeDrugName(recipe.getRecipeDrugName());
        r.setRecipeShowTime(recipe.getRecipeShowTime());
        r.setShowTip(recipe.getShowTip());
        r.setRecipeSourceType(recipe.getRecipeSourceType());
        r.setRecipeCode(recipe.getRecipeCode());
        r.setClinicOrgan(recipe.getClinicOrgan());
        r.setPayFlag(recipe.getPayFlag());
        return r;
    }

    public static RecipeBean convertRecipeForRAPNew(Recipe recipe, List<HisRecipeDetailBean> recipeDetailBeans) {
        RecipeBean r = new RecipeBean();
        r.setRecipeId(recipe.getRecipeId());
        r.setCreateDate(recipe.getCreateDate());
        r.setRecipeType(recipe.getRecipeType());
        r.setStatus(recipe.getStatus());
        r.setOrganDiseaseName(recipe.getOrganDiseaseName());
        r.setRecipeDrugName(recipe.getRecipeDrugName());
        r.setRecipeShowTime(recipe.getRecipeShowTime());
        r.setShowTip(recipe.getShowTip());
        r.setRecipeSourceType(recipe.getRecipeSourceType());
        r.setRecipeCode(recipe.getRecipeCode());
        r.setClinicOrgan(recipe.getClinicOrgan());
        r.setPayFlag(recipe.getPayFlag());
        r.setDetailData(recipeDetailBeans);
        return r;
    }

    private static void getMedicalInfo(Recipe recipe) {
        if (null == recipe || null == recipe.getRecipeId()) {
            return;
        }
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
    }

    public static RecipeBean convertHisRecipeForRAP(HisRecipeBean recipe) {
        RecipeBean r = new RecipeBean();
        r = ObjectCopyUtils.convert(recipe, RecipeBean.class);

//        r.setRecipeId(recipe.ge);
        if (StringUtils.isNotEmpty(recipe.getSignDate())) {
            r.setCreateDate(Timestamp.valueOf(recipe.getSignDate()));
        }
        r.setRecipeType(StringUtils.isEmpty(recipe.getRecipeType()) ? null : Integer.parseInt(recipe.getRecipeType()));
//        r.setStatus(recipe.getStatus());
        r.setOrganDiseaseName(recipe.getOrganDiseaseName());
        LOGGER.info("RecipeServiceSub convertHisRecipeForRAP recipe:{}.", JSONUtils.toString(recipe));
        if (StringUtils.isNotEmpty(recipe.getDetailData().get(0).getDrugDisplaySplicedName())) {
            HisRecipeDetailBean hisRecipeDetailBean = recipe.getDetailData().get(0);
            Recipedetail recipedetail = new Recipedetail();
            recipedetail.setDrugName(hisRecipeDetailBean.getDrugName());
            if (hisRecipeDetailBean.getUseDose() != null) {
                recipedetail.setUseDose(Double.parseDouble(hisRecipeDetailBean.getUseDose()));
            }
            recipedetail.setUseDoseUnit(hisRecipeDetailBean.getUseDoseUnit());
            recipedetail.setMemo(hisRecipeDetailBean.getMemo());
            recipedetail.setDrugType(Integer.parseInt(recipe.getRecipeType()));
            recipedetail.setDrugDisplaySplicedName(hisRecipeDetailBean.getDrugDisplaySplicedName());
            r.setRecipeDrugName(DrugNameDisplayUtil.dealwithRecipeDrugName(recipedetail, recipedetail.getDrugType(), recipe.getClinicOrgan()));
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(recipe.getDetailData().get(0).getDrugName());
            stringBuilder.append(" ").append((recipe.getDetailData().get(0).getDrugSpec()) == null ? "" : recipe.getDetailData().get(0).getDrugSpec()).append("/").append(recipe.getDetailData().get(0).getDrugUnit() == null ? "" : recipe.getDetailData().get(0).getDrugUnit());
            //统一显示第一个药品信息
            r.setRecipeDrugName(stringBuilder.toString());
        }


        if (StringUtils.isNotEmpty(recipe.getSignDate())) {
            r.setRecipeShowTime(Timestamp.valueOf(recipe.getSignDate()));
        }
//        r.setShowTip(recipe.getShowTip());
        r.setRecipeSourceType(2);
        r.setRecipeCode(recipe.getRecipeCode());
        r.setClinicOrgan(recipe.getClinicOrgan());
        r.setDetailData(recipe.getDetailData());
        //科室
        AppointDepartService appointDepartService = ApplicationUtils.getBasicService(AppointDepartService.class);
        AppointDepartDTO appointDepartDTO = appointDepartService.getByOrganIDAndAppointDepartCode(recipe.getClinicOrgan(), recipe.getDepartCode());
        if (appointDepartDTO != null) {
            r.setDepart(appointDepartDTO.getDepartId());
        }
        return r;
    }

    /**
     * 获取处方详情
     *
     * @param recipeId
     * @param isDoctor true:医生端  false:健康端
     * @return
     */
    public static Map<String, Object> getRecipeAndDetailByIdImpl(int recipeId, boolean isDoctor) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);

        Recipe recipe = recipeDAO.getByRecipeId(recipeId);

        Map<String, Object> map = Maps.newHashMap();
        if (recipe == null) {
            return map;
        }
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);

        DrugsEnterpriseService drugsEnterpriseService = ApplicationUtils.getRecipeService(DrugsEnterpriseService.class);
        map.put("checkEnterprise", drugsEnterpriseService.checkEnterprise(recipe.getClinicOrgan()));
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        PatientDTO patientBean = patientService.get(recipe.getMpiid());
        PatientDTO patient = null;
        if (patientBean != null) {
            //添加患者标签和关注这些字段
            RecipeServiceSub.setPatientMoreInfo(patientBean, recipe.getDoctor());
            patient = RecipeServiceSub.patientDesensitization(patientBean);
            //判断该就诊人是否为儿童就诊人
            if (patient.getAge() <= 5 && !ObjectUtils.isEmpty(patient.getGuardianCertificate())) {
                GuardianBean guardian = new GuardianBean();
                guardian.setName(patient.getGuardianName());
                try {
                    guardian.setAge(ChinaIDNumberUtil.getAgeFromIDNumber(patient.getGuardianCertificate()));
                    guardian.setSex(ChinaIDNumberUtil.getSexFromIDNumber(patient.getGuardianCertificate()));
                } catch (ValidateException exception) {
                    LOGGER.warn("监护人使用身份证号获取年龄或者性别出错.{}.", exception.getMessage(), exception);
                }
                map.put("guardian", guardian);
            }
            if (!ObjectUtils.isEmpty(patient.getGuardianCertificate())) {
                //对监护人信息进行脱敏处理
                patient.setGuardianCertificate(ChinaIDNumberUtil.hideIdCard(patient.getGuardianCertificate()));
            }
        }
        List<Recipedetail> recipedetails = detailDAO.findByRecipeId(recipeId);
        //中药处方处理
        if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType())) {
            if (CollectionUtils.isNotEmpty(recipedetails)) {
                Recipedetail recipedetail = recipedetails.get(0);
                recipe.setTcmUsePathways(recipedetail.getUsePathways());
                recipe.setTcmUsingRate(recipedetail.getUsingRate());
            }
        }
        map.put("patient", patient);
        map.put("recipedetails", RecipeValidateUtil.covertDrugUnitdoseAndUnit(RecipeValidateUtil.validateDrugsImplForDetail(recipe), isDoctor, recipe.getClinicOrgan()));
        //隐方
        boolean isHiddenRecipeDetail = false;
        if (isDoctor == false) {
            boolean isReturnRecipeDetail = recipeListService.isReturnRecipeDetail(recipe.getRecipeId());
            if (!isReturnRecipeDetail) {
                List<RecipeDetailBean> recipeDetailVOs = (List<RecipeDetailBean>) map.get("recipedetails");
                if (recipeDetailVOs != null && recipeDetailVOs.size() > 0) {
                    for (int j = 0; j < recipeDetailVOs.size(); j++) {
                        recipeDetailVOs.get(j).setDrugName(null);
                        recipeDetailVOs.get(j).setDrugSpec(null);
                    }
                }
            }
            isHiddenRecipeDetail = !isReturnRecipeDetail;
        }
        map.put("isHiddenRecipeDetail", isHiddenRecipeDetail);

        if (isDoctor) {
            ConsultSetService consultSetService = ApplicationUtils.getBasicService(ConsultSetService.class);
            // 获取处方单药品总价
            RecipeUtil.getRecipeTotalPriceRange(recipe, recipedetails);

            //date 20200506
            //通过订单的状态判断
            Map<String, Integer> orderStatus = new HashMap<>();
            if (null != recipe.getRecipeCode()) {
                List<RecipeOrder> recipeOrders = orderDAO.findValidListbyCodes(Arrays.asList(recipe.getOrderCode()));
                orderStatus = recipeOrders.stream().collect(Collectors.toMap(RecipeOrder::getOrderCode, RecipeOrder::getStatus));
            }
            Map<String, String> tipMap = RecipeServiceSub.getTipsByStatusCopy(recipe.getStatus(), recipe, null, (orderStatus == null || 0 >= orderStatus.size()) ? null : orderStatus.get(recipe.getOrderCode()));
            map.put("cancelReason", MapValueUtil.getString(tipMap, "cancelReason"));
            map.put("tips", MapValueUtil.getString(tipMap, "tips"));
            IRecipeAuditService recipeAuditService = RecipeAuditAPI.getService(IRecipeAuditService.class, "recipeAuditServiceImpl");
            //获取审核不通过详情
            List<Map<String, Object>> mapList = recipeAuditService.getCheckNotPassDetail(recipeId);
            if (!ObjectUtils.isEmpty(mapList)) {
                for (int i = 0; i < mapList.size(); i++) {
                    Map<String, Object> notPassMap = mapList.get(i);
                    List results = (List) notPassMap.get("checkNotPassDetails");
                    List<RecipeDetailBean> recipeDetailBeans = ObjectCopyUtil.convert(results, RecipeDetailBean.class);
                    try {
                        for (RecipeDetailBean recipeDetailBean : recipeDetailBeans) {
                            RecipeValidateUtil.setUsingRateIdAndUsePathwaysId(recipe, recipeDetailBean);
                        }
                    } catch (Exception e) {
                        LOGGER.error("RecipeServiceSub  setUsingRateIdAndUsePathwaysId error", e);
                    }
                }
            }
            map.put("reasonAndDetails", mapList);

            //设置处方撤销标识 true:可以撤销, false:不可撤销
            Boolean cancelFlag = false;
            if (RecipeStatusConstant.REVOKE != recipe.getStatus()) {
                if (!Integer.valueOf(1).equals(recipe.getPayFlag()) && recipe.getStatus() != RecipeStatusConstant.UNSIGN && recipe.getStatus() != RecipeStatusConstant.HIS_FAIL && recipe.getStatus() != RecipeStatusConstant.NO_DRUG && recipe.getStatus() != RecipeStatusConstant.NO_PAY && recipe.getStatus() != RecipeStatusConstant.NO_OPERATOR && recipe.getStatus() != RecipeStatusConstant.RECIPE_MEDICAL_FAIL && recipe.getStatus() != RecipeStatusConstant.CHECKING_HOS && recipe.getStatus() != RecipeStatusConstant.NO_MEDICAL_INSURANCE_RETURN
                        //date 2020/05/14
                        //将签名失败和审核失败的
                        && recipe.getStatus() != RecipeStatusConstant.SIGN_ERROR_CODE_PHA && recipe.getStatus() != RecipeStatusConstant.SIGN_ERROR_CODE_DOC && recipe.getStatus() != RecipeStatusConstant.SIGN_ING_CODE_PHA && !Integer.valueOf(1).equals(recipe.getChooseFlag())
                        && recipe.getStatus() != RecipeStatusConstant.SIGN_NO_CODE_PHA) {
                    cancelFlag = true;
                }
            }
            map.put("cancelFlag", cancelFlag);
            //能否开医保处方
            boolean medicalFlag = false;
            ConsultSetDTO set = consultSetService.getBeanByDoctorId(recipe.getDoctor());
            if (null != set && null != set.getMedicarePrescription()) {
                medicalFlag = (true == set.getMedicarePrescription()) ? true : false;
            }
            map.put("medicalFlag", medicalFlag);
            if (null != recipe.getChecker() && recipe.getChecker() > 0) {
                String ysTel = doctorService.getMobileByDoctorId(recipe.getChecker());
                if (StringUtils.isNotEmpty(ysTel)) {
                    recipe.setCheckerTel(ysTel);
                }
            }

            //审核不通过处方单详情增加二次签名标记
            //date 20191011
            //添加一次审核不通过标志位,取消之前通过订单是否有效的判断
            boolean b = RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus() && (recipe.canMedicalPay() || (RecipecCheckStatusConstant.First_Check_No_Pass == recipe.getCheckStatus()));
            if (b) {
                map.put("secondSignFlag", canSecondAudit(recipe.getClinicOrgan()));
            }

            //增加医生返回智能审方结果药品问题列表 2018.11.26 shiyp
            //判断开关是否开启
            PrescriptionService prescriptionService = ApplicationUtils.getRecipeService(PrescriptionService.class);
            if (recipe.getStatus() != 0) {
                if (prescriptionService.getIntellectJudicialFlag(recipe.getClinicOrgan()) == 1) {
                    List<AuditMedicinesBean> auditMedicines = getAuditMedicineIssuesByRecipeId(recipeId);
                    map.put("medicines", getAuditMedicineIssuesByRecipeId(recipeId)); //返回药品分析数据
//                AuditMedicineIssueDAO auditMedicineIssueDAO = DAOFactory.getDAO(AuditMedicineIssueDAO.class);
                    List<eh.recipeaudit.model.AuditMedicineIssueBean> auditMedicineIssues = iAuditMedicinesService.findIssueByRecipeId(recipeId);
                    if (CollectionUtils.isNotEmpty(auditMedicineIssues)) {
                        List<AuditMedicineIssueBean> resultMedicineIssues = new ArrayList<>();
                        auditMedicineIssues.forEach(item -> {
                            if (null == item.getMedicineId()) {
                                resultMedicineIssues.add(item);
                            }
                        });

                        List<PAWebRecipeDanger> recipeDangers = new ArrayList<>();
                        resultMedicineIssues.forEach(item -> {
                            PAWebRecipeDanger recipeDanger = new PAWebRecipeDanger();
                            recipeDanger.setDangerDesc(item.getDetail());
                            recipeDanger.setDangerDrug(item.getTitle());
                            recipeDanger.setDangerLevel(item.getLvlCode());
                            recipeDanger.setDangerType(item.getLvl());
                            recipeDanger.setDetailUrl(item.getDetailUrl());
                            recipeDangers.add(recipeDanger);
                        });
                        map.put("recipeDangers", recipeDangers); //返回处方分析数据
                    }
                }
            }
            //医生处方单详情页按钮显示
            doctorRecipeInfoBottonShow(map, recipe);
        } else {
            //处方详情单底部文案提示说明---机构配置
            map.put("bottomText", getBottomTextForPatient(recipe.getClinicOrgan()));
            RecipeOrder order = orderDAO.getOrderByRecipeId(recipeId);
            if (recipe.getRecipeMode() == RecipeBussConstant.RECIPEMODE_ZJJGPT) {
                map.put("tips", getTipsByStatusForPatient(recipe, order));
            } else {
                PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
                map.put("tips", purchaseService.getTipsByStatusForPatient(recipe, order));
            }
            //获取医生撤销原因
            if (recipe.getStatus() == RecipeStatusConstant.REVOKE) {
                map.put("cancelReason", getCancelReasonForPatient(recipeId));
            }
            boolean b = null != recipe.getEnterpriseId() && RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode()) && (recipe.getStatus() == RecipeStatusConstant.WAIT_SEND || recipe.getStatus() == RecipeStatusConstant.IN_SEND || recipe.getStatus() == RecipeStatusConstant.FINISH);
            if (b) {
                DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                map.put("depTel", drugsEnterpriseDAO.getTelById(recipe.getEnterpriseId()));
            }

            recipe.setOrderAmount(recipe.getTotalMoney());
            BigDecimal actualPrice = null;
            if (null != order) {
                //合并处方这里要改--得重新计算药品费用不能从order里取
                //actualPrice = order.getRecipeFee();
                if (order.getEnterpriseId() != null) {
                    RecipeOrderService recipeOrderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                    actualPrice = recipeOrderService.reCalculateRecipeFee(order.getEnterpriseId(), Arrays.asList(recipeId), null);
                }
                recipe.setDiscountAmount(order.getCouponName());
            } else {
                // couponId = -1有优惠券  不使用 显示“不使用优惠券”
                actualPrice = recipe.getActualPrice();
                recipe.setDiscountAmount("0.0");

                //如果获取不到有效订单，则不返回订单编号（场景：医保消息发送成功后，处方单关联了一张无效订单，此时处方单点击自费结算，应跳转到订单确认页面）
                recipe.setOrderCode(null);
            }
            if (null == actualPrice) {
                actualPrice = recipe.getTotalMoney();
            }
            recipe.setActualPrice(actualPrice);

            //无法配送时间文案提示
            map.put("unSendTitle", getUnSendTitleForPatient(recipe, order));
            //患者处方取药方式提示
            map.put("recipeGetModeTip", getRecipeGetModeTip(recipe));

            if (null != order && 1 == order.getEffective() && StringUtils.isNotEmpty(recipe.getOrderCode())) {
                //如果创建过自费订单，则不显示医保支付
                recipe.setMedicalPayFlag(0);
            }
            //返回前端是否能合并支付的按钮--提示可以合并支付----可能患者从消息进去到处方详情时
            Boolean mergeRecipeFlag = false;
            try {
                if (StringUtils.isEmpty(recipe.getOrderCode()) && StringUtils.isNotEmpty(recipeExtend.getRegisterID()) && !RecipeStatusEnum.RECIPE_STATUS_NO_OPERATOR.getType().equals(recipe.getStatus())) {
                    GroupRecipeConfDTO groupRecipeConfDTO = groupRecipeManager.getMergeRecipeSetting();
                    mergeRecipeFlag = groupRecipeConfDTO.getMergeRecipeFlag();
                    if (mergeRecipeFlag) {
                        String mergeRecipeWay = (String) configService.getConfiguration(recipe.getClinicOrgan(), "mergeRecipeWay");
                        Integer numCanMergeRecipe = recipeDAO.getNumCanMergeRecipeByMergeRecipeWay(recipe.getMpiid(), recipeExtend.getRegisterID(), recipe.getClinicOrgan(), mergeRecipeWay, recipeExtend.getChronicDiseaseName());
                        LOGGER.info("RecipeServiceSub getRecipeAndDetailByIdImpl recipeId:{},numCanMergeRecipe:{}.", recipe.getRecipeId(), numCanMergeRecipe);
                        //获取能合并处方的单数大于1的时候才能跳转列表页
                        if (numCanMergeRecipe <= 1) {
                            mergeRecipeFlag = false;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("RecipeServiceSub.getRecipeAndDetailByIdImpl error, recipeId:{}", recipeId, e);
                mergeRecipeFlag = false;
            }
            map.put("mergeRecipeFlag", mergeRecipeFlag);
            //Explain:审核是否通过
            boolean isOptional = !(RecipeStatusEnum.getCheckShowFlag(recipe.getStatus()) ||
                    RecipecCheckStatusConstant.First_Check_No_Pass.equals(recipe.getCheckStatus()) && ReviewTypeConstant.Preposition_Check == recipe.getReviewType());
            map.put("optional", isOptional);

            //date 2190929
            //添加处方详情上提示信息的展示颜色类型
            //添加一次审核不通过，状态待审核
            Integer recipestatus = recipe.getStatus();
            if (RecipecCheckStatusConstant.First_Check_No_Pass == recipe.getCheckStatus()) {
                recipestatus = RecipeStatusConstant.READY_CHECK_YS;
            }
            RecipeTipesColorTypeEnum colorType = RecipeTipesColorTypeEnum.fromRecipeStatus(recipestatus);
            if (null != colorType) {
                map.put("tipsType", colorType.getShowType());
            }
            //患者处方单详情页按钮显示
            patientRecipeInfoButtonShowNew(map, recipe, order);
            patientRecipeInfoBottonShow(map, recipe, order);
        }


        //设置失效时间
        if (RecipeStatusConstant.CHECK_PASS == recipe.getStatus()) {
            recipe.setRecipeSurplusHours(getRecipeSurplusHours(recipe.getSignDate()));
        }

        //获取该医生所在科室，判断是否为儿科科室
        Integer departId = recipe.getDepart();
        DepartmentDTO departmentDTO = departmentService.get(departId);
        Boolean childRecipeFlag = false;
        if (!ObjectUtils.isEmpty(departmentDTO)) {
            if (departmentDTO.getName().contains("儿科") || departmentDTO.getName().contains("新生儿科") || departmentDTO.getName().contains("儿内科") || departmentDTO.getName().contains("儿外科")) {
                childRecipeFlag = true;
            }
        }
        map.put("childRecipeFlag", childRecipeFlag);


        //慢病列表配置
        try {
            IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            Integer recipeChooseChronicDisease = (Integer) configurationService.getConfiguration(recipe.getClinicOrgan(), "recipeChooseChronicDisease");
            map.put("recipeChooseChronicDisease", recipeChooseChronicDisease);
        } catch (Exception e) {
            LOGGER.error("RecipeServiceSub.getRecipeAndDetailByIdImpl 获取慢病配置error, recipeId:{}", recipeId, e);
        }


        //设置订单信息
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            map.put("recipeOrder", recipeOrder);
        }
        //设置签名图片
        Map<String, String> signInfo = attachSealPic(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getChecker(), recipeId);
        if (StringUtils.isNotEmpty(signInfo.get("doctorSignImg"))) {
            map.put("doctorSignImg", signInfo.get("doctorSignImg"));
            map.put("doctorSignImgToken", FileAuth.instance().createToken(signInfo.get("doctorSignImg"), 3600L));
        }
        //设置药师手签图片id-----药师撤销审核结果/CA签名中/签名失败/未签名 不应该显示药师手签
        if (StringUtils.isNotEmpty(signInfo.get("checkerSignImg"))) {
            if (recipe.getStatus() != RecipeStatusConstant.READY_CHECK_YS) {
                if (!(recipe.getStatus() == RecipeStatusConstant.SIGN_ERROR_CODE_PHA ||
                        recipe.getStatus() == RecipeStatusConstant.SIGN_ING_CODE_PHA ||
                        recipe.getStatus() == RecipeStatusConstant.SIGN_NO_CODE_PHA)) {
                    map.put("checkerSignImg", signInfo.get("checkerSignImg"));
                    map.put("checkerSignImgToken", FileAuth.instance().createToken(signInfo.get("checkerSignImg"), 3600L));
                }
            }
        } else {
            if (recipe.getStatus() != RecipeStatusConstant.READY_CHECK_YS && recipe.getRecipeSourceType().equals(2) && !ValidateUtil.integerIsEmpty(recipe.getChecker())) {
                if (!(recipe.getStatus() == RecipeStatusConstant.SIGN_ERROR_CODE_PHA ||
                        recipe.getStatus() == RecipeStatusConstant.SIGN_ING_CODE_PHA ||
                        recipe.getStatus() == RecipeStatusConstant.SIGN_NO_CODE_PHA)) {
                    DoctorDTO defaultDoctor = doctorService.get(recipe.getChecker());
                    map.put("checkerSignImg", defaultDoctor.getSignImage());
                    map.put("checkerSignImgToken", FileAuth.instance().createToken(defaultDoctor.getSignImage(), 3600L));
                }
            }
        }
        //获取药师撤销原因
        if (recipe.getStatus() == RecipeStatusConstant.READY_CHECK_YS && ReviewTypeConstant.Preposition_Check.equals(recipe.getReviewType())) {
            map.put("cancelReason", getCancelReasonForChecker(recipeId));
        }
        //Date:2019/12/16
        //Explain:添加判断展示处方参考价格
        //获取处方下的药品，判断是否有药品对应的医院药品金额为空，有的话不展示参考价格
        boolean flag = getShowReferencePriceFlag(recipe, recipedetails);
        map.put("showReferencePrice", flag);

        //Date:20200226
        //添加展示药师签名判断
        //1.不设置二次审核，审核通过展示；
        //2.设置二次审核，一次通过展示（没有审核不通过日志的且审核通过的）
        //总结的来说就是只要审核通过的并且没有不通过记录就展示
        boolean showChecker = isShowChecker(recipeId, recipe);
        if (recipe.getCheckMode() != null && recipe.getCheckMode() == 2) {
            //TODO HIS审方不显示药师签名
            showChecker = false;
        }
        map.put("showChecker", showChecker);

        //医生端/患者端获取处方扩展信息
        if (recipeExtend != null) {
            if (recipeExtend.getDecoctionId() != null && recipeExtend.getDecoctionText() != null) {
                DrugDecoctionWayDao DecoctionWayDao = DAOFactory.getDAO(DrugDecoctionWayDao.class);
                DecoctionWay decoctionWay = DecoctionWayDao.get(Integer.valueOf(recipeExtend.getDecoctionId()));
                if (decoctionWay != null && decoctionWay.getDecoctionPrice() != null) {
                    recipeExtend.setDecoctionPrice(decoctionWay.getDecoctionPrice());
                }
            }
            //EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
            map.put("recipeExtend", recipeExtend);
        }
        RecipeBean recipeBean = ObjectCopyUtils.convert(recipe, RecipeBean.class);
        recipeBean.setGiveModeText(GiveModeFactory.getGiveModeBaseByRecipe(recipe).getGiveModeTextByRecipe(recipe));
        if (recipe.getRecipeSourceType().equals(1) && null != recipeBean.getChecker() && StringUtils.isEmpty(recipeBean.getCheckerText())) {
            recipeBean.setCheckerText(DictionaryUtil.getDictionary("eh.base.dictionary.Doctor", recipeBean.getChecker()));
        }

        //线下转线上的处方  设置默认审方药师
        if (recipe.getRecipeSourceType().equals(2) && recipe.getChecker() == null) {
            IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            String doctorId = (String) configurationService.getConfiguration(recipe.getClinicOrgan(), "offlineDefaultRecipecheckDoctor");
            if (doctorId != null) {
                Map<String, String> signInfoDefault = attachSealPic(recipe.getClinicOrgan(), recipe.getDoctor(), Integer.valueOf(doctorId), recipeId);
                //该默认药师在平台的签名是有值的
                if (StringUtils.isNotEmpty(signInfoDefault.get("checkerSignImg"))) {
                    LOGGER.info("signInfoDefault:{}", JSONUtils.toString(signInfoDefault));
                    map.put("checkerSignImg", signInfoDefault.get("checkerSignImg"));
                    map.put("checkerSignImgToken", FileAuth.instance().createToken(signInfoDefault.get("checkerSignImg"), 3600L));
                } else {
                    DoctorDTO defaultDoctor = doctorService.get(Integer.valueOf(doctorId));
                    LOGGER.info("defaultDoctor:{}", JSONUtils.toString(signInfoDefault));
                    map.put("checkerSignImg", defaultDoctor.getSignImage());
                    map.put("checkerSignImgToken", FileAuth.instance().createToken(defaultDoctor.getSignImage(), 3600L));
                }
            }
        }

        //处理审核药师
        if ((recipe.getStatus() == RecipeStatusConstant.SIGN_ERROR_CODE_PHA ||
                recipe.getStatus() == RecipeStatusConstant.SIGN_ING_CODE_PHA ||
                recipe.getStatus() == RecipeStatusConstant.SIGN_NO_CODE_PHA || recipe.getStatus() == RecipeStatusConstant.READY_CHECK_YS) ||
                (recipe.getRecipeSourceType().equals(2) && !StringUtils.isNotEmpty(signInfo.get("checkerSignImg")))
        ) {
            recipeBean.setCheckerText("");
        }
        if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType())) {
            //处理线下转线上的代煎费
            if (new Integer(2).equals(recipe.getRecipeSourceType())) {
                //表示为线下的处方
                HisRecipeDAO hisRecipeDAO = DAOFactory.getDAO(HisRecipeDAO.class);
                HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
                //设置代煎费
//                if (hisRecipe != null && hisRecipe.getDecoctionFee() != null) {
//                    //说明线下处方有代煎费
//                    recipeBean.setDecoctionFee(hisRecipe.getDecoctionFee());
//                }

                String decoctionDeploy = ((String[]) configService.getConfiguration(recipe.getClinicOrgan(), "decoctionDeploy"))[0];
                //用于确认订单页显示线下处方代煎费 兼容老版本（修复老版本的bug）
                //如果为医生选择且recipeExt存在decoctionText
                if ("1".equals(decoctionDeploy)
                        && recipeExtend != null && StringUtils.isNotEmpty(recipeExtend.getDecoctionText())) {
                    if (hisRecipe != null && hisRecipe.getDecoctionFee() != null) {
                        //有代煎总额
                        recipeBean.setDecoctionFee(hisRecipe.getDecoctionFee());
                    } else {
                        //无代煎总额 需计算代煎总额=贴数*代煎单价
                        if (hisRecipe.getDecoctionUnitFee() != null && recipe.getCopyNum() != null) {
                            //代煎费等于剂数乘以代煎单价
                            recipeBean.setDecoctionFee(hisRecipe.getDecoctionUnitFee().multiply(BigDecimal.valueOf(recipe.getCopyNum())));
                        }
                    }
                }

            }
        }
        recipeBean.setCheckerTel(LocalStringUtil.coverMobile(recipeBean.getCheckerTel()));
        map.put("recipe", recipeBean);
        //20200519 zhangx 是否展示退款按钮(重庆大学城退款流程)，前端调用patientRefundForRecipe
        map.put("showRefund", 0);
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String patient_refund_organList = recipeParameterDao.getByName("patient_refund_organList");
        List<Integer> organIds = JSONUtils.parse(patient_refund_organList, ArrayList.class);
        if (organIds != null && organIds.contains(recipe.getClinicOrgan())) {
            map.put("showRefund", 1);
        }

        //对北京互联网流转模式处理
        if (new Integer(2).equals(recipe.getRecipeSource())) {
            HisRecipeDAO hisRecipeDAO = DAOFactory.getDAO(HisRecipeDAO.class);
            HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
            if (hisRecipe != null && new Integer(2).equals(hisRecipe.getMedicalType())) {
                map.put("medicalType", 2);
            }
        }

        map.put("qrName", recipeManager.getToHosProof(recipe, recipeExtend));
        if (recipe.getEnterpriseId() != null) {
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipe.getEnterpriseId());
            if (drugsEnterprise != null && drugsEnterprise.getSendType() != null) {
                map.put("sendType", drugsEnterprise.getSendType());
            }
        }
        LOGGER.info("getRecipeAndDetailByIdImpl map : {}", JSONUtils.toString(map));
        return map;
    }

    /**
     * 根据配置项sealDataFrom获取签章图片
     *
     * @param doctorId
     * @param
     * @Author liumin
     */
    public static Map<String, String> attachSealPic(Integer clinicOrgan, Integer doctorId, Integer checker, Integer recipeId) {
        Map<String, String> map = new HashMap<>();
        AttachSealPicDTO sttachSealPicDTO = signManager.attachSealPic(clinicOrgan, doctorId, checker, recipeId);
        map.put("doctorSignImg", sttachSealPicDTO.getDoctorSignImg());
        map.put("checkerSignImg", sttachSealPicDTO.getCheckerSignImg());
        return map;
    }

    private static void doctorRecipeInfoBottonShow(Map<String, Object> map, Recipe recipe) {
        //按钮枚举
        for (DoctorRecipePageButtonStatusEnum e : DoctorRecipePageButtonStatusEnum.values()) {
            map.put(e.getButtonName(), e.getStatusList().contains(recipe.getStatus()));
            if ("continueOpenRecipeFlag".equals(e.getButtonName()) && e.getStatusList().contains(recipe.getStatus())) {
                map.put("continueOpenRecipeFlag", canShowContinueSignFlag(recipe));
            }
        }
    }

    private static boolean canShowContinueSignFlag(Recipe recipe) {
        IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        return (Boolean) configurationService.getConfiguration(recipe.getClinicOrgan(), "continueOpenRecipeFlag") && StringUtils.isEmpty(recipe.getOrderCode());
    }

    private static void patientRecipeInfoButtonShowNew(Map<String, Object> map, Recipe recipe, RecipeOrder order) {
        //是否可以下载处方签
        map.put("isDownload", getDownConfig(recipe, order));
        GiveModeShowButtonVO giveModeShowButtonVO;
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        IGiveModeBase giveModeBase = GiveModeFactory.getGiveModeBaseByRecipe(recipe);
        try {
            //校验数据
            giveModeBase.validRecipeData(recipe);
        } catch (Exception e) {
            LOGGER.error("patientRecipeInfoBottonShowNew error:{}.", e.getMessage());
            return;
        }
        //从运营平台获取配置项
        giveModeShowButtonVO = giveModeBase.getGiveModeSettingFromYypt(recipe.getClinicOrgan());
        //设置按钮是否可点击
        giveModeBase.setButtonOptional(giveModeShowButtonVO, recipe);
        //设置按钮展示类型
        giveModeBase.setButtonType(giveModeShowButtonVO, recipe);
        //设置其他按钮
        giveModeBase.setOtherButton(giveModeShowButtonVO, recipe);
        //设置特殊按钮
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        giveModeBase.setSpecialItem(giveModeShowButtonVO, recipe, recipeExtend);
        giveModeBase.afterSetting(giveModeShowButtonVO, recipe);
        giveModeBase.setShowButton(giveModeShowButtonVO, recipe);
        map.put("giveModeShowButtonVO", giveModeShowButtonVO);
        GiveModeButtonBean giveModeButtonBean = getShowThirdOrder(recipe);
        if (null != giveModeButtonBean) {
            map.put("showThirdOrder", giveModeButtonBean);
        }
    }

    private static GiveModeButtonBean getShowThirdOrder(Recipe recipe) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        //设置第三方订单跳转的按钮
        Integer enterpriseId = recipe.getEnterpriseId();
        if (null == enterpriseId) {
            return null;
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(enterpriseId);
        if (null == drugsEnterprise.getOrderType() || new Integer(1).equals(drugsEnterprise.getOrderType())) {
            return null;
        }
        if (RecipeStatusEnum.RECIPE_STATUS_WAIT_SEND.getType().equals(recipe.getStatus())
                || RecipeStatusEnum.RECIPE_STATUS_IN_SEND.getType().equals(recipe.getStatus())
                || RecipeStatusEnum.RECIPE_STATUS_FINISH.getType().equals(recipe.getStatus())
                || RecipeStatusEnum.RECIPE_STATUS_REVOKE.getType().equals(recipe.getStatus())
                || RecipeStatusEnum.RECIPE_STATUS_HAVE_PAY.getType().equals(recipe.getStatus())) {
            //orderType=0表示订单在第三方生成
            GiveModeButtonBean giveModeButton = new GiveModeButtonBean();
            giveModeButton.setButtonSkipType("3");
            giveModeButton.setShowButtonName("查看订单");
            giveModeButton.setShowButtonKey("supportThirdOrder");
            return giveModeButton;
        }
        return null;
    }

    private static void patientRecipeInfoBottonShow(Map<String, Object> map, Recipe recipe, RecipeOrder order) {
        //Date:20190904
        //Explain:添加患者点击按钮信息
        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode()) || RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
            //获取配置项
            IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
            //添加按钮配置项key
            Object payModeDeploy = configService.getConfiguration(recipe.getClinicOrgan(), "payModeDeploy");
            if (null != payModeDeploy) {
                List<String> configurations = new ArrayList<>(Arrays.asList((String[]) payModeDeploy));
                //将配置的购药方式放在map上
                for (String configuration : configurations) {
                    map.put(configuration, 1);
                }
            }

            //互联网按钮信息（特殊化）
            if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
                PayModeShowButtonBean payModeShowButtonBean = new PayModeShowButtonBean();
                if (map.get("supportOnline") != null && 1 == (Integer) map.get("supportOnline")) {
                    payModeShowButtonBean.setSupportOnline(true);
                }
                if (map.get("supportToHos") != null && 1 == (Integer) map.get("supportToHos")) {
                    payModeShowButtonBean.setSupportToHos(true);
                }

                //如果运营平台没设置按钮，那底下也不用走了
                if (payModeShowButtonBean.getSupportOnline() || payModeShowButtonBean.getSupportToHos()) {
                    RecipeListService recipeListService = new RecipeListService();
                    recipeListService.initInternetModel(null, payModeShowButtonBean, recipe);

                    if (null != payModeShowButtonBean.getSupportToHos() && !payModeShowButtonBean.getSupportToHos()) {
                        map.put("supportToHos", 0);
                    }
                    if (null != payModeShowButtonBean.getSupportOnline() && !payModeShowButtonBean.getSupportOnline()) {
                        map.put("supportOnline", 0);
                    }
                }
            }
        }
        //Date:20190909
        //Explain:判断是否下载处方签

        //1.判断配置项中是否配置了下载处方签，
        //2.是否是后置的，后置的判断状态是否是已审核，已完成, 配送中，
        //3.如果不是后置的，判断实际金额是否为0：为0则ordercode关联则展示，不为0支付则展示
        boolean isDownload = getDownConfig(recipe, order);
        map.put("isDownload", isDownload);
        //date 20200508
        //设置展示配送到家的配送方式
        //判断当前处方对应的机构支持的配送药企包含的配送类型

        //首先判断按钮中配送药品购药方式是否展示，不展示购药方式按钮就不展示药企配送和医院配送
        boolean showSend = (null == map.get("supportOnline") ? false : 1 == Integer.parseInt(map.get("supportOnline").toString()));
        map.put("showSendToEnterprises", 0);
        map.put("showSendToHos", 0);
        //显示配送才判断具体显示哪个配送按钮
        if (showSend) {
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            List<Integer> payModeSupport = RecipeServiceSub.getDepSupportMode(RecipeBussConstant.PAYMODE_ONLINE);
            payModeSupport.addAll(RecipeServiceSub.getDepSupportMode(RecipeBussConstant.PAYMODE_COD));
            Long enterprisesSend = drugsEnterpriseDAO.getCountByOrganIdAndPayModeSupportAndSendType(recipe.getClinicOrgan(), payModeSupport, EnterpriseSendConstant.Enterprise_Send);
            Long hosSend = drugsEnterpriseDAO.getCountByOrganIdAndPayModeSupportAndSendType(recipe.getClinicOrgan(), payModeSupport, EnterpriseSendConstant.Hos_Send);
            if (null != enterprisesSend && 0 < enterprisesSend) {
                map.put("showSendToEnterprises", 1);
            }
            if (null != hosSend && 0 < hosSend) {
                map.put("showSendToHos", 1);
            }
            //不支持配送，则按钮都不显示--包括药店取药
            if (RecipeDistributionFlagEnum.HOS_HAVE.getType().equals(recipe.getDistributionFlag())) {
                map.put("showSendToEnterprises", 0);
                map.put("showSendToHos", 0);
                map.put("supportTFDS", 0);
                map.put("supportOnline", 0);
                map.put("supportToHos", 0);
            }
            if (RecipeDistributionFlagEnum.DRUGS_HAVE.getType().equals(recipe.getDistributionFlag())) {
                map.put("supportToHos", 0);
            }
        }
        //date 20200724 北京互联网按钮展示根据HIS进行判断
        if (new Integer(2).equals(recipe.getRecipeSource())) {
            HisRecipeDAO hisRecipeDAO = DAOFactory.getDAO(HisRecipeDAO.class);
            HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
            //只有北京互联网医院DeliveryCode是不为空的
            if (hisRecipe != null && StringUtils.isNotEmpty(hisRecipe.getDeliveryCode())) {
                map.put("supportDownload", 0);
                map.put("supportToHos", 0);
                map.put("showSendToHos", 0);
                map.put("showSendToEnterprises", 0);
                map.put("supportTFDS", 0);
                if (new Integer(1).equals(recipe.getGiveMode())) {
                    //表示配送到家,需要判断是药企配送还是医院配送
                    DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                    DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getByAccount(hisRecipe.getDeliveryCode());
                    if (drugsEnterprise != null && new Integer(1).equals(drugsEnterprise.getSendType())) {
                        //表示为医院配送
                        map.put("showSendToHos", 1);
                    } else {
                        //表示为药企配送
                        map.put("showSendToEnterprises", 1);
                    }
                } else if (new Integer(2).equals(recipe.getGiveMode())) {
                    //表示到院取药
                    map.put("supportToHos", 1);

                } else if (new Integer(3).equals(recipe.getGiveMode())) {
                    //表示到店取药
                    map.put("supportTFDS", 1);
                }
            }
        }
        //临沭县人民医院医保支付按钮--点击跳转到东软h5页面
        if (recipe.getClinicOrgan() == 1002753) {
            PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
            if (purchaseService.isMedicarePatient(1002753, recipe.getMpiid())) {
                map.put("supportMedicalPayment", 1);
            }
        }
        //date 2191011
        //添加处方详情上是否展示按钮
        boolean showButton = false;
        if (!((null == map.get("supportTFDS") || 0 == Integer.parseInt(map.get("supportTFDS").toString()))
                && (null == map.get("supportOnline") || 0 == Integer.parseInt(map.get("supportOnline").toString()))
                && (null == map.get("supportDownload") || 0 == Integer.parseInt(map.get("supportDownload").toString()))
                && (null == map.get("supportToHos") || 0 == Integer.parseInt(map.get("supportToHos").toString()))
                && (null == map.get("supportMedicalPayment")))) {
            if (ReviewTypeConstant.Preposition_Check == recipe.getReviewType()) {
                //待药师审核，审核一次不通过，待处理无订单
                if ((RecipeStatusEnum.getCheckStatusFlag(recipe.getStatus()) || RecipecCheckStatusConstant.First_Check_No_Pass.equals(recipe.getCheckStatus())) && null == recipe.getOrderCode()) {
                    showButton = true;
                }
            } else {
                if (RecipeStatusConstant.CHECK_PASS == recipe.getStatus() && null == recipe.getOrderCode()) {
                    showButton = true;
                }
            }
        }

        map.put("showButton", showButton);
    }

    public static String getCancelReasonForChecker(int recipeId) {
        RecipeLogDAO recipeLogDAO = DAOFactory.getDAO(RecipeLogDAO.class);
        List<RecipeLog> recipeLogs = recipeLogDAO.findByRecipeIdAndAfterStatusDesc(recipeId, RecipeStatusConstant.READY_CHECK_YS);
        String cancelReason = "";
        if (CollectionUtils.isNotEmpty(recipeLogs)) {
            if (new Integer(RecipeStatusConstant.CHECK_PASS).equals(recipeLogs.get(0).getBeforeStatus())) {
                cancelReason = "药师已撤销审方结果," + recipeLogs.get(0).getMemo() + "。请耐心等待药师再次审核";
            }
        }
        return cancelReason;
    }

    private static String getCancelReasonForPatient(int recipeId) {
        String cancelReason = "";
        RecipeRefundDAO recipeRefundDAO = DAOFactory.getDAO(RecipeRefundDAO.class);
        if (CollectionUtils.isNotEmpty(recipeRefundDAO.findRefundListByRecipeId(recipeId))) {
            cancelReason = "由于患者申请退费成功，该处方已取消。";
        } else {
            RecipeLogDAO recipeLogDAO = DAOFactory.getDAO(RecipeLogDAO.class);
            List<RecipeLog> recipeLogs = recipeLogDAO.findByRecipeIdAndAfterStatusDesc(recipeId, RecipeStatusConstant.REVOKE);
            if (CollectionUtils.isNotEmpty(recipeLogs)) {
                cancelReason = "开方医生已撤销处方,撤销原因:" + recipeLogs.get(0).getMemo();
            }
        }

        return cancelReason;
    }

    private static boolean isShowChecker(int recipeId, Recipe recipe) {
        boolean showChecker = false;
        RecipeLogDAO recipeLogDAO = DAOFactory.getDAO(RecipeLogDAO.class);
        IRecipeCheckService recipeCheckService = RecipeAuditAPI.getService(IRecipeCheckService.class, "recipeCheckServiceImpl");
        RecipeCheckBean recipeCheckBean = recipeCheckService.getNowCheckResultByRecipeId(recipe.getRecipeId());
        LOGGER.info("当前处方已有审核记录{}", recipeId);
        //判断是否是通过的
        if (recipeCheckBean != null) {
            if (null != recipeCheckBean.getCheckStatus() && 1 == recipeCheckBean.getCheckStatus()) {
                LOGGER.info("当前处方已有审核通过记录{}", recipeId);
                //判断有没有不通过的记录，没有就说明是直接审核通过的
                List<RecipeLog> recipeLogs = recipeLogDAO.findByRecipeIdAndAfterStatus(recipeId, RecipeStatusConstant.CHECK_NOT_PASS_YS);
                if (CollectionUtils.isEmpty(recipeLogs)) {
                    LOGGER.info("当前处方已有审核通过中无审核不通过记录{}", recipeId);
                    showChecker = true;
                }
            }
        }
        return showChecker;
    }

    private static Object getBottomTextForPatient(Integer clinicOrgan) {
        try {
            IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            return configurationCenterUtilsService.getConfiguration(clinicOrgan, "RecipeInfoBottomText");
        } catch (Exception e) {
            LOGGER.error("getBottomTextForPatient error", e);
        }
        return null;
    }

    /**
     * @param recipe        查询的处方
     * @param recipedetails 处方对应的药品详情
     * @return boolean 是否展示参考价格
     * @method getShowReferencePriceFlag
     * @description 获取是否要展示参考价格
     * @date: 2019/12/17
     * @author: JRK
     */
    private static boolean getShowReferencePriceFlag(Recipe recipe, List<Recipedetail> recipedetails) {
        boolean flag = true;
        if (null == recipedetails || 0 >= recipedetails.size()) {
            flag = false;
        } else {
            //只要有一个药品的价格为空或0都不展示参考价格
            //date 2019/1/6
            //修改判断通过处方单详情中药品信息，如果价格有0则不显示价格
            for (Recipedetail recipedetail : recipedetails) {
                if (null == recipedetail) {
                    LOGGER.warn("当前机构{}下药品code{}的药品为空", recipe.getClinicOrgan(), recipedetail.getOrganDrugCode());
                    flag = false;
                    break;
                }
                if (null == recipedetail.getDrugCost() || 0 <= BigDecimal.ZERO.compareTo(recipedetail.getDrugCost())) {
                    LOGGER.info("当前机构药品{}的金额为空", recipedetail.getOrganDrugCode());
                    flag = false;
                    break;
                }
            }
        }
        return flag;
    }

    /**
     * @param recipe 当前处方
     * @param order  当前处方对应的订单
     * @return boolean 是否可以下载
     * @method getDownConfig
     * @description 获取下载处方签的配置
     * @date: 2019/9/10
     * @author: JRK
     */
    public static boolean getDownConfig(Recipe recipe, RecipeOrder order) {
        //互联网的不需要下载处方笺
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
            return false;
        }
        Boolean isDownload = false;
        //获取配置项
        IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        //添加按钮配置项key
        Object downloadPrescription = configService.getConfiguration(recipe.getClinicOrgan(), "downloadPrescription");
        //date 2020/1/9
        //逻辑修改成：如果是下载处方购药方式的，无需判断配不配置【患者展示下载处方笺】
        //非下载处方的购药方式，只有配置了【患者展示下载处方笺】才判断是否展示下载按钮
        if (RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(recipe.getGiveMode())) {
            isDownload = canDown(recipe, order, showDownloadRecipeStatus, true);
        } else {
            if (null != downloadPrescription) {
                boolean canDown = 0 != (Integer) downloadPrescription;
                if (canDown) {
                    isDownload = canDown(recipe, order, showRecipeStatus, false);
                } else {
                    //没有配置则不会展示下载按钮
                    isDownload = false;
                }
            }
        }
        return isDownload;
    }

    /**
     * @param recipe     当前处方
     * @param order      当前处方的订单
     * @param isDownLoad 是否是下载处方
     * @return boolean 是否可以下载处方签
     * @method canDown
     * @description 修改下载配置项
     * @date: 2019/9/10
     * @author: JRK
     */
    private static boolean canDown(Recipe recipe, RecipeOrder order, Integer[] status, Boolean isDownLoad) {
        boolean isDownload = false;
        //后置的时候判断处方的状态是一些状态的时候是展示按钮的
        if (ReviewTypeConstant.Postposition_Check == recipe.getReviewType()) {
            if (Arrays.asList(status).contains(recipe.getStatus())) {
                isDownload = true;
            }
        } else if (ReviewTypeConstant.Not_Need_Check == recipe.getReviewType() && RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(recipe.getGiveMode()) && RecipeStatusConstant.FINISH != recipe.getStatus()) {
            //这里当是不需审核，且选择的下载处方的购药方式的时候，没有产生订单，直接判断没有选定购药方式
            if (1 == recipe.getChooseFlag()) {
                isDownload = true;
            }
        } else {
            //如果实际金额为0则判断有没有关联ordercode，实际金额不为0则判断是否已经支付,展示下载处方签，
            //当下载处方购药时，已完成处方不展示下载处方签
            if (null != recipe.getOrderCode() && null != order && !(isDownLoad && RecipeStatusConstant.FINISH == recipe.getStatus())) {
                if (0 == order.getActualPrice() || (0 < order.getActualPrice() && 1 == recipe.getPayFlag())) {
                    isDownload = true;
                }

            }
        }
        return isDownload;
    }

    public static List<AuditMedicinesBean> getAuditMedicineIssuesByRecipeId(int recipeId) {
        List<AuditMedicinesBean> medicines = iAuditMedicinesService.findMedicinesByRecipeId(recipeId);
        List<AuditMedicinesBean> list = Lists.newArrayList();
        if (medicines != null && medicines.size() > 0) {
            list = ObjectCopyUtils.convert(medicines, AuditMedicinesBean.class);
            List<AuditMedicineIssueBean> issues = iAuditMedicinesService.findIssueByRecipeId(recipeId);
            if (issues != null && issues.size() > 0) {
                List<AuditMedicineIssueBean> issueList;
                for (AuditMedicinesBean auditMedicinesDTO : list) {
                    issueList = Lists.newArrayList();
                    for (AuditMedicineIssueBean auditMedicineIssue : issues) {
                        if (null != auditMedicineIssue.getMedicineId() && auditMedicineIssue.getMedicineId().equals(auditMedicinesDTO.getId())) {
                            issueList.add(auditMedicineIssue);
                        }
                    }
                    auditMedicinesDTO.setAuditMedicineIssues(ObjectCopyUtils.convert(issueList, AuditMedicineIssueBean.class));
                }
            }
        }
        return list;
    }

    /**
     * 状态文字提示（患者端）
     *
     * @param recipe
     * @return
     */
    public static String getTipsByStatusForPatient(Recipe recipe, RecipeOrder order) {
        Integer status = recipe.getStatus();
        Integer payMode = order.getPayMode();
        Integer payFlag = recipe.getPayFlag();
        Integer giveMode = recipe.getGiveMode();
        String orderCode = recipe.getOrderCode();
        String tips = "";
        RecipeRefundDAO recipeRefundDAO = DAOFactory.getDAO(RecipeRefundDAO.class);
        switch (status) {
            case RecipeStatusConstant.FINISH:
                tips = "处方单已完结.";
                break;
            case RecipeStatusConstant.HAVE_PAY:
                if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)) {
                    //配送到家
                    tips = "您已支付，药品将尽快为您配送.";
                } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode)) {
                    //医院取药
                    tips = "您已支付，请尽快到院取药.";
                }
                break;
            case RecipeStatusConstant.NO_PAY:
                tips = "由于您未及时缴费，该处方单已失效，请联系医生.";
                break;
            case RecipeStatusConstant.NO_DRUG:
                tips = "由于您未及时取药，该处方单已失效.";
                break;
            case RecipeStatusConstant.CHECK_PASS:
                if (null == payMode || null == giveMode) {
                    tips = "";
                } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode) && RecipeBussConstant.PAYMODE_OFFLINE.equals(payMode) && 0 == payFlag) {
                    tips = "您已选择到院支付，请及时缴费并取药.";
                }

                if (StringUtils.isNotEmpty(orderCode) && null != order && 1 == order.getEffective()) {
                    tips = "您已选择配送到家，请及时支付并取药.";
                }

                break;
            case RecipeStatusConstant.READY_CHECK_YS:
                tips = "处方正在审核中.";

                if (ReviewTypeConstant.Preposition_Check.equals(recipe.getReviewType())) {
                    String reason = RecipeServiceSub.getCancelReasonForChecker(recipe.getRecipeId());
                    if (StringUtils.isNotEmpty(reason)) {
                        tips = reason;
                    }
                }
                break;
            case RecipeStatusConstant.CHECK_PASS_YS:
                if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode) && RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
                    //在线支付
                    tips = "您已支付，药品将尽快为您配送.";
                } else if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode) && RecipeBussConstant.PAYMODE_OFFLINE.equals(payMode)) {
                    //货到付款
                    tips = "药品将尽快为您配送.";
                } else if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode)) {
                    tips = "请尽快前往药店取药.";
                } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode)) {
                    tips = "请尽快前往医院取药.";
                }
                break;
            case RecipeStatusConstant.IN_SEND:
                if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode) && RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
                    //在线支付
                    tips = "您已支付，药品正在配送中，请保持手机畅通.";
                } else if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode) && RecipeBussConstant.PAYMODE_OFFLINE.equals(payMode)) {
                    //货到付款
                    tips = "药品正在配送中，请保持手机畅通.";
                }
                break;
            case RecipeStatusConstant.CHECK_NOT_PASS_YS:
                tips = "由于未通过审核，该处方单已失效，请联系医生.";
                if (StringUtils.isNotEmpty(orderCode) && null != order && 1 == order.getEffective()) {
                    tips = "处方正在审核中.";
                }

                break;
            case RecipeStatusConstant.REVOKE:
                if (CollectionUtils.isNotEmpty(recipeRefundDAO.findRefundListByRecipeId(recipe.getRecipeId()))) {
                    tips = "由于患者申请退费成功，该处方已取消。";
                } else {
                    tips = "由于医生已撤销，该处方单已失效，请联系医生.";
                }
                break;
            //天猫特殊状态
            case RecipeStatusConstant.USING:
                tips = "处理中";
                break;
            case RecipeStatusConstant.SIGN_ING_CODE_PHA:
            case RecipeStatusConstant.SIGN_NO_CODE_PHA:
            case RecipeStatusConstant.SIGN_ERROR_CODE_PHA:
                tips = "处方正在审核中.";
                break;
            default:
                tips = "未知状态" + status;

        }
        return tips;
    }

    /**
     * 无法配送时间段文案提示
     * 处方单详情（待处理，待支付,药师未审核，状态为待配送,药师已审核，状态为待配送）
     */
    public static String getUnSendTitleForPatient(Recipe recipe, RecipeOrder order) {
        String unSendTitle = "";
        switch (recipe.getStatus()) {
            case RecipeStatusConstant.READY_CHECK_YS:
                if (!RecipeBussConstant.GIVEMODE_TFDS.equals(recipe.getGiveMode())
                        && !(RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode()) && RecipeBussConstant.PAYMODE_OFFLINE.equals(order.getPayMode()))) {
                    unSendTitle = cacheService.getParam(ParameterConstant.KEY_RECIPE_UNSEND_TIP);
                }
                //患者选择药店取药但是未点击下一步而返回处方单详情，此时payMode会变成4，增加判断条件
                if (RecipeBussConstant.GIVEMODE_TFDS.equals(recipe.getGiveMode()) && 0 == recipe.getChooseFlag()) {
                    unSendTitle = cacheService.getParam(ParameterConstant.KEY_RECIPE_UNSEND_TIP);
                }
                break;
            default:
                unSendTitle = "";
        }
        return unSendTitle;
    }

    /**
     * 患者处方取药方式提示
     */
    public static String getRecipeGetModeTip(Recipe recipe) {
        String recipeGetModeTip = "";
        // 该处方不是只能配送处方，可以显示 到院取药 的文案
        if (1 != recipe.getChooseFlag() && !(RecipeDistributionFlagEnum.DRUGS_HAVE.getType().equals(recipe.getDistributionFlag()))) {
            String organName = StringUtils.isEmpty(recipe.getOrganName()) ? "医院" : recipe.getOrganName();
            // 邵逸夫特殊处理院区
            if (1 == recipe.getClinicOrgan()) {
                organName = "浙大附属邵逸夫医院庆春院区";
            }
            recipeGetModeTip = cacheService.getParam(ParameterConstant.KEY_RECIPE_GETMODE_TIP);
            recipeGetModeTip = LocalStringUtil.processTemplate(recipeGetModeTip, ImmutableMap.of("orgName", organName));
        }
        return recipeGetModeTip;

    }

    /**
     * 获取处方失效剩余时间
     *
     * @param signDate
     * @return
     */
    public static String getRecipeSurplusHours(Date signDate) {
        String recipeSurplusHours = "0.1";
        if (null != signDate) {
            long startTime = Calendar.getInstance().getTimeInMillis();
            long endTime = DateConversion.getDateAftXDays(signDate, 3).getTime();
            if (endTime > startTime) {
                DecimalFormat df = new DecimalFormat("0.00");
                recipeSurplusHours = df.format((endTime - startTime) / (float) (1000 * 60 * 60));
            }
        }

        return recipeSurplusHours;
    }

    /**
     * 配送模式选择
     *
     * @param payMode
     * @return
     */
    public static List<Integer> getDepSupportMode(Integer payMode) {
        //具体见DrugsEnterprise的payModeSupport字段
        //配送模式支持 0:不支持 1:线上付款 2:货到付款 3:药店取药 8:货到付款和药店取药 9:都支持
        List<Integer> supportMode = new ArrayList<>();
        if (null == payMode) {
            return supportMode;
        }

        if (RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_ONLINE);
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS);
        } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode)) {
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_COD);
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_COD_TFDS);
        } else if (RecipeBussConstant.PAYMODE_TFDS.equals(payMode)) {
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_TFDS);
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_COD_TFDS);
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS);
        } else if (RecipeBussConstant.PAYMODE_MEDICAL_INSURANCE.equals(payMode)) {
            //医保选用线上支付配送方式
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_ONLINE);
        }

        if (CollectionUtils.isNotEmpty(supportMode)) {
            supportMode.add(RecipeBussConstant.DEP_SUPPORT_ALL);
        }

        return supportMode;
    }

    /**
     * 往咨询界面发送处方卡片
     *
     * @param recipe
     * @param details
     * @param rMap
     * @param send    true:发送卡片
     */
    public static void sendRecipeTagToPatient(Recipe recipe, List<Recipedetail> details, Map<String, Object> rMap, boolean send) {
        IConsultService iConsultService = ApplicationUtils.getConsultService(IConsultService.class);
        IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);
        getMedicalInfo(recipe);
        RecipeTagMsgBean recipeTagMsg = getRecipeMsgTag(recipe, details);
        //由于就诊人改造，已经可以知道申请人的信息，所以可以直接往当前咨询发消息
        if (StringUtils.isNotEmpty(recipe.getRequestMpiId()) && null != recipe.getDoctor()) {
            sendRecipeMsgTag(recipe.getRequestMpiId(), recipe, recipeTagMsg, rMap, send);
        } else if (StringUtils.isNotEmpty(recipe.getMpiid()) && null != recipe.getDoctor()) {
            //处方的患者编号在咨询单里其实是就诊人编号，不是申请人编号
            List<String> requestMpiIds;
            if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource())) {
                requestMpiIds = iRevisitService.findPendingConsultByMpiIdAndDoctor(recipe.getMpiid(), recipe.getDoctor());
            } else {
                requestMpiIds = iConsultService.findPendingConsultByMpiIdAndDoctor(recipe.getMpiid(), recipe.getDoctor());
            }

            if (CollectionUtils.isNotEmpty(requestMpiIds)) {
                for (String requestMpiId : requestMpiIds) {
                    sendRecipeMsgTag(requestMpiId, recipe, recipeTagMsg, rMap, send);
                }
            }
        }
    }

    private static void sendRecipeMsgTag(String requestMpiId, Recipe recipe, RecipeTagMsgBean recipeTagMsg, Map<String, Object> rMap, boolean send) {
        INetworkclinicMsgService iNetworkclinicMsgService = MessageAPI.getService(INetworkclinicMsgService.class);
        ConsultMessageService iConsultMessageService = MessageAPI.getService(ConsultMessageService.class);
        Integer consultId = recipe.getClinicId();
        Integer bussSource = recipe.getBussSource();
        if (consultId != null) {
            if (null != rMap && null == rMap.get("consultId")) {
                rMap.put("consultId", consultId);
                rMap.put("bussSource", bussSource);
            }
            if (send) {
                //11月大版本改造--咨询单或者网络门诊单是否正在处理中有他们那边判断
                LOGGER.info("sendRecipeMsgTag recipeTagMsg={}", JSONUtils.toString(recipeTagMsg));
                if (RecipeBussConstant.BUSS_SOURCE_WLZX.equals(bussSource)) {
                    iNetworkclinicMsgService.handleRecipeMsg(consultId, recipeTagMsg, recipe.getDoctor());
                } else if (RecipeBussConstant.BUSS_SOURCE_WZ.equals(bussSource)) {
                    iConsultMessageService.handleRecipeMsg(consultId, recipeTagMsg, recipe.getDoctor());
                } else if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(bussSource)) {
                    IRevisitMessageService revisitMessageService = MessageAPI.getService(IRevisitMessageService.class);
                    revisitMessageService.handleRecipeMsg(consultId, recipeTagMsg, recipe.getDoctor());
                }
            }
        }
    }

    /**
     * 获取处方卡片信息
     *
     * @param recipe
     * @param details
     * @return
     */
    private static RecipeTagMsgBean getRecipeMsgTag(Recipe recipe, List<Recipedetail> details) {
        IRecipeService recipeService = RecipeAPI.getService(IRecipeService.class);
        RecipeTagMsgBean recipeTagMsg = new RecipeTagMsgBean();
        if (new Integer(3).equals(recipe.getRecipeSourceType())) {
            recipeTagMsg.setTitle(recipe.getPatientName() + "的诊疗处方");
            Long count = recipeDetailManager.getCountByRecipeId(recipe.getRecipeId());
            recipeTagMsg.setContent("共" + count + "个项目");
            recipeTagMsg.setRecipeSourceType(recipe.getRecipeSourceType());
        } else {
            //获取诊断疾病名称
            String diseaseName = recipe.getOrganDiseaseName();
            List<String> drugNames = Lists.newArrayList();
            //取第一个药的药品显示拼接名
            drugNames.add(DrugNameDisplayUtil.dealwithRecipeDrugName(details.get(0), recipe.getRecipeType(), recipe.getClinicOrgan()));

            recipeTagMsg.setDiseaseName(diseaseName);
            recipeTagMsg.setDrugNames(drugNames);
            recipeTagMsg.setTitle(recipe.getPatientName() + "的电子处方单");
        }
        recipeTagMsg.setFlag(recipeService.getItemSkipType(recipe.getClinicOrgan()).get("itemList"));
        if (null != recipe.getRecipeId()) {
            recipeTagMsg.setRecipeId(recipe.getRecipeId());
        }
        LOGGER.info("RecipeServiceSub getRecipeMsgTag recipeTagMsg:{}.", JSON.toJSONString(recipeTagMsg));
        return recipeTagMsg;
    }

    /**
     * 往咨询界面发送处方卡片
     *
     * @param mpiId
     * @param organId
     * @param recipeCode
     * @param cardId
     * @param consultId
     * @param doctorId
     * @Author liumin
     */
    @RpcService
    public static void sendRecipeTagToPatientWithOfflineRecipe(String mpiId, Integer organId, String recipeCode, String cardId, Integer consultId, Integer doctorId) {

        RecipeTagMsgBean recipeTagMsg = new RecipeTagMsgBean();
        PatientService patientService = BasicAPI.getService(PatientService.class);
        PatientDTO patientDTO = patientService.getPatientBeanByMpiId(mpiId);
        if (StringUtils.isNotEmpty(cardId)) {
            patientDTO.setCardId(cardId);
        } else {
            patientDTO.setCardId("");
        }
        if (null == patientDTO) {
            throw new DAOException(609, "患者信息不存在");
        }
        String organName = "";
        try {
            organName = organService.getNameById(organId);
        } catch (Exception e) {
            LOGGER.info("getRecipeMsgTagWithOfflineRecipe getNameById error：{}", e);
            e.printStackTrace();
        }
        if (specitalOrganList.contains(organId.toString()) || organName.contains("上海市第七人民医院")) {
            recipeTagMsg = getRecipeMsgTagWithOfflineRecipe(patientDTO, true);
        } else {
            if (StringUtils.isEmpty(recipeCode)) {
                recipeTagMsg = getRecipeMsgTagWithOfflineRecipe(patientDTO, false);
            } else {
                //获取当前处方详情
                HisResponseTO<List<QueryHisRecipResTO>> hisResponseTO = hisRecipeManager.queryData(organId, patientDTO, null, 1, recipeCode);
                QueryHisRecipResTO queryHisRecipResTO = getRecipeInfoByRecipeCode(hisResponseTO, recipeCode);
                if (queryHisRecipResTO == null || StringUtils.isEmpty(queryHisRecipResTO.getRecipeCode())) {
                    LOGGER.info("sendRecipeTagToPatientWithOfflineRecipe recipeCode：{} 根据recipeCode没查询到线下处方！！！", recipeCode);
                    recipeTagMsg = getRecipeMsgTagWithOfflineRecipe(patientDTO, false);
                } else {
                    //拼接卡片显示参数
                    recipeTagMsg = getRecipeMsgTagWithOfflineRecipe(queryHisRecipResTO, patientDTO);
                }
            }
        }
        //环信消息发送
        LOGGER.info("sendRecipeTagToPatientWithOfflineRecipe revisitMessageService.handleRecipeMsg recipecode:{} param:[{},{},{}]", recipeCode, consultId, JSONUtils.toString(recipeTagMsg), doctorId);
        IRevisitMessageService revisitMessageService = MessageAPI.getService(IRevisitMessageService.class);
        revisitMessageService.handleRecipeMsg(consultId, recipeTagMsg, doctorId);
    }

    /**
     * 卡片消息显示参数拼接
     *
     * @param queryHisRecipResTO
     * @param patientDTO
     * @return
     */
    private static RecipeTagMsgBean getRecipeMsgTagWithOfflineRecipe(QueryHisRecipResTO queryHisRecipResTO, PatientDTO patientDTO) {
        LOGGER.info("getRecipeMsgTagWithOfflineRecipe param:{}", JSONUtils.toString(queryHisRecipResTO));
        //获取诊断疾病名称
        String diseaseName = queryHisRecipResTO.getDiseaseName();
        List<String> drugNames = Lists.newArrayList();
        //取第一个药的药品显示拼接名
        List<RecipeDetailTO> recipeDetailTOs = queryHisRecipResTO.getDrugList();
        RecipeDetailTO recipeDetailTO = new RecipeDetailTO();
        if (recipeDetailTOs != null && recipeDetailTOs.size() > 0) {
            recipeDetailTO = recipeDetailTOs.get(0);
        }
        RecipeTagMsgBean recipeTagMsg = new RecipeTagMsgBean();
        if (recipeDetailTO == null) {
            return recipeTagMsg;
        }
        //封装recipeDetail
        Recipedetail recipeDetail = new Recipedetail();
        recipeDetail.setDrugName(recipeDetailTO.getDrugName());
        if (!StringUtils.isEmpty(recipeDetailTO.getUseDose())) {
            try {
                recipeDetail.setUseDose(Double.valueOf(recipeDetailTO.getUseDose()));//高优先级
            } catch (Exception e) {
                recipeDetail.setUseDoseStr(recipeDetailTO.getUseDose() + recipeDetailTO.getUseDoseUnit());
            }
        }
        //  线下特殊用法
        if (!StringUtils.isEmpty(recipeDetailTO.getUseDoseStr())) {
            try {
                if (recipeDetail.getUseDose() == null) {
                    recipeDetail.setUseDose(Double.valueOf(recipeDetailTO.getUseDoseStr()));
                }
            } catch (Exception e) {
                recipeDetail.setUseDoseStr(recipeDetailTO.getUseDoseStr() + recipeDetailTO.getUseDoseUnit());//高优先级
            }
        }
        recipeDetail.setUseDoseUnit(recipeDetailTO.getUseDoseUnit());
        recipeDetail.setMemo(recipeDetailTO.getMemo());
        recipeDetail.setDrugSpec(recipeDetailTO.getDrugSpec());
        recipeDetail.setDrugUnit(recipeDetailTO.getDrugUnit());

        drugNames.add(DrugNameDisplayUtil.dealwithRecipeDrugName(recipeDetail, queryHisRecipResTO.getRecipeType(), queryHisRecipResTO.getClinicOrgan()));

        recipeTagMsg.setDiseaseName(diseaseName);
        recipeTagMsg.setDrugNames(drugNames);
        recipeTagMsg.setTitle(patientDTO.getPatientName() + "的电子处方单");
        //recipeTagMsg.setFlag(recipeService.getItemSkipType(queryHisRecipResTO.getClinicOrgan()).get("itemList"));
        recipeTagMsg.setFlag("1");
        if (null != queryHisRecipResTO.getRecipeCode()) {
            recipeTagMsg.setRecipeCode(queryHisRecipResTO.getRecipeCode());
        }
        recipeTagMsg.setCardId(patientDTO.getCardId());
        recipeTagMsg.setRecipeSourceType(2);
        LOGGER.info("getRecipeMsgTagWithOfflineRecipe response:{}", JSONUtils.toString(recipeTagMsg));
        return recipeTagMsg;
    }

    private static RecipeTagMsgBean getRecipeMsgTagWithOfflineRecipe(PatientDTO patientDTO, boolean dumpMzjf) {
        LOGGER.info("getRecipeMsgTagWithOfflineRecipe param:{}", JSONUtils.toString(patientDTO));
        //获取诊断疾病名称
        RecipeTagMsgBean recipeTagMsg = new RecipeTagMsgBean();
        recipeTagMsg.setTitle(patientDTO.getPatientName() + "的电子处方单");
        recipeTagMsg.setFlag("1");
        if (dumpMzjf) {
            recipeTagMsg.setFlag("3");
            recipeTagMsg.setContent("请点击查看处方单或检查检验单");
        }
        recipeTagMsg.setCardId(patientDTO.getCardId());
        recipeTagMsg.setRecipeSourceType(2);
        LOGGER.info("getRecipeMsgTagWithOfflineRecipe response:{}", JSONUtils.toString(recipeTagMsg));
        return recipeTagMsg;
    }

    /**
     * 处方撤销接口区分医生端和运营平台
     *
     * @param recipeId
     * @param flag
     * @return
     */
    public static Map<String, Object> cancelRecipeImpl(Integer recipeId, Integer flag, String name, String message) {
        LOGGER.info("cancelRecipe [recipeId：" + recipeId + "]");
        //获取订单
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder order = orderDAO.getOrderByRecipeId(recipeId);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        RecipeToHisMqService hisMqService = ApplicationUtils.getRecipeService(RecipeToHisMqService.class);
        RecipeCancelService recipeCancelService = ApplicationUtils.getRecipeService(RecipeCancelService.class);

        //获取处方单
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        Map<String, Object> rMap = Maps.newHashMap();
        Boolean result = false;
        //医生撤销提醒msg，供医生app端使用
        String msg = "";
        if (null == recipe) {
            msg = "该处方单不存在";
            rMap.put("result", result);
            rMap.put("msg", msg);
            return rMap;
        }

        String recipeMode = recipe.getRecipeMode();
        //获取撤销前处方单状态
        Integer beforeStatus = recipe.getStatus();
        //不能撤销的情况:1 患者已支付 2 药师已审核(不管是否通过)
        if (Integer.valueOf(RecipeStatusConstant.REVOKE).equals(recipe.getStatus())) {
            msg = "该处方单已撤销，不能进行撤销操作";
        }
        if (recipe.getStatus() == RecipeStatusConstant.UNSIGN) {
            msg = "暂存的处方单不能进行撤销";
        }
        if (Integer.valueOf(1).equals(recipe.getPayFlag())) {
            msg = "该处方单用户已支付，不能进行撤销操作";
        }
        if (recipe.getStatus() == RecipeStatusConstant.HIS_FAIL || recipe.getStatus() == RecipeStatusConstant.NO_DRUG || recipe.getStatus() == RecipeStatusConstant.NO_PAY || recipe.getStatus() == RecipeStatusConstant.NO_OPERATOR) {
            msg = "该处方单已取消，不能进行撤销操作";
        }
        if (Integer.valueOf(1).equals(recipe.getChooseFlag())) {
            msg = "患者已选择购药方式，不能进行撤销操作";
        }
        if (1 == flag) {
            if (StringUtils.isEmpty(name)) {
                msg = "姓名不能为空";
            }
            if (StringUtils.isEmpty(message)) {
                msg = "撤销原因不能为空";
            }
        }

        //判断第三方处方能否取消,若不能则获取不能取消的原因----只有推送药企成功后才判断能否撤销
        if (new Integer(1).equals(recipe.getPushFlag())) {
            HisResponseTO res = recipeCancelService.canCancelRecipe(recipe);
            if (!res.isSuccess()) {
                msg = res.getMsg();
            }
        }
        if (StringUtils.isNotEmpty(msg)) {
            rMap.put("result", result);
            rMap.put("msg", msg);
            return rMap;
        }

        //处方撤销信息，供记录日志使用
        StringBuilder memo = new StringBuilder(msg);
        Map<String, Integer> changeAttr = Maps.newHashMap();
        if (order != null) {
            if (!recipe.canMedicalPay()) {
                changeAttr.put("chooseFlag", 1);
            }
            orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO, true);
        }
        //撤销处方
        changeAttr.put("checkFlag", null);
        result = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.REVOKE, changeAttr);
        if (result) {
            msg = "处方撤销成功";
            EmrRecipeManager emrRecipeManager = AppContextHolder.getBean("emrRecipeManager", EmrRecipeManager.class);
            //将药品移出病历
            emrRecipeManager.deleteRecipeDetailsFromDoc(recipeId);
            //向患者推送处方撤销消息
            if (!(RecipeStatusConstant.READY_CHECK_YS == recipe.getStatus() && recipe.canMedicalPay())) {
                //医保的处方待审核时患者无法看到处方，不发送撤销消息提示
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.REVOKE);
            }
            memo.append(msg);
            //HIS消息发送
            if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipeMode)) {

                boolean succFlag = hisService.recipeStatusUpdate(recipeId);
                if (succFlag) {
                    memo.append(",HIS推送成功");
                } else {
                    memo.append(",HIS推送失败");
                }
            } else if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeMode)) {
                hisMqService.recipeStatusToHis(HisMqRequestInit.initRecipeStatusToHisReq(recipe, HisBussConstant.TOHIS_RECIPE_STATUS_REVOKE));
                memo.append(",HIS推送成功");
                OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
                List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
                for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                    if ("aldyf".equals(drugsEnterprise.getCallSys())) {
                        //向阿里大药房推送处方撤销通知
                        DrugEnterpriseResult drugEnterpriseResult = null;
                        try {
                            AldyfRemoteService aldyfRemoteService = ApplicationUtils.getRecipeService(AldyfRemoteService.class);
                            drugEnterpriseResult = aldyfRemoteService.updatePrescriptionStatus(recipe.getRecipeCode(), AlDyfRecipeStatusConstant.RETREAT);
                        } catch (Exception e) {
                            LOGGER.warn("cancelRecipeImpl  向阿里大药房推送处方撤销通知,{}", JSONUtils.toString(drugEnterpriseResult), e);
                        }
                        LOGGER.info("向阿里大药房推送处方撤销通知,{}", JSONUtils.toString(drugEnterpriseResult));
                    }
                }
            }
            LOGGER.info("cancelRecipe result:" + memo);
            //如果是待审核要取消未结束任务
            if (RecipeStatusConstant.READY_CHECK_YS == beforeStatus) {
                ApplicationUtils.getBaseService(IAsynDoBussService.class).fireEvent(new BussCancelEvent(recipeId, BussTypeConstant.RECIPE));
            }
            //处方撤销后将状态设为已撤销，供记录日志使用
            recipe.setStatus(RecipeStatusConstant.REVOKE);
            //推送处方到监管平台
            RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(recipe.getRecipeId(), 1));
        } else {
            msg = "未知原因，处方撤销失败";
            memo.append("," + msg);
        }

        if (1 == flag) {
            memo.append("处方撤销成功。" + "撤销人：" + name + ",撤销原因：" + message);
        } else {
            if (result) {
                if (StringUtils.isNotEmpty(message)) {
                    memo = new StringBuilder(message);
                } else {
                    memo = new StringBuilder("无");
                }

            }
        }
        //记录日志
        RecipeLogService.saveRecipeLog(recipeId, beforeStatus, recipe.getStatus(), memo.toString());
        rMap.put("result", result);
        rMap.put("msg", msg);
        LOGGER.info("cancelRecipe execute ok! rMap:" + JSONUtils.toString(rMap));
        return rMap;
    }

    /**
     * 获取医院时候可以药店取药
     */
    public static Boolean getDrugToHos(Integer recipeId, Integer organId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(recipeId);
        if (null == recipe) {
            LOGGER.info("处方不存在 recipeId[{}]", recipeId);
            return false;
        }
        //平台的取平台配置项
        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())) {
            //获取配置项
            GiveModeShowButtonVO giveModeShowButtonVO = GiveModeFactory.getGiveModeBaseByRecipe(recipe).getGiveModeSettingFromYypt(organId);
            Map result = giveModeShowButtonVO.getGiveModeButtons().stream().collect(Collectors.toMap(GiveModeButtonBean::getShowButtonKey, GiveModeButtonBean::getShowButtonName));
            return result.containsKey("supportToHos");
        } else {
            return organService.getTakeMedicineFlagById(organId);
        }


    }

    /**
     * 杭州市互联网获取是否是医保病人
     *
     * @param mpiid
     * @param clinicOrgan
     * @return
     */
    public static Boolean isMedicalPatient(String mpiid, Integer clinicOrgan) {
        HealthCardService healthCardService = ApplicationUtils.getBasicService(HealthCardService.class);
        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        //杭州市互联网医院监管中心 管理单元eh3301
        OrganDTO organDTO = organService.getByManageUnit("eh3301");
        if (organDTO != null) {
            //医保卡id ----
            String medicareCardId = healthCardService.getMedicareCardId(mpiid, organDTO.getOrganId());
            if (StringUtils.isNotEmpty(medicareCardId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 是否配置了杭州市互联网
     *
     * @param clinicOrgan
     * @return
     */
    public static boolean isNotHZInternet(Integer clinicOrgan) {
        OrganAndDrugsepRelationDAO dao = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        List<DrugsEnterprise> enterprises = dao.findDrugsEnterpriseByOrganIdAndStatus(clinicOrgan, 1);
        if (CollectionUtils.isNotEmpty(enterprises)) {
            if ("hzInternet".equals(enterprises.get(0).getCallSys())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 是否配置了走扁鹊处方流转平台
     *
     * @param clinicOrgan
     * @return
     */
    public static boolean isBQEnterprise(Integer clinicOrgan) {
        OrganAndDrugsepRelationDAO dao = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        List<DrugsEnterprise> enterprises = dao.findDrugsEnterpriseByOrganIdAndStatus(clinicOrgan, 1);
        if (CollectionUtils.isNotEmpty(enterprises)) {
            if ("bqEnterprise".equals(enterprises.get(0).getAccount())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否配置了走扁鹊处方流转平台
     *
     * @param depId 药企id
     * @return
     */
    public static boolean isBQEnterpriseBydepId(Integer depId) {
        if (depId != null) {
            DrugsEnterpriseDAO dao = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            DrugsEnterprise drugsEnterprise = dao.getById(depId);
            if (drugsEnterprise != null && "bqEnterprise".equals(drugsEnterprise.getAccount())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否是配置了江苏监管平台的机构
     *
     * @param clinicOrgan
     * @return
     */
    public static boolean isJSOrgan(Integer clinicOrgan) {
        try {
            IHisServiceConfigService configService = AppContextHolder.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
            List<ServiceConfigResponseTO> serviceConfigResponseTOS = configService.findAllRegulationOrgan();
            if (CollectionUtils.isEmpty(serviceConfigResponseTOS)) {
                return false;
            }
            //判断机构是否关联了江苏监管平台
            List<Integer> organList = serviceConfigResponseTOS.stream().filter(regulation -> regulation.getRegulationAppDomainId().startsWith("jssjgpt")).map(ServiceConfigResponseTO::getOrganid).collect(Collectors.toList());
            LOGGER.info("isJSOrgan organId={}", JSONUtils.toString(organList));
            if (organList.contains(clinicOrgan)) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("isJSOrgan error", e);
            return false;
        }
        return false;
    }

    /**
     * 获取机构是否支持二次审方
     *
     * @param clinicOrgan
     * @return
     */
    public static boolean canSecondAudit(Integer clinicOrgan) {
        //默认不支持
        Boolean flag = false;
        try {
            IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
            flag = (Boolean) configService.getConfiguration(clinicOrgan, "doctorSecondAuditFlag");
        } catch (Exception e) {
            LOGGER.error("canSecondAudit 获取机构配置异常", e);
        }
        return flag;
    }

    /**
     * 设置没有用药天数字符的处方详情
     *
     * @param recipeDetails
     * @return
     */
    public static void setUseDaysBToDetali(List<Recipedetail> recipeDetails) {
        for (Recipedetail recipedetail : recipeDetails) {
            if (StringUtils.isEmpty(recipedetail.getUseDaysB())) {

                recipedetail.setUseDaysB(null != recipedetail.getUseDays() ? recipedetail.getUseDays().toString() : "0");

            }
        }
    }

    /**
     * 转换组织机构编码
     *
     * @param organId
     * @return
     */
    public static Integer transformOrganIdToClinicOrgan(String organId) {
        //需要转换组织机构编码
        Integer clinicOrgan = null;
        try {
            if (isClinicOrgan(organId)) {
                return Integer.valueOf(organId);
            }
            IOrganService organService = BaseAPI.getService(IOrganService.class);
            List<OrganBean> organList = organService.findByOrganizeCode(organId);
            if (CollectionUtils.isNotEmpty(organList)) {
                clinicOrgan = organList.get(0).getOrganId();
            }
        } catch (Exception e) {
            LOGGER.error("queryRecipeInfo 平台未匹配到该组织机构编码. organId={}", organId, e);
        }
        return clinicOrgan;
    }

    /**
     * 判断是否是平台机构id规则----长度为7的纯数字
     *
     * @param organId
     * @return
     */
    public static boolean isClinicOrgan(String organId) {
        return RegexEnum.regular(organId, RegexEnum.NUMBER) && (organId.length() == 7);
    }

    public static Integer getOrganEnterprisesDockType(Integer organId) {
        Object dockType = configService.getConfiguration(organId, "EnterprisesDockType");
        return null != dockType ? Integer.parseInt(dockType.toString()) : new Integer(0);
    }

    /**
     * 获取当前recipeCode的处方信息
     *
     * @param responseTO
     * @param recipeCode
     * @return
     * @Author liumin
     */
    private static QueryHisRecipResTO getRecipeInfoByRecipeCode(HisResponseTO<List<QueryHisRecipResTO>> responseTO, String recipeCode) {
        LOGGER.info("getRecipeInfoByRecipeCode recipecode:{} , param:{}", recipeCode, JSONUtils.toString(responseTO));
        QueryHisRecipResTO response = new QueryHisRecipResTO();
        if (!StringUtils.isEmpty(recipeCode)) {
            if (responseTO != null) {
                List<QueryHisRecipResTO> queryHisRecipResTOs = responseTO.getData();
                List<QueryHisRecipResTO> queryHisRecipResTOFilters = new ArrayList<>();
                if (!CollectionUtils.isEmpty(queryHisRecipResTOs)) {
                    for (QueryHisRecipResTO queryHisRecipResTO : queryHisRecipResTOs) {
                        if (recipeCode.equals(queryHisRecipResTO.getRecipeCode())) {
                            response = queryHisRecipResTO;
                        }
                    }
                }
            }
        }
        LOGGER.info("getRecipeInfoByRecipeCode recipecode:{} , response:{}", recipeCode, JSONUtils.toString(response));
        return response;
    }

    /**
     * 判断机构是否是配置的重庆监管平台
     *
     * @param clinicOrgan
     * @return
     */
    public static boolean isCQOrgan(Integer clinicOrgan) {
        LOGGER.info("isCQOrgan request:{}", clinicOrgan);
        try {
            IHisServiceConfigService configService = AppContextHolder.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
            List<ServiceConfigResponseTO> serviceConfigResponseTOS = configService.findAllRegulationOrgan();
            if (CollectionUtils.isEmpty(serviceConfigResponseTOS)) {
                return false;
            }
            //判断机构是否关联了重庆监管平台
            List<Integer> organList = serviceConfigResponseTOS.stream().filter(regulation -> regulation.getRegulationAppDomainId().startsWith("cqsjgpt")).map(ServiceConfigResponseTO::getOrganid).collect(Collectors.toList());
            LOGGER.info("isCQOrgan organId={}", JSONUtils.toString(organList));
            if (organList.contains(clinicOrgan)) {
                LOGGER.info("isCQOrgan response true");
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("isCQOrgan response false cause error", e);
            return false;
        }
        LOGGER.info("isCQOrgan response false ");
        return false;
    }
}

