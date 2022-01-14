package recipe.audit.service;

import com.google.common.collect.Maps;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.dto.ApothecaryDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.CardBean;
import com.ngari.recipe.recipe.model.GuardianBean;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import ctd.account.UserRoleToken;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.schema.exception.ValidateException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.recipeaudit.api.IAuditMedicinesService;
import eh.recipeaudit.api.IRecipeAuditService;
import eh.recipeaudit.api.IRecipeCheckService;
import eh.recipeaudit.model.AuditMedicineIssueBean;
import eh.recipeaudit.model.Intelligent.PAWebRecipeDangerBean;
import eh.recipeaudit.model.RecipeCheckBean;
import eh.recipeaudit.util.RecipeAuditAPI;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.aop.LogRecord;
import recipe.bussutil.AESUtils;
import recipe.client.DoctorClient;
import recipe.client.RecipeAuditClient;
import recipe.constant.*;
import recipe.dao.*;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.enumerate.type.TakeMedicineWayEnum;
import recipe.manager.ButtonManager;
import recipe.manager.DepartManager;
import recipe.service.RecipeService;
import recipe.util.ChinaIDNumberUtil;
import recipe.util.DateConversion;
import recipe.vo.second.ApothecaryVO;

import java.util.*;

import static recipe.util.ByteUtils.hideIdCard;

/**
 * 服务在用 新方法不再此类新增
 *
 * @author wzc
 * @date 2020-10-27 14:27
 * @desc 运营平台处方服务
 */
@RpcBean("operationPlatformRecipeService")
@Deprecated
public class OperationPlatformRecipeService {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationPlatformRecipeService.class);
    private static final Integer GRABORDER_STATUS_YES = 1;
    private PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
    private DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
    @Autowired
    private IRecipeCheckService recipeCheckService;
    @Autowired
    private DoctorClient doctorClient;
    @Autowired
    private ButtonManager buttonManager;
    @Autowired
    private IAuditMedicinesService auditMedicinesService;
    @Autowired
    private DepartManager departManager;
    @Autowired
    private RecipeAuditClient recipeAuditClient;
    @Autowired
    private CommonRemoteService commonRemoteService;

    /**
     * 审核平台 获取处方单详情
     *
     * @param recipeId
     * @return
     */
    @RpcService
    @LogRecord
    public Map<String, Object> findRecipeAndDetailsAndCheckByIdEncrypt(String recipeId, Integer doctorId) {
        LOGGER.info("findRecipeAndDetailsAndCheckByIdEncrypt recipeId={},doctorId={}", recipeId, doctorId);
        //20200323 解密recipe
        Integer reicpeIdI = null;
        try {
            String recipeS = AESUtils.decrypt(recipeId, "1234567890123gmw");
            reicpeIdI = Integer.valueOf(recipeS);
        } catch (Exception e) {
            LOGGER.error("findRecipeAndDetailsAndCheckByIdEncrypt-recipeId解密异常", e);
            throw new DAOException("处方号解密异常");
        }
        //20200323 越权检查
        checkUserIsChemistByDoctorId(reicpeIdI, doctorId);
        return findRecipeAndDetailsAndCheckById(reicpeIdI, doctorId);
    }

    /**
     * 审核平台 获取处方单详情
     *
     * @param recipeId
     * @return
     */
    @RpcService
    @LogRecord
    public Map<String, Object> findRecipeAndDetailsAndCheckById(int recipeId, Integer checkerId) {

        LOGGER.info("findRecipeAndDetailsAndCheckById recipeId={}.checkerId={}", recipeId, checkerId);
        RecipeDAO rDao = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        RecipeExtendDAO extendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        //取recipe需要的字段
        Recipe recipe = rDao.getByRecipeId(recipeId);

        if (null == recipe) {
            LOGGER.error("findRecipeAndDetailsAndCheckById recipeId={} can't find.", recipeId);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "无法找到该处方单");
        }
        Integer doctorId = recipe.getDoctor();
        RecipeBean r = new RecipeBean();
        r.setRecipeId(recipe.getRecipeId());
        //加密recipeId
        try {
            String recipeS = AESUtils.encrypt(recipe.getRecipeId() + "", "1234567890123gmw");
            r.setRecipeIdE(recipeS);
        } catch (Exception e) {
            LOGGER.error("findRecipeAndDetailsAndCheckById-recipeId加密异常", e);
        }
        r.setRecipeId(recipe.getRecipeId());
        r.setRecipeType(recipe.getRecipeType());
        r.setDoctor(doctorId);
        r.setOrganDiseaseName(recipe.getOrganDiseaseName());
        r.setClinicOrgan(recipe.getClinicOrgan());
        r.setSignDate(recipe.getSignDate());
        r.setDepart(recipe.getDepart());
        r.setCreateDate(recipe.getCreateDate());
        r.setCheckDateYs(recipe.getCheckDateYs());
        r.setChecker(recipe.getChecker());
        r.setCheckFailMemo(recipe.getCheckFailMemo());
        r.setCheckOrgan(recipe.getCheckOrgan());
        //诊断备注
        r.setMemo(recipe.getMemo());
        //处方号新加
        r.setRecipeCode(recipe.getRecipeCode());
        //新增配送详细地址
        String address = recipeService.getCompleteAddress(recipeId);
        r.setAddress1(address);
        //新增患者在医院的病历号
        r.setPatientID(recipe.getPatientID());
        //补充说明
        r.setSupplementaryMemo(recipe.getSupplementaryMemo());
        //处方状态
        r.setStatus(recipe.getStatus());
        //配送方式
        r.setGiveMode(recipe.getGiveMode());
        //配送方式文案
        r.setGiveModeText(buttonManager.getGiveModeTextByRecipe(recipe));
        //支付状态
        r.setPayFlag(recipe.getPayFlag());
        //医生签名文件
        r.setSignFile(recipe.getSignFile());
        //医生+药师签名文件
        r.setChemistSignFile(recipe.getChemistSignFile());
        //设置医生处方签
        r.setSignRecipeCode(recipe.getSignRecipeCode());
        //设置时间戳
        r.setSignCADate(recipe.getSignCADate());
        r.setFromflag(recipe.getFromflag());
        r.setOrderCode(recipe.getOrderCode());
        //贴数
        r.setCopyNum(recipe.getCopyNum());
        //总金额
        r.setTotalMoney(recipe.getTotalMoney());
        r.setActualPrice(recipe.getActualPrice());
        //处方备注
        r.setRecipeMemo(recipe.getRecipeMemo());
        try {
            String showTip = DictionaryController.instance().get("eh.cdr.dictionary.RecipeStatus").getText(recipe.getStatus());
            r.setShowTip(showTip);
        } catch (ControllerException e) {
            e.printStackTrace();
        }
        //挂号科室代码
        AppointDepartDTO appointDepart = departManager.getAppointDepartByOrganIdAndDepart(recipe);
        //挂号科室名称
        LOGGER.info("findRecipeAndDetailsAndCheckById reicpeid={},appointDepart={}", recipeId, JSONUtils.toString(appointDepart));
        r.setAppointDepartName((null != appointDepart) ? appointDepart.getAppointDepartName() : "");
        //机构所属一级科室
        r.setOrganProfession((null != appointDepart) ? appointDepart.getOrganProfession() : null);
        LOGGER.info("findRecipeAndDetailsAndCheckById reicpeid={},r={}", recipeId, JSONUtils.toString(r));
        //取医生的手机号
        DoctorDTO doctor = new DoctorDTO();
        try {
            doctor = doctorService.get(doctorId);
            doctor.setIdNumber(hideIdCard(doctor.getIdNumber()));
        } catch (Exception e) {
            LOGGER.warn("findRecipeAndDetailsAndCheckById get doctor error. doctorId={}", recipe.getDoctor(), e);
        }
        RecipeExtend extend = extendDAO.getByRecipeId(recipeId);
        //监护人信息
        GuardianBean guardian = new GuardianBean();
        //取patient需要的字段
        PatientBean p = new PatientBean();
        try {
            PatientDTO patient = patientService.get(recipe.getMpiid());
            if (null != patient) {
                p.setPatientName(patient.getPatientName());
                p.setPatientSex(patient.getPatientSex());
                p.setAge(null == patient.getBirthday() ? 0 : DateConversion.getAge(patient.getBirthday()));
                p.setBirthday(null == patient.getBirthday() ? new Date() : patient.getBirthday());
                p.setPatientType(patient.getPatientType());
                //加上手机号 和 身份证信息（脱敏）
                p.setMobile(patient.getMobile());
                p.setIdcard(hideIdCard(patient.getCertificate()));
                p.setCertificate(hideIdCard(patient.getCertificate()));
                p.setMpiId(patient.getMpiId());
                p.setCertificateType(patient.getCertificateType());
                //判断该就诊人是否为儿童就诊人
                if (new Integer(1).equals(patient.getPatientUserType()) || new Integer(2).equals(patient.getPatientUserType())) {
                    if (null != extend && StringUtils.isNotEmpty(extend.getGuardianCertificate())) {
                        guardian.setName(extend.getGuardianName());
                        guardian.setGuardianCertificate(hideIdCard(extend.getGuardianCertificate()));
                        guardian.setMobile(extend.getGuardianMobile());
                    } else {
                        guardian.setName(patient.getGuardianName());
                        guardian.setGuardianCertificate(hideIdCard(patient.getGuardianCertificate()));
                        guardian.setMobile(patient.getMobile());
                    }
                    try {
                        guardian.setAge(ChinaIDNumberUtil.getAgeFromIDNumber(patient.getGuardianCertificate()));
                        guardian.setSex(ChinaIDNumberUtil.getSexFromIDNumber(patient.getGuardianCertificate()));
                    } catch (ValidateException exception) {
                        LOGGER.warn("监护人使用身份证号获取年龄或者性别出错.{}.", exception.getMessage(), exception);
                    }
                }

            } else {
                LOGGER.warn("findRecipeAndDetailsAndCheckById patient is null. mpiId={}", recipe.getMpiid());
            }
        } catch (Exception e) {
            LOGGER.warn("findRecipeAndDetailsAndCheckById get patient error. mpiId={}", recipe.getMpiid(), e);
        }

        Map<String, Object> map = Maps.newHashMap();
        //判断是否为儿童处方
        //兼容老版本（此版本暂时不做删除）
        Boolean childRecipeFlag = false;
        if (extend != null) {
            if (Integer.valueOf(1).equals(extend.getRecipeFlag())) {
                childRecipeFlag = true;
            }
            map.put("recipeExtend", extend);
            r.setMedicalType(extend.getMedicalType());
            r.setMedicalTypeText(extend.getMedicalTypeText());
        }
        map.put("showAllergyMedical", (null != extend && StringUtils.isNotEmpty(extend.getAllergyMedical())));
        //date 20191111
        //添加处方审核状态
        Integer checkResult = getCheckResultByPending(recipe);
        map.put("checkStatus", checkResult);
        //获取撤销原因
        if (recipe.getStatus() == RecipeStatusConstant.REVOKE) {
            map.put("cancelReason", getCancelReasonForChecker(recipe.getRecipeId()));
        }
        //date 20200512
        //添加签名页面展示信息
        if (recipe.getStatus() == RecipeStatusConstant.SIGN_ING_CODE_PHA || recipe.getStatus() == RecipeStatusConstant.SIGN_ERROR_CODE_PHA) {
            map.put("cancelReason", getSignReasonForChecker(recipe.getRecipeId(), recipe.getStatus()));
        }
        List<Recipedetail> details = detailDAO.findByRecipeId(recipeId);
        try {
            Integer organId = recipe.getClinicOrgan();
            for (Recipedetail recipedetail : details) {
                Integer drugId = recipedetail.getDrugId();
                List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(drugId, organId);
                if (CollectionUtils.isNotEmpty(organDrugLists)) {
                    recipedetail.setDrugForm(organDrugLists.get(0).getDrugForm());
                }
            }
        } catch (Exception e) {
            LOGGER.info("findRecipeAndDetailsAndCheckById recipe:{},{}.", JSONUtils.toString(recipe), e.getMessage(), e);
        }

        //获取审核不通过详情
        IRecipeAuditService recipeAuditService = RecipeAuditAPI.getService(IRecipeAuditService.class, "recipeAuditServiceImpl");
        List<Map<String, Object>> mapList = recipeAuditService.getCheckNotPassDetail(recipeId);
        map.put("reasonAndDetails", mapList);

        //开方日期 yyyy-MM-dd HH:mm 格式化
        Date signDate = recipe.getSignDate();
        String dateString = "";
        if (null != signDate) {
            dateString = DateConversion.getDateFormatter(signDate, "yyyy-MM-dd HH:mm");
        }
        //处方供应商
        Integer enterpriseId = recipe.getEnterpriseId();
        DrugsEnterpriseBean e = new DrugsEnterpriseBean();
        if (enterpriseId != null) {
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(enterpriseId);
            if (null != drugsEnterprise) {
                e.setName(drugsEnterprise.getName());
                e.setPayModeSupport(drugsEnterprise.getPayModeSupport());
                e.setCreateType(drugsEnterprise.getCreateType());
            }
        }
        RecipeOrderBean order = null;
        String orderCode = recipe.getOrderCode();
        if (!StringUtils.isEmpty(orderCode)) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
            order = ObjectCopyUtils.convert(recipeOrder, RecipeOrderBean.class);
            if (order == null) {
                order = new RecipeOrderBean();
                //跟前端约定好这个字段一定会给的，所以定义了-1作为无支付类型
                order.setOrderType(-1);
            }

            Integer effective = order.getEffective() == null ? Integer.valueOf(1) : order.getEffective();
            Integer orderType = order.getOrderType() == null ? Integer.valueOf(0) : order.getOrderType();
            //老数据处理
            if (Integer.valueOf(0).equals(effective)) {
                order.setOrderType(-1);
            } else {
                order.setOrderType(orderType);
            }
            if ((null != order.getAddressID() || TakeMedicineWayEnum.TAKE_MEDICINE_STATION.getType().equals(order.getTakeMedicineWay()))) {
                order.setCompleteAddress(commonRemoteService.getCompleteAddress(recipeOrder));
            }
        }

        //药师能否撤销标识
        Boolean cancelRecipeFlag = false;
        //只有审核通过的才有标识
        if (checkerId != null && checkerId.equals(recipe.getChecker()) && new Integer(1).equals(checkResult)
                && !(RecipeStatusConstant.SIGN_NO_CODE_PHA == recipe.getStatus() || RecipeStatusConstant.SIGN_ERROR_CODE_PHA == recipe.getStatus() || RecipeStatusConstant.SIGN_ING_CODE_PHA == recipe.getStatus())) {
            cancelRecipeFlag = true;
        }
        map.put("cancelRecipeFlag", cancelRecipeFlag);

        //患者就诊卡信息
        if (extend != null) {
            try {
                //就诊卡卡号--只有复诊的患者才有就诊卡类型
                String cardNo = extend.getCardNo();
                //就诊卡类型
                String cardType = extend.getCardType();
                //如果cardName存在，则取cardName,否则从字典中取，如果两者都没有的话，那就是没有
                //就诊卡名称
                String cardTypeName = extend.getCardTypeName() == null ? DictionaryController.instance().get("eh.mpi.dictionary.CardType").getText(extend.getCardType()) : extend.getCardTypeName();
                CardBean cardBean = new CardBean();
                cardBean.setCardNo(cardNo);
                cardBean.setCardType(cardType);
                cardBean.setCardTypeName(cardTypeName);
                map.put("card", cardBean);
            } catch (Exception e1) {
                LOGGER.error("findRecipeAndDetailsAndCheckById.error", e1);
            }
        }
        map.put("childRecipeFlag", childRecipeFlag);
        map.put("guardian", guardian);

        map.put("dateString", dateString);
        map.put("recipe", r);
        map.put("patient", p);
        map.put("doctor", doctor);
        map.put("details", ObjectCopyUtils.convert(details, RecipeDetailBean.class));
        map.put("drugsEnterprise", e);
        map.put("recipeOrder", order);
        //如果配了智能审方则显示智能审方结果
        //增加返回智能审方结果药品问题列表
        //判断开关是否开启
        PrescriptionService prescriptionService = ApplicationUtils.getRecipeService(PrescriptionService.class);
        if (recipe.getStatus() != 0) {
            if (prescriptionService.getIntellectJudicialFlag(recipe.getClinicOrgan()) == 1) {
                map.put("medicines", recipeAuditClient.getAuditMedicineIssuesByRecipeId(recipeId));
                List<AuditMedicineIssueBean> auditMedicineIssues = auditMedicinesService.findIssueByRecipeId(recipeId);
                if (CollectionUtils.isNotEmpty(auditMedicineIssues)) {
                    List<AuditMedicineIssueBean> resultMedicineIssues = new ArrayList<>();
                    auditMedicineIssues.forEach(item -> {
                        if (null == item.getMedicineId()) {
                            resultMedicineIssues.add(item);
                        }
                    });

                    List<PAWebRecipeDangerBean> recipeDangers = new ArrayList<>();
                    resultMedicineIssues.forEach(item -> {
                        PAWebRecipeDangerBean recipeDanger = new PAWebRecipeDangerBean();
                        recipeDanger.setDangerDesc(item.getDetail());
                        recipeDanger.setDangerDrug(item.getTitle());
                        recipeDanger.setDangerLevel(item.getLvlCode());
                        recipeDanger.setDangerType(item.getLvl());
                        recipeDanger.setDetailUrl(item.getDetailUrl());
                        recipeDangers.add(recipeDanger);
                    });
                    map.put("recipeDangers", recipeDangers);
                }
            }
        }
        Integer one = 1;
        //运营平台 编辑订单信息按钮是否显示（自建药企、已审核、配送到家、药店取药、已支付）
        if (null != checkResult && (checkResult == 1 || checkResult == 3) && one.equals(r.getPayFlag()) && null != r.getGiveMode() && r.getGiveMode() <= 3) {
            map.put("editFlag", one);
        } else {
            map.put("editFlag", 0);
        }


        UserRoleToken urt = UserRoleToken.getCurrent();

        if (null != urt && null != urt.getProperty("doctor")) {
            DoctorDTO loginDoctor = BeanUtils.map(urt.getProperty("doctor"), DoctorDTO.class);
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("doctorId", loginDoctor.getDoctorId());
            paramMap.put("recipeId", r.getRecipeId());
            Map<String, Object> orderStatusAndLimitTime = getGrabOrderStatusAndLimitTime(paramMap);
            setSignStatusToGrabOrder(orderStatusAndLimitTime, recipe);
            if (!orderStatusAndLimitTime.isEmpty()) {
                map.put("lockLimitTime", orderStatusAndLimitTime.get("lockLimitTime"));
                map.put("grabOrderStatus", orderStatusAndLimitTime.get("grabOrderStatus"));
            }
        }
        ApothecaryDTO apothecaryDTO = doctorClient.getApothecary(recipe);
        ApothecaryVO apothecaryVO = new ApothecaryVO();
        org.springframework.beans.BeanUtils.copyProperties(apothecaryDTO, apothecaryVO);
        map.put("apothecary", apothecaryVO);
        LOGGER.info("findRecipeAndDetailsAndCheckById.map={}", JSONUtils.toString(map));
        return map;
    }


    private String getSignReasonForChecker(Integer recipeId, Integer status) {
        RecipeLogDAO recipeLogDAO = DAOFactory.getDAO(RecipeLogDAO.class);
        List<RecipeLog> recipeLogs = recipeLogDAO.findByRecipeIdAndAfterStatusDesc(recipeId, RecipeStatusConstant.SIGN_ERROR_CODE_PHA);
        String signReason = "";
        if (RecipeStatusConstant.SIGN_ERROR_CODE_PHA == status) {
            if (CollectionUtils.isNotEmpty(recipeLogs)) {
                signReason = "审方签名失败：" + recipeLogs.get(0).getMemo();
            } else {
                signReason = "审方签名失败!";
            }
        }
        if (RecipeStatusConstant.SIGN_ING_CODE_PHA == status) {
            signReason = "审方签名中";
        }
        return signReason;
    }


    /**
     * 获取待审核列表审核结果
     *
     * @param recipe checkResult 0:未审核 1:通过 2:不通过 3:二次签名 4:失效
     * @return
     */
    private Integer getCheckResultByPending(Recipe recipe) {
        Integer checkResult = 0;
        Integer status = recipe.getStatus();
        RecipeRefundDAO recipeRefundDAO = DAOFactory.getDAO(RecipeRefundDAO.class);
        //date 20191127
        //添加前置判断:当审核方式是不需要审核则返回通过审核状态
        if (ReviewTypeConstant.Not_Need_Check == recipe.getReviewType()) {
            return RecipePharmacistCheckConstant.Check_Pass;
        }
        if (RecipeStatusConstant.REVOKE == status) {
            //date 20200721 添加“已取消”状态
            if (CollectionUtils.isNotEmpty(recipeRefundDAO.findRefundListByRecipeId(recipe.getRecipeId()))) {
                return RecipePharmacistCheckConstant.Cancel;
            }
            return RecipePharmacistCheckConstant.Check_Failure;
        }
        if (RecipeStatusConstant.SIGN_ERROR_CODE_PHA == status || RecipeStatusConstant.SIGN_ING_CODE_PHA == status) {
            RecipeCheckBean nowRecipeCheck = recipeCheckService.getNowCheckResultByRecipeId(recipe.getRecipeId());
            if (null != nowRecipeCheck) {
                if (1 == nowRecipeCheck.getCheckStatus()) {
                    checkResult = RecipePharmacistCheckConstant.Check_Pass;
                } else if (0 == nowRecipeCheck.getCheckStatus() && null != nowRecipeCheck.getChecker()) {
                    checkResult = RecipePharmacistCheckConstant.Check_Failure;
                }
            } else {
                LOGGER.warn("当前处方{}不存在！", recipe.getRecipeId());
            }
            return checkResult;
        }
        if (RecipeStatusConstant.READY_CHECK_YS == status) {
            checkResult = RecipePharmacistCheckConstant.Already_Check;
        } else if (RecipeStatusConstant.REVOKE == status) {
            //date 20200721 添加“已取消”状态
            if (CollectionUtils.isNotEmpty(recipeRefundDAO.findRefundListByRecipeId(recipe.getRecipeId()))) {
                return RecipePharmacistCheckConstant.Cancel;
            }
            checkResult = 4;
        } else if (RecipeStatusConstant.SIGN_NO_CODE_PHA == status) {
            checkResult = 8;
        } else {
            if (StringUtils.isNotEmpty(recipe.getSupplementaryMemo())) {
                checkResult = RecipePharmacistCheckConstant.Second_Sign;
            } else {
                RecipeCheckBean recipeCheck = recipeCheckService.getByRecipeId(recipe.getRecipeId());
                //有审核记录就展示
                if (recipeCheck != null) {
                    if (null != recipeCheck.getChecker() && RecipecCheckStatusConstant.First_Check_No_Pass == recipeCheck.getCheckStatus()) {
                        checkResult = RecipePharmacistCheckConstant.Check_Pass;
                    } else if (null != recipeCheck.getChecker() && RecipecCheckStatusConstant.Check_Normal == recipeCheck.getCheckStatus()) {
                        checkResult = RecipePharmacistCheckConstant.Check_No_Pass;
                    }
                }
            }
        }

        return checkResult;
    }

    private void setSignStatusToGrabOrder(Map<String, Object> orderStatusAndLimitTime, Recipe recipe) {
        if (RecipeStatusConstant.SIGN_ERROR_CODE_PHA == recipe.getStatus() || RecipeStatusConstant.SIGN_ING_CODE_PHA == recipe.getStatus()) {
            orderStatusAndLimitTime.put("grabOrderStatus", GrabOrderStatusConstant.GRAB_ORDERED_OWN);
            orderStatusAndLimitTime.put("lockLimitTime", 10);
        }
    }


    private String getCancelReasonForChecker(Integer recipeId) {
        RecipeRefundDAO recipeRefundDAO = DAOFactory.getDAO(RecipeRefundDAO.class);
        String cancelReason = "";
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


    public void checkUserIsChemistByDoctorId(Integer recipeId, Integer doctorId) {
        RecipeDAO rDao = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = rDao.getByRecipeId(recipeId);
        if (recipe == null) {
            LOGGER.error("checkUserIsChemistByDoctorId-未获取到处方信息");
            throw new DAOException("未获取到处方信息");
        }
        Integer organId = recipe.getClinicOrgan();
        if (null == doctorId || null == organId) {
            LOGGER.error("药师ID或者机构ID不能为空doctorId[{}],organId[{}]", doctorId, organId);
            throw new DAOException("药师ID不能为空");
        }
        DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
        List<Integer> organIds = doctorService.findAPOrganIdsByDoctorId(doctorId);
        if (!(organIds.contains(organId))) {
            LOGGER.error("当前用户没有权限审核该处方doctorId[{}],organId[{}]", doctorId, organId);
            throw new DAOException("当前用户没有权限审核该处方");
        }
    }


    /**
     * chuwei
     * 前端页面调用该接口查询是否存在待审核的处方单
     *
     * @param organ 审核机构
     * @return
     */
    @RpcService
    @LogRecord
    public boolean existUncheckedRecipe(int organ) {
        RecipeDAO rDao = DAOFactory.getDAO(RecipeDAO.class);
        return rDao.checkIsExistUncheckedRecipe(organ);
    }


    /**
     * 获取药师能审核的机构
     *
     * @param doctorId 药师ID
     * @return
     */
    @RpcService
    @LogRecord
    public List<OrganBean> findCheckOrganList(Integer doctorId) {
        return recipeAuditClient.findCheckOrganList(doctorId);
    }


    /**
     * 获取抢单状态和自动解锁时间
     *
     * @param map
     * @return
     */
    @RpcService
    @LogRecord
    public Map<String, Object> getGrabOrderStatusAndLimitTime(Map<String, Object> map) {
        return recipeAuditClient.getGrabOrderStatusAndLimitTime(map);
    }
}
