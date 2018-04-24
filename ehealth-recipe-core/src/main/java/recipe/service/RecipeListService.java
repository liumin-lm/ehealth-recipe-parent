package recipe.service;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.base.sysparamter.service.ISysParamterService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.RecipeRollingInfoBean;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import recipe.bean.RecipeResultBean;
import recipe.constant.OrderStatusConstant;
import recipe.constant.ParameterConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.dao.bean.PatientRecipeBean;
import recipe.util.ApplicationUtils;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

import java.util.*;

import static recipe.service.RecipeServiceSub.convertPatientForRAP;
import static recipe.service.RecipeServiceSub.convertRecipeForRAP;

/**
 * 处方业务一些列表查询
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/2/13.
 */
@RpcBean("recipeListService")
public class RecipeListService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeListService.class);

    public static final String LIST_TYPE_RECIPE = "1";

    public static final String LIST_TYPE_ORDER = "2";


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
        recipeId = (null == recipeId || Integer.valueOf(0).equals(recipeId)) ? Integer.valueOf(Integer.MAX_VALUE) : recipeId;

        List<Map<String, Object>> list = new ArrayList<>(0);
        IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);

        List<Recipe> recipeList = recipeDAO.findRecipesForDoctor(doctorId, recipeId, 0, limit);
        LOGGER.info("findRecipesForDoctor recipeList size={}", recipeList.size());
        if (CollectionUtils.isNotEmpty(recipeList)) {
            List<String> patientIds = new ArrayList<>(0);
            Map<Integer, Recipe> recipeMap = Maps.newHashMap();
            for (Recipe recipe : recipeList) {
                if (StringUtils.isNotEmpty(recipe.getMpiid())) {
                    patientIds.add(recipe.getMpiid());
                }
                //设置处方具体药品名称
                recipe.setRecipeDrugName(recipeDetailDAO.getDrugNamesByRecipeId(recipe.getRecipeId()));
                //前台页面展示的时间源不同
                recipe.setRecipeShowTime(recipe.getCreateDate());
                boolean effective = false;
                //只有审核未通过的情况需要看订单状态
                if (RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
                    effective = orderDAO.isEffectiveOrder(recipe.getOrderCode(), recipe.getPayMode());
                }
                Map<String, String> tipMap = RecipeServiceSub.getTipsByStatus(recipe.getStatus(), recipe, effective);
                recipe.setShowTip(MapValueUtil.getString(tipMap, "listTips"));
                recipeMap.put(recipe.getRecipeId(), convertRecipeForRAP(recipe));
            }

            Map<String, PatientBean> patientMap = Maps.newHashMap();
            if (CollectionUtils.isNotEmpty(patientIds)) {
                List<PatientBean> patientList = iPatientService.findByMpiIdIn(patientIds);
                if (CollectionUtils.isNotEmpty(patientList)) {
                    for (PatientBean patient : patientList) {
                        //设置患者数据
                        RecipeServiceSub.setPatientMoreInfo(patient, doctorId);
                        patientMap.put(patient.getMpiId(), convertPatientForRAP(patient));
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

        HashMap<String, Object> map = Maps.newHashMap();
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        ISysParamterService iSysParamterService = ApplicationUtils.getBaseService(ISysParamterService.class);

        List<String> allMpiIds = recipeService.getAllMemberPatientsByCurrentPatient(mpiId);
        List<Integer> recipeIds = recipeDAO.findPendingRecipes(allMpiIds, RecipeStatusConstant.CHECK_PASS, 0, 1);
        String title;
        String recipeGetModeTip = "";
        if (CollectionUtils.isNotEmpty(recipeIds)) {
            title = "赶快结算您的处方单吧！";
            List<Map> recipesMap = new ArrayList<>(0);
            for (Integer recipeId : recipeIds) {
                Map<String, Object> recipeInfo = recipeService.getPatientRecipeById(recipeId);
                recipeGetModeTip = MapValueUtil.getString(recipeInfo, "recipeGetModeTip");
                recipesMap.add(recipeInfo);
            }

            map.put("recipes", recipesMap);
        } else {
            title = "暂无待处理处方单";
        }

        List<PatientRecipeBean> otherRecipes = this.findOtherRecipesForPatient(mpiId, 0, 1);
        if (CollectionUtils.isNotEmpty(otherRecipes)) {
            map.put("haveFinished", true);
        } else {
            map.put("haveFinished", false);
        }

        map.put("title", title);
        map.put("unSendTitle", iSysParamterService.getParam(ParameterConstant.KEY_RECIPE_UNSEND_TIP, null));
        map.put("recipeGetModeTip", recipeGetModeTip);

        return map;
    }

    @RpcService
    public List<PatientRecipeBean> findOtherRecipesForPatient(String mpiId, Integer index, Integer limit) {
        Assert.hasLength(mpiId, "findOtherRecipesForPatient mpiId is null.");
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        List<String> allMpiIds = recipeService.getAllMemberPatientsByCurrentPatient(mpiId);
        //获取待处理那边最新的一单
        List<Integer> recipeIds = recipeDAO.findPendingRecipes(allMpiIds, RecipeStatusConstant.CHECK_PASS, 0, 1);
        List<PatientRecipeBean> backList = recipeDAO.findOtherRecipesForPatient(allMpiIds, recipeIds, index, limit);
        processListDate(backList, allMpiIds);
        return backList;
    }

    /**
     * 获取所有处方单信息
     *
     * @param mpiId
     * @param index
     * @param limit
     * @return
     */
    @RpcService
    public List<PatientRecipeBean> findAllRecipesForPatient(String mpiId, Integer index, Integer limit) {
        Assert.hasLength(mpiId, "findAllRecipesForPatient mpiId is null.");
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

//        List<String> allMpiIds = recipeService.getAllMemberPatientsByCurrentPatient(mpiId);
        List<String> allMpiIds = Arrays.asList(mpiId);
        //获取待处理那边最新的一单
//        List<Integer> recipeIds = recipeDAO.findPendingRecipes(allMpiIds, RecipeStatusConstant.CHECK_PASS,0,1);
        List<PatientRecipeBean> backList = recipeDAO.findOtherRecipesForPatient(allMpiIds, null, index, limit);
        processListDate(backList, allMpiIds);
        return backList;
    }

    /**
     * 处理列表数据
     *
     * @param backList
     * @param allMpiIds
     */
    private void processListDate(List<PatientRecipeBean> backList, List<String> allMpiIds) {
        IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);

        if (CollectionUtils.isNotEmpty(backList)) {
            //处理订单类型数据
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            List<PatientBean> patientList = iPatientService.findByMpiIdIn(allMpiIds);
            Map<String, PatientBean> patientMap = Maps.newHashMap();
            if (null != patientList && !patientList.isEmpty()) {
                for (PatientBean p : patientList) {
                    if (StringUtils.isNotEmpty(p.getMpiId())) {
                        patientMap.put(p.getMpiId(), p);
                    }
                }
            }

            PatientBean p;
            for (PatientRecipeBean record : backList) {
                p = patientMap.get(record.getMpiId());
                if (null != p) {
                    record.setPatientName(p.getPatientName());
                    record.setPhoto(p.getPhoto());
                    record.setPatientSex(p.getPatientSex());
                }
                if (LIST_TYPE_RECIPE.equals(record.getRecordType())) {
                    record.setStatusText(getRecipeStatusText(record.getStatusCode()));
                    //设置失效时间
                    if (RecipeStatusConstant.CHECK_PASS == record.getStatusCode()) {
                        record.setRecipeSurplusHours(RecipeServiceSub.getRecipeSurplusHours(record.getSignDate()));
                    }
                    //药品详情
                    record.setRecipeDetail(detailDAO.findByRecipeId(record.getRecordId()));
                } else if (LIST_TYPE_ORDER.equals(record.getRecordType())) {
                    RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                    record.setStatusText(getOrderStatusText(record.getStatusCode()));
                    RecipeResultBean resultBean = orderService.getOrderDetailById(record.getRecordId());
                    if (RecipeResultBean.SUCCESS.equals(resultBean.getCode())) {
                        if (null != resultBean.getObject() && resultBean.getObject() instanceof RecipeOrder) {
                            RecipeOrder order = (RecipeOrder) resultBean.getObject();
                            List<PatientRecipeBean> recipeList = (List<PatientRecipeBean>) order.getList();
                            if (CollectionUtils.isNotEmpty(recipeList)) {
                                // 前端要求，先去掉数组形式，否则前端不好处理
//                                List<PatientRecipeBean> subList = new ArrayList<>(5);
//                                PatientRecipeBean _bean;
                                for (PatientRecipeBean recipe : recipeList) {
//                                    _bean = new PatientRecipeBean();
//                                    _bean.setRecordType(LIST_TYPE_RECIPE);
                                    // 当前订单只有一个处方，处方内的患者信息使用订单的信息就可以
//                                    _bean.setPatientName(record.getPatientName());
//                                    _bean.setPhoto(record.getPhoto());
//                                    _bean.setPatientSex(record.getPatientSex());

                                    record.setRecipeId(recipe.getRecipeId());
                                    record.setRecipeType(recipe.getRecipeType());
                                    record.setOrganDiseaseName(recipe.getOrganDiseaseName());
                                    // 订单支付方式
                                    record.setPayMode(recipe.getPayMode());
                                    //药品详情
                                    record.setRecipeDetail(recipe.getRecipeDetail());
//                                    _bean.setSignDate(recipe.getSignDate());
                                    if (RecipeStatusConstant.CHECK_PASS == recipe.getStatusCode()
                                            && OrderStatusConstant.READY_PAY.equals(record.getStatusCode())) {
                                        record.setRecipeSurplusHours(recipe.getRecipeSurplusHours());
                                    }
//                                    subList.add(_bean);
                                }

//                                record.setRecipeList(subList);
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * 获取最新开具的处方单前limit条，用于跑马灯显示
     *
     * @param limit
     * @return
     */
    public List<RecipeRollingInfoBean> findLastesRecipeList(List<Integer> organIds, int start, int limit) {
        IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);
        IDoctorService iDoctorService = ApplicationUtils.getBaseService(IDoctorService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        List<Integer> testDocIds = iDoctorService.findTestDoctors(organIds);
        String endDt = DateTime.now().toString(DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateTime.now().minusMonths(3).toString(DateConversion.DEFAULT_DATE_TIME);
        List<RecipeRollingInfoBean> list = recipeDAO.findLastesRecipeList(startDt, endDt, organIds, testDocIds, start, limit);

        // 个性化微信号医院没有开方医生不展示
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        List<String> mpiIdList = new ArrayList<>();
        for (RecipeRollingInfoBean info : list) {
            mpiIdList.add(info.getMpiId());
        }

        List<PatientBean> patientList = iPatientService.findByMpiIdIn(mpiIdList);
        Map<String, PatientBean> patientMap = Maps.uniqueIndex(patientList, new Function<PatientBean, String>() {
            @Override
            public String apply(PatientBean input) {
                return input.getMpiId();
            }
        });

        PatientBean patient;
        for (RecipeRollingInfoBean info : list) {
            patient = patientMap.get(info.getMpiId());
            if (null != patient) {
                info.setPatientName(patient.getPatientName());
            }
        }

        return list;
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
        IDoctorService iDoctorService = ApplicationUtils.getBaseService(IDoctorService.class);

        List<Integer> testDocIds = iDoctorService.findTestDoctors(organIds);
        String endDt = DateTime.now().toString(DateConversion.DEFAULT_DATE_TIME);
        String startDt = DateTime.now().minusMonths(3).toString(DateConversion.DEFAULT_DATE_TIME);
        return recipeDAO.findDoctorIdSortByCount(startDt, endDt, organIds, testDocIds, start, limit);
    }

    private String getOrderStatusText(Integer status) {
        String msg = "未知";
        if (OrderStatusConstant.FINISH.equals(status)) {
            msg = "已完成";
        } else if (OrderStatusConstant.READY_PAY.equals(status)) {
            msg = "待支付";
        } else if (OrderStatusConstant.READY_GET_DRUG.equals(status)) {
            msg = "待取药";
        } else if (OrderStatusConstant.READY_CHECK.equals(status)) {
            msg = "待审核";
        } else if (OrderStatusConstant.READY_SEND.equals(status)) {
            msg = "待配送";
        } else if (OrderStatusConstant.SENDING.equals(status)) {
            msg = "配送中";
        } else if (OrderStatusConstant.CANCEL_NOT_PASS.equals(status)) {
            msg = "已取消，审核未通过";
        } else if (OrderStatusConstant.CANCEL_AUTO.equals(status)
                || OrderStatusConstant.CANCEL_MANUAL.equals(status)) {
            msg = "已取消";
        }

        return msg;
    }

    private String getRecipeStatusText(int status) {
        String msg;
        switch (status) {
            case RecipeStatusConstant.FINISH:
                msg = "已完成";
                break;
            case RecipeStatusConstant.HAVE_PAY:
                msg = "已支付，待取药";
                break;
            case RecipeStatusConstant.CHECK_PASS:
                msg = "待处理";
                break;
            case RecipeStatusConstant.NO_PAY:
            case RecipeStatusConstant.NO_OPERATOR:
            case RecipeStatusConstant.REVOKE:
            case RecipeStatusConstant.NO_DRUG:
            case RecipeStatusConstant.CHECK_NOT_PASS_YS:
            case RecipeStatusConstant.DELETE:
            case RecipeStatusConstant.HIS_FAIL:
                msg = "已取消";
                break;
            case RecipeStatusConstant.IN_SEND:
                msg = "配送中";
                break;
            case RecipeStatusConstant.WAIT_SEND:
            case RecipeStatusConstant.READY_CHECK_YS:
            case RecipeStatusConstant.CHECK_PASS_YS:
                msg = "待配送";
                break;
            default:
                msg = "未知状态";
        }

        return msg;
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
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        IPatientService patientService = ApplicationUtils.getBaseService(IPatientService.class);

        List<Map<String, Object>> list = new ArrayList<>();
        List<Recipe> recipes = recipeDAO.findRecipeListByDoctorAndPatient(doctorId, mpiId, start, limit);
        PatientBean patient = RecipeServiceSub.convertPatientForRAP(patientService.get(mpiId));
        if (CollectionUtils.isNotEmpty(recipes)) {
            for (Recipe recipe : recipes) {
                Map<String, Object> map = Maps.newHashMap();
                recipe.setRecipeDrugName(recipeDetailDAO.getDrugNamesByRecipeId(recipe.getRecipeId()));
                recipe.setRecipeShowTime(recipe.getCreateDate());
                boolean effective = false;
                //只有审核未通过的情况需要看订单状态
                if (RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
                    effective = orderDAO.isEffectiveOrder(recipe.getOrderCode(), recipe.getPayMode());
                }
                Map<String, String> tipMap = RecipeServiceSub.getTipsByStatus(recipe.getStatus(), recipe, effective);
                recipe.setShowTip(MapValueUtil.getString(tipMap, "listTips"));
                map.put("recipe", RecipeServiceSub.convertRecipeForRAP(recipe));
                map.put("patient", patient);
                list.add(map);
            }

        }
        return list;
    }

}
