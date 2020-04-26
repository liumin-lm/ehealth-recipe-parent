package recipe.recipecheck;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.organconfig.service.IOrganConfigService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.push.model.SmsInfoBean;
import com.ngari.base.push.service.ISmsPushService;
import com.ngari.base.searchcontent.model.SearchContentBean;
import com.ngari.base.searchcontent.service.ISearchContentService;
import com.ngari.home.asyn.model.BussFinishEvent;
import com.ngari.home.asyn.service.IAsynDoBussService;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.audit.model.AuditListReq;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.GuardianBean;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import ctd.account.UserRoleToken;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.schema.exception.ValidateException;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.event.GlobalEventExecFactory;
import eh.cdr.constant.RecipeStatusConstant;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.audit.auditmode.AuditModeContext;
import recipe.audit.bean.PAWebRecipeDanger;
import recipe.audit.service.PrescriptionService;
import recipe.bean.CheckYsInfoBean;
import recipe.bussutil.openapi.util.AESUtils;
import recipe.constant.*;
import recipe.dao.*;
import recipe.purchase.IPurchaseService;
import recipe.purchase.PurchaseEnum;
import recipe.service.*;
import recipe.thread.PushRecipeToRegulationCallable;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.ChinaIDNumberUtil;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author zhongzx
 * @date 2016/5/20
 * 药师审核平台的服务
 */
@RpcBean("recipeCheckService")
public class RecipeCheckService {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeCheckService.class);

    private PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);

    private DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);

    private DepartmentService departmentService = ApplicationUtils.getBasicService(DepartmentService.class);

    @Autowired
    private RecipeCheckDAO recipeCheckDAO;

    @Autowired
    private RecipeDAO recipeDAO;

    private static final String RECIPEID_SECRET = "1234567890123gmw";

    /**
     * zhongzx
     * 根据flag查询处方列表   for phone
     * flag 0-待审核 1-审核通过 2-审核未通过 3-全部
     *
     * @param doctorId
     * @param flag
     * @param start    每页起始数 首页从0开始
     * @param limit    每页限制条数
     * @return
     */
    @RpcService
    public List<Map<String, Object>> findRecipeListWithPage(int doctorId, int flag, int start, int limit) {
        AuditListReq request = new AuditListReq();
        request.setOrganIdList(null);
        request.setDoctorId(doctorId);
        request.setStatus(flag);
        return findRecipeListWithPageExt(request, start, limit);
    }

    /**
     * 查询审核处方列表扩展
     *
     * @param request
     * @return
     */
    @RpcService
    public List<Map<String, Object>> findRecipeListWithPageExt(AuditListReq request, int start, int limit) {
        LOGGER.info("findRecipeListWithPageExt request={}", JSONUtils.toString(request));
        if (null == request.getDoctorId() || null == request.getStatus()) {
            return Lists.newArrayList();
        }

        if (CollectionUtils.isEmpty(request.getOrganIdList())) {
            List<Integer> organIds = findAPOrganIdsByDoctorId(request.getDoctorId());
            request.setOrganIdList(organIds);
        }
        if (CollectionUtils.isEmpty(request.getOrganIdList())) {
            return Lists.newArrayList();
        }
        RecipeDAO rDao = DAOFactory.getDAO(RecipeDAO.class);
        List<Recipe> list = rDao.findRecipeByFlag(request.getOrganIdList(), request.getStatus(),
                start, limit);
        List<Map<String, Object>> mapList = covertRecipeListPageInfo(list);
        return mapList;
    }

    /**
     * zhongzx
     * 根据flag查询处方列表   for pc pc大小界面 和 手机大小界面 调用接口不同
     * flag 0-待审核 1-审核通过 2-审核未通过 3-全部
     *
     * @param doctorId
     * @param flag
     * @param start    开始位置 从0开始
     * @param limit    每页限制条数
     * @return
     */
    @RpcService
    public List<Map<String, Object>> findRecipeListWithPageForPC(int doctorId, int flag, int start, int limit) {
        RecipeDAO rDao = DAOFactory.getDAO(RecipeDAO.class);

        List<Integer> organIds = findAPOrganIdsByDoctorId(doctorId);
        AuditListReq request = new AuditListReq();
        request.setOrganIdList(organIds);
        request.setDoctorId(doctorId);
        request.setStatus(flag);
        List<Map<String, Object>> mapList = findRecipeListWithPageExt(request, start, limit);

        Long count = rDao.getRecipeCountByFlag(organIds, flag);
        Map<String, Object> countMap = Maps.newHashMap();
        countMap.put("start", start);
        countMap.put("limit", limit);
        countMap.put("count", count);
        mapList.add(countMap);
        return mapList;
    }

    private List<Map<String, Object>> covertRecipeListPageInfo(List<Recipe> list) {
        //只返回要用到的字段
        List<Map<String, Object>> mapList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            /*RecipeLogDAO recipeLogDAO = DAOFactory.getDAO(RecipeLogDAO.class);*/
            PatientDTO patient;
            PatientDTO dbPatient;
            for (Recipe r : list) {
                /*//获取待审核撤销的处方单
                if (r.getStatus() == RecipeStatusConstant.REVOKE){
                    List<RecipeLog> recipeLogs = recipeLogDAO.findByRecipeIdAndAfterStatus(r.getRecipeId(), RecipeStatusConstant.REVOKE);
                    if (CollectionUtils.isNotEmpty(recipeLogs)){
                        //撤销前的状态不是待审核或者审核后的则过滤
                        if (recipeLogs.get(0).getBeforeStatus()!=RecipeStatusConstant.READY_CHECK_YS
                            || r.getChecker() == null){
                            continue;
                        }
                    }
                }*/
                Map<String, Object> map = Maps.newHashMap();
                //组装需要的处方数据
                Recipe recipe = new Recipe();
                recipe.setRecipeId(r.getRecipeId());
                recipe.setSignDate(r.getSignDate());
                recipe.setDoctor(r.getDoctor());
                recipe.setDepart(r.getDepart());
                recipe.setOrganDiseaseName(r.getOrganDiseaseName());
                recipe.setChecker(r.getChecker());
                //组装需要的患者数据
                patient = new PatientDTO();
                try {
                    dbPatient = patientService.get(r.getMpiid());
                    patient.setPatientName(dbPatient.getPatientName());
                    patient.setPatientSex(dbPatient.getPatientSex());
                    patient.setAge(null == dbPatient.getBirthday() ? 0 : DateConversion.getAge(dbPatient.getBirthday()));
                    patient.setBirthday(null == dbPatient.getBirthday() ? new Date() : dbPatient.getBirthday());
                } catch (Exception e) {
                    LOGGER.warn("covertRecipeListPageInfo patient is error. mpiId={}, ", r.getMpiid(), e);
                }
                //显示一条详情数据
                List<Recipedetail> details = detailDAO.findByRecipeId(r.getRecipeId());
                Recipedetail detail = null;
                if (null != details && details.size() > 0) {
                    detail = details.get(0);
                }

                Date signDate = r.getSignDate();
                String dateString = "";
                if (null != signDate) {
                    dateString = DateConversion.getDateFormatter(signDate, "yyyy-MM-dd HH:mm");
                }
                //拿到当前药品详情的药品ID
                try {
                    Integer drugId = detail.getDrugId();
                    Integer organId = recipe.getClinicOrgan();
                    OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
                    if (organId != null) {
                        List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(drugId, organId);
                        if (CollectionUtils.isNotEmpty(organDrugLists)) {
                            detail.setDrugForm(organDrugLists.get(0).getDrugForm());
                        }
                    } else {
                        Integer recipeId = recipe.getRecipeId();
                        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
                        Recipe recipe1 = recipeDAO.getByRecipeId(recipeId);
                        if (recipe1 != null) {
                            List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(drugId, recipe1.getClinicOrgan());
                            if (CollectionUtils.isNotEmpty(organDrugLists)) {
                                detail.setDrugForm(organDrugLists.get(0).getDrugForm());
                            }
                        } else {
                            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
                            DrugList drugList = drugListDAO.getById(drugId);
                            if (drugList != null) {
                                detail.setDrugForm(drugList.getDrugForm());
                            }
                        }

                    }
                } catch (Exception e) {
                    LOGGER.info("covertRecipeListPageInfo recipeId:{},error:{}.", JSONUtils.toString(recipe), e.getMessage());
                }

                map.put("dateString", dateString);
                RecipeBean recipeBean = ObjectCopyUtils.convert(recipe, RecipeBean.class);
                //加密recipeId
                try {
                    String recipeS = AESUtils.encrypt(recipe.getRecipeId() + "", "1234567890123gmw");
                    recipeBean.setRecipeIdE(recipeS);
                } catch (Exception e) {
                    LOGGER.error("findRecipeAndDetailsAndCheckById-recipeId加密异常");
                }
                //checkResult 0:未审核 1:通过 2:不通过
                Integer checkResult = getCheckResult(r);

                RecipeCheck recipeCheck = recipeCheckDAO.getByRecipeIdAndCheckStatus(r.getRecipeId());
                UserRoleToken urt = UserRoleToken.getCurrent();

                if (null != urt && null != urt.getProperty("doctor")) {
                    DoctorDTO loginDoctor = BeanUtils.map(urt.getProperty("doctor"), DoctorDTO.class);
                    if(null != recipeCheck && recipeCheck.getGrabOrderStatus().equals(1) && null == recipeCheck.getChecker()
                            &&recipeCheck.getGrabDoctorId().equals(loginDoctor.getDoctorId())){ //已抢单
                        checkResult = 6;
                    }else if(null != recipeCheck && recipeCheck.getGrabOrderStatus().equals(1) && null == recipeCheck.getChecker()
                            &&!recipeCheck.getGrabDoctorId().equals(loginDoctor.getDoctorId())){ //已被抢单
                        checkResult = 5;
                    }
                }

                map.put("recipe", recipeBean);
                map.put("patient", patient);
                map.put("check", checkResult);
                map.put("detail", ObjectCopyUtils.convert(detail, RecipeDetailBean.class));
                mapList.add(map);
            }
        }

        return mapList;
    }

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
            LOGGER.error("findRecipeAndDetailsAndCheckByIdEncrypt-recipeId解密异常");
            throw new DAOException("处方号解密异常");
        }
        //20200323 越权检查
        checkUserIsChemistByDoctorId(reicpeIdI, doctorId);

        return findRecipeAndDetailsAndCheckById(reicpeIdI);
    }

    /**
     * 审核平台 获取处方单详情
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public Map<String, Object> findRecipeAndDetailsAndCheckById(int recipeId) {

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
            LOGGER.error("findRecipeAndDetailsAndCheckById-recipeId加密异常");
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

        try {
            String showTip = DictionaryController.instance().get("eh.cdr.dictionary.RecipeStatus").getText(recipe.getStatus());
            r.setShowTip(showTip);
        } catch (ControllerException e) {
            e.printStackTrace();
        }
        //取医生的手机号
        DoctorDTO doc = new DoctorDTO();
        try {
            DoctorDTO doctor = doctorService.get(doctorId);
            if (null != doctor) {
                doc.setMobile(doctor.getMobile());
            }
        } catch (Exception e) {
            LOGGER.warn("findRecipeAndDetailsAndCheckById get doctor error. doctorId={}", recipe.getDoctor(), e);
        }

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
                if (p.getAge() <= 5 && !ObjectUtils.isEmpty(patient.getGuardianCertificate())) {
                    guardian.setName(patient.getGuardianName());
                    try {
                        guardian.setAge(ChinaIDNumberUtil.getAgeFromIDNumber(patient.getGuardianCertificate()));
                        guardian.setSex(ChinaIDNumberUtil.getSexFromIDNumber(patient.getGuardianCertificate()));
                    } catch (ValidateException exception) {
                        LOGGER.warn("监护人使用身份证号获取年龄或者性别出错.{}.", exception.getMessage());
                    }
                }

            } else {
                LOGGER.warn("findRecipeAndDetailsAndCheckById patient is null. mpiId={}", recipe.getMpiid());
            }
        } catch (Exception e) {
            LOGGER.warn("findRecipeAndDetailsAndCheckById get patient error. mpiId={}", recipe.getMpiid(), e);
        }

        Map<String, Object> map = Maps.newHashMap();
        //医生端获取处方扩展信息
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (recipeExtend != null) {
            map.put("recipeExtend", recipeExtend);
        }
        //date 20191111
        //添加处方审核状态
        Integer checkResult = getCheckResultByPending(recipe);
        map.put("checkStatus", checkResult);
        //获取撤销原因
        if (recipe.getStatus() == RecipeStatusConstant.REVOKE) {
            map.put("cancelReason", getCancelReasonForChecker(recipe.getRecipeId()));
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
            LOGGER.info("findRecipeAndDetailsAndCheckById recipe:{},{}.", JSONUtils.toString(recipe), e.getMessage());
        }

        //获取审核不通过详情
        List<Map<String, Object>> mapList = getCheckNotPassDetail(recipeId);
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

        //患者就诊卡信息
        RecipeExtend extend = extendDAO.getByRecipeId(recipeId);
        HashMap<String, String> cardMap = Maps.newHashMap();
        if(extend!=null){
            String cardNo = extend.getCardNo();
            String cardTypeName = extend.getCardTypeName();
            cardMap.put("cardNo", cardNo);
            cardMap.put("cardTypeName", cardTypeName);
        }
        map.put("card", cardMap);

        map.put("childRecipeFlag", childRecipeFlag);
        map.put("guardian", guardian);

        map.put("dateString", dateString);
        map.put("recipe", r);
        map.put("patient", p);
        map.put("doctor", doc);
        map.put("details", ObjectCopyUtils.convert(details, RecipeDetailBean.class));
        map.put("drugsEnterprise", e);
        map.put("recipeOrder", order);
        //如果配了智能审方则显示智能审方结果
        //增加返回智能审方结果药品问题列表
        //判断开关是否开启
        PrescriptionService prescriptionService = ApplicationUtils.getRecipeService(PrescriptionService.class);
        if (prescriptionService.getIntellectJudicialFlag(recipe.getClinicOrgan()) == 1) {
            map.put("medicines", RecipeServiceSub.getAuditMedicineIssuesByRecipeId(recipeId));
            AuditMedicineIssueDAO auditMedicineIssueDAO = DAOFactory.getDAO(AuditMedicineIssueDAO.class);
            List<AuditMedicineIssue> auditMedicineIssues = auditMedicineIssueDAO.findIssueByRecipeId(recipeId);
            if(CollectionUtils.isNotEmpty(auditMedicineIssues)) {
                List<AuditMedicineIssue> resultMedicineIssues = new ArrayList<>();
                auditMedicineIssues.forEach(item -> {
                    if (StringUtils.isNotEmpty(item.getDetailUrl())) {
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

        //运营平台 编辑订单信息按钮是否显示（自建药企、已审核、配送到家、药店取药、已支付）
        if (e.getCreateType() != null && e.getCreateType() == 0
                && checkResult != null && (checkResult == 1 || checkResult == 3)
                && r.getGiveMode() != null && (r.getGiveMode() == 1 || r.getGiveMode() == 3)
                && r.getPayFlag() != null && r.getPayFlag() == 1) {
            map.put("editFlag", 1);
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
            if (!orderStatusAndLimitTime.isEmpty()) {
                map.put("lockLimitTime", orderStatusAndLimitTime.get("lockLimitTime"));
                map.put("grabOrderStatus", orderStatusAndLimitTime.get("grabOrderStatus"));
            }
        }
        return map;
    }

    private String getCancelReasonForChecker(Integer recipeId) {
        RecipeLogDAO recipeLogDAO = DAOFactory.getDAO(RecipeLogDAO.class);
        List<RecipeLog> recipeLogs = recipeLogDAO.findByRecipeIdAndAfterStatus(recipeId, RecipeStatusConstant.REVOKE);
        String cancelReason = "";
        if (CollectionUtils.isNotEmpty(recipeLogs)) {
            cancelReason = "开方医生已撤销处方,撤销原因:" + recipeLogs.get(0).getMemo();
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
        RecipeCheckDAO recipeCheckDAO = DAOFactory.getDAO(RecipeCheckDAO.class);
        RecipeCheckDetailDAO checkDetailDAO = DAOFactory.getDAO(RecipeCheckDetailDAO.class);
        RecipeCheck recipeCheck = recipeCheckDAO.getByRecipeIdAndCheckStatus(recipeId);
        if (null != recipeCheck) {
            //审核不通过 查询审核详情记录
            List<RecipeCheckDetail> checkDetails = checkDetailDAO.findByCheckId(recipeCheck.getCheckId());
            if (null != checkDetails) {
                List<Map<String, Object>> mapList = new ArrayList<>();

                for (RecipeCheckDetail checkDetail : checkDetails) {
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
     * 获取审核结果
     *
     * @param recipe checkResult 0:未审核 1:通过 2:不通过 3:二次签名 4:已撤销(被医生撤销)
     * @return
     */
    private Integer getCheckResult(Recipe recipe) {
        Integer checkResult = 0;
        Integer status = recipe.getStatus();
        if (RecipeStatusConstant.READY_CHECK_YS == status) {
            checkResult = 0;
        } else if (RecipeStatusConstant.REVOKE == status) {
            checkResult = 4;
        } else {
            if (StringUtils.isNotEmpty(recipe.getSupplementaryMemo())) {
                checkResult = 3;
            } else {
                RecipeCheckDAO recipeCheckDAO = DAOFactory.getDAO(RecipeCheckDAO.class);
                List<RecipeCheck> recipeCheckList = recipeCheckDAO.findByRecipeId(recipe.getRecipeId());
                //找不到审核记录 就用以前根据状态判断的方法判断
                if (CollectionUtils.isEmpty(recipeCheckList)) {
                    if (status == RecipeStatusConstant.IN_SEND ||
                            status == RecipeStatusConstant.WAIT_SEND ||
                            status == RecipeStatusConstant.FINISH ||
                            status == RecipeStatusConstant.CHECK_PASS_YS) {
                        checkResult = 1;
                    }
                    if (status == RecipeStatusConstant.CHECK_NOT_PASS_YS) {
                        checkResult = 2;
                    }
                } else {
                    RecipeCheck recipeCheck = recipeCheckList.get(0);
                    if (RecipecCheckStatusConstant.First_Check_No_Pass == recipeCheck.getCheckStatus()) {
                        checkResult = 1;
                    } else {
                        checkResult = 2;
                    }

                }
            }
        }

        return checkResult;
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
        //date 20191127
        //添加前置判断:当审核方式是不需要审核则返回通过审核状态
        if (ReviewTypeConstant.Not_Need_Check == recipe.getReviewType()) {
            return RecipePharmacistCheckConstant.Check_Pass;
        }
        if (RecipeStatusConstant.READY_CHECK_YS == status) {
            checkResult = RecipePharmacistCheckConstant.Already_Check;
        } else {
            if (StringUtils.isNotEmpty(recipe.getSupplementaryMemo())) {
                checkResult = RecipePharmacistCheckConstant.Second_Sign;
            } else {
                RecipeCheckDAO recipeCheckDAO = DAOFactory.getDAO(RecipeCheckDAO.class);
                List<RecipeCheck> recipeCheckList = recipeCheckDAO.findByRecipeId(recipe.getRecipeId());
                //有审核记录就展示
                if (CollectionUtils.isNotEmpty(recipeCheckList)) {
                    RecipeCheck recipeCheck = recipeCheckList.get(0);
                    if (RecipecCheckStatusConstant.First_Check_No_Pass == recipeCheck.getCheckStatus()) {
                        checkResult = RecipePharmacistCheckConstant.Check_Pass;
                    } else {
                        checkResult = RecipePharmacistCheckConstant.Check_No_Pass;
                    }
                    //记录没有审核信息的处方，说明是没有进行审核的状态是失效的
                } else {
                    checkResult = RecipePharmacistCheckConstant.Check_Failure;
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
            LOGGER.error("获取审核不通过原因字典文本出错reasonIds:" + JSONUtils.toString(reList));
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
     * zhongzx
     * 保存药师审核平台审核结果(recipeId加密)
     *
     * @param paramMap 包含以下属性
     *                 String     recipeId 处方ID(加密)
     *                 int        checkOrgan  检查机构
     *                 int        checker    检查人员
     *                 int        result  1:审核通过 0-通过失败
     *                 String     failMemo 备注
     * @return boolean
     */
    @RpcService
    public Map<String, Object> saveCheckResultEncrypt(Map<String, Object> paramMap) {
        String recipeIdE = MapValueUtil.getString(paramMap, "recipeId");
        //先解密recipeId
        try {
            String recipeS = AESUtils.decrypt(recipeIdE, "1234567890123gmw");
            paramMap.put("recipeId", Integer.valueOf(recipeS));
        } catch (Exception e) {
            LOGGER.error("saveCheckResultEncrypt-recipeId解密异常");
            throw new DAOException("处方号解密异常");
        }
        Map<String, Object> map = saveCheckResult(paramMap);
        return map;
    }

    /**
     * zhongzx
     * 保存药师审核平台审核结果
     *
     * @param paramMap 包含以下属性
     *                 int         recipeId 处方ID
     *                 int        checkOrgan  检查机构
     *                 int        checker    检查人员
     *                 int        result  1:审核通过 0-通过失败
     *                 String     failMemo 备注
     * @return boolean
     */
    @RpcService
    public Map<String, Object> saveCheckResult(Map<String, Object> paramMap) {
        Integer recipeId = MapValueUtil.getInteger(paramMap, "recipeId");
        Integer result = MapValueUtil.getInteger(paramMap, "result");
        if (null == recipeId || null == result) {
            throw new DAOException(DAOException.VALUE_NEEDED, "params are needed");
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (recipe == null) {
            throw new DAOException(609, "处方不存在");
        }
        //默认平台审核
        Integer checkMode = recipe.getCheckMode() == null ? 1 : recipe.getCheckMode();
        return getCheckService(checkMode).saveCheckResult(paramMap);
    }

    /**
     * 脱敏身份证号
     *
     * @param idCard
     * @return
     */
    private String hideIdCard(String idCard) {
        if (StringUtils.isEmpty(idCard)) {
            return "";
        }
        //显示前1-3位
        String str1 = idCard.substring(0, 3);
        //显示后15-18位
        String str2 = idCard.substring(14, 18);
        idCard = str1 + "***********" + str2;
        return idCard;
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
     * 药师搜索方法 开方医生 审方医生 患者姓名 患者patientId
     *
     * @param doctorId
     * @param searchString 搜索内容
     * @param searchFlag   0-开方医生 1-审方医生 2-患者姓名 3-病历号
     * @param organId      限定机构条件
     * @param start
     * @param limit
     * @return
     * @author zhongzx
     */
    @RpcService
    public List<Map<String, Object>> searchRecipeForChecker(Integer doctorId, String searchString, Integer searchFlag,
                                                            Integer organId, Integer start, Integer limit) {
        RecipeCheckDAO recipeCheckDAO = DAOFactory.getDAO(RecipeCheckDAO.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

        /**
         * 保存患者搜索记录
         */
        if (!StringUtils.isEmpty(searchString)) {
            ISearchContentService iSearchContentService = ApplicationUtils.getBaseService(ISearchContentService.class);
            SearchContentBean content = new SearchContentBean();
            content.setDoctorId(doctorId);
            content.setContent(searchString);
            content.setBussType(4);
            iSearchContentService.addSearchContent(content, 1);
        }

        DoctorDTO doctor = doctorService.get(doctorId);
        if (null == doctor) {
            LOGGER.error("doctor is null");
            throw new DAOException(ErrorCode.SERVICE_ERROR, "doctorId = " + doctorId + " 找不到该医生");
        }
        Set<Integer> organs = new HashSet<>();
        if (null != organId) {
            organs.add(organId);
        } else {
            List<Integer> organIds = findAPOrganIdsByDoctorId(doctorId);
            if (null == organIds || organIds.size() == 0) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "该药师没有配置能审核的机构");
            }
            organs.addAll(organIds);
        }

        List<Recipe> recipeList = recipeCheckDAO.searchRecipe(organs,
                searchFlag, searchString, start, limit);

        List<Map<String, Object>> mapList = new ArrayList<>();

        if (null != recipeList && recipeList.size() > 0) {
            for (Recipe r : recipeList) {
                Map<String, Object> map = Maps.newHashMap();
                //组装需要的处方数据
                RecipeBean recipe = new RecipeBean();
                recipe.setRecipeId(r.getRecipeId());
                recipe.setSignDate(r.getSignDate());
                recipe.setDoctor(r.getDoctor());
                recipe.setDepart(r.getDepart());
                recipe.setOrganDiseaseName(r.getOrganDiseaseName());
                recipe.setChecker(r.getChecker());
                //组装需要的患者数据
                PatientDTO p = patientService.get(r.getMpiid());
                PatientDTO patient = new PatientDTO();
                patient.setPatientName(p.getPatientName());
                patient.setPatientSex(p.getPatientSex());
                Date birthDay = p.getBirthday();
                if (null != birthDay) {
                    patient.setAge(DateConversion.getAge(birthDay));
                }
                //显示一条详情数据
                List<Recipedetail> details = detailDAO.findByRecipeId(r.getRecipeId());
                Recipedetail detail = null;
                if (null != details && details.size() > 0) {
                    detail = details.get(0);
                }
                //拿到当前药品详情的药品ID
                try {
                    Integer drugId = detail.getDrugId();
                    OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
                    if (organId != null) {
                        List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(drugId, organId);
                        if (CollectionUtils.isNotEmpty(organDrugLists)) {
                            detail.setDrugForm(organDrugLists.get(0).getDrugForm());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.info("searchRecipeForChecker recipe:{},error:{}.", JSONUtils.toString(recipe), e.getMessage());
                }

                //checkResult 0:未审核 1:通过 2:不通过
                Integer checkResult = getCheckResult(r);

                Date signDate = r.getSignDate();
                String dateString = "";
                if (null != signDate) {
                    dateString = DateConversion.getDateFormatter(signDate, "yyyy-MM-dd HH:mm");
                }
                map.put("dateString", dateString);
                map.put("recipe", recipe);
                map.put("patient", patient);
                map.put("check", checkResult);
                map.put("detail", ObjectCopyUtils.convert(detail, RecipeDetailBean.class));
                mapList.add(map);
            }
        }
        return mapList;
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
     * 自动审核通过情况
     *
     * @param result
     * @throws Exception
     */
    public void autoPassForCheckYs(CheckYsInfoBean result) throws Exception {
        Map<String, Object> checkParam = Maps.newHashMap();
        checkParam.put("recipeId", result.getRecipeId());
        checkParam.put("checkOrgan", result.getCheckOrganId());
        checkParam.put("checker", result.getCheckDoctorId());
        checkParam.put("result", 1);
        checkParam.put("failMemo", "");
        saveCheckResult(checkParam);
    }

    /**
     * 互联网医院 app首页 获取是否有待审核处方
     *
     * @param doctorId
     * @return
     */
    @RpcService
    public Boolean getUncheckRecipeFlag(Integer doctorId) {
        Boolean flag = false;
        long l = getUncheckedRecipeNum(doctorId);
        if (l > 0) {
            flag = true;
        }
        return flag;
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
            DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
            organIds = doctorService.findAPOrganIdsByDoctorId(doctorId);
        }

        return organIds;
    }

    private IRecipeCheckService getCheckService(Integer checkMode) {
        RecipeCheckEnum[] list = RecipeCheckEnum.values();
        String serviceName = null;
        for (RecipeCheckEnum e : list) {
            if (e.getCheckMode().equals(checkMode)) {
                serviceName = e.getServiceName();
                break;
            }
        }

        IRecipeCheckService recipeCheckService = null;
        if (StringUtils.isNotEmpty(serviceName)) {
            recipeCheckService = AppContextHolder.getBean(serviceName, IRecipeCheckService.class);
        }

        return recipeCheckService;
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

    public static void main(String[] args) {
        try{
            String encrypt1 = AESUtils.encrypt("3444", RECIPEID_SECRET);
            String encrypt2 = AESUtils.encrypt("3443", RECIPEID_SECRET);
            System.out.println("3444:"+encrypt1+"3443"+encrypt2);
        }catch (Exception e){

        }
    }

    /**
     * 点击抢单/取消抢单
     *
     * @param map
     * @return
     */
    @RpcService
    public Map<String, Object> grabOrderApply(Map<String, Object> map) {
        Map<String, Object> resultMap = Maps.newHashMap();
        Integer recipeId = MapUtils.getInteger(map, "recipeId");
        Integer doctorId = MapUtils.getInteger(map, "doctorId");
        Integer applyFlag = MapUtils.getInteger(map, "applyFlag");
        Args.notNull(recipeId, "recipeId");
        Args.notNull(doctorId, "doctorId");
        Args.notNull(applyFlag, "applyFlag");
        try {
            if (applyFlag.equals(1)) {
                RecipeCheck recipeCheck = new RecipeCheck();
                recipeCheck.setGrabDoctorId(doctorId);
                recipeCheck.setGrabOrderStatus(GrabOrderStatusConstant.GRAB_ORDERED_OWN);
                recipeCheck.setLocalLimitDate(eh.utils.DateConversion.getDateAftMinute(new Date(), 10));
                recipeCheck.setCheckStatus(RecipecCheckStatusConstant.Check_Normal);
                recipeCheck.setRecipeId(recipeId);
                RecipeCheck recipeCheck2 = recipeCheckDAO.getByRecipeIdAndCheckStatus(recipeId);
                if(null == recipeCheck2){
                    recipeCheckDAO.save(recipeCheck);
                }else{
                    recipeCheck2.setGrabOrderStatus(GrabOrderStatusConstant.GRAB_ORDERED_OWN);
                    recipeCheck2.setGrabDoctorId(doctorId);
                    recipeCheck2.setCheckStatus(RecipecCheckStatusConstant.Check_Normal);
                    recipeCheck2.setLocalLimitDate(eh.utils.DateConversion.getDateAftMinute(new Date(), 10));
                    recipeCheckDAO.update(recipeCheck2);
                }
                resultMap.put("grabOrderStatus", GrabOrderStatusConstant.GRAB_ORDERED_OWN);
            } else if (applyFlag.equals(0)) { //取消抢单
                RecipeCheck recipeCheck = recipeCheckDAO.getByRecipeIdAndCheckStatus(recipeId);
                if (null != recipeCheck) {
                    recipeCheck.setGrabOrderStatus(GrabOrderStatusConstant.GRAB_ORDER_NO);
                    recipeCheckDAO.update(recipeCheck);
                    //推送
                    sendRecipeReadyCheckYsPush(recipeCheck);
                    resultMap.put("grabOrderStatus", GrabOrderStatusConstant.GRAB_ORDER_NO);
                }
            }
        } catch (Exception e) {
            LOGGER.error("grabOrderApply error", e);
            resultMap.put("grabOrderStatus", GrabOrderStatusConstant.GRAB_ORDER_NO); //失败处理
            return resultMap;
        }
        return resultMap;
    }

    private void sendRecipeReadyCheckYsPush(RecipeCheck recipeCheck){
        Recipe byRecipeId = recipeDAO.getByRecipeId(recipeCheck.getRecipeId());
        SmsInfoBean info = new SmsInfoBean();
        info.setBusId(recipeCheck.getRecipeId());
        info.setBusType(RecipeMsgEnum.RECIPE_READY_CHECK_YS.getMsgType());
        info.setSmsType(RecipeMsgEnum.RECIPE_READY_CHECK_YS.getMsgType());
        info.setOrganId(byRecipeId.getClinicOrgan());
        info.setStatus(0);
        info.setExtendValue(String.valueOf(recipeCheck.getGrabDoctorId()));
        LOGGER.info("RecipeReadyCheckYs send msg : {}", JSONUtils.toString(info));
        ISmsPushService iSmsPushService = ApplicationUtils.getBaseService(ISmsPushService.class);
        iSmsPushService.pushMsg(info);
    }

    /**
     * 获取抢单状态和自动解锁时间
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
            RecipeCheck recipeCheck = recipeCheckDAO.getByRecipeIdAndCheckStatus(recipeId);
            if (null == recipeCheck || recipeCheck.getGrabOrderStatus().equals(GrabOrderStatusConstant.GRAB_ORDER_NO)) {
                resultMap.put("grabOrderStatus", GrabOrderStatusConstant.GRAB_ORDER_NO);
                resultMap.put("lockLimitTime", 10); //未抢单默认返回10
                return resultMap;
            }
            if (recipeCheck.getGrabOrderStatus().equals(1) && doctorId.equals(recipeCheck.getGrabDoctorId())) {//本人抢单
                resultMap.put("grabOrderStatus", GrabOrderStatusConstant.GRAB_ORDERED_OWN);
                long now = new Date().getTime();
                long localLimeDate = recipeCheck.getLocalLimitDate().getTime();
                long diff = localLimeDate - now;
                if (diff <= 0) {
                    resultMap.put("lockLimitTime", 0);
                } else {
                    int localLimitTime = (int) diff / (1000 * 60);
                    resultMap.put("lockLimitTime", localLimitTime);
                }

            } else if (recipeCheck.getGrabOrderStatus().equals(1) && !doctorId.equals(recipeCheck.getGrabDoctorId())) { //他人抢单
                resultMap.put("grabOrderStatus", GrabOrderStatusConstant.GRAB_ORDERED_OTHER);
                resultMap.put("lockLimitTime", 10); //他人抢单默认返回10
            }
            return resultMap;

        } catch (Exception e) {
            LOGGER.error("getGrabOrderStatusAndLimitTime error", e);
            throw new DAOException("查询失败");
        }
    }

    /**
     * 处方抢单解锁定时器
     */
    @RpcService
    public void cancelOverTimeRecipeCheck(){
        List<RecipeCheck> overTimeRecipeChecks = recipeCheckDAO.findOverTimeRecipeCheck();
        if(CollectionUtils.isNotEmpty(overTimeRecipeChecks)){
            overTimeRecipeChecks.forEach(item->{
                item.setGrabOrderStatus(GrabOrderStatusConstant.GRAB_ORDER_NO);
                item.setLocalLimitDate(null);
                LOGGER.info("处方抢单超时解锁，recipe={}", item.getRecipeId());
                recipeCheckDAO.update(item);
                sendRecipeReadyCheckYsPush(item);
            });
        }
    }
}
