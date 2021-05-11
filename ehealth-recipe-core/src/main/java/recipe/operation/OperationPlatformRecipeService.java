package recipe.operation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.GuardianBean;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipeorder.model.ApothecaryVO;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import ctd.account.UserRoleToken;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.Dictionary;
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
import eh.recipeaudit.api.IRecipeCheckDetailService;
import eh.recipeaudit.api.IRecipeCheckService;
import eh.recipeaudit.model.AuditMedicineIssueBean;
import eh.recipeaudit.model.RecipeCheckBean;
import eh.recipeaudit.model.RecipeCheckDetailBean;
import eh.recipeaudit.util.RecipeAuditAPI;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.audit.bean.PAWebRecipeDanger;
import recipe.audit.service.PrescriptionService;
import recipe.bussutil.openapi.util.AESUtils;
import recipe.constant.*;
import recipe.dao.*;
import recipe.givemode.business.GiveModeFactory;
import recipe.service.RecipeService;
import recipe.service.RecipeServiceSub;
import recipe.service.client.DoctorClient;
import recipe.util.ByteUtils;
import recipe.util.ChinaIDNumberUtil;
import recipe.util.DateConversion;

import java.util.*;

/**
 * @author wzc
 * @date 2020-10-27 14:27
 * @desc 运营平台处方服务
 */
@RpcBean("operationPlatformRecipeService")
public class OperationPlatformRecipeService {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationPlatformRecipeService.class);
    private static final Integer GRABORDER_STATUS_YES = 1;
    private PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
    private DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
    private DepartmentService departmentService = ApplicationUtils.getBasicService(DepartmentService.class);
    @Autowired
    private IRecipeCheckService recipeCheckService;
    @Autowired
    private IRecipeCheckDetailService recipeCheckDetailService;

    @Autowired
    private DoctorClient doctorClient;

    @Autowired
    private IAuditMedicinesService auditMedicinesService;

    /**
     * 审核平台 获取处方单详情
     *
     * @param recipeId
     * @return
     */
    @RpcService
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
    public Map<String, Object> findRecipeAndDetailsAndCheckById(int recipeId, Integer checkerId) {

        LOGGER.info("findRecipeAndDetailsAndCheckById recipeId={}.checkerId={}", recipeId,checkerId);
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
        r.setGiveModeText(GiveModeFactory.getGiveModeBaseByRecipe(recipe).getGiveModeTextByRecipe(recipe));
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
                if (!ObjectUtils.isEmpty(patient.getGuardianCertificate())) {
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
        if (extend != null) {
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

        }

        //获取该医生所在科室，判断是否为儿科科室
        Integer departId = recipe.getDepart();
        DepartmentDTO departmentDTO = departmentService.get(departId);
        Boolean childRecipeFlag = false;
        if (!ObjectUtils.isEmpty(departmentDTO)) {
            if (departmentDTO.getName().contains("儿科") || departmentDTO.getName().contains("新生儿科")
                    || departmentDTO.getName().contains("儿内科") || departmentDTO.getName().contains("儿外科")) {
                childRecipeFlag = true;
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
        HashMap<String, String> cardMap = Maps.newHashMap();
        if (extend != null) {
            try {
            //就诊卡卡号--只有复诊的患者才有就诊卡类型
            String cardNo = extend.getCardNo();
            //就诊卡类型
            String cardType = extend.getCardType();
            //如果cardName存在，则取cardName,否则从字典中取，如果两者都没有的话，那就是没有
            //就诊卡名称
            String  cardTypeName=extend.getCardTypeName()==null?DictionaryController.instance().get("eh.mpi.dictionary.CardType").getText(extend.getCardType()):extend.getCardTypeName();
                cardMap.put("cardType",cardType);
                cardMap.put("cardNo", cardNo);
                cardMap.put("cardTypeName", cardTypeName);
                map.put("card", cardMap);
            }catch (Exception e1){
                LOGGER.error("findRecipeAndDetailsAndCheckById.error",e1);
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
        if (prescriptionService.getIntellectJudicialFlag(recipe.getClinicOrgan()) == 1) {
            map.put("medicines", RecipeServiceSub.getAuditMedicineIssuesByRecipeId(recipeId));
            List<AuditMedicineIssueBean> auditMedicineIssues = auditMedicinesService.findIssueByRecipeId(recipeId);
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
                map.put("recipeDangers", recipeDangers);
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
        ApothecaryVO apothecaryVO = doctorClient.getApothecary(recipe);
        map.put("apothecary", apothecaryVO);
        LOGGER.info("findRecipeAndDetailsAndCheckById.map={}",JSONUtils.toString(map));
        return map;
    }

    private void setSignStatusToGrabOrder(Map<String, Object> orderStatusAndLimitTime, Recipe recipe) {
        if (RecipeStatusConstant.SIGN_ERROR_CODE_PHA == recipe.getStatus() || RecipeStatusConstant.SIGN_ING_CODE_PHA == recipe.getStatus()) {
            orderStatusAndLimitTime.put("grabOrderStatus", GrabOrderStatusConstant.GRAB_ORDERED_OWN);
            orderStatusAndLimitTime.put("lockLimitTime", 10);
        }
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

    /**
     * 获取不通过详情
     *
     * @param recipeId
     * @return
     */
    public List<Map<String, Object>> getCheckNotPassDetail(Integer recipeId) {
        RecipeCheckBean recipeCheck = recipeCheckService.getByRecipeIdAndCheckStatus(recipeId);
        if (null != recipeCheck) {
            //审核不通过 查询审核详情记录
            List<RecipeCheckDetailBean> checkDetails = recipeCheckDetailService.findByCheckId(recipeCheck.getCheckId());
            if (null != checkDetails) {
                List<Map<String, Object>> mapList = new ArrayList<>();

                for (RecipeCheckDetailBean checkDetail : checkDetails) {
                    Map<String, Object> checkMap = Maps.newHashMap();
                    String recipeDetailIds = checkDetail.getRecipeDetailIds();
                    String reasonIds = checkDetail.getReasonIds();
                    List<Integer> detailIdList;
                    List<Integer> reasonIdList;
                    if (StringUtils.isNotEmpty(recipeDetailIds)) {
                        detailIdList = JSONUtils.parse(recipeDetailIds, List.class);
                        checkMap.put("checkNotPassDetails", getRecipeDetailList(detailIdList));
                    }
                    if (StringUtils.isNotEmpty(reasonIds)) {
                        reasonIdList = JSONUtils.parse(reasonIds, List.class);
                        checkMap.put("reason", getReasonDicList(reasonIdList));
                    }
                    mapList.add(checkMap);
                }

                return mapList;
            }
        }
        return null;
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

    /**
     * 获取原因文本
     *
     * @param reList
     * @return
     */
    public List<String> getReasonDicList(List<Integer> reList) {
        List<String> reasonList = new ArrayList<>();
        try {
            Dictionary dictionary = DictionaryController.instance().get("eh.cdr.dictionary.Reason");
            if (null != reList) {
                for (Integer key : reList) {
                    String reason = dictionary.getText(key);
                    if (StringUtils.isNotEmpty(reason)) {
                        reasonList.add(reason);
                    }
                }
            }
        } catch (ControllerException e) {
            LOGGER.error("获取审核不通过原因字典文本出错reasonIds:" + JSONUtils.toString(reList), e);
        }
        return reasonList;
    }

    /**
     * 根据序号列表 获取详情列表
     *
     * @param ids
     * @return
     */
    private List<Recipedetail> getRecipeDetailList(List<Integer> ids) {
        List<Recipedetail> recipedetailList = new ArrayList<>();
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        if (null != ids) {
            for (Integer id : ids) {
                Recipedetail recipedetail = recipeDetailDAO.getByRecipeDetailId(id);
                if (null != recipedetail) {
                    recipedetailList.add(recipedetail);
                }
            }
        }
        return recipedetailList;
    }


    /**
     * 脱敏身份证号
     *
     * @param idCard
     * @return
     */
    private String hideIdCard(String idCard) {
        return ByteUtils.hideIdCard(idCard);
    }

    /**
     * chuwei
     * 前端页面调用该接口查询是否存在待审核的处方单
     *
     * @param organ 审核机构
     * @return
     */
    @RpcService
    public boolean existUncheckedRecipe(int organ) {
        RecipeDAO rDao = DAOFactory.getDAO(RecipeDAO.class);
        boolean bResult = rDao.checkIsExistUncheckedRecipe(organ);
        return bResult;
    }


    /**
     * 获取药师能审核的机构
     *
     * @param doctorId 药师ID
     * @return
     */
    @RpcService
    public List<OrganBean> findCheckOrganList(Integer doctorId) {
        List<OrganBean> organList = Lists.newArrayList();
        List<Integer> organIds = findAPOrganIdsByDoctorId(doctorId);
        if (CollectionUtils.isNotEmpty(organIds)) {
            IOrganService organService = ApplicationUtils.getBaseService(IOrganService.class);
            List<OrganBean> detailOrgan = organService.findByIdIn(organIds);
            OrganBean organBean;
            for (OrganBean bean : detailOrgan) {
                organBean = new OrganBean();
                organBean.setOrganId(bean.getOrganId());
                organBean.setShortName(bean.getShortName());
                organBean.setName(bean.getName());
                organList.add(organBean);
            }
        }
        return organList;
    }


    /**
     * app端 显示未处理业务的条数
     * zhongzx
     *
     * @param doctorId
     * @return
     */
    public long getUncheckedRecipeNum(Integer doctorId) {
        List<Integer> organIds = findAPOrganIdsByDoctorId(doctorId);
        if (CollectionUtils.isEmpty(organIds)) {
            return 0;
        }
        RecipeDAO rDao = DAOFactory.getDAO(RecipeDAO.class);
        //flag = 0 查询待药师审核的条数
        Long num = rDao.getRecipeCountByFlag(organIds, 0);
        return null == num ? 0 : num;
    }

    private List<Integer> findAPOrganIdsByDoctorId(Integer doctorId) {
        List<Integer> organIds = null;
        if (null != doctorId) {
            organIds = doctorService.findAPOrganIdsByDoctorId(doctorId);
        }
        return organIds;
    }


    /**
     * 判断登录用户能否审核机构下的处方
     *
     * @param recipeId
     * @param doctorId
     */
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
     * 获取抢单状态和自动解锁时间
     *
     * @param map
     * @return
     */
    @RpcService
    public Map<String, Object> getGrabOrderStatusAndLimitTime(Map<String, Object> map) {
        Map<String, Object> resultMap = new HashMap<>();
        Integer recipeId = MapUtils.getInteger(map, "recipeId");
        Integer doctorId = MapUtils.getInteger(map, "doctorId");
        Args.notNull(recipeId, "recipeId");
        Args.notNull(doctorId, "doctorId");
        String recipeS = null;
        try {
            RecipeCheckBean recipeCheck = recipeCheckService.getNowCheckResultByRecipeId(recipeId);
            if (null == recipeCheck || GrabOrderStatusConstant.GRAB_ORDER_NO.equals(recipeCheck.getGrabOrderStatus())) {
                resultMap.put("grabOrderStatus", GrabOrderStatusConstant.GRAB_ORDER_NO);
                //单默认返回10未抢
                resultMap.put("lockLimitTime", 10);
                return resultMap;
            }
            //本人抢单
            if (GRABORDER_STATUS_YES.equals(recipeCheck.getGrabOrderStatus()) && doctorId.equals(recipeCheck.getGrabDoctorId())) {
                resultMap.put("grabOrderStatus", GrabOrderStatusConstant.GRAB_ORDERED_OWN);
                long now = new Date().getTime();
                long localLimeDate = recipeCheck.getLocalLimitDate().getTime();
                long diff = localLimeDate - now;
                if (null == recipeCheck.getChecker() && diff <= 0) {
                    // 自动解除抢单
                    recipeCheck.setGrabOrderStatus(GrabOrderStatusConstant.GRAB_ORDER_NO);
                    recipeCheck.setLocalLimitDate(null);
                    recipeCheckService.update(recipeCheck);
                    resultMap.put("lockLimitTime", 0);
                } else {
                    int localLimitTime = (int) diff / (1000 * 60);
                    resultMap.put("lockLimitTime", localLimitTime + 1);
                }

            } else if (GRABORDER_STATUS_YES.equals(recipeCheck.getGrabOrderStatus()) && !doctorId.equals(recipeCheck.getGrabDoctorId())) { //他人抢单
                resultMap.put("grabOrderStatus", GrabOrderStatusConstant.GRAB_ORDERED_OTHER);
                resultMap.put("lockLimitTime", 10); //他人抢单默认返回10
            }
            return resultMap;

        } catch (Exception e) {
            LOGGER.error("getGrabOrderStatusAndLimitTime error", e);
            throw new DAOException("查询失败");
        }
    }
}
