package recipe.service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.basic.ds.PatientVO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.grouprecipe.model.GroupRecipeConf;
import com.ngari.recipe.recipe.constant.RecipeDistributionFlagEnum;
import com.ngari.recipe.recipe.constant.RecipeListTabStatusEnum;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import ctd.account.UserRoleToken;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.FileAuth;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.event.GlobalEventExecFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import recipe.ApplicationUtils;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.comment.DictionaryUtil;
import recipe.constant.*;
import recipe.dao.*;
import recipe.dao.bean.PatientRecipeBean;
import recipe.dao.bean.RecipeListBean;
import recipe.dao.bean.RecipeRollingInfo;
import recipe.factory.status.constant.GiveModeEnum;
import recipe.factory.status.constant.RecipeOrderStatusEnum;
import recipe.factory.status.constant.RecipeStatusEnum;
import recipe.givemode.business.GiveModeFactory;
import recipe.givemode.business.IGiveModeBase;
import recipe.service.client.IConfigurationClient;
import recipe.service.common.RecipeCacheService;
import recipe.service.manager.EmrRecipeManager;
import recipe.service.manager.GroupRecipeManager;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ctd.persistence.DAOFactory.getDAO;
import static recipe.service.RecipeServiceSub.convertRecipeForRAP;
import static recipe.service.RecipeServiceSub.convertSensitivePatientForRAP;

/**
 * 处方业务一些列表查询
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/2/13.
 */
@RpcBean("recipeListService")
public class RecipeListService extends RecipeBaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeListService.class);

    public static final String LIST_TYPE_RECIPE = "1";

    public static final String LIST_TYPE_ORDER = "2";

    public static final Integer RECIPE_PAGE = 0;

    public static final Integer ORDER_PAGE = 1;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private PatientService patientService;
    @Autowired
    private DrugsEnterpriseService drugsEnterpriseService;
    @Autowired
    private OrganDrugListDAO organDrugListDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Resource
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private RecipeOrderDAO orderDAO;
    @Autowired
    private IConfigurationCenterUtilsService configService;
    @Autowired
    private RecipeRefundDAO recipeRefundDAO;
    @Autowired
    private GroupRecipeManager groupRecipeManager;
    @Resource
    private PharmacyTcmDAO pharmacyTcmDAO;
    @Resource
    private IConfigurationClient configurationClient;
    //历史处方显示的状态：未处理、未支付、审核不通过、失败、已完成、his失败、取药失败
    //date 20191016
    //历史处方展示的状态不包含已删除，已撤销，同步his失败（原已取消状态）
    //date 20191126
    //添加上已撤销的处方
    public static final Integer[] HistoryRecipeListShowStatusList = {RecipeStatusConstant.NO_OPERATOR, RecipeStatusConstant.NO_PAY, RecipeStatusConstant.CHECK_NOT_PASS_YS, RecipeStatusConstant.RECIPE_FAIL, RecipeStatusConstant.FINISH, RecipeStatusConstant.NO_DRUG, RecipeStatusConstant.REVOKE};

    public static final Integer No_Show_Button = 3;

    /**
     * 医生端处方列表展示
     *
     * @param doctorId 医生ID
     * @param recipeId 上一页最后一条处方ID，首页传0
     * @param limit    每页限制数
     * @return
     */
    @RpcService
    public List<Map<String, Object>> findRecipesForDoctor(Integer doctorId, Integer recipeId, Integer limit) {
        Assert.notNull(doctorId, "findRecipesForDoctor doctor is null.");
        checkUserHasPermissionByDoctorId(doctorId);
        recipeId = (null == recipeId || Integer.valueOf(0).equals(recipeId)) ? Integer.valueOf(Integer.MAX_VALUE) : recipeId;

        List<Map<String, Object>> list = new ArrayList<>(0);
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);

        List<Recipe> recipeList = recipeDAO.findRecipesForDoctor(doctorId, recipeId, 0, limit);
        LOGGER.info("findRecipesForDoctor recipeList size={}", recipeList.size());
        if (CollectionUtils.isNotEmpty(recipeList)) {
            List<String> patientIds = new ArrayList<>(0);
            Map<Integer, RecipeBean> recipeMap = Maps.newHashMap();
            //date 20200506
            //获取处方对应的订单信息
            Map<String, Integer> orderStatus = new HashMap<>();
            List<String> recipeCodes = recipeList.stream().map(recipe -> recipe.getOrderCode()).filter(code -> StringUtils.isNotEmpty(code)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(recipeCodes)) {

                List<RecipeOrder> recipeOrders = orderDAO.findValidListbyCodes(recipeCodes);
                orderStatus = recipeOrders.stream().collect(Collectors.toMap(RecipeOrder::getOrderCode, RecipeOrder::getStatus));
            }

            for (Recipe recipe : recipeList) {
                if (StringUtils.isNotEmpty(recipe.getMpiid())) {
                    patientIds.add(recipe.getMpiid());
                }
                //设置处方具体药品名称
                List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
                StringBuilder stringBuilder = new StringBuilder();

                for (Recipedetail recipedetail : recipedetails) {
                    List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(recipedetail.getDrugId(), recipe.getClinicOrgan());
                    if (organDrugLists != null && 0 < organDrugLists.size()) {
                        stringBuilder.append(organDrugLists.get(0).getSaleName());
                        if (StringUtils.isNotEmpty(organDrugLists.get(0).getDrugForm())) {
                            stringBuilder.append(organDrugLists.get(0).getDrugForm());
                        }
                    } else {
                        stringBuilder.append(recipedetail.getDrugName());
                    }
                    stringBuilder.append(" ").append(recipedetail.getDrugSpec()).append("/").append(recipedetail.getDrugUnit()).append("、");
                }
                if (-1 != stringBuilder.lastIndexOf("、")) {
                    stringBuilder.deleteCharAt(stringBuilder.lastIndexOf("、"));
                }
                recipe.setRecipeDrugName(stringBuilder.toString());
                //前台页面展示的时间源不同
                recipe.setRecipeShowTime(recipe.getCreateDate());
                boolean effective = false;
                //只有审核未通过的情况需要看订单状态
                if (RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
                    effective = orderDAO.isEffectiveOrder(recipe.getOrderCode());
                }
                //Map<String, String> tipMap = RecipeServiceSub.getTipsByStatus(recipe.getStatus(), recipe, effective);
                //date 20190929
                //修改医生端状态文案显示
                Map<String, String> tipMap = RecipeServiceSub.getTipsByStatusCopy(recipe.getStatus(), recipe, effective, (orderStatus == null || 0 >= orderStatus.size()) ? null : orderStatus.get(recipe.getOrderCode()));

                recipe.setShowTip(MapValueUtil.getString(tipMap, "listTips"));
                recipeMap.put(recipe.getRecipeId(), convertRecipeForRAP(recipe));
            }

            Map<String, PatientVO> patientMap = Maps.newHashMap();
            if (CollectionUtils.isNotEmpty(patientIds)) {
                List<PatientDTO> patientList = patientService.findByMpiIdIn(patientIds);
                if (CollectionUtils.isNotEmpty(patientList)) {
                    for (PatientDTO patient : patientList) {
                        //设置患者数据
                        RecipeServiceSub.setPatientMoreInfo(patient, doctorId);
                        patientMap.put(patient.getMpiId(), convertSensitivePatientForRAP(patient));
                    }
                }
            }

            for (Recipe recipe : recipeList) {
                String mpiId = recipe.getMpiid();
                HashMap<String, Object> map = Maps.newHashMap();
                map.put("recipe", recipeMap.get(recipe.getRecipeId()));
                map.put("patient", patientMap.get(mpiId));
                list.add(map);
            }
        }

        return list;
    }

    /**
     * 健康端获取待处理中最新的一单处方单
     *
     * @param mpiId 患者ID
     * @return
     */
    @RpcService
    public Map<String, Object> getLastestPendingRecipe(String mpiId) {
        Assert.hasLength(mpiId, "getLastestPendingRecipe mpiId is null.");
        checkUserHasPermissionByMpiId(mpiId);
        HashMap<String, Object> map = Maps.newHashMap();
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);

        List<String> allMpiIds = recipeService.getAllMemberPatientsByCurrentPatient(mpiId);
        List<Integer> recipeIds = recipeDAO.findPendingRecipes(allMpiIds, RecipeStatusConstant.CHECK_PASS, 0, 1);
        String title;
        String recipeGetModeTip = "";
        //默认需要展示 “购药”
        map.put("checkEnterprise", true);
        if (CollectionUtils.isNotEmpty(recipeIds)) {
            title = "赶快结算您的处方单吧！";
            List<Map> recipesMap = new ArrayList<>(0);
            for (Integer recipeId : recipeIds) {
                Map<String, Object> recipeInfo = recipeService.getPatientRecipeById(recipeId);
                recipeGetModeTip = MapValueUtil.getString(recipeInfo, "recipeGetModeTip");
                if (null != recipeInfo.get("checkEnterprise")) {
                    map.put("checkEnterprise", (Boolean) recipeInfo.get("checkEnterprise"));
                }
                recipesMap.add(recipeInfo);
            }
            map.put("recipes", recipesMap);
        } else {
            title = "暂无待处理处方单";
        }

        List<PatientRecipeDS> otherRecipes = this.findOtherRecipesForPatient(mpiId, 0, 1);
        if (CollectionUtils.isNotEmpty(otherRecipes)) {
            map.put("haveFinished", true);
        } else {
            map.put("haveFinished", false);
        }

        map.put("title", title);
        map.put("unSendTitle", cacheService.getParam(ParameterConstant.KEY_RECIPE_UNSEND_TIP));
        map.put("recipeGetModeTip", recipeGetModeTip);

        return map;
    }

    @RpcService
    public List<PatientRecipeDS> findOtherRecipesForPatient(String mpiId, Integer index, Integer limit) {
        Assert.hasLength(mpiId, "findOtherRecipesForPatient mpiId is null.");
        checkUserHasPermissionByMpiId(mpiId);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        List<String> allMpiIds = recipeService.getAllMemberPatientsByCurrentPatient(mpiId);
        //获取待处理那边最新的一单
        List<Integer> recipeIds = recipeDAO.findPendingRecipes(allMpiIds, RecipeStatusConstant.CHECK_PASS, 0, 1);
        List<PatientRecipeBean> backList = recipeDAO.findOtherRecipesForPatient(allMpiIds, recipeIds, index, limit);

        return ObjectCopyUtils.convert(processListDate(backList, allMpiIds), PatientRecipeDS.class);
    }


    /**
     * rpc接口不支持重载，线上异常，紧急处理bug#65156
     **/
    @RpcService
    public List<PatientRecipeDTO> findPatientAllRecipes(String mpiId, Integer index, Integer limit) {
        return findAllRecipesForPatient(mpiId, index, limit);
    }

    /**
     * 获取所有处方单信息
     * 患者端没有用到
     *
     * @param mpiId
     * @param index
     * @param limit
     * @return
     */
    @RpcService
    public List<PatientRecipeDTO> findAllRecipesForPatient(String mpiId, Integer index, Integer limit) {
        Assert.hasLength(mpiId, "findAllRecipesForPatient mpiId is null.");
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<String> allMpiIds = Arrays.asList(mpiId);
        //获取待处理那边最新的一单
        List<PatientRecipeBean> backList = recipeDAO.findOtherRecipesForPatient(allMpiIds, null, index, limit);
        return processListDate(backList, allMpiIds);
    }

    /**
     * 处理列表数据
     *
     * @param list
     * @param allMpiIds
     */
    private List<PatientRecipeDTO> processListDate(List<PatientRecipeBean> list, List<String> allMpiIds) {
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        DrugsEnterpriseService drugsEnterpriseService = ApplicationUtils.getRecipeService(DrugsEnterpriseService.class);
        List<PatientRecipeDTO> backList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(list)) {
            backList = ObjectCopyUtils.convert(list, PatientRecipeDTO.class);
            //处理订单类型数据
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            List<PatientDTO> patientList = patientService.findByMpiIdIn(allMpiIds);
            Map<String, PatientDTO> patientMap = Maps.newHashMap();
            if (null != patientList && !patientList.isEmpty()) {
                for (PatientDTO p : patientList) {
                    if (StringUtils.isNotEmpty(p.getMpiId())) {
                        patientMap.put(p.getMpiId(), p);
                    }
                }
            }

            Map<Integer, Boolean> checkEnterprise = Maps.newHashMap();
            PatientDTO p;
            for (PatientRecipeDTO record : backList) {
                p = patientMap.get(record.getMpiId());
                if (null != p) {
                    record.setPatientName(p.getPatientName());
                    record.setPhoto(p.getPhoto());
                    record.setPatientSex(p.getPatientSex());
                }
                //能否购药进行设置，默认可购药
                record.setCheckEnterprise(true);
                if (null != record.getOrganId()) {
                    if (null == checkEnterprise.get(record.getOrganId())) {
                        checkEnterprise.put(record.getOrganId(), drugsEnterpriseService.checkEnterprise(record.getOrganId()));
                    }
                    record.setCheckEnterprise(checkEnterprise.get(record.getOrganId()));
                }

                if (LIST_TYPE_RECIPE.equals(record.getRecordType())) {
                    record.setStatusText(getRecipeStatusText(record.getStatusCode()));
                    //设置失效时间
                    if (RecipeStatusConstant.CHECK_PASS == record.getStatusCode()) {
                        record.setRecipeSurplusHours(RecipeServiceSub.getRecipeSurplusHours(record.getSignDate()));
                    }
                    //药品详情
                    List<Recipedetail> recipedetailList = detailDAO.findByRecipeId(record.getRecordId());
                    record.setRecipeDetail(ObjectCopyUtils.convert(recipedetailList, RecipeDetailBean.class));
                } else if (LIST_TYPE_ORDER.equals(record.getRecordType())) {
                    RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                    record.setStatusText(getOrderStatusText(record.getStatusCode()));
                    RecipeResultBean resultBean = orderService.getOrderDetailById(record.getRecordId());
                    if (RecipeResultBean.SUCCESS.equals(resultBean.getCode())) {
                        if (null != resultBean.getObject() && resultBean.getObject() instanceof RecipeOrderBean) {
                            RecipeOrderBean order = (RecipeOrderBean) resultBean.getObject();
                            if (null != order.getLogisticsCompany()) {
                                try {
                                    //4.01需求：物流信息查询
                                    String logComStr = DictionaryController.instance().get("eh.cdr.dictionary.KuaiDiNiaoCode").getText(order.getLogisticsCompany());
                                    record.setLogisticsCompany(logComStr);
                                    record.setTrackingNumber(order.getTrackingNumber());
                                } catch (ControllerException e) {
                                    LOGGER.warn("processListDate KuaiDiNiaoCode get error. code={}", order.getLogisticsCompany(), e);
                                }
                            }
                            List<PatientRecipeDTO> recipeList = (List<PatientRecipeDTO>) order.getList();
                            if (CollectionUtils.isNotEmpty(recipeList)) {
                                for (PatientRecipeDTO recipe : recipeList) {
                                    record.setRecipeId(recipe.getRecipeId());
                                    record.setRecipeType(recipe.getRecipeType());
                                    record.setOrganDiseaseName(recipe.getOrganDiseaseName());
                                    record.setRecipeMode(recipe.getRecipeMode());
                                    // 订单支付方式
                                    record.setPayMode(recipe.getPayMode());
                                    //药品详情
                                    record.setRecipeDetail(recipe.getRecipeDetail());
                                    if (RecipeStatusConstant.CHECK_PASS == recipe.getStatusCode() && OrderStatusConstant.READY_PAY.equals(record.getStatusCode())) {
                                        record.setRecipeSurplusHours(recipe.getRecipeSurplusHours());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return backList;
    }

    /**
     * 获取最新开具的处方单前limit条，用于跑马灯显示
     *
     * @param limit
     * @return
     */
    public List<RecipeRollingInfoBean> findLastesRecipeList(List<Integer> organIds, int start, int limit) {
        IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);
        //date  2019/12/16
        //修改findTestDoctors接口从basic查询
        DoctorService iDoctorService = ApplicationUtils.getBasicService(DoctorService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        List<Integer> testDocIds = iDoctorService.findTestDoctors(organIds);
        String endDt = DateTime.now().toString(DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateTime.now().minusMonths(3).toString(DateConversion.DEFAULT_DATE_TIME);
        List<RecipeRollingInfo> list = recipeDAO.findLastesRecipeList(startDt, endDt, organIds, testDocIds, start, limit);

        // 个性化微信号医院没有开方医生不展示
        if (CollectionUtils.isEmpty(list)) {
            return Lists.newArrayList();
        }
        List<String> mpiIdList = new ArrayList<>();
        List<RecipeRollingInfoBean> backList = Lists.newArrayList();
        RecipeRollingInfoBean bean;
        for (RecipeRollingInfo info : list) {
            mpiIdList.add(info.getMpiId());
            bean = new RecipeRollingInfoBean();
            BeanUtils.copyProperties(info, bean);
            backList.add(bean);
        }

        List<PatientBean> patientList = iPatientService.findByMpiIdIn(mpiIdList);
        Map<String, PatientBean> patientMap = Maps.uniqueIndex(patientList, input -> input.getMpiId());

        PatientBean patient;
        for (RecipeRollingInfoBean info : backList) {
            patient = patientMap.get(info.getMpiId());
            if (null != patient) {
                info.setPatientName(patient.getPatientName());
            }
        }

        return backList;
    }

    /**
     * 处方患者端主页展示推荐医生 (样本采集数量在3个月内)
     *
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public List<Integer> findDoctorIdSortByCount(int start, int limit, List<Integer> organIds) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        //date  2019/12/16
        //修改findTestDoctors接口从basic查询
        DoctorService iDoctorService = ApplicationUtils.getBasicService(DoctorService.class);

        List<Integer> testDocIds = iDoctorService.findTestDoctors(organIds);
        String endDt = DateTime.now().toString(DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateTime.now().minusMonths(3).toString(DateConversion.DEFAULT_DATE_TIME);
        return recipeDAO.findDoctorIdSortByCount(startDt, endDt, organIds, testDocIds, start, limit);
    }


    private String getRecipeStatusText(int status) {
        String msg;
        RecipeStatusEnum recipeStatusEnum = RecipeStatusEnum.getRecipeStatusEnum(status);
        switch (recipeStatusEnum) {
            case RECIPE_STATUS_HIS_FAIL:
                msg = "已取消";
                break;
            case RECIPE_STATUS_CHECK_PASS_YS:
                msg = "待配送";
                break;
            default:
                msg = RecipeStatusEnum.getRecipeStatus(status);
        }
        return msg;
    }

    private String getOrderStatusText(Integer status) {
        return RecipeOrderStatusEnum.getOrderStatus(status);
    }

    private String getOrderStatusTabText(Integer status, Integer giveMode, Integer recipeStatus) {
        if (RecipeOrderStatusEnum.ORDER_STATUS_READY_GET_DRUG.getType().equals(status) && GiveModeEnum.GIVE_MODE_DOWNLOAD_RECIPE.getType().equals(giveMode) && !RecipeStatusEnum.RECIPE_STATUS_RECIPE_DOWNLOADED.getType().equals(recipeStatus)) {
            return "待下载";
        }
        if (RecipeOrderStatusEnum.READY_GET_DRUG.contains(status)) {
            return "待取药";
        }
        return getOrderStatusText(status);

    }

    private String getRecipeStatusTabText(int status, int recipeId) {
        String msg = RecipeStatusEnum.getRecipeStatus(status);
        if (RecipeStatusEnum.RECIPE_STATUS_REVOKE.getName().equals(msg)) {
            if (CollectionUtils.isNotEmpty(recipeRefundDAO.findRefundListByRecipeId(recipeId))) {
                msg = "已取消";
            }
        }
        return msg;
    }


    /**
     * 获取历史处方
     *
     * @param consultId
     * @param organId
     * @param doctorId
     * @param mpiId
     * @return
     */
    @RpcService
    public List<Map<String, Object>> findHistoryRecipeList(Integer consultId, Integer organId, Integer doctorId, String mpiId) {
        LOGGER.info("findHistoryRecipeList consultId={}, organId={},doctorId={},mpiId={}", consultId, organId, doctorId, mpiId);
        ICurrentUserInfoService currentUserInfoService = AppDomainContext.getBean("eh.remoteCurrentUserInfoService", ICurrentUserInfoService.class);
        Map<String, Object> upderLineRecipesByHis = new ConcurrentHashMap<>();
        Future<Map<String, Object>> hisTask = null;
        RecipePreserveService recipeService = ApplicationUtils.getRecipeService(RecipePreserveService.class);
        if (!("patient".equals(UserRoleToken.getCurrent().getRoleId()))) {
            //医生端逻辑照旧
            hisTask = GlobalEventExecFactory.instance().getExecutor().submit(() -> {
                return recipeService.getHosRecipeList(consultId, organId, mpiId, 180);
            });
        } else {
            //患者端如果公众号是区域公众号则需查询该区域公众号下所有机构线下处方
            List<Integer> organIds = currentUserInfoService.getCurrentOrganIds();
            LOGGER.info("findHistoryRecipeList organId:{},mpiId:{}, organIds:{}", organId, mpiId, JSONUtils.toString(organIds));
            hisTask = GlobalEventExecFactory.instance().getExecutor().submit(() -> {
                return recipeService.getAllHosRecipeList(consultId, organIds, mpiId, 180);
            });
        }
        //从Recipe表获取线上、线下处方
        List<Map<String, Object>> onLineAndUnderLineRecipesByRecipe = new ArrayList<>();
        try {
            onLineAndUnderLineRecipesByRecipe = findRecipeListByDoctorAndPatient(doctorId, mpiId, 0, 10000);
            LOGGER.info("findHistoryRecipeList 从recipe表获取处方信息success:{}", onLineAndUnderLineRecipesByRecipe);
        } catch (Exception e) {
            LOGGER.info("findHistoryRecipeList 从recipe表获取处方信息error:{}", e);
        }

        try {
            upderLineRecipesByHis = hisTask.get(5000, TimeUnit.MILLISECONDS);
            LOGGER.info("findHistoryRecipeList 从his获取已缴费处方信息:{}", JSONUtils.toString(upderLineRecipesByHis));
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("findHistoryRecipeList hisTask exception:{}", e.getMessage(), e);
        }

        //过滤重复数据并重新排序
        try {
            List<Map<String, Object>> res = dealRepeatDataAndSort(onLineAndUnderLineRecipesByRecipe, upderLineRecipesByHis);
            //返回结果集
            LOGGER.info("findHistoryRecipeList res:{}", JSONUtils.toString(res));
            return res;
        } catch (Exception e) {
            LOGGER.error("findHistoryRecipeList dealRepeatDataAndSort", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 重复数据处理、数据重新排序
     *
     * @param onLineAndUnderLineRecipesByRecipe
     * @param upderLineRecipesByHis
     * @return
     */
    private List<Map<String, Object>> dealRepeatDataAndSort(List<Map<String, Object>> onLineAndUnderLineRecipesByRecipe, Map<String, Object> upderLineRecipesByHis) {
        LOGGER.info("dealRepeatDataAndSort参数onLineAndUnderLineRecipesByRecipe:{},upderLineRecipesByHis:{}", JSONUtils.toString(onLineAndUnderLineRecipesByRecipe), JSONUtils.toString(upderLineRecipesByHis));
        Long beginTime = new Date().getTime();
        List<Map<String, Object>> res = new ArrayList<>();
        //过滤重复数据
        List<HisRecipeBean> hisRecipes = (List<HisRecipeBean>) upderLineRecipesByHis.get("hisRecipe");
        if (hisRecipes == null || hisRecipes.size() <= 0) {
            return onLineAndUnderLineRecipesByRecipe;
        }
        if (onLineAndUnderLineRecipesByRecipe != null && onLineAndUnderLineRecipesByRecipe.size() > 0) {
            for (Map<String, Object> map : onLineAndUnderLineRecipesByRecipe) {
                RecipeBean recipeBean = (RecipeBean) map.get("recipe");
                String recipeKey = recipeBean.getRecipeCode() + recipeBean.getClinicOrgan();
                for (int i = hisRecipes.size() - 1; i >= 0; i--) {
                    HisRecipeBean hisRecipeBean = hisRecipes.get(i);
                    String hiskey = hisRecipeBean.getRecipeCode() + hisRecipeBean.getClinicOrgan();
                    if (StringUtils.isEmpty(hiskey)) {
                        continue;
                    }
                    if (hiskey.equals(recipeKey)) {
                        LOGGER.info("dealRepeatDataAndSort删除线下处方:recipeCode{},clinicOrgan{}", hisRecipeBean.getRecipeCode(), hisRecipeBean.getClinicOrgan());
                        hisRecipes.remove(hisRecipeBean);//删除重复元素
                    }
                }
            }
        }

        //合并数据
        res.addAll(onLineAndUnderLineRecipesByRecipe);
        for (int i = hisRecipes.size() - 1; i >= 0; i--) {
            HisRecipeBean hisRecipeBean = hisRecipes.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("recipe", RecipeServiceSub.convertHisRecipeForRAP(hisRecipeBean));
            map.put("patient", upderLineRecipesByHis.get("patient"));
            attachDocSignPic(hisRecipeBean, map);
            res.add(map);
        }

        //根据创建时间降序排序
        Collections.sort(res, (o1, o2) -> {
            Date date1 = ((RecipeBean) o1.get("recipe")).getCreateDate() == null ? new Date() : ((RecipeBean) o1.get("recipe")).getCreateDate();
            Date date2 = ((RecipeBean) o2.get("recipe")).getCreateDate() == null ? new Date() : ((RecipeBean) o2.get("recipe")).getCreateDate();
            return date2.compareTo(date1);
        });
        Long totalConsumedTime = new Date().getTime() - beginTime;
        LOGGER.info("dealRepeatDataAndSort cost:{}", totalConsumedTime);
        return res;
    }

    /**
     * 线下处方设置签名图片
     *
     * @param hisRecipeBean
     * @param map
     */
    private void attachDocSignPic(HisRecipeBean hisRecipeBean, Map<String, Object> map) {
        EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
        if (StringUtils.isNotEmpty(hisRecipeBean.getDoctorCode())) {
            EmploymentDTO employmentDTO = employmentService.getByJobNumberAndOrganId(hisRecipeBean.getDoctorCode(), hisRecipeBean.getClinicOrgan());
            if (employmentDTO != null && employmentDTO.getDoctorId() != null) {
                //设置签名图片
                Map<String, String> signInfo = RecipeServiceSub.attachSealPic(hisRecipeBean.getClinicOrgan(), employmentDTO.getDoctorId(), null, null);
                if (StringUtils.isNotEmpty(signInfo.get("doctorSignImg"))) {
                    Map<String, String> otherInfo = new HashMap<>();
                    otherInfo.put("doctorSignImg", signInfo.get("doctorSignImg"));
                    otherInfo.put("doctorSignImgToken", FileAuth.instance().createToken(signInfo.get("doctorSignImg"), 3600L));
                    map.put("otherInfo", otherInfo);
                }
            }
        }
    }

    /**
     * 查找指定医生和患者间开的处方单列表
     *
     * @param doctorId
     * @param mpiId
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public List<Map<String, Object>> findRecipeListByDoctorAndPatient(Integer doctorId, String mpiId, int start, int limit) {
        //checkUserHasPermissionByDoctorId(doctorId);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        //List<Recipe> recipes = recipeDAO.findRecipeListByDoctorAndPatient(doctorId, mpiId, start, limit);
        //修改逻辑历史处方中获取的处方列表：只显示未处理、未支付、审核不通过、失败、已完成状态的
        List<Recipe> recipes = recipeDAO.findRecipeListByDoctorAndPatientAndStatusList(doctorId, mpiId, start, limit, new ArrayList<>(Arrays.asList(HistoryRecipeListShowStatusList)));
        LOGGER.info("findRecipeListByDoctorAndPatient mpiId:{} ,recipes:{} ", mpiId, JSONUtils.toString(recipes));
        PatientVO patient = RecipeServiceSub.convertSensitivePatientForRAP(patientService.get(mpiId));
        LOGGER.info("findRecipeListByDoctorAndPatient mpiId:{} ,patient:{} ", mpiId, JSONUtils.toString(patient));
        return instanceRecipesAndPatientNew(recipes, patient);
    }


    /**
     * 获取返回对象
     * 废弃
     *
     * @param recipes
     * @param patient
     * @return
     */
    public List<Map<String, Object>> instanceRecipesAndPatientNew(List<Recipe> recipes, PatientVO patient) {
        LOGGER.info("instanceRecipesAndPatient recipes:{} ,patient:{} ", JSONUtils.toString(recipes), JSONUtils.toString(patient));
        Long beginTime = new Date().getTime();
        List<Map<String, Object>> list = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(recipes)) {
            RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            RecipeRefundDAO recipeRefundDAO = DAOFactory.getDAO(RecipeRefundDAO.class);

            //date 20200506
            //获取处方对应的订单信息
            Map<String, Integer> orderStatus = new HashMap<>();
            List<String> recipeCodes = recipes.stream().map(recipe -> recipe.getOrderCode()).filter(code -> StringUtils.isNotEmpty(code)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(recipeCodes)) {
                List<RecipeOrder> recipeOrders = orderDAO.findValidListbyCodes(recipeCodes);
                orderStatus = recipeOrders.stream().collect(Collectors.toMap(RecipeOrder::getOrderCode, RecipeOrder::getStatus));
            }
            LOGGER.info("instanceRecipesAndPatient orderStatus:{} ", JSONUtils.toString(orderStatus));

            List<Integer> recipeIds = recipes.stream().map(recipe -> recipe.getRecipeId()).collect(Collectors.toList());
            //获取处方对应的退费信息
            Map<Integer, Integer> refundIdMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(recipeIds)) {
                List<RecipeRefund> recipeRefunds = recipeRefundDAO.findRefundListByRecipeIdsAndNode(recipeIds);
                refundIdMap = recipeRefunds.stream().collect(Collectors.toMap(RecipeRefund::getBusId, RecipeRefund::getId, (k1, k2) -> k1));
            }
            LOGGER.info("instanceRecipesAndPatient refundIdMap:{} ", JSONUtils.toString(refundIdMap));

            //获取处方对应的处方详情
            Map<Integer, List<Recipedetail>> recipeDetailMap = new HashMap<>();
            LOGGER.info("instanceRecipesAndPatient recipeIds:{} ", JSONUtils.toString(recipeIds));
            if (CollectionUtils.isNotEmpty(recipeIds)) {
                List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeIds(recipeIds);
                LOGGER.info("instanceRecipesAndPatient recipedetails:{} ", JSONUtils.toString(recipedetails));
                recipeDetailMap = recipedetails.stream().collect(Collectors.toMap(Recipedetail::getRecipeId, part -> Lists.newArrayList(part), (List<Recipedetail> newValueList, List<Recipedetail> oldValueList) -> {
                    oldValueList.addAll(newValueList);
                    return oldValueList;
                }));
            }
            LOGGER.info("instanceRecipesAndPatient recipeDetailMap:{} ", JSONUtils.toString(recipeDetailMap));

            //获取处方对应的药品信息
            Map<String, OrganDrugList> organDrugListMap = new HashMap<>();
            LOGGER.info("instanceRecipesAndPatient recipeIds:{} ", JSONUtils.toString(recipeIds));
            if (CollectionUtils.isNotEmpty(recipeIds)) {
                List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(recipeIds);
                LOGGER.info("instanceRecipesAndPatient organDrugLists:{} ", JSONUtils.toString(organDrugLists));
                organDrugListMap = organDrugLists.stream().collect(Collectors.toMap(k -> k.getOrganId() + "_" + k.getDrugId(), a -> a, (k1, k2) -> k1));
                LOGGER.info("instanceRecipesAndPatient organDrugListMap:{} ", JSONUtils.toString(organDrugListMap));
            }
            for (Recipe recipe : recipes) {
                Map<String, Object> map = Maps.newHashMap();
                //设置处方具体药品名称---取第一个药
                List<Recipedetail> recipedetails = recipeDetailMap.get(recipe.getRecipeId());
                if (null != recipedetails && recipedetails.size() > 0) {
                    //这里反向取一下要，前面跌倒了
                    recipe.setRecipeDrugName(DrugNameDisplayUtil.dealwithRecipeDrugName(recipedetails.get(recipedetails.size() - 1), recipe.getRecipeType(), recipe.getClinicOrgan()));
                }
                recipe.setRecipeShowTime(recipe.getCreateDate());
                boolean effective = false;
                //只有审核未通过的情况需要看订单状态
//                if (RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
//                    effective = orderDAO.isEffectiveOrder(recipe.getOrderCode(), recipe.getPayMode());
//                }
                //添加订单的状态
                Map<String, String> tipMap = RecipeServiceSub.getTipsByStatusCopy2(recipe.getStatus(), recipe, null, (orderStatus == null || 0 >= orderStatus.size()) ? null : orderStatus.get(recipe.getOrderCode()), refundIdMap.get(recipe.getRecipeId()));

                recipe.setShowTip(MapValueUtil.getString(tipMap, "listTips"));


                Integer pharmacyId = recipedetails.get(0).getPharmacyId();
                PharmacyTcm pharmacyTcm = null;
                if (Objects.nonNull(pharmacyId)) {
                    pharmacyTcm = pharmacyTcmDAO.get(pharmacyId);
                }

                PharmacyTcm finalPharmacyTcm = pharmacyTcm;
                List<HisRecipeDetailBean> collect1 = recipedetails.stream().map(recipeDetail -> {
                    HisRecipeDetailBean convert = ObjectCopyUtils.convert(recipeDetail, HisRecipeDetailBean.class);
                    if (Objects.nonNull(finalPharmacyTcm)) {
                        convert.setPharmacyCode(finalPharmacyTcm.getPharmacyCode());
                    }
                    return convert;
                }).collect(Collectors.toList());
                map.put("recipe", RecipeServiceSub.convertRecipeForRAPNew(recipe, collect1));
                map.put("patient", patient);
                //LOGGER.info("instanceRecipesAndPatient map:{}", JSONUtils.toString(map));
                list.add(map);
            }

        }
        LOGGER.info("instanceRecipesAndPatient response recipes:{} ,patient:{} ,list:{}", JSONUtils.toString(recipes), JSONUtils.toString(patient), JSONUtils.toString(list));
        Long totalConsumedTime = new Date().getTime() - beginTime;
        LOGGER.info("instanceRecipesAndPatient cost:{}", totalConsumedTime);
        return list;
    }

    /**
     * 获取患者的所有处方单-web福建省立
     *
     * @param mpiId
     * @param start
     * @return
     */
    @RpcService
    public Map<String, Object> findAllRecipesForPatient(String mpiId, Integer organId, int start, int limit) {
        LOGGER.info("findAllRecipesForPatient mpiId =" + mpiId);
        Map<String, Object> result = Maps.newHashMap();
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        QueryResult<Recipe> resultList = recipeDAO.findRecipeListByMpiID(mpiId, organId, start, limit);
        List<Recipe> list = resultList.getItems();
        if (CollectionUtils.isEmpty(list)) {
            return result;
        }
        result.put("total", resultList.getTotal());
        result.put("start", resultList.getStart());
        result.put("limit", resultList.getLimit());
        List<Map<String, Object>> mapList = Lists.newArrayList();
        Map<String, Object> map;
        List<Recipedetail> recipedetails;
        try {
            Dictionary usingRateDic = DictionaryController.instance().get("eh.cdr.dictionary.UsingRate");
            Dictionary usePathwaysDic = DictionaryController.instance().get("eh.cdr.dictionary.UsePathways");
            Dictionary departDic = DictionaryController.instance().get("eh.base.dictionary.Depart");
            String organText = DictionaryController.instance().get("eh.base.dictionary.Organ").getText(organId);
            for (Recipe recipe : list) {
                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
                map = Maps.newHashMap();
                map.put("recipeId", recipe.getRecipeId());
                map.put("patientName", recipe.getPatientName());
                map.put("doctorDepart", organText + departDic.getText(recipe.getDepart()));
                map.put("diseaseName", recipe.getOrganDiseaseName());
                map.put("signTime", DateConversion.getDateFormatter(recipe.getSignDate(), "MM月dd日 HH:mm"));
                map.put("doctorName", recipe.getDoctorName());
                recipedetails = detailDAO.findByRecipeId(recipe.getRecipeId());

                Map<String, String> drugInfo;
                List<Map<String, String>> drugInfoList = Lists.newArrayList();
                String useDose;
                String usingRateText;
                String usePathwaysText;
                for (Recipedetail detail : recipedetails) {
                    drugInfo = Maps.newHashMap();
                    if (StringUtils.isNotEmpty(detail.getUseDoseStr())) {
                        useDose = detail.getUseDoseStr();
                    } else {
                        useDose = detail.getUseDose() == null ? null : String.valueOf(detail.getUseDose());
                    }
                    drugInfo.put("drugName", detail.getDrugName());
                    //开药总量+药品单位
                    String dSpec = "*" + detail.getUseTotalDose().intValue() + detail.getDrugUnit();
                    drugInfo.put("drugTotal", dSpec);
                    usingRateText = StringUtils.isNotEmpty(detail.getUsingRateTextFromHis()) ? detail.getUsingRateTextFromHis() : usingRateDic.getText(detail.getUsingRate());
                    usePathwaysText = StringUtils.isNotEmpty(detail.getUsePathwaysTextFromHis()) ? detail.getUsePathwaysTextFromHis() : usePathwaysDic.getText(detail.getUsePathways());
                    String useWay = "用法：每次" + useDose + detail.getUseDoseUnit() + "/" + usingRateText + "/" + usePathwaysText + detail.getUseDays() + "天";
                    drugInfo.put("useWay", useWay);
                    drugInfoList.add(drugInfo);
                }
                map.put("rp", drugInfoList);
                map.put("memo", recipe.getMemo());
                switch (recipe.getStatus()) {
                    case RecipeStatusConstant.CHECK_PASS:
                        if (!Objects.isNull(recipeExtend) && StringUtils.isNotEmpty(recipeExtend.getPharmNo())) {
                            map.put("statusText", "药师审核处方通过，请去医院取药窗口取药:[" + recipeExtend.getPharmNo() + "]");
                        } else {
                            map.put("statusText", "药师审核处方通过，请去医院取药窗口取药");
                        }
                        break;
                    case RecipeStatusConstant.NO_OPERATOR:
                        map.put("statusText", "已取消(超过三天未取药)");
                        break;
                    case RecipeStatusConstant.REVOKE:
                        map.put("statusText", "由于医生已撤销，该处方单已失效，请联系医生.");
                        break;
                    case RecipeStatusConstant.FINISH:
                        map.put("statusText", "已完成");
                        break;
                    case RecipeStatusConstant.READY_CHECK_YS:
                        map.put("statusText", "等待药师审核处方");
                        break;
                    case RecipeStatusConstant.CHECK_NOT_PASS_YS:
                        map.put("statusText", "药师审核处方不通过，请联系开方医生");
                        break;
                    default:
                        map.put("statusText", DictionaryController.instance().get("eh.cdr.dictionary.RecipeStatus").getText(recipe.getStatus()));
                        break;
                }
                mapList.add(map);
            }
            result.put("list", mapList);
        } catch (Exception e) {
            LOGGER.error("findAllRecipesForPatient error" + e.getMessage(), e);
        }

        return result;
    }

    @Deprecated
    @RpcService
    public List<PatientTabStatusRecipeDTO> findRecipesForPatientAndTabStatus(String tabStatus, String mpiId, Integer index, Integer limit) {
        LOGGER.info("findRecipesForPatientAndTabStatus tabStatus:{} mpiId:{} ", tabStatus, mpiId);
        List<PatientTabStatusRecipeDTO> recipeList = new ArrayList<>();
        Assert.hasLength(mpiId, "findRecipesForPatientAndTabStatus 用户id为空!");
        checkUserHasPermissionByMpiId(mpiId);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        List<String> allMpiIds = recipeService.getAllMemberPatientsByCurrentPatient(mpiId);
        //获取页面展示的对象
        TabStatusEnum recipeStatusList = TabStatusEnum.fromTabStatusAndStatusType(tabStatus, "recipe");
        if (null == recipeStatusList) {
            LOGGER.error("findRecipesForPatientAndTabStatus:{}tab没有查询到recipe的状态列表", tabStatus);
            return recipeList;
        }
        TabStatusEnum orderStatusList = TabStatusEnum.fromTabStatusAndStatusType(tabStatus, "order");
        if (null == orderStatusList) {
            LOGGER.error("findRecipesForPatientAndTabStatus:{}tab没有查询到order的状态列表", tabStatus);
            return recipeList;
        }
        List<Integer> specialStatusList = new ArrayList<>();
        if ("ongoing".equals(tabStatus)) {
            specialStatusList.add(RecipeStatusConstant.RECIPE_DOWNLOADED);
            //date 20200511
            //添加处方单中新加的药师签名中签名失败的，患者认为是待审核
            //date 20200922 【CA药师未签名】患者认为是待审核
            specialStatusList.addAll(new ArrayList<Integer>() {
                private static final long serialVersionUID = -1964815829160506615L;

                {
                    add(RecipeStatusConstant.SIGN_ERROR_CODE_PHA);
                    add(RecipeStatusConstant.SIGN_ING_CODE_PHA);
                    add(RecipeStatusConstant.SIGN_NO_CODE_PHA);
                }
            });
        }
        try {
            List<PatientRecipeBean> backList = recipeDAO.findTabStatusRecipesForPatient(allMpiIds, index, limit, recipeStatusList.getStatusList(), orderStatusList.getStatusList(), specialStatusList, tabStatus);
            return processTabListDate(backList, allMpiIds);
        } catch (Exception e) {
            LOGGER.error("findRecipesForPatientAndTabStatus error :.", e);
        }
        return null;
    }

    /**
     * 患者端获取患者处方列表页
     *
     * @param tabStatus onready待处理 ongoing进行中 isover已结束
     * @param mpiId     就诊人
     * @param index
     * @param limit
     * @return
     */
    @RpcService
    public List<PatientTabStatusMergeRecipeDTO> findRecipesForPatientAndTabStatusNew(String tabStatus, String mpiId, Integer index, Integer limit) {
        LOGGER.info("findRecipesForPatientAndTabStatusNew tabStatus:{} mpiId:{} index:{} limit:{} ", tabStatus, mpiId, index, limit);
        Assert.hasLength(mpiId, "findRecipesForPatientAndTabStatusNew mpiId为空!");
        checkUserHasPermissionByMpiId(mpiId);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);

        List<String> allMpiIds = recipeService.getAllMemberPatientsByCurrentPatient(mpiId);
        List<PatientTabStatusMergeRecipeDTO> patientTabStatusMergeRecipeDTOS = Lists.newArrayList();
        //获取页面展示的对象
        TabStatusEnumNew recipeStatusList = TabStatusEnumNew.fromTabStatusAndStatusType(tabStatus, "recipe");
        if (null == recipeStatusList) {
            LOGGER.error("findRecipesForPatientAndTabStatusNew {}tab没有查询到recipe的状态列表", tabStatus);
            return patientTabStatusMergeRecipeDTOS;
        }
        TabStatusEnumNew orderStatusList = TabStatusEnumNew.fromTabStatusAndStatusType(tabStatus, "order");
        if (null == orderStatusList) {
            LOGGER.error("findRecipesForPatientAndTabStatusNew {}tab没有查询到order的状态列表", tabStatus);
            return patientTabStatusMergeRecipeDTOS;
        }
        GroupRecipeConf groupRecipeConf = groupRecipeManager.getMergeRecipeSetting();
        Boolean mergeRecipeFlag = groupRecipeConf.getMergeRecipeFlag();
        String mergeRecipeWayAfter = groupRecipeConf.getMergeRecipeWayAfter();
        if (RecipeListTabStatusEnum.ON_READY.getText().equals(tabStatus)) {
            // 待处理的走原来老的方法
            patientTabStatusMergeRecipeDTOS = getRecipeByOnReady(mergeRecipeFlag, allMpiIds, index, limit, tabStatus, mergeRecipeWayAfter, recipeStatusList, orderStatusList);
        } else if (RecipeListTabStatusEnum.ON_GOING.getText().equals(tabStatus) ||
                RecipeListTabStatusEnum.ON_OVER.getText().equals(tabStatus)) {
            // 已处理跟已完成 走 新的逻辑,合并处方展示仅看是否同一订单
            patientTabStatusMergeRecipeDTOS = getRecipeByGoingAndOver(patientTabStatusMergeRecipeDTOS, allMpiIds, index, limit, tabStatus, recipeStatusList, groupRecipeConf);
        }
        return patientTabStatusMergeRecipeDTOS;

    }

    /**
     * 已完成 与 进行中 处方列表查询
     *
     * @param result
     * @param allMpiIds
     * @param index
     * @param limit
     * @param tabStatus
     * @param recipeStatusList
     * @return
     */
    private List<PatientTabStatusMergeRecipeDTO> getRecipeByGoingAndOver(List<PatientTabStatusMergeRecipeDTO> result, List<String> allMpiIds, Integer index, Integer limit, String tabStatus, TabStatusEnumNew recipeStatusList, GroupRecipeConf groupRecipeConf) {
        List<RecipeListBean> recipeListByMPIId = recipeDAO.findRecipeListByMPIId(allMpiIds, index, limit, tabStatus, recipeStatusList.getStatusList());
        LOGGER.info("getRecipeByGoingAndOver recipeListByMPIId = {}", recipeListByMPIId);
        if (CollectionUtils.isEmpty(recipeListByMPIId)) {
            return result;
        }
        Map<String, List<RecipeListBean>> orderMap = recipeListByMPIId.stream().filter(recipeListBean -> recipeListBean.getOrderCode() != null).collect(Collectors.groupingBy(RecipeListBean::getOrderCode));
        Set<Integer> recipeIds = new HashSet<>();
        Boolean mergeRecipeFlag = groupRecipeConf.getMergeRecipeFlag();
        String mergeRecipeWayAfter = groupRecipeConf.getMergeRecipeWayAfter();
        recipeListByMPIId.forEach(recipeListBean -> {
            if (!recipeIds.contains(recipeListBean.getRecipeId())) {
                PatientTabStatusMergeRecipeDTO patientTabStatusMergeRecipeDTO = new PatientTabStatusMergeRecipeDTO();
                // 获取合并处方的关键字
                patientTabStatusMergeRecipeDTO.setFirstRecipeId(recipeListBean.getRecipeId());
                patientTabStatusMergeRecipeDTO.setMergeRecipeFlag(mergeRecipeFlag);
                patientTabStatusMergeRecipeDTO.setMergeRecipeWay(mergeRecipeWayAfter);
                if ("e.registerId".equals(mergeRecipeWayAfter)) {
                    // 挂号序号
                    patientTabStatusMergeRecipeDTO.setGroupField(recipeListBean.getRegisterID());
                } else {
                    // 慢病名称
                    patientTabStatusMergeRecipeDTO.setGroupField(recipeListBean.getChronicDiseaseName());
                }
                String orderCode = recipeListBean.getOrderCode();
                List<PatientTabStatusRecipeDTO> recipe = Lists.newArrayList();
                if (Objects.isNull(orderCode)) {
                    PatientTabStatusRecipeDTO patientTabStatusRecipeDTO = PatientTabStatusRecipeConvert(recipeListBean);
                    recipe.add(patientTabStatusRecipeDTO);
                    recipeIds.add(recipeListBean.getRecipeId());
                } else {
                    List<RecipeListBean> recipeListBeans = orderMap.get(orderCode);
                    recipeListBeans.forEach(recipeListBean1 -> {
                        PatientTabStatusRecipeDTO patientTabStatusRecipeDTO = PatientTabStatusRecipeConvert(recipeListBean1);
                        recipe.add(patientTabStatusRecipeDTO);
                        recipeIds.add(recipeListBean1.getRecipeId());
                    });
                }
                patientTabStatusMergeRecipeDTO.setRecipe(recipe);
                result.add(patientTabStatusMergeRecipeDTO);
            }
        });
        return result;
    }

    /**
     * recipeListBean 转换 PatientTabStatusRecipeDTO
     *
     * @param recipeListBean
     * @return
     */
    private PatientTabStatusRecipeDTO PatientTabStatusRecipeConvert(RecipeListBean recipeListBean) {
        PatientTabStatusRecipeDTO patientTabStatusRecipeDTO = ObjectCopyUtils.convert(recipeListBean, PatientTabStatusRecipeDTO.class);
        patientTabStatusRecipeDTO.setStatusText(RecipeStatusEnum.getRecipeStatusEnum(recipeListBean.getStatus()).getName());
        patientTabStatusRecipeDTO.setStatusCode(recipeListBean.getStatus());
        patientTabStatusRecipeDTO.setRecordCode(recipeListBean.getOrderCode());
        patientTabStatusRecipeDTO.setRecordId(recipeListBean.getOrderId());

        String recipeNumber = configurationClient.getValueCatch(recipeListBean.getClinicOrgan(), "recipeNumber", "");
        if (StringUtils.isNotEmpty(recipeNumber)) {
            patientTabStatusRecipeDTO.setRecipeNumber(recipeNumber);
        }
        patientTabStatusRecipeDTO.setDepartName(DictionaryUtil.getDictionary("eh.base.dictionary.Depart", recipeListBean.getDepart()));
        patientTabStatusRecipeDTO.setMpiId(recipeListBean.getMpiid());
        patientTabStatusRecipeDTO.setOrganId(recipeListBean.getClinicOrgan());
        //存入每个页面的按钮信息（展示那种按钮，如果是购药按钮展示哪些按钮）
        Recipe recipe = recipeDAO.get(recipeListBean.getRecipeId());
        GiveModeShowButtonVO giveModeShowButtonVO = getShowButtonNew(null, recipe);
        patientTabStatusRecipeDTO.setGiveModeShowButtonVO(giveModeShowButtonVO);
        patientTabStatusRecipeDTO.setJumpPageType(Objects.isNull(recipe.getOrderCode()) ? RECIPE_PAGE : ORDER_PAGE);
        return patientTabStatusRecipeDTO;
    }

    /**
     * 待处理状态 处方列表查询
     *
     * @param mergeRecipeFlag
     * @param allMpiIds
     * @param index
     * @param limit
     * @param tabStatus
     * @param mergeRecipeWayAfter
     * @param recipeStatusList
     * @param orderStatusList
     * @return
     */
    private List<PatientTabStatusMergeRecipeDTO> getRecipeByOnReady(Boolean mergeRecipeFlag, List<String> allMpiIds, Integer index, Integer limit, String tabStatus, String mergeRecipeWayAfter, TabStatusEnumNew recipeStatusList, TabStatusEnumNew orderStatusList) {
        try {
            if (mergeRecipeFlag) {
                //返回合并处方
                return findMergeRecipe(allMpiIds, index, limit, recipeStatusList.getStatusList(), orderStatusList.getStatusList(), tabStatus, mergeRecipeWayAfter);
            } else {
                //返回非合并处方
                return findNoMergeRecipe(allMpiIds, index, limit, recipeStatusList.getStatusList(), orderStatusList.getStatusList(), tabStatus);
            }
        } catch (Exception e) {
            LOGGER.error("findRecipesForPatientAndTabStatusNew error sql", e);
            throw new DAOException(609, e.getMessage());
        }
    }

    @RpcService
    public List<PatientTabStatusMergeRecipeDTO> findNoMergeRecipe(List<String> allMpiIds, Integer index, Integer limit, List<Integer> recipeStatusList, List<Integer> orderStatusList, String tabStatus) {
        //还是用原来的方法获取处方
        List<PatientTabStatusMergeRecipeDTO> backList = Lists.newArrayList();
        PatientTabStatusMergeRecipeDTO mergeRecipeDTO;
        List<Integer> recipeIdWithoutHisAndPayList = recipeDAO.findRecipeIdWithoutHisAndPay(allMpiIds);
        List<PatientRecipeBean> backRecipeList = recipeDAO.findTabStatusRecipesForPatientNew(allMpiIds, index, limit, recipeStatusList, orderStatusList, tabStatus, recipeIdWithoutHisAndPayList);
        List<PatientTabStatusRecipeDTO> patientTabStatusRecipeDTOS = processTabListDate(backRecipeList, allMpiIds);
        for (PatientTabStatusRecipeDTO tabStatusRecipeDTO : patientTabStatusRecipeDTOS) {
            mergeRecipeDTO = new PatientTabStatusMergeRecipeDTO();
            mergeRecipeDTO.setRecipe(Arrays.asList(tabStatusRecipeDTO));
            mergeRecipeDTO.setMergeRecipeFlag(false);
            backList.add(mergeRecipeDTO);
        }
        return backList;
    }

    @RpcService
    public List<PatientTabStatusMergeRecipeDTO> findMergeRecipe(List<String> allMpiIds, Integer index, Integer limit, List<Integer> statusList, List<Integer> orderStatusList, String tabStatus, String mergeRecipeWay) {
        //先获取挂号序号和处方id的关系
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        PatientTabStatusMergeRecipeDTO mergeRecipeDTO;
        List<PatientTabStatusMergeRecipeDTO> backList = Lists.newArrayList();
        Map<String, List<Integer>> registerIdRelation;
        //已结束的tab页应该拆成有订单和无订单的合并
        if ("isover".equals(tabStatus)) {
            registerIdRelation = recipeDAO.findRecipeIdAndRegisterIdRelationByIsover(allMpiIds, index, limit, statusList, orderStatusList, mergeRecipeWay);
        } else {
            registerIdRelation = recipeDAO.findRecipeIdAndRegisterIdRelation(allMpiIds, index, limit, statusList, orderStatusList, tabStatus, mergeRecipeWay);
        }
        LOGGER.info("findMergeRecipe tabStatus={},statusList={},registerIdRelation={}", tabStatus, JSONUtils.toString(statusList), JSONUtils.toString(registerIdRelation));
        for (Map.Entry<String, List<Integer>> entry : registerIdRelation.entrySet()) {
            String key = "";
            if (StringUtils.isNotEmpty(entry.getKey())) {
                key = entry.getKey().split(",")[0];
            }
            //具体到某一个挂号序号下的处方列表
            //先处理挂号序号为空的情况 -1
            if ("-1".equals(key)) {
                //挂号序号为空表示不能合并处方单
                for (Integer recipeId : entry.getValue()) {
                    mergeRecipeDTO = new PatientTabStatusMergeRecipeDTO();
                    mergeRecipeDTO.setRecipe(processTabListDataNew(Arrays.asList(recipeId)));
                    mergeRecipeDTO.setFirstRecipeId(recipeId);
                    mergeRecipeDTO.setMergeRecipeFlag(true);
                    mergeRecipeDTO.setMergeRecipeWay(mergeRecipeWay);
                    backList.add(mergeRecipeDTO);
                }
            } else {
                mergeRecipeDTO = new PatientTabStatusMergeRecipeDTO();
                //分组字段值
                mergeRecipeDTO.setGroupField(key);
                mergeRecipeDTO.setMergeRecipeWay(mergeRecipeWay);
                mergeRecipeDTO.setRecipe(processTabListDataNew(entry.getValue()));
                mergeRecipeDTO.setFirstRecipeId(entry.getValue().get(0));
                mergeRecipeDTO.setMergeRecipeFlag(true);
                backList.add(mergeRecipeDTO);
            }
        }
        //按处方id从大到小排列
        backList.sort((o1, o2) -> o2.getFirstRecipeId() - o1.getFirstRecipeId());
        return backList;
    }

    /**
     * 处理tab下的列表数据New
     */
    private List<PatientTabStatusRecipeDTO> processTabListDataNew(List<Integer> recipeIds) {
        List<PatientTabStatusRecipeDTO> backList = Lists.newArrayList();
        if (CollectionUtils.isEmpty(recipeIds)) {
            return backList;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIds);
        if (CollectionUtils.isEmpty(recipes)) {
            return backList;
        }
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        Map<Integer, Boolean> checkEnterprise = Maps.newHashMap();
        DrugsEnterpriseService drugsEnterpriseService = ApplicationUtils.getRecipeService(DrugsEnterpriseService.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        for (Recipe recipe : recipes) {
            PatientTabStatusRecipeDTO patientRecipe = new PatientTabStatusRecipeDTO();
            patientRecipe.setRecipeId(recipe.getRecipeId());
            patientRecipe.setOrganId(recipe.getClinicOrgan());
            try {
                Object recipeNumber = configService.getConfiguration(recipe.getClinicOrgan(), "recipeNumber");
                LOGGER.info("processTabListDataNew  recipeId={},recipeNumber={}", recipe.getRecipeId(), recipeNumber);
                if (null != recipeNumber && StringUtils.isNotEmpty(recipeNumber.toString())) {
                    patientRecipe.setRecipeNumber(recipeNumber.toString());
                }
            } catch (Exception e) {
                LOGGER.error("processTabListDataNew error recipeId={}", recipe.getRecipeId());
            }
            patientRecipe.setMpiId(recipe.getMpiid());
            patientRecipe.setPatientName(recipe.getPatientName());
            //能否购药进行设置，默认可购药
            patientRecipe.setCheckEnterprise(true);
            if (null != patientRecipe.getOrganId()) {
                if (null == checkEnterprise.get(patientRecipe.getOrganId())) {
                    checkEnterprise.put(patientRecipe.getOrganId(), drugsEnterpriseService.checkEnterprise(patientRecipe.getOrganId()));
                }
                patientRecipe.setCheckEnterprise(checkEnterprise.get(patientRecipe.getOrganId()));
            }
            //处方详情
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
            patientRecipe.setOrganDiseaseName(recipe.getOrganDiseaseName());
            patientRecipe.setDoctorName(recipe.getDoctorName());
            try {
                patientRecipe.setDepartName(DictionaryController.instance().get("eh.base.dictionary.Depart").getText(recipe.getDepart()));
            } catch (ControllerException e) {
                LOGGER.warn("processTabListDataNew 字典转化异常");
            }
            patientRecipe.setRecipeCode(recipe.getRecipeCode());
            patientRecipe.setGiveMode(recipe.getGiveMode());
            patientRecipe.setSignDate(recipe.getSignDate());
            patientRecipe.setRecipeMode(recipe.getRecipeMode());
            patientRecipe.setPayFlag(recipe.getPayFlag());
            patientRecipe.setRecipeSource(recipe.getRecipeSource());
            patientRecipe.setRecipeType(recipe.getRecipeType());
            patientRecipe.setTotalMoney(recipe.getTotalMoney());

            if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
                patientRecipe.setRecordType(LIST_TYPE_ORDER);
                RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                if (recipeOrder != null) {
                    patientRecipe.setRecordId(recipeOrder.getOrderId());
                    patientRecipe.setRecordCode(recipeOrder.getOrderCode());
                    patientRecipe.setStatusCode(recipeOrder.getStatus());
                    patientRecipe.setStatusText(getOrderStatusTabText(patientRecipe.getStatusCode(), patientRecipe.getGiveMode(), recipe.getStatus()));
                    patientRecipe.setEnterpriseId(recipeOrder.getEnterpriseId());
                    if (null != recipeOrder.getLogisticsCompany()) {
                        try {
                            //4.01需求：物流信息查询
                            String logComStr = DictionaryController.instance().get("eh.cdr.dictionary.KuaiDiNiaoCode").getText(recipeOrder.getLogisticsCompany());
                            patientRecipe.setLogisticsCompany(logComStr);
                            patientRecipe.setTrackingNumber(recipeOrder.getTrackingNumber());
                            DrugsEnterpriseDAO drugsEnterpriseDAO = getDAO(DrugsEnterpriseDAO.class);
                            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipeOrder.getEnterpriseId());
                            if (drugsEnterprise != null) {
                                // 药企物流对接方式
                                patientRecipe.setLogisticsType(drugsEnterprise.getLogisticsType());
                            }
                        } catch (ControllerException e) {
                            LOGGER.warn("findRecipesForPatientAndTabStatus: 获取物流信息失败，物流方code={}", recipeOrder.getLogisticsCompany(), e);
                        }
                    }
                    if (RecipeStatusConstant.CHECK_PASS == recipe.getStatus() && OrderStatusConstant.READY_PAY.equals(recipeOrder.getStatus())) {
                        patientRecipe.setRecipeSurplusHours(recipe.getRecipeSurplusHours());
                    }
                }
            } else {
                patientRecipe.setRecordType(LIST_TYPE_RECIPE);
                patientRecipe.setRecordId(recipe.getRecipeId());
                patientRecipe.setRecordCode(recipe.getRecipeId().toString());
                patientRecipe.setStatusCode(recipe.getStatus());
                patientRecipe.setStatusText(getRecipeStatusTabText(patientRecipe.getStatusCode(), patientRecipe.getRecordId()));
                //设置失效时间
                if (RecipeStatusConstant.CHECK_PASS == patientRecipe.getStatusCode()) {
                    patientRecipe.setRecipeSurplusHours(RecipeServiceSub.getRecipeSurplusHours(patientRecipe.getSignDate()));
                }
            }
            //药品详情
            List<Recipedetail> recipedetailList = detailDAO.findByRecipeId(recipe.getRecipeId());
            patientRecipe.setRecipeDetail(ObjectCopyUtils.convert(recipedetailList, RecipeDetailBean.class));
            //添加处方笺文件，获取用户处方信息中的处方id，获取处方笺文件,设置跳转的页面
            getPageMsg(patientRecipe, recipe);
            //存入每个页面的按钮信息（展示那种按钮，如果是购药按钮展示哪些按钮）
            GiveModeShowButtonVO giveModeShowButtonVO = getShowButtonNew(patientRecipe, recipe);
            patientRecipe.setGiveModeShowButtonVO(giveModeShowButtonVO);
            patientRecipe.setButtons(getShowButton(patientRecipe, recipe));
            //根据隐方配置返回处方详情
            boolean isReturnRecipeDetail = isReturnRecipeDetail(patientRecipe.getRecipeId());
            if (!isReturnRecipeDetail && CollectionUtils.isNotEmpty(patientRecipe.getRecipeDetail())) {
                patientRecipe.getRecipeDetail().forEach(a -> {
                    a.setDrugName(null);
                    a.setDrugSpec(null);
                });
            }
            //返回是否隐方
            patientRecipe.setIsHiddenRecipeDetail(!isReturnRecipeDetail);
            backList.add(patientRecipe);
        }

        return backList;
    }

    /**
     * 处理tab下的列表数据
     *
     * @param list
     * @param allMpiIds
     */
    private List<PatientTabStatusRecipeDTO> processTabListDate(List<PatientRecipeBean> list, List<String> allMpiIds) {
        if (CollectionUtils.isEmpty(list)) {
            return Lists.newArrayList();
        }
        List<PatientTabStatusRecipeDTO> backList = ObjectCopyUtils.convert(list, PatientTabStatusRecipeDTO.class);
        //处理订单类型数据
        List<PatientDTO> patientList = patientService.findByMpiIdIn(allMpiIds);
        Map<String, PatientDTO> patientMap = patientList.stream().collect(Collectors.toMap(PatientDTO::getMpiId, a -> a, (k1, k2) -> k1));
        Map<Integer, Boolean> checkEnterprise = Maps.newHashMap();
        for (PatientTabStatusRecipeDTO record : backList) {
            PatientDTO p = patientMap.get(record.getMpiId());
            if (null != p) {
                record.setPatientName(p.getPatientName());
                record.setPhoto(p.getPhoto());
                record.setPatientSex(p.getPatientSex());
            }
            //能否购药进行设置，默认可购药
            record.setCheckEnterprise(true);
            if (null != record.getOrganId()) {
                if (null == checkEnterprise.get(record.getOrganId())) {
                    checkEnterprise.put(record.getOrganId(), drugsEnterpriseService.checkEnterprise(record.getOrganId()));
                }
                record.setCheckEnterprise(checkEnterprise.get(record.getOrganId()));
            }
            Recipe recipe = null;
            if (LIST_TYPE_RECIPE.equals(record.getRecordType())) {
                record.setStatusText(getRecipeStatusTabText(record.getStatusCode(), record.getRecordId()));
                //设置失效时间
                if (RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType().equals(record.getStatusCode())) {
                    record.setRecipeSurplusHours(RecipeServiceSub.getRecipeSurplusHours(record.getSignDate()));
                }
                //处方详情
                recipe = recipeDAO.getByRecipeId(record.getRecordId());
                if (recipe != null) {
                    RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                    EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
                    record.setOrganDiseaseName(recipe.getOrganDiseaseName());
                    record.setDoctorName(recipe.getDoctorName());
                    record.setDepartName(DictionaryUtil.getDictionary("eh.base.dictionary.Depart", recipe.getDepart()));
                    record.setRecipeCode(recipe.getRecipeCode());
                }

                //药品详情
                List<Recipedetail> recipedetailList = recipeDetailDAO.findByRecipeId(record.getRecordId());
                for (Recipedetail recipedetail : recipedetailList) {
                    List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(recipedetail.getDrugId(), record.getOrganId());
                    if (CollectionUtils.isNotEmpty(organDrugLists)) {
                        recipedetail.setDrugForm(organDrugLists.get(0).getDrugForm());
                    }
                }
                record.setRecipeDetail(ObjectCopyUtils.convert(recipedetailList, RecipeDetailBean.class));
            } else if (LIST_TYPE_ORDER.equals(record.getRecordType())) {
                recipe = recipeDAO.get(0 == record.getRecipeId() ? record.getRecordId() : record.getRecipeId());
                record.setStatusText(getOrderStatusTabText(record.getStatusCode(), record.getGiveMode(), recipe.getStatus()));
                RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                RecipeResultBean resultBean = orderService.getOrderDetailById(record.getRecordId());
                if (!RecipeResultBean.SUCCESS.equals(resultBean.getCode())) {
                    continue;
                }
                if (null == resultBean.getObject() || !(resultBean.getObject() instanceof RecipeOrderBean)) {
                    continue;
                }
                RecipeOrderBean order = (RecipeOrderBean) resultBean.getObject();
                record.setEnterpriseId(order.getEnterpriseId());
                if (null != order.getLogisticsCompany()) {
                    //4.01需求：物流信息查询
                    record.setLogisticsCompany(DictionaryUtil.getDictionary("eh.cdr.dictionary.KuaiDiNiaoCode", order.getLogisticsCompany()));
                    record.setTrackingNumber(order.getTrackingNumber());
                    // 药企物流对接方式
                    Optional.ofNullable(drugsEnterpriseDAO.getById(order.getEnterpriseId())).ifPresent(a -> record.setLogisticsType(a.getLogisticsType()));
                }
                Optional.ofNullable((List<PatientRecipeDTO>) order.getList()).orElseGet(Collections::emptyList).forEach(a -> {
                    record.setRecipeId(a.getRecipeId());
                    record.setRecipeType(a.getRecipeType());
                    record.setOrganDiseaseName(a.getOrganDiseaseName());
                    record.setRecipeMode(a.getRecipeMode());
                    // 订单支付方式
                    record.setPayMode(a.getPayMode());
                    record.setDoctorName(a.getDoctorName());
                    record.setDepartName(a.getDepartName());
                    record.setRecipeCode(a.getRecipeCode());
                    record.setSignDate(a.getSignDate());

                    //药品详情
                    List<RecipeDetailBean> recipedetailList = a.getRecipeDetail();
                    for (RecipeDetailBean recipedetail : recipedetailList) {
                        Recipe recipes = recipeDAO.getByRecipeId(recipedetail.getRecipeId());
                        List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(recipedetail.getDrugId(), recipes.getClinicOrgan());
                        if (CollectionUtils.isNotEmpty(organDrugLists)) {
                            recipedetail.setDrugForm(organDrugLists.get(0).getDrugForm());
                        }
                    }
                    record.setRecipeDetail(recipedetailList);
                    if (RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType().equals(a.getStatusCode()) && RecipeOrderStatusEnum.ORDER_STATUS_READY_PAY.getType().equals(record.getStatusCode())) {
                        record.setRecipeSurplusHours(a.getRecipeSurplusHours());
                    }
                });
            }
            //添加处方笺文件，获取用户处方信息中的处方id，获取处方笺文件,设置跳转的页面
            getPageMsg(record, recipe);
            //存入每个页面的按钮信息（展示那种按钮，如果是购药按钮展示哪些按钮）
            GiveModeShowButtonVO giveModeShowButtonVO = getShowButtonNew(record, recipe);
            record.setGiveModeShowButtonVO(giveModeShowButtonVO);
            record.setButtons(getShowButton(record, recipe));
            boolean isReturnRecipeDetail = isReturnRecipeDetail(record.getRecipeId());
            //返回是否隐方
            record.setIsHiddenRecipeDetail(!isReturnRecipeDetail);
            //根据隐方配置返回处方详情
            if (!isReturnRecipeDetail && CollectionUtils.isNotEmpty(record.getRecipeDetail())) {
                record.getRecipeDetail().forEach(a -> {
                    a.setDrugName(null);
                    a.setDrugSpec(null);
                });
            }
            try {
                Object recipeNumber = configService.getConfiguration(recipe.getClinicOrgan(), "recipeNumber");
                LOGGER.info("processTabListDate  recipeId={},recipeNumber={}", recipe.getRecipeId(), recipeNumber);
                if (null != recipeNumber && StringUtils.isNotEmpty(recipeNumber.toString())) {
                    record.setRecipeNumber(recipeNumber.toString());
                }
            } catch (Exception e) {
                LOGGER.error("processTabListDate error recipeId={}", recipe.getRecipeId());
            }
        }

        return backList;
    }

    /**
     * 是否返回处方详情
     *
     * @param
     * @return
     * @author liumin
     */
    public boolean isReturnRecipeDetail(Integer recipeId) {
        if (recipeId == null) {
            throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "该处方单信息已变更，请退出重新获取处方信息。");
        }
        boolean isReturnRecipeDetail = true;//默认返回详情
        Recipe recipe = recipeDAO.get(recipeId);
        RecipeOrder order = orderDAO.getOrderByRecipeId(recipe.getRecipeId());
        LOGGER.info("isReturnRecipeDetail recipeId:{} recipe:{} order:{}", recipeId, JSONUtils.toString(recipe), JSONUtils.toString(order));
        try {
            //如果运营平台-配置管理 中药是否隐方的配置项, 选择隐方后,患者在支付成功处方费用后才可以显示中药明细，否则就隐藏掉对应的中药明细。
            Object isHiddenRecipeDetail = configService.getConfiguration(recipe.getClinicOrgan(), "isHiddenRecipeDetail");
            LOGGER.info("isReturnRecipeDetail 是否是中药：{} 是否隐方", RecipeUtil.isTcmType(recipe.getRecipeType()), isHiddenRecipeDetail);
            if (RecipeUtil.isTcmType(recipe.getRecipeType())//中药
                    && (boolean) isHiddenRecipeDetail == true//隐方)
            ) {
                //支付状态为非已支付
                if (order == null) {
                    if (recipe.getStatus() != 6) {// 未完成
                        isReturnRecipeDetail = false;//不返回详情
                    }
                } else {
                    LOGGER.info("isReturnRecipeDetail  order ！=null");
                    if (order.getPayMode() == 1 || "111".equals(order.getWxPayWay())) {// 线上支付（包括卫宁付）
                        if ((order.getPayFlag() != 1)) {
                            isReturnRecipeDetail = false;//不返回详情
                        }
                    } else {//线下支付
                        if (recipe.getStatus() != 6) {// 处方状态未完成
                            isReturnRecipeDetail = false;//不返回详情
                        }
                    }

                }
            }
        } catch (Exception e) {
            LOGGER.error("isReturnRecipeDetail error:{}", e);
        }

        return isReturnRecipeDetail;
    }

    /**
     * @param record 患者处方信息
     * @return void
     * @method getFile
     * @description 获取处方笺文件信息
     * @date: 2019/9/3
     * @author: JRK
     */
    private void getPageMsg(PatientTabStatusRecipeDTO record, Recipe recipe) {
        if (null == recipe) {
            LOGGER.warn("processTabListDate: recipeId:{},对应处方信息不存在,", record.getRecipeId());
            return;
        }
        record.setChemistSignFile(recipe.getChemistSignFile());
        record.setSignFile(recipe.getSignFile());
        record.setJumpPageType(getJumpPage(recipe));
        record.setOrderCode(recipe.getOrderCode());
        record.setClinicOrgan(recipe.getClinicOrgan());

    }

    /**
     * @param recipe 处方信息
     * @return java.lang.Integer
     * @method getJumpPage
     * @description 获取跳转的方式
     * @date: 2019/10/8
     * @author: JRK
     */
    private Integer getJumpPage(Recipe recipe) {
        return null == recipe.getOrderCode() ? RECIPE_PAGE : ORDER_PAGE;
    }


    private GiveModeShowButtonVO getShowButtonNew(PatientTabStatusRecipeDTO record, Recipe recipe) {
        GiveModeShowButtonVO giveModeShowButtonVO = new GiveModeShowButtonVO();
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        IGiveModeBase giveModeBase = GiveModeFactory.getGiveModeBaseByRecipe(recipe);
        try {
            //校验数据
            giveModeBase.validRecipeData(recipe);
        } catch (Exception e) {
            LOGGER.error("getShowButtonNew error:{}.", e.getMessage());
            return giveModeShowButtonVO;
        }
        //从运营平台获取配置项
        giveModeShowButtonVO = giveModeBase.getGiveModeSettingFromYypt(recipe.getClinicOrgan());
        if (CollectionUtils.isEmpty(giveModeShowButtonVO.getGiveModeButtons())) {
            return giveModeShowButtonVO;
        }
        //设置按钮是否可点击
        giveModeBase.setButtonOptional(giveModeShowButtonVO, recipe);
        //设置按钮展示类型
        giveModeBase.setButtonType(giveModeShowButtonVO, recipe);
        //设置特殊按钮
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        giveModeBase.setSpecialItem(giveModeShowButtonVO, recipe, recipeExtend);
        //设置列表不显示的按钮
        giveModeBase.setItemListNoShow(giveModeShowButtonVO, recipe);
        //后置设置处理
        giveModeBase.afterSetting(giveModeShowButtonVO, recipe);
        return giveModeShowButtonVO;
    }

    /**
     * @param record 获取医院的配置项
     * @return com.ngari.recipe.recipe.model.PayModeShowButtonBean：
     * 医院下购药方式展示的按钮对象
     * @method getShowButton
     * @description 获取页面上展示的按钮信息，按钮展示：true，不展示：false
     * @date: 2019/8/19
     * @author: JRK
     */
    private PayModeShowButtonBean getShowButton(PatientTabStatusRecipeDTO record, Recipe recipe) {
        PayModeShowButtonBean payModeShowButtonBean = new PayModeShowButtonBean();
        if (null == recipe) {
            LOGGER.warn("processTabListDate: recipeId:{},对应处方信息不存在,", record.getRecipeId());
            payModeShowButtonBean.noUserButtons();
            return payModeShowButtonBean;
        }
        //流转到扁鹊处方流转平台的处方购药按钮都不显示
        if (recipe.getEnterpriseId() != null && RecipeServiceSub.isBQEnterpriseBydepId(recipe.getEnterpriseId())) {
            payModeShowButtonBean.noUserButtons();
            return payModeShowButtonBean;
        }
        //获取配置项
        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(record.getRecipeMode()) || RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(record.getRecipeMode())) {

            //添加按钮配置项key
            Object payModeDeploy = configService.getConfiguration(record.getOrganId(), "payModeDeploy");
            if (null == payModeDeploy) {
                payModeShowButtonBean.noUserButtons();
                return payModeShowButtonBean;
            }
            List<String> configurations = new ArrayList<>(Arrays.asList((String[]) payModeDeploy));
            //将购药方式的显示map对象转化为页面展示的对象
            Map<String, Boolean> buttonMap = new HashMap<>(10);
            for (String configuration : configurations) {
                buttonMap.put(configuration, true);
            }
            payModeShowButtonBean = JSONUtils.parse(JSON.toJSONString(buttonMap), PayModeShowButtonBean.class);
            //当审方为前置并且审核没有通过，设置成不可选择

            //判断购药按钮是否可选状态的,当审方方式是前置且正在审核中时，不可选
            boolean isOptional = !(ReviewTypeConstant.Preposition_Check == recipe.getReviewType() && (RecipeStatusConstant.SIGN_NO_CODE_PHA == recipe.getStatus() || RecipeStatusConstant.SIGN_ERROR_CODE_PHA == recipe.getStatus() || RecipeStatusConstant.SIGN_ING_CODE_PHA == recipe.getStatus() || RecipeStatusConstant.READY_CHECK_YS == recipe.getStatus() || (RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus() && RecipecCheckStatusConstant.First_Check_No_Pass == recipe.getCheckStatus())));
            payModeShowButtonBean.setOptional(isOptional);

            //初始化互联网按钮信息（特殊化）
            if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(record.getRecipeMode())) {
                initInternetModel(record, payModeShowButtonBean, recipe);
            }
        } else {
            LOGGER.warn("processTabListDate: recipeId:{}  recipeMode:{},对应处方流转方式无法识别", record.getRecipeId(), record.getRecipeMode());
            payModeShowButtonBean.noUserButtons();
            return payModeShowButtonBean;
        }

        //设置按钮的展示类型
        Boolean showUseDrugConfig = (Boolean) configService.getConfiguration(record.getOrganId(), "medicationGuideFlag");
        //已完成的处方单设置
        if ((LIST_TYPE_ORDER.equals(record.getRecordType()) && RecipeOrderStatusEnum.ORDER_STATUS_DONE.getType().equals(record.getStatusCode())) || (LIST_TYPE_RECIPE.equals(record.getRecordType()) && RecipeStatusEnum.RECIPE_STATUS_FINISH.getType() == record.getStatusCode())) {

            //设置用药指导按钮
            if (showUseDrugConfig) {
                payModeShowButtonBean.setSupportMedicationGuide(true);
            }
        }

        payModeShowButtonBean.setButtonType(getButtonType(payModeShowButtonBean, recipe, record.getRecordType(), record.getStatusCode(), showUseDrugConfig));

        //date 20200508
        //设置展示配送到家的配送方式
        //判断当前处方对应的机构支持的配送药企包含的配送类型

        //首先判断按钮中配送药品购药方式是否展示，不展示购药方式按钮就不展示药企配送和医院配送
        if (!payModeShowButtonBean.getSupportOnline()) {
            return payModeShowButtonBean;
        }
        List<Integer> payModeSupport = RecipeServiceSub.getDepSupportMode(RecipeBussConstant.PAYMODE_ONLINE);
        payModeSupport.addAll(RecipeServiceSub.getDepSupportMode(RecipeBussConstant.PAYMODE_COD));
        Long enterprisesSend = drugsEnterpriseDAO.getCountByOrganIdAndPayModeSupportAndSendType(recipe.getClinicOrgan(), payModeSupport, EnterpriseSendConstant.Enterprise_Send);
        Long hosSend = drugsEnterpriseDAO.getCountByOrganIdAndPayModeSupportAndSendType(recipe.getClinicOrgan(), payModeSupport, EnterpriseSendConstant.Hos_Send);
        if (null != enterprisesSend && 0 < enterprisesSend) {

            payModeShowButtonBean.setShowSendToEnterprises(true);
        }
        if (null != hosSend && 0 < hosSend) {

            payModeShowButtonBean.setShowSendToHos(true);
        }
        //不支持配送，则按钮都不显示--包括药店取药
        if (RecipeDistributionFlagEnum.HOS_HAVE.getType().equals(recipe.getDistributionFlag())) {
            payModeShowButtonBean.setShowSendToEnterprises(false);
            payModeShowButtonBean.setShowSendToHos(false);
            payModeShowButtonBean.setSupportTFDS(false);
        }
        return payModeShowButtonBean;
    }

    /**
     * @param record                患者处方信息
     * @param payModeShowButtonBean 按钮信息
     * @param recipe                处方信息
     * @return void
     * @method initInternetModel
     * @description 初始化互联网模式的按钮信息
     * @date: 2019/9/3
     * @author: JRK
     */
    public void initInternetModel(PatientTabStatusRecipeDTO record, PayModeShowButtonBean payModeShowButtonBean, Recipe recipe) {


        RecipeExtendDAO RecipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = RecipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if (null != recipeExtend && null != recipeExtend.getGiveModeFormHis()) {
            if ("1".equals(recipeExtend.getGiveModeFormHis())) {
                //只支持配送到家
                payModeShowButtonBean.setSupportToHos(false);
            } else if ("2".equals(recipeExtend.getGiveModeFormHis())) {
                //只支持到院取药
                payModeShowButtonBean.setSupportOnline(false);
            } else if ("3".equals(recipeExtend.getGiveModeFormHis())) {
                //都支持
            } else {
                //都不支持
                payModeShowButtonBean.setSupportOnline(false);
                payModeShowButtonBean.setSupportToHos(false);
            }
        } else {
            //省平台互联网购药方式的配置
            if (RecipeDistributionFlagEnum.DRUGS_HAVE.getType().equals(recipe.getDistributionFlag())) {
                payModeShowButtonBean.setSupportToHos(false);
            }
        }


    }

    /**
     * @param recordType 患者处方的类型
     * @param statusCode 患者处方的状态
     * @return java.lang.Integer 按钮的显示类型
     * @method getButtonType
     * @description 获取按钮显示类型
     * @date: 2019/9/2
     * @author: JRK
     */
    private Integer getButtonType(PayModeShowButtonBean payModeShowButtonBean, Recipe recipe, String recordType, Integer statusCode, Boolean showUseDrugConfig) {
        //添加判断，当选药按钮都不显示的时候，按钮状态为不展示
        if (null != payModeShowButtonBean) {
            //当处方在待处理、前置待审核通过时，购药配送为空不展示按钮
            Boolean noHaveBuyDrugConfig = !payModeShowButtonBean.getSupportOnline() && !payModeShowButtonBean.getSupportTFDS() && !payModeShowButtonBean.getSupportToHos();

            //只有当亲处方有订单，且物流公司和订单号都有时展示物流信息
            Boolean haveSendInfo = false;
            RecipeOrder order = orderDAO.getOrderByRecipeId(recipe.getRecipeId());
            if (null != order && null != order.getLogisticsCompany() && StringUtils.isNotEmpty(order.getTrackingNumber())) {
                haveSendInfo = true;
            }

            RecipePageButtonStatusEnum buttonStatus = RecipePageButtonStatusEnum.
                    fromRecodeTypeAndRecodeCodeAndReviewTypeByConfigure(recordType, statusCode, recipe.getReviewType(), showUseDrugConfig, noHaveBuyDrugConfig, haveSendInfo);
            return buttonStatus.getPageButtonStatus();
        } else {
            LOGGER.error("当前按钮的显示信息不存在");
            return No_Show_Button;
        }

    }

    /**
     * 审核前置弹窗确认点击按钮是否审核通过的
     */
    @RpcService
    public Integer getCheckResult(Integer recipeId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(recipeId);
        if (null == recipe) {
            LOGGER.error("该处方不存在！");
            return 0;
        }

        if (ReviewTypeConstant.Preposition_Check == recipe.getReviewType()) {
            //date 2019/10/10
            //添加一次审核不通过标识位
            if (RecipeStatusConstant.READY_CHECK_YS == recipe.getStatus()) {
                return 0;
            } else if (RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
                if (RecipecCheckStatusConstant.First_Check_No_Pass == recipe.getCheckStatus()) {
                    return 0;
                } else {
                    return 2;
                }
            }
        }
        return 1;
    }

    /**
     * 医生端处方列表根据tab展示
     * doctorId 医生ID
     * start    起始数
     * limit    每页限制数
     * tabStatus     电子处方列表tab状态
     * 0或不传-[ 全部 ]：包含所有状态的处方单
     * 1- [ 未签名 ]：暂存 [ 未签名 ] 的处方单
     * 2- [ 处理中 ]：其余所有状态的处方单
     * 3- [ 审核不通过 ]：[ 审核不通过 ] 的处方单
     * 4- [ 已结束 ]：包括 [ 已取消 ]、[ 已完成 ]、[ 已撤销 ] 的处方单
     *
     * @return
     */
    @RpcService
    public List<Map<String, Object>> findRecipesForDoctorByTapstatusNew(Map<String, Integer> params) {
        if (params.get("doctorId") == null) {
            throw new DAOException("findRecipesForDoctor doctorId不允许为空");
        }
        if (params.get("start") == null) {
            throw new DAOException("findRecipesForDoctor start不允许为空");
        }
        if (params.get("limit") == null) {
            throw new DAOException("findRecipesForDoctor limit不允许为空");
        }

        Integer doctorId = params.get("doctorId");
        Integer start = params.get("start");
        Integer limit = params.get("limit");
        Integer tapStatus = params.get("tapStatus");
        checkUserHasPermissionByDoctorId(doctorId);

        List<Map<String, Object>> list = new ArrayList<>(0);
        List<Recipe> recipeList = recipeDAO.findRecipesByTabstatusForDoctorNew(doctorId, start, limit, tapStatus);
        LOGGER.info("findRecipesForDoctorByTapstatusNew recipeList size={}", recipeList.size());
        if (CollectionUtils.isNotEmpty(recipeList)) {
            List<String> patientIds = new ArrayList<>(0);
            Map<Integer, RecipeBean> recipeMap = Maps.newHashMap();

            //date 20200506
            //获取处方对应的订单信息
            List<String> recipeCodes = recipeList.stream().map(Recipe::getOrderCode).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
            Map<String, Integer> orderStatus = new HashMap<>();
            if (CollectionUtils.isNotEmpty(recipeCodes)) {
                List<RecipeOrder> recipeOrders = orderDAO.findValidListbyCodes(recipeCodes);
                orderStatus = recipeOrders.stream().collect(Collectors.toMap(RecipeOrder::getOrderCode, RecipeOrder::getStatus));
            }

            List<OrganDrugList> organDrugLists;
            Map<String, Integer> configDrugNameMap;
            for (Recipe recipe : recipeList) {
                if (StringUtils.isNotEmpty(recipe.getMpiid())) {
                    patientIds.add(recipe.getMpiid());
                }
                //设置处方具体药品名称---取第一个药展示
                List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
                if (null != recipedetails && recipedetails.size() > 0) {
                    //未签名显示实时
                    if (RecipeStatusEnum.RECIPE_STATUS_UNSIGNED.getType().equals(recipe.getStatus())) {
                        //如果是中药暂存只取药品名显示
                        if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType())) {
                            recipe.setRecipeDrugName(recipedetails.get(0).getDrugName());
                        } else {
                            //剂型获取---暂存重新获取配置药品名由于Recipedetail没有剂型要重新获取一遍
                            organDrugLists = organDrugListDAO.findByOrganIdAndOrganDrugCodeAndDrugIdWithoutStatus(recipe.getClinicOrgan(), recipedetails.get(0).getOrganDrugCode(), recipedetails.get(0).getDrugId());
                            if (CollectionUtils.isNotEmpty(organDrugLists)) {
                                if (StringUtils.isNotEmpty(organDrugLists.get(0).getDrugForm())) {
                                    recipedetails.get(0).setDrugForm(organDrugLists.get(0).getDrugForm());
                                }
                            }
                            //药品名拼接配置
                            configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(recipe.getClinicOrgan(), recipe.getRecipeType()));
                            recipe.setRecipeDrugName(DrugDisplayNameProducer.getDrugName(ObjectCopyUtils.convert(recipedetails.get(0), RecipeDetailBean.class), configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(recipe.getRecipeType())));
                        }
                    } else {
                        recipe.setRecipeDrugName(DrugNameDisplayUtil.dealwithRecipeDrugName(recipedetails.get(0), recipe.getRecipeType(), recipe.getClinicOrgan()));
                    }
                }

                //前台页面展示的时间源不同
                recipe.setRecipeShowTime(recipe.getCreateDate());
                boolean effective = false;
                //只有审核未通过的情况需要看订单状态
                if (RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
                    effective = orderDAO.isEffectiveOrder(recipe.getOrderCode());
                }
                //Map<String, String> tipMap = RecipeServiceSub.getTipsByStatus(recipe.getStatus(), recipe, effective);
                //date 20190929
                //修改医生端状态文案显示
                Map<String, String> tipMap = RecipeServiceSub.getTipsByStatusCopy(recipe.getStatus(), recipe, effective, (orderStatus == null || 0 >= orderStatus.size()) ? null : orderStatus.get(recipe.getOrderCode()));

                recipe.setShowTip(MapValueUtil.getString(tipMap, "listTips"));
                recipeMap.put(recipe.getRecipeId(), convertRecipeForRAP(recipe));
            }

            Map<String, PatientVO> patientMap = Maps.newHashMap();
            if (CollectionUtils.isNotEmpty(patientIds)) {
                List<PatientDTO> patientList = patientService.findByMpiIdIn(patientIds);
                if (CollectionUtils.isNotEmpty(patientList)) {
                    for (PatientDTO patient : patientList) {
                        //设置患者数据
                        RecipeServiceSub.setPatientMoreInfo(patient, doctorId);
                        patientMap.put(patient.getMpiId(), convertSensitivePatientForRAP(patient));
                    }
                }
            }

            for (Recipe recipe : recipeList) {
                String mpiId = recipe.getMpiid();
                HashMap<String, Object> map = Maps.newHashMap();
                map.put("recipe", recipeMap.get(recipe.getRecipeId()));
                map.put("patient", patientMap.get(mpiId));
                list.add(map);
            }
        }

        return list;
    }

    /**
     * 医生端处方列表根据tab展示-----已废弃仅做兼容老app使用
     * doctorId 医生ID
     * recipeId 上一页最后一条处方ID，首页传0
     * limit    每页限制数
     * tabStatus     电子处方列表tab状态
     * 0或不传-[ 全部 ]：包含所有状态的处方单
     * 1- [ 未签名 ]：暂存 [ 未签名 ] 的处方单
     * 2- [ 处理中 ]：其余所有状态的处方单
     * 3- [ 审核不通过 ]：[ 审核不通过 ] 的处方单
     * 4- [ 已结束 ]：包括 [ 已取消 ]、[ 已完成 ]、[ 已撤销 ] 的处方单
     *
     * @return
     */
    @RpcService
    @Deprecated
    public List<Map<String, Object>> findRecipesForDoctorByTapstatus(Map<String, Integer> params) {
        if (params.get("doctorId") == null) {
            throw new DAOException("findRecipesForDoctor doctorId不允许为空");
        }
        if (params.get("recipeId") == null) {
            throw new DAOException("findRecipesForDoctor recipeId不允许为空");
        }
        if (params.get("limit") == null) {
            throw new DAOException("findRecipesForDoctor limit不允许为空");
        }

        Integer doctorId = params.get("doctorId");
        Integer recipeId = params.get("recipeId");
        Integer limit = params.get("limit");
        Integer tapStatus = params.get("tapStatus");
        checkUserHasPermissionByDoctorId(doctorId);
        recipeId = (null == recipeId || Integer.valueOf(0).equals(recipeId)) ? Integer.valueOf(Integer.MAX_VALUE) : recipeId;

        List<Map<String, Object>> list = new ArrayList<>(0);
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);

        List<Recipe> recipeList = recipeDAO.findRecipesByTabstatusForDoctor(doctorId, recipeId, 0, limit, tapStatus);
        LOGGER.info("findRecipesForDoctor recipeList size={}", recipeList.size());
        if (CollectionUtils.isNotEmpty(recipeList)) {
            List<String> patientIds = new ArrayList<>(0);
            Map<Integer, RecipeBean> recipeMap = Maps.newHashMap();

            //date 20200506
            //获取处方对应的订单信息
            List<String> recipeCodes = recipeList.stream().map(recipe -> recipe.getOrderCode()).filter(code -> StringUtils.isNotEmpty(code)).collect(Collectors.toList());
            Map<String, Integer> orderStatus = new HashMap<>();
            if (CollectionUtils.isNotEmpty(recipeCodes)) {

                List<RecipeOrder> recipeOrders = orderDAO.findValidListbyCodes(recipeCodes);
                orderStatus = recipeOrders.stream().collect(Collectors.toMap(RecipeOrder::getOrderCode, RecipeOrder::getStatus));
            }

            for (Recipe recipe : recipeList) {
                if (StringUtils.isNotEmpty(recipe.getMpiid())) {
                    patientIds.add(recipe.getMpiid());
                }
                //设置处方具体药品名称
                List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
                StringBuilder stringBuilder = new StringBuilder();
                if (null != recipedetails && recipedetails.size() > 0) {
                    for (Recipedetail recipedetail : recipedetails) {
                        List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(recipedetail.getDrugId(), recipe.getClinicOrgan());
                        if (organDrugLists != null && 0 < organDrugLists.size()) {
                            stringBuilder.append(organDrugLists.get(0).getSaleName());
                            if (StringUtils.isNotEmpty(organDrugLists.get(0).getDrugForm())) {
                                stringBuilder.append(organDrugLists.get(0).getDrugForm());
                            }
                        } else {
                            stringBuilder.append(recipedetail.getDrugName());
                        }
                        stringBuilder.append(" ").append(recipedetail.getDrugSpec()).append("/").append(recipedetail.getDrugUnit()).append("、");
                    }
                    stringBuilder.deleteCharAt(stringBuilder.lastIndexOf("、"));
                    recipe.setRecipeDrugName(stringBuilder.toString());
                }

                //前台页面展示的时间源不同
                recipe.setRecipeShowTime(recipe.getCreateDate());
                boolean effective = false;
                //只有审核未通过的情况需要看订单状态
                if (RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
                    effective = orderDAO.isEffectiveOrder(recipe.getOrderCode());
                }
                //Map<String, String> tipMap = RecipeServiceSub.getTipsByStatus(recipe.getStatus(), recipe, effective);
                //date 20190929
                //修改医生端状态文案显示
                Map<String, String> tipMap = RecipeServiceSub.getTipsByStatusCopy(recipe.getStatus(), recipe, effective, (orderStatus == null || 0 >= orderStatus.size()) ? null : orderStatus.get(recipe.getOrderCode()));

                recipe.setShowTip(MapValueUtil.getString(tipMap, "listTips"));
                recipeMap.put(recipe.getRecipeId(), convertRecipeForRAP(recipe));
            }

            Map<String, PatientVO> patientMap = Maps.newHashMap();
            if (CollectionUtils.isNotEmpty(patientIds)) {
                List<PatientDTO> patientList = patientService.findByMpiIdIn(patientIds);
                if (CollectionUtils.isNotEmpty(patientList)) {
                    for (PatientDTO patient : patientList) {
                        //设置患者数据
                        RecipeServiceSub.setPatientMoreInfo(patient, doctorId);
                        patientMap.put(patient.getMpiId(), convertSensitivePatientForRAP(patient));
                    }
                }
            }

            for (Recipe recipe : recipeList) {
                String mpiId = recipe.getMpiid();
                HashMap<String, Object> map = Maps.newHashMap();
                map.put("recipe", recipeMap.get(recipe.getRecipeId()));
                map.put("patient", patientMap.get(mpiId));
                list.add(map);
            }
        }

        return list;
    }

}
