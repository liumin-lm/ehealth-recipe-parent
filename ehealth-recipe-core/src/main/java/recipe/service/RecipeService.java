package recipe.service;

import ca.service.ISignRecipeInfoService;
import ca.vo.CaSignResultVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.cdr.model.DiseaseDTO;
import com.ngari.base.cdr.service.IDiseaseService;
import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.base.organconfig.service.IOrganConfigService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.push.model.SmsInfoBean;
import com.ngari.base.push.service.ISmsPushService;
import com.ngari.base.serviceconfig.service.IHisServiceConfigService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.common.service.IConsultService;
import com.ngari.his.recipe.mode.*;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.home.asyn.model.BussCancelEvent;
import com.ngari.home.asyn.model.BussFinishEvent;
import com.ngari.home.asyn.service.IAsynDoBussService;
import com.ngari.opbase.log.mode.DataSyncDTO;
import com.ngari.opbase.log.service.IDataSyncLogService;
import com.ngari.patient.ds.PatientDS;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.dto.*;
import com.ngari.patient.service.*;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.basic.ds.PatientVO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.common.RequestVisitVO;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import com.ngari.recipe.recipeorder.model.RecipeOrderInfoBean;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.request.ValidRevisitRequest;
import com.ngari.revisit.common.service.IRevisitService;
import com.ngari.revisit.process.service.IRecipeOnLineRevisitService;
import ctd.account.UserRoleToken;
import ctd.net.broadcast.MQHelper;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.schema.exception.ValidateException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.event.GlobalEventExecFactory;
import eh.base.constant.ErrorCode;
import eh.base.constant.PageConstant;
import eh.cdr.constant.OrderStatusConstant;
import eh.recipeaudit.model.AuditMedicinesBean;
import eh.recipeaudit.model.RecipeCheckBean;
import eh.utils.params.ParameterConstant;
import eh.wxpay.constant.PayConstant;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.Args;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.aop.LogRecord;
import recipe.audit.auditmode.AuditModeContext;
import recipe.bean.CheckYsInfoBean;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.RecipeInvalidDTO;
import recipe.business.CaBusinessService;
import recipe.business.DrugBusinessService;
import recipe.bussutil.RecipeValidateUtil;
import recipe.caNew.AbstractCaProcessType;
import recipe.caNew.CaAfterProcessType;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.*;
import recipe.common.CommonConstant;
import recipe.common.OnsConfig;
import recipe.common.response.CommonResponse;
import recipe.constant.*;
import recipe.core.api.IStockBusinessService;
import recipe.dao.*;
import recipe.dao.bean.PatientRecipeBean;
import recipe.drugTool.service.DrugToolService;
import recipe.drugTool.validate.RecipeDetailValidateTool;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.HdRemoteService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.drugsenterprise.YtRemoteService;
import recipe.drugsenterprise.bean.YdUrlPatient;
import recipe.enumerate.status.*;
import recipe.enumerate.type.*;
import recipe.hisservice.HisMqRequestInit;
import recipe.hisservice.RecipeToHisMqService;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.hisservice.syncdata.SyncExecutorService;
import recipe.manager.*;
import recipe.mq.Buss2SessionProducer;
import recipe.purchase.PurchaseService;
import recipe.service.common.RecipeCacheService;
import recipe.service.common.RecipeSignService;
import recipe.serviceprovider.recipe.service.RemoteRecipeService;
import recipe.thread.*;
import recipe.util.*;
import recipe.vo.patient.RecipeGiveModeButtonRes;
import video.ainemo.server.IVideoInfoService;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ctd.persistence.DAOFactory.getDAO;
import static recipe.service.RecipeServiceSub.getRecipeAndDetailByIdImpl;
import static recipe.service.afterpay.IAfterPayBussService.REVISIT_STATUS_IN;

/**
 * 处方服务类
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2016/4/27.
 */
@RpcBean("recipeService")
public class RecipeService extends RecipeBaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeService.class);

    private static List<String> beforeCAList = Arrays.asList("gdsign", "gdsign|2", "jiangsuCA", "beijingCA", "bjYwxCA");


    private static final Integer CA_OLD_TYPE = new Integer(0);

    private static final Integer CA_NEW_TYPE = new Integer(1);

    private PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);

    private DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);

    private static IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);

    private RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);

    private ISignRecipeInfoService signRecipeInfoService = AppDomainContext.getBean("ca.signRecipeInfoService", ISignRecipeInfoService.class);

    private IDataSyncLogService dataSyncLogService = AppDomainContext.getBean("opbase.dataSyncLogService", IDataSyncLogService.class);

    private static final int havChooseFlag = 1;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private CreatePdfFactory createPdfFactory;
    @Resource
    private AuditModeContext auditModeContext;
    @Resource
    private OrganDrugListDAO organDrugListDAO;
    @Resource
    private DrugListMatchDAO drugListMatchDAO;
    @Autowired
    private IConfigurationCenterUtilsService configService;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private OperationClient operationClient;
    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;
    @Autowired
    private RecipeServiceSub recipeServiceSub;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private DrugToolService drugToolService;
    @Autowired
    private OrganDrugListService organDrugListService;
    @Autowired
    private PharmacyTcmService pharmacyTcmService;
    @Resource
    private CaAfterProcessType caAfterProcessType;
    @Resource
    private RecipeOrderDAO recipeOrderDAO;
    @Resource
    private SyncDrugExcDAO syncDrugExcDAO;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private IStockBusinessService drugEnterpriseBusinessService;
    @Autowired
    private RemoteRecipeService remoteRecipeService;
    @Autowired
    private RevisitManager revisitManager;
    @Autowired
    private ConsultManager consultManager;
    @Autowired
    private PayClient payClient;
    @Autowired
    private RecipeAuditClient recipeAuditClient;
    @Autowired
    private RecipeOrderPayFlowManager recipeOrderPayFlowManager;
    @Autowired
    private ButtonManager buttonManager;
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private DepartManager departManager;
    @Autowired
    private RecipeDetailValidateTool recipeDetailValidateTool;
    @Autowired
    private DocIndexClient docIndexClient;
    @Autowired
    private IStockBusinessService stockBusinessService;
    @Resource
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private StateManager stateManager;
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Autowired
    private OrderManager orderManager;
    @Resource
    private DrugManager drugManager;
    @Resource
    private CaManager caManager;
    @Autowired
    private OrganManager organManager;
    @Autowired
    private OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO;
    @Autowired
    private OrganConfigService organConfigService;
    @Autowired
    CaBusinessService caBusinessService;
    @Autowired
    DrugBusinessService drugBusinessService;
    @Resource
    private DrugListDAO drugListDAO;
    @Autowired
    private OrganDrugListSyncFieldDAO organDrugListSyncFieldDAO;


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
    /**
     * 患者手动退款
     */
    public static final int REFUND_PATIENT = 5;

    public static final Integer RECIPE_EXPIRED_DAYS = 3;

    /**
     * 二次签名处方审核不通过过期时间
     */
    public static final Integer RECIPE_EXPIRED_SECTION = 30;

    /**
     * 过期处方查询起始天数
     */
    public static final Integer RECIPE_EXPIRED_SEARCH_DAYS = 13;

    @Autowired
    private DrugOrganConfigDAO drugOrganConfigDao;


    @RpcService
    public RecipeBean getByRecipeId(int recipeId) {
        Recipe recipe = DAOFactory.getDAO(RecipeDAO.class).get(recipeId);
        return ObjectCopyUtils.convert(recipe, RecipeBean.class);
    }

    @RpcService
    public List<RecipeBean> findRecipe(int start, int limit) {
        List<Recipe> recipes = DAOFactory.getDAO(RecipeDAO.class).findRecipeByStartAndLimit(start, limit);
        return ObjectCopyUtils.convert(recipes, RecipeBean.class);
    }

    /**
     * 复诊页面点击开处方
     * 判断视频问诊后才能开具处方 并且视频大于30s
     */
    @RpcService
    public void openRecipeOrNotForVideo(CanOpenRecipeReqDTO req) {
        Args.notNull(req.getOrganId(), "organId");
        Args.notNull(req.getClinicID(), "clinicID");
        Boolean openRecipeOrNotForVideo = false;
        try {
            IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            openRecipeOrNotForVideo = (Boolean) configurationCenterUtilsService.getConfiguration(req.getOrganId(), "openRecipeOrNotForVideo");
        } catch (Exception e) {
            LOGGER.error("openRecipeOrNotForVideo error", e);
        }
        if (openRecipeOrNotForVideo) {
            IVideoInfoService videoInfoService = AppContextHolder.getBean("video.videoInfoService", IVideoInfoService.class);
            //字典eh.bus.dictionary.VideoBussType
            Boolean canVideo = videoInfoService.haveVideoByIdAndTime(req.getClinicID(), 35, 30);
            if (!canVideo) {
                throw new DAOException(609, "您与患者的视频未达到医院规定时长，无法开具处方。若达到时长请稍后再次尝试开具处方");
            }
        }
    }

    /**
     * 判断医生是否可以处方
     *
     * @param doctorId 医生ID
     * @return Map<String, Object>
     */
    @RpcService
    @Deprecated
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
                medicalFlag = true == set.getMedicarePrescription();
            }
        }

        map.put("result", canCreateRecipe);
        map.put("medicalFlag", medicalFlag);
        map.put("tips", tips);
        return map;

    }

    /**
     * 判断医生是否可以处方
     *
     * @param doctorId 医生ID
     * @return Map<String, Object>
     */
    @RpcService
    @Deprecated
    public Map<String, Object> openRecipeOrNotNew(Integer doctorId, Integer organId) {
        Args.notNull(organId, "organId");
        Args.notNull(doctorId, "doctorId");
        ConsultSetService consultSetService = ApplicationUtils.getBasicService(ConsultSetService.class);
        IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);

        Map<String, Object> map = Maps.newHashMap();
        OrganDrugListDAO organDrugListDAO = getDAO(OrganDrugListDAO.class);
        DrugListDAO drugListDAO = getDAO(DrugListDAO.class);
        int listNum = organDrugListDAO.getCountByOrganIdAndStatus(Arrays.asList(organId));
        boolean haveDrug = listNum > 0;

        //获取中药数量
        boolean havezhongDrugNum = false;
        if (haveDrug) {
            Long zhongDrugNum = drugListDAO.getSpecifyNum(organId, 3);
            havezhongDrugNum = zhongDrugNum > 0;
        }

        ConsultSetDTO consultSetDTO = consultSetService.getBeanByDoctorId(doctorId);
        //西药开方权
        boolean xiYaoRecipeRight = null == consultSetDTO.getXiYaoRecipeRight() ? false : consultSetDTO.getXiYaoRecipeRight();
        //中成药开方权
        boolean zhongChengRecipeRight = null == consultSetDTO.getZhongChengRecipeRight() ? false : consultSetDTO.getZhongChengRecipeRight();
        //中药开方权
        boolean zhongRecipeRight = null != consultSetDTO.getZhongRecipeRight() && consultSetDTO.getZhongRecipeRight() && havezhongDrugNum;
        //膏方开方权
        boolean gaoFangRecipeRight = null == consultSetDTO.getGaoFangRecipeRight() ? false : consultSetDTO.getGaoFangRecipeRight();
        // 靶向药开方权
        boolean targetedDrugTypeRecipeRight = null == consultSetDTO.getTargetedDrugTypeRecipeRight() ? false : consultSetDTO.getTargetedDrugTypeRecipeRight();
        map.put("xiYaoRecipeRight", xiYaoRecipeRight);
        map.put("zhongChengRecipeRight", zhongChengRecipeRight);
        map.put("zhongRecipeRight", zhongRecipeRight);
        map.put("gaoFangRecipeRight", gaoFangRecipeRight);
        map.put("targetedDrugTypeRecipeRight", targetedDrugTypeRecipeRight);
        //开方权
        boolean prescription = false;
        if (xiYaoRecipeRight || zhongChengRecipeRight || zhongRecipeRight || gaoFangRecipeRight || targetedDrugTypeRecipeRight) {
            prescription = true;
        }

        String tips = (String) configurationCenterUtilsService.getConfiguration(organId, "DoctorRecipeNoPermissionText");

        //能否开医保处方
        boolean medicalFlag = false;
        if (haveDrug) {
            ConsultSetDTO set = consultSetService.getBeanByDoctorId(doctorId);
            if (null != set && null != set.getMedicarePrescription()) {
                medicalFlag = true == set.getMedicarePrescription();
            }
        }
        //开处方时增加无法配送时间文案提示
        String openRecipeTopText = (String) configurationCenterUtilsService.getConfiguration(organId, "openRecipeTopTextConfig");
        map.put("unSendTitle", openRecipeTopText);

        map.put("result", prescription && haveDrug);
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
        RecipeExtendDAO recipeExtendDAO = getDAO(RecipeExtendDAO.class);
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
            return true;
        }
        if (null == recipe.getStatus() || (recipe.getStatus() > RecipeStatusConstant.UNSIGN) && recipe.getStatus() != RecipeStatusConstant.HIS_FAIL) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方单不是新处方或者审核失败的处方，不能删除");
        }

        boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.DELETE, null);
        stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_DELETED, RecipeStateEnum.SUB_DELETED_DOCTOR_NOT_SUBMIT);
        //记录日志
        RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusConstant.DELETE, "删除处方单");

        return rs;
    }

    /**
     * 保存处方
     *
     * @param recipeBean     处方对象
     * @param detailBeanList 处方详情
     * @return int
     */
    @RpcService
    @LogRecord
    public Integer saveRecipeData(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList) {
        Integer recipeId = recipeServiceSub.saveRecipeDataImpl(recipeBean, detailBeanList, 1);
        updateRevisitOrConsultInfo(recipeId, recipeBean.getBussSource(), recipeBean.getClinicId());
        RecipeBusiThreadPool.execute(() -> drugManager.saveCommonDrug(recipeId));
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
     * 暂存时更新复诊或者咨询recipeId
     *
     * @param recipeId
     * @param bussSource
     * @param clinicId
     */
    private void updateRevisitOrConsultInfo(Integer recipeId, Integer bussSource, Integer clinicId) {
        try {
            if (ValidateUtil.validateObjects(recipeId, clinicId, bussSource)) {
                return;
            }
            if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(bussSource)) {
                revisitManager.updateRecipeIdByConsultId(recipeId, clinicId);
            } else if (RecipeBussConstant.BUSS_SOURCE_WZ.equals(bussSource)) {
                consultManager.updateRecipeIdByConsultId(recipeId, clinicId);
            }
        } catch (Exception e) {
            LOGGER.error("updateRevisitOrConsultInfo error recipeId:{}", recipeId, e);
        }
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
            Recipe recipe = recipeDAO.get(recipeId);
            if (null != recipe) {
                if (StringUtils.isEmpty(address)) {
                    //从订单获取
                    RecipeOrder order = recipeOrderDAO.getOrderByRecipeId(recipeId);
                    if (null != order && (null != order.getAddressID() || TakeMedicineWayEnum.TAKE_MEDICINE_STATION.getType().equals(order.getTakeMedicineWay()))) {
                        address = orderManager.getCompleteAddress(order);
                    }
                }
            }
        }
        return address;
    }


    /**
     * 医生端处方重新编辑功能, 返回处方药品信息
     * 详情页reOpenRecipeEditorFlag=true时，前端展示重新编辑按钮
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public List<RecipeDetailBean> reCreatedRecipe(Integer recipeId) {
        LOGGER.info("recipeService reCreatedRecipe recipeId = {}", JSON.toJSONString(recipeId));
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        //查询现有（原来）处方数据信息
        Recipe dbRecipe = RecipeValidateUtil.checkRecipeCommonInfo(recipeId, resultBean);
        if (null == dbRecipe) {
            LOGGER.error("reCreatedRecipe 平台无该处方对象. recipeId=[{}] error={}", recipeId, JSONUtils.toString(resultBean));
            return Lists.newArrayList();
        }

        if (null == dbRecipe.getStatus() || dbRecipe.getStatus() != RecipeStatusConstant.HIS_FAIL) {
            LOGGER.error("reCreatedRecipe 该处方不是审核未通过的处方或者His写入失败的处方. recipeId=[{}]", recipeId);
            return Lists.newArrayList();
        }
        return RecipeValidateUtil.validateDrugsImpl(dbRecipe);
    }

    /**
     * 重新开具 或这续方时校验 药品数据
     *
     * @param recipeId
     * @return
     */
    @RpcService
    @Deprecated
    public List<RecipeDetailBean> validateDrugs(Integer recipeId) {
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Recipe dbRecipe = RecipeValidateUtil.checkRecipeCommonInfo(recipeId, resultBean);
        if (null == dbRecipe) {
            LOGGER.error("validateDrugs 平台无该处方对象. recipeId=[{}] error={}", recipeId, JSONUtils.toString(resultBean));
            return Lists.newArrayList();
        }
        List<RecipeDetailBean> detailBeans = RecipeValidateUtil.validateDrugsImpl(dbRecipe);
        return detailBeans;
    }

    /**
     * 重新开具 或这续方时校验 药品数据---new校验接口，原接口保留-app端有对validateDrugs单独处理
     * 还有暂存的处方点进来时做药房配置的判断
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public void validateDrugsData(Integer recipeId) {
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Recipe dbRecipe = RecipeValidateUtil.checkRecipeCommonInfo(recipeId, resultBean);
        if (null == dbRecipe) {
            LOGGER.error("validateDrugsData 平台无该处方对象. recipeId=[{}] ", recipeId);
            throw new DAOException(609, "获取不到处方数据");
        }
        List<Recipedetail> details = detailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isEmpty(details)) {
            return;
        }
        List<RecipeDetailBean> detailBeans = ObjectCopyUtils.convert(details, RecipeDetailBean.class);
        //药房配置校验
        if (CollectionUtils.isNotEmpty(detailBeans)) {
            List<PharmacyTcm> pharmacyTcms = pharmacyTcmDAO.findByOrganId(dbRecipe.getClinicOrgan());
            if (CollectionUtils.isNotEmpty(pharmacyTcms)) {
                List<Integer> pharmacyIdList = pharmacyTcms.stream().map(PharmacyTcm::getPharmacyId).collect(Collectors.toList());
                OrganDrugList organDrugList;
                for (RecipeDetailBean recipedetail : detailBeans) {
                    if (recipedetail.getPharmacyId() == null || recipedetail.getPharmacyId() == 0) {
                        //throw new DAOException(609, "您所在的机构已更新药房配置，需要重新开具处方");
                        continue;
                    }
                    //判断药房机构库配置
                    if (!pharmacyIdList.contains(recipedetail.getPharmacyId())) {
                        throw new DAOException(609, "您所在的机构已更新药房配置，需要重新开具处方");
                    }
                    //判断药品归属药房
                    organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(dbRecipe.getClinicOrgan(), recipedetail.getOrganDrugCode(), recipedetail.getDrugId());
                    if (organDrugList != null) {
                        if (StringUtils.isNotEmpty(organDrugList.getPharmacy())) {
                            List<String> pharmacyIds = Splitter.on(",").splitToList(organDrugList.getPharmacy());
                            if (!pharmacyIds.contains(String.valueOf(recipedetail.getPharmacyId()))) {
                                throw new DAOException(609, "您所在的机构已更新药房配置，需要重新开具处方");
                            }
                        } else {
                            throw new DAOException(609, "您所在的机构已更新药房配置，需要重新开具处方");
                        }
                    }
                }
            }
        }
    }

    /**
     * 写入his成功后，生成pdf并签名
     *
     * @param oldRecipe
     */
    @LogRecord
    private RecipeResultBean generateRecipePdfAndSign(Recipe oldRecipe) {
        if (SignEnum.SIGN_STATE_ORDER.getType().equals(oldRecipe.getDoctorSignState())) {
            return RecipeResultBean.getSuccess();
        }
        Integer recipeId = oldRecipe.getRecipeId();
        stateManager.updateStatus(recipeId, RecipeStatusEnum.RECIPE_STATUS_SIGN_ING_CODE_DOC, SignEnum.SIGN_STATE_SUBMIT);
        stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_SUBMIT, RecipeStateEnum.NONE);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        boolean old;
        try {
            old = createPdfFactory.generateRecipePdf(recipe);
        } catch (Exception e) {
            LOGGER.error("generateRecipePdfAndSign old标准化CA签章报错 recipeId={} ,doctor={} ,e==============", recipeId, recipe.getDoctor(), e);
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "old标准对接CA方式 医生部分pdf的生成失败");
            return RecipeResultBean.getFail();
        }
        if (old) {
            stateManager.updateStatus(recipeId, RecipeStatusEnum.RECIPE_STATUS_SIGN_SUCCESS_CODE_DOC, SignEnum.SIGN_STATE_ORDER);
            stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_SUBMIT, RecipeStateEnum.NONE);
            //调用审方等接口 并修改处方状态
            RecipeMsgService.batchSendMsg(recipeId, RecipeMsgEnum.PRESCRIBE_SUCCESS.getStatus());
            caAfterProcessType.caComplete(recipe, "老模式不走ca回调,签名成功");
        }
        createPdfFactory.updatePdfToImg(recipeId, SignImageTypeEnum.SIGN_IMAGE_TYPE_DOCTOR.getType());
        return RecipeResultBean.getSuccess();
    }

    //重试二次医生审核通过签名
    @Deprecated
    public void retryDoctorSecondCheckPass(Recipe recipe) {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);

        Integer afterStatus = RecipeStatusConstant.CHECK_PASS_YS;
        //添加后置状态设置
        if (ReviewTypeConstant.Postposition_Check == recipe.getReviewType()) {
            if (!recipe.canMedicalPay()) {
                RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
                boolean effective = orderDAO.isEffectiveOrder(recipe.getOrderCode());
                if (null != recipe.getOrderCode() && !effective) {
                    LOGGER.warn("当前处方{}已失效");
                    return;
                }
            } else {
                afterStatus = RecipeStatusConstant.CHECK_PASS;
            }
        } else if (ReviewTypeConstant.Preposition_Check == recipe.getReviewType()) {
            afterStatus = RecipeStatusConstant.CHECK_PASS;
        }
        if (!recipe.canMedicalPay()) {
            RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
            boolean effective = orderDAO.isEffectiveOrder(recipe.getOrderCode());
            if (null != recipe.getOrderCode() && !effective) {
                LOGGER.warn("当前处方{}已失效");
                return;
            }
        } else {
            afterStatus = RecipeStatusConstant.CHECK_PASS;
        }
        Map<String, Object> updateMap = new HashMap<>();

        //date 20190929
        //这里提示文案描述，扩展成二次审核通过/二次审核不通过的说明
        recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), afterStatus, updateMap);
        afterCheckPassYs(recipe);
        //date20200227 判断前置的时候二次签名成功，发对应的消息
        if (ReviewTypeConstant.Preposition_Check == recipe.getReviewType()) {
            auditModeContext.getAuditModes(recipe.getReviewType()).afterCheckPassYs(recipe);
        }
    }

    //重试二次医生审核不通过签名
    @Deprecated
    public void retryDoctorSecondCheckNoPass(Recipe dbRecipe) {
        LOGGER.info("retryDoctorSecondCheckNoPass recipeId = {}", dbRecipe.getRecipeId());
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        //date 2020/1/2
        //发送二次不通过消息判断是否是二次审核不通过
        if (!RecipecCheckStatusConstant.Check_Normal.equals(dbRecipe.getCheckStatus())) {
            //添加发送不通过消息
            RecipeMsgService.batchSendMsg(dbRecipe, RecipeStatusConstant.CHECK_NOT_PASSYS_REACHPAY);
            //更新处方一次审核不通过标记
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("checkStatus", RecipecCheckStatusConstant.Check_Normal);
            recipeDAO.updateRecipeInfoByRecipeId(dbRecipe.getRecipeId(), updateMap);
            //HIS消息发送
            //审核不通过 往his更新状态（已取消）
            Recipe recipe = recipeDAO.getByRecipeId(dbRecipe.getRecipeId());
            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
            hisService.recipeStatusUpdate(recipe.getRecipeId());
            //记录日志
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "审核不通过处理完成");
        }

        //患者如果使用优惠券将优惠券解锁
        RecipeCouponService recipeCouponService = ApplicationUtils.getRecipeService(RecipeCouponService.class);
        recipeCouponService.unuseCouponByRecipeId(dbRecipe.getRecipeId());

        //根据审方模式改变--审核未通过处理
        auditModeContext.getAuditModes(dbRecipe.getReviewType()).afterCheckNotPassYs(dbRecipe);
    }

    /**
     * his回调后 ca签名处理
     *
     * @param recipeId
     */
    @RpcService
    @LogRecord
    public void retryDoctorSignCheck(Integer recipeId) {
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        Integer caType = caManager.caProcessType(recipe);
        if (null == caType) {
            return;
        }
        RecipeResultBean recipeSignResult;
        if (CA_OLD_TYPE.equals(caType)) {
            recipeSignResult = this.generateRecipePdfAndSign(recipe);
        } else {
            recipeSignResult = AbstractCaProcessType.getCaProcessFactory(recipe.getClinicOrgan()).hisCallBackCARecipeFunction(recipe.getRecipeId());
        }
        LOGGER.info("retryDoctorSignCheck recipeSignResult ！{}", JSON.toJSONString(recipeSignResult));
        //添加逻辑：ca返回异步无结果
        if (RecipeResultBean.FAIL.equals(recipeSignResult.getCode())) {
            //说明处方签名失败
            stateManager.updateStatus(recipeId, RecipeStatusEnum.RECIPE_STATUS_SIGN_ERROR_CODE_DOC, SignEnum.SIGN_STATE_AUDIT);
            stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_SUBMIT, RecipeStateEnum.NONE);
            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "说明处方签名失败:" + recipeSignResult.getMsg());
            //CA同步回调的接口 发送环信消息
            if (new Integer(2).equals(recipe.getBussSource())) {
                IRecipeOnLineRevisitService recipeOnLineRevisitService = RevisitAPI.getService(IRecipeOnLineRevisitService.class);
                recipeOnLineRevisitService.sendRecipeDefeat(recipe.getRecipeId(), recipe.getClinicId());
            }
        }
    }

    /**
     * 药师ca回调
     *
     * @param resultVo
     */
    @RpcService
    public void retryCaPharmacistCallBackToRecipe(CaSignResultVo resultVo) {
        //ca完成签名签章后，将和返回的结果给平台
        //平台根据结果设置处方业务的跳转
        if (null == resultVo) {
            LOGGER.warn("当期药师签名异步调用接口返回参数为空，无法设置相关信息");
            return;
        }
        LOGGER.info("当前ca异步接口返回：{}", JSONUtils.toString(resultVo));
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeLogDAO recipeLogDAO = getDAO(RecipeLogDAO.class);
        Integer recipeId = resultVo.getRecipeId();

        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (RecipeStateEnum.PROCESS_STATE_DELETED.getType().equals(recipe.getProcessState()) ||
                RecipeStateEnum.PROCESS_STATE_CANCELLATION.getType().equals(recipe.getProcessState())) {
            LOGGER.info("retryCaPharmacistCallBackToRecipe 当前处方单已删除或作废");
            return;
        }
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);

        Integer organId = recipe.getClinicOrgan();
        RecipeResultBean checkResult = RecipeResultBean.getFail();

        Map<String, Object> esignResponseMap = resultVo.getEsignResponseMap();
        Integer caType = caManager.caProcessType(recipe);
        try {
            String fileId = null;
            DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getChecker());
            if (null == doctorDTO) {
                LOGGER.warn("当前处方{}审核药师为空，请检查处方相关信息", recipeId);
                return;
            }
            if (MapUtils.isNotEmpty(esignResponseMap)) {
                LOGGER.info("reviewRecipe esignService esignResponseMap:{}", JSONUtils.toString(esignResponseMap));
                //易签保返回0表示成功
                Integer code = MapValueUtil.getInteger(esignResponseMap, "code");
                if (new Integer(0).equals(code)) {
                    checkResult.setCode(RecipeResultBean.SUCCESS);
                }
            } else {
                if (new Integer(200).equals(resultVo.getCode())) {
                    //date 202001013 修改非易签保流程下的pdf
                    boolean usePlatform = true;
                    Object recipeUsePlatformCAPDF = configService.getConfiguration(organId, "recipeUsePlatformCAPDF");
                    if (null != recipeUsePlatformCAPDF) {
                        usePlatform = Boolean.parseBoolean(recipeUsePlatformCAPDF.toString());
                    }
                    //使用平台CA模式，手动生成pdf
                    //生成pdf分解成，先生成无医生药师签名的pdf，再将医生药师的签名放置在pdf上
                    String pdfString = null;
                    // 不做ca签名
                    if (!recipeAuditClient.isShowCheckCA(recipeId)) {
                        pharmacyToRecipePDFDefault(recipeId);
                        checkResult.setCode(RecipeResultBean.SUCCESS);
                    } else {
                        // 是否需要跳过pdf渲染
                        if (!usePlatform) {
                            if (null == resultVo.getPdfBase64()) {
                                LOGGER.warn("当前处方{}返回CA图片为空！", recipeId);
                            }
                            //只有当使用CApdf的时候才去赋值
                            pdfString = resultVo.getPdfBase64();
                        }
                        //保存签名值、时间戳、电子签章文件
                        checkResult.setCode(RecipeResultBean.SUCCESS);
                        RecipeServiceEsignExt.saveSignRecipePDFCA(pdfString, recipeId, null, resultVo.getSignCADate(), resultVo.getSignRecipeCode(), false, fileId);
                        resultVo.setFileId(fileId);
                        //date 20200922
                        //老流程保存sign，新流程已经移动至CA保存
                        if (CA_OLD_TYPE.equals(caType)) {
                            caManager.signRecipeInfoSave(recipe, resultVo, false);
                        }
                    }
                } else {
                    ISmsPushService smsPushService = AppContextHolder.getBean("eh.smsPushService", ISmsPushService.class);
                    SmsInfoBean smsInfo = new SmsInfoBean();
                    smsInfo.setBusId(0);
                    smsInfo.setOrganId(0);
                    smsInfo.setBusType("PhaSignNotify");
                    smsInfo.setSmsType("PhaSignNotify");
                    smsInfo.setExtendValue(doctorDTO.getUrt() + "|" + recipeId + "|" + doctorDTO.getLoginId());
                    smsPushService.pushMsgData2OnsExtendValue(smsInfo);
                    checkResult.setCode(RecipeResultBean.FAIL);
                }
            }

        } catch (Exception e) {
            LOGGER.error("reviewRecipe signFile 标准化CA签章报错 recipeId={}, doctor={}", recipeId, recipe.getDoctor(), e);
        }

        //首先判断当前ca是否是有结束结果的
        if (-1 == resultVo.getResultCode()) {
            LOGGER.info("当期处方{}药师ca签名异步调用接口返回：未触发处方业务结果", recipeId);
            return;
        }

        if (RecipeResultBean.FAIL == checkResult.getCode()) {
            //说明处方签名失败
            LOGGER.info("当前审核处方{}签名失败！", recipeId);
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.SIGN_ERROR_CODE_PHA, null);
            stateManager.updateCheckerSignState(recipeId, SignEnum.SIGN_STATE_AUDIT);
            recipeLogDAO.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), checkResult.getMsg());
            return;
        } else {
            //说明处方签名成功，记录日志，走签名成功逻辑
            LOGGER.info("当前审核处方{}签名成功！", recipeId);
            stateManager.updateCheckerSignState(recipeId, SignEnum.SIGN_STATE_ORDER);
            pharmacyToRecipePDF(recipeId);
            recipeLogDAO.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "当前审核处方签名成功");
            if (MapUtils.isNotEmpty(esignResponseMap)) {
                String recipeFileId = MapValueUtil.getString(esignResponseMap, "fileId");
                Map<String, Object> atrMap = new HashedMap();
                atrMap.put("chemistSignFile", recipeFileId);
                atrMap.put("SignFile", recipeFileId);
                boolean updateCheckPdf = recipeDAO.updateRecipeInfoByRecipeId(recipeId, atrMap);
                LOGGER.info("当前处方更新药师签名pdf结果：{}", updateCheckPdf);
            }
        }
        //组装审核的结果重新判断审核通过审核不通过
        //根据当前处方最新的审核结果判断审核，获取审核的结果
        RecipeCheckBean recipeCheckBean = recipeAuditClient.getNowCheckResultByRecipeId(recipe.getRecipeId());
        if (null == recipeCheckBean) {
            LOGGER.warn("当前药师签名的处方{}没有审核结果，无法进行签名", recipeId);
            return;
        }
        CheckYsInfoBean resultBean = new CheckYsInfoBean();
        resultBean.setCheckFailMemo(recipe.getCheckFailMemo());
        resultBean.setCheckResult(recipeCheckBean.getCheckStatus());
        List<RecipeCheckDetail> recipeCheckDetails = recipeAuditClient.findByCheckId(recipeCheckBean.getCheckId());
        resultBean.setCheckDetailList(recipeCheckDetails);
        int resultNow = recipeCheckBean.getCheckStatus();

        //date 20200512
        //更新处方审核结果状态
        int recipeStatus = RecipeStatusConstant.CHECK_NOT_PASS_YS;
        if (1 == resultNow) {
            //根据审方模式改变状态
            recipeStatus = auditModeContext.getAuditModes(recipe.getReviewType()).afterAuditRecipeChange();
            if (recipe.canMedicalPay()) {
                //如果是可医保支付的单子，审核是在用户看到之前，所以审核通过之后变为待处理状态
                recipeStatus = RecipeStatusConstant.CHECK_PASS;
            }
        }
        recipeDAO.updateRecipeInfoByRecipeId(recipeId, recipeStatus, null);
        //审核成功往药厂发消息
        //审方做异步处理
        GlobalEventExecFactory.instance().getExecutor().submit(() -> {
            if (1 == resultNow) {
                //审方成功，订单状态的
                auditModeContext.getAuditModes(recipe.getReviewType()).afterCheckPassYs(recipe);
            } else {
                //审核不通过后处理
                doAfterCheckNotPassYs(recipe);
            }
            //将审核结果推送HIS
            try {
                RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
                hisService.recipeAudit(recipe, resultBean);
            } catch (Exception e) {
                LOGGER.warn("saveCheckResult send recipeAudit to his error. recipeId={}", recipeId, e);
            }
            if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())) {
                try {
                    LOGGER.info("retryCaPharmacistCallBackToRecipe pendingTask start recipeId={}", recipeId);
                    //增加药师首页待处理任务---完成任务
                    ApplicationUtils.getBaseService(IAsynDoBussService.class).fireEvent(new BussFinishEvent(recipeId, BussTypeConstant.RECIPE));
                    LOGGER.info("retryCaPharmacistCallBackToRecipe pendingTask end");
                } catch (Exception e) {
                    LOGGER.warn("retryCaPharmacistCallBackToRecipe pendingTask error. recipeId={}", recipeId, e);
                }
            }
        });
        //推送处方到监管平台(审核后数据)
        RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(Collections.singletonList(recipe.getRecipeId()), 2));
        //审核通过盖章
        RecipeBusiThreadPool.execute(() -> remoteRecipeService.generateSignetRecipePdf(recipe.getRecipeId(), recipe.getClinicOrgan()));
    }


    /**
     * 重试   当状态为  11（his写入失败） 19（处方医保状态上传失败） 43（审方接口异常）的时候出现的情况  已取消的状态
     *
     * @param recipeId
     */
    @RpcService
    public RecipeResultBean sendNewRecipeToHIS(Integer recipeId) {
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

        Recipe recipe = recipeDAO.getByRecipeId(recipeId);

        //date 20191127
        //重试功能添加his写入失败的处方
        if (null == recipe || null == recipe.getStatus() || (recipe.getStatus() != RecipeStatusConstant.CHECKING_HOS && recipe.getStatus() != RecipeStatusConstant.HIS_FAIL)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方不能重试");
        }
        //his写回提示，处方推送成功，否则再次推送
        String recipeCode = recipe.getRecipeCode();
        if (StringUtils.isNotEmpty(recipeCode)) {
            resultBean.setCode(RecipeResultBean.PUSHSUCCESS);
            resultBean.setMsg("处方已推送成功");
        } else {
            resultBean.setCode(RecipeResultBean.SUCCESS);
            resultBean.setMsg("已重新提交医院系统");
        }
        LOGGER.info("sendNewRecipeToHIS before His! dbRecipe={}", JSONUtils.toString(recipe));
        hisService.recipeSendHis(recipeId, null);
        return resultBean;
    }

    /**
     * 签名服务（新-平台）
     *
     * @param recipeBean     处方
     * @param detailBeanList 详情
     * @param continueFlag   校验标识
     * @return Map<String, Object>
     */
    @RpcService
    @LogRecord
    public Map<String, Object> doSignRecipeNew(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList, int continueFlag) {
        LOGGER.info("RecipeService.doSignRecipeNew param: recipeBean={} detailBean={} continueFlag={}", JSONUtils.toString(recipeBean), JSONUtils.toString(detailBeanList), continueFlag);
        recipeDetailValidateTool.validateMedicalChineDrugNumber(recipeBean, detailBeanList);
        caManager.setCaPassWord(recipeBean.getClinicOrgan(), recipeBean.getDoctor(), recipeBean.getCaPassword());
        Map<String, Object> rMap = new HashMap<>();
        rMap.put("signResult", true);
        try {
            recipeBean.setDistributionFlag(continueFlag);
            //第一步暂存处方（处方状态未签名）
            doSignRecipeSave(recipeBean, detailBeanList);
            //第二步预校验
            if (continueFlag == 0) {
                HisSyncSupervisionService service = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
                RecipeBusiThreadPool.execute(() -> service.uploadRecipePrepareCheck(recipeBean.getRecipeId()));
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
            if (ValidateUtil.integerIsEmpty(recipeBean.getVersion())) {
                Integer appointEnterpriseType = recipeBean.getRecipeExtend().getAppointEnterpriseType();
                if ((continueFlag == 0 || continueFlag == 4) && ValidateUtil.integerIsEmpty(appointEnterpriseType)) {
                    rMap = drugEnterpriseBusinessService.enterpriseStockMap(recipeBean.getRecipeId(), StockCheckSourceTypeEnum.DOCTOR_STOCK.getType());
                    boolean signResult = Boolean.valueOf(rMap.get("signResult").toString());
                    if (!signResult) {
                        return rMap;
                    }
                }
            } else {
                if (StringUtils.isEmpty(recipeBean.getRecipeSupportGiveMode())) {
                    throw new DAOException(recipe.constant.ErrorCode.SERVICE_ERROR, "无购药方式");
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

            if (getIntellectJudicialFlag(recipeBean.getClinicOrgan())) {
                //更新审方信息
                RecipeBusiThreadPool.execute(new SaveAutoReviewRunnable(recipeBean, detailBeanList));
            }
            //健康卡数据上传
            RecipeBusiThreadPool.execute(new CardDataUploadRunable(recipeBean.getClinicOrgan(), recipeBean.getMpiid(), "010106"));
            Integer caType = configurationClient.getValueCatchReturnInteger(recipeBean.getClinicOrgan(), "CAProcessType", CA_OLD_TYPE);
            LOGGER.info("doSignRecipeNew caType={}", caType);
            //触发CA前置操作
            if (CA_OLD_TYPE.equals(caType)) {
                //老版默认走后置的逻辑，直接将处方推his
                caAfterProcessType.signCABeforeRecipeFunction(recipeBean, detailBeanList);
            } else {
                AbstractCaProcessType.getCaProcessFactory(recipeBean.getClinicOrgan()).signCABeforeRecipeFunction(recipeBean, detailBeanList);
            }
        } catch (Exception e) {
            LOGGER.error("doSignRecipeNew error", e);
            throw new DAOException(recipe.constant.ErrorCode.SERVICE_ERROR, e.getMessage());
        }

        rMap.put("signResult", true);
        rMap.put("recipeId", recipeBean.getRecipeId());
        rMap.put("consultId", recipeBean.getClinicId());
        rMap.put("errorFlag", false);
        rMap.put("canContinueFlag", "0");
        LOGGER.info("doSignRecipeNew execute ok! rMap:" + JSONUtils.toString(rMap));

        // 处方失效时间处理
        handleRecipeInvalidTime(recipeBean.getClinicOrgan(), recipeBean.getRecipeId(), recipeBean.getSignDate());
        revisitManager.saveRevisitTracesList(recipeDAO.get(recipeBean.getRecipeId()));
        return rMap;
    }

    /**
     * 设置处方失效时间，非当天小于24小时的发送失效延迟消息
     *
     * @param
     */
    public static void handleRecipeInvalidTime(Integer clinicOrgan, Integer recipeId, Date signDate) {
        try {
            RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
            if (Objects.isNull(signDate)) {
                signDate = recipeDAO.getByRecipeId(recipeId).getSignDate();
            }

            // 获取失效时间及类型
            RecipeInvalidDTO invalidDTO = getRecipeInvalidInfo(clinicOrgan, recipeId, signDate);
            if (null != invalidDTO.getInvalidDate()) {
                // 更新处方失效时间
                Map<String, Object> attMap = new HashMap<>();
                attMap.put("invalidTime", invalidDTO.getInvalidDate());
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, attMap);
                Date nowDate = new Date();
                // 已失效按失效逻辑处理（线下转线上可能计算失效时间时已失效）
                if (nowDate.getTime() >= invalidDTO.getInvalidDate().getTime()) {
                    Recipe recipe = recipeDAO.getByRecipeId(recipeId);
                    List<Recipe> recipeList = new ArrayList<>();
                    recipeList.add(recipe);
                    doRecipeCancelByInvalidTime(recipeList);
                } else {
                    // 未失效且为延迟队列处理类型-发送延迟消息，其他类型通过定时任务处理
                    if ("h".equals(invalidDTO.getInvalidType())) {
                        try {
                            // 毫秒
                            long millSecond = eh.utils.DateConversion.secondsBetweenDateTime(nowDate, invalidDTO.getInvalidDate()) * 1000;
                            LOGGER.info("机构处方失效时间-发送延迟消息内容，机构id={},处方id={},延迟时间={}毫秒", clinicOrgan, recipeId, millSecond);
                            MQHelper.getMqPublisher().publish(OnsConfig.recipeDelayTopic, String.valueOf(recipeId), RecipeSystemConstant.RECIPE_INVALID_TOPIC_TAG, String.valueOf(recipeId), millSecond);
                        } catch (Exception e) {
                            LOGGER.error("机构处方失效时间-发送延迟消息异常，机构id={},处方id={}", clinicOrgan, recipeId, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("机构处方失效时间-处理异常,机构id={},处方id={}", clinicOrgan, recipeId, e);
        }
    }

    public static RecipeInvalidDTO getRecipeInvalidInfo(Integer clinicOrgan, Integer recipeId, Date signDate) {
        RecipeInvalidDTO invalidDTO = new RecipeInvalidDTO();
        try {
            IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            Object invalidInfoObject = configurationService.getConfiguration(clinicOrgan, "recipeInvalidTime");
            JSONArray jsonArray = JSON.parseArray(JSONObject.toJSONString(invalidInfoObject));
            LOGGER.info("机构处方失效时间-查询配置结果，机构={},处方id={},配置={}", clinicOrgan, recipeId, JSONObject.toJSONString(invalidInfoObject));
            if (CollectionUtils.isNotEmpty(jsonArray)) {
                // 配置格式：签名当天后某天24点前=d2-天数;签名后大于24小时=d1-小时数;签名后小于一天=h-小时数
                // 签名后小于一天用延迟队列取消处方，其余由定时任务取消
                String[] invalidArr = jsonArray.getString(0).split("-");
                invalidDTO.setInvalidType(invalidArr[0]);
                Double invalidValue = Double.parseDouble(invalidArr[1]);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(signDate);
                Date invalidDate = null;
                switch (invalidArr[0]) {
                    case "d1":
                        // 签名时间往后推invalidValue小时
                        calendar.add(Calendar.HOUR, invalidValue.intValue());
                        invalidDate = calendar.getTime();
                        break;
                    case "d2":
                        // 签名时间往后推invalidValue天的最大时间
                        calendar.add(Calendar.DATE, invalidValue.intValue());
                        Date afterDate = calendar.getTime();
                        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(afterDate.getTime()), ZoneId.systemDefault());

                        LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
                        invalidDate = Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
                        break;
                    case "h":
                        // 签名时间往后推invalidValue*60 分钟
                        int minute = (int) (invalidValue * 60);
                        calendar.add(Calendar.MINUTE, minute);
                        invalidDate = calendar.getTime();
                        break;
                    default:
                        LOGGER.error("机构处方失效时间-配置格式错误，机构={},处方id={},配置={}", clinicOrgan, recipeId, JSONObject.toJSONString(invalidInfoObject));
                        break;
                }
                invalidDTO.setInvalidDate(invalidDate);
            }
        } catch (Exception e) {
            LOGGER.error("机构处方失效时间-计算失效失效异常，机构={}", clinicOrgan, e);
        }
        return invalidDTO;
    }


    /**
     * 签名服务（处方存储）
     *
     * @param recipe  处方
     * @param details 详情
     * @return
     */
    @RpcService
    @LogRecord
    public void doSignRecipeSave(RecipeBean recipe, List<RecipeDetailBean> details) {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        PatientDTO patient = patientService.get(recipe.getMpiid());
        //解决旧版本因为wx2.6患者身份证为null，而业务申请不成功
        if (patient == null || StringUtils.isEmpty(patient.getCertificate())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该患者还未填写身份证信息，不能开处方");
        }
        // 就诊人改造：为了确保删除就诊人后历史处方不会丢失，加入主账号用户id
        //bug#46436 本人就诊人被删除保存不了导致后续微信模板消息重复推送多次
        List<PatientDTO> requestPatients = patientService.findOwnPatient(patient.getLoginId());
        if (CollectionUtils.isNotEmpty(requestPatients)) {
            PatientDTO requestPatient = requestPatients.get(0);
            if (null != requestPatient && null != requestPatient.getMpiId()) {
                recipe.setRequestMpiId(requestPatient.getMpiId());
                // urt用于系统消息推送
                recipe.setRequestUrt(requestPatient.getUrt());
            }
        }
        boolean optimize = openRecipeOptimize(recipe);
        //配置开启，根据有效的挂号序号进行判断
        if (!optimize) {
            LOGGER.error("ErrorCode.SERVICE_ERROR={}", ErrorCode.SERVICE_ERROR);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "当前患者就诊信息已失效，无法进行开方。");
        }
        if (null != recipe.getRecipeExtend()) {
            EmrDetailDTO emrDetailDTO = docIndexClient.getEmrDetails(recipe.getRecipeExtend().getDocIndexId());
            recipe.setOrganDiseaseName(emrDetailDTO.getOrganDiseaseName());
            recipe.setOrganDiseaseId(emrDetailDTO.getOrganDiseaseId());
        }
        Integer recipeId = recipe.getRecipeId();
        LOGGER.info("doSignRecipeSave recipe={}", JSON.toJSONString(recipe));
        //如果是已经暂存过的处方单，要去数据库取状态 判断能不能进行签名操作
        details.stream().filter(a -> "无特殊煎法".equals(a.getMemo())).forEach(a -> a.setMemo(""));
        if (null != recipeId && recipeId > 0) {
            Integer status = recipeDAO.getStatusByRecipeId(recipeId);
            if (null == status || (status > RecipeStatusConstant.UNSIGN && status != RecipeStatusConstant.HIS_FAIL)) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "处方单已处理,不能重复签名");
            }
            updateRecipeAndDetail(recipe, details);
        } else {
            recipeId = saveRecipeData(recipe, details);
            recipe.setRecipeId(recipeId);
        }
    }


    @RpcService
    public void getConsultIdForRecipeSource(RecipeBean recipe, Boolean registerNo) {
        //根据申请人mpiid，requestMode 获取当前咨询单consultId
        //如果没有进行中的复诊就取进行中的咨询否则没有
        IConsultService iConsultService = ApplicationUtils.getConsultService(IConsultService.class);
        IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);

        ValidRevisitRequest revisitRequest = new ValidRevisitRequest();
        revisitRequest.setMpiId(recipe.getMpiid());
        revisitRequest.setDoctorID(recipe.getDoctor());
        revisitRequest.setRequestMode(RecipeSystemConstant.CONSULT_TYPE_RECIPE);
        revisitRequest.setRegisterNo(registerNo);
        LOGGER.info(" 处方查询复诊入参revisitRequest={}", JSONUtils.toString(revisitRequest));

        //获取在线复诊
        List<Integer> consultIds = new ArrayList<>();
        Integer consultId = null;
        Integer revisitId = iRevisitService.findValidRevisitByMpiIdAndDoctorId(revisitRequest);

        if (revisitId != null) {
            consultId = revisitId;
            recipe.setBussSource(2);
        } else {
            //图文咨询
            consultIds = iConsultService.findApplyingConsultByRequestMpiAndDoctorId(recipe.getRequestMpiId(), recipe.getDoctor(), RecipeSystemConstant.CONSULT_TYPE_GRAPHIC);
            if (CollectionUtils.isNotEmpty(consultIds)) {
                consultId = consultIds.get(0);
                recipe.setBussSource(1);
            }
        }
        recipe.setClinicId(consultId);
    }

    @RpcService
    @Deprecated
    public Boolean isOpenRecipeNumber(RequestVisitVO requestVisitVO) {
        return recipeManager.isOpenRecipeNumber(requestVisitVO.getClinicId(), requestVisitVO.getOrganId(), null);
    }


    /**
     * 修改处方
     *
     * @param recipeBean     处方对象
     * @param detailBeanList 处方详情
     */
    @RpcService
    @LogRecord
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

        Recipe dbRecipe = recipeDAO.getByRecipeId(recipeId);
        if (StringUtils.isEmpty(recipe.getRecipeSupportGiveMode())) {
            recipe.setRecipeSupportGiveMode(dbRecipe.getRecipeSupportGiveMode());
        }
        if (null == dbRecipe.getStatus() || (dbRecipe.getStatus() > RecipeStatusConstant.UNSIGN) && dbRecipe.getStatus() != RecipeStatusConstant.HIS_FAIL) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该处方单不是新处方或者审核失败的处方，不能修改");
        }

        int beforeStatus = dbRecipe.getStatus();
        List<Recipedetail> recipedetails = ObjectCopyUtils.convert(detailBeanList, Recipedetail.class);
        if (null != detailBeanList && detailBeanList.size() > 0) {
            if (null == recipedetails) {
                recipedetails = new ArrayList<>(0);
            }
        }

        RecipeServiceSub.setRecipeMoreInfo(recipe, recipedetails, recipeBean, 1);
        //将原先处方单详情的记录都置为无效 status=0
        recipeDetailDAO.updateDetailInvalidByRecipeId(recipeId);
        Integer dbRecipeId;

        try {
            dbRecipeId = recipeDAO.updateOrSaveRecipeAndDetail(recipe, recipedetails, true);
        } catch (Exception e) {
            LOGGER.error("recipeService updateRecipeAndDetail recipe:{} , recipedetails={}", JSON.toJSONString(recipe), JSON.toJSONString(recipedetails), e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }

        //武昌需求，加入处方扩展信息
        RecipeExtendBean recipeExt = recipeBean.getRecipeExtend();
        if (null != recipeExt && null != dbRecipeId) {
            RecipeExtend recipeExtend = ObjectCopyUtils.convert(recipeExt, RecipeExtend.class);
            recipeExtend.setRecipeId(dbRecipeId);
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
            String cardNo = recipeManager.getCardNoByRecipe(recipe);
            if (StringUtils.isNotEmpty(cardNo)) {
                recipeExtend.setCardNo(cardNo);
            }
            try {
                DrugDecoctionWayDao drugDecoctionWayDao = DAOFactory.getDAO(DrugDecoctionWayDao.class);
                if (null == recipeExtend.getDoctorIsDecoction()) {
                    recipeExtend.setDoctorIsDecoction("0");
                    if (StringUtils.isNotEmpty(recipeExtend.getDecoctionId())) {
                        DecoctionWay decoctionWay = drugDecoctionWayDao.get(Integer.parseInt(recipeExtend.getDecoctionId()));
                        if (decoctionWay.getGenerationisOfDecoction()) {
                            recipeExtend.setDoctorIsDecoction("1");
                        } else {
                            recipeExtend.setDoctorIsDecoction("0");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Integer recipeChooseChronicDisease = configurationClient.getValueCatchReturnInteger(recipeBean.getClinicOrgan(), "recipeChooseChronicDisease", 0);
            if (!RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipeBean.getBussSource()) && new Integer(6).equals(recipeChooseChronicDisease)) {
                recipeExtend.setRecipeChooseChronicDisease(null);
                recipeExtend.setChronicDiseaseCode("");
                recipeExtend.setChronicDiseaseName("");
            }
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            recipeExtendDAO.saveOrUpdateRecipeExtend(recipeExtend);
        }
        //记录日志
        RecipeLogService.saveRecipeLog(dbRecipeId, beforeStatus, beforeStatus, "修改处方单");
        return dbRecipeId;
    }

    public void setMergeDrugType(List<Recipedetail> recipedetails, Recipe dbRecipe) {
        //date  20200529 JRK
        //根据配置项重新设置处方类型和处方药品详情属性类型
        IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        boolean isMergeRecipeType = (null != configurationService.getConfiguration(dbRecipe.getClinicOrgan(), "isMergeRecipeType")) ? (Boolean) configurationService.getConfiguration(dbRecipe.getClinicOrgan(), "isMergeRecipeType") : false;
        //允许中西药合并
        DrugList nowDrugList;
        if (isMergeRecipeType) {
            if (CollectionUtils.isNotEmpty(recipedetails)) {
                nowDrugList = drugListDAO.getById(recipedetails.get(0).getDrugId());
                dbRecipe.setRecipeType(null != nowDrugList ? nowDrugList.getDrugType() : dbRecipe.getRecipeType());
                for (Recipedetail recipedetail : recipedetails) {
                    nowDrugList = drugListDAO.getById(recipedetail.getDrugId());
                    recipedetail.setDrugType(null != nowDrugList ? nowDrugList.getDrugType() : null);
                }
            }
        } else {
            recipedetails.forEach(detail -> {
                detail.setDrugType(dbRecipe.getRecipeType());
            });
        }
    }

    /**
     * 处方二次签名
     *
     * @param recipe
     * @return
     */
    @RpcService
    @LogRecord
    public RecipeResultBean doSecondSignRecipe(RecipeBean recipe) {
        LOGGER.info("RecipeService doSecondSignRecipe recipe ： {} ", JSON.toJSONString(recipe));
        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
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
        //添加后置状态设置
        if (ReviewTypeConstant.Postposition_Check == dbRecipe.getReviewType()) {
            if (!dbRecipe.canMedicalPay()) {
                RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
                boolean effective = orderDAO.isEffectiveOrder(dbRecipe.getOrderCode());
                if (null != recipe.getOrderCode() && !effective) {
                    resultBean.setCode(RecipeResultBean.FAIL);
                    resultBean.setMsg("该处方已失效");
                    return resultBean;
                }
            } else {
                afterStatus = RecipeStatusConstant.CHECK_PASS;
            }
        } else if (ReviewTypeConstant.Preposition_Check == dbRecipe.getReviewType()) {
            afterStatus = RecipeStatusConstant.CHECK_PASS;
        }
        if (!dbRecipe.canMedicalPay()) {
            RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
            boolean effective = orderDAO.isEffectiveOrder(dbRecipe.getOrderCode());
            if (null != recipe.getOrderCode() && !effective) {
                resultBean.setCode(RecipeResultBean.FAIL);
                resultBean.setMsg("该处方已失效");
                return resultBean;
            }
        } else {
            afterStatus = RecipeStatusConstant.CHECK_PASS;
        }
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("supplementaryMemo", recipe.getSupplementaryMemo());
        updateMap.put("checkStatus", RecipecCheckStatusConstant.Check_Normal);
        //二次签名 强制处方状态为通过 0 待审核  1 审核通过  2 审核不通过
        updateMap.put("checkFlag", 1);

        //date 20190929
        //这里提示文案描述，扩展成二次审核通过/二次审核不通过的说明
        recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), afterStatus, updateMap);
        afterCheckPassYs(dbRecipe);
        //date20200227 判断前置的时候二次签名成功，发对应的消息
        if (ReviewTypeConstant.Preposition_Check.equals(dbRecipe.getReviewType())) {
            auditModeContext.getAuditModes(dbRecipe.getReviewType()).afterCheckPassYs(dbRecipe);
        }
//        docIndexClient.updateStatusByBussIdBussType(recipe.getRecipeId(), DocIndexShowEnum.SHOW.getCode());
        stateManager.updateAuditState(recipe.getRecipeId(), RecipeAuditStateEnum.DOC_FORCED_PASS);
        recipeAuditClient.recipeAuditNotice(ObjectCopyUtils.convert(dbRecipe, com.ngari.platform.recipe.mode.RecipeBean.class), 1);
        LOGGER.info("RecipeService doSecondSignRecipe  execute ok!  recipeId ： {} ", recipe.getRecipeId());
        return resultBean;
    }

    /**
     * 处方药师审核通过后处理
     *
     * @param recipe
     * @return
     */
    @RpcService
    @LogRecord
    public RecipeResultBean afterCheckPassYs(Recipe recipe) {
        LOGGER.info("RecipeService afterCheckPassYs recipe:{}", JSON.toJSONString(recipe));
        if (null == recipe) {
            return null;
        }
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RemoteDrugEnterpriseService service = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);

        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Integer recipeId = recipe.getRecipeId();

        List<Recipedetail> recipedetailList = recipeDetailDAO.findByRecipeId(recipeId);

        RecipeOrder order = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        //正常平台处方
        if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(recipe.getFromflag())) {
            if (ReviewTypeConstant.Postposition_Check.equals(recipe.getReviewType())) {
                if (recipe.canMedicalPay()) {
                    //如果是可医保支付的单子，审核通过之后是变为待处理状态，需要用户支付完成才发往药企
                    RecipeServiceSub.sendRecipeTagToPatient(recipe, recipedetailList, null, true);
                    //向患者推送处方消息
                    RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_PASS);
                } else {
                    //date:20190920
                    //审核通过后设置订单的状态（后置）
                    PurchaseService purchaseService = ApplicationUtils.getRecipeService(PurchaseService.class);
                    Integer status = purchaseService.getOrderStatus(recipe);
                    orderService.updateOrderInfo(recipe.getOrderCode(), ImmutableMap.of("status", status), resultBean);
                    //发送患者审核完成消息
                    RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_PASS_YS);

                    //6.24 货到付款或者药店取药也走药师审核通过推送处方信息
                    // 平台处方发送药企处方信息
                    service.pushSingleRecipeInfo(recipeId);
                }
            }
            //date 2019/10/17
            //添加审核后的有库存无库存的消息平台逻辑
            //消息发送：平台处方且购药方式药店取药
            if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode()) && RecipeBussConstant.GIVEMODE_TFDS.equals(recipe.getGiveMode())) {
                //此处增加药店取药消息推送
                DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                if (recipe.getEnterpriseId() == null) {
                    LOGGER.info("审方后置-药店取药-药企为空");
                } else {
                    DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipe.getEnterpriseId());
                    EnterpriseStock enterpriseStock = stockBusinessService.enterpriseStockCheck(recipe, recipedetailList, drugsEnterprise.getId(), StockCheckSourceTypeEnum.DOCTOR_STOCK.getType());
                    boolean scanFlag = enterpriseStock.getStock();
                    LOGGER.info("AuditPostMode afterCheckPassYs scanFlag:{}.", scanFlag);
                    if (scanFlag) {
                        //表示需要进行库存校验并且有库存
                        RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_DRUG_HAVE_STOCK, recipe);
                    } else if (drugsEnterprise.getCheckInventoryFlag() == 2) {
                        //表示无库存但是药店可备货
                        RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_DRUG_NO_STOCK_READY, recipe);
                    }
                }
            }

            // 到院取药 审方后置 消息推送
            if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(recipe.getGiveMode())
                    && ReviewTypeConstant.Postposition_Check.equals(recipe.getReviewType())) {
                // 支付成功 到院取药 推送消息 审方后置
                RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_HOS_TAKE_MEDICINE, recipe);
            }

        } else if (RecipeBussConstant.FROMFLAG_HIS_USE.equals(recipe.getFromflag())) {
            Integer status = OrderStatusConstant.READY_SEND;
            if (RecipeBussConstant.GIVEMODE_TFDS.equals(recipe.getGiveMode())) {
                status = OrderStatusConstant.READY_GET_DRUG;
                // HOS处方发送药企处方信息
                service.pushSingleRecipeInfo(recipeId);
                //发送审核成功消息
                //${sendOrgan}：您的处方已审核通过，请于${expireDate}前到${pharmacyName}取药，地址：${addr}。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
                RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_YS_CHECKPASS_4TFDS, recipe);
            } else if (RecipeBussConstant.PAYMODE_ONLINE.equals(order.getPayMode())) {
                // HOS处方发送药企处方信息
                service.pushSingleRecipeInfo(recipeId);
                //发送审核成功消息
                //${sendOrgan}：您的处方已审核通过，我们将以最快的速度配送到：${addr}。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
                RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_YS_CHECKPASS_4STH, recipe);
            } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(recipe.getGiveMode()) && RecipeBussConstant.PAYMODE_OFFLINE.equals(order.getPayMode())) {
                status = OrderStatusConstant.READY_GET_DRUG;
            } else {
                status = OrderStatusConstant.READY_GET_DRUG;
                // HOS处方发送药企处方信息，由于是自由选择，所以匹配到的药企都发送一遍
                RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
                List<DrugsEnterprise> depList = recipeService.findSupportDepList(Lists.newArrayList(recipeId), recipe.getClinicOrgan(), null, false, null);
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
        // 病历处方-状态修改成显示
//        try {
//            DocIndexClient docIndexClient = AppContextHolder.getBean("docIndexClient", DocIndexClient.class);
//            docIndexClient.updateStatusByBussIdBussType(recipe.getRecipeId(), DocIndexShowEnum.SHOW.getCode());
//        } catch (Exception e) {
//            LOGGER.error("afterCheckPassYs recipeId:{} error", recipe.getRecipeId(), e);
//        }
        StateManager stateManager = AppContextHolder.getBean("stateManager", StateManager.class);
        if (ReviewTypeConstant.Postposition_Check.equals(recipe.getReviewType())) {
            RecipeStateEnum processStateDispensing = RecipeStateEnum.PROCESS_STATE_MEDICINE;
            RecipeStateEnum subOrderDeliveredMedicine = RecipeStateEnum.SUB_ORDER_TAKE_MEDICINE;
            OrderStateEnum processStateOrder = OrderStateEnum.PROCESS_STATE_ORDER;
            OrderStateEnum subOrderTakeMedicine = OrderStateEnum.SUB_ORDER_TAKE_MEDICINE;
            if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode())) {
                processStateDispensing = RecipeStateEnum.PROCESS_STATE_DISPENSING;
                subOrderDeliveredMedicine = RecipeStateEnum.SUB_ORDER_DELIVERED_MEDICINE;
                subOrderTakeMedicine = OrderStateEnum.SUB_ORDER_DELIVERED_MEDICINE;
            }
            stateManager.updateOrderState(order.getOrderId(), processStateOrder, subOrderTakeMedicine);
            stateManager.updateRecipeState(recipeId, processStateDispensing, subOrderDeliveredMedicine);

            if (Objects.nonNull(recipe.getClinicId()) && RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource())) {
                IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);
                RevisitBean revisitBean = iRevisitService.getById(recipe.getClinicId());
                if (revisitBean != null && REVISIT_STATUS_IN.equals(revisitBean.getStatus())) {
                    Buss2SessionProducer.sendMsgToMq(recipe, "recipeCheckPass", revisitBean.getSessionID());
                }
            }
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
        LOGGER.info("afterCheckNotPassYs recipeId= {}", recipe.getRecipeId());
        stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_AUDIT_NOT_PASS);
        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
        boolean effective = orderDAO.isEffectiveOrder(recipe.getOrderCode());
        //是否是有效订单
        if (!effective) {
            return;
        }
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        //相应订单处理
        orderService.cancelOrderByRecipeId(recipe.getRecipeId(), OrderStatusConstant.CANCEL_NOT_PASS, false);
        RecipeOrder order = orderDAO.getByOrderCode(recipe.getOrderCode());
        if (Objects.nonNull(order)) {
            stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_AUDIT_NOT_PASS);
        }
        //根据付款方式提示不同消息
        //date 2019/10/14
        //逻辑修改成，退款的不筛选支付方式
        if (PayConstant.PAY_FLAG_PAY_SUCCESS == recipe.getPayFlag()) {
            //线上支付
            //微信退款
            wxPayRefundForRecipe(2, recipe.getRecipeId(), null);
        }
        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode()) && RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource()) && recipe.getClinicId() != null) {
            IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);
            RevisitBean revisitBean = iRevisitService.getById(recipe.getClinicId());
            if (revisitBean != null && REVISIT_STATUS_IN.equals(revisitBean.getStatus())) {
                Buss2SessionProducer.sendMsgToMq(recipe, "recipeCheckNotPass", revisitBean.getSessionID());
            }
        }
    }

    /**
     * 医院处方审核 (当前为自动审核通过)
     *
     * @param recipeId
     * @return HashMap<String, Object>
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
        LOGGER.info("findRecipeAndDetailById recipeId = {}", recipeId);
        try {
            Map<String, Object> result = getRecipeAndDetailByIdImpl(recipeId, true, null);
            // 智能预审临时操作 bug 79725 【实施】【嘉定区中心医院】【B】【BUG】自动审方结果跟问题说明产生矛盾
//            result.remove("medicines");
            PatientDTO patient = (PatientDTO) result.get("patient");
            result.put("patient", ObjectCopyUtils.convert(patient, PatientVO.class));
            RecipeBean recipeBean = (RecipeBean) result.get("recipe");
            Recipe recipe = ObjectCopyUtils.convert(recipeBean, Recipe.class);
            EmrRecipeManager.getMedicalInfo(recipe, (RecipeExtend) result.get("recipeExtend"));
            return result;
        } catch (Exception e) {
            LOGGER.error("findRecipeAndDetailById is error ", e);
            throw new DAOException(recipe.constant.ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 获取智能审方结果详情
     *
     * @param recipeId 处方ID
     * @return
     */
    @RpcService
    public List<AuditMedicinesBean> getAuditMedicineIssuesByRecipeId(int recipeId) {
        return recipeAuditClient.getAuditMedicineIssuesByRecipeId(recipeId);
    }

    /**
     * 处方撤销方法(供医生端使用)---无撤销原因时调用保留为了兼容---新方法在RecipeCancelService里
     *
     * @param recipeId 处方Id
     * @return Map<String, Object>
     * 撤销成功返回 {"result":true,"msg":"处方撤销成功"}
     * 撤销失败返回 {"result":false,"msg":"失败原因"}
     */
    @RpcService
    @Deprecated
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

    public long timeDifference(String date) throws ParseException {
        SimpleDateFormat myFmt2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date1 = myFmt2.parse(date);
        String date2 = myFmt2.format(new Date());
        Date date3 = myFmt2.parse(date2);
        long diff = date3.getTime() - date1.getTime();
        long minutes = diff / (1000 * 60);
        return minutes;
    }

    /**
     * 检测机构推送药品数据
     *
     * @param organDrugs
     * @return
     */
    private HisResponseTO checkOrganDrugs(List<OrganDrugInfoTO> organDrugs) {
        HisResponseTO hisResponseTO = new HisResponseTO();
        for (OrganDrugInfoTO organDrug : organDrugs) {
            if (ObjectUtils.isEmpty(organDrug.getStatus())) {
                hisResponseTO.setMsgCode("-1");
                hisResponseTO.setMsg("存在推送数据 药品状态 未给出!");
                return hisResponseTO;
            }
        }
        hisResponseTO.setMsgCode("1");
        return hisResponseTO;
    }

    private List<String> checkOrganDrugInfoTO(OrganDrugInfoTO organDrugChange) {
        List<String> list = Lists.newArrayList();
        if (StringUtils.isEmpty(organDrugChange.getOrganDrugCode())) {
            list.add("organDrugCode(药品编号)");
        }
        if (StringUtils.isEmpty(organDrugChange.getDrugName())) {
            list.add("drugName(药品通用名)");
        }
        if (organDrugChange.getStatus() == 1) {
            if (StringUtils.isEmpty(organDrugChange.getDrugSpec())) {
                list.add("drugSpec(药品规格)");
            }
            if (ObjectUtils.isEmpty(organDrugChange.getPack())) {
                list.add("pack(转换系数)");
            }
            if (StringUtils.isEmpty(organDrugChange.getUnit())) {
                list.add("unit(药品单位)");
            }
            if (StringUtils.isEmpty(organDrugChange.getProducer())) {
                list.add("producer(生产厂家 名称)");
            }
            if (StringUtils.isEmpty(organDrugChange.getDrugform())) {
                list.add("drugform(剂型)");
            }
            if (ObjectUtils.isEmpty(organDrugChange.getDrugType())) {
                list.add("drugType(药品类型)");
            }
            if (ObjectUtils.isEmpty(organDrugChange.getUseDose())) {
                list.add("useDose(单次剂量)");
            }
            /*if (ObjectUtils.isEmpty(organDrugChange.getBaseDrug())) {
                list.add("baseDrug(是否基药)");
            }*/
            if (ObjectUtils.isEmpty(organDrugChange.getUseDoseUnit())) {
                list.add("useDoseUnit(剂量单位)");
            }
            if (ObjectUtils.isEmpty(organDrugChange.getPrice())) {
                list.add("price(药品单价)");
            }
            if (ObjectUtils.isEmpty(organDrugChange.getDrugManfCode())) {
                list.add("drugManfCode(药品产地编码)");
            }
        }

        return list;
    }

    /**
     * 机构推送药品调用方法 his调用
     *
     * @param organDrugs
     * @return
     */
    @RpcService(timeout = 6000)
    public HisResponseTO syncOrganDrug(List<OrganDrugInfoTO> organDrugs, Integer organId) {
        try {
            if (CollectionUtils.isNotEmpty(organDrugs)) {
                List<List<OrganDrugInfoTO>> partition = Lists.partition(organDrugs, 500);
                for (int i = 0; i < partition.size(); i++) {
                    LOGGER.info("syncOrganDrug" + organId + "data-" + i + "={}", JSONUtils.toString(partition.get(i)));
                }
            }
        } catch (Exception e) {
            LOGGER.error("drugInfoSynMovement dataerror{} ", e);
        }

        HisResponseTO hisResponseTO = new HisResponseTO();
        if (ObjectUtils.isEmpty(organId)) {
            hisResponseTO.setMsgCode("-1");
            hisResponseTO.setMsg("机构ID为空!");
            return hisResponseTO;
        }
        if (ObjectUtils.isEmpty(organDrugs)) {
            hisResponseTO.setMsgCode("-1");
            hisResponseTO.setMsg("推送数据为空!");
            return hisResponseTO;
        }
        DrugOrganConfig byOrganId1 = ObjectCopyUtils.convert(drugBusinessService.getConfigByOrganId(organId), DrugOrganConfig.class);
        if (ObjectUtils.isEmpty(byOrganId1)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到机构配置!");
        }
        Boolean sync = byOrganId1.getEnableDrugSync();
        Boolean deletes = byOrganId1.getEnableDrugDelete();
        Integer dockingMode = byOrganId1.getDockingMode();
        if (ObjectUtils.isEmpty(dockingMode)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到同步模式配置!");
        }

        if (dockingMode != 2) {
            throw new DAOException(DAOException.VALUE_NEEDED, "同步模式 非【主动推送】 调用无效!");
        }
        if (!sync) {
            hisResponseTO.setMsgCode("-1");
            hisResponseTO.setMsg("请开启【药品目录是否支持接口同步】配置后，再尝试进行同步推送!");
            return hisResponseTO;
        }
        Boolean add = byOrganId1.getEnableDrugAdd();
        Boolean commit = byOrganId1.getEnableDrugSyncArtificial();
        if (ObjectUtils.isEmpty(add)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到药品同步 新增 配置!");
        }
        if (ObjectUtils.isEmpty(deletes)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到药品同步 删除 配置!");
        }
        if (ObjectUtils.isEmpty(commit)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到药品同步 匹配 配置!");
        }
        drugListMatchDAO.deleteByOrganIdAndStatus(organId);
        HisResponseTO checkOrganDrugs = checkOrganDrugs(organDrugs);
        if ("-1".equals(checkOrganDrugs.getMsgCode())) {
            return checkOrganDrugs;
        }
        List<String> msg = Lists.newArrayList();
        for (OrganDrugInfoTO organDrug : organDrugs) {
            LOGGER.info("syncOrganDrug推送药品信息" + organId + "{}", JSONUtils.toString(organDrug));
            List<String> check = checkOrganDrugInfoTO(organDrug);
            if (!ObjectUtils.isEmpty(check)) {
                LOGGER.info("updateOrSaveOrganDrug 当前新增药品信息,信息缺失{}", JSONUtils.toString(check));
                msg.add("编码" + organDrug.getOrganDrugCode() + " 推送药品 " + organDrug.getDrugName() + " 信息缺失(包括:" + check.toString() + "),无法操作! ");
                continue;
            }
            switch (organDrug.getStatus()) {
                case 0:
                    LOGGER.info("syncOrganDrug机构药品数据推送 删除" + organDrug.getDrugName() + " organId=[{}] drug=[{}]", organId, JSONUtils.toString(organDrug));
                    OrganDrugList delete = organDrugListDAO.getByOrganIdAndOrganDrugCode(organId, organDrug.getOrganDrugCode());
                    if (ObjectUtils.isEmpty(delete)) {
                        msg.add(organDrug.getOrganDrugCode() + ":机构未找到该编码药品 " + organDrug.getDrugName() + " !");
                        continue;
                    }
                    try {
                        if (deletes) {
                            boolean isAllow = drugManager.isAllowDealBySyncDataRange(organDrug.getOrganDrugCode(), byOrganId1.getDelDrugDataRange(), byOrganId1.getDelSyncDrugType(), byOrganId1.getDelDrugFromList(), organDrug.getDrugType(), organDrug.getDrugform());
                            if (isAllow) {
                                organDrugListService.updateOrganDrugListStatusByIdSync(organId, delete.getOrganDrugId());
                            }
                        }
                    } catch (Exception e) {
                        DataSyncDTO dataSyncDTO = convertDataSyn(organDrug, organId, 4, e, 3, null);
                        List<DataSyncDTO> syncDTOList = Lists.newArrayList();
                        syncDTOList.add(dataSyncDTO);
                        dataSyncLogService.addDataSyncLog("1", syncDTOList);
                        LOGGER.info("syncOrganDrug机构药品数据推送 删除失败,{}", JSONUtils.toString(organDrug) + "Exception:{}" + e);
                        msg.add("编码" + organDrug.getOrganDrugCode() + " 推送药品 " + organDrug.getDrugName() + "药品数据推送 【删除失败】 !");
                        continue;
                    }
                    break;
                case 1:
                    OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCode(organId, organDrug.getOrganDrugCode());
                    if (ObjectUtils.isEmpty(organDrugList)) {
                        LOGGER.info("syncOrganDrug机构药品数据推送 新增" + organDrug.getDrugName() + " organId=[{}] drug=[{}]", organId, JSONUtils.toString(organDrug));
                        try {
                            if (add) {
                                boolean isAllow = drugManager.isAllowDealBySyncDataRange(organDrug.getOrganDrugCode(), byOrganId1.getAddDrugDataRange(), byOrganId1.getAddSyncDrugType(), byOrganId1.getAddDrugFromList(), organDrug.getDrugType(), organDrug.getDrugform());
                                if (isAllow) {
                                    addHisDrug(organDrug, organId, "推送");
                                    if (commit != null && !commit) {
                                        drugToolService.drugCommitT(null, organId);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            DataSyncDTO dataSyncDTO = convertDataSyn(organDrug, organId, 3, e, 1, null);
                            List<DataSyncDTO> syncDTOList = Lists.newArrayList();
                            syncDTOList.add(dataSyncDTO);
                            dataSyncLogService.addDataSyncLog("1", syncDTOList);
                            LOGGER.info("syncOrganDrug机构药品数据推送新增失败,{}", JSONUtils.toString(organDrug) + "Exception:{}" + e);
                            msg.add("编码" + organDrug.getOrganDrugCode() + " 推送药品 " + organDrug.getDrugName() + "药品数据推送 【新增失败】 !");
                            continue;
                        }
                    } else {
                        LOGGER.info("syncOrganDrug机构药品数据推送 更新" + organDrug.getDrugName() + " organId=[{}] drug=[{}]", organId, JSONUtils.toString(organDrug));
                        try {
                            boolean isAllow = drugManager.isAllowDealBySyncDataRange(organDrug.getOrganDrugCode(), byOrganId1.getUpdateDrugDataRange(), byOrganId1.getUpdateSyncDrugType(), byOrganId1.getUpdateDrugFromList(), organDrug.getDrugType(), organDrug.getDrugform());
                            if (isAllow) {
                                updateHisOrganDrug(organDrug, organDrugList, organId);
                            }

                        } catch (Exception e) {
                            DataSyncDTO dataSyncDTO = convertDataSyn(organDrug, organId, 3, e, 2, null);
                            List<DataSyncDTO> syncDTOList = Lists.newArrayList();
                            syncDTOList.add(dataSyncDTO);
                            dataSyncLogService.addDataSyncLog("1", syncDTOList);
                            LOGGER.info("syncOrganDrug机构药品数据推送 修改失败,{}", JSONUtils.toString(organDrug) + "Exception:{}" + e);
                            msg.add("编码" + organDrug.getOrganDrugCode() + " 推送药品 " + organDrug.getDrugName() + "药品数据推送 【更新失败】 !");
                            continue;
                        }
                    }
                    break;
                default:
                    break;
            }

        }
        if (!ObjectUtils.isEmpty(msg)) {
            hisResponseTO.setMsgCode("-1");
            hisResponseTO.setMsg(msg.toString());
            return hisResponseTO;
        }
        hisResponseTO.setMsgCode("200");
        hisResponseTO.setMsg("success");
        return hisResponseTO;
    }

    /**
     * 从缓存中实时获取同步情况
     *
     * @param organId
     * @return
     * @throws ParseException
     */
    @RpcService
    public Map<String, Object> getOrganDrugSyncData(Integer organId) throws ParseException {
        return (Map<String, Object>) redisClient.get(SyncDrugConstant.KEY_THE_DRUG_SYNC + organId.toString());
    }


    /**
     * 从缓存中删除异常同步情况
     *
     * @param organId
     * @return
     * @throws ParseException
     */
    @RpcService
    public void deleteOrganDrugSyncData(Integer organId) {
        redisClient.del(SyncDrugConstant.KEY_THE_DRUG_SYNC + organId.toString());
    }


    /**
     * 从缓存中删除异常同步情况
     *
     * @param organId
     * @return
     * @throws ParseException
     */
    @RpcService
    public Long getTimeByOrganId(Integer organId) throws ParseException {
        long minutes = 0L;
        Map<String, Object> hget = redisClient.get(SyncDrugConstant.KEY_THE_DRUG_SYNC + organId.toString());
        if (hget != null) {
            Integer status = (Integer) hget.get("Status");
            String date = (String) hget.get("Date");
            minutes = timeDifference(date);
        }
        return minutes;
    }


    public void addOrUpdateDrugInfoSynMovement(Integer organId, List<OrganDrugInfoTO> list, Integer way, String operator, Boolean commit) throws InterruptedException {
        if (list != null && list.size() > 0) {
            if (way == 1) {
                for (OrganDrugInfoTO organDrugInfoTO : list) {
                    try {
                        addHisDrug(organDrugInfoTO, organId, operator);
                        if (commit != null && !commit) {
                            drugToolService.drugCommitT(null, organId);
                            LOGGER.info("drugInfoSynMovement 自动提交完成,organID={}", organId);
                        }
                    } catch (Exception e) {
                        DataSyncDTO dataSyncDTO = convertDataSyn(organDrugInfoTO, organId, 3, e, 1, null);
                        List<DataSyncDTO> syncDTOList = Lists.newArrayList();
                        syncDTOList.add(dataSyncDTO);
                        dataSyncLogService.addDataSyncLog("1", syncDTOList);
                        LOGGER.info("drugInfoSynMovement 新增失败,{}", JSONUtils.toString(organDrugInfoTO) + "Exception:{}" + e);
                    }
                }
            } else if (way == 2) {
                for (OrganDrugInfoTO organDrugInfoTO : list) {
                    OrganDrugList byOrganIdAndOrganDrugCode = organDrugListDAO.getByOrganIdAndOrganDrugCode(organId, organDrugInfoTO.getOrganDrugCode());
                    try {
                        updateHisOrganDrug(organDrugInfoTO, byOrganIdAndOrganDrugCode, organId);
                    } catch (Exception e) {
                        DataSyncDTO dataSyncDTO = convertDataSyn(organDrugInfoTO, organId, 3, e, 2, null);
                        List<DataSyncDTO> syncDTOList = Lists.newArrayList();
                        syncDTOList.add(dataSyncDTO);
                        dataSyncLogService.addDataSyncLog("1", syncDTOList);
                        LOGGER.info("drugInfoSynMovement 修改失败,{}", JSONUtils.toString(organDrugInfoTO) + "Exception:{}" + e);
                    }
                }
            }
        }
    }


    public DataSyncDTO convertDataSyn(OrganDrugInfoTO drug, Integer organId, Integer status, Exception e, Integer operType, OrganDrugList organDrugList) {

        DataSyncDTO dataSyncDTO = new DataSyncDTO();
        dataSyncDTO.setType(1);
        dataSyncDTO.setOrganId(organId);
        if (ObjectUtils.isEmpty(drug)) {
            Map<String, Object> param = new HashedMap();
            BeanUtils.map(organDrugList, param);
            DrugListDAO dao = getDAO(DrugListDAO.class);
            if (!ObjectUtils.isEmpty(organDrugList.getDrugId())) {
                DrugList drugList = dao.get(organDrugList.getDrugId());
                param.put("drugType", drugList.getDrugType());
            }
            dataSyncDTO.setReqMsg(JSONUtils.toString(param));
        } else {
            dataSyncDTO.setReqMsg(JSONUtils.toString(drug));
        }
        dataSyncDTO.setStatus(status);
        if (e != null) {
            dataSyncDTO.setRespMsg(e.getMessage());
        } else {
            dataSyncDTO.setRespMsg("成功");
        }
        dataSyncDTO.setOperType(operType);
        dataSyncDTO.setSyncTime(new Date());

        return dataSyncDTO;
    }


    /**
     * 平台手动同步机构药品
     * 配置校验放到前端去做了
     *
     * @param organId
     * @param drugForms
     * @return
     */
    @LogRecord
    @RpcService(timeout = 600000)
    public Map<String, Object> drugInfoSynMovement(Integer organId, List<String> drugForms) throws ParseException {
        Map<String, Object> hget = redisClient.get(SyncDrugConstant.KEY_THE_DRUG_SYNC + organId.toString());
        if (hget != null) {
            Integer status = (Integer) hget.get("Status");
            String date = (String) hget.get("Date");
            long minutes = timeDifference(date);
            if (minutes < 10L) {
                throw new DAOException(DAOException.VALUE_NEEDED, "距离上次手动同步未超过10分钟，请稍后再尝试数据同步!");
            }
            if (status == 0) {
                throw new DAOException(DAOException.VALUE_NEEDED, "药品数据正在同步中，请耐心等待...");
            }
        }
        UserRoleToken urt = UserRoleToken.getCurrent();
        DrugOrganConfig byOrganId1 = ObjectCopyUtils.convert(drugBusinessService.getConfigByOrganId(organId), DrugOrganConfig.class);
        if (ObjectUtils.isEmpty(byOrganId1)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到机构配置!");
        }
        Boolean sync = byOrganId1.getEnableDrugSync();
        Boolean delete = byOrganId1.getEnableDrugDelete();
        Integer dockingMode = byOrganId1.getDockingMode();
//        Integer addDrugDataRange = byOrganId1.getAddDrugDataRange();
        Boolean add = byOrganId1.getEnableDrugAdd();
        Boolean update = byOrganId1.getEnableDrugUpdate();
        Boolean commit = byOrganId1.getEnableDrugSyncArtificial();
        if (ObjectUtils.isEmpty(dockingMode)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到同步模式配置!");
        }
        //前端去做
//        if (ObjectUtils.isEmpty(addDrugDataRange)) {
//            throw new DAOException(DAOException.VALUE_NEEDED, "未找到药品同步 数据范围 配置!");
//        }
        if (dockingMode == 2) {
            throw new DAOException(DAOException.VALUE_NEEDED, "同步模式 为【主动推送】 调用无效!");
        }
        if (!sync) {
            throw new DAOException(DAOException.VALUE_NEEDED, "请先确认接口对接已完成，且配置管理-机构配置-机构设置-业务设置-【药品目录是否支持接口同步】已开启，再尝试进行同步!");
        }
        if (ObjectUtils.isEmpty(add)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到药品同步 同步数据类型：新增 配置!");
        }
        if (ObjectUtils.isEmpty(update)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到药品同步 同步数据类型：更新  配置!");
        }
        if (ObjectUtils.isEmpty(delete)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到药品同步 同步数据类型：删除 配置!");
        }
        if (ObjectUtils.isEmpty(commit)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到药品同步 匹配 配置!");
        }
        //获取纳里机构药品目录
        List<OrganDrugList> details = organDrugListDAO.findOrganDrugByOrganId(organId);

        Map<String, OrganDrugList> drugMap = Maps.newHashMap();
        if (!CollectionUtils.isEmpty(details)) {
            drugMap = details.stream().collect(Collectors.toMap(OrganDrugList::getOrganDrugCode, a -> a, (k1, k2) -> k1));
        }
        LOGGER.info("drugInfoSynMovement剂型list organId=[{}] drugFromList=[{}]", organId, JSONUtils.toString(drugForms));
        //LOGGER.info("drugInfoSynMovement map organId=[{}] map=[{}]", organId, JSONUtils.toString(drugMap));
        return drugInfoSynHandle(organId, drugForms, drugMap, urt.getUserName(), sync, add, commit, delete, update);
    }

    /**
     * 平台手动同步
     *
     * @param organId
     * @param drugForms
     * @return
     */
    @LogRecord
    public Map<String, Object> drugInfoSynHandle(Integer organId, List<String> drugForms, Map<String, OrganDrugList> drugMap, String operator, Boolean sync, Boolean add, Boolean commit, Boolean delete, Boolean update) throws ParseException {
        SimpleDateFormat myFmt2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Map<String, Object> map = Maps.newHashMap();
        map.put("Date", myFmt2.format(new Date()));
        map.put("Status", 0);
        map.put("Exception", 0);
        redisClient.del(SyncDrugConstant.KEY_THE_DRUG_SYNC + organId.toString());
        redisClient.set(SyncDrugConstant.KEY_THE_DRUG_SYNC + organId.toString(), map);
        RecipeBusiThreadPool.submit(() -> {
            return drugInfoSynCore(organId, drugForms, drugMap, operator, sync, add, commit, delete, update);
        });
        return map;
    }

    /**
     * @param byOrganId1
     * @return
     */
    private OrganDrugInfoRequestTO obtainQueryOrganDrugInfoParam(DrugOrganConfig byOrganId1) {
        OrganDrugInfoRequestTO request = new OrganDrugInfoRequestTO();
        request.setOrganId(byOrganId1.getOrganId());
        //查询全部药品信息，返回的是医院所有有效的药品信息
        request.setData(Lists.newArrayList());
        request.setDrcode(Lists.newArrayList());
        request.setDrugItemCode(Lists.newArrayList());
        if (byOrganId1 != null) {
            request.setDrugDataSource(StringUtils.isEmpty(byOrganId1.getDrugDataSource()) ? "1" : byOrganId1.getDrugDataSource());
        }
        return request;
    }


    /**
     * 定时任务:每分钟调用一次 查询时间间隔内 机构同步配置
     */
    @RpcService(timeout = 6000000)
    @LogRecord
    public void drugInfoSynMovementDTask() {
        LocalTime localTime = LocalTime.now();
        LocalTime localTime1 = localTime.minusMinutes(1);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String format = localTime.format(dateTimeFormatter);
        String format1 = localTime1.format(dateTimeFormatter);
        Time etime = Time.valueOf(format);
        Time stime = Time.valueOf(format1);
        List<Integer> organIds = drugOrganConfigDao.findOrganIdByEnableDrugSyncAndTime(stime, etime);
        if (!ObjectUtils.isEmpty(organIds)) {
            for (Integer organId : organIds) {
                try {
                    drugInfoSynMovementD(organId, Lists.newArrayList());
                } catch (Exception e) {
                    LOGGER.info("drugInfoSynMovementDTask定时同步 organId=[{}],机构定时更新异常：exception=[{}].", organId, e);
                }
            }
        }
    }


    /**
     * 平台手动同步 (定时方法调用)
     *
     * @param organId
     * @param drugForms
     * @return
     */
    @RpcService(timeout = 6000000)
    public Map<String, Object> drugInfoSynMovementD(Integer organId, List<String> drugForms) throws ParseException {
        Map<String, Object> hget = redisClient.get(SyncDrugConstant.KEY_THE_DRUG_SYNC + organId.toString());
        if (hget != null) {
            Integer status = (Integer) hget.get("Status");
            String date = (String) hget.get("Date");
            long minutes = timeDifference(date);
            if (minutes < 10L) {
                throw new DAOException(DAOException.VALUE_NEEDED, "距离上次同步未超过10分钟，请稍后再尝试数据同步!");
            }
            if (status == 0) {
                throw new DAOException(DAOException.VALUE_NEEDED, "药品数据正在同步中，请耐心等待...");
            }
        }
        DrugOrganConfig byOrganId1 = ObjectCopyUtils.convert(drugBusinessService.getConfigByOrganId(organId), DrugOrganConfig.class);
        if (ObjectUtils.isEmpty(byOrganId1)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到机构配置!");
        }
        Boolean sync = byOrganId1.getEnableDrugSync();
        Boolean delete = byOrganId1.getEnableDrugDelete();
        Boolean update = byOrganId1.getEnableDrugUpdate();
        Integer dockingMode = byOrganId1.getDockingMode();
        if (ObjectUtils.isEmpty(dockingMode)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到同步模式配置!");
        }
        if (dockingMode == 2) {
            throw new DAOException(DAOException.VALUE_NEEDED, "同步模式 为【主动推送】 调用无效!");
        }
        if (!sync) {
            throw new DAOException(DAOException.VALUE_NEEDED, "请先确认接口对接已完成，且配置管理-机构配置-机构设置-业务设置-【药品目录是否支持接口同步】已开启，再尝试进行同步!");
        }
        Boolean add = byOrganId1.getEnableDrugAdd();
        Boolean commit = byOrganId1.getEnableDrugSyncArtificial();
        if (ObjectUtils.isEmpty(add)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到药品同步 新增 配置!");
        }
        if (ObjectUtils.isEmpty(commit)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到药品同步 匹配 配置!");
        }
        //获取纳里机构药品目录
        List<OrganDrugList> details = organDrugListDAO.findOrganDrugByOrganId(organId);

        Map<String, OrganDrugList> drugMap = Maps.newHashMap();
        if (!CollectionUtils.isEmpty(details)) {
            drugMap = details.stream().collect(Collectors.toMap(OrganDrugList::getOrganDrugCode, a -> a, (k1, k2) -> k1));
        }
        drugForms = Lists.newArrayList();
        LOGGER.info("定时drugInfoSynMovement剂型list organId=[{}] drugFromList=[{}]", organId, JSONUtils.toString(drugForms));
        return drugInfoSynCore(organId, null, drugMap, "定时任务", sync, add, commit, delete, update);
    }


    /**
     * 抽离公共核心方法--药品同步
     * 同步数据范围 1药品类型 2  药品剂型  默认1
     * 新增药品 勾选的药品类型是否包括当前药品类型 包括新增，不包括不新增
     * 更新字段 字段没被勾选上不更新，其余情况全部更新
     *
     * @param organId
     * @param drugForms
     * @param drugMap
     * @param operator
     * @param sync
     * @param add
     * @param commit
     * @param delete
     * @return
     * @throws ParseException
     */
    public Map<String, Object> drugInfoSynCore(Integer organId, List<String> drugForms, Map<String, OrganDrugList> drugMap, String operator, Boolean sync, Boolean add, Boolean commit, Boolean delete, Boolean update) throws ParseException {
        SimpleDateFormat myFmt2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Map<String, Object> map = Maps.newHashMap();
        DrugOrganConfig byOrganId1 = ObjectCopyUtils.convert(drugBusinessService.getConfigByOrganId(organId), DrugOrganConfig.class);
        long start = System.currentTimeMillis();

        IRecipeHisService recipeHisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        OrganDrugInfoResponseTO responseTO = new OrganDrugInfoResponseTO();
        try {
            OrganDrugInfoRequestTO request = obtainQueryOrganDrugInfoParam(byOrganId1);
            LOGGER.info("drugInfoSynMovement request={}", JSONUtils.toString(request));
            responseTO = recipeHisService.queryOrganDrugInfo(request);
        } catch (Exception e) {
            LOGGER.error("drugInfoSynMovement error{} ", e);
        }
        List<OrganDrugInfoTO> data = Lists.newArrayList();
        if (responseTO != null) {
            data = responseTO.getData();
            try {
                if (CollectionUtils.isNotEmpty(data)) {
                    List<List<OrganDrugInfoTO>> partition = Lists.partition(data, 1000);
                    for (int i = 0; i < partition.size(); i++) {
                        LOGGER.info("queryOrganDrugInfo response" + organId + "data-" + i + "={}", JSONUtils.toString(partition.get(i)));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("drugInfoSynMovement dataerror{} ", e);
            }
        }
        if (ObjectUtils.isEmpty(data)) {
            LOGGER.info("his查询药品数据为空 organId=[{}]", organId);
            map.put("Date", myFmt2.format(new Date()));
            map.put("Status", 2);
            map.put("Exception", 0);
            map.put("hisException", "his查询药品数据为空!");
            redisClient.del(SyncDrugConstant.KEY_THE_DRUG_SYNC + organId.toString());
            redisClient.set(SyncDrugConstant.KEY_THE_DRUG_SYNC + organId.toString(), map);
            return map;
        }
        //查询起始下标
        Long updateNum = 0L;
        Long addNum = 0L;
        Long deleteNum = 0L;
        int startIndex = 0;
        List<OrganDrugInfoTO> addList = Lists.newArrayList();
        List<OrganDrugInfoTO> updateList = Lists.newArrayList();
        boolean finishFlag = true;
        long total = data.size();
        if (sync) {
            ////TODO 这个条件没啥用吧？？ 没啥用以后有这块逻辑改动去掉吧
            while (finishFlag) {
                if (!CollectionUtils.isEmpty(data)) {
                    //循环机构药品 与平台机构药品对照 有则更新 无则新增到临时表
                    for (OrganDrugInfoTO drug : data) {
                        Integer status = drug.getStatus();
                        LOGGER.info("定时drugInfoSynMovementaddHisDrug前期" + drug.getDrugName() + " organId=[{}] drug=[{}]", organId, JSONUtils.toString(drug));
                        OrganDrugList organDrug = drugMap.get(drug.getOrganDrugCode());
                        if (null == organDrug && add) {
                            if (status != null && status == 0) {
                                startIndex++;
                                continue;
                            }
                            boolean isAllow = drugManager.isAllowDealBySyncDataRange(drug.getOrganDrugCode(), byOrganId1.getAddDrugDataRange(), byOrganId1.getAddSyncDrugType(), byOrganId1.getAddDrugFromList(), drug.getDrugType(), drug.getDrugform());
                            if (isAllow) {
                                List<DrugListMatch> dataByOrganDrugCode = drugListMatchDAO.findDataByOrganDrugCode(drug.getOrganDrugCode(), organId);
                                if (dataByOrganDrugCode != null && dataByOrganDrugCode.size() > 0) {
                                    for (DrugListMatch drugListMatch : dataByOrganDrugCode) {
                                        drugListMatchDAO.remove(drugListMatch.getDrugId());
                                    }
                                }
                                addList.add(drug);
                                addNum++;
                            }
                            startIndex++;
                            continue;
                        } else if (null != organDrug && update) {
                            boolean isAllow = drugManager.isAllowDealBySyncDataRange(organDrug.getOrganDrugCode(), byOrganId1.getUpdateDrugDataRange(), byOrganId1.getUpdateSyncDrugType(), byOrganId1.getUpdateDrugFromList(), drug.getDrugType(), drug.getDrugform());
                            if (isAllow) {
                                updateList.add(drug);
                                updateNum++;
                            }
                            LOGGER.info("drugInfoSynMovementupdateNum" + drug.getDrugName() + " organId=[{}] drug=[{}]", organId, JSONUtils.toString(drug));
                            startIndex++;
                            continue;
                        }
                        startIndex++;
                    }
                } else {
                    break;
                }
                if (startIndex >= total) {
                    //这个打印条数仅供参考
                    LOGGER.info("drugInfoSynMovement organId=[{}] 本次查询量：total=[{}] ,总更新量：update=[{}]，新增量：update=[{}]，药品信息更新结束.", organId, startIndex, updateNum, addNum);
                    finishFlag = false;
                }
            }
        }
        try {
            syncDrugExcDAO.deleteByOrganId(organId, 1);
            //根据机构id删除 临时表 已提交数据
            drugListMatchDAO.deleteByOrganIdAndStatus(organId);
            addOrUpdateDrugInfoSynMovement(organId, addList, 1, operator, commit);
            addOrUpdateDrugInfoSynMovement(organId, updateList, 2, operator, commit);
            //TODO ？？机构药品his没给就直接删掉 这先不动吧 当时约定的就这样吧
            if (!ObjectUtils.isEmpty(delete)) {
                if (delete) {
                    if (!CollectionUtils.isEmpty(data)) {
                        Map<String, OrganDrugInfoTO> collect = data.stream().collect(Collectors.toMap(OrganDrugInfoTO::getOrganDrugCode, a -> a, (k1, k2) -> k1));
                        List<OrganDrugList> details = organDrugListDAO.findOrganDrugByOrganIdAndAllStatus(organId);
                        if (!ObjectUtils.isEmpty(details)) {
                            for (OrganDrugList detail : details) {
                                boolean isAllow = drugManager.isAllowDealBySyncDataRange(detail.getOrganDrugCode(), byOrganId1.getDelDrugDataRange(), byOrganId1.getDelSyncDrugType(), byOrganId1.getDelDrugFromList(), drugListDAO.get(detail.getDrugId()).getDrugType(), detail.getDrugForm());
                                if (isAllow) {
                                    OrganDrugInfoTO organDrugInfoTO = collect.get(detail.getOrganDrugCode());
                                    if (ObjectUtils.isEmpty(organDrugInfoTO)) {
                                        try {
                                            organDrugListService.updateOrganDrugListStatusByIdSync(organId, detail.getOrganDrugId());
                                            deleteNum++;
                                        } catch (Exception e) {
                                            DataSyncDTO dataSyncDTO = convertDataSyn(ObjectCopyUtils.convert(detail, OrganDrugInfoTO.class), organId, 4, e, 3, null);
                                            List<DataSyncDTO> syncDTOList = Lists.newArrayList();
                                            syncDTOList.add(dataSyncDTO);
                                            dataSyncLogService.addDataSyncLog("1", syncDTOList);
                                            LOGGER.info("drugInfoSynMovement机构药品数据同步 删除失败,{}", JSONUtils.toString(detail) + "Exception:{}" + e);
                                            continue;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.info("定时drugInfoSynMovement list新增修改,", e);
        }
        map.put("addNum", addNum);
        map.put("updateNum", updateNum);
        map.put("deleteNum", deleteNum);
        map.put("Date", myFmt2.format(new Date()));
        map.put("Status", 1);
        List<SyncDrugExc> byOrganId = syncDrugExcDAO.findByOrganIdAndSyncType(organId, 1);
        if (!ObjectUtils.isEmpty(byOrganId)) {
            map.put("Exception", 1);
        }
        redisClient.del(SyncDrugConstant.KEY_THE_DRUG_SYNC + organId.toString());
        redisClient.set(SyncDrugConstant.KEY_THE_DRUG_SYNC + organId.toString(), map);
        //drugInfoSynTaskExt(organId);
        long elapsedTime = System.currentTimeMillis() - start;
        LOGGER.info("RecipeBusiThreadPool drugInfoSynMovementExt ES-推送药品 执行时间:{}.", elapsedTime);
        return map;
    }


    /**
     * 定时任务:同步HIS医院药品信息 每天凌晨1点同步
     */
    @RpcService(timeout = 600000)
    public void drugInfoSynTask() {
        drugInfoSynTaskExt(null);
    }

    @RpcService(timeout = 600000)
    public void drugInfoSynTaskExt(Integer organId) {
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        IOrganConfigService iOrganConfigService = ApplicationUtils.getBaseService(IOrganConfigService.class);
        syncDrugExcDAO.deleteByOrganId(organId, 2);
        List<Integer> organIds = new ArrayList<>();
        if (null == organId) {
            //查询 base_organconfig 表配置需要同步的机构
            organIds = drugOrganConfigDao.findEnableDrugSync();
        } else {
            organIds.add(organId);
        }

        if (CollectionUtils.isEmpty(organIds)) {
            LOGGER.info("drugInfoSynTask organIds is empty.");
            return;
        }

        OrganDrugListDAO organDrugListDAO = getDAO(OrganDrugListDAO.class);
        Long updateNum = 0L;

        for (int oid : organIds) {
            try {
                //获取纳里机构药品目录
                List<OrganDrugList> details = organDrugListDAO.findOrganDrugByOrganId(oid);
                if (CollectionUtils.isEmpty(details)) {
                    LOGGER.info("drugInfoSynTask 当前医院organId=[{}]，平台没有匹配到机构药品.", oid);
                    continue;
                }
                Map<String, OrganDrugList> drugMap = details.stream().collect(Collectors.toMap(OrganDrugList::getOrganDrugCode, a -> a, (k1, k2) -> k1));
                //查询起始下标
                int startIndex = 0;
                boolean finishFlag = true;
                long total = organDrugListDAO.getTotal(oid);
                while (finishFlag) {
                    List<DrugInfoTO> drugInfoList = hisService.getDrugInfoFromHis(oid, false, startIndex);
                    if (!CollectionUtils.isEmpty(drugInfoList)) {
                        //是否有效标志 1-有效 0-无效
                        for (DrugInfoTO drug : drugInfoList) {
                            OrganDrugList organDrug = drugMap.get(drug.getDrcode());
                            if (null == organDrug) {
                                continue;
                            }
                            updateHisDrug(drug, organDrug, oid);
                            updateNum++;
                            LOGGER.info("drugInfoSynTask organId=[{}] drug=[{}]", oid, JSONUtils.toString(drug));
                        }
                    }
                    startIndex++;
                    if (startIndex >= total) {
                        LOGGER.info("drugInfoSynTask organId=[{}] 本次查询量：total=[{}] ,总更新量：update=[{}]，药品信息更新结束.", oid, startIndex, updateNum);
                        finishFlag = false;
                    }
                }
            } catch (Exception e) {
                LOGGER.info("定时drugInfoSynTaskExt机构药品数据同步失败,{}", oid + "Exception:{}" + e);
            }
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
        StringBuilder memo = new StringBuilder();
        RecipeOrder order;
        //设置查询时间段
        String endDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(Integer.parseInt(cacheService.getParam(ParameterConstant.KEY_RECIPE_VALIDDATE_DAYS, RECIPE_EXPIRED_DAYS.toString()))), DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(Integer.parseInt(cacheService.getParam(ParameterConstant.KEY_RECIPE_CANCEL_DAYS, RECIPE_EXPIRED_SEARCH_DAYS.toString()))), DateConversion.DEFAULT_DATE_TIME);

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
                orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO, true);
                //变更处方状态
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.NO_OPERATOR, ImmutableMap.of("chooseFlag", 1));
                stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_TIMEOUT_NOT_ORDER);
                if (Objects.nonNull(order)) {
                    stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_OTHER);
                }
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
            //修改cdr_his_recipe status为已处理
            orderService.updateHisRecieStatus(recipes);
        }
        List<Integer> statusList = Arrays.asList(RecipeStatusConstant.NO_PAY, RecipeStatusConstant.NO_OPERATOR);
        for (Integer status : statusList) {
            // 2021失效时间可以配置需求，原定时任务查询增加失效时间为空条件
            List<Recipe> recipeList = recipeDAO.getRecipeListForCancelRecipe(status, startDt, endDt);
            LOGGER.info("cancelRecipeTask 状态=[{}], 取消数量 = [{}], 详情={}", status, recipeList.size(), JSONUtils.toString(recipeList));
            if (CollectionUtils.isNotEmpty(recipeList)) {
                for (Recipe recipe : recipeList) {
                    //过滤掉流转到扁鹊处方流转平台的处方
                    if (RecipeServiceSub.isBQEnterpriseBydepId(recipe.getEnterpriseId())) {
                        if (Objects.isNull(recipe.getOrderCode()) && Integer.valueOf("1").equals(recipe.getPushFlag())) {
                            recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_FINISH.getType());
                            recipe.setSubState(RecipeStateEnum.SUB_DONE_UPLOAD_THIRD.getType());
                            recipe.setProcessState(RecipeStateEnum.PROCESS_STATE_DONE.getType());
                            recipeDAO.update(recipe);
                        }
                        continue;
                    }
                    sendDrugEnterproseMsg(recipe);
                    memo.delete(0, memo.length());
                    int recipeId = recipe.getRecipeId();
                    //相应订单处理
                    order = orderDAO.getOrderByRecipeId(recipeId);
                    orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO, true);
                    // 超时未支付订单处理
                    if (Objects.nonNull(order)) {
                        stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_TIMEOUT_NON_PAYMENT);
                    }
                    if (recipe.getFromflag().equals(RecipeBussConstant.FROMFLAG_HIS_USE)) {
                        if (null != order) {
                            orderDAO.updateByOrdeCode(order.getOrderCode(), ImmutableMap.of("cancelReason", "患者未在规定时间内支付，该处方单已失效"));
                        }
                        //发送超时取消消息
                        //${sendOrgan}：抱歉，您的处方单由于超过${overtime}未处理，处方单已失效。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
                        RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_CANCEL_4HIS, recipe);
                    }

                    //变更处方状态
                    recipeDAO.updateRecipeInfoByRecipeId(recipeId, status, ImmutableMap.of("chooseFlag", 1));
                    stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_TIMEOUT_NOT_ORDER);
                    RecipeMsgService.batchSendMsg(recipe, status);
                    if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())) {
                        //药师首页待处理任务---取消未结束任务
                        ApplicationUtils.getBaseService(IAsynDoBussService.class).fireEvent(new BussCancelEvent(recipeId, BussTypeConstant.RECIPE));
                    }
                    if (RecipeStatusConstant.NO_PAY == status) {
                        memo.append("已取消,超过3天未支付");
                    } else if (RecipeStatusConstant.NO_OPERATOR == status) {
                        memo.append("已取消,超过3天未操作");
                    } else {
                        memo.append("未知状态:" + status);
                    }
                    if (RecipeStatusConstant.NO_PAY == status) {
                        //未支付，三天后自动取消后，优惠券自动释放
                        RecipeCouponService recipeCouponService = ApplicationUtils.getRecipeService(RecipeCouponService.class);
                        recipeCouponService.unuseCouponByRecipeId(recipeId);
                    }
                    //推送处方到监管平台
                    RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(Collections.singletonList(recipe.getRecipeId()), 1));
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
                //修改cdr_his_recipe status为已处理
                orderService.updateHisRecieStatus(recipeList);
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
        List<Integer> statusList = Arrays.asList(RecipeStatusConstant.PATIENT_NO_OPERATOR, RecipeStatusConstant.PATIENT_NO_PAY, RecipeStatusConstant.PATIENT_NODRUG_REMIND);
        Date now = DateTime.now().toDate();
        //设置查询时间段
        String endDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(1), DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(Integer.parseInt(cacheService.getParam(ParameterConstant.KEY_RECIPE_VALIDDATE_DAYS, RECIPE_EXPIRED_DAYS.toString()))), DateConversion.DEFAULT_DATE_TIME);
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
     * 定时任务:根据失效时间取消处方单-每半个小时执行一次
     */
    @RpcService
    public void cancelRecipeTaskByInvalidTime() {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        //设置查询时间段：失效时间-当前时间往前推3天
        String startTime = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(3), DateConversion.DEFAULT_DATE_TIME);
        String endTime = DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME);

        List<Recipe> recipeList = recipeDAO.getInvalidRecipeListByInvalidTime(startTime, endTime);
        LOGGER.info("cancelRecipeTaskByInvalidTime 取消数量=[{}], 详情={}", recipeList.size(), JSONUtils.toString(recipeList));
        doRecipeCancelByInvalidTime(recipeList);


    }

    public static void doRecipeCancelByInvalidTime(List<Recipe> recipeList) {
        try {
            if (CollectionUtils.isNotEmpty(recipeList)) {
                RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
                StateManager stateManager = AppContextHolder.getBean("stateManager", StateManager.class);
                RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
                RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
                HisRecipeDAO hisRecipeDAO = getDAO(HisRecipeDAO.class);
                RecipeBeforeOrderDAO recipeBeforeOrderDAO = getDAO(RecipeBeforeOrderDAO.class);
                StringBuilder memo = new StringBuilder();
                RecipeOrder order;
                List<Integer> recipeIds = new ArrayList<>();
                for (Recipe recipe : recipeList) {
                    try {
                        Boolean lock = RedisClient.instance().setIfAbsentAndExpire(RecipeSystemConstant.RECIPE_INVALID_LOCK_KEY + recipe.getRecipeId(), recipe.getRecipeId(), RecipeSystemConstant.RECIPE_INVALID_LOCK_TIMEOUT);
                        LOGGER.info("处方失效获取redis锁结果-lock={},recipeId={}", lock, recipe.getRecipeId());
                        if (!lock) {
                            continue;
                        }
                        //过滤掉流转到扁鹊处方流转平台的处方
                        if (RecipeServiceSub.isBQEnterpriseBydepId(recipe.getEnterpriseId())) {
                            if (Objects.isNull(recipe.getOrderCode()) && Integer.valueOf("1").equals(recipe.getPushFlag())) {
                                recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_FINISH.getType());
                                recipe.setSubState(RecipeStateEnum.SUB_DONE_UPLOAD_THIRD.getType());
                                recipe.setProcessState(RecipeStateEnum.PROCESS_STATE_DONE.getType());
                                recipeDAO.update(recipe);
                            }
                            continue;
                        }
                        Integer status = RecipeService.getStatus(recipe);
                        if (status == null || (RecipeStatusConstant.NO_PAY != status && RecipeStatusConstant.NO_OPERATOR != status && RecipeStatusConstant.RECIPE_ORDER_CACEL != status)) {
                            continue;
                        }
                        sendDrugEnterproseMsg(recipe);
                        memo.delete(0, memo.length());
                        int recipeId = recipe.getRecipeId();
                        //相应订单处理
                        order = orderDAO.getOrderByRecipeId(recipeId);
                        orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO, true);
                        if (Objects.nonNull(order)) {
                            stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_OTHER);
                        }
                        // 邵逸夫模式下 需要查询有无支付审方费
                        if (null != order) {
                            IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
                            Object invalidInfoObject = configurationService.getConfiguration(order.getOrganId(), "syfPayMode");
                            Boolean syfPayMode = (Boolean) invalidInfoObject;
                            if (syfPayMode) {
                                // 查询是否有流水
                                RecipeOrderPayFlowDao recipeOrderPayFlowDao = ApplicationUtils.getRecipeService(RecipeOrderPayFlowDao.class);
                                List<RecipeOrderPayFlow> byOrderId = recipeOrderPayFlowDao.findByOrderId(order.getOrderId());
                                // 退费
                                if (CollectionUtils.isNotEmpty(byOrderId)) {
                                    PayClient payClient = ApplicationUtils.getRecipeService(PayClient.class);
                                    payClient.refund(order.getOrderId(), PayBusTypeEnum.OTHER_BUS_TYPE.getName());
                                }

                            }
                        }
                        if (recipe.getFromflag().equals(RecipeBussConstant.FROMFLAG_HIS_USE)) {
                            if (null != order) {
                                orderDAO.updateByOrdeCode(order.getOrderCode(), ImmutableMap.of("cancelReason", "患者未在规定时间内支付，该处方单已失效"));
                            }
                            //发送超时取消消息
                            //${sendOrgan}：抱歉，您的处方单由于超过${overtime}未处理，处方单已失效。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
                            RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_CANCEL_4HIS, recipe);
                        }
                        //变更处方状态 RECIPE_ORDER_CACEL按NO_OPERATOR处理
                        Integer updateStatus = status == RecipeStatusConstant.RECIPE_ORDER_CACEL ? RecipeStatusConstant.NO_OPERATOR : status;
                        recipeDAO.updateRecipeInfoByRecipeId(recipeId, updateStatus, ImmutableMap.of("chooseFlag", 1));
                        stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_TIMEOUT_NOT_ORDER);
                        if (new Integer("2").equals(recipe.getRecipeSourceType()) && new Integer(RecipeStatusConstant.NO_OPERATOR).equals(updateStatus)) {
                            hisRecipeDAO.updateHisRecieStatus(recipe.getClinicOrgan(), recipe.getRecipeCode(), HisRecipeConstant.HISRECIPESTATUS_EXPIRED);
                        }
                        RecipeMsgService.batchSendMsg(recipe, status);
                        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())) {
                            //药师首页待处理任务---取消未结束任务
                            ApplicationUtils.getBaseService(IAsynDoBussService.class).fireEvent(new BussCancelEvent(recipeId, BussTypeConstant.RECIPE));
                        }
                        if (RecipeStatusConstant.NO_PAY == status) {
                            memo.append("已取消,超过失效时间未支付");
                        } else if (RecipeStatusConstant.NO_OPERATOR == status || RecipeStatusConstant.RECIPE_ORDER_CACEL == status) {
                            memo.append("已取消,超过失效时间未操作");
                        } else {
                            memo.append("未知状态:" + status);
                        }
                        if (RecipeStatusConstant.NO_PAY == status) {
                            //未支付，三天后自动取消后，优惠券自动释放
                            RecipeCouponService recipeCouponService = ApplicationUtils.getRecipeService(RecipeCouponService.class);
                            recipeCouponService.unuseCouponByRecipeId(recipeId);
                        }
                        //推送处方到监管平台的处方
                        recipeIds.add(recipeId);
                        //HIS消息发送
                        boolean succFlag = hisService.recipeStatusUpdate(recipeId);
                        if (succFlag) {
                            memo.append(",HIS推送成功");
                        } else {
                            memo.append(",HIS推送失败");
                        }
                        //保存处方状态变更日志
                        RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS, updateStatus, memo.toString());


                    } catch (Exception e) {
                        LOGGER.error("根据失效时间处理到期处方异常，处方={}", JSONObject.toJSONString(recipeList), e);
                    } finally {
                        RedisClient.instance().del(RecipeSystemConstant.RECIPE_INVALID_LOCK_KEY + recipe.getRecipeId());
                    }
                }
                //修改cdr_his_recipe status为已处理
                orderService.updateHisRecieStatus(recipeList);
                RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(recipeIds, 1));
                // 删除预下单信息
                if (CollectionUtils.isNotEmpty(recipeIds)) {
                    recipeBeforeOrderDAO.updateDeleteFlagByRecipeId(recipeIds);
                }
            }
        } catch (Exception e) {
            LOGGER.error("doRecipeCancelByInvalidTime error", e);
        }
    }

    //向药企推送处方过期的通知
    public static void sendDrugEnterproseMsg(Recipe recipe) {
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
            OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
            List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
            for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                if ("aldyf".equals(drugsEnterprise.getCallSys()) || ("tmdyf".equals(drugsEnterprise.getCallSys()) && recipe.getPushFlag() == 1)) {
                    //向药企推送处方过期的通知
                    try {
                        AccessDrugEnterpriseService remoteService = RemoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
                        DrugEnterpriseResult drugEnterpriseResult = remoteService.updatePrescriptionStatus(recipe.getRecipeCode(), AlDyfRecipeStatusConstant.EXPIRE);
                        LOGGER.info("向药企推送处方过期通知,{}", JSONUtils.toString(drugEnterpriseResult));
                    } catch (Exception e) {
                        LOGGER.info("向药企推送处方过期通知有问题{}", recipe.getRecipeId(), e);
                    }
                }

            }
        }
    }

    /**
     * 获取处方状态：未支付/未操作状态
     *
     * @param recipe
     * @return
     */
    public static Integer getStatus(Recipe recipe) {
        Integer fromFlag = recipe.getFromflag();
        Integer dbStatus = recipe.getStatus();
        Integer payFlag = recipe.getPayFlag();
//        Integer payMode = recipe.getPayMode();
        String orderCode = recipe.getOrderCode();
        //处方状态未支付： fromflag in (1,2) and status =" + RecipeStatusConstant.CHECK_PASS + " and payFlag=0 and payMode is not null and orderCode is not null
        Integer status = null;
        if ((fromFlag != null && (fromFlag == 1 || fromFlag == 2)) && dbStatus != null && dbStatus == RecipeStatusConstant.CHECK_PASS && payFlag != null && payFlag == 0 && StringUtils.isNotBlank(orderCode)) {
            status = RecipeStatusConstant.NO_PAY;
        }
        //处方状态未操作：fromflag = 1 and status =" + RecipeStatusConstant.CHECK_PASS + " and payMode is null or ( status in (8,24) and reviewType = 1)
        if ((fromFlag != null && fromFlag == 1) && dbStatus != null && dbStatus == RecipeStatusConstant.CHECK_PASS && StringUtils.isBlank(orderCode)) {
            status = RecipeStatusConstant.NO_OPERATOR;
        }
        if (recipe.getReviewType() != null && recipe.getReviewType() == 1 && (dbStatus != null && (dbStatus == 8 || dbStatus == 24))) {
            status = RecipeStatusConstant.NO_OPERATOR;
        }
        // 到店取药且到店支付:status in (7,8) and giveMode = 3 and payMode = 4
        if ((dbStatus != null && (dbStatus == 7 || dbStatus == 8)) && (recipe.getGiveMode() != null && recipe.getGiveMode() == 3) && StringUtils.isNotBlank(orderCode)) {
            RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
            RecipeOrder order = orderDAO.getByOrderCode(orderCode);
            // status not in (6,7,8) and drugStoreCode is not null
            if (order != null && StringUtils.isNotBlank(order.getDrugStoreCode()) && (order.getPayMode() != null && order.getPayMode() == 4)
                    && (order.getStatus() != null && order.getStatus() != 6 && order.getStatus() != 7 && order.getStatus() != 8)) {
                status = RecipeStatusConstant.RECIPE_ORDER_CACEL;
            }
        }
        return status;
    }

    /**
     * 定时任务: 查询过期的药师审核不通过，需要医生二次确认的处方
     * 查询规则: 药师审核不通过时间点的 2天前-1月前这段时间内，医生未处理的处方单
     * <p>
     * //date 2019/10/14
     * //修改规则：处方开放时间，时间点的 3天前-1月前这段时间内，医生未处理的处方单
     */
    @RpcService
    public void afterCheckNotPassYsTask() {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);

        //将一次审方不通过的处方，搁置的设置成审核不通过
        String endDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(3), DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(RECIPE_EXPIRED_SECTION), DateConversion.DEFAULT_DATE_TIME);
        //根据条件查询出来的数据都是需要主动退款的
        List<Recipe> list = recipeDAO.findFirstCheckNoPass(startDt, endDt);
        LOGGER.info("afterCheckNotPassYsTask 处理数量=[{}], 详情={}", list.size(), JSONUtils.toString(list));
        Map<String, Object> updateMap = new HashMap<>();
        for (Recipe recipe : list) {
            //判断处方是否有关联订单，
            if (null != recipe.getOrderCode()) {
                // 关联：修改处方一次审核不通过的标志位，并且把订单的状态审核成审核不通过
                //更新处方标志位
                updateMap.put("checkStatus", RecipecCheckStatusConstant.Check_Normal);
                recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), updateMap);
                //更新订单的状态，退款
                afterCheckNotPassYs(recipe);
            } else {
                // 不关联：修改处方一次审核不通过的标志位
                //更新处方标志位
                updateMap.put("checkStatus", RecipecCheckStatusConstant.Check_Normal);
                recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), updateMap);
            }
            //发消息(审核不通过的)
            //添加发送不通过消息
            RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_NOT_PASSYS_REACHPAY);
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
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(Integer.parseInt(cacheService.getParam(ParameterConstant.KEY_RECIPE_VALIDDATE_DAYS, RECIPE_EXPIRED_DAYS.toString()))), DateConversion.DEFAULT_DATE_TIME);
        String endDt = DateConversion.getDateFormatter(DateTime.now().toDate(), DateConversion.DEFAULT_DATE_TIME);
        Map<Integer, List<String>> map = Maps.newHashMap();
        List<Recipe> list = recipeDAO.getRecipeStatusFromHis(startDt, endDt);
        LOGGER.info("getRecipeStatusFromHis 需要同步HIS处方，数量=[{}]", (null == list) ? 0 : list.size());
        assembleQueryStatusFromHis(list, map);
        LOGGER.info("getRecipeStatusFromHis 需要同步HIS处方，机构数量=[{}]", map.keySet().size());
        RecipeBusiThreadPool.execute(new UpdateRecipeStatusFromHisCallable(map));
    }

    /**
     * 自动审核通过情况
     *
     * @param result
     * @throws Exception
     */
    public void autoPassForCheckYs(CheckYsInfoBean result) throws Exception {
        //武昌项目用到--自动审核通过不走现在的审方逻辑
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(result.getRecipeId());
        int recipeStatus = auditModeContext.getAuditModes(recipe.getReviewType()).afterAuditRecipeChange();
        recipeDAO.updateRecipeInfoByRecipeId(result.getRecipeId(), recipeStatus, null);
        LOGGER.info("autoPassForCheckYs recipeId={};status={}", result.getRecipeId(), recipeStatus);
        auditModeContext.getAuditModes(recipe.getReviewType()).afterCheckPassYs(recipe);
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
        String endDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(3), DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(RECIPE_EXPIRED_SECTION), DateConversion.DEFAULT_DATE_TIME);

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
        Map<String, Object> result = getRecipeAndDetailByIdImpl(recipeId, false, null);
        PatientDTO patient = (PatientDTO) result.get("patient");
        result.put("patient", ObjectCopyUtils.convert(patient, PatientDS.class));
        return result;
    }

    /**
     * 健康端获取处方详情
     *
     * @param recipeId
     * @return
     */
    public Map<String, Object> getPatientRecipeByIdForOfflineRecipe(int recipeId) {
        Map<String, Object> result = getRecipeAndDetailByIdImpl(recipeId, false, null);
        PatientDTO patient = (PatientDTO) result.get("patient");
        result.put("patient", ObjectCopyUtils.convert(patient, PatientDS.class));
        return result;
    }

    /**
     * 健康端获取处方详情-----合并处方
     *
     * @param ext       没用
     * @param recipeIds 处方ID列表
     */
    @RpcService
    public List<Map<String, Object>> findPatientRecipesByIds(Integer ext, List<Integer> recipeIds) {
        Collections.sort(recipeIds, Collections.reverseOrder());
        LOGGER.info("findPatientRecipesByIds recipeIds:{}", JSONUtils.toString(recipeIds));
        //把处方对象返回给前端--合并处方--原确认订单页面的处方详情是通过getPatientRecipeById获取的
        if (CollectionUtils.isNotEmpty(recipeIds)) {
            List<Map<String, Object>> recipeInfos = new ArrayList<>(recipeIds.size());
            for (Integer recipeId : recipeIds) {
                recipeInfos.add(getRecipeAndDetailByIdImpl(recipeId, false, null));
            }
            LOGGER.info("findPatientRecipesByIds response:{}", JSONUtils.toString(recipeInfos));
            return recipeInfos;
        }
        return null;
    }

    /**
     * 健康端获取处方详情-----合并处方
     * bug 88808 【实施】【中和医疗互联网医院】【C】【BUG】患者端处方支付修显示价格与结算价格不一致
     * 页面展示的是机构价格,当患者选择药企的时候根据更新药企销售价格到处方详情
     *
     * @param ext       没用
     * @param recipeIds 处方ID列表
     * @param depId     药企id
     */
    @RpcService
    public List<Map<String, Object>> findPatientRecipesByIdsAndDepId(Integer ext, List<Integer> recipeIds, Integer depId) {
        // 校验越权
        recipeIds.forEach(recipeId -> {
            checkUserHasPermission(recipeId);
        });
        Collections.sort(recipeIds, Collections.reverseOrder());
        LOGGER.info("findPatientRecipesByIdsAndDepId recipeIds:{} depId:{}", JSONUtils.toString(recipeIds), depId);
        //把处方对象返回给前端--合并处方--原确认订单页面的处方详情是通过getPatientRecipeById获取的
        if (CollectionUtils.isNotEmpty(recipeIds)) {
            List<Map<String, Object>> recipeInfos = new ArrayList<>(recipeIds.size());
            for (Integer recipeId : recipeIds) {
                recipeInfos.add(getRecipeAndDetailByIdImpl(recipeId, false, depId));
            }
            LOGGER.info("findPatientRecipesByIdsAndDepId response:{}", JSONUtils.toString(recipeInfos));
            return recipeInfos;
        }
        return null;
    }

    /**
     * 处方签获取
     *
     * @param recipeId
     * @param organId
     * @return
     */
    @RpcService
    public Map<String, List<RecipeLabelDTO>> queryRecipeLabelById(int recipeId, Integer organId) {
        RecipeInfoDTO recipePdfDTO = recipeManager.getRecipeInfoDictionary(recipeId);
        Recipe recipe = recipePdfDTO.getRecipe();
        ApothecaryDTO apothecaryDTO = caManager.attachSealPic(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getChecker(), recipe.getGiveUser(), recipe.getRecipeId());
        recipePdfDTO.setApothecary(apothecaryDTO);
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        if (null == recipeOrder || null == recipeOrder.getDispensingTime()) {
            apothecaryDTO.setGiveUserSignImg(null);
        }
        recipePdfDTO.setRecipeOrder(recipeOrder);
        Map<String, List<RecipeLabelDTO>> recipeLabel = operationClient.queryRecipeLabel(recipePdfDTO);
        LOGGER.info("recipeService queryRecipeLabelById ,recipeLabel={}", JSON.toJSONString(recipeLabel));
        return recipeLabel;
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
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
        Recipe dbRecipe = recipeDAO.getByRecipeId(recipeId);
        RecipeOrder order = recipeOrderDAO.getByOrderCode(dbRecipe.getOrderCode());
        if (null == dbRecipe) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipe not exist!");
        }

        //进行判断该处方单是否已处理，已处理则返回具体购药方式
        if (1 == dbRecipe.getChooseFlag()) {
            //如果之前选择的是到院取药且未支付 则可以进行转在线支付的方式
            if (1 == flag && RecipeBussConstant.GIVEMODE_TO_HOS.equals(dbRecipe.getGiveMode()) && 0 == dbRecipe.getPayFlag()) {
                return 0;
            }
            return PayModeGiveModeUtil.getPayMode(order.getPayMode(), dbRecipe.getGiveMode());
        } else {
            return 0;
        }

    }

    @RpcService
    public void sendRecipeTagToPatientWithOfflineRecipe(String mpiId, Integer organId, String recipeCode, String cardId, Integer consultId, Integer doctorId) {
        RecipeServiceSub.sendRecipeTagToPatientWithOfflineRecipe(mpiId, organId, recipeCode, cardId, consultId, doctorId);
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
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        //date 20191022到院取药取配置项
        boolean flag = RecipeServiceSub.getDrugToHos(recipe);
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
        Integer giveMode = PayModeGiveModeUtil.getGiveMode(payMode);
        if (GiveModeEnum.GIVE_MODE_HOSPITAL_DRUG.getType().equals(giveMode)) {
            return selectDepId;
        }
        Integer backDepId = null;
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        //date 20191022 修改到院取药配置项
        boolean flag = RecipeServiceSub.getDrugToHos(recipe);
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
            List<DrugsEnterprise> list = findSupportDepList(Arrays.asList(recipeId), recipe.getClinicOrgan(), payMode, true, selectDepId);
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
    public List<DrugsEnterprise> findSupportDepList(List<Integer> recipeIdList, int organId, Integer payMode, boolean sigle, Integer selectDepId) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
        SaleDrugListDAO saleDrugListDAO = getDAO(SaleDrugListDAO.class);
        RecipeDetailDAO recipeDetailDAO = getDAO(RecipeDetailDAO.class);
        IHisConfigService iHisConfigService = ApplicationUtils.getBaseService(IHisConfigService.class);

        List<DrugsEnterprise> backList = new ArrayList<>(5);
        //线上支付能力判断
        boolean onlinePay = true;
        if (null == payMode || RecipeBussConstant.PAYMODE_ONLINE.equals(payMode) || RecipeBussConstant.PAYMODE_MEDICAL_INSURANCE.equals(payMode)) {
            //需要判断医院HIS是否开通
            boolean hisStatus = iHisConfigService.isHisEnable(organId);
            LOGGER.info("findSupportDepList payAccount={}, hisStatus={}", null, hisStatus);
            if (!hisStatus) {
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
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            List<Recipedetail> recipedetailList = recipeDetailDAO.findByRecipeId(recipeId);
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
                    RecipeSupportGiveModeEnum recipeSupportGiveModeEnum = enterpriseManager.getDepSupportModeByPayMode(payMode);
                    if (Objects.isNull(recipeSupportGiveModeEnum)) {
                        LOGGER.error("findSupportDepList 处方[{}]无法匹配配送方式. payMode=[{}]", recipeId, payMode);
                        break;
                    }

                    //筛选出来的数据已经去掉不支持任何方式配送的药企
                    drugsEnterpriseList = drugsEnterpriseDAO.findByOrganIdAndPayModeSupport(organId, recipeSupportGiveModeEnum.getType());
                    if (CollectionUtils.isEmpty(drugsEnterpriseList)) {
                        LOGGER.error("findSupportDepList 处方[{}]没有任何药企可以进行配送！", recipeId);
                        break;
                    }
                } else {
                    drugsEnterpriseList = drugsEnterpriseDAO.findByOrganId(organId);
                }
            }
            List<OrganAndDrugsepRelation> organAndDrugsDepRelationList = organAndDrugsepRelationDAO.findByOrganId(organId);
            Map<Integer, OrganAndDrugsepRelation> drugsDepRelationMap = organAndDrugsDepRelationList.stream().collect(Collectors.toMap(OrganAndDrugsepRelation::getDrugsEnterpriseId, a -> a, (k1, k2) -> k1));
            for (DrugsEnterprise dep : drugsEnterpriseList) {
                //根据药企是否能满足所有配送的药品优先
                Integer depId = dep.getId();
                OrganAndDrugsepRelation drugsDepRelation = drugsDepRelationMap.get(dep.getId());
                //不支持在线支付跳过该药企
                String giveModeSupport = drugsDepRelation.getDrugsEnterpriseSupportGiveMode();
                if ((giveModeSupport.contains(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType().toString()) ||
                        giveModeSupport.contains(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType().toString()))
                        && !onlinePay) {
                    continue;
                }
                //药品匹配成功标识
                boolean succFlag = false;
                //date 20200921 修改【his管理的药企】不用校验配送药品，由预校验结果
                if (new Integer(1).equals(RecipeServiceSub.getOrganEnterprisesDockType(organId))) {
                    succFlag = true;
                } else {
                    Long count = saleDrugListDAO.getCountByOrganIdAndDrugIds(depId, drugIds);
                    if (null != count && count > 0) {
                        if (count == drugIds.size()) {
                            succFlag = true;
                        }
                    }
                }

                if (!succFlag) {
                    LOGGER.error("findSupportDepList 药企名称=[{}]存在不支持配送药品. 处方ID=[{}], 药企ID=[{}], drugIds={}", dep.getName(), recipeId, depId, JSONUtils.toString(drugIds));
                    continue;
                } else {
                    //通过查询该药企库存，最终确定能否配送
                    EnterpriseStock enterpriseStock = stockBusinessService.enterpriseStockCheck(recipe, recipedetailList, dep.getId(), StockCheckSourceTypeEnum.PATIENT_STOCK.getType());
                    succFlag = enterpriseStock.getStock();
                    if (succFlag || dep.getCheckInventoryFlag() == 2) {
                        subDepList.add(dep);
                        //只需要查询单供应商就返回
                        if (sigle) {
                            break;
                        }
                        LOGGER.info("findSupportDepList 药企名称=[{}]支持配送该处方所有药品. 处方ID=[{}], 药企ID=[{}], drugIds={}", dep.getName(), recipeId, depId, JSONUtils.toString(drugIds));
                    } else {
                        LOGGER.error("findSupportDepList  药企名称=[{}]药企库存查询返回药品无库存. 处方ID=[{}], 药企ID=[{}]", dep.getName(), recipeId, depId);
                    }
                }
            }

            if (CollectionUtils.isEmpty(subDepList)) {
                LOGGER.error("findSupportDepList 该处方获取不到支持的药企无法配送. recipeId=[{}]", recipeId);
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


    public List<DrugEnterpriseResult> findUnSupportDepList(Integer recipeId, int organId) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
        SaleDrugListDAO saleDrugListDAO = getDAO(SaleDrugListDAO.class);
        RecipeDetailDAO recipeDetailDAO = getDAO(RecipeDetailDAO.class);
        IHisConfigService iHisConfigService = ApplicationUtils.getBaseService(IHisConfigService.class);

        List<DrugEnterpriseResult> backList = new ArrayList<>();
        //线上支付能力判断
        boolean hisStatus = iHisConfigService.isHisEnable(organId);
        boolean onlinePay = hisStatus;

        //检测配送的药品是否按照完整的包装开的药，如 1*20支，开了10支，则不进行选择，数据库里主要是useTotalDose不为小数
        List<Double> totalDoses = recipeDetailDAO.findUseTotalDoseByRecipeId(recipeId);
        if (null != totalDoses && !totalDoses.isEmpty()) {
            for (Double totalDose : totalDoses) {
                if (null != totalDose) {
                    int itotalDose = (int) totalDose.doubleValue();
                    if (itotalDose != totalDose.doubleValue()) {
                        LOGGER.error("findUnSupportDepList 不支持非完整包装的计量药品配送. recipeId=[{}], totalDose=[{}]", recipeId, totalDose);
                        break;
                    }
                } else {
                    LOGGER.error("findUnSupportDepList 药品计量为null. recipeId=[{}]", recipeId);
                    break;
                }
            }
        } else {
            LOGGER.error("findUnSupportDepList 所有药品计量为null. recipeId=[{}]", recipeId);
            return backList;
        }

        List<Integer> drugIds = recipeDetailDAO.findDrugIdByRecipeId(recipeId);
        if (CollectionUtils.isEmpty(drugIds)) {
            LOGGER.error("findUnSupportDepList 处方[{}]没有任何药品！", recipeId);
            return backList;
        }
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findByOrganId(organId);
        List<OrganAndDrugsepRelation> organAndDrugsDepRelationList = organAndDrugsepRelationDAO.findByOrganId(organId);
        Map<Integer, OrganAndDrugsepRelation> drugsDepRelationMap = organAndDrugsDepRelationList.stream().collect(Collectors.toMap(OrganAndDrugsepRelation::getDrugsEnterpriseId, a -> a, (k1, k2) -> k1));
        for (DrugsEnterprise dep : drugsEnterpriseList) {
            //根据药企是否能满足所有配送的药品优先
            Integer depId = dep.getId();
            OrganAndDrugsepRelation drugsDepRelation = drugsDepRelationMap.get(dep.getId());
            //不支持在线支付跳过该药企
            String giveModeSupport = drugsDepRelation.getDrugsEnterpriseSupportGiveMode();
            if ((giveModeSupport.contains(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType().toString()) ||
                    giveModeSupport.contains(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType().toString()))
                            && !onlinePay) {
                DrugEnterpriseResult result = new DrugEnterpriseResult(RecipeResultBean.FAIL);
                result.setObject(null);
                backList.add(result);
                continue;
            }
            //药品匹配成功标识
            boolean succFlag = false;
            //date 20200921 修改【his管理的药企】不用校验配送药品，由预校验结果
            if (new Integer(1).equals(RecipeServiceSub.getOrganEnterprisesDockType(organId))) {
                succFlag = true;
            } else {
                Long count = saleDrugListDAO.getCountByOrganIdAndDrugIds(depId, drugIds);
                if (null != count && count > 0) {
                    if (count == drugIds.size()) {
                        succFlag = true;
                    }
                }
            }
            if (!succFlag) {
                LOGGER.error("findUnSupportDepList 药企名称=[{}]存在不支持配送药品. 处方ID=[{}], 药企ID=[{}], drugIds={}", dep.getName(), recipeId, depId, JSONUtils.toString(drugIds));
                DrugEnterpriseResult result = new DrugEnterpriseResult(RecipeResultBean.FAIL);
                result.setObject(null);
                backList.add(result);
                continue;
            } else {
                //通过查询该药企库存，最终确定能否配送
                Recipe recipe = recipeDAO.getByRecipeId(recipeId);
                List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipeId);
                EnterpriseStock enterpriseStock = stockBusinessService.enterpriseStockCheck(recipe, recipeDetailList, dep.getId(), StockCheckSourceTypeEnum.PATIENT_STOCK.getType());
                succFlag = enterpriseStock.getStock();
                if (succFlag || dep.getCheckInventoryFlag() == 2) {
                    LOGGER.info("findUnSupportDepList 药企名称=[{}]支持配送该处方所有药品. 处方ID=[{}], 药企ID=[{}], drugIds={}", dep.getName(), recipeId, depId, JSONUtils.toString(drugIds));
                } else {
                    DrugEnterpriseResult result = new DrugEnterpriseResult(RecipeResultBean.FAIL);
                    backList.add(result);
                    result.setObject(null);
                    LOGGER.error("findUnSupportDepList  药企名称=[{}]药企库存查询返回药品无库存. 处方ID=[{}], 药企ID=[{}]", dep.getName(), recipeId, depId);
                }
            }
        }
        // 存在满足库存的药企
        if (CollectionUtils.isNotEmpty(backList) && CollectionUtils.isNotEmpty(drugsEnterpriseList) && backList.size() < drugsEnterpriseList.size()) {
            backList.clear();
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
        wxPayRefundForRecipe(4, recipeId, "操作人:[" + ((StringUtils.isEmpty(operName)) ? "" : operName) + "],理由:[" + ((StringUtils.isEmpty(reason)) ? "" : reason) + "]");
    }

    /**
     * 患者手动退款
     *
     * @return
     **/
    @RpcService
    public void patientRefundForRecipe(int recipeId) {
        //退款
        wxPayRefundForRecipe(5, recipeId, "患者手动申请退款");

        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);

        //上传监管平台
        CommonResponse response = null;
        HisSyncSupervisionService hisSyncService = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
        Boolean isRelationJgpt = recipe != null && organManager.isRelationJgpt(recipe.getClinicOrgan());
        LOGGER.info("patientRefundForRecipe recipeId={} isRelationJgpt={}", recipe.getRecipeId(), isRelationJgpt);
        try {

            if (isRelationJgpt) {
                response = hisSyncService.uploadRecipeVerificationIndicators(Arrays.asList(recipe));
                if (CommonConstant.SUCCESS.equals(response.getCode())) {
                    //记录日志
                    RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "监管平台上传处方退款信息成功");
                    LOGGER.info("patientRefundForRecipe execute success. recipeId={}", recipe.getRecipeId());
                } else {
                    RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "监管平台上传处方退款信息失败," + response.getMsg());
                    LOGGER.warn("patientRefundForRecipe execute error. recipe={}", JSONUtils.toString(recipe));
                }
            }

        } catch (Exception e) {
            LOGGER.warn("patientRefundForRecipe exception recipe={}", JSONUtils.toString(recipe), e);
        }
    }


    /**
     * 退款方法
     *
     * @param flag
     * @param recipeId
     */
    @RpcService
    public void wxPayRefundForRecipe(int flag, int recipeId, String log) {
        LOGGER.info("wxPayRefundForRecipe flag:{}, recipeId:{}, log:{}.", flag, recipeId, log);
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
            case 5:
                errorInfo += log;
                status = RecipeStatusConstant.REVOKE;
                break;
            case 6:
                errorInfo += log;
                break;
            default:
                errorInfo += "未知,flag=" + flag;

        }

        RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), status, errorInfo);

        //相应订单处理
        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
        RecipeOrder order = orderDAO.getByOrderCode(recipe.getOrderCode());
        List<Integer> recipeIds = JSONUtils.parse(order.getRecipeIdList(), List.class);
        if (1 == flag || 6 == flag) {
            orderService.updateOrderInfo(order.getOrderCode(), ImmutableMap.of("status", OrderStatusConstant.READY_PAY), null);
        } else if (PUSH_FAIL == flag) {
            orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO, false);
            if (Objects.nonNull(order)) {
                stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_OTHER);
            }
        } else if (REFUND_MANUALLY == flag) {
            orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO, false);
            if (Objects.nonNull(order)) {
                stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_USER);
            }
            for (Integer recid : recipeIds) {
                //处理处方单
                recipeDAO.updateRecipeInfoByRecipeId(recid, status, null);
            }
        } else if (REFUND_PATIENT == flag) {
            orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO, false);
            if (Objects.nonNull(order)) {
                stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_USER);
            }
            orderService.updateOrderInfo(order.getOrderCode(), ImmutableMap.of("payFlag", 2), null);
            for (Integer recid : recipeIds) {
                //处理处方单
                recipeDAO.updateRecipeInfoByRecipeId(recid, status, null);
            }
        }
        orderService.updateOrderInfo(order.getOrderCode(), ImmutableMap.of("refundFlag", 1, "refundTime", new Date()), null);

        try {
            //退款,根据是否邵逸夫支付进行退款处理
            //通过运营平台控制开关决定是否走此种模式
            Boolean syfPayMode = configurationClient.getValueBooleanCatch(order.getOrganId(), "syfPayMode", false);
            if (syfPayMode) {
                RecipeOrderPayFlow recipeOrderPayFlow = recipeOrderPayFlowManager.getByOrderIdAndType(order.getOrderId(), PayFlowTypeEnum.RECIPE_AUDIT.getType());
                if (null != recipeOrderPayFlow) {
                    if (StringUtils.isEmpty(recipeOrderPayFlow.getOutTradeNo())) {
                        //表示没有实际支付审方或者快递费,只需要更新状态
                        recipeOrderPayFlow.setPayFlag(PayFlagEnum.REFUND_SUCCESS.getType());
                        recipeOrderPayFlowManager.updateNonNullFieldByPrimaryKey(recipeOrderPayFlow);
                    } else {
                        //说明需要正常退审方费
                        payClient.refund(order.getOrderId(), PayBusTypeEnum.OTHER_BUS_TYPE.getName());
                    }
                }
                payClient.refund(order.getOrderId(), PayBusTypeEnum.RECIPE_BUS_TYPE.getName());
            } else {
                payClient.refund(order.getOrderId(), PayBusTypeEnum.RECIPE_BUS_TYPE.getName());
            }
        } catch (Exception e) {
            LOGGER.error("wxPayRefundForRecipe " + errorInfo + "*****微信退款异常！recipeId[" + recipeId + "],err[" + e.getMessage() + "]", e);
        }
        try {
            if (CHECK_NOT_PASS == flag || PUSH_FAIL == flag || REFUND_MANUALLY == flag) {
                //HIS消息发送
                RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
                hisService.recipeRefund(recipeId);
            }
        } catch (Exception e) {
            LOGGER.error("wxPayRefundForRecipe " + errorInfo + "*****HIS消息发送异常！recipeId[" + recipeId + "],err[" + e.getMessage() + "]", e);
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
        List<String> allMpiIds = patientClient.getAllMemberPatientsByCurrentPatient(mpiid);
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
    public RecipeResultBean updateRecipePayResultImplForOrder(boolean saveFlag, Integer recipeId, Integer payFlag, Map<String, Object> info, BigDecimal recipeFee) {
        LOGGER.info("recipe updateRecipePayResultImplForOrder recipeIds={},payFlag={} ,mapInfo={}", recipeId, payFlag, JSONArray.toJSONString(info));
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
        } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode) || RecipeBussConstant.PAYMODE_ONLINE.equals(payMode) || RecipeBussConstant.PAYMODE_MEDICAL_INSURANCE.equals(payMode)) {
            //配送到家
            giveMode = RecipeBussConstant.GIVEMODE_SEND_TO_HOME;
        } else if (RecipeBussConstant.PAYMODE_TO_HOS.equals(payMode)) {
            //到院取药
            giveMode = RecipeBussConstant.GIVEMODE_TO_HOS;
        } else if (RecipeBussConstant.PAYMODE_DOWNLOAD_RECIPE.equals(payMode)) {
            giveMode = RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE;
        } else {
            giveMode = null;
        }
        attrMap.put("giveMode", giveMode);
        attrMap.remove("payMode");
        LOGGER.info("recipe updateRecipePayResultImplForOrder giveMode={}", giveMode);
        Recipe dbRecipe = recipeDAO.getByRecipeId(recipeId);

        if (saveFlag && RecipeResultBean.SUCCESS.equals(result.getCode())) {
            if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(dbRecipe.getFromflag()) || RecipeBussConstant.FROMFLAG_HIS_USE.equals(dbRecipe.getFromflag())) {
                createPdfFactory.updateTotalPdfExecute(recipeId, recipeFee);
                //HIS消息发送
                RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
                hisService.recipeDrugTake(recipeId, payFlag, result);
            }
        }
        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            //根据审方模式改变
            auditModeContext.getAuditModes(dbRecipe.getReviewType()).afterPayChange(saveFlag, dbRecipe, result, attrMap);
            //支付成功后pdf异步显示对应的配送信息
            if (1 == payFlag) {
                createPdfFactory.updateAddressPdfExecute(recipeId);
            }
        }
        return result;
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
    public Map<String, Object> getHosRecipeList(Integer consultId, Integer organId, String mpiId) {
        RecipePreserveService preserveService = ApplicationUtils.getRecipeService(RecipePreserveService.class);
        //查询3个月以前的历史处方数据
        return preserveService.getHosRecipeList(consultId, organId, mpiId, 180);
    }

    @RpcService
    public RecipeResultBean getPageDetail(int recipeId) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        Recipe nowRecipe = DAOFactory.getDAO(RecipeDAO.class).get(recipeId);
        if (null == nowRecipe) {
            LOGGER.info("getPageDetailed: [recipeId:" + recipeId + "] 对应的处方信息不存在！");
            result.setCode(RecipeResultBean.FAIL);
            result.setError("处方单id对应的处方为空");
            return result;
        }
        Map<String, Object> ext = new HashMap<>(10);
        Map<String, Object> recipeMap = getPatientRecipeById(recipeId);
        if (null == nowRecipe.getOrderCode()) {
            result.setObject(recipeMap);
            ext.put("jumpType", "0");
            result.setExt(ext);
        } else {
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            RecipeResultBean orderDetail = orderService.getOrderDetail(nowRecipe.getOrderCode());
            result = orderDetail;
            Map<String, Object> nowExt = result.getExt();
            if (null == nowExt) {
                ext.put("jumpType", "1");
                result.setExt(ext);

            } else {
                nowExt.put("jumpType", "1");
                result.setExt(nowExt);
            }
            //date 2019/10/18
            //添加逻辑：添加处方的信息
            if (null != result.getObject()) {
                RecipeOrderBean orderBean = (RecipeOrderBean) result.getObject();
                RecipeOrderInfoBean infoBean = ObjectCopyUtils.convert(orderBean, RecipeOrderInfoBean.class);
                infoBean.setRecipeInfoMap(recipeMap);
                result.setObject(infoBean);
            }

        }
        return result;
    }

    @RpcService
    public RecipeResultBean changeRecipeStatusInfo(int recipeId, int status) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(recipeId);
        if (null == recipe) {
            LOGGER.info("changeRecipeStatusInfo: [recipeId:" + recipeId + "] 对应的处方信息不存在！");
            result.setCode(RecipeResultBean.FAIL);
            result.setError("处方单id对应的处方为空");
            return result;
        }
        Map<String, Object> searchMap = new HashMap<>(10);
        //判断修改的处方的状态是否是已下载
        if (status == RecipeStatusConstant.RECIPE_DOWNLOADED) {
            //当前处方下载处方状态的时候，确认处方的购药方式
            //首先判断处方的
            if (havChooseFlag == recipe.getChooseFlag() && RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE != recipe.getGiveMode()) {
                LOGGER.info("changeRecipeStatusInfo: [recipeId:" + recipeId + "] 对应的处方的购药方式不是下载处方不能设置成已下载状态！");
                result.setCode(RecipeResultBean.FAIL);
                result.setError("处方选择的购药方式不是下载处方");
                return result;
            }
            Integer beforStatus = recipe.getStatus();
            if (beforStatus == RecipeStatusConstant.REVOKE) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "处方单已被撤销");
            }
            searchMap.put("giveMode", RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE);
            searchMap.put("chooseFlag", havChooseFlag);
            //更新处方的信息
            Boolean updateResult = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.RECIPE_DOWNLOADED, searchMap);
            //更新处方log信息
            if (updateResult) {
                stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_DONE, RecipeStateEnum.SUB_DONE_DOWNLOAD);
                RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
                RecipeOrder order = orderDAO.getByOrderCode(recipe.getOrderCode());
                if (Objects.nonNull(order)) {
                    stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_DISPENSING, OrderStateEnum.SUB_DONE_DOWNLOAD);
                }
                RecipeLogService.saveRecipeLog(recipeId, beforStatus, RecipeStatusConstant.RECIPE_DOWNLOADED, "已下载状态修改成功");
            } else {
                LOGGER.info("changeRecipeStatusInfo: [recipeId:" + recipeId + "] 处方更新已下载状态失败！");
                result.setCode(RecipeResultBean.FAIL);
                result.setError("处方更新已下载状态失败");
                return result;
            }
            //处方来源于线下转线上的处方单
            if (recipe.getRecipeSourceType() == 2) {
                HisRecipeDAO hisRecipeDAO = getDAO(HisRecipeDAO.class);
                HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
                if (hisRecipe != null) {
                    hisRecipeDAO.updateHisRecieStatus(recipe.getClinicOrgan(), recipe.getRecipeCode(), 2);
                }
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
        if (CollectionUtils.isNotEmpty(recipeList)) {
            for (Recipe recipe : recipeList) {
                //更新处方的状态-已完成
                recipeId = recipe.getRecipeId();
                Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), RecipeStatusConstant.FINISH, null);
                //完成订单
                if (rs) {
                    LOGGER.info("changeDownLoadToFinishTask: 处方{}设置处方为已完成！", recipeId);
                    //完成订单
                    RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                    RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

                    orderService.finishOrder(recipe.getOrderCode(), null);
                    LOGGER.info("changeDownLoadToFinishTask: 订单{}设置为已完成！", recipe.getOrderCode());
                    //记录日志
                    RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.RECIPE_DOWNLOADED, RecipeStatusConstant.FINISH, "下载处方订单完成");
                    //HIS消息发送
                    hisService.recipeFinish(recipeId);
                    //发送取药完成消息(暂时不需要发送消息推送)

                    //监管平台核销上传
                    SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
                    syncExecutorService.uploadRecipeVerificationIndicators(recipeId);
                    stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_DONE, RecipeStateEnum.SUB_DONE_DOWNLOAD);

                } else {
                    LOGGER.warn("处方：{},更新失败", recipe.getRecipeId());
                }
            }
        }

    }


    /**
     * 当前新增药品数据到中间表
     * <p>
     * TODO药品转换 1 手工定时同步新增时用 2推送时新增用
     *
     * @param drug
     * @param organId
     */
    @RpcService
    public void addHisDrug(OrganDrugInfoTO drug, Integer organId, String operator) {
        List<OrganDrugListSyncField> organDrugListSyncFields = new ArrayList<>();
        DrugListMatch drugListMatch = new DrugListMatch();
        Map<String, OrganDrugListSyncField> organDrugListSyncFieldMap = new HashMap<>();
        organDrugListSyncFields = organDrugListSyncFieldDAO.findByOrganIdAndType(organId, SyncDrugConstant.ADD);
        organDrugListSyncFieldMap = organDrugListSyncFields.stream().collect(Collectors.toMap(OrganDrugListSyncField::getFieldCode, Function.identity()));

        if (StringUtils.isEmpty(drug.getOrganDrugCode())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugCode 机构 药品编码必填!");
        } else {
            drugListMatch.setOrganDrugCode(drug.getOrganDrugCode());

        }
        if (StringUtils.isEmpty(drug.getDrugName())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugName 机构 药品名称必填!");
        } else {
            drugListMatch.setDrugName(drug.getDrugName());
        }
        if (StringUtils.isEmpty(drug.getSaleName())) {
            drugListMatch.setSaleName(drug.getDrugName());
        } else {
            drugListMatch.setSaleName(drug.getSaleName());
        }
        if (ObjectUtils.isEmpty(drug.getUseDose())) {
            //中药不需要设置
            if (!(new Integer(3).equals(drug.getDrugType()))) {
                throw new DAOException(DAOException.VALUE_NEEDED, "useDose 机构 单次剂量(规格单位)必填!");
            }
        } else {
            drugListMatch.setUseDose(drug.getUseDose());
        }


        if (StringUtils.isEmpty(drug.getDrugSpec())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugSpec 机构 药品规格必填!");
        } else {
            drugListMatch.setDrugSpec(drug.getDrugSpec());
        }
        if (ObjectUtils.isEmpty(drug.getDrugType())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugType 机构 药品类型必填!");
        } else {
            drugListMatch.setDrugType(drug.getDrugType());
        }
        if (!ObjectUtils.isEmpty(drug.getDrugType())) {
            drugListMatch.setDrugType(drug.getDrugType());
        }
        if (!ObjectUtils.isEmpty(drug.getChemicalName())) {
            drugListMatch.setChemicalName(drug.getChemicalName());
        }
        if (ObjectUtils.isEmpty(drug.getPack())) {
            //中药不需要设置
            if (!(new Integer(3).equals(drug.getDrugType()))) {
                throw new DAOException(DAOException.VALUE_NEEDED, "pack 机构 药品包装数量必填!");
            } else {
                drug.setPack(1);
            }
        } else {
            drugListMatch.setPack(drug.getPack().intValue());
        }

        if (ObjectUtils.isEmpty(drug.getUnit())) {
            //中药不需要设置
            if (!(new Integer(3).equals(drug.getDrugType()))) {
                throw new DAOException(DAOException.VALUE_NEEDED, "unit 机构 药品单位必填!");
            }
        } else {
            drugListMatch.setUnit(drug.getUnit());
        }
        if (ObjectUtils.isEmpty(drug.getProducer())) {
            if (!(new Integer(3).equals(drug.getDrugType()))) {
                throw new DAOException(DAOException.VALUE_NEEDED, "producer 机构 药品生产厂家必填!");
            }
        } else {
            drugListMatch.setProducer(drug.getProducer());
        }
        if (!ObjectUtils.isEmpty(drug.getBaseDrug())) {
            drugListMatch.setBaseDrug(drug.getBaseDrug());
        }
        if (!ObjectUtils.isEmpty(drug.getUseDoseUnit())) {
            drugListMatch.setUseDoseUnit(drug.getUseDoseUnit());
        }
        if (!ObjectUtils.isEmpty(drug.getRetrievalCode())) {
            drugListMatch.setRetrievalCode(drug.getRetrievalCode());
        }
        if (!ObjectUtils.isEmpty(drug.getPrice())) {
            BigDecimal drugPrice = new BigDecimal(drug.getPrice());
            drugListMatch.setPrice(drugPrice);
        }
        if (!ObjectUtils.isEmpty(drug.getDrugManfCode())) {
            drugListMatch.setDrugManfCode(drug.getDrugManfCode());
        }
        if (!ObjectUtils.isEmpty(drug.getLicenseNumber())) {
            drugListMatch.setLicenseNumber(drug.getLicenseNumber());
        }
        if (!ObjectUtils.isEmpty(drug.getStandardCode())) {
            drugListMatch.setStandardCode(drug.getStandardCode());
        }
        if (!ObjectUtils.isEmpty(drug.getIndications())) {
            drugListMatch.setIndications(drug.getIndications());
        }
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.drugForm))) {
            if (!ObjectUtils.isEmpty(drug.getDrugform())) {
                drugListMatch.setDrugForm(drug.getDrugform());
            }
        }

        if (!ObjectUtils.isEmpty(drug.getPackingMaterials())) {
            drugListMatch.setPackingMaterials(drug.getPackingMaterials());
        }
        if (!ObjectUtils.isEmpty(drug.getMedicalDrugCode())) {
            drugListMatch.setMedicalDrugCode(drug.getMedicalDrugCode());
        }
        if (!ObjectUtils.isEmpty(drug.getMedicalDrugFormCode())) {
            drugListMatch.setMedicalDrugFormCode(drug.getMedicalDrugFormCode());
        }
        if (!ObjectUtils.isEmpty(drug.getHisFormCode())) {
            drugListMatch.setHisFormCode(drug.getHisFormCode());
        }
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.medicalInsuranceControl))) {
            if (!ObjectUtils.isEmpty(drug.getMedicalInsuranceControl())) {
                drugListMatch.setMedicalInsuranceControl(drug.getMedicalInsuranceControl());
            }
        }
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.indicationsDeclare))) {
            if (!ObjectUtils.isEmpty(drug.getIndicationsDeclare())) {
                drugListMatch.setIndicationsDeclare(drug.getIndicationsDeclare());
            }
        }

        //单复方
        if (!ObjectUtils.isEmpty(drug.getUnilateralCompound())) {
            drugListMatch.setUnilateralCompound(drug.getUnilateralCompound());
        }
        if (Objects.nonNull(drug.getSmallestUnitUseDose())) {
            drugListMatch.setSmallestUnitUseDose(drug.getSmallestUnitUseDose());
        }
        if (Objects.nonNull(drug.getDefaultSmallestUnitUseDose())) {
            drugListMatch.setDefaultSmallestUnitUseDose(drug.getDefaultSmallestUnitUseDose());
        }
        if (StringUtils.isNotEmpty(drug.getUseDoseSmallestUnit())) {
            drugListMatch.setUseDoseSmallestUnit(drug.getUseDoseSmallestUnit());
        }

        //药房
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.pharmacy))
            && !ObjectUtils.isEmpty(drug.getPharmacyCode())) {
            StringBuilder pharmacys = new StringBuilder();
            String[] pharmcyCodeArr = drug.getPharmacyCode().split(",");
            String[] pharmcyNameArr = null;
            if (StringUtils.isNotEmpty(drug.getPharmacyName())) {
                pharmcyNameArr = drug.getPharmacyName().split(",");
            }
            for (int i = 0; i < pharmcyCodeArr.length; i++) {
                String pharmacyCode = pharmcyCodeArr[i];
                if(StringUtils.isNotEmpty(pharmacyCode)){
                    PharmacyTcm pharmacyTcmDb = pharmacyTcmDAO.getByPharmacyAndOrganId(pharmacyCode, organId);
                    if(pharmacyTcmDb!=null){
                        if (i != pharmcyCodeArr.length - 1) {
                            pharmacys.append(pharmacyTcmDb.getPharmacyId() + ",");
                        } else {
                            pharmacys.append(pharmacyTcmDb.getPharmacyId());
                        }
                    }else{
                        if (Objects.nonNull(pharmcyNameArr) && !ObjectUtils.isEmpty(pharmcyNameArr[i])) {
                            PharmacyTcm pharmacyTcm = new PharmacyTcm();
                            pharmacyTcm.setOrganId(organId);
                            pharmacyTcm.setPharmacyCode(pharmacyCode);
                            pharmacyTcm.setPharmacyName(pharmcyNameArr[i]);
                            pharmacyTcm.setPharmacyCategray("中药,西药,中成药,膏方");
                            pharmacyTcm.setDrugFormType(RecipeDrugFormTypeEnum.TCM_DECOCTION_PIECES.getType().toString());
                            pharmacyTcm.setWhDefault(false);
                            pharmacyTcm.setSort(1000);
                            boolean b = pharmacyTcmService.addPharmacyTcmForOrgan(pharmacyTcm);
                            if (b) {
                                PharmacyTcm pharmacyTcm1 = pharmacyTcmService.querPharmacyTcmByOrganIdAndName2(organId, pharmcyNameArr[i]);
                                if (i != pharmcyCodeArr.length - 1) {
                                    pharmacys.append(pharmacyTcm1.getPharmacyId() + ",");
                                } else {
                                    pharmacys.append(pharmacyTcm1.getPharmacyId());
                                }
                            }
                        }

                    }
                }
            }
            drugListMatch.setPharmacy(pharmacys.toString());
        }
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.drugsEnterpriseIds))) {
            if (!ObjectUtils.isEmpty(drug.getDrugsEnterpriseCode())) {
                String pharmacyCode = drug.getDrugsEnterpriseCode();
                String[] split = pharmacyCode.split(",");
                StringBuilder ss = new StringBuilder();
                for (int i = 0; i < split.length; i++) {
                    DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                    DrugsEnterprise byEnterpriseCode = drugsEnterpriseDAO.getByEnterpriseCode(split[i], organId);
                    if (ObjectUtils.isEmpty(byEnterpriseCode)) {
                        throw new DAOException(DAOException.VALUE_NEEDED, "平台根据药企编码" + split[i] + " 未找到药企");
                    } else {
                        if (i != split.length - 1) {
                            ss.append(byEnterpriseCode.getId().toString() + ",");
                        } else {
                            ss.append(byEnterpriseCode.getId().toString());
                        }
                    }
                }
                drugListMatch.setDrugsEnterpriseIds(ss.toString());
            }
        }

        if (!ObjectUtils.isEmpty(drug.getRegulationDrugCode())) {
            drugListMatch.setRegulationDrugCode(drug.getRegulationDrugCode());
        }
        if (!ObjectUtils.isEmpty(organId)) {
            drugListMatch.setSourceOrgan(organId);
        }
        //药品嘱托
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.drugEntrust))) {
            if (!ObjectUtils.isEmpty(drug.getDrugEntrust())) {
                drugListMatch.setDrugEntrust(drug.getDrugEntrust());
            }
        }
        drugListMatch.setStatus(0);
        drugListMatch.setDrugSource(1);
        drugListMatch.setApplyBusiness("1");
        if (StringUtils.isNotEmpty(drug.getDrugItemCode())) {
            drugListMatch.setDrugItemCode(drug.getDrugItemCode());
        }
        if (!ObjectUtils.isEmpty(drug.getTargetedDrugType())) {
            drugListMatch.setTargetedDrugType(drug.getTargetedDrugType());
        }
        if (!ObjectUtils.isEmpty(drug.getSmallestSaleMultiple())) {
            drugListMatch.setSmallestSaleMultiple(drug.getSmallestSaleMultiple());
        }
        if (!ObjectUtils.isEmpty(drug.getSmallestSaleMultiple())) {
            drugListMatch.setRecommendedUseDose(drug.getRecommendedUseDose());
        }
        if (!ObjectUtils.isEmpty(drug.getUsePathways())) {
            drugListMatch.setUsePathways(drug.getUsePathways());
            IUsePathwaysService bean = AppContextHolder.getBean("basic.usePathwaysService", IUsePathwaysService.class);
            List<UsePathwaysDTO> allUsePathwaysByOrganId = bean.findAllUsePathwaysByOrganId(organId);
            if (ObjectUtils.isEmpty(allUsePathwaysByOrganId)) {
                UsePathwaysDTO usePathwaysDTO = bean.findUsePathwaysByOrganAndKey(0, drug.getUsePathways());
                if (ObjectUtils.isEmpty(usePathwaysDTO)) {
                    String error = "usePathways:" + drug.getUsePathways() + " 平台未找到该用药途径!";
                    throw new DAOException(DAOException.VALUE_NEEDED, error);
                } else {
                    drugListMatch.setUsePathwaysId(usePathwaysDTO.getId().toString());
                }
            } else {
                UsePathwaysDTO usePathwaysDTO = bean.findUsePathwaysByOrganAndKey(organId, drug.getUsePathways());
                if (ObjectUtils.isEmpty(usePathwaysDTO)) {
                    String error = "usePathways:" + drug.getUsePathways() + " 机构未找到该用药途径!";
                    throw new DAOException(DAOException.VALUE_NEEDED, error);
                } else {
                    drugListMatch.setUsePathwaysId(usePathwaysDTO.getId().toString());
                }
            }
        }
        if (!ObjectUtils.isEmpty(drug.getUsingRate())) {
            drugListMatch.setUsingRate(drug.getUsingRate());
            IUsingRateService bean = AppContextHolder.getBean("basic.usingRateService", IUsingRateService.class);
            List<UsingRateDTO> allusingRateByOrganId = bean.findAllusingRateByOrganId(organId);
            if (ObjectUtils.isEmpty(allusingRateByOrganId)) {
                UsingRateDTO usingRateDTO = bean.findUsingRateDTOByOrganAndKey(0, drug.getUsingRate());
                if (ObjectUtils.isEmpty(usingRateDTO)) {
                    String error = "usingRate:" + drug.getUsingRate() + " 平台未找到该用药频次!";
                    throw new DAOException(DAOException.VALUE_NEEDED, error);
                } else {
                    drugListMatch.setUsingRateId(usingRateDTO.getId().toString());
                }

            } else {
                UsingRateDTO usingRateDTOByOrganAndKey = bean.findUsingRateDTOByOrganAndKey(organId, drug.getUsingRate());
                if (ObjectUtils.isEmpty(usingRateDTOByOrganAndKey)) {
                    String error = "usingRate:" + drug.getUsingRate() + " 机构未找到该用药频次!";
                    throw new DAOException(DAOException.VALUE_NEEDED, error);
                } else {
                    drugListMatch.setUsingRateId(usingRateDTOByOrganAndKey.getId().toString());
                }
            }
        }
        //是否抗肿瘤药物,及抗肿瘤药物等级
        if (!ObjectUtils.isEmpty(drug.getAntiTumorDrugFlag())) {
            if (new Integer(1).equals(drug.getAntiTumorDrugFlag()) && ObjectUtils.isEmpty(drug.getAntiTumorDrugLevel())) {
                throw new DAOException(DAOException.VALUE_NEEDED, "药品为抗肿瘤药物时，抗肿瘤药物等级必填!");
            } else {
                drugListMatch.setAntiTumorDrugFlag(drug.getAntiTumorDrugFlag());
                if (!ObjectUtils.isEmpty(drug.getAntiTumorDrugLevel())) {
                    drugListMatch.setAntiTumorDrugLevel(drug.getAntiTumorDrugLevel());
                }
            }
        }
        //抗菌素药物
        if (!ObjectUtils.isEmpty(drug.getAntibioticsDrugLevel())) {
            drugListMatch.setAntibioticsDrugLevel(drug.getAntibioticsDrugLevel());
        }
        //是否精神药物
        if (!ObjectUtils.isEmpty(drug.getPsychotropicDrugFlag())) {
            drugListMatch.setPsychotropicDrugFlag(drug.getPsychotropicDrugFlag());
        }
        //是否麻醉药物
        if (!ObjectUtils.isEmpty(drug.getNarcoticDrugFlag())) {
            drugListMatch.setNarcoticDrugFlag(drug.getNarcoticDrugFlag());
        }
        //是否毒性药物
        if (!ObjectUtils.isEmpty(drug.getToxicDrugFlag())) {
            drugListMatch.setToxicDrugFlag(drug.getToxicDrugFlag());
        }
        //是否放射性药物
        if (!ObjectUtils.isEmpty(drug.getRadioActivityDrugFlag())) {
            drugListMatch.setRadioActivityDrugFlag(drug.getRadioActivityDrugFlag());
        }
        //是否特殊使用级抗生素药物
        if (!ObjectUtils.isEmpty(drug.getSpecialUseAntibioticDrugFlag())) {
            drugListMatch.setSpecialUseAntibioticDrugFlag(drug.getSpecialUseAntibioticDrugFlag());
        }

        if (!ObjectUtils.isEmpty(drug.getUnitHisCode())) {
            drugListMatch.setUnitHisCode(drug.getUnitHisCode());
        }

        if (!ObjectUtils.isEmpty(drug.getUseDoseUnitHisCode())) {
            drugListMatch.setUseDoseUnitHisCode(drug.getUseDoseUnitHisCode());
        }

        if (!ObjectUtils.isEmpty(drug.getUseDoseSmallestUnitHisCode())) {
            drugListMatch.setUseDoseSmallestUnitHisCode(drug.getUseDoseSmallestUnitHisCode());
        }

        if(drug.getSkinTestDrugFlag() != null){
            drugListMatch.setSkinTestDrugFlag(drug.getSkinTestDrugFlag());
        }

        if(drug.getNationalStandardDrugFlag() != null){
            drugListMatch.setNationalStandardDrugFlag(drug.getNationalStandardDrugFlag());
        }

        if(StringUtils.isNotEmpty(drug.getHisDrugClassCode())){
            drugListMatch.setHisDrugClassCode(drug.getHisDrugClassCode());
        }

        if(StringUtils.isNotEmpty(drug.getHisDrugClassName())){
            drugListMatch.setHisDrugClassName(drug.getHisDrugClassName());
        }

        if(drug.getMaximum() != null){
            drugListMatch.setMaximum(drug.getMaximum());
        }

//        if(drug.getColdChainTransportationFlag() != null){
//            drugListMatch.setColdChainTransportationFlag(drug.getColdChainTransportationFlag());
//        }

        LOGGER.info("drugInfoSynMovementaddHisDrug" + drug.getDrugName() + "organId=[{}] drug=[{}]", organId, JSONUtils.toString(drug));
        List<DrugListMatch> dataByOrganDrugCode = drugListMatchDAO.findDataByOrganDrugCode(drugListMatch.getOrganDrugCode(), drugListMatch.getSourceOrgan());
        if (ObjectUtils.isEmpty(dataByOrganDrugCode)) {
            DrugListMatch save = drugListMatchDAO.save(drugListMatch);
            try {
                drugToolService.automaticDrugMatch(save, operator);
                DrugListMatch drugListMatch1 = drugListMatchDAO.get(save.getDrugId());
                IHisServiceConfigService configService = AppContextHolder.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
                Boolean regulationFlag = configService.getRegulationFlag();
                if (regulationFlag) {
                    drugListMatch1.setRegulationDrugCode(drugListMatch1.getPlatformDrugId().toString());
                    drugListMatchDAO.updateData(drugListMatch1);
                }
            } catch (Exception e) {
                LOGGER.error("addHisDrug.updateMatchAutomatic fail,", e);
            }
        }
        DataSyncDTO dataSyncDTO = convertDataSyn(drug, organId, 1, null, 1, null);
        List<DataSyncDTO> syncDTOList = Lists.newArrayList();
        syncDTOList.add(dataSyncDTO);
        dataSyncLogService.addDataSyncLog("1", syncDTOList);
        LOGGER.info("addHisDrug 成功{}", JSONUtils.toString(drugListMatch));
    }

    /**
     * 是否允许同步字段
     *
     * @param obj
     * @return
     */
    private boolean isAllowSyncField(OrganDrugListSyncField obj) {
        //默认为同步
        //页面没勾选该字段，表示不同步
        return null == obj || !"0".equals(obj.getIsSync());

    }

    /**
     * 手动同步药品数据
     * TODO药品转换 2 手工同步修改时用 2推送更新用
     *
     * @param drug
     * @param organDrug
     */
    public void updateHisOrganDrug(OrganDrugInfoTO drug, OrganDrugList organDrug, Integer organId) {
        if (null == organDrug) {
            LOGGER.info("updateHisOrganDrug 机构药品空");
            return;
        }
        List<OrganDrugListSyncField> organDrugListSyncFields = new ArrayList<>();
        Map<String, OrganDrugListSyncField> organDrugListSyncFieldMap = new HashMap<>();
        organDrugListSyncFields = organDrugListSyncFieldDAO.findByOrganIdAndType(organId, SyncDrugConstant.UPDATE);
        organDrugListSyncFieldMap = organDrugListSyncFields.stream().collect(Collectors.toMap(OrganDrugListSyncField::getFieldCode, Function.identity()));

        //是否抗肿瘤药物,及抗肿瘤药物等级
        if (!ObjectUtils.isEmpty(drug.getAntiTumorDrugFlag())) {
            if (new Integer(1).equals(drug.getAntiTumorDrugFlag()) && ObjectUtils.isEmpty(drug.getAntiTumorDrugLevel())) {
                throw new DAOException(DAOException.VALUE_NEEDED, "药品为抗肿瘤药物时，抗肿瘤药物等级必填!");
            } else {
                organDrug.setAntiTumorDrugFlag(drug.getAntiTumorDrugFlag());
                if (!ObjectUtils.isEmpty(drug.getAntiTumorDrugLevel())) {
                    organDrug.setAntiTumorDrugLevel(drug.getAntiTumorDrugLevel());
                }
            }
        }
        //药品本位码
        if (!ObjectUtils.isEmpty(drug.getStandardCode())) {
            organDrug.setStandardCode(drug.getStandardCode());
        }
        //抗菌素药物
        if (!ObjectUtils.isEmpty(drug.getAntibioticsDrugLevel())) {
            organDrug.setAntibioticsDrugLevel(drug.getAntibioticsDrugLevel());
        }
        //是否精神药物
        if (!ObjectUtils.isEmpty(drug.getPsychotropicDrugFlag())) {
            organDrug.setPsychotropicDrugFlag(drug.getPsychotropicDrugFlag());
        }
        //是否麻醉药物
        if (!ObjectUtils.isEmpty(drug.getNarcoticDrugFlag())) {
            organDrug.setNarcoticDrugFlag(drug.getNarcoticDrugFlag());
        }
        //是否毒性药物
        if (!ObjectUtils.isEmpty(drug.getToxicDrugFlag())) {
            organDrug.setToxicDrugFlag(drug.getToxicDrugFlag());
        }
        //是否放射性药物
        if (!ObjectUtils.isEmpty(drug.getRadioActivityDrugFlag())) {
            organDrug.setRadioActivityDrugFlag(drug.getRadioActivityDrugFlag());
        }
        //是否特殊使用级抗生素药物
        if (!ObjectUtils.isEmpty(drug.getSpecialUseAntibioticDrugFlag())) {
            organDrug.setSpecialUseAntibioticDrugFlag(drug.getSpecialUseAntibioticDrugFlag());
        }
        if (!ObjectUtils.isEmpty(drug.getUnitHisCode())) {
            organDrug.setUnitHisCode(drug.getUnitHisCode());
        }

        if (!ObjectUtils.isEmpty(drug.getUseDoseUnitHisCode())) {
            organDrug.setUseDoseUnitHisCode(drug.getUseDoseUnitHisCode());
        }

        if (!ObjectUtils.isEmpty(drug.getUseDoseSmallestUnitHisCode())) {
            organDrug.setUseDoseSmallestUnitHisCode(drug.getUseDoseSmallestUnitHisCode());
        }
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.drugForm))
                && !ObjectUtils.isEmpty(drug.getDrugform())) {
            organDrug.setDrugForm(drug.getDrugform());
            organDrug.setHisDrugForm(drug.getDrugform());
        }
        //获取金额
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.salePrice))
                && StringUtils.isNotEmpty(drug.getPrice())) {
            BigDecimal drugPrice = new BigDecimal(drug.getPrice());
            organDrug.setSalePrice(drugPrice);
        }
        //药品单位
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.unit))
                && StringUtils.isNotEmpty(drug.getUnit())) {
            String packUnit = drug.getUnit();
            organDrug.setUnit(packUnit);
        }
        if (StringUtils.isNotEmpty(drug.getChemicalName())) {
            organDrug.setChemicalName(drug.getChemicalName());
        }
        //药品规格
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.drugSpec))
                && StringUtils.isNotEmpty(drug.getDrugSpec())) {
            organDrug.setDrugSpec(drug.getDrugSpec());
        }
        //医保药品编码
        if (StringUtils.isNotEmpty(drug.getMedicalDrugCode())) {
            organDrug.setMedicalDrugCode(drug.getMedicalDrugCode());
        }
        //转换系数
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.pack))
                && !ObjectUtils.isEmpty(drug.getPack())) {
            organDrug.setPack(Integer.valueOf(drug.getPack()));
        }
        //实际单次剂量（规格单位）
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.useDose))
                && !ObjectUtils.isEmpty(drug.getUseDose())) {
            organDrug.setUseDose(drug.getUseDose());
        }
        //生产厂家
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.producer))
                && !StringUtils.isEmpty(drug.getProducer())) {
            organDrug.setProducer(drug.getProducer());
        }
       /* //生产厂家编码
        if (StringUtils.isNotEmpty(drug.getDrugManfCode())) {
            organDrug.setProducerCode(drug.getDrugManfCode());
        }*/
        //商品名
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.saleName))
                && StringUtils.isNotEmpty(drug.getSaleName())) {
            organDrug.setSaleName(drug.getSaleName());
        }
        //通用名
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.drugName))
                && StringUtils.isNotEmpty(drug.getDrugName())) {
            organDrug.setDrugName(drug.getDrugName());

        }
//        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.drugType))){
//            if (null!=drug.getDrugType()) {
//                organDrug.setDrugType(drug.getDrugName());
//            }
//        }
        //单次剂量单位（规格单位）
        if (!ObjectUtils.isEmpty(drug.getUseDoseUnit())) {
            organDrug.setUseDoseUnit(drug.getUseDoseUnit());
        }
        //院内检索关键字
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.retrievalCode))) {
            if (!ObjectUtils.isEmpty(drug.getRetrievalCode())) {
                organDrug.setRetrievalCode(drug.getRetrievalCode());
            }
        }

        //单复方
        if (!ObjectUtils.isEmpty(drug.getUnilateralCompound())) {
            organDrug.setUnilateralCompound(drug.getUnilateralCompound());
        }

        //药房
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.pharmacy))
            &&!ObjectUtils.isEmpty(drug.getPharmacyCode())) {
            StringBuilder pharmacys = new StringBuilder();
            String[] pharmcyCodeArr = drug.getPharmacyCode().split(",");
            String[] pharmcyNameArr = null;
            if (StringUtils.isNotEmpty(drug.getPharmacyName())) {
                pharmcyNameArr = drug.getPharmacyName().split(",");
            }
            for (int i = 0; i < pharmcyCodeArr.length; i++) {
                String pharmacyCode = pharmcyCodeArr[i];
                if(StringUtils.isNotEmpty(pharmacyCode)){
                    PharmacyTcm pharmacyTcmDb = pharmacyTcmDAO.getByPharmacyAndOrganId(pharmacyCode, organId);
                    if(pharmacyTcmDb!=null){
                        if (i != pharmcyCodeArr.length - 1) {
                            pharmacys.append(pharmacyTcmDb.getPharmacyId() + ",");
                        } else {
                            pharmacys.append(pharmacyTcmDb.getPharmacyId());
                        }
                    }else{
                        if (Objects.nonNull(pharmcyNameArr) && !ObjectUtils.isEmpty(pharmcyNameArr[i])) {
                            PharmacyTcm pharmacyTcm = new PharmacyTcm();
                            pharmacyTcm.setOrganId(organId);
                            pharmacyTcm.setPharmacyCode(pharmacyCode);
                            pharmacyTcm.setPharmacyName(pharmcyNameArr[i]);
                            pharmacyTcm.setPharmacyCategray("中药,西药,中成药,膏方");
                            pharmacyTcm.setDrugFormType(RecipeDrugFormTypeEnum.TCM_DECOCTION_PIECES.getType().toString());
                            pharmacyTcm.setWhDefault(false);
                            pharmacyTcm.setSort(1000);
                            boolean b = pharmacyTcmService.addPharmacyTcmForOrgan(pharmacyTcm);
                            if (b) {
                                PharmacyTcm pharmacyTcm1 = pharmacyTcmService.querPharmacyTcmByOrganIdAndName2(organId, pharmcyNameArr[i]);
                                if (i != pharmcyCodeArr.length - 1) {
                                    pharmacys.append(pharmacyTcm1.getPharmacyId() + ",");
                                } else {
                                    pharmacys.append(pharmacyTcm1.getPharmacyId());
                                }
                            }
                        }

                    }
                }
            }
            organDrug.setPharmacy(pharmacys.toString());
        }

        //监管平台药品编码
        if (!ObjectUtils.isEmpty(drug.getRegulationDrugCode())) {
            organDrug.setRegulationDrugCode(drug.getRegulationDrugCode());
        } else {
            if (ObjectUtils.isEmpty(organDrug.getRegulationDrugCode())) {
                IHisServiceConfigService configService = AppContextHolder.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
                Boolean regulationFlag = configService.getRegulationFlag();
                if (regulationFlag) {
                    organDrug.setRegulationDrugCode(organDrug.getDrugId().toString());
                }
            }
        }
        //是否基药
        if (!ObjectUtils.isEmpty(drug.getBaseDrug())) {
            organDrug.setBaseDrug(drug.getBaseDrug());
        }
        //批准文号
        if (!ObjectUtils.isEmpty(drug.getLicenseNumber())) {
            organDrug.setLicenseNumber(drug.getLicenseNumber());
        }
        //包装材料
        if (!ObjectUtils.isEmpty(drug.getPackingMaterials())) {
            organDrug.setPackingMaterials(drug.getPackingMaterials());
        }
        //医保剂型编码
        if (!ObjectUtils.isEmpty(drug.getMedicalDrugFormCode())) {
            organDrug.setMedicalDrugFormCode(drug.getMedicalDrugFormCode());
        }
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.medicalInsuranceControl))
                && !ObjectUtils.isEmpty(drug.getMedicalInsuranceControl())) {
            organDrug.setMedicalInsuranceControl(drug.getMedicalInsuranceControl());
        }
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.indicationsDeclare))
                && !ObjectUtils.isEmpty(drug.getIndicationsDeclare())) {
            organDrug.setIndicationsDeclare(drug.getIndicationsDeclare());
        }
        //药品嘱托
        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.drugEntrust))
                && !ObjectUtils.isEmpty(drug.getDrugEntrust())) {
            organDrug.setDrugEntrust(drug.getDrugEntrust());
        }
        if (Objects.nonNull(drug.getSmallestUnitUseDose())) {
            organDrug.setSmallestUnitUseDose(drug.getSmallestUnitUseDose());
        }
        if (Objects.nonNull(drug.getDefaultSmallestUnitUseDose())) {
            organDrug.setDefaultSmallestUnitUseDose(drug.getDefaultSmallestUnitUseDose());
        }
        if (StringUtils.isNotEmpty(drug.getUseDoseSmallestUnit())) {
            organDrug.setUseDoseSmallestUnit(drug.getUseDoseSmallestUnit());
        }

        if(drug.getSkinTestDrugFlag() != null){
            organDrug.setSkinTestDrugFlag(drug.getSkinTestDrugFlag());
        }

        if(drug.getNationalStandardDrugFlag() != null){
            organDrug.setNationalStandardDrugFlag(drug.getNationalStandardDrugFlag());
        }

        if(StringUtils.isNotEmpty(drug.getHisDrugClassCode())){
            organDrug.setHisDrugClassCode(drug.getHisDrugClassCode());
        }

        if(StringUtils.isNotEmpty(drug.getHisDrugClassName())){
            organDrug.setHisDrugClassName(drug.getHisDrugClassName());
        }

        if(drug.getMaximum() != null){
            organDrug.setMaximum(drug.getMaximum());
        }

//        if(drug.getColdChainTransportationFlag() != null){
//            organDrug.setColdChainTransportationFlag(drug.getColdChainTransportationFlag());
//        }

        if (isAllowSyncField(organDrugListSyncFieldMap.get(SyncDrugConstant.drugsEnterpriseIds))
                && !ObjectUtils.isEmpty(drug.getDrugsEnterpriseCode())) {
            String drugsEnterpriseCodeHis = drug.getDrugsEnterpriseCode();

            String[] drugsEnterpriseCodeHisArr = drugsEnterpriseCodeHis.split(",");
            StringBuilder ss = new StringBuilder();
            String drugsEnterpriseIdsDb = organDrug.getDrugsEnterpriseIds();
            for (int i = 0; i < drugsEnterpriseCodeHisArr.length; i++) {
                DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                DrugsEnterprise drugsEnterpriseDb = drugsEnterpriseDAO.getByEnterpriseCode(drugsEnterpriseCodeHisArr[i], organId);
                if (ObjectUtils.isEmpty(drugsEnterpriseDb)) {
                    throw new DAOException(DAOException.VALUE_NEEDED, "平台根据药企编码" + drugsEnterpriseCodeHisArr[i] + " 未找到药企");
                } else {
                    if (ObjectUtils.isEmpty(drugsEnterpriseIdsDb)) {
                        //数据库没值
                        if (i != drugsEnterpriseCodeHisArr.length - 1) {
                            ss.append(drugsEnterpriseDb.getId().toString() + ",");
                        } else {
                            ss.append(drugsEnterpriseDb.getId().toString());
                        }
                    } else {
                        drugsEnterpriseIdsDb = addOne(drugsEnterpriseIdsDb, drugsEnterpriseDb.getId());
                    }
                }
            }
            if (!StringUtils.isEmpty(ss)) {
                organDrug.setDrugsEnterpriseIds(ss.toString());
            } else {
                if (null != drugsEnterpriseIdsDb) {
                    organDrug.setDrugsEnterpriseIds(drugsEnterpriseIdsDb);
                }

            }

        }
        //使用状态 0 无效 1 有效
        if (!ObjectUtils.isEmpty(drug.getStatus())) {
            organDrug.setStatus(drug.getStatus());
        }
        organDrug.setLastModify(new Date());
        if (StringUtils.isNotEmpty(drug.getDrugItemCode())) {
            organDrug.setDrugItemCode(drug.getDrugItemCode());
        }
        if (!ObjectUtils.isEmpty(drug.getTargetedDrugType())) {
            organDrug.setTargetedDrugType(drug.getTargetedDrugType());
        }
        if (!ObjectUtils.isEmpty(drug.getSmallestSaleMultiple())) {
            organDrug.setSmallestSaleMultiple(drug.getSmallestSaleMultiple());
        }
        if (!ObjectUtils.isEmpty(drug.getRecommendedUseDose())) {
            organDrug.setRecommendedUseDose(drug.getRecommendedUseDose());
        }
        if (!ObjectUtils.isEmpty(drug.getUsePathways())) {
            organDrug.setUsePathways(drug.getUsePathways());
            IUsePathwaysService bean = AppContextHolder.getBean("basic.usePathwaysService", IUsePathwaysService.class);
            List<UsePathwaysDTO> allUsePathwaysByOrganId = bean.findAllUsePathwaysByOrganId(organId);
            if (ObjectUtils.isEmpty(allUsePathwaysByOrganId)) {
                UsePathwaysDTO usePathwaysDTO = bean.findUsePathwaysByOrganAndKey(0, drug.getUsePathways());
                if (ObjectUtils.isEmpty(usePathwaysDTO)) {
                    String error = "usePathways:" + drug.getUsePathways() + " 平台未找到该用药途径!";
                    throw new DAOException(DAOException.VALUE_NEEDED, error);
                } else {
                    organDrug.setUsePathwaysId(usePathwaysDTO.getId().toString());
                }
            } else {
                UsePathwaysDTO usePathwaysDTO = bean.findUsePathwaysByOrganAndKey(organId, drug.getUsePathways());
                if (ObjectUtils.isEmpty(usePathwaysDTO)) {
                    String error = "usePathways:" + drug.getUsePathways() + " 机构未找到该用药途径!";
                    throw new DAOException(DAOException.VALUE_NEEDED, error);
                } else {
                    organDrug.setUsePathwaysId(usePathwaysDTO.getId().toString());
                }
            }
        }
        if (!ObjectUtils.isEmpty(drug.getUsingRate())) {
            organDrug.setUsingRate(drug.getUsingRate());
            IUsingRateService bean = AppContextHolder.getBean("basic.usingRateService", IUsingRateService.class);
            List<UsingRateDTO> allusingRateByOrganId = bean.findAllusingRateByOrganId(organId);
            if (ObjectUtils.isEmpty(allusingRateByOrganId)) {
                UsingRateDTO usingRateDTO = bean.findUsingRateDTOByOrganAndKey(0, drug.getUsingRate());
                if (ObjectUtils.isEmpty(usingRateDTO)) {
                    String error = "usingRate:" + drug.getUsingRate() + " 平台未找到该用药频次!";
                    throw new DAOException(DAOException.VALUE_NEEDED, error);
                } else {
                    organDrug.setUsingRateId(usingRateDTO.getId().toString());
                }

            } else {
                UsingRateDTO usingRateDTOByOrganAndKey = bean.findUsingRateDTOByOrganAndKey(organId, drug.getUsingRate());
                if (ObjectUtils.isEmpty(usingRateDTOByOrganAndKey)) {
                    String error = "usingRate:" + drug.getUsingRate() + " 机构未找到该用药频次!";
                    throw new DAOException(DAOException.VALUE_NEEDED, error);
                } else {
                    organDrug.setUsingRateId(usingRateDTOByOrganAndKey.getId().toString());
                }
            }
        }

        OrganDrugList update = organDrugListDAO.update(organDrug);
        //同步药品到监管备案
        RecipeBusiThreadPool.submit(() -> {
            organDrugListService.uploadDrugToRegulation(update);
            return null;
        });
        LOGGER.info("drugInfoSynMovement updateHisDrug" + update.getDrugName() + "organId=[{}] drug=[{}]", organId, JSONUtils.toString(update));
        try {
            drugToolService.organDrugSync(update);
        } catch (Exception e) {
            LOGGER.info("机构药品手动同步修改同步对应药企" + e);

        }
        DataSyncDTO dataSyncDTO = convertDataSyn(drug, organId, 2, null, 2, null);
        List<DataSyncDTO> syncDTOList = Lists.newArrayList();
        syncDTOList.add(dataSyncDTO);
        dataSyncLogService.addDataSyncLog("1", syncDTOList);
    }


    /**
     * 如果存在就返回原药企串，不存在就添加一个
     *
     * @param
     * @param
     * @return
     */
    public static String addOne(String drugsEnterpriseIdsDb, Integer drugEnterpriseId) {
        // 返回结果
        String result = "";
        // 判断是否存在。如果存在，移除指定药房 ID；如果不存在，则直接返回空
        // 拆分成数组
        String[] drugsEnterpriseIdsDbArr = drugsEnterpriseIdsDb.split(",");
        // 数组转集合
        List<String> drugsEnterpriseIdsDbList = new ArrayList<String>(Arrays.asList(drugsEnterpriseIdsDbArr));
        if (!drugsEnterpriseIdsDbList.contains(String.valueOf(drugEnterpriseId))) {
            // 添加指定药企 ID
            drugsEnterpriseIdsDbList.add(drugEnterpriseId.toString());
            // 把剩下的药企 ID 再拼接起来
            result = com.aliyun.openservices.shade.org.apache.commons.lang3.StringUtils.join(drugsEnterpriseIdsDbList, ",");
        } else {
            result = drugsEnterpriseIdsDb;
        }
        // 返回
        return result;
    }

    /**
     * 当前同步药品数据
     * TODO 凌晨一点
     *
     * @param drug
     * @param organDrug
     */
    private void updateHisDrug(DrugInfoTO drug, OrganDrugList organDrug, Integer organId) {
        if (null == organDrug) {
            return;
        }
        //获取金额
        if (StringUtils.isNotEmpty(drug.getDrugPrice())) {
            BigDecimal drugPrice = new BigDecimal(drug.getDrugPrice());
            organDrug.setSalePrice(drugPrice);
        }
        //药品规格
        if (StringUtils.isNotEmpty(drug.getDrmodel())) {
            organDrug.setDrugSpec(drug.getDrmodel());
        }
        //医保药品编码
        if (StringUtils.isNotEmpty(drug.getMedicalDrugCode())) {
            organDrug.setMedicalDrugCode(drug.getMedicalDrugCode());
        }
        //转换系数
        if (StringUtils.isNotEmpty(drug.getPack())) {
            organDrug.setPack(Integer.valueOf(drug.getPack()));
        }
        //生产厂家
        if (StringUtils.isNotEmpty(drug.getProducer())) {
            organDrug.setProducer(drug.getProducer());
        }
        //商品名称
        if (StringUtils.isNotEmpty(drug.getTradename())) {
            organDrug.setSaleName(drug.getTradename());
        }
        //通用名
        if (StringUtils.isNotEmpty(drug.getDrname())) {
            organDrug.setDrugName(drug.getDrname());
        }
        //药品包装单位
        if (StringUtils.isNotEmpty(drug.getPackUnit())) {
            organDrug.setUnit(drug.getPackUnit());
        }
        //实际单次剂量（规格单位）
        if (!ObjectUtils.isEmpty(drug.getUseDose())) {
            organDrug.setUseDose(drug.getUseDose());
        }
        //推荐单次剂量（规格单位）
        if (!ObjectUtils.isEmpty(drug.getRecommendedUseDose())) {
            organDrug.setRecommendedUseDose(drug.getRecommendedUseDose());
        }
        //单次剂量单位（规格单位）
        if (!ObjectUtils.isEmpty(drug.getUseDoseUnit())) {
            organDrug.setUseDoseUnit(drug.getUseDoseUnit());
        }
        //实际单位剂量（最小单位）
        if (!ObjectUtils.isEmpty(drug.getSmallestUnitUseDose())) {
            organDrug.setSmallestUnitUseDose(drug.getSmallestUnitUseDose());
        }
        //默认单位剂量（最小单位）
        if (!ObjectUtils.isEmpty(drug.getDefaultSmallestUnitUseDose())) {
            organDrug.setDefaultSmallestUnitUseDose(drug.getDefaultSmallestUnitUseDose());
        }
        //单位剂量单位（最小单位）
        if (!ObjectUtils.isEmpty(drug.getUseDoseSmallestUnit())) {
            organDrug.setUseDoseSmallestUnit(drug.getUseDoseSmallestUnit());
        }
        //使用频率平台
        if (!ObjectUtils.isEmpty(drug.getUsingRate())) {
            organDrug.setUsingRate(drug.getUsingRate());
        }
        //用药途径平台
        if (!ObjectUtils.isEmpty(drug.getUsePathways())) {
            organDrug.setUsePathways(drug.getUsePathways());
        }
        //搜索关键字，一般包含通用名，商品名及医院自定义值
        if (!ObjectUtils.isEmpty(drug.getSearchKey())) {
            organDrug.setSearchKey(drug.getSearchKey());
        }
        //使用状态 0 无效 1 有效
        if (!ObjectUtils.isEmpty(drug.getStatus())) {
            organDrug.setStatus(drug.getStatus());
        }
        //生产厂家代码
        if (!ObjectUtils.isEmpty(drug.getProducerCode())) {
            organDrug.setProducerCode(drug.getProducerCode());
        }
        //外带药标志 1:外带药
        if (!ObjectUtils.isEmpty(drug.getTakeMedicine())) {
            organDrug.setTakeMedicine(drug.getTakeMedicine());
        }
        //院内检索关键字
        if (!ObjectUtils.isEmpty(drug.getRetrievalCode())) {
            organDrug.setRetrievalCode(drug.getRetrievalCode());
        }
        //单复方
        if (!ObjectUtils.isEmpty(drug.getUnilateralCompound())) {
            organDrug.setUnilateralCompound(drug.getUnilateralCompound());
        }
        //药房
        if (!ObjectUtils.isEmpty(drug.getPharmacyCode())) {
            String pharmacyCode = drug.getPharmacyCode();
            PharmacyTcm byPharmacyAndOrganId = pharmacyTcmDAO.getByPharmacyAndOrganId(pharmacyCode, organId);
            if (byPharmacyAndOrganId != null) {
                organDrug.setPharmacy(byPharmacyAndOrganId.getPharmacyId().toString());
            } else {
                if (!ObjectUtils.isEmpty(drug.getPharmacy())) {
                    PharmacyTcm pharmacyTcm = new PharmacyTcm();
                    pharmacyTcm.setOrganId(organId);
                    pharmacyTcm.setPharmacyCode(drug.getPharmacyCode());
                    pharmacyTcm.setPharmacyName(drug.getPharmacy());
                    pharmacyTcm.setWhDefault(false);
                    pharmacyTcm.setSort(1000);
                    boolean b = pharmacyTcmService.addPharmacyTcmForOrgan(pharmacyTcm);
                    if (b) {
                        PharmacyTcm pharmacyTcm1 = pharmacyTcmService.querPharmacyTcmByOrganIdAndName2(organId, drug.getPharmacy());
                        organDrug.setPharmacy(pharmacyTcm1.getPharmacyId().toString());
                    }
                }
            }
        }
        //是否抗肿瘤药物,及抗肿瘤药物等级
        if (!ObjectUtils.isEmpty(drug.getAntiTumorDrugFlag())) {
            if (new Integer(1).equals(drug.getAntiTumorDrugFlag()) && ObjectUtils.isEmpty(drug.getAntiTumorDrugLevel())) {
                throw new DAOException(DAOException.VALUE_NEEDED, "药品为抗肿瘤药物时，抗肿瘤药物等级必填!");
            } else {
                organDrug.setAntiTumorDrugFlag(drug.getAntiTumorDrugFlag());
                if (!ObjectUtils.isEmpty(drug.getAntiTumorDrugLevel())) {
                    organDrug.setAntiTumorDrugLevel(drug.getAntiTumorDrugLevel());
                }
            }
        }
        //药品本位码
        if (!ObjectUtils.isEmpty(drug.getStandardCode())) {
            organDrug.setStandardCode(drug.getStandardCode());
        }
        //抗菌素药物
        if (!ObjectUtils.isEmpty(drug.getAntibioticsDrugLevel())) {
            organDrug.setAntibioticsDrugLevel(drug.getAntibioticsDrugLevel());
        }
        //是否精神药物
        if (!ObjectUtils.isEmpty(drug.getPsychotropicDrugFlag())) {
            organDrug.setPsychotropicDrugFlag(drug.getPsychotropicDrugFlag());
        }
        //是否麻醉药物
        if (!ObjectUtils.isEmpty(drug.getNarcoticDrugFlag())) {
            organDrug.setNarcoticDrugFlag(drug.getNarcoticDrugFlag());
        }
        //是否毒性药物
        if (!ObjectUtils.isEmpty(drug.getToxicDrugFlag())) {
            organDrug.setToxicDrugFlag(drug.getToxicDrugFlag());
        }
        //是否放射性药物
        if (!ObjectUtils.isEmpty(drug.getRadioActivityDrugFlag())) {
            organDrug.setRadioActivityDrugFlag(drug.getRadioActivityDrugFlag());
        }
        //是否特殊使用级抗生素药物
        if (!ObjectUtils.isEmpty(drug.getSpecialUseAntibioticDrugFlag())) {
            organDrug.setSpecialUseAntibioticDrugFlag(drug.getSpecialUseAntibioticDrugFlag());
        }
        if (!ObjectUtils.isEmpty(drug.getUnitHisCode())) {
            organDrug.setUnitHisCode(drug.getUnitHisCode());
        }

        if (!ObjectUtils.isEmpty(drug.getUseDoseUnitHisCode())) {
            organDrug.setUseDoseUnitHisCode(drug.getUseDoseUnitHisCode());
        }

        if (!ObjectUtils.isEmpty(drug.getUseDoseSmallestUnitHisCode())) {
            organDrug.setUseDoseSmallestUnitHisCode(drug.getUseDoseSmallestUnitHisCode());
        }

        if(drug.getSkinTestDrugFlag() != null){
            organDrug.setSkinTestDrugFlag(drug.getSkinTestDrugFlag());
        }

        if(drug.getNationalStandardDrugFlag() != null){
            organDrug.setNationalStandardDrugFlag(drug.getNationalStandardDrugFlag());
        }

        if(StringUtils.isNotEmpty(drug.getHisDrugClassCode())){
            organDrug.setHisDrugClassCode(drug.getHisDrugClassCode());
        }

        if(StringUtils.isNotEmpty(drug.getHisDrugClassName())){
            organDrug.setHisDrugClassName(drug.getHisDrugClassName());
        }

        if(drug.getMaximum() != null){
            organDrug.setMaximum(drug.getMaximum());
        }

//        if(drug.getColdChainTransportationFlag() != null){
//            organDrug.setColdChainTransportationFlag(drug.getColdChainTransportationFlag());
//        }

        //医院药房名字
        if (!ObjectUtils.isEmpty(drug.getPharmacy())) {
            organDrug.setPharmacyName(drug.getPharmacy());
        }
        //监管平台药品编码
        if (!ObjectUtils.isEmpty(drug.getRegulationDrugCode())) {
            organDrug.setRegulationDrugCode(drug.getRegulationDrugCode());
        } else {
            if (ObjectUtils.isEmpty(organDrug.getRegulationDrugCode())) {
                IHisServiceConfigService configService = AppContextHolder.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
                Boolean regulationFlag = configService.getRegulationFlag();
                if (regulationFlag) {
                    organDrug.setRegulationDrugCode(organDrug.getDrugId().toString());
                }
            }
        }
        //剂型
        if (!ObjectUtils.isEmpty(drug.getDrugForm())) {
            organDrug.setDrugForm(drug.getDrugForm());
            organDrug.setHisDrugForm(drug.getDrugForm());
        }
        //是否基药
        if (!ObjectUtils.isEmpty(drug.getBaseDrug())) {
            organDrug.setBaseDrug(drug.getBaseDrug());
        }
        //批准文号
        if (!ObjectUtils.isEmpty(drug.getLicenseNumber())) {
            organDrug.setLicenseNumber(drug.getLicenseNumber());
        }
        //包装材料
        if (!ObjectUtils.isEmpty(drug.getPackingMaterials())) {
            organDrug.setPackingMaterials(drug.getPackingMaterials());
        }
        //HIS剂型编码
        if (!ObjectUtils.isEmpty(drug.getDrugFormCode())) {
            organDrug.setDrugFormCode(drug.getDrugFormCode());
        }
        if (!ObjectUtils.isEmpty(drug.getMedicalInsuranceControl())) {
            organDrug.setMedicalInsuranceControl(drug.getMedicalInsuranceControl());
        }
        if (!ObjectUtils.isEmpty(drug.getIndicationsDeclare())) {
            organDrug.setIndicationsDeclare(drug.getIndicationsDeclare());
        }
        //药品嘱托
        if (!ObjectUtils.isEmpty(drug.getDrugEntrust())) {
            organDrug.setDrugEntrust(drug.getDrugEntrust());
        }
        //医保剂型编码
        if (!ObjectUtils.isEmpty(drug.getMedicalDrugFormCode())) {
            organDrug.setMedicalDrugFormCode(drug.getMedicalDrugFormCode());
        }
        if (StringUtils.isNotEmpty(drug.getDrugItemCode())) {
            organDrug.setDrugItemCode(drug.getDrugItemCode());
        }

        if (Objects.nonNull(drug.getSmallestUnitUseDose())) {
            organDrug.setSmallestUnitUseDose(drug.getSmallestUnitUseDose());
        }
        if (Objects.nonNull(drug.getDefaultSmallestUnitUseDose())) {
            organDrug.setDefaultSmallestUnitUseDose(drug.getDefaultSmallestUnitUseDose());
        }
        if (StringUtils.isNotEmpty(drug.getUseDoseSmallestUnit())) {
            organDrug.setUseDoseSmallestUnit(drug.getUseDoseSmallestUnit());
        }
        if (Objects.nonNull(drug.getTargetedDrugType())) {
            organDrug.setTargetedDrugType(drug.getTargetedDrugType());
        }
        if (Objects.nonNull(drug.getSmallestSaleMultiple())) {
            organDrug.setSmallestSaleMultiple(drug.getSmallestSaleMultiple());
        }
        if (!ObjectUtils.isEmpty(drug.getUsePathways())) {
            organDrug.setUsePathways(drug.getUsePathways());
            IUsePathwaysService bean = AppContextHolder.getBean("basic.usePathwaysService", IUsePathwaysService.class);
            List<UsePathwaysDTO> allUsePathwaysByOrganId = bean.findAllUsePathwaysByOrganId(organId);
            if (ObjectUtils.isEmpty(allUsePathwaysByOrganId)) {
                UsePathwaysDTO usePathwaysDTO = bean.findUsePathwaysByOrganAndKey(0, drug.getUsePathways());
                if (ObjectUtils.isEmpty(usePathwaysDTO)) {
                    String error = "usePathways:" + drug.getUsePathways() + " 平台未找到该用药途径!";
                    throw new DAOException(DAOException.VALUE_NEEDED, error);
                } else {
                    organDrug.setUsePathwaysId(usePathwaysDTO.getId().toString());
                }
            } else {
                UsePathwaysDTO usePathwaysDTO = bean.findUsePathwaysByOrganAndKey(organId, drug.getUsePathways());
                if (ObjectUtils.isEmpty(usePathwaysDTO)) {
                    String error = "usePathways:" + drug.getUsePathways() + " 机构未找到该用药途径!";
                    throw new DAOException(DAOException.VALUE_NEEDED, error);
                } else {
                    organDrug.setUsePathwaysId(usePathwaysDTO.getId().toString());
                }
            }
        }
        if (!ObjectUtils.isEmpty(drug.getUsingRate())) {
            organDrug.setUsingRate(drug.getUsingRate());
            IUsingRateService bean = AppContextHolder.getBean("basic.usingRateService", IUsingRateService.class);
            List<UsingRateDTO> allusingRateByOrganId = bean.findAllusingRateByOrganId(organId);
            if (ObjectUtils.isEmpty(allusingRateByOrganId)) {
                UsingRateDTO usingRateDTO = bean.findUsingRateDTOByOrganAndKey(0, drug.getUsingRate());
                if (ObjectUtils.isEmpty(usingRateDTO)) {
                    String error = "usingRate:" + drug.getUsingRate() + " 平台未找到该用药频次!";
                    throw new DAOException(DAOException.VALUE_NEEDED, error);
                } else {
                    organDrug.setUsingRateId(usingRateDTO.getId().toString());
                }

            } else {
                UsingRateDTO usingRateDTOByOrganAndKey = bean.findUsingRateDTOByOrganAndKey(organId, drug.getUsingRate());
                if (ObjectUtils.isEmpty(usingRateDTOByOrganAndKey)) {
                    String error = "usingRate:" + drug.getUsingRate() + " 机构未找到该用药频次!";
                    throw new DAOException(DAOException.VALUE_NEEDED, error);
                } else {
                    organDrug.setUsingRateId(usingRateDTOByOrganAndKey.getId().toString());
                }
            }
        }
        LOGGER.info("updateHisDrug 更新后药品信息 organDrug：{}", JSONUtils.toString(organDrug));
        OrganDrugList update = organDrugListDAO.update(organDrug);
        //同步药品到监管备案
        RecipeBusiThreadPool.submit(() -> {
            organDrugListService.uploadDrugToRegulation(update);
            return null;
        });
        try {
            drugToolService.organDrugSync(update);
        } catch (Exception e) {
            LOGGER.info("机构药品定时同步修改同步对应药企" + e);

        }
    }

    @RpcService
    public String getThirdRecipeUrl(String mpiId) {
        List<PatientBean> patientBeans = iPatientService.findByMpiIdIn(Arrays.asList(mpiId));
        return getThirdUrlString(patientBeans, "", "");
    }

    public String getThirdUrlString(List<PatientBean> patientBeans, String recipeNo, String patientId) {
        String url = "";
        RecipeParameterDao parameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        if (CollectionUtils.isNotEmpty(patientBeans)) {
            Integer urt = patientBeans.get(0).getUrt();
            PatientService patientService = BasicAPI.getService(PatientService.class);
            List<PatientDTO> patientDTOList = patientService.findPatientByUrt(urt);
            LOGGER.info("recipeService-getThirdRecipeUrl patientBean:{}.", JSONUtils.toString(patientDTOList));
            String pre_url = parameterDao.getByName("yd_thirdurl");
            String yd_hospital_code = parameterDao.getByName("yd_hospital_code");
            List<YdUrlPatient> ydUrlPatients = new ArrayList<>();
            for (PatientDTO patientDTO : patientDTOList) {
                YdUrlPatient ydUrlPatient = new YdUrlPatient();
                String idnum = patientDTO.getIdcard();
                String mobile = patientDTO.getMobile();
                String pname = patientDTO.getPatientName();
                ydUrlPatient.setMobile(mobile);
                ydUrlPatient.setIdnum(idnum);
                ydUrlPatient.setPname(pname);
                if (StringUtils.isNotEmpty(patientId)) {
                    ydUrlPatient.setPno(patientId);
                } else {
                    //查询该用户最新的一条处方记录
                    HospitalRecipeDAO hospitalRecipeDAO = DAOFactory.getDAO(HospitalRecipeDAO.class);
                    List<HospitalRecipe> hospitalRecipes = hospitalRecipeDAO.findByCertificate(patientDTO.getIdcard());
                    if (CollectionUtils.isNotEmpty(hospitalRecipes)) {
                        ydUrlPatient.setPno(hospitalRecipes.get(0).getPatientId());
                    }
                }
                ydUrlPatient.setHisno("");
                ydUrlPatients.add(ydUrlPatient);
            }

            String patient = JSONUtils.toString(ydUrlPatients);
            StringBuilder stringBuilder = new StringBuilder();
            try {
                patient = URLEncoder.encode(patient, "UTF-8");
            } catch (Exception e) {
                LOGGER.error("recipeService-getThirdRecipeUrl url:{}.", JSONUtils.toString(stringBuilder), e);
            }
            stringBuilder.append("?q=").append(patient);
            stringBuilder.append("&h=").append(yd_hospital_code);
            stringBuilder.append("&r=");
            if (StringUtils.isNotEmpty(recipeNo)) {
                stringBuilder.append(recipeNo);
            }
            url = pre_url + stringBuilder.toString();
            LOGGER.info("recipeService-getThirdRecipeUrl url:{}.", JSONUtils.toString(url));
        }
        return url;
    }

    //根据recipeId 判断有没有关联的订单，有订单返回相关的订单id
    //2020春节代码添加
    @RpcService
    public Integer getOrderIdByRecipe(Integer recipeId) {
        LOGGER.info("getOrderIdByRecipe查询处方关联订单，处方id:{}", recipeId);
        //根据recipeId将对应的订单获得，有就返回订单id没有就不返回
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder order = orderDAO.getRelationOrderByRecipeId(recipeId);
        if (null != order) {
            LOGGER.info("getOrderIdByRecipe当前处方关联上订单");
            return order.getOrderId();
        }
        LOGGER.info("getOrderIdByRecipe当前处方没有关联上订单");
        return null;
    }

    //根据recipeId 判断有没有关联处方是否支持配送
    //2020春节代码添加
    @RpcService
    public Boolean recipeCanDelivery(RecipeBean recipe, List<RecipeDetailBean> details) {
        LOGGER.error("recipeCanDelivery 查询处方是否可配送入参：{},{}.", JSON.toJSONString(recipe), JSON.toJSONString(details));
        boolean flag = false;
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = getDAO(RecipeDetailDAO.class);
        RecipeExtendDAO recipeExtendDAO = getDAO(RecipeExtendDAO.class);
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

        recipe.setStatus(RecipeStatusConstant.UNSIGN);
        recipe.setSignDate(DateTime.now().toDate());
        recipe.setChooseFlag(0);
        recipe.setRemindFlag(0);
        recipe.setPushFlag(0);
        recipe.setTakeMedicine(0);
        recipe.setRecipeMode(null == recipe.getRecipeMode() ? "" : recipe.getRecipeMode());
        recipe.setGiveFlag(null == recipe.getGiveFlag() ? 0 : recipe.getGiveFlag());
        recipe.setPayFlag(null == recipe.getPayFlag() ? 0 : recipe.getPayFlag());
        //date 20200226 添加默认值
        recipe.setTotalMoney(null == recipe.getTotalMoney() ? BigDecimal.ZERO : recipe.getTotalMoney());
        //如果是已经暂存过的处方单，要去数据库取状态 判断能不能进行签名操作
        if (null == recipe || null == details || 0 == details.size()) {
            LOGGER.error("recipeCanDelivery 当前处方或者药品信息不全：{},{}.", JSON.toJSONString(recipe), JSON.toJSONString(details));
            return false;
        }
        Recipe dbrecipe = ObjectCopyUtils.convert(recipe, Recipe.class);
        List<Recipedetail> recipedetails = ObjectCopyUtils.convert(details, Recipedetail.class);
        //设置药品价格
        boolean isSucc = RecipeServiceSub.setDetailsInfo(dbrecipe, recipedetails);
        if (!isSucc) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipeCanDelivery-药品详情数据有误");
        }
        Integer recipeId = recipeDAO.updateOrSaveRecipeAndDetail(dbrecipe, recipedetails, false);

        boolean checkEnterprise = drugsEnterpriseService.checkEnterprise(recipe.getClinicOrgan());
        if (checkEnterprise) {
            //药企库存实时查询
            //首先获取机构匹配支持配送的药企列表
            DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
            //筛选出来的数据已经去掉不支持任何方式配送的药企
            List<DrugsEnterprise> drugsEnterprisesHos = drugsEnterpriseDAO.findByOrganIdAndPayModeSupport(recipe.getClinicOrgan(), RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType());
            List<DrugsEnterprise> drugsEnterprisesEnt = drugsEnterpriseDAO.findByOrganIdAndPayModeSupport(recipe.getClinicOrgan(), RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType());
            List<DrugsEnterprise> drugsEnterprises = new ArrayList<>();
            drugsEnterprises.addAll(drugsEnterprisesHos);
            drugsEnterprises.addAll(drugsEnterprisesEnt);

            YtRemoteService ytRemoteService;
            HdRemoteService hdRemoteService;
            for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                AccessDrugEnterpriseService enterpriseService = RemoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
                if (null == enterpriseService) {
                    LOGGER.error("recipeCanDelivery 当前药企没有对接{}.", enterpriseService);
                    continue;
                }
                if (DrugEnterpriseConstant.COMPANY_YT.equals(drugsEnterprise.getCallSys())) {
                    ytRemoteService = (YtRemoteService) enterpriseService;
                    LOGGER.error("recipeCanDelivery 处方[{}]请求药企{}库存", recipeId, drugsEnterprise.getCallSys());
                    if (ytRemoteService.scanStockSend(recipeId, drugsEnterprise)) {
                        flag = true;
                        break;
                    }

                } else if (DrugEnterpriseConstant.COMPANY_HDDYF.equals(drugsEnterprise.getCallSys())) {
                    hdRemoteService = (HdRemoteService) enterpriseService;
                    LOGGER.error("recipeCanDelivery 处方[{}]请求药企{}库存", recipeId, drugsEnterprise.getCallSys());
                    if (hdRemoteService.sendScanStock(recipeId, drugsEnterprise, DrugEnterpriseResult.getFail())) {
                        flag = true;
                        break;
                    }

                } else {
                    LOGGER.error("recipeCanDelivery 处方[{}]请求药企{}库存", recipeId, drugsEnterprise.getCallSys());
                    List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipeId);
                    EnterpriseStock enterpriseStock = stockBusinessService.enterpriseStockCheck(dbrecipe, recipeDetailList, drugsEnterprise.getId(), StockCheckSourceTypeEnum.PATIENT_STOCK.getType());
                    if (enterpriseStock.getStock()) {
                        flag = true;
                        break;
                    }
                }
            }
        }
        if (null != recipeId) {
            LOGGER.info("recipeCanDelivery 处方[{}],删除无用数据中", recipeId);
            recipeDAO.remove(recipeId);
            recipeDetailDAO.deleteByRecipeId(recipeId);
            recipeExtendDAO.remove(recipeId);
        }
        LOGGER.info("recipeCanDelivery 处方[{}],是否支持配送：{}", recipeId, flag);
        return flag;
    }

    /**
     * 开处方时，通过年龄判断是否能够开处方
     * mpiid
     *
     * @return Map<String, Object>
     */
    @RpcService
    public Map<String, Object> findCanRecipeByAge(Map<String, String> params) {
        LOGGER.info("findCanRecipeByAge 参数{}", JSONUtils.toString(params));
        if (StringUtils.isEmpty(params.get("mpiid"))) {
            throw new DAOException("findCanRecipeByAge mpiid不允许为空");
        }
        if (StringUtils.isEmpty(params.get("organId"))) {
            throw new DAOException("findCanRecipeByAge organId不允许为空");
        }
        Map<String, Object> map = Maps.newHashMap();
        boolean canRecipe = false;//默认不可开处方
        //从opbase配置项获取允许开处方患者年龄 findCanRecipeByAge
        IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        Integer findCanRecipeByAge = (Integer) configService.getConfiguration(Integer.parseInt(params.get("organId")), "findCanRecipeByAge");
        LOGGER.info("findCanRecipeByAge 从opbase配置项获取允许开处方患者年龄{}", findCanRecipeByAge);
        if (findCanRecipeByAge == null) {
            canRecipe = true;//查询不到设置值或默认值或没配置配置项 设置可开处方
        }
        if (!canRecipe) {
            //从opbase获取患者数据
            List<String> findByMpiIdInParam = new ArrayList<>();
            findByMpiIdInParam.add(params.get("mpiid"));
            List<PatientDTO> patientList = patientService.findByMpiIdIn(findByMpiIdInParam);
            if (patientList != null && patientList.size() > 0) {
                //通过生日获取患者年龄
                Integer age = 0;
                try {
                    if (patientList.get(0) != null && patientList.get(0).getBirthday() != null) {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        age = ChinaIDNumberUtil.getAgeFromBirth(simpleDateFormat.format(patientList.get(0).getBirthday()));
                    }
                    LOGGER.info("findCanRecipeByAge 通过证件号码获取患者年龄{}", age);
                } catch (ValidateException e) {
                    LOGGER.error("findCanRecipeByAge 通过证件号码获取患者年龄异常" + e.getMessage(), e);
                    e.printStackTrace();
                }
                //实际年龄>=配置年龄 设置可开处方
                if (age >= findCanRecipeByAge) {
                    canRecipe = true;
                }
            }

        }
        map.put("canRecipe", canRecipe);
        map.put("canRecipeAge", findCanRecipeByAge);
        return map;
    }


    /**
     * 根据organid 获取长处方按钮是否开启、开药天数范围
     *
     * @return Map<String, Object>
     */
    @RpcService
    public Map<String, Object> findisCanOpenLongRecipeAndUseDayRange(Map<String, String> params) {
        LOGGER.info("findisCanOpenLongRecipeAndUseDayRange 参数{}", JSONUtils.toString(params));
        if (StringUtils.isEmpty(params.get("organId"))) {
            throw new DAOException("findUseDayRange organId不允许为空");
        }
        Map<String, Object> map = Maps.newHashMap();

        //获取长处方配置
        IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        Object isCanOpenLongRecipe = configService.getConfiguration(Integer.parseInt(params.get("organId")), "isCanOpenLongRecipe");
        LOGGER.info("findisCanOpenLongRecipeAndUseDayRange 从opbase配置项获取是否能开长处方{}", isCanOpenLongRecipe);
        if (isCanOpenLongRecipe == null || !(boolean) isCanOpenLongRecipe) {//按钮没配置或关闭
        }
        if ((boolean) isCanOpenLongRecipe) {//按钮开启
            Object yesLongRecipe = configService.getConfiguration(Integer.parseInt(params.get("organId")), "yesLongRecipe");
            LOGGER.info("findisCanOpenLongRecipeAndUseDayRange 从opbase配置项获取长处方开药天数范围是{}", yesLongRecipe == null ? yesLongRecipe : ((String) yesLongRecipe).replace(",", "-"));
            map.put("longTimeRange", yesLongRecipe == null ? yesLongRecipe : ((String) yesLongRecipe).replace(",", "-"));
            Object noLongRecipe = configService.getConfiguration(Integer.parseInt(params.get("organId")), "noLongRecipe");
            LOGGER.info("findisCanOpenLongRecipeAndUseDayRange 从opbase配置项获取非长处方开药天数范围是{}", noLongRecipe == null ? noLongRecipe : ((String) noLongRecipe).replace(",", "-"));
            map.put("shortTimeRange", noLongRecipe == null ? noLongRecipe : ((String) noLongRecipe).replace(",", "-"));
        }
        map.put("canOpenLongRecipe", isCanOpenLongRecipe);

        //获取用药天数配置
        Object isLimitUseDays = configService.getConfiguration(Integer.parseInt(params.get("organId")), "isLimitUseDays");
        LOGGER.info("findisCanOpenLongRecipeAndUseDayRange 从opbase配置项获取是否开启用药天数配置{}", isLimitUseDays);
        if ((boolean) isLimitUseDays) {//按钮开启
            Object useDaysRange = configService.getConfiguration(Integer.parseInt(params.get("organId")), "useDaysRange");
            LOGGER.info("findisCanOpenLongRecipeAndUseDayRange 从opbase配置项获取用药天数配置天数范围是{}", useDaysRange == null ? useDaysRange : ((String) useDaysRange).replace(",", "-"));
            map.put("useDaysRange", useDaysRange == null ? useDaysRange : ((String) useDaysRange).replace(",", "-"));
        }
        map.put("limitUseDays", isLimitUseDays);

        return map;
    }


    @RpcService
    private Map<String, String> getRevisitType() {
        Map<String, String> map = new HashMap<>();
        map.put("0", "自费");
        map.put("1", "普通保险");
        map.put("2", "门特保险");
        return map;
    }

    public void doAfterCheckNotPassYs(Recipe recipe) {
        LOGGER.info("RecipeService doAfterCheckNotPassYs recipeId= {}，clinicOrgan={}", recipe.getRecipeId(), recipe.getClinicOrgan());
        boolean secondSignFlag = RecipeServiceSub.canSecondAudit(recipe.getClinicOrgan());
        if (!secondSignFlag) {
            //不支持二次签名的机构直接执行后续操作, 一次审核不通过的需要将优惠券释放
            RecipeCouponService recipeCouponService = ApplicationUtils.getRecipeService(RecipeCouponService.class);
            recipeCouponService.unuseCouponByRecipeId(recipe.getRecipeId());
            //TODO 根据审方模式改变
            auditModeContext.getAuditModes(recipe.getReviewType()).afterCheckNotPassYs(recipe);
            //审核不通过 往his更新状态（已取消）
            if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
                //互联网模式
                RecipeToHisMqService hisMqService = ApplicationUtils.getRecipeService(RecipeToHisMqService.class);
                hisMqService.recipeStatusToHis(HisMqRequestInit.initRecipeStatusToHisReq(recipe,
                        HisBussConstant.TOHIS_RECIPE_STATUS_REVOKE));
            } else {
                //平台模式
                RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
                hisService.recipeStatusUpdate(recipe.getRecipeId());
            }

            //记录日志
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "审核不通过处理完成");
        } else {
            //需要二次审核，这里是一次审核不通过的流程
            //需要将处方的审核状态设置成一次审核不通过的状态
            Map<String, Object> updateMap = new HashMap<>();
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            updateMap.put("checkStatus", RecipecCheckStatusConstant.First_Check_No_Pass);
            recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), updateMap);
            stateManager.updateAuditState(recipe.getRecipeId(), RecipeAuditStateEnum.FAIL_DOC_CONFIRMING);
            stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_AUDIT, RecipeStateEnum.SUB_AUDIT_DOCTOR_READY);
        }
        //由于支持二次签名的机构第一次审方不通过时医生收不到消息。所以将审核不通过推送消息放这里处理
        sendCheckNotPassYsMsg(recipe);
    }

    private void sendCheckNotPassYsMsg(Recipe recipe) {
        RecipeDAO rDao = DAOFactory.getDAO(RecipeDAO.class);
        if (null == recipe) {
            return;
        }
        recipe = rDao.get(recipe.getRecipeId());
        if (RecipeBussConstant.FROMFLAG_HIS_USE.equals(recipe.getFromflag())) {
            //发送审核不成功消息
            //${sendOrgan}：抱歉，您的处方未通过药师审核。如有收取费用，款项将为您退回，预计1-5个工作日到账。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
            RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_YS_CHECKNOTPASS_4HIS, recipe);
            //date 2019/10/10
            //添加判断 一次审核不通过不需要向患者发送消息
        } else if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(recipe.getFromflag())) {
            //发送审核不成功消息
            //处方审核不通过通知您的处方单审核不通过，如有疑问，请联系开方医生
            RecipeMsgService.batchSendMsg(recipe, eh.cdr.constant.RecipeStatusConstant.CHECK_NOT_PASSYS_REACHPAY);
        }
    }

    @RpcService
    public String queryRecipeGetUrl(Integer recipeId) {
        //根据选中的处方信息，获取对应处方的处方笺医院的url

        //根据当前机构配置的pdfurl组装处方信息到动态外链上
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            LOGGER.warn("queryRecipeGetUrl-当前处方{}不存在", recipeId);
            return null;
        }
        RecipeExtendDAO extendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = extendDAO.getByRecipeId(recipeId);
        if (null == recipeExtend) {
            LOGGER.warn("queryRecipeGetUrl-当前处方扩展信息{}不存在", recipeId);
        }
        //获取当前机构配置的处方笺下载链接
        Object downPrescriptionUrl = configService.getConfiguration(recipe.getClinicOrgan(), "downPrescriptionUrl");
        if (null == downPrescriptionUrl) {
            LOGGER.warn("queryRecipeGetUrl-当前机构下{}获取处方笺url的配置为空", recipe.getClinicOrgan());
            return null;
        }
        //根据recipe信息组装url的动态链接
        Map<String, Object> paramMap = Maps.newHashMap();
        paramMap.put("registerID", null != recipeExtend ? recipeExtend.getRegisterID() : null);
        paramMap.put("recipeCode", recipe.getRecipeCode());
        paramMap.put("patientID", recipe.getPatientID());
        paramMap.put("cardNo", null != recipeExtend ? recipeExtend.getCardNo() : null);
        paramMap.put("cardType", null != recipeExtend ? recipeExtend.getCardType() : null);
        LOGGER.info("queryRecipeGetUrl-当前处方动态外链组装的入参{}", JSONObject.toJSONString(paramMap));
        String resultUrl = LocalStringUtil.processTemplate((String) downPrescriptionUrl, paramMap);
        return resultUrl;
    }


    /**
     * 定时任务:定时取消处方的(便捷购药除外)
     */
    @RpcService
    public void cancelSignRecipeTask() {
        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

        //设置查询时间段
        String endDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(Integer.parseInt(cacheService.getParam(ParameterConstant.KEY_RECIPE_VALIDDATE_DAYS, RECIPE_EXPIRED_DAYS.toString()))), DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(Integer.parseInt(cacheService.getParam(ParameterConstant.KEY_RECIPE_CANCEL_DAYS, RECIPE_EXPIRED_SEARCH_DAYS.toString()))), DateConversion.DEFAULT_DATE_TIME);

        //筛选处方状态是签名中和签名失败的
        List<Recipe> recipeList = recipeDAO.getRecipeListForSignCancelRecipe(startDt, endDt);
        //这里要取消处方的首先判断处方的状态是
        //取消处方的步骤：1.判断处方
        LOGGER.info("cancelSignRecipeTask 取消的ca签名处方列表{}", JSONUtils.toString(recipeList));
        RecipeOrder order = new RecipeOrder();
        StringBuilder memo = new StringBuilder();
        Integer status;
        List<Integer> recipeIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(recipeList)) {
            for (Recipe recipe : recipeList) {
                if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
                    OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
                    List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
                    for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                        if (("aldyf".equals(drugsEnterprise.getCallSys()) || "tmdyf".equals(drugsEnterprise.getCallSys())) && recipe.getPushFlag() == 1) {
                            //向药企推送处方过期的通知
                            try {
                                AccessDrugEnterpriseService remoteService = RemoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
                                DrugEnterpriseResult drugEnterpriseResult = remoteService.updatePrescriptionStatus(recipe.getRecipeCode(), AlDyfRecipeStatusConstant.EXPIRE);
                                LOGGER.info("向药企推送处方过期通知,{}", JSONUtils.toString(drugEnterpriseResult));
                            } catch (Exception e) {
                                LOGGER.info("向药企推送处方过期通知有问题{}", recipe.getRecipeId(), e);
                            }
                        }


                    }
                }
                memo.delete(0, memo.length());
                int recipeId = recipe.getRecipeId();
                //相应订单处理
                order = orderDAO.getOrderByRecipeId(recipeId);
                if (null != order) {
                    orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO, true);
                    if (Objects.nonNull(order)) {
                        stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_TIMEOUT_NON_PAYMENT);
                    }
                    if (recipe.getFromflag().equals(RecipeBussConstant.FROMFLAG_HIS_USE)) {
                        orderDAO.updateByOrdeCode(order.getOrderCode(), ImmutableMap.of("cancelReason", "患者未在规定时间内支付，该处方单已失效"));
                        //发送超时取消消息
                        //${sendOrgan}：抱歉，您的处方单由于超过${overtime}未处理，处方单已失效。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
                        RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_CANCEL_4HIS, recipe);
                    }
                }


                //变更处方状态
                status = recipe.getStatus();
                //date 20200709 修改前置的处方药师ca签名中签名失败，处方状态未处理
                if (ReviewTypeConstant.Preposition_Check.equals(recipe.getReviewType()) && (RecipeStatusConstant.SIGN_ING_CODE_PHA == status || RecipeStatusConstant.SIGN_ERROR_CODE_PHA == status)) {
                    recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.NO_OPERATOR, ImmutableMap.of("chooseFlag", 1));
                    stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_TIMEOUT_NOT_ORDER);
                } else {
                    recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.DELETE, ImmutableMap.of("chooseFlag", 1));
                    stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_DELETED, RecipeStateEnum.SUB_DELETED_DOCTOR_NOT_SUBMIT);
                }

                memo.append("当前处方ca操作超时没处理，失效删除");
                //未支付，三天后自动取消后，优惠券自动释放
                RecipeCouponService recipeCouponService = ApplicationUtils.getRecipeService(RecipeCouponService.class);
                recipeCouponService.unuseCouponByRecipeId(recipeId);
                //推送处方到监管平台
                recipeIds.add(recipeId);
                //药师首页待处理任务---取消未结束任务
                if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())) {
                    ApplicationUtils.getBaseService(IAsynDoBussService.class).fireEvent(new BussCancelEvent(recipeId, BussTypeConstant.RECIPE));
                }
                //HIS消息发送
                boolean succFlag = hisService.recipeStatusUpdate(recipeId);
                if (succFlag) {
                    memo.append(",HIS推送成功");
                } else {
                    memo.append(",HIS推送失败");
                }
                //保存处方状态变更日志
                RecipeLogService.saveRecipeLog(recipeId, status, RecipeStatusConstant.DELETE, memo.toString());
                RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(recipeIds, 1));
            }
        }
    }

    /**
     * 取消ca处方过期包含过期时间
     */
    @RpcService(timeout = 600000)
    public void cancelSignRecipe(Integer endDate, Integer startDate) {
        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeOrderDAO recipeOrderDAO = getDAO(RecipeOrderDAO.class);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

        //设置查询时间段
        String endDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(endDate), DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(startDate), DateConversion.DEFAULT_DATE_TIME);

        //筛选处方状态是签名中和签名失败的
        List<Recipe> recipeList = recipeDAO.getRecipeListForSignCancelRecipe(startDt, endDt);
        //这里要取消处方的首先判断处方的状态是
        //取消处方的步骤：1.判断处方
        LOGGER.info("cancelSignRecipeTask 取消的ca签名处方列表{}", JSONUtils.toString(recipeList));
        RecipeOrder order = null;
        StringBuilder memo = new StringBuilder();
        Integer status;
        if (CollectionUtils.isNotEmpty(recipeList)) {
            for (Recipe recipe : recipeList) {
                if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
                    OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
                    List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
                    for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                        if (("aldyf".equals(drugsEnterprise.getCallSys()) || "tmdyf".equals(drugsEnterprise.getCallSys())) && recipe.getPushFlag() == 1) {
                            //向药企推送处方过期的通知
                            RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
                            try {
                                AccessDrugEnterpriseService remoteService = RemoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
                                DrugEnterpriseResult drugEnterpriseResult = remoteService.updatePrescriptionStatus(recipe.getRecipeCode(), AlDyfRecipeStatusConstant.EXPIRE);
                                LOGGER.info("向药企推送处方过期通知,{}", JSONUtils.toString(drugEnterpriseResult));
                            } catch (Exception e) {
                                LOGGER.info("向药企推送处方过期通知有问题{}", recipe.getRecipeId(), e);
                            }
                        }


                    }
                }
                memo.delete(0, memo.length());
                int recipeId = recipe.getRecipeId();
                //相应订单处理
                order = orderDAO.getOrderByRecipeId(recipeId);
                if (null != order) {

                    orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO, true);
                    if (Objects.nonNull(order)) {
                        stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_TIMEOUT_NON_PAYMENT);
                    }
                    if (recipe.getFromflag().equals(RecipeBussConstant.FROMFLAG_HIS_USE)) {
                        orderDAO.updateByOrdeCode(order.getOrderCode(), ImmutableMap.of("cancelReason", "患者未在规定时间内支付，该处方单已失效"));
                        //发送超时取消消息
                        //${sendOrgan}：抱歉，您的处方单由于超过${overtime}未处理，处方单已失效。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
                        RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_CANCEL_4HIS, recipe);
                    }
                }

                //变更处方状态
                status = recipe.getStatus();
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.DELETE, ImmutableMap.of("chooseFlag", 1));
                stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_TIMEOUT_NOT_ORDER);
                memo.append("当前处方ca操作超时没处理，失效删除");
                //未支付，三天后自动取消后，优惠券自动释放
                RecipeCouponService recipeCouponService = ApplicationUtils.getRecipeService(RecipeCouponService.class);
                recipeCouponService.unuseCouponByRecipeId(recipeId);
                //推送处方到监管平台
                RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(Collections.singletonList(recipe.getRecipeId()), 1));
                //HIS消息发送
                boolean succFlag = hisService.recipeStatusUpdate(recipeId);
                if (succFlag) {
                    memo.append(",HIS推送成功");
                } else {
                    memo.append(",HIS推送失败");
                }
                //保存处方状态变更日志
                RecipeLogService.saveRecipeLog(recipeId, status, RecipeStatusConstant.DELETE, memo.toString());

            }
        }

        //处理过期取消的处方
        List<Integer> statusList = Arrays.asList(RecipeStatusConstant.NO_PAY, RecipeStatusConstant.NO_OPERATOR);
        for (Integer statusCancel : statusList) {
            List<Recipe> recipeCancelList = recipeDAO.getRecipeListForCancelRecipe(statusCancel, startDt, endDt);
            LOGGER.info("cancelRecipeTask 状态=[{}], 取消数量=[{}], 详情={}", statusCancel, recipeCancelList.size(), JSONUtils.toString(recipeCancelList));
            if (CollectionUtils.isNotEmpty(recipeCancelList)) {
                for (Recipe recipe : recipeCancelList) {
                    if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
                        OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
                        List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
                        for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                            if ("aldyf".equals(drugsEnterprise.getCallSys()) || ("tmdyf".equals(drugsEnterprise.getCallSys()) && recipe.getPushFlag() == 1)) {
                                //向药企推送处方过期的通知
                                RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
                                try {
                                    AccessDrugEnterpriseService remoteService = RemoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
                                    DrugEnterpriseResult drugEnterpriseResult = remoteService.updatePrescriptionStatus(recipe.getRecipeCode(), AlDyfRecipeStatusConstant.EXPIRE);
                                    LOGGER.info("向药企推送处方过期通知,{}", JSONUtils.toString(drugEnterpriseResult));
                                } catch (Exception e) {
                                    LOGGER.info("向药企推送处方过期通知有问题{}", recipe.getRecipeId(), e);
                                }
                            }


                        }
                    }
                    memo.delete(0, memo.length());
                    int recipeId = recipe.getRecipeId();
                    //相应订单处理
                    order = orderDAO.getOrderByRecipeId(recipeId);
                    orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO, true);
                    if (Objects.nonNull(order)) {
                        stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_TIMEOUT_NON_PAYMENT);
                    }
                    if (recipe.getFromflag().equals(RecipeBussConstant.FROMFLAG_HIS_USE)) {
                        if (null != order) {
                            orderDAO.updateByOrdeCode(order.getOrderCode(), ImmutableMap.of("cancelReason", "患者未在规定时间内支付，该处方单已失效"));
                        }
                        //发送超时取消消息
                        //${sendOrgan}：抱歉，您的处方单由于超过${overtime}未处理，处方单已失效。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
                        RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_CANCEL_4HIS, recipe);
                    }

                    //变更处方状态
                    recipeDAO.updateRecipeInfoByRecipeId(recipeId, statusCancel, ImmutableMap.of("chooseFlag", 1));
                    stateManager.updateRecipeState(order.getOrderId(), RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_TIMEOUT_NOT_ORDER);
                    RecipeMsgService.batchSendMsg(recipe, statusCancel);
                    if (RecipeStatusConstant.NO_PAY == statusCancel) {
                        memo.append("已取消,超过3天未支付");
                    } else if (RecipeStatusConstant.NO_OPERATOR == statusCancel) {
                        memo.append("已取消,超过3天未操作");
                    } else {
                        memo.append("未知状态:" + statusCancel);
                    }
                    if (RecipeStatusConstant.NO_PAY == statusCancel) {
                        //未支付，三天后自动取消后，优惠券自动释放
                        RecipeCouponService recipeCouponService = ApplicationUtils.getRecipeService(RecipeCouponService.class);
                        recipeCouponService.unuseCouponByRecipeId(recipeId);
                    }
                    //推送处方到监管平台
                    RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(Collections.singletonList(recipe.getRecipeId()), 1));
                    //HIS消息发送
                    boolean succFlag = hisService.recipeStatusUpdate(recipeId);
                    if (succFlag) {
                        memo.append(",HIS推送成功");
                    } else {
                        memo.append(",HIS推送失败");
                    }
                    //保存处方状态变更日志
                    RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS, statusCancel, memo.toString());
                }
            }
        }

    }

    /**
     * 定时任务:定时取消处方的(购药除外)
     */
    @RpcService
    public void cancelSignRecipeForFastRecipe() {
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        //设置查询时间段
        String endDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(Integer.parseInt(cacheService.getParam(ParameterConstant.KEY_RECIPE_VALIDDATE_DAYS, RECIPE_EXPIRED_DAYS.toString()))), DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateConversion.getDateFormatter(DateConversion.getDateTimeDaysAgo(Integer.parseInt(cacheService.getParam(ParameterConstant.KEY_RECIPE_CANCEL_DAYS, RECIPE_EXPIRED_SEARCH_DAYS.toString()))), DateConversion.DEFAULT_DATE_TIME);

        //筛选便捷购药处方状态是签名中和签名失败的
        List<Recipe> recipeList = recipeDAO.getFastRecipeListForSignFail(startDt, endDt);
        //这里要取消处方的首先判断处方的状态是
        //取消处方的步骤：1.判断处方
        LOGGER.info("cancelSignRecipeForFastRecipe 取消的ca签名处方列表{}", JSONUtils.toString(recipeList));
        if (CollectionUtils.isNotEmpty(recipeList)) {
            for (Recipe recipe : recipeList) {
                if (recipe.getStatus().equals(RecipeStatusEnum.RECIPE_STATUS_SIGN_ERROR_CODE_DOC.getType()) ||
                        recipe.getStatus().equals(RecipeStatusEnum.RECIPE_STATUS_SIGN_ING_CODE_DOC.getType())) {
                    caBusinessService.updateSignFailState(recipe, "", RecipeStatusEnum.RECIPE_STATUS_DELETE, true);
                } else {
                    caBusinessService.updateSignFailState(recipe, "", RecipeStatusEnum.RECIPE_STATUS_DELETE, false);
                }
            }
        }
    }

    @RpcService
    public List<String> findCommonSymptomIdByDoctorAndOrganId(int doctorId, int organId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findCommonSymptomIdByDoctorAndOrganId(doctorId, organId);
    }


    @RpcService
    public List<Symptom> findCommonSymptomByDoctorAndOrganId(int doctor, int organId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        return recipeDAO.findCommonSymptomByDoctorAndOrganId(doctor, organId, 0, 10);
    }

    @RpcService
    public String getGiveModeTextByKey(String supportType, Integer organId) {
        //设置购药方式文案
        return getGiveModeText(supportType, organId);
    }

    private String getGiveModeText(String supportType, Integer organId) {
        Recipe recipe = new Recipe();
        Map<String, String> map = buttonManager.getGiveModeSettingFromYypt(organId).getGiveModeButtons().stream().collect(Collectors.toMap(GiveModeButtonDTO::getShowButtonKey, GiveModeButtonDTO::getShowButtonName));
        return map.get(supportType);
    }

    /**
     * 根据 第三方id 与 状态 获取最新处方id
     *
     * @param clinicId 第三方关联id （目前只有复诊）
     * @param status   处方状态
     * @return
     */
    @RpcService
    public Integer getRecipeIdByClinicId(Integer clinicId, Integer status) {
        LOGGER.info("RecipeService.getRecipeByClinicId clinicId={}", clinicId);
        return Optional.ofNullable(recipeDAO.getByClinicIdAndStatus(clinicId, status)).map(Recipe::getRecipeId).orElse(null);
    }

    /**
     * 再触发药师签名的时候将pdf先生成，回调的时候再将CA的返回更新
     * 之所以不放置在CA回调里，是因为老流程里不是一定调用回调函数的
     *
     * @param recipeId
     */
    @RpcService
    public void pharmacyToRecipePDF(Integer recipeId) {
        LOGGER.info("recipe pharmacyToRecipePDF,recipeId={}", recipeId);
        createPdfFactory.updateCheckNamePdf(recipeId);
    }

    @RpcService
    public void pharmacyToRecipePDFDefault(Integer recipeId) {
        LOGGER.info("recipe pharmacyToRecipePDFDefault,recipeId={}", recipeId);
        createPdfFactory.updateCheckNamePdfDefault(recipeId, null);
    }

    /**
     * 复诊结束后医生不能开出处方规则优化
     *
     * @param recipe
     * @return
     */
    public boolean openRecipeOptimize(RecipeBean recipe) {
        //开具处方时复诊状态判断配置
        String isUnderwayRevisit = configurationClient.getValueEnumCatch(recipe.getClinicOrgan(), "isUnderwayRevisit", WriteRecipeConditionTypeEnum.NO_CONDITION.getType());
        if (WriteRecipeConditionTypeEnum.NO_CONDITION.getType().equals(isUnderwayRevisit)) {
            return true;
        }
        if (BussSourceTypeEnum.BUSSSOURCE_REVISIT.getType().equals(recipe.getBussSource()) && null != recipe.getClinicId()) {
            IRevisitService revisitService = RevisitAPI.getService(IRevisitService.class);
            return revisitService.findValidRevisitByMpiIdAndDoctorIdEffectiveExtension(recipe.getClinicOrgan(), recipe.getClinicId());
        }
        return true;
    }

    /**
     * 只有安卓使用 5-1 版本删除
     *
     * @param organId       机构Id
     * @param OrganDrugCode 机构药品编码
     * @param drugType      药品类型
     * @return
     */
    @RpcService
    @Deprecated
    public List<DrugEntrustDTO> queryDrugEntrustByOrganIdAndDrugCode(Integer organId, String OrganDrugCode, Integer drugType) {
        LOGGER.info(" queryDrugEntrustByOrganIdAndDrugCode.organId={},OrganDrugCode={},drugType={}", organId, OrganDrugCode, drugType);
        if (null == organId) {
            throw new DAOException(recipe.constant.ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }

        if (null == OrganDrugCode) {
            throw new DAOException(recipe.constant.ErrorCode.SERVICE_ERROR, "机构药品编码不能为空");
        }

        if (null == drugType) {
            throw new DAOException(recipe.constant.ErrorCode.SERVICE_ERROR, "机构药品类型不能为空");
        }

        OrganDrugListDAO drugListDAO = getDAO(OrganDrugListDAO.class);
        DrugEntrustService entrustService = ApplicationUtils.getRecipeService(DrugEntrustService.class);
        List<DrugEntrustDTO> dtoList = new ArrayList<>();
        String defaultDrugEntrust = drugListDAO.getDrugEntrustByOrganDrugCodeAndOrganId(organId, OrganDrugCode);
        //区分西药和中药默认嘱托 RecipeBussConstant  drugType==1||drugType==2
        if (RecipeBussConstant.RECIPETYPE_WM.equals(drugType) || RecipeBussConstant.RECIPETYPE_CPM.equals(drugType)) {
            //西药 中成药 --平台默认嘱托进行填充
            if (StringUtils.isNotEmpty(defaultDrugEntrust)) {
                DrugEntrustDTO drugEntrustDTO = new DrugEntrustDTO();
                drugEntrustDTO.setDrugEntrustDefaultFlag(true);
                drugEntrustDTO.setDrugEntrustId(0);
                drugEntrustDTO.setCreateDt(new Date());
                drugEntrustDTO.setDrugEntrustCode("自定义默认000");
                drugEntrustDTO.setDrugEntrustName(defaultDrugEntrust);
                drugEntrustDTO.setDrugEntrustValue("西药，中成药平台默认设置嘱托");
                dtoList.add(drugEntrustDTO);
                return dtoList;
            }
        } else if (RecipeBussConstant.RECIPETYPE_TCM.equals(drugType)) {
            //中草药  --中药嘱托字典库  drugType==3
            List<DrugEntrustDTO> drugEntrustDTOList = entrustService.querDrugEntrustByOrganId(organId);
            if (StringUtils.isNotEmpty(defaultDrugEntrust)) {
                for (DrugEntrustDTO dto : drugEntrustDTOList) {
                    if (defaultDrugEntrust.equals(dto.getDrugEntrustName())) {
                        dto.setDrugEntrustDefaultFlag(true);
                        break;
                    }
                }
            }
            LOGGER.info(" queryDrugEntrustByOrganIdAndDrugCode.drugEntrustDTOList{}", JSONUtils.toString(drugEntrustDTOList));
            return drugEntrustDTOList;
        }
        LOGGER.info(" queryDrugEntrustByOrganIdAndDrugCode.dtoList{}", JSONUtils.toString(dtoList));
        return dtoList;
    }

    /**
     * 医保药品判定
     *
     * @param detailBeanList 处方单详情
     * @param organId        机构ID
     */
    @RpcService
    public List<OrganDrugList> medicalCheck(List<RecipeDetailBean> detailBeanList, Integer organId) {
        LOGGER.info("medicalCheck request param:{}", JSONUtils.toString(detailBeanList));
        List<Integer> drugIds = detailBeanList.stream().map(RecipeDetailBean::getDrugId).distinct().collect(Collectors.toList());
        List<OrganDrugList> byOrganIdAndDrugIdList = organDrugListDAO.findByOrganIdAndDrugAndMedicalIdList(organId, drugIds);
        Map<Integer, OrganDrugList> organDrugListMaps = byOrganIdAndDrugIdList.stream().collect(Collectors.toMap(OrganDrugList::getDrugId, Function.identity(), (o, o2) -> o));
        List<OrganDrugList> result = new ArrayList<>();
        for (RecipeDetailBean recipeDetailBean : detailBeanList) {
            if (organDrugListMaps.containsKey(recipeDetailBean.getDrugId())) {
                result.add(organDrugListMaps.get(recipeDetailBean.getDrugId()));
            }
        }
        LOGGER.info("medicalCheck response param:{}", JSONUtils.toString(result));
        return result;
    }

    /**
     * 根据处方的id获取多个处方支持的购药方式
     *
     * @param recipeIds
     * @return
     */
    @RpcService
    public List<RecipeGiveModeButtonRes> getRecipeGiveModeButtonRes(List<Integer> recipeIds) {
        LOGGER.info("getRecipeGiveModeButtonRes.recipeIds{}", JSONUtils.toString(recipeIds));
        List<RecipeGiveModeButtonRes> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(recipeIds)) {
            return list;
        }
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIds);
        if (CollectionUtils.isEmpty(recipes)) {
            return list;
        }
        // 从运营平台获取所有的购药方式
        GiveModeShowButtonDTO giveModeShowButtonVO = buttonManager.getGiveModeSettingFromYypt(recipes.get(0).getClinicOrgan());

        List<GiveModeButtonDTO> giveModeButtons = giveModeShowButtonVO.getGiveModeButtons();
        LOGGER.info("getRecipeGiveModeButtonRes.giveModeButtons{}", JSONUtils.toString(giveModeButtons));
        Map<String, List<GiveModeButtonDTO>> buttonsMap = giveModeButtons.stream().collect(Collectors.groupingBy(GiveModeButtonDTO::getShowButtonKey));
        // 例外支付单独处理 只要机构配置了例外支付,所有处方都支持
        List<GiveModeButtonDTO> giveModeButtonBeans = buttonsMap.get(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText());
        if (CollectionUtils.isNotEmpty(giveModeButtonBeans)) {
            RecipeGiveModeButtonRes supportMedicalPaymentButton = new RecipeGiveModeButtonRes(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText(),
                    giveModeButtonBeans.get(0).getShowButtonName(), recipeIds, true, giveModeButtonBeans.get(0).getButtonSkipType());
            list.add(supportMedicalPaymentButton);
        }
        RecipeSupportGiveModeEnum[] values = RecipeSupportGiveModeEnum.values();
        for (RecipeSupportGiveModeEnum value : values) {
            getGiveModeButton(value, recipes, buttonsMap, list, recipeIds.size());
        }
        //绍兴市人民医院个性化处理，配置了白名单的就诊人只显示例外支付按钮,否则隐藏
        try {
            list = handleMedicalPaymentButton(recipes.get(0), list);
        } catch (Exception e) {
            LOGGER.error("getRecipeGiveModeButtonRes error", e);
        }
        LOGGER.info("getRecipeGiveModeButtonRes.List<RecipeGiveModeButtonRes> = {}", JSONUtils.toString(list));
        return list;
    }

    /**
     * 针对绍兴市人民医院做个性化处理
     * 配置了白名单的就诊人只显示例外支付按钮，不在白名单的则隐藏例外支付按钮
     *
     * @param recipe
     * @param list
     * @return
     */
    public List<RecipeGiveModeButtonRes> handleMedicalPaymentButton(Recipe recipe, List<RecipeGiveModeButtonRes> list) {
        RecipeParameterDao parameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String recipeIdCardWhiteListOrgan = parameterDao.getByName("recipe_idCard_whiteList_organ");
        LOGGER.info("GiveModeManager handleMedicalPaymentButton={}", recipeIdCardWhiteListOrgan);
        if (StringUtils.isNotEmpty(recipeIdCardWhiteListOrgan)) {
            List<String> organIdList = Arrays.asList(recipeIdCardWhiteListOrgan.split(","));
            String recipeIdCardWhiteList = parameterDao.getByName("recipe_idCard_whiteList");
            LOGGER.info("GiveModeManager recipeIdCardWhiteList={}", recipeIdCardWhiteList);
            if (organIdList.contains(recipe.getClinicOrgan().toString())) {
                if (StringUtils.isEmpty(recipeIdCardWhiteList)) {
                    list = list.stream().filter(a -> !a.getGiveModeKey().equals(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText())).collect(Collectors.toList());
                    return list;
                }
                List<String> recipeIdCardWhiteLists = Arrays.asList(recipeIdCardWhiteList.split(","));
                PatientDTO patient = patientService.get(recipe.getMpiid());
                if (Objects.nonNull(patient)) {
                    LOGGER.info("GiveModeManager patient={}", JSONUtils.toString(patient));
                    if (recipeIdCardWhiteLists.contains(patient.getIdcard())) {
                        list = list.stream().filter(a -> a.getGiveModeKey().equals(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText())).collect(Collectors.toList());
                    } else {
                        list = list.stream().filter(a -> !a.getGiveModeKey().equals(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText())).collect(Collectors.toList());
                    }
                }
            }
        }
        return list;
    }

    private void getGiveModeButton(RecipeSupportGiveModeEnum recipeSupportGiveModeEnum, List<Recipe> recipes, Map<String, List<GiveModeButtonDTO>> buttonsMap, List<RecipeGiveModeButtonRes> list, Integer size) {
        List<GiveModeButtonDTO> giveModeButtonBeans = buttonsMap.get(recipeSupportGiveModeEnum.getText());
        if (CollectionUtils.isEmpty(giveModeButtonBeans) || RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText().equals(giveModeButtonBeans.get(0).getShowButtonKey())) {
            return;
        }
        RecipeGiveModeButtonRes button = new RecipeGiveModeButtonRes(recipeSupportGiveModeEnum.getText(), giveModeButtonBeans.get(0).getShowButtonName());
        List<Integer> buttonList = new ArrayList<>();
        recipes.forEach(recipe -> {
            String recipeSupportGiveMode = recipe.getRecipeSupportGiveMode();
            if (StringUtils.isEmpty(recipeSupportGiveMode)) {
                return;
            }
            if (recipeSupportGiveMode.contains(String.valueOf(recipeSupportGiveModeEnum.getType()))) {
                buttonList.add(recipe.getRecipeId());
            }
        });
        if (CollectionUtils.isEmpty(buttonList)) {
            return;
        }
        button.setJumpType(giveModeButtonBeans.get(0).getButtonSkipType());
        boolean buttonFlag = false;
        if (RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText().equals(giveModeButtonBeans.get(0).getShowButtonKey())) {
            if (size.equals(1)) {
                buttonFlag = true;
            }
        } else {
            if (size.equals(buttonList.size())) {
                buttonFlag = true;
            }
        }
        button.setButtonFlag(buttonFlag);
        button.setRecipeIds(buttonList);
        list.add(button);
    }


    /**
     * @param recipeId
     * @param type
     * @return
     */
    @RpcService
    public boolean testMethod(int recipeId, String type) {
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if ("1".equals(type)) {
            //支付完成后
            RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.RECIPE_PAY_CALL_SUCCESS);
        } else if ("2".equals(type)) {
            // return auditModeContext.getAuditModes(recipe.getReviewType()).notifyPharAudit(recipe);
        } else if ("3".equals(type)) {
            //发送消息--待审核消息
            RecipeMsgService.batchSendMsg(recipe.getRecipeId(), 2);
        } else if ("4".equals(type)) {
            //发送消息--待审核消息
            RecipeMsgService.batchSendMsg(recipe.getRecipeId(), 8);
        } else if ("5".equals(type)) {
            departManager.getAppointDepartByOrganIdAndDepart(recipe);
        } else if ("6".equals(type)) {
            //根据处方获取挂号科室
            recipe.setAppointDepart(null);
            departManager.getAppointDepartByOrganIdAndDepart(recipe);
        } else if ("7".equals(type)) {
            //前置pdf测试
            AbstractCaProcessType.getCaProcessFactory(recipe.getClinicOrgan()).hisCallBackCARecipeFunction(recipeId);
        } else if ("8".equals(type)) {
            //获取患者附近药房
            List<TakeMedicineByToHos> takeMedicineByToHosList = enterpriseManager.getTakeMedicineByToHosList(recipe.getClinicOrgan(), recipe);
            LOGGER.info(JSONUtils.toString(takeMedicineByToHosList));
        } else if ("9".equals(type)) {
//            consultClient.getRecipePaymentFee();
        } else if ("10".equals(type)) {
            caManager.obtainFastRecipeCaParam(recipe);
        } else {
            Object isDefaultGiveModeToHos = configService.getConfiguration(recipe.getClinicOrgan(), "isDefaultGiveModeToHos");
            LOGGER.info("setGiveMode isDefaultGiveModeToHos：{} ", isDefaultGiveModeToHos);
            if ((boolean) isDefaultGiveModeToHos == true) {
                //默认到院取药
                if (null == recipe.getGiveMode()) {
                    recipe.setGiveMode(RecipeBussConstant.GIVEMODE_TO_HOS);
                }
            }
            LOGGER.info("setGiveMode result recipe:{}", JSONUtils.toString(recipe));
        }
        return true;
    }


    /**
     * recipeFlag数据处理
     *
     * @param startTime
     * @param endTime
     * @param recipeIds
     * @param organId
     */
    @RpcService
    public void handDealRecipeFlag(String startTime, String endTime, List<Integer> recipeIds, Integer organId) {
        LOGGER.info("handDealRecipeFlag startTime={} endTime={} recipeIds={} organId={}", startTime, endTime, JSONUtils.toString(recipeIds), organId);
        try {
            List<Recipe> recipes = recipeDAO.queryRecipeByTimeAndRecipeIdsAndOrganId(startTime, endTime, recipeIds, organId);
            recipes.forEach(recipe -> {
                try {
                    RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                    if (recipeExtend != null) {
                        PatientDTO patient = patientService.get(recipe.getMpiid());
                        if (patient != null) {
                            if (new Integer(1).equals(patient.getPatientUserType()) || new Integer(2).equals(patient.getPatientUserType())) {
                                recipeExtend.setRecipeFlag(1);
                            } else if (new Integer(0).equals(patient.getPatientUserType())) {
                                recipeExtend.setRecipeFlag(0);
                            }
                        }
                        recipeExtendDAO.updateNonNullFieldByPrimaryKey(recipeExtend);
                    }
                } catch (Exception e) {
                    LOGGER.error("RecipeOpenAtop updateNonNullFieldByPrimaryKey error e", e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("RecipeService handDealRecipeFlag error e", e);
            throw new DAOException(recipe.constant.ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 判断审方信息
     *
     * @param organId
     * @return
     */
    private boolean getIntellectJudicialFlag(Integer organId) {
        IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        Integer intellectJudicialFlag = (Integer) configurationCenterUtilsService.getConfiguration(organId, "intellectJudicialFlag");
        if (intellectJudicialFlag == 2 || intellectJudicialFlag == 3) {
            intellectJudicialFlag = 1;
        }
        LOGGER.info("PrescriptionService getIntellectJudicialFlag  organId = {} , intellectJudicialFlag={}", organId, intellectJudicialFlag);
        return Integer.valueOf(1).equals(intellectJudicialFlag);
    }

    /**
     * 组装上传到监管平台的中医疾病
     */
    public DiseaseDTO assembleMultipleDisease(Integer organId, String diseaseIds) {
        LOGGER.info("assembleMultipleDisease organId={},diseases={}", organId, diseaseIds);
        IDiseaseService diseaseService = AppContextHolder.getBean("eh.diseasService", IDiseaseService.class);
        DiseaseDTO disease = new DiseaseDTO();
        String[] diseaseIdArray = diseaseIds.split(";");
        if (new Integer(1).equals(diseaseIdArray.length)) {
            DiseaseDTO diseaseDTO = diseaseService.getDiseasByCodeAndOrganId(organId, diseaseIds);
            LOGGER.info("assembleMultipleSymptom diseaseDTO1={}", JSONUtils.toString(diseaseDTO));
            return diseaseDTO;
        }
        StringBuilder regulationOrganDiseaseId = new StringBuilder();
        StringBuilder regulationOrganDiseaseName = new StringBuilder();
        StringBuilder regulationMedicalDiseaseCode = new StringBuilder();
        StringBuilder regulationMedicalDiseaseName = new StringBuilder();
        try {
            for (String diseaseId : diseaseIdArray) {
                DiseaseDTO diseaseDTO = diseaseService.getDiseasByCodeAndOrganId(organId, diseaseId);
                LOGGER.info("assembleMultipleSymptom diseaseDTO={}", JSONUtils.toString(diseaseDTO));
                if (null != diseaseDTO) {
                    if (null != diseaseDTO.getJgDiseasId()) {
                        regulationOrganDiseaseId.append(diseaseDTO.getJgDiseasId()).append("|");
                    }
                    if (null != diseaseDTO.getJgDiseasName()) {
                        regulationOrganDiseaseName.append(diseaseDTO.getJgDiseasName()).append("|");
                    }
                    if (null != diseaseDTO.getMedicalDiseaseCode()) {
                        regulationMedicalDiseaseCode.append(diseaseDTO.getMedicalDiseaseCode()).append("|");
                    }
                    if (null != diseaseDTO.getMedicalDiseaseName()) {
                        regulationMedicalDiseaseName.append(diseaseDTO.getMedicalDiseaseName()).append("|");
                    }
                }
            }
            if (StringUtils.isNotEmpty(regulationOrganDiseaseId)) {
                disease.setJgDiseasId(regulationOrganDiseaseId.substring(0, regulationOrganDiseaseId.length() - 1));
            }
            if (StringUtils.isNotEmpty(regulationOrganDiseaseName)) {
                disease.setJgDiseasName(regulationOrganDiseaseName.substring(0, regulationOrganDiseaseName.length() - 1));
            }
            if (StringUtils.isNotEmpty(regulationMedicalDiseaseCode)) {
                disease.setMedicalDiseaseCode(regulationMedicalDiseaseCode.substring(0, regulationMedicalDiseaseCode.length() - 1));
            }
            if (StringUtils.isNotEmpty(regulationMedicalDiseaseName)) {
                disease.setMedicalDiseaseName(regulationMedicalDiseaseName.substring(0, regulationMedicalDiseaseName.length() - 1));
            }
            LOGGER.info("assembleMultipleSymptom symptoms={}", JSONUtils.toString(disease));
        } catch (Exception e) {
            LOGGER.error("assembleMultipleSymptom error", e);
        }
        return disease;
    }

    /**
     * 药企药品同步配置 机构控件
     * @param organ
     * @param startDate
     * @param endDate
     * @param createDtSortType
     * @param start
     * @param limit
     * @param drugsEnterpriseId
     * @return
     */
//    @RpcService
//    public QueryResult<OrganDTO> queryOrganWithSortByStartAndLimit2(OrganDTO organ, Date startDate, Date endDate, String createDtSortType, Integer start, Integer limit,Integer drugsEnterpriseId) {
//        OrganService organDAO = AppContextHolder.getBean("basic.organService",OrganService.class);
//        QueryResult<OrganDTO> organDTOQueryResult = organDAO.queryOrganWithSortByStartAndLimit(organ, startDate, endDate, createDtSortType, start, limit);
//        List<OrganDTO> list=Lists.newArrayList();
//        List<OrganDTO> items = organDTOQueryResult.getItems();
//        //过滤机构配置流转药企
//        List<OrganAndDrugsepRelation> organAndDrugsepRelationList=organAndDrugsepRelationDAO.findByEntId(drugsEnterpriseId);
//        Map<Integer, OrganAndDrugsepRelation> organAndDrugsepRelationMap = organAndDrugsepRelationList.stream().collect(Collectors.toMap(x->x.getOrganId(), x -> x,(k1,k2)->k1));
//        if (!ObjectUtils.isEmpty(items)){
//            for (OrganDTO item : items) {
//                OrganService organService = AppContextHolder.getBean("basic.organService",OrganService.class);
//                if (!ObjectUtils.isEmpty(item.getManageUnit())){
//                    Long organNumFromTo = organService.getOrganNumFromTo(item.getManageUnit() + "%", startDate, endDate);
//                    if (!ObjectUtils.isEmpty(organNumFromTo)){
//                        Long number=organNumFromTo-1;
//                        item.setSubOrganNumber(number.toString());
//                    }
//                }
//                //过滤机构配置流转药企
//                if(organAndDrugsepRelationMap.containsKey(item.getOrganId())){
//                    list.add(item);
//                }
//            }
//            organDTOQueryResult.setItems(list);
//        }
//
//        return organDTOQueryResult;
//    }

    /**
     * 药企药品同步配置 机构控件
     *
     * @param organ
     * @param startDate
     * @param endDate
     * @param createDtSortType
     * @param start
     * @param limit
     * @param drugsEnterpriseId
     * @return
     */
    @RpcService
    public QueryResult<OrganDTO> queryOrganWithSortByStartAndLimit(OrganDTO organ, Date startDate, Date endDate, String createDtSortType, Integer start, Integer limit, Integer drugsEnterpriseId) {
        OrganService organDAO = AppContextHolder.getBean("basic.organService", OrganService.class);
        List<OrganAndDrugsepRelation> organAndDrugsepRelationList = organAndDrugsepRelationDAO.findByEntId(drugsEnterpriseId);
        List<Integer> organIds = organAndDrugsepRelationList.stream().map(OrganAndDrugsepRelation::getOrganId).collect(Collectors.toList());
        List<OrganDTO> organDTOList = organDAO.findOrgansByOrganIds(organIds);
        List<OrganDTO> items;
        if (createDtSortType.equals("desc")) {
            items = organDTOList.stream().sorted(Comparator.comparing(OrganDTO::getCreateDt).reversed()).collect(Collectors.toList());
        } else {
            items = organDTOList.stream().sorted(Comparator.comparing(OrganDTO::getCreateDt)).collect(Collectors.toList());
        }
        List<OrganDTO> list = Lists.newArrayList();

        QueryResult<OrganDTO> organDTOQueryResult = new QueryResult<>();
        if (!ObjectUtils.isEmpty(items)) {
            for (OrganDTO item : items) {
                OrganService organService = AppContextHolder.getBean("basic.organService", OrganService.class);
                if (!ObjectUtils.isEmpty(item.getManageUnit())) {
                    Long organNumFromTo = organService.getOrganNumFromTo(item.getManageUnit() + "%", startDate, endDate);
                    if (!ObjectUtils.isEmpty(organNumFromTo)) {
                        Long number = organNumFromTo - 1;
                        item.setSubOrganNumber(number.toString());
                    }
                }
                list.add(item);
            }
            organDTOQueryResult.setItems(list);
        }
        return organDTOQueryResult;
    }
}
