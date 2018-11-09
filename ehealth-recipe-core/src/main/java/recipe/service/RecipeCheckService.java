package recipe.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.organconfig.service.IOrganConfigService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.searchcontent.model.SearchContentBean;
import com.ngari.base.searchcontent.service.ISearchContentService;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.audit.model.AuditListReq;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.CheckYsInfoBean;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

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
            PatientDTO patient;
            PatientDTO dbPatient;
            for (Recipe r : list) {
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
                } catch (Exception e) {
                    LOGGER.warn("covertRecipeListPageInfo patient is error. mpiId={}, ", r.getMpiid(), e);
                }
                //显示一条详情数据
                List<Recipedetail> details = detailDAO.findByRecipeId(r.getRecipeId());
                Recipedetail detail = null;
                if (null != details && details.size() > 0) {
                    detail = details.get(0);
                }
                //checkResult 0:未审核 1:通过 2:不通过
                Integer checkResult = getCheckResult(r);

                Date signDate = r.getSignDate();
                String dateString = "";
                if (null != signDate) {
                    dateString = DateConversion.getDateFormatter(signDate, "yyyy-MM-dd HH:mm");
                }
                map.put("dateString", dateString);
                map.put("recipe", ObjectCopyUtils.convert(recipe, RecipeBean.class));
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
    public Map<String, Object> findRecipeAndDetailsAndCheckById(int recipeId) {
        RecipeDAO rDao = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);

        //取recipe需要的字段
        Recipe recipe = rDao.getByRecipeId(recipeId);
        if (null == recipe) {
            LOGGER.error("findRecipeAndDetailsAndCheckById recipeId={} can't find.", recipeId);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "无法找到该处方单");
        }
        Integer doctorId = recipe.getDoctor();
        RecipeBean r = new RecipeBean();
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

        //取patient需要的字段
        PatientBean p = new PatientBean();
        try {
            PatientDTO patient = patientService.get(recipe.getMpiid());
            if (null != patient) {
                p.setPatientName(patient.getPatientName());
                p.setPatientSex(patient.getPatientSex());
                p.setAge(null == patient.getBirthday() ? 0 : DateConversion.getAge(patient.getBirthday()));
                p.setPatientType(patient.getPatientType());
                //加上手机号 和 身份证信息（脱敏）
                p.setMobile(patient.getMobile());
                p.setIdcard(hideIdCard(patient.getCertificate()));
                p.setMpiId(patient.getMpiId());
            } else {
                LOGGER.warn("findRecipeAndDetailsAndCheckById patient is null. mpiId={}", recipe.getMpiid());
            }
        } catch (Exception e) {
            LOGGER.warn("findRecipeAndDetailsAndCheckById get patient error. mpiId={}", recipe.getMpiid(), e);
        }

        Map<String, Object> map = Maps.newHashMap();
        List<Recipedetail> details = detailDAO.findByRecipeId(recipeId);
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
            }
        }
        RecipeOrderBean order = null;
        String orderCode = recipe.getOrderCode();
        if (!StringUtils.isEmpty(orderCode)) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
            order = ObjectCopyUtils.convert(recipeOrder, RecipeOrderBean.class);
        }

        map.put("dateString", dateString);
        map.put("recipe", r);
        map.put("patient", p);
        map.put("doctor", doc);
        map.put("details", ObjectCopyUtils.convert(details, RecipeDetailBean.class));
        map.put("drugsEnterprise", e);
        map.put("recipeOrder", order);
        return map;
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
     * @param recipe checkResult 0:未审核 1:通过 2:不通过 3:二次签名
     * @return
     */
    private Integer getCheckResult(Recipe recipe) {
        Integer checkResult = 0;
        Integer status = recipe.getStatus();
        if (RecipeStatusConstant.READY_CHECK_YS == status) {
            checkResult = 0;
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
                    if (1 == recipeCheck.getCheckStatus()) {
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

        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        //审核处方单（药师相关数据处理）
        CheckYsInfoBean resultBean = recipeService.reviewRecipe(paramMap);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        //审核成功往药厂发消息
        if (1 == result) {
            recipeService.afterCheckPassYs(recipe);
        } else {
            IOrganConfigService iOrganConfigService = ApplicationUtils.getBaseService(IOrganConfigService.class);
            boolean secondsignflag = iOrganConfigService.getEnableSecondsignByOrganId(recipe.getClinicOrgan());
            //不支持二次签名的机构直接执行后续操作
            if (!secondsignflag) {
                recipeService.afterCheckNotPassYs(recipe);
            }
        }

        Map<String, Object> resMap = Maps.newHashMap();
        resMap.put("result", resultBean.isRs());
        resMap.put("recipeId", recipeId);
        //把审核结果再返回前端 0:未审核 1:通过 2:不通过
        resMap.put("check", (1 == result) ? 1 : 2);

        //将审核结果推送HIS
        try {
            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
            hisService.recipeAudit(recipe, resultBean);
        } catch (Exception e) {
            LOGGER.warn("saveCheckResult send recipeAudit to his error. recipeId={}", recipeId, e);
        }

        return resMap;
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
            IDoctorService iDoctorService = ApplicationUtils.getBaseService(IDoctorService.class);
            organIds = iDoctorService.findAPOrganIdsByDoctorId(doctorId);
        }

        return organIds;
    }
}
