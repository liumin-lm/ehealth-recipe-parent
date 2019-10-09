package recipe.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.department.service.IDepartmentService;
import com.ngari.base.esign.service.IESignBaseService;
import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.base.organconfig.service.IOrganConfigService;
import com.ngari.base.patient.model.DocIndexBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.base.payment.service.IPaymentService;
import com.ngari.consult.common.service.IConsultService;
import com.ngari.his.recipe.mode.DrugInfoTO;
import com.ngari.patient.ds.PatientDS;
import com.ngari.patient.dto.ConsultSetDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.*;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.audit.model.AuditMedicinesDTO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.*;
import com.ngari.wxpay.service.INgariRefundService;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.base.constant.ErrorCode;
import eh.base.constant.PageConstant;
import eh.cdr.constant.OrderStatusConstant;
import eh.utils.params.ParameterConstant;
import eh.wxpay.constant.PayConstant;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.audit.auditmode.AuditModeContext;
import recipe.bean.CheckYsInfoBean;
import recipe.bean.DrugEnterpriseResult;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.RecipeValidateUtil;
import recipe.constant.*;
import recipe.dao.*;
import recipe.dao.bean.PatientRecipeBean;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.hisservice.RecipeToHisCallbackService;
import recipe.hisservice.syncdata.SyncExecutorService;
import recipe.purchase.PurchaseService;
import recipe.service.common.RecipeCacheService;
import recipe.thread.PushRecipeToHisCallable;
import recipe.thread.PushRecipeToRegulationCallable;
import recipe.thread.RecipeBusiThreadPool;
import recipe.thread.UpdateRecipeStatusFromHisCallable;
import recipe.util.DateConversion;
import recipe.util.DigestUtil;
import recipe.util.MapValueUtil;
import recipe.util.RedisClient;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * 处方服务类
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2016/4/27.
 */
@RpcBean("recipeService")
public class RecipeService extends RecipeBaseService{

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeService.class);

    private static final String UNSIGN = "unsign";

    private static final String UNCHECK = "uncheck";

    private static final String EXTEND_VALUE_FLAG = "1";

    private PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);

    private DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);

    private OrganService organService = ApplicationUtils.getBasicService(OrganService.class);

    private static IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);

    private RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);

    private static final int havChooseFlag = 1;
    @Autowired
    private RedisClient redisClient;

    @Resource
    private AuditModeContext auditModeContext;

    /**
     * 药师审核不通过
     */
    public static final int CHECK_NOT_PASS = 2;
    /**
     * 推送药企失败
     */
    public static final int PUSH_FAIL = 3;
    /**
     * 手动退款
     */
    public static final int REFUND_MANUALLY = 4;

    public static final String WX_RECIPE_BUSTYPE = "recipe";

    public static final Integer RECIPE_EXPIRED_DAYS = 3;

    /**
     * 二次签名处方审核不通过过期时间
     */
    public static final Integer RECIPE_EXPIRED_SECTION = 30;

    /**
     * 过期处方查询起始天数
     */
    public static final Integer RECIPE_EXPIRED_SEARCH_DAYS = 13;

    @RpcService
    public RecipeBean getByRecipeId(int recipeId) {
        Recipe recipe = DAOFactory.getDAO(RecipeDAO.class).get(recipeId);
        return ObjectCopyUtils.convert(recipe, RecipeBean.class);
    }

    @RpcService
    public List<RecipeBean> findRecipe(int start,int limit) {
        List<Recipe> recipes = DAOFactory.getDAO(RecipeDAO.class).findRecipeByStartAndLimit(start,limit);
        return ObjectCopyUtils.convert(recipes, RecipeBean.class);
    }

    /**
     * 判断医生是否可以处方
     *
     * @param doctorId 医生ID
     * @return Map<String, Object>
     */
    @RpcService
    public Map<String, Object> openRecipeOrNot(Integer doctorId) {
        EmploymentService employmentService = ApplicationUtils.getBasicService(EmploymentService.class);
        ConsultSetService consultSetService = ApplicationUtils.getBasicService(ConsultSetService.class);

        Boolean canCreateRecipe = false;
        String tips = "";
        Map<String, Object> map = Maps.newHashMap();
        List<EmploymentDTO> employmentList = employmentService.findEmploymentByDoctorId(doctorId);
        List<Integer> organIdList = new ArrayList<>();
        if (employmentList.size() > 0) {
            for (EmploymentDTO employment : employmentList) {
                organIdList.add(employment.getOrganId());
            }
            OrganDrugListDAO organDrugListDAO = getDAO(OrganDrugListDAO.class);
            int listNum = organDrugListDAO.getCountByOrganIdAndStatus(organIdList);
            canCreateRecipe = listNum > 0;
            if (!canCreateRecipe) {
                tips = "抱歉，您所在医院暂不支持开处方业务。";
            }
        }

        //能否开医保处方
        boolean medicalFlag = false;
        if (canCreateRecipe) {
            ConsultSetDTO set = consultSetService.getBeanByDoctorId(doctorId);
            if (null != set && null != set.getMedicarePrescription()) {
                medicalFlag = (true == set.getMedicarePrescription()) ? true : false;
            }
        }

        map.put("result", canCreateRecipe);
        map.put("medicalFlag", medicalFlag);
        map.put("tips", tips);
        return map;

    }

    /**
     * 新的处方列表  pc端仍在使用
     *
     * @param doctorId 医生ID
     * @param start    记录开始下标
     * @param limit    每页限制条数
     * @return list
     */
    @RpcService
    public List<HashMap<String, Object>> findNewRecipeAndPatient(int doctorId, int start, int limit) {
        checkUserHasPermissionByDoctorId(doctorId);
        return RecipeServiceSub.findRecipesAndPatientsByDoctor(doctorId, start, PageConstant.getPageLimit(limit), 0);
    }

    /**
     * 历史处方列表 pc端仍在使用
     *
     * @param doctorId 医生ID
     * @param start    记录开始下标
     * @param limit    每页限制条数
     * @return list
     */
    @RpcService
    public List<HashMap<String, Object>> findOldRecipeAndPatient(int doctorId, int start, int limit) {
        checkUserHasPermissionByDoctorId(doctorId);
        return RecipeServiceSub.findRecipesAndPatientsByDoctor(doctorId, start, PageConstant.getPageLimit(limit), 1);
    }

    /**
     * 强制删除处方(接收医院处方发送失败时处理)
     *
     * @param recipeId 处方ID
     * @return boolean
     */
    @RpcService
    public Boolean delRecipeForce(int recipeId) {
        LOGGER.info("delRecipeForce [recipeId:" + recipeId + "]");
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeExtendDAO recipeExtendDAO=getDAO(RecipeExtendDAO.class);
        recipeDAO.remove(recipeId);
        recipeExtendDAO.remove(recipeId);
        return true;
    }

    /**
     * 删除处方
     *
     * @param recipeId 处方ID
     * @return boolean
     */
    @RpcService
    public Boolean delRecipe(int recipeId) {
        LOGGER.info("delRecipe [recipeId:" + recipeId + "]");
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方单不存在或者已删除");
        }
        if (null == recipe.getStatus() || recipe.getStatus() > RecipeStatusConstant.UNSIGN) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方单不是新处方或者审核失败的处方，不能删除");
        }

        boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.DELETE, null);

        //记录日志
        RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusConstant.DELETE, "删除处方单");

        return rs;
    }

    /**
     * 撤销处方单
     *
     * @param recipeId 处方ID
     * @return boolean
     */
    @RpcService
    public Boolean undoRecipe(int recipeId) {
        LOGGER.info("undoRecipe [recipeId：" + recipeId + "]");
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方单不存在或者已删除");
        }
        if (null == recipe.getStatus() || RecipeStatusConstant.UNCHECK != recipe.getStatus()) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方单不是待审核的处方，不能撤销");
        }

        boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.UNSIGN, null);

        //记录日志
        RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusConstant.UNSIGN, "撤销处方单");

        return rs;
    }

    /**
     * 保存处方
     *
     * @param recipeBean  处方对象
     * @param detailBeanList 处方详情
     * @return int
     */
    @RpcService
    public Integer saveRecipeData(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList) {
        Integer recipeId = RecipeServiceSub.saveRecipeDataImpl(recipeBean, detailBeanList, 1);
        if (RecipeBussConstant.FROMFLAG_HIS_USE.equals(recipeBean.getFromflag())) {
            //生成订单数据，与 HosPrescriptionService 中 createPrescription 方法一致
            HosPrescriptionService service = AppContextHolder.getBean("hosPrescriptionService", HosPrescriptionService.class);
            recipeBean.setRecipeId(recipeId);
            //设置订单基本参数
            HospitalRecipeDTO hospitalRecipeDTO = new HospitalRecipeDTO();
            hospitalRecipeDTO.setRecipeCode(recipeBean.getRecipeCode());
            hospitalRecipeDTO.setOrderTotalFee(recipeBean.getTotalMoney().toPlainString());
            hospitalRecipeDTO.setActualFee(hospitalRecipeDTO.getOrderTotalFee());
            recipeBean.setPayFlag(PayConstant.PAY_FLAG_NOT_PAY);
            service.createBlankOrderForHos(recipeBean, hospitalRecipeDTO);
        }
        return recipeId;
    }

    /**
     * 保存HIS处方
     *
     * @param recipe
     * @param details
     * @return
     */
    public Integer saveRecipeDataForHos(RecipeBean recipe, List<RecipeDetailBean> details) {
        return RecipeServiceSub.saveRecipeDataImpl(recipe, details, 0);
    }

    /**
     * 保存处方电子病历
     *
     * @param recipe 处方对象
     */
    public void saveRecipeDocIndex(Recipe recipe) {
        IDepartmentService iDepartmentService = ApplicationUtils.getBaseService(IDepartmentService.class);

        DocIndexBean docIndex = new DocIndexBean();
        String docType = "3";
        try {
            String docTypeText = DictionaryController.instance().get("eh.cdr.dictionary.DocType").getText(docType);
            docIndex.setDocSummary(docTypeText);
            docIndex.setDoctypeName(docTypeText);
        } catch (ControllerException e) {
            LOGGER.error("saveRecipeDocIndex DocType dictionary error! docType=" + docType);
        }
        try {
            String recipeTypeText = DictionaryController.instance().get("eh.cdr.dictionary.RecipeType").getText(recipe.getRecipeType());
            docIndex.setDocTitle(recipeTypeText);
        } catch (ControllerException e) {
            LOGGER.error("saveRecipeDocIndex RecipeType dictionary error! recipeType=" + recipe.getRecipeType());
        }
        docIndex.setDocId(recipe.getRecipeId());
        docIndex.setMpiid(recipe.getMpiid());
        docIndex.setCreateOrgan(recipe.getClinicOrgan());
        docIndex.setCreateDepart(recipe.getDepart());
        docIndex.setCreateDoctor(recipe.getDoctor());
        docIndex.setDoctorName(doctorService.getNameById(recipe.getDoctor()));
        docIndex.setDepartName(iDepartmentService.getNameById(recipe.getDepart()));
        iPatientService.saveRecipeDocIndex(docIndex, docType, 3);
    }

    /**
     * 根据处方ID获取完整地址
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public String getCompleteAddress(Integer recipeId) {
        String address = "";
        if (null != recipeId) {
            CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
            RecipeDAO recipeDAO = getDAO(RecipeDAO.class);

            Recipe recipe = recipeDAO.get(recipeId);
            if (null != recipe) {
                if (null != recipe.getAddressId()) {
                    StringBuilder sb = new StringBuilder();
                    commonRemoteService.getAddressDic(sb, recipe.getAddress1());
                    commonRemoteService.getAddressDic(sb, recipe.getAddress2());
                    commonRemoteService.getAddressDic(sb, recipe.getAddress3());
                    sb.append(StringUtils.isEmpty(recipe.getAddress4()) ? "" : recipe.getAddress4());
                    address = sb.toString();
                }

                if (StringUtils.isEmpty(address)) {
                    RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
                    //从订单获取
                    RecipeOrder order = recipeOrderDAO.getOrderByRecipeId(recipeId);
                    if (null != order && null != order.getAddressID()) {
                        address = commonRemoteService.getCompleteAddress(order);
                    }
                }
            }
        }

        return address;
    }

    /**
     * 审核处方结果(药师平台)
     *
     * @param paramMap 参数
     *                 包含一下属性
     *                 int         recipeId 处方ID
     *                 int        checkOrgan  检查机构
     *                 int        checker    检查人员
     *                 int        result  1:审核通过 0-通过失败
     *                 String     failMemo 备注
     *                 List<Map<String, Object>>     checkList
     */
    public CheckYsInfoBean reviewRecipe(Map<String, Object> paramMap) {
        Integer recipeId = MapValueUtil.getInteger(paramMap, "recipeId");
        Integer checkOrgan = MapValueUtil.getInteger(paramMap, "checkOrgan");
        Integer checker = MapValueUtil.getInteger(paramMap, "checker");
        Integer checkFlag = MapValueUtil.getInteger(paramMap, "result");
        //是否是线下药师审核标记
        Integer hosAuditFlag = MapValueUtil.getInteger(paramMap, "hosAuditFlag");
        CheckYsInfoBean resultBean = new CheckYsInfoBean();
        resultBean.setRecipeId(recipeId);
        resultBean.setCheckResult(checkFlag);
        resultBean.setCheckDoctorId(checker);
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        //校验数据
        if (null == recipeId || null == checkOrgan || null == checker || null == checkFlag) {
            throw new DAOException(DAOException.VALUE_NEEDED, "recipeId or checkOrgan or checker or result is null");
        }
        String memo = MapValueUtil.getString(paramMap, "failMemo");
        resultBean.setCheckFailMemo(memo);
        Object checkListObj = paramMap.get("checkList");

        List<Map<String, Object>> checkList = null;
        if (null != checkListObj && checkListObj instanceof List) {
            checkList = (List<Map<String, Object>>) checkListObj;
        }
        //如果审核不通过 详情审核结果不能为空
        if (0 == checkFlag) {
            if (null == checkList || checkList.size() == 0) {
                throw new DAOException(DAOException.VALUE_NEEDED, "详情审核结果不能为空");
            } else {
                for (Map<String, Object> map : checkList) {
                    String recipeDetailIds = MapValueUtil.getString(map, "recipeDetailIds");
                    String reasonIds = MapValueUtil.getString(map, "reasonIds");
                    if (StringUtils.isEmpty(recipeDetailIds) || StringUtils.isEmpty(reasonIds)) {
                        throw new DAOException(DAOException.VALUE_NEEDED, "请选择不通过理由以及不合理药品");
                    }
                }
            }
        }
        LOGGER.info("reviewRecipe [recipeId：" + recipeId + ",checkFlag: " + checkFlag + "]");
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方单不存在或者已删除");
        }
        if (hosAuditFlag == null){
            if (null == recipe.getStatus() || recipe.getStatus() != RecipeStatusConstant.READY_CHECK_YS) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方已被审核");
            }
        }

        int beforeStatus = recipe.getStatus();
        String logMemo = "审核不通过(药师平台，药师：" + checker + "):" + memo;
        int recipeStatus = RecipeStatusConstant.CHECK_NOT_PASS_YS;
        if (1 == checkFlag) {
            //根据审方模式改变状态
            recipeStatus = auditModeContext.getAuditModes(recipe.getReviewType()).afterAuditRecipeChange();
            if (recipe.canMedicalPay()) {
                //如果是可医保支付的单子，审核是在用户看到之前，所以审核通过之后变为待处理状态
                recipeStatus = RecipeStatusConstant.CHECK_PASS;
            }
            logMemo = "审核通过(药师平台，药师：" + checker + ")";
        }

        Date now = DateTime.now().toDate();
        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("checkDateYs", now);
        attrMap.put("checkOrgan", checkOrgan);
        attrMap.put("checker", checker);
        attrMap.put("checkFailMemo", (StringUtils.isEmpty(memo)) ? "" : memo);

        //保存审核记录和详情审核记录
        RecipeCheck recipeCheck = new RecipeCheck();
        recipeCheck.setChecker(checker);
        recipeCheck.setRecipeId(recipeId);
        recipeCheck.setCheckOrgan(checkOrgan);
        recipeCheck.setCheckDate(now);
        recipeCheck.setMemo((StringUtils.isEmpty(memo)) ? "" : memo);
        recipeCheck.setCheckStatus(checkFlag);
        DoctorDTO doctorDTO = doctorService.getByDoctorId(checker);
        if (doctorDTO != null) {
            recipeCheck.setCheckerName(doctorDTO.getName());
        }
        List<RecipeCheckDetail> recipeCheckDetails;
        if (0 == checkFlag) {
            recipeCheckDetails = new ArrayList<>();
            for (Map<String, Object> map : checkList) {
                //这里的数组是已字符串的形式传入保存，查询详情时需要解析成数组
                String recipeDetailIds = MapValueUtil.getString(map, "recipeDetailIds");
                String reasonIds = MapValueUtil.getString(map, "reasonIds");
                if (StringUtils.isEmpty(recipeDetailIds) || StringUtils.isEmpty(reasonIds)) {
                    throw new DAOException(DAOException.VALUE_NEEDED, "请选择不通过理由以及不合理药品");
                }
                RecipeCheckDetail recipeCheckDetail = new RecipeCheckDetail();
                recipeCheckDetail.setRecipeDetailIds(recipeDetailIds);
                recipeCheckDetail.setReasonIds(reasonIds);
                recipeCheckDetails.add(recipeCheckDetail);
            }
        } else {
            recipeCheckDetails = null;
        }
        RecipeCheckDAO recipeCheckDAO = getDAO(RecipeCheckDAO.class);
        recipeCheckDAO.saveRecipeCheckAndDetail(recipeCheck, recipeCheckDetails);

        boolean bl = recipeDAO.updateRecipeInfoByRecipeId(recipeId, recipeStatus, attrMap);
        if (!bl) {
            LOGGER.error("reviewRecipe update recipe[" + recipeId + "] error!");
            resultBean.setRs(bl);
            return resultBean;
        }

        //记录日志
        RecipeLogService.saveRecipeLog(recipeId, beforeStatus, recipeStatus, logMemo);
        if (1 == checkFlag) {
            String errorMsg = "";
            if (null != recipe.getSignFile()) {
                IESignBaseService esignService = ApplicationUtils.getBaseService(IESignBaseService.class);

                Map<String, Object> dataMap = Maps.newHashMap();
                dataMap.put("fileName", "recipecheck_" + recipeId + ".pdf");
                dataMap.put("recipeSignFileId", recipe.getSignFile());
                if (RecipeUtil.isTcmType(recipe.getRecipeType())){
                    dataMap.put("templateType","tcm");
                }else {
                    dataMap.put("templateType","wm");
                }

                Map<String, Object> backMap = esignService.signForRecipe(false, checker, dataMap);
                //0表示成功
                Integer code = MapValueUtil.getInteger(backMap, "code");
                if (Integer.valueOf(0).equals(code)) {
                    String recipeFileId = MapValueUtil.getString(backMap, "fileId");
                    bl = recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.<String, Object>of("chemistSignFile", recipeFileId));
                } else {
                    LOGGER.error("reviewRecipe signFile error. recipeId={}, result={}", recipeId, JSONUtils.toString(backMap));
                    errorMsg = JSONUtils.toString(backMap);
                    bl = false;
                }
            } else {
                LOGGER.error("reviewRecipe signFile is empty recipeId=" + recipeId);
                errorMsg = "signFileId is empty. recipeId=" + recipeId;
                bl = false;
            }

            if (!bl) {
                RecipeLogService.saveRecipeLog(recipeId, beforeStatus, recipeStatus, "reviewRecipe 添加药师签名失败. " + errorMsg);
            }
        }

        resultBean.setRs(bl);
        resultBean.setCheckDetailList(recipeCheckDetails);
        return resultBean;
    }

    /**
     * 药师审核不通过的情况下，医生重新开处方
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public List<RecipeDetailBean> reCreatedRecipe(Integer recipeId) {
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Recipe dbRecipe = RecipeValidateUtil.checkRecipeCommonInfo(recipeId, resultBean);
        if (null == dbRecipe) {
            LOGGER.error("reCreatedRecipe 平台无该处方对象. recipeId=[{}] error={}", recipeId, JSONUtils.toString(resultBean));
            return Lists.newArrayList();
        }
        Integer status = dbRecipe.getStatus();
        if (null == status || status != RecipeStatusConstant.CHECK_NOT_PASS_YS) {
            LOGGER.error("reCreatedRecipe 该处方不是审核未通过的处方. recipeId=[{}]", recipeId);
            return Lists.newArrayList();
        }
        //根据审方模式改变--审核未通过处理
        auditModeContext.getAuditModes(dbRecipe.getReviewType()).afterCheckNotPassYs(dbRecipe);
        List<Recipedetail> detailBeanList = RecipeValidateUtil.validateDrugsImpl(dbRecipe);
        return ObjectCopyUtils.convert(detailBeanList, RecipeDetailBean.class);
    }

    /**
     * 重新开具 或这续方时校验 药品数据
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public List<RecipeDetailBean> validateDrugs(Integer recipeId) {
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Recipe dbRecipe = RecipeValidateUtil.checkRecipeCommonInfo(recipeId, resultBean);
        if (null == dbRecipe) {
            LOGGER.error("validateDrugs 平台无该处方对象. recipeId=[{}] error={}", recipeId, JSONUtils.toString(resultBean));
            return Lists.newArrayList();
        }
        List<Recipedetail> detailBeanList = RecipeValidateUtil.validateDrugsImpl(dbRecipe);
        return ObjectCopyUtils.convert(detailBeanList, RecipeDetailBean.class);
    }

    /**
     * 生成pdf并签名
     *
     * @param recipeId
     */
    @RpcService
    public void generateRecipePdfAndSign(Integer recipeId) {
        if (null == recipeId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipeId is null");
        }
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeDetailDAO detailDAO = getDAO(RecipeDetailDAO.class);
        IESignBaseService esignService = ApplicationUtils.getBaseService(IESignBaseService.class);

        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        List<Recipedetail> details = detailDAO.findByRecipeId(recipeId);

        //组装生成pdf的参数
        String fileName = "recipe_" + recipeId + ".pdf";
        Map<String, Object> paramMap = Maps.newHashMap();
        recipe.setSignDate(DateTime.now().toDate());
        if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
            //中药pdf参数
            paramMap = RecipeServiceSub.createParamMapForChineseMedicine(recipe, details, fileName);
        } else {
            paramMap = RecipeServiceSub.createParamMap(recipe, details, fileName);
            //上传处方图片
            generateRecipeImageAndUpload(recipeId,paramMap);
        }
        //上传阿里云
        String memo = "";
        Map<String, Object> backMap = esignService.signForRecipe(true, recipe.getDoctor(), paramMap);
        String imgFileId = MapValueUtil.getString(backMap, "imgFileId");
        Map<String, Object> attrMapimg = Maps.newHashMap();
        attrMapimg.put("signImg",imgFileId);
        recipeDAO.updateRecipeInfoByRecipeId(recipeId, attrMapimg);
        LOGGER.info("generateRecipeImg 签名图片成功. fileId={}, recipeId={}", imgFileId, recipe.getRecipeId());
        //0表示成功
        Integer code = MapValueUtil.getInteger(backMap, "code");
        if (Integer.valueOf(0).equals(code)) {
            String recipeFileId = MapValueUtil.getString(backMap, "fileId");
            Map<String, Object> attrMap = Maps.newHashMap();
            attrMap.put("signFile", recipeFileId);
            attrMap.put("signDate", recipe.getSignDate());
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, attrMap);
            memo = "签名上传文件成功, fileId=" + recipeFileId;
            LOGGER.info("generateRecipePdfAndSign 签名成功. fileId={}, recipeId={}", recipeFileId, recipe.getRecipeId());
        } else {
            memo = "签名上传文件失败！原因：" + MapValueUtil.getString(backMap, "msg");
            LOGGER.error("generateRecipePdfAndSign 签名上传文件失败. recipeId={}, result={}", recipe.getRecipeId(), JSONUtils.toString(backMap));
        }

        //日志记录
        RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), memo);
    }

    //暂时只有西药能生成处方图片
    private void generateRecipeImageAndUpload(Integer recipeId,Map<String, Object> paramMap) {
        String fileName = "img_recipe_" + recipeId + ".jpg";
        try {
            /*//先生成本地图片文件----这里通过esign去生成
            byte[] bytes = CreateRecipeImageUtil.createImg(paramMap);*/
            paramMap.put("recipeImgId",recipeId);
            /*paramMap.put("recipeImgData",bytes);*/
        }catch (Exception e){
            LOGGER.error("uploadRecipeImgSignFile exception:" + e.getMessage());
        }
    }

    /**
     * 重试
     *
     * @param recipeId
     */
    @RpcService
    public RecipeResultBean sendNewRecipeToHIS(Integer recipeId) {
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

        Integer status = recipeDAO.getStatusByRecipeId(recipeId);
        if (null == status || status != RecipeStatusConstant.CHECKING_HOS) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方不能重试");
        }

        //HIS消息发送
        RecipeResultBean scanResult = hisService.scanDrugStockByRecipeId(recipeId);
        if (RecipeResultBean.FAIL.equals(scanResult.getCode())) {
            resultBean.setCode(scanResult.getCode());
            resultBean.setMsg(scanResult.getError());
            if (EXTEND_VALUE_FLAG.equals(scanResult.getExtendValue())) {
                resultBean.setError(scanResult.getError());
            }
            return resultBean;
        }

        hisService.recipeSendHis(recipeId, null);
        return resultBean;
    }

    /**
     * 发送只能配送处方，当医院库存不足时医生略过库存提醒后调用
     *
     * @param recipeBean
     * @return
     */
    @RpcService
    public Map<String, Object> sendDistributionRecipe(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList) {
        if (null == recipeBean) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "传入参数为空");
        }

        recipeBean.setDistributionFlag(1);
        recipeBean.setGiveMode(RecipeBussConstant.GIVEMODE_SEND_TO_HOME);
        return doSignRecipeExt(recipeBean, detailBeanList);
    }

    /**
     * 签名服务
     *
     * @param recipe  处方
     * @param details 详情
     * @return Map<String, Object>
     */
    @RpcService
    public Map<String, Object> doSignRecipe(RecipeBean recipe, List<RecipeDetailBean> details) {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        DrugsEnterpriseService drugsEnterpriseService = ApplicationUtils.getRecipeService(DrugsEnterpriseService.class);

        Map<String, Object> rMap = Maps.newHashMap();
        PatientDTO patient = patientService.get(recipe.getMpiid());
        //解决旧版本因为wx2.6患者身份证为null，而业务申请不成功
        if (patient == null || StringUtils.isEmpty(patient.getCertificate())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该患者还未填写身份证信息，不能开处方");
        }
        // 就诊人改造：为了确保删除就诊人后历史处方不会丢失，加入主账号用户id
        PatientDTO requestPatient = patientService.getOwnPatientForOtherProject(patient.getLoginId());
        if (null != requestPatient && null != requestPatient.getMpiId()) {
            recipe.setRequestMpiId(requestPatient.getMpiId());
            // urt用于系统消息推送
            recipe.setRequestUrt(requestPatient.getUrt());
        }
        //根据申请人mpiid，requestMode 获取当前咨询单consultId
        IConsultService iConsultService = ApplicationUtils.getConsultService(IConsultService.class);
        List<Integer> consultIds = iConsultService.findApplyingConsultByRequestMpiAndDoctorId(recipe.getRequestMpiId(),
                recipe.getDoctor(), RecipeSystemConstant.CONSULT_TYPE_RECIPE);
        Integer consultId = null;
        if (CollectionUtils.isNotEmpty(consultIds)) {
            consultId = consultIds.get(0);
            recipe.setClinicId(consultId);
            rMap.put("consultId", consultId);
        }
        recipe.setStatus(RecipeStatusConstant.UNSIGN);
        recipe.setSignDate(DateTime.now().toDate());
        Integer recipeId = recipe.getRecipeId();
        //如果是已经暂存过的处方单，要去数据库取状态 判断能不能进行签名操作
        if (null != recipeId && recipeId > 0) {
            Integer status = recipeDAO.getStatusByRecipeId(recipeId);
            if (null == status || status > RecipeStatusConstant.UNSIGN) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "处方单已处理,不能重复签名");
            }

            updateRecipeAndDetail(recipe, details);
        } else {
            recipeId = saveRecipeData(recipe, details);
            recipe.setRecipeId(recipeId);
        }

        //非只能配送处方需要进行医院库存校验
        if (!Integer.valueOf(1).equals(recipe.getDistributionFlag())) {
            //HIS消息发送
            RecipeResultBean scanResult = hisService.scanDrugStockByRecipeId(recipeId);
            if (RecipeResultBean.FAIL.equals(scanResult.getCode())) {
                rMap.put("signResult", false);
                rMap.put("recipeId", recipeId);
                rMap.put("msg", scanResult.getError());
                if (EXTEND_VALUE_FLAG.equals(scanResult.getExtendValue())) {
                    //这个字段为true，前端展示框内容为msg，走二次确认配送流程
                    rMap.put("scanDrugStock", true);
                }
                return rMap;
            }
        }

        boolean checkEnterprise = drugsEnterpriseService.checkEnterprise(recipe.getClinicOrgan());
        if (checkEnterprise) {
            //药企库存实时查询
            RecipePatientService recipePatientService = ApplicationUtils.getRecipeService(RecipePatientService.class);
            RecipeResultBean recipeResultBean = recipePatientService.findSupportDepList(0, Arrays.asList(recipeId));
            if (RecipeResultBean.FAIL.equals(recipeResultBean.getCode())) {
                LOGGER.error("doSignRecipe scanStock enterprise error. result={} ", JSONUtils.toString(recipeResultBean));
//            throw new DAOException(ErrorCode.SERVICE_ERROR, "很抱歉，当前库存不足无法开处方，请联系客服：" +
//                    iSysParamterService.getParam(ParameterConstant.KEY_CUSTOMER_TEL, RecipeSystemConstant.CUSTOMER_TEL));
                rMap.put("signResult", false);
                rMap.put("recipeId", recipeId);
                //错误信息弹出框，只有 确定  按钮
                rMap.put("errorFlag", true);
                rMap.put("msg", "很抱歉，当前库存不足无法开处方，请联系客服：" +
                        cacheService.getParam(ParameterConstant.KEY_CUSTOMER_TEL, RecipeSystemConstant.CUSTOMER_TEL));
                return rMap;
            }
        }

        //HIS消息发送--异步处理
        /*boolean result = hisService.recipeSendHis(recipeId, null);*/
        RecipeBusiThreadPool.submit(new PushRecipeToHisCallable(recipeId));
        rMap.put("signResult", true);
        rMap.put("recipeId", recipeId);
        rMap.put("errorFlag", false);

        LOGGER.info("doSignRecipe execute ok! rMap:" + JSONUtils.toString(rMap));
        return rMap;
    }

    /**
     * 修改处方
     *
     * @param recipeBean       处方对象
     * @param detailBeanList   处方详情
     */
    @RpcService
    public Integer updateRecipeAndDetail(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList) {
        if (recipeBean == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "recipe is required!");
        }
        Integer recipeId = recipeBean.getRecipeId();
        if (recipeId == null || recipeId <= 0) {
            throw new DAOException(DAOException.VALUE_NEEDED, "recipeId is required!");
        }
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = getDAO(RecipeDetailDAO.class);

        Recipe recipe = ObjectCopyUtils.convert(recipeBean, Recipe.class);
        List<Recipedetail> recipedetails = ObjectCopyUtils.convert(detailBeanList, Recipedetail.class);

        Recipe dbRecipe = recipeDAO.getByRecipeId(recipeId);
        if (null == dbRecipe.getStatus() || dbRecipe.getStatus() > RecipeStatusConstant.UNSIGN) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方单不是新处方或者审核失败的处方，不能修改");
        }

        int beforeStatus = dbRecipe.getStatus();
        if (null == recipedetails) {
            recipedetails = new ArrayList<>(0);
        }
        for (Recipedetail recipeDetail : recipedetails) {
            RecipeValidateUtil.validateRecipeDetailData(recipeDetail, dbRecipe);
        }
        //由于使用BeanUtils.map，空的字段不会进行copy，要进行手工处理
        if (StringUtils.isEmpty(recipe.getMemo())) {
            dbRecipe.setMemo("");
        }
        //医嘱
        if (StringUtils.isEmpty(recipe.getRecipeMemo())) {
            dbRecipe.setRecipeMemo("");
        }
        //复制修改的数据
        BeanUtils.map(recipe, dbRecipe);
        //设置药品价格
        boolean isSucc = RecipeServiceSub.setDetailsInfo(dbRecipe, recipedetails);
        if (!isSucc) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品详情数据有误");
        }
        //将原先处方单详情的记录都置为无效 status=0
        recipeDetailDAO.updateDetailInvalidByRecipeId(recipeId);
        Integer dbRecipeId = recipeDAO.updateOrSaveRecipeAndDetail(dbRecipe, recipedetails, true);

        //武昌需求，加入处方扩展信息
        RecipeExtendBean recipeExt = recipeBean.getRecipeExtend();
        if(null != recipeExt && null != dbRecipeId) {
            RecipeExtend recipeExtend = ObjectCopyUtils.convert(recipeExt, RecipeExtend.class);
            recipeExtend.setRecipeId(dbRecipeId);
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            recipeExtendDAO.saveOrUpdateRecipeExtend(recipeExtend);
        }

        //记录日志
        RecipeLogService.saveRecipeLog(dbRecipeId, beforeStatus, beforeStatus, "修改处方单");
        return dbRecipeId;
    }

    /**
     * 新版签名服务
     *
     * @param recipeBean  处方
     * @param detailBeanList 详情
     * @return Map<String, Object>
     * @paran consultId  咨询单Id
     */
    @RpcService
    public Map<String, Object> doSignRecipeExt(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList) {
        Map<String, Object> rMap = doSignRecipe(recipeBean, detailBeanList);
        //获取处方签名结果
        Boolean result = Boolean.parseBoolean(rMap.get("signResult").toString());
        if (result) {
            //非可使用省医保的处方立即发送处方卡片，使用省医保的处方需要在药师审核通过后显示
            if (!recipeBean.canMedicalPay()) {
                //发送卡片
                Recipe recipe = ObjectCopyUtils.convert(recipeBean, Recipe.class);
                List<Recipedetail> details = ObjectCopyUtils.convert(detailBeanList, Recipedetail.class);
                RecipeServiceSub.sendRecipeTagToPatient(recipe, details, rMap, false);
                Integer consultId = MapValueUtil.getInteger(rMap, "consultId");
                if(null != consultId) {
                    RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
                    Map<String, Object> attrMap = Maps.newHashMap();
                    attrMap.put("clinicId", consultId);
                    recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), attrMap);
                }
            }
            //高州市人民医院特殊处理
            if (1000423==recipeBean.getClinicOrgan()){
                RecipeBusiThreadPool.submit(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        PatientService patientService = BasicAPI.getService(PatientService.class);
                        PatientDTO patientDTO = patientService.getPatientByMpiId(recipeBean.getMpiid());
                        Date now = DateTime.now().toDate();
                        String str = "";
                        if(patientDTO != null && StringUtils.isNotEmpty(patientDTO.getCertificate())){
                            str = patientDTO.getCertificate().substring(patientDTO.getCertificate().length()-5);
                        }

                        RecipeToHisCallbackService service = ApplicationUtils.getRecipeService(RecipeToHisCallbackService.class);
                        HisSendResTO response = new HisSendResTO();
                        response.setRecipeId(((Integer)rMap.get("recipeId")).toString());
                        List<OrderRepTO> repList = Lists.newArrayList();
                        OrderRepTO orderRepTO = new OrderRepTO();
                        //门诊号处理 年月日+患者身份证后5位 例：2019060407915
                        orderRepTO.setPatientID(DateConversion.getDateFormatter(now,"yyMMdd")+str);
                        orderRepTO.setRegisterID(orderRepTO.getPatientID());
                        //生成处方编号，不需要通过HIS去产生
                        String recipeCodeStr = DigestUtil.md5For16(recipeBean.getClinicOrgan() +
                                recipeBean.getMpiid() + Calendar.getInstance().getTimeInMillis());
                        orderRepTO.setRecipeNo(recipeCodeStr);
                        repList.add(orderRepTO);
                        response.setData(repList);
                        service.sendSuccess(response);
                        return null;
                    }
                });
            }
        }
        LOGGER.info("doSignRecipeExt execute ok! rMap:" + JSONUtils.toString(rMap));
        return rMap;
    }

    /**
     * 处方二次签名
     *
     * @param recipe
     * @return
     */
    @RpcService
    public RecipeResultBean doSecondSignRecipe(RecipeBean recipe) {
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);

        Recipe dbRecipe = RecipeValidateUtil.checkRecipeCommonInfo(recipe.getRecipeId(), resultBean);
        if (null == dbRecipe) {
            LOGGER.error("validateDrugs 平台无该处方对象. recipeId=[{}] error={}", recipe.getRecipeId(), JSONUtils.toString(resultBean));
            return resultBean;
        }

        Integer status = dbRecipe.getStatus();
        if (null == status || status != RecipeStatusConstant.CHECK_NOT_PASS_YS) {
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("该处方不是审核未通过状态");
            return resultBean;
        }

        Integer afterStatus = RecipeStatusConstant.CHECK_PASS_YS;
        if (!dbRecipe.canMedicalPay()) {
            RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
            boolean effective = orderDAO.isEffectiveOrder(dbRecipe.getOrderCode(), dbRecipe.getPayMode());
            if (!effective) {
                resultBean.setCode(RecipeResultBean.FAIL);
                resultBean.setMsg("该处方已失效");
                return resultBean;
            }
        } else {
            afterStatus = RecipeStatusConstant.CHECK_PASS;
        }
        //date 20190929
        //这里提示文案描述，扩展成二次审核通过/二次审核不通过的说明
        recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), afterStatus,
                ImmutableMap.of("supplementaryMemo", recipe.getSupplementaryMemo()));
        afterCheckPassYs(dbRecipe);
        try {
            //生成pdf并签名
            recipeService.generateRecipePdfAndSign(recipe.getRecipeId());
        } catch (Exception e) {
            LOGGER.error("doSecondSignRecipe 签名失败. recipeId=[{}], error={}", recipe.getRecipeId(), e.getMessage());
        }

        LOGGER.info("doSecondSignRecipe execute ok! ");
        return resultBean;
    }

    /**
     * 处方药师审核通过后处理
     *
     * @param recipe
     * @return
     */
    @RpcService
    public RecipeResultBean  afterCheckPassYs(Recipe recipe) {
        if (null == recipe) {
            return null;
        }
        RecipeDetailDAO detailDAO = getDAO(RecipeDetailDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RemoteDrugEnterpriseService service = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);

        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Integer recipeId = recipe.getRecipeId();

        //正常平台处方
        if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(recipe.getFromflag())) {
            /*if(RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeMode)){
                //药师审方前置后，审核通过需要发送卡片消息及处方待处理消息，暂时不发
                RecipeServiceSub.sendRecipeTagToPatient(recipe, detailDAO.findByRecipeId(recipeId), null, true);
                //向患者推送处方消息
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_PASS);
                //同步到互联网监管平台
                SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
                syncExecutorService.uploadRecipeIndicators(recipe);
            } else {*/
                if (recipe.canMedicalPay()) {
                    //如果是可医保支付的单子，审核通过之后是变为待处理状态，需要用户支付完成才发往药企
                    RecipeServiceSub.sendRecipeTagToPatient(recipe, detailDAO.findByRecipeId(recipeId), null, true);
                    //向患者推送处方消息
                    RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_PASS);
                } else {
                    /*if (RecipeBussConstant.PAYMODE_COD.equals(recipe.getPayMode()) || RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode())) {
                        //货到付款|药店取药 审核完成，往药企发送审核完成消息
                        Integer status = OrderStatusConstant.READY_SEND;
                        //到店取药审核完成是带取药状态
                        //data:20190919
                        //添加到院取药和下载处方的状态（这里后置的逻辑）
                        if (RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode())) {
                            status = OrderStatusConstant.READY_GET_DRUG;
                        }
                        orderService.updateOrderInfo(recipe.getOrderCode(), ImmutableMap.of("status", status), resultBean);
                        //发送患者审核完成消息
                        RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_PASS_YS);
                    }*/

                    //date:20190920
                    //审核通过后设置订单的状态（后置）
                    Integer status = OrderStatusConstant.READY_SEND;
                    //到店取药审核完成是带取药状态
                    //data:20190919
                    //添加到院取药和下载处方的状态（这里后置的逻辑）
//                    if (RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode())
//                            || RecipeBussConstant.PAYMODE_TO_HOS.equals(recipe.getPayMode()) ||
//                            RecipeBussConstant.PAYMODE_DOWNLOAD_RECIPE.equals(recipe.getPayMode())) {
//                        status = OrderStatusConstant.READY_GET_DRUG;
//                    }
                    PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
                    status = purchaseService.getOrderStatus(recipe);
                    orderService.updateOrderInfo(recipe.getOrderCode(), ImmutableMap.of("status", status), resultBean);
                    //发送患者审核完成消息
                    RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_PASS_YS);

                    //6.24 货到付款或者药店取药也走药师审核通过推送处方信息
                    // 平台处方发送药企处方信息
                    service.pushSingleRecipeInfo(recipeId);
                }
        } else if (RecipeBussConstant.FROMFLAG_HIS_USE.equals(recipe.getFromflag())) {
            Integer status = OrderStatusConstant.READY_SEND;
            if (RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode())) {
                status = OrderStatusConstant.READY_GET_DRUG;
                // HOS处方发送药企处方信息
                service.pushSingleRecipeInfo(recipeId);
                //发送审核成功消息
                //${sendOrgan}：您的处方已审核通过，请于${expireDate}前到${pharmacyName}取药，地址：${addr}。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
                RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_YS_CHECKPASS_4TFDS, recipe);
            } else if (RecipeBussConstant.PAYMODE_ONLINE.equals(recipe.getPayMode())) {
                // HOS处方发送药企处方信息
                service.pushSingleRecipeInfo(recipeId);
                //发送审核成功消息
                //${sendOrgan}：您的处方已审核通过，我们将以最快的速度配送到：${addr}。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
                RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_YS_CHECKPASS_4STH, recipe);
            } else {
                status = OrderStatusConstant.READY_GET_DRUG;
                // HOS处方发送药企处方信息，由于是自由选择，所以匹配到的药企都发送一遍
                RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
                List<DrugsEnterprise> depList = recipeService.findSupportDepList(Lists.newArrayList(recipeId),
                        recipe.getClinicOrgan(), null, false, null);
                LOGGER.info("afterCheckPassYs recipeId={}, 匹配到药企数量[{}]", recipeId, depList.size());
                for (DrugsEnterprise dep : depList) {
                    service.pushSingleRecipeInfoWithDepId(recipeId, dep.getId());
                }

                //自由选择消息发送
                //${sendOrgan}：您的处方已通过药师审核，请联系开方医生选择取药方式并支付处方费用。如有疑问，请拨打${customerTel}联系小纳。
                RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_YS_CHECKPASS_4FREEDOM, recipe);
            }

            orderService.updateOrderInfo(recipe.getOrderCode(), ImmutableMap.of("status", status), resultBean);
        }

        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "审核通过处理完成");
        return resultBean;
    }

    /**
     * 药师审核不通过后处理
     *
     * @param recipe
     */
    public void afterCheckNotPassYs(Recipe recipe) {
        if (null == recipe) {
            return;
        }
        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
        boolean effective = orderDAO.isEffectiveOrder(recipe.getOrderCode(), recipe.getPayMode());
        //是否是有效订单
        if (!effective) {
            return;
        }
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        //相应订单处理
        orderService.cancelOrderByRecipeId(recipe.getRecipeId(), OrderStatusConstant.CANCEL_NOT_PASS);

        //根据付款方式提示不同消息
        if (RecipeBussConstant.PAYMODE_ONLINE.equals(recipe.getPayMode()) && PayConstant.PAY_FLAG_PAY_SUCCESS == recipe.getPayFlag()) {
            //线上支付
            //微信退款
            wxPayRefundForRecipe(2, recipe.getRecipeId(), null);
            /*if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(recipe.getFromflag())) {
                RecipeMsgService.batchSendMsg(recipe.getRecipeId(), RecipeStatusConstant.CHECK_NOT_PASSYS_PAYONLINE);
            }*/
        } else if (RecipeBussConstant.PAYMODE_COD.equals(recipe.getPayMode()) || RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode())) {
            //货到付款 | 药店取药
           /* if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(recipe.getFromflag())) {
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_NOT_PASSYS_REACHPAY);
            }*/
        }

        /*if (RecipeBussConstant.FROMFLAG_HIS_USE.equals(recipe.getFromflag())) {
            //发送审核不成功消息
            //${sendOrgan}：抱歉，您的处方未通过药师审核。如有收取费用，款项将为您退回，预计1-5个工作日到账。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
            RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_YS_CHECKNOTPASS_4HIS, recipe);
        }*/
    }

    /**
     * 医院处方审核 (当前为自动审核通过)
     *
     * @param recipeId
     * @return HashMap<String,Object>
     */
    @RpcService
    @Deprecated
    public HashMap<String, Object> recipeAutoCheck(Integer recipeId) {
        LOGGER.info("recipeAutoCheck get in recipeId=" + recipeId);
        HashMap<String, Object> map = Maps.newHashMap();
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        Integer recipeStatus = recipe.getStatus();
        if (RecipeStatusConstant.UNCHECK == recipeStatus) {
            int afterStatus = RecipeStatusConstant.CHECK_PASS;
            Map<String, Object> attrMap = Maps.newHashMap();
            attrMap.put("checkDate", DateTime.now().toDate());
            attrMap.put("checkOrgan", recipe.getClinicOrgan());
            attrMap.put("checker", 0);
            attrMap.put("checkFailMemo", "");
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, afterStatus, attrMap);

            RecipeLogService.saveRecipeLog(recipeId, recipeStatus, afterStatus, "自动审核通过");

            map.put("code", RecipeSystemConstant.SUCCESS);
            map.put("msg", "");

            recipe.setStatus(afterStatus);
            //审核失败的话需要发送信息
//            RecipeMsgService.batchSendMsg(recipe,RecipeStatusConstant.CHECK_NOT_PASS);
        } else {
            map.put("code", RecipeSystemConstant.FAIL);
            map.put("msg", "处方单不是待审核状态，不能进行自动审核");
        }

        // 医院审核系统的对接

        return map;
    }

    /**
     * 处方单详情服务
     *
     * @param recipeId 处方ID
     * @return HashMap<String, Object>
     */
    @RpcService
    public Map<String, Object> findRecipeAndDetailById(int recipeId) {
        //bug#30596医生患者电子病历下方处方单，点击非本医生开具的处方单，打开页面显示错误----去掉越权
        /*checkUserHasPermission(recipeId);*/
        return RecipeServiceSub.getRecipeAndDetailByIdImpl(recipeId, true);
    }

    /**
     * 获取智能审方结果详情
     *
     * @param recipeId 处方ID
     * @return
     */
    @RpcService
    public List<AuditMedicinesDTO> getAuditMedicineIssuesByRecipeId(int recipeId){
        return RecipeServiceSub.getAuditMedicineIssuesByRecipeId(recipeId);
    }
    
    /**
     * 处方撤销方法(供医生端使用)
     *
     * @param recipeId 处方Id
     * @return Map<String, Object>
     * 撤销成功返回 {"result":true,"msg":"处方撤销成功"}
     * 撤销失败返回 {"result":false,"msg":"失败原因"}
     */
    @RpcService
    public Map<String, Object> cancelRecipe(Integer recipeId) {
        return RecipeServiceSub.cancelRecipeImpl(recipeId, 0, "", "");
    }

    /**
     * 处方撤销方法(供运营平台使用)
     *
     * @param recipeId
     * @param name     操作人员姓名
     * @param message  处方撤销原因
     * @return
     */
    @RpcService
    public Map<String, Object> cancelRecipeForOperator(Integer recipeId, String name, String message) {
        return RecipeServiceSub.cancelRecipeImpl(recipeId, 1, name, message);
    }


    /**
     * 定时任务:同步HIS医院药品信息
     */
    @RpcService
    public void drugInfoSynTask() {
        drugInfoSynTaskExt(null);
    }

    @RpcService
    public void drugInfoSynTaskExt(Integer organId) {
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        IOrganConfigService iOrganConfigService = ApplicationUtils.getBaseService(IOrganConfigService.class);

        List<Integer> organIds = new ArrayList<>();
        if (null == organId) {
            //查询 base_organconfig 表配置需要同步的机构
            organIds = iOrganConfigService.findEnableDrugSync();
        } else {
            organIds.add(organId);
        }

        List<String> unuseDrugs = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(organIds)) {
            for (int oid : organIds) {
                //查询起始下标
                int startIndex = 0;
                boolean finishFlag = true;
                do {
                    unuseDrugs.clear();
                    List<DrugInfoTO> drugInfoList = hisService.getDrugInfoFromHis(oid, false, startIndex);
                    if (null != drugInfoList) {
                        //是否有效标志 1-有效 0-无效
                        for (DrugInfoTO drug : drugInfoList) {
                            if ("0".equals(drug.getUseflag())) {
                                unuseDrugs.add(drug.getDrcode());
                            }
                        }

//                        if (CollectionUtils.isNotEmpty(unuseDrugs)) {
                        //暂时不启用自动修改
//                            organDrugListDAO.updateStatusByOrganDrugCode(unuseDrugs, 0);
//                        }
                        startIndex += 100;
                        LOGGER.info("drugInfoSynTask organId=[{}] 同步完成. 关闭药品数量[{}], drugCode={}", oid, unuseDrugs.size(), JSONUtils.toString(unuseDrugs));
                    } else {
                        LOGGER.error("drugInfoSynTask organId=[{}] total=[{}] 药品信息更新结束.", startIndex, oid);
                        finishFlag = false;
                    }
                } while (finishFlag);
            }
        } else {
            LOGGER.info("drugInfoSynTask organIds is empty.");
        }
    }

    /**
     * 定时任务:定时取消处方单
     */
    @RpcService
    public void cancelRecipeTask() {
        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

        List<Integer> statusList = Arrays.asList(RecipeStatusConstant.NO_PAY, RecipeStatusConstant.NO_OPERATOR);
        StringBuilder memo = new StringBuilder();
        RecipeOrder order;
        //设置查询时间段
        String endDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(
                Integer.parseInt(cacheService.getParam(ParameterConstant.KEY_RECIPE_VALIDDATE_DAYS, RECIPE_EXPIRED_DAYS.toString()))
        ), DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(
                Integer.parseInt(cacheService.getParam(ParameterConstant.KEY_RECIPE_CANCEL_DAYS, RECIPE_EXPIRED_SEARCH_DAYS.toString()))), DateConversion.DEFAULT_DATE_TIME);

        //增加订单未取药推送
        List<String> orderCodes = recipeOrderDAO.getRecipeIdForCancelRecipeOrder(startDt, endDt);
        if (CollectionUtils.isNotEmpty(orderCodes)) {
            List<Recipe> recipes = recipeDAO.getRecipeListByOrderCodes(orderCodes);
            LOGGER.info("cancelRecipeOrderTask , 取消数量=[{}], 详情={}", recipes.size(), JSONUtils.toString(recipes));
            for (Recipe recipe : recipes) {
                memo.delete(0, memo.length());
                int recipeId = recipe.getRecipeId();
                //相应订单处理
                order = orderDAO.getOrderByRecipeId(recipeId);
                orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO);
                //变更处方状态
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.NO_OPERATOR, ImmutableMap.of("chooseFlag", 1));
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.RECIPE_ORDER_CACEL);
                memo.append("已取消,超过3天未操作");
                //HIS消息发送
                boolean succFlag = hisService.recipeStatusUpdate(recipeId);
                if (succFlag) {
                    memo.append(",HIS推送成功");
                } else {
                    memo.append(",HIS推送失败");
                }
                //保存处方状态变更日志
                RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS, RecipeStatusConstant.NO_OPERATOR, memo.toString());
            }
        }
        for (Integer status : statusList) {
            List<Recipe> recipeList = recipeDAO.getRecipeListForCancelRecipe(status, startDt, endDt);
            LOGGER.info("cancelRecipeTask 状态=[{}], 取消数量=[{}], 详情={}", status, recipeList.size(), JSONUtils.toString(recipeList));
            if (CollectionUtils.isNotEmpty(recipeList)) {
                for (Recipe recipe : recipeList) {
                    if(RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
                        OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
                        List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
                        for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                            if ("aldyf".equals(drugsEnterprise.getCallSys()) || ("tmdyf".equals(drugsEnterprise.getCallSys()) && recipe.getPushFlag() == 1)) {
                                //向药企推送处方过期的通知
                                RemoteDrugEnterpriseService remoteDrugEnterpriseService =
                                    ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
                                try {
                                    AccessDrugEnterpriseService remoteService = remoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
                                    DrugEnterpriseResult drugEnterpriseResult = remoteService.updatePrescriptionStatus(recipe.getRecipeCode(), AlDyfRecipeStatusConstant.EXPIRE);
                                    LOGGER.info("向药企推送处方过期通知,{}", JSONUtils.toString(drugEnterpriseResult));
                                } catch (Exception e) {
                                    LOGGER.info("向药企推送处方过期通知有问题{}", recipe.getRecipeId(), e);
                                }
//                                try {
//                                    //向阿里大药房推送处方过期的通知
//                                    AldyfRemoteService aldyfRemoteService = ApplicationUtils.getRecipeService(AldyfRemoteService.class);
//                                    DrugEnterpriseResult drugEnterpriseResult = aldyfRemoteService.updatePrescriptionStatus(recipe.getRecipeCode(), AlDyfRecipeStatusConstant.EXPIRE);
//                                    LOGGER.info("向阿里大药房推送处方过期通知,{}", JSONUtils.toString(drugEnterpriseResult));
//                                } catch (Exception e) {
//                                    LOGGER.info("向阿里大药房推送处方过期通知有问题{}", recipe.getRecipeId(), e);
//                                }
                            }


                        }
                    }
                    memo.delete(0, memo.length());
                    int recipeId = recipe.getRecipeId();
                    //相应订单处理
                    order = orderDAO.getOrderByRecipeId(recipeId);
                    orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO);
                    if (recipe.getFromflag().equals(RecipeBussConstant.FROMFLAG_HIS_USE)){
                        orderDAO.updateByOrdeCode(order.getOrderCode(),ImmutableMap.of("cancelReason", "患者未在规定时间内支付，该处方单已失效"));
                        //发送超时取消消息
                        //${sendOrgan}：抱歉，您的处方单由于超过${overtime}未处理，处方单已失效。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
                        RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_CANCEL_4HIS, recipe);
                    }

                    //变更处方状态
                    recipeDAO.updateRecipeInfoByRecipeId(recipeId, status, ImmutableMap.of("chooseFlag", 1));
                    RecipeMsgService.batchSendMsg(recipe, status);
                    if (RecipeStatusConstant.NO_PAY == status) {
                        memo.append("已取消,超过3天未支付");
                    } else if (RecipeStatusConstant.NO_OPERATOR == status) {
                        memo.append("已取消,超过3天未操作");
                    } else {
                        memo.append("未知状态:" + status);
                    }
                    //推送处方到监管平台(江苏)
                    RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(recipe.getRecipeId(),1));
                    //HIS消息发送
                    boolean succFlag = hisService.recipeStatusUpdate(recipeId);
                    if (succFlag) {
                        memo.append(",HIS推送成功");
                    } else {
                        memo.append(",HIS推送失败");
                    }
                    //保存处方状态变更日志
                    RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS, status, memo.toString());
                }
            }
        }

    }

    /**
     * 定时任务:处方单失效提醒
     * 根据处方单失效时间：
     * 如果医生签名确认时间是：9：00-24：00  ，在处方单失效前一天的晚上6点推送；
     * 如果医生签名确认时间是：00-8：59 ，在处方单失效前两天的晚上6点推送；
     */
    @RpcService
    public void remindRecipeTask() {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);

        //处方失效前一天提醒，但是需要根据签名时间进行推送，故查数据时选择超过一天的数据就可以
        List<Integer> statusList = Arrays.asList(RecipeStatusConstant.PATIENT_NO_OPERATOR, RecipeStatusConstant.PATIENT_NO_PAY,
                RecipeStatusConstant.PATIENT_NODRUG_REMIND);
        Date now = DateTime.now().toDate();
        //设置查询时间段
        String endDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(1), DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(
                Integer.parseInt(cacheService.getParam(ParameterConstant.KEY_RECIPE_VALIDDATE_DAYS, RECIPE_EXPIRED_DAYS.toString()))
        ), DateConversion.DEFAULT_DATE_TIME);
        for (Integer status : statusList) {
            List<Recipe> recipeList = recipeDAO.getRecipeListForRemind(status, startDt, endDt);
            //筛选数据
            List<Integer> recipeIds = new ArrayList<>(10);
            for (Recipe recipe : recipeList) {
                Date signDate = recipe.getSignDate();
                if (null != signDate) {
                    int hour = DateConversion.getHour(signDate);
                    //签名时间在 00-8：59，则进行提醒
                    if (hour >= 0 && hour < 9) {
                        recipeIds.add(recipe.getRecipeId());
                    } else {
                        //如果是在9-24开的药，则判断签名时间与当前时间在2天后
                        int days = DateConversion.getDaysBetween(signDate, now);
                        if (days >= 2) {
                            recipeIds.add(recipe.getRecipeId());
                        }
                    }
                }
            }

            LOGGER.info("remindRecipeTask 状态=[{}], 提醒数量=[{}], 详情={}", status, recipeIds.size(), JSONUtils.toString(recipeIds));
            if (CollectionUtils.isNotEmpty(recipeIds)) {
                //批量更新 处方失效前提醒标志位
                recipeDAO.updateRemindFlagByRecipeId(recipeIds);
                //批量信息推送（失效前的消息提示取消）
                //RecipeMsgService.batchSendMsg(recipeIds, status);
            }
        }
    }

    /**
     * 定时任务: 查询过期的药师审核不通过，需要医生二次确认的处方
     * 查询规则: 药师审核不通过时间点的 2天前-1月前这段时间内，医生未处理的处方单
     */
    @RpcService
    public void afterCheckNotPassYsTask() {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);

        String endDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(2), DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(RECIPE_EXPIRED_SECTION), DateConversion.DEFAULT_DATE_TIME);
        //根据条件查询出来的数据都是需要主动退款的
        List<Recipe> list = recipeDAO.findCheckNotPassNeedDealList(startDt, endDt);
        LOGGER.info("afterCheckNotPassYsTask 处理数量=[{}], 详情={}", list.size(), JSONUtils.toString(list));
        for (Recipe recipe : list) {
            afterCheckNotPassYs(recipe);
        }
    }

    /**
     * 定时任务:从HIS中获取处方单状态
     * 选择了到医院取药方法，需要定时从HIS上获取该处方状态数据
     */
    @RpcService
    public void getRecipeStatusFromHis() {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);

        //设置查询时间段
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(
                Integer.parseInt(cacheService.getParam(ParameterConstant.KEY_RECIPE_VALIDDATE_DAYS, RECIPE_EXPIRED_DAYS.toString()))
        ), DateConversion.DEFAULT_DATE_TIME);
        String endDt = DateConversion.getDateFormatter(DateTime.now().toDate(), DateConversion.DEFAULT_DATE_TIME);
        //key为organId,value为recipdeCode集合
        Map<Integer, List<String>> map = Maps.newHashMap();
        List<Recipe> list = recipeDAO.getRecipeStatusFromHis(startDt, endDt);
        LOGGER.info("getRecipeStatusFromHis 需要同步HIS处方，数量=[{}]", (null == list) ? 0 : list.size());

        assembleQueryStatusFromHis(list, map);
        List<UpdateRecipeStatusFromHisCallable> callables = new ArrayList<>(0);
        for (Integer organId : map.keySet()) {
            callables.add(new UpdateRecipeStatusFromHisCallable(map.get(organId), organId));
        }

        if (CollectionUtils.isNotEmpty(callables)) {
            try {
                RecipeBusiThreadPool.submitList(callables);
            } catch (InterruptedException e) {
                LOGGER.error("getRecipeStatusFromHis 线程池异常");
            }
        }
    }

    /**
     * 定时任务:更新药企token
     */
    @RpcService
    public void updateDrugsEnterpriseToken() {
        DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
        List<Integer> list = drugsEnterpriseDAO.findNeedUpdateIds();
        LOGGER.info("updateDrugsEnterpriseToken 此次更新药企数量=[{}]", (null == list) ? 0 : list.size());
        RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        //非空已在方法内部判断
        remoteDrugService.updateAccessToken(list);
    }

    /**
     * 定时任务向患者推送确认收货微信消息
     */
    @RpcService
    public void pushPatientConfirmReceiptTask() {
        // 设置查询时间段
        String endDt =
                DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(3),
                        DateConversion.DEFAULT_DATE_TIME);
        String startDt =
                DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(
                        RECIPE_EXPIRED_SECTION), DateConversion.DEFAULT_DATE_TIME);

        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        List<Recipe> recipes = recipeDAO.findNotConfirmReceiptList(startDt, endDt);

        LOGGER.info("pushPatientConfirmReceiptTask size={}, detail={}", recipes.size(), JSONUtils.toString(recipes));
        // 批量信息推送
        RecipeMsgService.batchSendMsgForNew(recipes, RecipeStatusConstant.RECIPR_NOT_CONFIRM_RECEIPT);
    }

    /************************************************患者类接口 START*************************************************/

    /**
     * 健康端获取处方详情
     *
     * @param recipeId 处方ID
     * @return HashMap<String, Object>
     */
    @RpcService
    public Map<String, Object> getPatientRecipeById(int recipeId) {
        checkUserHasPermission(recipeId);
        Map<String, Object> result = RecipeServiceSub.getRecipeAndDetailByIdImpl(recipeId, false);
        PatientDTO patient = (PatientDTO) result.get("patient");
        result.put("patient", ObjectCopyUtils.convert(patient, PatientDS.class));
        return result;
    }


    /**
     * 获取该处方的购药方式(用于判断这个处方是不是被处理)
     *
     * @param recipeId
     * @param flag     1:表示处方单详情页从到院取药转直接支付的情况判断
     * @return 0未处理  1线上支付 2货到付款 3到院支付
     */


    @RpcService
    public int getRecipePayMode(int recipeId, int flag) {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe dbRecipe = recipeDAO.getByRecipeId(recipeId);
        if (null == dbRecipe) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipe not exist!");
        }

        //进行判断该处方单是否已处理，已处理则返回具体购药方式
        if (1 == dbRecipe.getChooseFlag()) {
            //如果之前选择的是到院取药且未支付 则可以进行转在线支付的方式
            if (1 == flag && RecipeBussConstant.GIVEMODE_TO_HOS.equals(dbRecipe.getGiveMode()) && RecipeBussConstant.GIVEMODE_TFDS.equals(dbRecipe.getPayMode())
                    && 0 == dbRecipe.getPayFlag()) {
                return 0;
            }
            return dbRecipe.getPayMode();
        } else {
            return 0;
        }

    }

    /**
     * 判断该处方是否支持医院取药
     *
     * @param clinicOrgan 开药机构
     * @return boolean
     */
    @Deprecated
    @RpcService
    public boolean supportTakeMedicine(Integer recipeId, Integer clinicOrgan) {
        if (null == recipeId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipeId is required!");
        }

        if (null == clinicOrgan) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "clinicOrgan is required!");
        }
        boolean succFlag = false;
        boolean flag = organService.getTakeMedicineFlagById(clinicOrgan);
        //是否支持医院取药 true：支持
        if (flag) {
            String backInfo = searchRecipeStatusFromHis(recipeId, 1);
            if (StringUtils.isNotEmpty(backInfo)) {
                succFlag = false;
                throw new DAOException(ErrorCode.SERVICE_ERROR, backInfo);
            }
        } else {
            LOGGER.error("supportTakeMedicine organ[" + clinicOrgan + "] not support take medicine!");
        }

        return succFlag;
    }

    /**
     * 扩展配送校验方法
     *
     * @param recipeId
     * @param clinicOrgan
     * @param selectDepId 可能之前选定了某个药企
     * @param payMode
     * @return
     */
    public Integer supportDistributionExt(Integer recipeId, Integer clinicOrgan, Integer selectDepId, Integer payMode) {
        Integer backDepId = null;
        boolean flag = organService.getTakeMedicineFlagById(clinicOrgan);
        //是否支持医院取药 true：支持
        //该医院不对接HIS的话，则不需要进行该校验
        if (flag) {
            String backInfo = searchRecipeStatusFromHis(recipeId, 2);
            if (StringUtils.isNotEmpty(backInfo)) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, backInfo);
            }
        }

        //进行药企配送分配，检测药企是否有能力进行该处方的配送
        Integer depId = getDrugsEpsIdByOrganId(recipeId, payMode, selectDepId);
        if (!Integer.valueOf(-1).equals(depId)) {
            if (!(null != selectDepId && !selectDepId.equals(depId))) {
                //不是同一家药企配送，无法配送
                backDepId = depId;
            }
        }

        return backDepId;
    }

    private String getWxAppIdForRecipeFromOps(Integer recipeId, Integer busOrgan) {
        IPaymentService iPaymentService = ApplicationUtils.getBaseService(IPaymentService.class);
        //参数二 PayWayEnum.WEIXIN_WAP
        //参数三 BusTypeEnum.RECIPE
        return iPaymentService.getPayAppId(busOrgan, "40", BusTypeEnum.RECIPE.getCode());
    }

    /**
     * 根据开方机构分配药企进行配送并入库 （获取某一购药方式最合适的供应商）
     *
     * @param recipeId
     * @param payMode
     * @param selectDepId
     * @return
     */
    public Integer getDrugsEpsIdByOrganId(Integer recipeId, Integer payMode, Integer selectDepId) {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        Integer depId = -1;
        if (null != recipe) {
            List<DrugsEnterprise> list = findSupportDepList(Arrays.asList(recipeId), recipe.getClinicOrgan(),
                    payMode, true, selectDepId);
            if (CollectionUtils.isNotEmpty(list)) {
                depId = list.get(0).getId();
            }
        } else {
            LOGGER.error("getDrugsEpsIdByOrganId 处方[" + recipeId + "]不存在！");
        }

        return depId;
    }

    /**
     * 查询符合条件的药企供应商
     *
     * @param recipeIdList 处方ID
     * @param organId      开方机构
     * @param payMode      购药方式，为NULL时表示查询所有药企
     * @param sigle        true:表示只返回第一个合适的药企，false:表示符合条件的所有药企
     * @param selectDepId  指定某个药企
     * @return
     */
    public List<DrugsEnterprise> findSupportDepList(List<Integer> recipeIdList, int organId, Integer payMode, boolean sigle,
                                                    Integer selectDepId) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
        SaleDrugListDAO saleDrugListDAO = getDAO(SaleDrugListDAO.class);
        RecipeDetailDAO recipeDetailDAO = getDAO(RecipeDetailDAO.class);
        RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        IHisConfigService iHisConfigService = ApplicationUtils.getBaseService(IHisConfigService.class);

        List<DrugsEnterprise> backList = new ArrayList<>(5);
        //线上支付能力判断
        boolean onlinePay = true;
        if (null == payMode || RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)
                || RecipeBussConstant.PAYMODE_MEDICAL_INSURANCE.equals(payMode)) {
            //只支持线上付款后配送，则需要判断医院是否有付款帐号
            //不需要再判断支付帐号的问题
            /*String wxAccount = null;
            try {
                wxAccount = getWxAppIdForRecipeFromOps(null, organId);
            } catch (Exception e) {
                LOGGER.warn("findSupportDepList getWxAppIdForRecipeFromOps error. organId={}", organId, e);
                wxAccount = null;
            }*/
            //需要判断医院HIS是否开通
            boolean hisStatus = iHisConfigService.isHisEnable(organId);
            LOGGER.info("findSupportDepList payAccount={}, hisStatus={}", null, hisStatus);
            if (!isCanSupportPayOnline(null, hisStatus)) {
                LOGGER.error("findSupportDepList 机构[" + organId + "]不支持线上支付！");
                //这里判断payMode=null的情况，是为了筛选供应商提供依据
                if (null == payMode) {
                    onlinePay = false;
                } else {
                    return backList;
                }
            }
        }

        for (Integer recipeId : recipeIdList) {
            List<DrugsEnterprise> subDepList = new ArrayList<>(5);
            //检测配送的药品是否按照完整的包装开的药，如 1*20支，开了10支，则不进行选择，数据库里主要是useTotalDose不为小数
            List<Double> totalDoses = recipeDetailDAO.findUseTotalDoseByRecipeId(recipeId);
            if (null != totalDoses && !totalDoses.isEmpty()) {
                for (Double totalDose : totalDoses) {
                    if (null != totalDose) {
                        int itotalDose = (int) totalDose.doubleValue();
                        if (itotalDose != totalDose.doubleValue()) {
                            LOGGER.error("findSupportDepList 不支持非完整包装的计量药品配送. recipeId=[{}], totalDose=[{}]", recipeId, totalDose);
                            break;
                        }
                    } else {
                        LOGGER.error("findSupportDepList 药品计量为null. recipeId=[{}]", recipeId);
                        break;
                    }
                }
            } else {
                LOGGER.error("findSupportDepList 所有药品计量为null. recipeId=[{}]", recipeId);
                break;
            }

            List<Integer> drugIds = recipeDetailDAO.findDrugIdByRecipeId(recipeId);
            if (CollectionUtils.isEmpty(drugIds)) {
                LOGGER.error("findSupportDepList 处方[{}]没有任何药品！", recipeId);
                break;
            }

            List<DrugsEnterprise> drugsEnterpriseList = new ArrayList<>(0);
            if (null != selectDepId) {
                DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(selectDepId);
                if (null != drugsEnterprise) {
                    drugsEnterpriseList.add(drugsEnterprise);
                }
            } else {
                if (null != payMode) {
                    List<Integer> payModeSupport = RecipeServiceSub.getDepSupportMode(payMode);
                    if (CollectionUtils.isEmpty(payModeSupport)) {
                        LOGGER.error("findSupportDepList 处方[{}]无法匹配配送方式. payMode=[{}]", recipeId, payMode);
                        break;
                    }

                    //筛选出来的数据已经去掉不支持任何方式配送的药企
                    drugsEnterpriseList = drugsEnterpriseDAO.findByOrganIdAndPayModeSupport(organId, payModeSupport);
                    if (CollectionUtils.isEmpty(drugsEnterpriseList)) {
                        LOGGER.error("findSupportDepList 处方[{}]没有任何药企可以进行配送！", recipeId);
                        break;
                    }
                } else {
                    drugsEnterpriseList = drugsEnterpriseDAO.findByOrganId(organId);
                }
            }

            for (DrugsEnterprise dep : drugsEnterpriseList) {
                //根据药企是否能满足所有配送的药品优先
                Integer depId = dep.getId();
                //不支持在线支付跳过该药企
                if (Integer.valueOf(1).equals(dep.getPayModeSupport()) && !onlinePay) {
                    continue;
                }
                //药品匹配成功标识
                boolean succFlag = false;
                Long count = saleDrugListDAO.getCountByOrganIdAndDrugIds(depId, drugIds);
                if (null != count && count > 0) {
                    if (count == drugIds.size()) {
                        succFlag = true;
                    }
                }

                if (!succFlag) {
                    LOGGER.error("findSupportDepList 存在不支持配送药品. 处方ID=[{}], 药企ID=[{}], 药企名称=[{}], drugIds={}",
                            recipeId, depId, dep.getName(), JSONUtils.toString(drugIds));
                    continue;
                } else {
                    //通过查询该药企库存，最终确定能否配送
                    succFlag = remoteDrugService.scanStock(recipeId, dep);
                    if (succFlag) {
                        subDepList.add(dep);
                        //只需要查询单供应商就返回
                        if (sigle) {
                            break;
                        }
                    } else {
                        LOGGER.error("findSupportDepList 药企库存查询返回药品无库存. 处方ID=[{}], 药企ID=[{}], 药企名称=[{}]",
                                recipeId, depId, dep.getName());
                    }
                }
            }

            if (CollectionUtils.isEmpty(subDepList)) {
                LOGGER.error("findSupportDepList 该处方无法配送. recipeId=[{}]", recipeId);
                backList.clear();
                break;
            } else {
                //药企求一个交集
                if (CollectionUtils.isEmpty(backList)) {
                    backList.addAll(subDepList);
                } else {
                    //交集需要处理
                    backList.retainAll(subDepList);
                }
            }
        }

        return backList;
    }

    /**
     * 手动进行处方退款服务
     *
     * @param recipeId
     * @param operName
     * @param reason
     */
    @RpcService
    public void manualRefundForRecipe(int recipeId, String operName, String reason) {
        wxPayRefundForRecipe(4, recipeId, "操作人:[" + ((StringUtils.isEmpty(operName)) ? "" : operName) + "],理由:[" +
                ((StringUtils.isEmpty(reason)) ? "" : reason) + "]");
    }

    /**
     * 退款方法
     *
     * @param flag
     * @param recipeId
     */
    @RpcService
    public void wxPayRefundForRecipe(int flag, int recipeId, String log) {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);

        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        int status = recipe.getStatus();

        String errorInfo = "退款-";
        switch (flag) {
            case 1:
                errorInfo += "HIS线上支付返回：写入his失败";
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.PATIENT_HIS_FAIL);
                break;
            case 2:
                errorInfo += "药师审核不通过";
                break;
            case 3:
                errorInfo += "推送药企失败";
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.RECIPE_LOW_STOCKS);
                break;
            case 4:
                errorInfo += log;
                status = RecipeStatusConstant.REVOKE;
                break;
            default:
                errorInfo += "未知,flag=" + flag;

        }

        RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), status, errorInfo);

        //相应订单处理
        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
        RecipeOrder order = orderDAO.getByOrderCode(recipe.getOrderCode());
        if (1 == flag) {
            orderService.updateOrderInfo(order.getOrderCode(), ImmutableMap.of("status", OrderStatusConstant.READY_PAY), null);
        } else if (PUSH_FAIL == flag) {
            orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO);
        } else if (REFUND_MANUALLY == flag) {
            orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO);
            //处理处方单
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, status, null);
        }

        try {
            //退款
            INgariRefundService rufundService = BaseAPI.getService(INgariRefundService.class);
            rufundService.refund(order.getOrderId(), RecipeService.WX_RECIPE_BUSTYPE);
        } catch (Exception e) {
            LOGGER.error("wxPayRefundForRecipe " + errorInfo + "*****微信退款异常！recipeId[" + recipeId + "],err[" + e.getMessage() + "]");
        }

        if (CHECK_NOT_PASS == flag || PUSH_FAIL == flag || REFUND_MANUALLY == flag) {
            //HIS消息发送
            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
            hisService.recipeRefund(recipeId);
        }

    }

    /**
     * 更新机构相应药品价格
     *
     * @param organId  机构ID
     * @param priceMap 药品价格，key:药品id，value:价格
     */
    public void updateDrugPrice(Integer organId, Map<Integer, BigDecimal> priceMap) {
        if (null != organId && null != priceMap && !priceMap.isEmpty()) {
            OrganDrugListDAO organDrugListDAO = getDAO(OrganDrugListDAO.class);

            for (Map.Entry<Integer, BigDecimal> entry : priceMap.entrySet()) {
                if (null != entry.getKey() && null != entry.getValue()) {
                    organDrugListDAO.updateDrugPrice(organId, entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /************************************************患者类接口 END***************************************************/


    /**
     * 组装从HIS获取处方状态的map，key为organId,value为HIS端处方编号 recipeCode集合
     *
     * @param list
     * @param map
     */
    private void assembleQueryStatusFromHis(List<Recipe> list, Map<Integer, List<String>> map) {
        if (CollectionUtils.isNotEmpty(list)) {
            for (Recipe recipe : list) {
                //到院取药的去查询HIS状态
                if (RecipeStatusConstant.HAVE_PAY == recipe.getStatus() || RecipeStatusConstant.CHECK_PASS == recipe.getStatus()) {
                    if (!map.containsKey(recipe.getClinicOrgan())) {
                        map.put(recipe.getClinicOrgan(), new ArrayList<String>(0));
                    }

                    if (StringUtils.isNotEmpty(recipe.getRecipeCode())) {
                        map.get(recipe.getClinicOrgan()).add(recipe.getRecipeCode());
                    }
                }
            }
        }
    }

    /**
     * 获取当前患者所有家庭成员(包括自己)
     *
     * @param mpiId
     * @return
     */
    public List<String> getAllMemberPatientsByCurrentPatient(String mpiId) {
        //获取所有家庭成员的患者编号
        List<String> allMpiIds = iPatientService.findMemberMpiByMpiid(mpiId);
        if (null == allMpiIds) {
            allMpiIds = new ArrayList<>(0);
        }
        //加入患者自己的编号
        allMpiIds.add(mpiId);
        return allMpiIds;
    }

    /**
     * 在线续方首页，获取当前登录患者待处理处方单
     *
     * @param mpiid 当前登录患者mpiid
     * @return
     */
    @RpcService
    public RecipeResultBean getHomePageTaskForPatient(String mpiid) {
        LOGGER.info("getHomePageTaskForPatient mpiId={}", mpiid);
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        //根据mpiid获取当前患者所有家庭成员(包括自己)
        List<String> allMpiIds = getAllMemberPatientsByCurrentPatient(mpiid);
        //获取患者待处理处方单id
        List<Integer> recipeIds = recipeDAO.findPendingRecipes(allMpiIds, RecipeStatusConstant.CHECK_PASS, 0, Integer.MAX_VALUE);
        //获取患者历史处方单，有一个即不为空
        List<PatientRecipeBean> backList = recipeDAO.findOtherRecipesForPatient(allMpiIds, recipeIds, 0, 1);
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();

        if (CollectionUtils.isEmpty(recipeIds)) {
            if (CollectionUtils.isEmpty(backList)) {
                resultBean.setExtendValue("-1");
                resultBean.setMsg("查看我的处方单");
            } else {
                resultBean.setExtendValue("0");
                resultBean.setMsg("查看我的处方单");
            }
        } else {
            resultBean.setExtendValue("1");
            resultBean.setMsg(String.valueOf(recipeIds.size()));
        }
        return resultBean;
    }

    /**
     * 处方订单下单时和下单之后对处方单的更新
     *
     * @param saveFlag
     * @param recipeId
     * @param payFlag
     * @param info
     * @return
     */
    public RecipeResultBean updateRecipePayResultImplForOrder(boolean saveFlag, Integer recipeId, Integer payFlag,
                                                              Map<String, Object> info) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        if (null == recipeId) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("处方单id为null");
            return result;
        }

        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);

        Map<String, Object> attrMap = Maps.newHashMap();
        if (null != info) {
            attrMap.putAll(info);
        }
        Integer payMode = MapValueUtil.getInteger(attrMap, "payMode");
        Integer giveMode = null;
        if (RecipeBussConstant.PAYMODE_TFDS.equals(payMode)) {
            //到店取药
            giveMode = RecipeBussConstant.GIVEMODE_TFDS;
        } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode) || RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)
                || RecipeBussConstant.PAYMODE_MEDICAL_INSURANCE.equals(payMode)) {
            //配送到家
            giveMode = RecipeBussConstant.GIVEMODE_SEND_TO_HOME;
        } else if (RecipeBussConstant.PAYMODE_TO_HOS.equals(payMode)) {
            //到院取药
            giveMode = RecipeBussConstant.GIVEMODE_TO_HOS;
        } else if(RecipeBussConstant.PAYMODE_DOWNLOAD_RECIPE.equals(payMode)){
            giveMode = RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE;
        } else {
            giveMode = null;
        }
        attrMap.put("giveMode", giveMode);
        Recipe dbRecipe = recipeDAO.getByRecipeId(recipeId);
        //根据审方模式改变
        auditModeContext.getAuditModes(dbRecipe.getReviewType()).afterPayChange(saveFlag, dbRecipe, result,attrMap);

        if (saveFlag && RecipeResultBean.SUCCESS.equals(result.getCode())) {
            if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(dbRecipe.getFromflag())
                    || RecipeBussConstant.FROMFLAG_HIS_USE.equals(dbRecipe.getFromflag())) {
                //HIS消息发送
                RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
                //HIS调用失败不应该导致业务失败
                hisService.recipeDrugTake(recipeId, payFlag, null);
            }
        }
        return result;
    }

    /**
     * 判断是否可以线上支付
     *
     * @param acount 微信帐号
     * @param hisStatus true:his启用
     * @return
     */
    private boolean isCanSupportPayOnline(String acount, boolean hisStatus) {
        if (hisStatus) {
            return true;
        }
        return false;
    }

    /**
     * 查询单个处方在HIS中的状态
     *
     * @param recipeId
     * @param modelFlag
     * @return
     */
    public String searchRecipeStatusFromHis(Integer recipeId, int modelFlag) {
        LOGGER.info("searchRecipeStatusFromHis " + ((1 == modelFlag) ? "supportTakeMedicine" : "supportDistribution") + "  recipeId=" + recipeId);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        //HIS发送消息
        return hisService.recipeSingleQuery(recipeId);
    }

    /**
     * 患者mpiId变更后修改处方数据内容
     *
     * @param newMpiId
     * @param oldMpiId
     */
    public void updatePatientInfoForRecipe(String newMpiId, String oldMpiId) {
        if (StringUtils.isNotEmpty(newMpiId) && StringUtils.isNotEmpty(oldMpiId)) {
            RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
            Integer count = recipeDAO.updatePatientInfoForRecipe(newMpiId, oldMpiId);
            LOGGER.info("updatePatientInfoForRecipe newMpiId=[{}], oldMpiId=[{}], count=[{}]", newMpiId, oldMpiId, count);
        }
    }

    @RpcService
    public Map<String,Object> getHosRecipeList(Integer consultId,Integer organId,String mpiId) {
        RecipePreserveService preserveService = ApplicationUtils.getRecipeService(RecipePreserveService.class);
        return preserveService.getHosRecipeList(consultId,organId,mpiId);
    }

    /*@RpcService
    public QueryRecipeResponseTO getHosRecipeListInfoByMpiId(Integer organId,String mpiId){
        LOGGER.info("getHosRecipeListInfoByMpiId  organId={},mpiId={}", organId,mpiId);
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        PatientDTO patientDTO = patientService.get(mpiId);
        if (patientDTO == null){
            LOGGER.error("找不到该患者");
            return null;
        }
        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        OrganDTO organDTO = organService.getByOrganId(organId);
        if (organDTO == null){
            LOGGER.error("找不到该机构");
            return null;
        }
        Date endDate = DateTime.now().toDate();
        Date startDate = DateConversion.getDateTimeDaysAgo(180);

        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        QueryRecipeRequestTO request = new QueryRecipeRequestTO();
        PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
        patientBaseInfo.setPatientName(patientDTO.getPatientName());
        patientBaseInfo.setCertificate(patientDTO.getCertificate());
        patientBaseInfo.setCertificateType(patientDTO.getCertificateType());
        request.setPatientInfo(patientBaseInfo);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setOrgan(organId);
        LOGGER.info("getHosRecipeList request={}", JSONUtils.toString(request));
        QueryRecipeResponseTO response = null;
        try {
            response = hisService.queryRecipeListInfo(request);
        } catch (Exception e) {
            LOGGER.warn("getHosRecipeList his error. ", e);
        }
        LOGGER.info("getHosRecipeListInfoByMpiId  response={}", JSONUtils.toString(response));
        return response;
    }*/

    @RpcService
    public RecipeResultBean getPageDetail(int recipeId) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        Recipe nowRecipe = DAOFactory.getDAO(RecipeDAO.class).get(recipeId);
        if(null == nowRecipe){
            LOGGER.info("getPageDetailed: [recipeId:" + recipeId + "] 对应的处方信息不存在！");
            result.setCode(RecipeResultBean.FAIL);
            result.setError("处方单id对应的处方为空");
            return result;
        }
        Map<String, String> ext = new HashMap<>();
        if(null == nowRecipe.getOrderCode()){
            Map<String, Object> recipeMap = getPatientRecipeById(recipeId);
            result.setObject(recipeMap);
            ext.put("jumpType", "0");
            result.setExt(ext);
        }else{
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            RecipeResultBean orderDetail = orderService.getOrderDetail(nowRecipe.getOrderCode());
            result = orderDetail;
            Map<String, String> nowExt = result.getExt();
            if(null == nowExt){
                ext.put("jumpType", "1");
                result.setExt(ext);
            }else{
                nowExt.put("jumpType", "1");
                result.setExt(nowExt);
            }
        }
        return result;
    }

    @RpcService
    public RecipeResultBean changeRecipeStatusInfo(int recipeId, int status) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(recipeId);
        if(null == recipe){
            LOGGER.info("changeRecipeStatusInfo: [recipeId:" + recipeId + "] 对应的处方信息不存在！");
            result.setCode(RecipeResultBean.FAIL);
            result.setError("处方单id对应的处方为空");
            return result;
        }
        Map<String, Object> searchMap = new HashMap<>();
        //判断修改的处方的状态是否是已下载
        if(status == RecipeStatusConstant.RECIPE_DOWNLOADED){
            //当前处方下载处方状态的时候，确认处方的购药方式
            //首先判断处方的
            if(havChooseFlag == recipe.getChooseFlag() && RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE != recipe.getGiveMode()){
                LOGGER.info("changeRecipeStatusInfo: [recipeId:" + recipeId + "] 对应的处方的购药方式不是下载处方不能设置成已下载状态！");
                result.setCode(RecipeResultBean.FAIL);
                result.setError("处方选择的购药方式不是下载处方");
                return result;
            }
            Integer beforStatus = recipe.getStatus();
            searchMap.put("giveMode", RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE);
            searchMap.put("chooseFlag", havChooseFlag);
            //更新处方的信息
            Boolean updateResult = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.RECIPE_DOWNLOADED, searchMap);
            //更新处方log信息
            if(updateResult){
                RecipeLogService.saveRecipeLog(recipeId, beforStatus, RecipeStatusConstant.RECIPE_DOWNLOADED, "已下载状态修改成功");
            }else{
                LOGGER.info("changeRecipeStatusInfo: [recipeId:" + recipeId + "] 处方更新已下载状态失败！");
                result.setCode(RecipeResultBean.FAIL);
                result.setError("处方更新已下载状态失败");
                return result;
            }
        }
        return result;
    }

    /**
     * 定时任务:定时将下载处方后3天的处方设置成已完成
     * 每小时扫描一次，当前时间到前3天时间轴上的处方已下载
     */
    @RpcService
    public void changeDownLoadToFinishTask() {
        LOGGER.info("changeDownLoadToFinishTask: 开始定时任务，设置已下载3天后处方为已完成！");
        //首先获取当前时间前6天的时间到当前时间前3天时间区间
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DateConversion.DEFAULT_DATE_TIME);
        String endDate = LocalDateTime.now().minusDays(3).format(fmt);
        String startDate = LocalDateTime.now().minusDays(6).format(fmt);
        //获取当前时间区间状态是已下载的处方单
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<Recipe> recipeList = recipeDAO.findDowloadedRecipeToFinishList(startDate, endDate);
        Integer recipeId;
        //将处方单状态设置为已完成
        if(CollectionUtils.isNotEmpty(recipeList)){
            for(Recipe recipe : recipeList){
                //更新处方的状态-已完成
                recipeId = recipe.getRecipeId();
                Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), RecipeStatusConstant.FINISH, null);
                //完成订单
                if (rs) {
                    LOGGER.info("changeDownLoadToFinishTask: 处方{}设置处方为已完成！", recipeId);
                    //完成订单
                    RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                    RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

                    orderService.finishOrder(recipe.getOrderCode(), recipe.getPayMode(), null);
                    LOGGER.info("changeDownLoadToFinishTask: 订单{}设置为已完成！", recipe.getOrderCode());
                    //记录日志
                    RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.RECIPE_DOWNLOADED, RecipeStatusConstant.FINISH, "下载处方订单完成");
                    //HIS消息发送
                    hisService.recipeFinish(recipeId);
                    //发送取药完成消息(暂时不需要发送消息推送)

                    //监管平台核销上传
                    SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
                    syncExecutorService.uploadRecipeVerificationIndicators(recipeId);
                } else {
                    LOGGER.warn("处方：{},更新失败", recipe.getRecipeId());
                }
            }
        }

    }

}
