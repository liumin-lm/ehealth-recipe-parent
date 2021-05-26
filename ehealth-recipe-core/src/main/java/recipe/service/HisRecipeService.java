package recipe.service;

import com.google.common.collect.Lists;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.*;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.*;
import com.ngari.patient.service.*;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.vo.FindHisRecipeListVO;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.base.constant.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.constant.OrderStatusConstant;
import recipe.constant.PayConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.dao.bean.HisRecipeListBean;
import recipe.factory.status.constant.OfflineToOnlineEnum;
import recipe.factory.status.constant.RecipeOrderStatusEnum;
import recipe.factory.status.constant.RecipeStatusEnum;
import recipe.givemode.business.GiveModeFactory;
import recipe.givemode.business.IGiveModeBase;
import recipe.service.manager.EmrRecipeManager;
import recipe.service.manager.MergeRecipeManager;
import recipe.util.MapValueUtil;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author yinsheng
 * @date 2020\3\10 0010 19:58
 */
@RpcBean(value = "hisRecipeService", mvc_authentication = false)
public class HisRecipeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HisRecipeService.class);

    @Autowired
    private HisRecipeDAO hisRecipeDAO;
    @Autowired
    private HisRecipeExtDAO hisRecipeExtDAO;
    @Autowired
    private HisRecipeDetailDAO hisRecipeDetailDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;
    @Autowired
    private EmrRecipeManager emrRecipeManager;
    @Resource
    private DrugsEnterpriseService drugsEnterpriseService;
    @Resource
    private RecipeService recipeService;
    @Autowired
    private IConfigurationCenterUtilsService configService;
    @Autowired
    private MergeRecipeManager mergeRecipeManager;

    private static final ThreadLocal<String> recipeCodeThreadLocal = new ThreadLocal<>();

    /**
     * organId 机构编码
     * mpiId 用户mpiId
     * timeQuantum 时间段  1 代表一个月  3 代表三个月 6 代表6个月
     * status 1 未处理 2 已处理
     *
     * @param findHisRecipeListVO 入参
     * @return 前端展示
     */
    @RpcService
    public List<HisPatientTabStatusMergeRecipeVO> findHisRecipe(FindHisRecipeListVO findHisRecipeListVO) {
        LOGGER.info("hisRecipeService findHisRecipe request:{}", JSONUtils.toString(findHisRecipeListVO));
        if (null == findHisRecipeListVO
                || findHisRecipeListVO.getOrganId() == null
                || StringUtils.isEmpty(findHisRecipeListVO.getMpiId())
                || StringUtils.isEmpty(findHisRecipeListVO.getStatus())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        try {
            List<HisPatientTabStatusMergeRecipeVO> result = findHisRecipeEnter(findHisRecipeListVO);
            LOGGER.info("hisRecipeService findHisRecipe result:{}", JSONUtils.toString(result));
            return result;
        } catch (DAOException e1) {
            LOGGER.error("hisRecipeService findHisRecipe error", e1);
            throw new DAOException(e1.getCode(), e1.getMessage());
        } catch (Exception e) {
            LOGGER.error("hisRecipeService findHisRecipe error", e);
            throw new DAOException(recipe.constant.ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 线下处方列表获取入口,根据前端传的状态onready（待处理）ongoing（进行中）isover（已完成）返回具体列表
     *
     * @param findHisRecipeListVO 请求入参
     * @return 线下处方列表
     */
    private List<HisPatientTabStatusMergeRecipeVO> findHisRecipeEnter(FindHisRecipeListVO findHisRecipeListVO) {
        LOGGER.info("hisRecipeService findHisRecipe findHisRecipeListVO:{}", JSONUtils.toString(findHisRecipeListVO));
        String status = findHisRecipeListVO.getStatus();
        String mpiId = findHisRecipeListVO.getMpiId();
        String carId = findHisRecipeListVO.getCardId();
        Integer organId = findHisRecipeListVO.getOrganId();
        Integer timeQuantum = findHisRecipeListVO.getTimeQuantum();
        Integer start = findHisRecipeListVO.getStart();
        Integer limit = findHisRecipeListVO.getLimit();

        PatientService patientService = BasicAPI.getService(PatientService.class);
        PatientDTO patientDTO = patientService.getPatientBeanByMpiId(mpiId);
        patientDTO.setCardId(StringUtils.isNotEmpty(carId) ? carId : "");

        IGiveModeBase giveModeBase = GiveModeFactory.getGiveModeBaseByRecipe(new Recipe());
        //获取机构配制的购药按钮
        GiveModeShowButtonVO giveModeShowButtons = giveModeBase.getGiveModeSettingFromYypt(organId);
        GiveModeButtonBean giveModeButtonBean = giveModeShowButtons.getListItem();

        //表示获取待缴费或者已处理的处方,此时需要查询HIS
        HisResponseTO<List<QueryHisRecipResTO>> hisResponseTO = queryData(organId, patientDTO, timeQuantum, OfflineToOnlineEnum.getOfflineToOnlineType(status), null);
        if (null == hisResponseTO) {
            return new ArrayList<>();
        }
        if ("ongoing".equals(status)) {
            //表示为进行中的处方
            return findOngoingHisRecipe(hisResponseTO.getData(), patientDTO, giveModeButtonBean, start, limit);
        } else {
            if ("onready".equals(findHisRecipeListVO.getStatus())) {
                List<HisRecipeVO> noPayFeeHisRecipeVO = covertToHisRecipeObject(hisResponseTO, patientDTO, OfflineToOnlineEnum.getOfflineToOnlineType(status));
                return findOnReadyHisRecipe(noPayFeeHisRecipeVO, giveModeButtonBean);
            } else {
                checkHisRecipeAndSave(status, patientDTO, hisResponseTO);
                return findFinishHisRecipes(mpiId, giveModeButtonBean, start, limit);
            }
        }
    }

    /**
     * @param data 当前获取HIS的处方单集合
     * @return 前端需要展示的进行中的处方单集合, 先获取进行中的处方返回给前端展示, 然后对处方数据进行校验, 处方发生
     * 变更需要删除处方,当患者点击处方列表时如果订单已删除,会弹框提示"该处方单信息已变更，请退出重新获取处方信息"
     */
    private List<HisPatientTabStatusMergeRecipeVO> findOngoingHisRecipe(List<QueryHisRecipResTO> data, PatientDTO patientDTO, GiveModeButtonBean giveModeButtonBean, Integer start, Integer limit) {
        LOGGER.info("hisRecipeService findOngoingHisRecipe request:{}", JSONUtils.toString(data));
        List<HisPatientTabStatusMergeRecipeVO> result = Lists.newArrayList();
        // 获取当前用户下所有患者
        List<String> allMpiIds = recipeService.getAllMemberPatientsByCurrentPatient(patientDTO.getMpiId());
        if (CollectionUtils.isEmpty(allMpiIds)) {
            allMpiIds.add(patientDTO.getMpiId());
        }
        //先查询进行中处方(目前仅指的是待支付的处方单)
        //查询所有进行中的线下处方
        List<HisRecipeListBean> hisRecipeListByMPIIds = hisRecipeDAO.findOngoingHisRecipeListByMPIIds(allMpiIds, start, limit);
        if (CollectionUtils.isEmpty(hisRecipeListByMPIIds)) {
            return result;
        }
        Map<String, List<HisRecipeListBean>> orderCodeMap = hisRecipeListByMPIIds.stream().filter(hisRecipeListBean -> hisRecipeListBean.getOrderCode() != null).collect(Collectors.groupingBy(HisRecipeListBean::getOrderCode));
        Map<Integer, List<Recipe>> recipeMap = getRecipeMap(hisRecipeListByMPIIds);
        Map<String, List<RecipeOrder>> recipeOrderMap = getRecipeOrderMap(orderCodeMap.keySet());

        Set<Integer> recipeIds = new HashSet<>();
        hisRecipeListByMPIIds.forEach(hisRecipeListBean -> {
            if (!recipeIds.contains(hisRecipeListBean.getHisRecipeID())) {
                String orderCode = hisRecipeListBean.getOrderCode();
                HisPatientTabStatusMergeRecipeVO hisPatientTabStatusMergeRecipeVO = new HisPatientTabStatusMergeRecipeVO();
                hisPatientTabStatusMergeRecipeVO.setFirstRecipeId(hisRecipeListBean.getHisRecipeID());
                hisPatientTabStatusMergeRecipeVO.setListSkipType(giveModeButtonBean.getButtonSkipType());
                // 获取合并处方的关键字
                String mergeRecipeWay = (String) mergeRecipeManager.getMergeRecipeSetting().get("mergeRecipeWayAfter");
                Boolean mergeRecipeFlag = (Boolean) mergeRecipeManager.getMergeRecipeSetting().get("mergeRecipeFlag");
                hisPatientTabStatusMergeRecipeVO.setMergeRecipeFlag(mergeRecipeFlag);
                hisPatientTabStatusMergeRecipeVO.setMergeRecipeWay(mergeRecipeWay);
                if ("e.registerId".equals(mergeRecipeWay)) {
                    // 挂号序号
                    hisPatientTabStatusMergeRecipeVO.setGroupField(hisRecipeListBean.getRegisteredId());
                } else {
                    // 慢病名称
                    hisPatientTabStatusMergeRecipeVO.setGroupField(hisRecipeListBean.getChronicDiseaseName());
                }
                List<HisRecipeListBean> hisRecipeListBeans = orderCodeMap.get(orderCode);
                List<HisRecipeVO> list = new ArrayList<>();
                RecipeOrder recipeOrder = recipeOrderMap.get(orderCode).get(0);
                setPatientTabStatusMerge(recipeMap, recipeIds, recipeOrder, hisRecipeListBeans, list);
                hisPatientTabStatusMergeRecipeVO.setRecipe(list);
                result.add(hisPatientTabStatusMergeRecipeVO);
            }
        });
        try {
            //更新数据校验
            hisRecipeInfoCheck(data, patientDTO);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo hisRecipeInfoCheck error ", e);
        }
        LOGGER.info("hisRecipeService findOngoingHisRecipe result:{}", JSONUtils.toString(result));
        return result;
    }

    private void setPatientTabStatusMerge(Map<Integer, List<Recipe>> collect, Set<Integer> recipeIds, RecipeOrder recipeOrder, List<HisRecipeListBean> hisRecipeListBeans, List<HisRecipeVO> list) {
        hisRecipeListBeans.forEach(hisRecipeListBean1 -> {
            HisRecipeVO hisRecipeVO = ObjectCopyUtils.convert(hisRecipeListBean1, HisRecipeVO.class);
            // 这个接口查询的所有处方都是线下处方 前端展示逻辑 0: 平台, 1: his
            hisRecipeVO.setFromFlag(1);
            // 有订单跳转订单
            hisRecipeVO.setJumpPageType(1);
            Recipe recipe = collect.get(hisRecipeListBean1.getRecipeId()).get(0);
            hisRecipeVO.setStatusText(getTipsByStatusForPatient(recipe, recipeOrder));
            list.add(hisRecipeVO);
            recipeIds.add(hisRecipeListBean1.getHisRecipeID());
        });
    }

    /**
     * 患者获取已完成处方列表,我们先进行数据校验,需要校验是否被其他人绑定了,是否线下的诊断药品等变化了
     *
     * @param status        待处理  进行中  已完成
     * @param patientDTO    患者信息
     * @param hisResponseTO 当前获取HIS的处方单集合
     */
    private void checkHisRecipeAndSave(String status, PatientDTO patientDTO, HisResponseTO<List<QueryHisRecipResTO>> hisResponseTO) {
        try {
            //更新数据校验
            hisRecipeInfoCheck(hisResponseTO.getData(), patientDTO);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo hisRecipeInfoCheck error ", e);
        }
        try {
            //数据入库
            saveHisRecipeInfo(hisResponseTO, patientDTO, OfflineToOnlineEnum.getOfflineToOnlineType(status));
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo saveHisRecipeInfo error ", e);
        }
    }

    /**
     * 获取待处理的线下的处方单
     *
     * @param request his的处方单集合
     * @return 前端需要的处方单集合
     */
    private List<HisPatientTabStatusMergeRecipeVO> findOnReadyHisRecipe(List<HisRecipeVO> request, GiveModeButtonBean giveModeButtonBean) {
        LOGGER.info("hisRecipeService findOnReadyHisRecipe request:{}", JSONUtils.toString(request));

        //查询线下待缴费处方
        List<HisPatientTabStatusMergeRecipeVO> result = new ArrayList<>();
        Map<String, Object> mergeSettings = mergeRecipeManager.getMergeRecipeSetting();
        Boolean mergeRecipeFlag = (Boolean) mergeSettings.get("mergeRecipeFlag");
        String mergeRecipeWayAfter = MapValueUtil.getString(mergeSettings, "mergeRecipeWayAfter");
        //移除正在进行中的处方单
        Iterator<HisRecipeVO> iterator = request.iterator();
        while (iterator.hasNext()) {
            HisRecipeVO hisRecipeVO = iterator.next();
            Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(hisRecipeVO.getRecipeCode(), hisRecipeVO.getClinicOrgan());
            if (null != recipe && StringUtils.isNotEmpty(recipe.getOrderCode())) {
                iterator.remove();
            }
        }
        if (mergeRecipeFlag) {
            //获取合并的处方
            if ("e.registerId".equals(mergeRecipeWayAfter)) {
                //表示根据挂号序号分组
                Map<String, List<HisRecipeVO>> registerIdRelation = request.stream().collect(Collectors.groupingBy(HisRecipeVO::getRegisteredId));
                for (Map.Entry<String, List<HisRecipeVO>> entry : registerIdRelation.entrySet()) {
                    List<HisRecipeVO> recipes = entry.getValue();
                    if (StringUtils.isEmpty(entry.getKey())) {
                        //表示挂号序号为空,不能进行处方合并
                        setMergeRecipeVO(recipes, mergeRecipeWayAfter, mergeRecipeFlag, result, giveModeButtonBean);
                    } else {
                        //可以进行合并支付
                        HisPatientTabStatusMergeRecipeVO tabStatusMergeRecipeVO = new HisPatientTabStatusMergeRecipeVO();
                        tabStatusMergeRecipeVO.setGroupField(recipes.get(0).getRegisteredId());
                        tabStatusMergeRecipeVO.setMergeRecipeFlag(true);
                        tabStatusMergeRecipeVO.setMergeRecipeWay(mergeRecipeWayAfter);
                        tabStatusMergeRecipeVO.setRecipe(recipes);
                        tabStatusMergeRecipeVO.setFirstRecipeId(recipes.get(0).getHisRecipeID());
                        tabStatusMergeRecipeVO.setListSkipType(giveModeButtonBean.getButtonSkipType());
                        result.add(tabStatusMergeRecipeVO);
                    }
                }
            } else {
                //表示根据相同挂号序号下的同一病种分组
                Map<String, Map<String, List<HisRecipeVO>>> map = request.stream().collect(Collectors.groupingBy(HisRecipeVO::getRegisteredId, Collectors.groupingBy(HisRecipeVO::getChronicDiseaseName)));
                for (Map.Entry<String, Map<String, List<HisRecipeVO>>> entry : map.entrySet()) {
                    //挂号序号为空表示不能进行处方合并
                    if (StringUtils.isEmpty(entry.getKey())) {
                        Map<String, List<HisRecipeVO>> recipeMap = entry.getValue();
                        for (Map.Entry<String, List<HisRecipeVO>> recipeEntry : recipeMap.entrySet()) {
                            List<HisRecipeVO> recipes = recipeEntry.getValue();
                            setMergeRecipeVO(recipes, mergeRecipeWayAfter, mergeRecipeFlag, result, giveModeButtonBean);
                        }
                    } else {
                        //表示挂号序号不为空,需要根据当前病种
                        Map<String, List<HisRecipeVO>> recipeMap = entry.getValue();
                        for (Map.Entry<String, List<HisRecipeVO>> recipeEntry : recipeMap.entrySet()) {
                            //如果病种为空不能进行合并
                            List<HisRecipeVO> recipes = recipeEntry.getValue();
                            if (StringUtils.isEmpty(recipeEntry.getKey())) {
                                setMergeRecipeVO(recipes, mergeRecipeWayAfter, mergeRecipeFlag, result, giveModeButtonBean);
                            } else {
                                //可以进行合并支付
                                HisPatientTabStatusMergeRecipeVO tabStatusMergeRecipeVO = new HisPatientTabStatusMergeRecipeVO();
                                tabStatusMergeRecipeVO.setGroupField(recipes.get(0).getChronicDiseaseName());
                                tabStatusMergeRecipeVO.setMergeRecipeFlag(true);
                                tabStatusMergeRecipeVO.setMergeRecipeWay(mergeRecipeWayAfter);
                                tabStatusMergeRecipeVO.setRecipe(recipes);
                                tabStatusMergeRecipeVO.setFirstRecipeId(recipes.get(0).getHisRecipeID());
                                result.add(tabStatusMergeRecipeVO);
                            }
                        }
                    }
                }
            }
        } else {
            //不开启合并支付开关
            setMergeRecipeVO(request, mergeRecipeWayAfter, false, result, giveModeButtonBean);
        }
        //按处方id从大到小排列
        result.sort((o1, o2) -> o2.getFirstRecipeId() - o1.getFirstRecipeId());
        LOGGER.info("hisRecipeService findOnReadyHisRecipe result:{}", JSONUtils.toString(result));
        return result;
    }

    private void setMergeRecipeVO(List<HisRecipeVO> recipes, String mergeRecipeWayAfter, Boolean mergeRecipeFlag, List<HisPatientTabStatusMergeRecipeVO> result, GiveModeButtonBean giveModeButtonBean) {
        for (HisRecipeVO hisRecipeVO : recipes) {
            Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(hisRecipeVO.getRecipeCode(), hisRecipeVO.getClinicOrgan());
            if (recipe != null && StringUtils.isNotEmpty(recipe.getOrderCode())) {
                continue;
            }
            HisPatientTabStatusMergeRecipeVO tabStatusMergeRecipeVO = new HisPatientTabStatusMergeRecipeVO();
            tabStatusMergeRecipeVO.setGroupField(hisRecipeVO.getChronicDiseaseName());
            tabStatusMergeRecipeVO.setMergeRecipeFlag(mergeRecipeFlag);
            tabStatusMergeRecipeVO.setMergeRecipeWay(mergeRecipeWayAfter);
            tabStatusMergeRecipeVO.setRecipe(Arrays.asList(hisRecipeVO));
            tabStatusMergeRecipeVO.setFirstRecipeId(hisRecipeVO.getHisRecipeID());
            tabStatusMergeRecipeVO.setListSkipType(giveModeButtonBean.getButtonSkipType());
            result.add(tabStatusMergeRecipeVO);
        }
    }

    /**
     * 查询当前账号下所有线下已处理处方列表
     *
     * @param mpiId
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public List<HisPatientTabStatusMergeRecipeVO> findFinishHisRecipes(String mpiId, GiveModeButtonBean giveModeButtonBean, Integer start, Integer limit) {
        LOGGER.info("findFinishHisRecipes mpiId:{} index:{} limit:{} ", mpiId, start, limit);
        Assert.hasLength(mpiId, "findFinishHisRecipes mpiId为空!");
        List<HisPatientTabStatusMergeRecipeVO> result = new ArrayList<>();
        // 获取当前用户下所有患者
        List<String> allMpiIds = recipeService.getAllMemberPatientsByCurrentPatient(mpiId);
        if (CollectionUtils.isEmpty(allMpiIds)) {
            allMpiIds.add(mpiId);
        }
        // 所有所有已处理的线下处方
        List<HisRecipeListBean> hisRecipeListByMPIIds = hisRecipeDAO.findHisRecipeListByMPIIds(allMpiIds, start, limit);
        if (CollectionUtils.isEmpty(hisRecipeListByMPIIds)) {
            return result;
        }
        Map<String, List<HisRecipeListBean>> orderCodeMap = hisRecipeListByMPIIds.stream().filter(hisRecipeListBean -> hisRecipeListBean.getOrderCode() != null).collect(Collectors.groupingBy(HisRecipeListBean::getOrderCode));
        Map<Integer, List<Recipe>> recipeMap = getRecipeMap(hisRecipeListByMPIIds);
        Map<String, List<RecipeOrder>> recipeOrderMap = getRecipeOrderMap(orderCodeMap.keySet());
        Set<Integer> recipeIds = new HashSet<>();
        hisRecipeListByMPIIds.forEach(hisRecipeListBean -> {
            if (!recipeIds.contains(hisRecipeListBean.getHisRecipeID())) {
                String orderCode = hisRecipeListBean.getOrderCode();
                HisPatientTabStatusMergeRecipeVO hisPatientTabStatusMergeRecipeVO = new HisPatientTabStatusMergeRecipeVO();
                hisPatientTabStatusMergeRecipeVO.setMergeRecipeFlag(true);
                hisPatientTabStatusMergeRecipeVO.setFirstRecipeId(hisRecipeListBean.getHisRecipeID());
                hisPatientTabStatusMergeRecipeVO.setListSkipType(giveModeButtonBean.getButtonSkipType());
                // 获取合并处方的关键字
                String mergeRecipeWay = (String) mergeRecipeManager.getMergeRecipeSetting().get("mergeRecipeWayAfter");
                Boolean mergeRecipeFlag = (Boolean) mergeRecipeManager.getMergeRecipeSetting().get("mergeRecipeFlag");
                hisPatientTabStatusMergeRecipeVO.setMergeRecipeWay(mergeRecipeWay);
                hisPatientTabStatusMergeRecipeVO.setMergeRecipeFlag(mergeRecipeFlag);
                if ("e.registerId".equals(mergeRecipeWay)) {
                    // 挂号序号
                    hisPatientTabStatusMergeRecipeVO.setGroupField(hisRecipeListBean.getRegisteredId());
                } else {
                    // 慢病名称
                    hisPatientTabStatusMergeRecipeVO.setGroupField(hisRecipeListBean.getChronicDiseaseName());
                }

                if (Objects.isNull(orderCode)) {
                    List<HisRecipeVO> list = new ArrayList<>();
                    HisRecipeVO hisRecipeVO = ObjectCopyUtils.convert(hisRecipeListBean, HisRecipeVO.class);
                    // 这个接口查询的所有处方都是线下处方 前端展示逻辑 0: 平台, 1: his
                    hisRecipeVO.setFromFlag(1);
                    hisRecipeVO.setJumpPageType(0);

                    hisRecipeVO.setStatusText(getRecipeStatusTabText(hisRecipeListBean.getStatus()));
                    list.add(hisRecipeVO);
                    recipeIds.add(hisRecipeListBean.getHisRecipeID());
                    hisPatientTabStatusMergeRecipeVO.setRecipe(list);
                    result.add(hisPatientTabStatusMergeRecipeVO);
                } else {
                    List<HisRecipeListBean> hisRecipeListBeans = orderCodeMap.get(orderCode);
                    List<HisRecipeVO> list1 = new ArrayList<>();
                    List<RecipeOrder> recipeOrders = recipeOrderMap.get(orderCode);
                    RecipeOrder recipeOrder = recipeOrders.get(0);
                    setPatientTabStatusMerge(recipeMap, recipeIds, recipeOrder, hisRecipeListBeans, list1);
                    hisPatientTabStatusMergeRecipeVO.setRecipe(list1);
                    result.add(hisPatientTabStatusMergeRecipeVO);
                }
            }

        });
        LOGGER.info("findFinishHisRecipes result:{} ", result);
        return result;
    }

    private Map<Integer, List<Recipe>> getRecipeMap(List<HisRecipeListBean> hisRecipeListByMPIIds){
        Set<Integer> recipes = hisRecipeListByMPIIds.stream().filter(hisRecipeListBean -> hisRecipeListBean.getRecipeId() != null).collect(Collectors.groupingBy(HisRecipeListBean::getRecipeId)).keySet();
        List<Recipe> byRecipes = recipeDAO.findByRecipeIds(recipes);
        Map<Integer, List<Recipe>> collect = null;
        if(CollectionUtils.isNotEmpty(byRecipes)) {
            collect = byRecipes.stream().collect(Collectors.groupingBy(Recipe::getRecipeId));
        }
        return collect;
    }

    private Map<String, List<RecipeOrder>> getRecipeOrderMap(Set<String> orderCodes){
        Map<String, List<RecipeOrder>> collect1 = null;
        if (CollectionUtils.isNotEmpty(orderCodes)) {
            List<RecipeOrder> byOrderCode = recipeOrderDAO.findByOrderCode(orderCodes);
            if(CollectionUtils.isNotEmpty(byOrderCode)) {
                collect1 = byOrderCode.stream().collect(Collectors.groupingBy(RecipeOrder::getOrderCode));
            }
        }
        return collect1;
    }

    /**
     * 查询线下处方 入库操作
     *
     * @param organId
     * @param patientDTO
     * @param timeQuantum
     * @param flag
     */
    @RpcService
    public List<HisRecipe> queryHisRecipeInfo(Integer organId, PatientDTO patientDTO, Integer timeQuantum, Integer flag) {
        List<HisRecipe> recipes = new ArrayList<>();
        //查询数据
        HisResponseTO<List<QueryHisRecipResTO>> responseTO = queryData(organId, patientDTO, timeQuantum, flag, null);
        if (null == responseTO || CollectionUtils.isEmpty(responseTO.getData())) {
            return null;
        }
        try {
            /** 更新数据校验*/
            hisRecipeInfoCheck(responseTO.getData(), patientDTO);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo hisRecipeInfoCheck error ", e);
        }
        try {
            //数据入库
            recipes = saveHisRecipeInfo(responseTO, patientDTO, flag);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo saveHisRecipeInfo error ", e);
        }
        return recipes;
    }

    /**
     * @param organId
     * @param patientDTO
     * @param timeQuantum
     * @param flag
     * @return
     * @Author liumin
     * @Desciption 从 his查询待缴费已缴费的处方信息
     */
    public HisResponseTO<List<QueryHisRecipResTO>> queryData(Integer organId, PatientDTO patientDTO, Integer timeQuantum, Integer flag, String recipeCode) {
        //TODO question 查询条件带recipeCode
        //TODO question 让前置机去过滤数据
        LOGGER.info("HisRecipeService HisResponseTO.queryData:organId:{},patientDTO:{}", organId, JSONUtils.toString(patientDTO));
        PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
        patientBaseInfo.setBirthday(patientDTO.getBirthday());
        patientBaseInfo.setPatientName(patientDTO.getPatientName());
        patientBaseInfo.setPatientSex(patientDTO.getPatientSex());
        patientBaseInfo.setMobile(patientDTO.getMobile());
        patientBaseInfo.setMpi(patientDTO.getMpiId());
        patientBaseInfo.setCardID(patientDTO.getCardId());
        patientBaseInfo.setCertificate(patientDTO.getCertificate());

        QueryRecipeRequestTO queryRecipeRequestTO = new QueryRecipeRequestTO();
        queryRecipeRequestTO.setPatientInfo(patientBaseInfo);
        queryRecipeRequestTO.setEndDate(new Date());
        queryRecipeRequestTO.setOrgan(organId);
        queryRecipeRequestTO.setQueryType(flag);
        if (StringUtils.isNotEmpty(recipeCode)) {
            queryRecipeRequestTO.setRecipeCode(recipeCode);
        }
        IRecipeHisService recipeHisService = AppContextHolder.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("queryHisRecipeInfo input:" + JSONUtils.toString(queryRecipeRequestTO, QueryRecipeRequestTO.class));
        HisResponseTO<List<QueryHisRecipResTO>> responseTO = recipeHisService.queryHisRecipeInfo(queryRecipeRequestTO);
        LOGGER.info("queryHisRecipeInfo output:" + JSONUtils.toString(responseTO, HisResponseTO.class));
        //过滤数据
        responseTO = filterData(responseTO);
        LOGGER.info("queryHisRecipeInfo queryData:{}.", JSONUtils.toString(responseTO));
        return responseTO;
    }

    /**
     * @param responseTO
     * @return
     * @author liumin
     * @Description 数据过滤
     */
    private HisResponseTO<List<QueryHisRecipResTO>> filterData(HisResponseTO<List<QueryHisRecipResTO>> responseTO) {
        if (!StringUtils.isEmpty(recipeCodeThreadLocal.get())) {
            String recipeCode = recipeCodeThreadLocal.get();
            if (responseTO != null) {
                LOGGER.info("queryHisRecipeInfo recipeCodeThreadLocal:{}", recipeCode);
                List<QueryHisRecipResTO> queryHisRecipResTOs = responseTO.getData();
                List<QueryHisRecipResTO> queryHisRecipResTOFilters = new ArrayList<>();
                if (!CollectionUtils.isEmpty(queryHisRecipResTOs)) {
                    for (QueryHisRecipResTO queryHisRecipResTO : queryHisRecipResTOs) {
                        if (recipeCode.equals(queryHisRecipResTO.getRecipeCode())) {
                            queryHisRecipResTOFilters.add(queryHisRecipResTO);
                        }
                    }
                }
                responseTO.setData(queryHisRecipResTOFilters);
            }
        }
        return responseTO;
    }

    /**
     * 线下处方转换成前端所需对象
     *
     * @param responseTO
     * @param patientDTO
     * @param flag
     * @return
     */
    public List<HisRecipeVO> covertToHisRecipeObject(HisResponseTO<List<QueryHisRecipResTO>> responseTO, PatientDTO patientDTO, Integer flag) {
        List<HisRecipeVO> hisRecipeVOs = new ArrayList<>();
        if (responseTO == null) {
            return hisRecipeVOs;
        }
        List<QueryHisRecipResTO> queryHisRecipResTOList = responseTO.getData();
        if (CollectionUtils.isEmpty(queryHisRecipResTOList)) {
            return hisRecipeVOs;
        }
        LOGGER.info("covertHisRecipeObject queryHisRecipResTOList:" + JSONUtils.toString(queryHisRecipResTOList));
        for (QueryHisRecipResTO queryHisRecipResTO : queryHisRecipResTOList) {
            HisRecipe hisRecipe1 = hisRecipeDAO.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(
                    patientDTO.getMpiId(), queryHisRecipResTO.getClinicOrgan(), queryHisRecipResTO.getRecipeCode());
            //数据库不存在处方信息，则新增
            if (null == hisRecipe1) {
                HisRecipe hisRecipe = new HisRecipe();
                hisRecipe.setCertificate(patientDTO.getCertificate());
                hisRecipe.setCertificateType(patientDTO.getCertificateType());
                hisRecipe.setMpiId(patientDTO.getMpiId());
                hisRecipe.setPatientName(patientDTO.getPatientName());
                hisRecipe.setPatientAddress(patientDTO.getAddress());
                hisRecipe.setPatientNumber(queryHisRecipResTO.getPatientNumber());
                hisRecipe.setPatientTel(patientDTO.getMobile());
                hisRecipe.setRegisteredId(StringUtils.isNotEmpty(queryHisRecipResTO.getRegisteredId()) ? queryHisRecipResTO.getRegisteredId() : "");
                hisRecipe.setRecipeCode(queryHisRecipResTO.getRecipeCode());
                hisRecipe.setDepartCode(queryHisRecipResTO.getDepartCode());
                hisRecipe.setDepartName(queryHisRecipResTO.getDepartName());
                hisRecipe.setDoctorName(queryHisRecipResTO.getDoctorName());
                hisRecipe.setCreateDate(queryHisRecipResTO.getCreateDate());
                hisRecipe.setChronicDiseaseCode(StringUtils.isNotEmpty(queryHisRecipResTO.getChronicDiseaseCode()) ? queryHisRecipResTO.getChronicDiseaseCode() : "");
                hisRecipe.setChronicDiseaseName(StringUtils.isNotEmpty(queryHisRecipResTO.getChronicDiseaseName()) ? queryHisRecipResTO.getChronicDiseaseName() : "");
                hisRecipe.setStatus(queryHisRecipResTO.getStatus());
                if (new Integer(2).equals(queryHisRecipResTO.getMedicalType())) {
                    hisRecipe.setMedicalType(queryHisRecipResTO.getMedicalType());//医保类型
                } else {
                    hisRecipe.setMedicalType(1);//默认自费
                }
                hisRecipe.setRecipeFee(queryHisRecipResTO.getRecipeFee());
                hisRecipe.setRecipeType(queryHisRecipResTO.getRecipeType());
                hisRecipe.setClinicOrgan(queryHisRecipResTO.getClinicOrgan());
                hisRecipe.setCreateTime(new Date());
                hisRecipe.setExtensionFlag(1);
                if (queryHisRecipResTO.getExtensionFlag() == null) {
                    hisRecipe.setRecipePayType(0); //设置外延处方的标志
                } else {
                    hisRecipe.setRecipePayType(queryHisRecipResTO.getExtensionFlag()); //设置外延处方的标志
                }

                if (!StringUtils.isEmpty(queryHisRecipResTO.getDiseaseName())) {
                    hisRecipe.setDiseaseName(queryHisRecipResTO.getDiseaseName());
                } else {
                    hisRecipe.setDiseaseName("无");
                }
                hisRecipe.setDisease(queryHisRecipResTO.getDisease());
                if (!StringUtils.isEmpty(queryHisRecipResTO.getDoctorCode())) {
                    hisRecipe.setDoctorCode(queryHisRecipResTO.getDoctorCode());
                }
                OrganService organService = BasicAPI.getService(OrganService.class);
                OrganDTO organDTO = organService.getByOrganId(queryHisRecipResTO.getClinicOrgan());
                if (null != organDTO) {
                    hisRecipe.setOrganName(organDTO.getName());
                }
                setMedicalInfo(queryHisRecipResTO, hisRecipe);
                hisRecipe.setGiveMode(queryHisRecipResTO.getGiveMode());
                hisRecipe.setDeliveryCode(queryHisRecipResTO.getDeliveryCode());
                hisRecipe.setDeliveryName(queryHisRecipResTO.getDeliveryName());
                hisRecipe.setSendAddr(queryHisRecipResTO.getSendAddr());
                hisRecipe.setRecipeSource(queryHisRecipResTO.getRecipeSource());
                hisRecipe.setReceiverName(queryHisRecipResTO.getReceiverName());
                hisRecipe.setReceiverTel(queryHisRecipResTO.getReceiverTel());
                //未缓存在平台
                hisRecipe.setIsCachePlatform(0);

                HisRecipeVO hisRecipeVO = ObjectCopyUtils.convert(hisRecipe, HisRecipeVO.class);
                //设置其它信息
                hisRecipeVO.setOrganDiseaseName(hisRecipe.getDiseaseName());
                hisRecipeVO.setRecipeMode("ngarihealth");
                setOtherInfo(hisRecipeVO, hisRecipe.getMpiId(), queryHisRecipResTO.getRecipeCode(), queryHisRecipResTO.getClinicOrgan());

                hisRecipeVOs.add(hisRecipeVO);
            } else {
                //如果为已支付，不予返回
                if (!new Integer("2").equals(hisRecipe1.getStatus())) {
                    HisRecipeVO hisRecipeVO = ObjectCopyUtils.convert(hisRecipe1, HisRecipeVO.class);
                    setOtherInfo(hisRecipeVO, hisRecipe1.getMpiId(), queryHisRecipResTO.getRecipeCode(), queryHisRecipResTO.getClinicOrgan());
                    hisRecipeVO.setOrganDiseaseName(queryHisRecipResTO.getDiseaseName());
                    hisRecipeVO.setRecipeMode("ngarihealth");
                    hisRecipeVOs.add(hisRecipeVO);
                }
            }
        }
        LOGGER.info("covertHisRecipeObject hisRecipeVOs:" + JSONUtils.toString(hisRecipeVOs));
        return hisRecipeVOs;
    }

    private void setMedicalInfo(QueryHisRecipResTO queryHisRecipResTO, HisRecipe hisRecipe) {
        if (null != queryHisRecipResTO.getMedicalInfo()) {
            MedicalInfo medicalInfo = queryHisRecipResTO.getMedicalInfo();
            if (!ObjectUtils.isEmpty(medicalInfo.getMedicalAmount())) {
                hisRecipe.setMedicalAmount(medicalInfo.getMedicalAmount());
            }
            if (!ObjectUtils.isEmpty(medicalInfo.getCashAmount())) {
                hisRecipe.setCashAmount(medicalInfo.getCashAmount());
            }
            if (!ObjectUtils.isEmpty(medicalInfo.getTotalAmount())) {
                hisRecipe.setTotalAmount(medicalInfo.getTotalAmount());
            }
        }
    }

    private void setOtherInfo(HisRecipeVO hisRecipeVO, String mpiId, String recipeCode, Integer clinicOrgan) {
        Recipe recipe = recipeDAO.getByHisRecipeCodeAndClinicOrganAndMpiid(mpiId, recipeCode, clinicOrgan);
        if (recipe == null) {
            hisRecipeVO.setStatusText("待处理");
            hisRecipeVO.setFromFlag(1);
            hisRecipeVO.setJumpPageType(0);
        } else {
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
            if (recipeExtend != null && recipeExtend.getFromFlag() == 0) {
                //表示该处方来源于HIS
                if (StringUtils.isEmpty(recipe.getOrderCode())) {
                    hisRecipeVO.setStatusText("待处理");
                    hisRecipeVO.setJumpPageType(0);
                } else {
                    RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                    if (recipeOrder != null) {
                        if (new Integer(0).equals(recipeOrder.getPayFlag())) {
                            hisRecipeVO.setStatusText("待支付");
                        } else {
                            hisRecipeVO.setStatusText("已完成");
                        }
                        hisRecipeVO.setJumpPageType(1);//跳转到订单详情页
                        hisRecipeVO.setStatusText(getTipsByStatusForPatient(recipe, recipeOrder));
                        hisRecipeVO.setOrderCode(recipe.getOrderCode());
                    }
                }
                hisRecipeVO.setFromFlag(recipe.getRecipeSourceType() == 2 ? 1 : 0);
            } else {
                //表示该处方来源于平台
                hisRecipeVO.setStatusText("待支付");
                hisRecipeVO.setFromFlag(0);
                hisRecipeVO.setJumpPageType(0);
                hisRecipeVO.setOrganDiseaseName(recipe.getOrganDiseaseName());
                hisRecipeVO.setHisRecipeID(recipe.getRecipeId());
                List<HisRecipeDetailVO> recipeDetailVOS = getHisRecipeDetailVOS(recipe);
                hisRecipeVO.setRecipeDetail(recipeDetailVOS);
            }
        }
    }


    /**
     * 保存线下处方数据到cdr_his_recipe、HisRecipeDetail、HisRecipeExt
     * 保存前先校验数据
     * （1）如果数据在cdr_his_recipe已经存在,并且不是由本人生成，支付状态为未支付，先删除cdr_his_recipe[这里少删了表]，后新增
     * ，并且是由本人生成 ，则跳过此处方
     * （2）如果数据在cdr_his_recipe不存在，则直接新增
     *
     * @param responseTO
     * @param patientDTO
     * @param flag
     * @return
     */
    @RpcService
    public List<HisRecipe> saveHisRecipeInfo(HisResponseTO<List<QueryHisRecipResTO>> responseTO, PatientDTO patientDTO, Integer flag) {
        List<HisRecipe> hisRecipes = new ArrayList<>();
        if (responseTO == null) {
            return hisRecipes;
        }
        List<QueryHisRecipResTO> queryHisRecipResTOList = responseTO.getData();

        if (CollectionUtils.isEmpty(queryHisRecipResTOList)) {
            return hisRecipes;
        }
        LOGGER.info("saveHisRecipeInfo queryHisRecipResTOList:" + JSONUtils.toString(queryHisRecipResTOList));
        for (QueryHisRecipResTO queryHisRecipResTO : queryHisRecipResTOList) {
            HisRecipe hisRecipe1 = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(queryHisRecipResTO.getClinicOrgan(), queryHisRecipResTO.getRecipeCode());
            if (hisRecipe1 != null) {
                if (!patientDTO.getMpiId().equals(hisRecipe1.getMpiId())) {
                    List<Integer> hisRecipeIds = new ArrayList<>();
                    hisRecipeIds.add(hisRecipe1.getHisRecipeID());
                    //同recipeCode,organId不是本人生成的线下处方
                    Recipe haveRecipe = recipeDAO.getByHisRecipeCodeAndClinicOrgan(queryHisRecipResTO.getRecipeCode(), queryHisRecipResTO.getClinicOrgan());
                    //如果处方已经转到cdr_recipe表并且支付状态为待支付并且非本人转储到cdr_his_recipe，则先删除后新增
                    if (haveRecipe != null) {
                        if (new Integer(0).equals(haveRecipe.getPayFlag())) {
                            hisRecipeDAO.deleteByHisRecipeIds(hisRecipeIds);
                            hisRecipe1 = null;
                        }
                    } else {
                        hisRecipeDAO.deleteByHisRecipeIds(hisRecipeIds);
                        hisRecipe1 = null;
                    }
                } else {
                    //本人
                    hisRecipes.add(hisRecipe1);
                    //如果已缴费处方在数据库里已存在，且数据里的状态是未缴费，则将数据库里的未缴费状态更新为已缴费状态
                    if (2 == flag) {
                        if (1 == hisRecipe1.getStatus()) {
                            hisRecipe1.setStatus(queryHisRecipResTO.getStatus());
                            hisRecipeDAO.update(hisRecipe1);
                        }
                    }
                }
            }

            //数据库不存在处方信息，则新增
            if (null == hisRecipe1) {
                HisRecipe hisRecipe = new HisRecipe();
                hisRecipe.setCertificate(patientDTO.getCertificate());
                hisRecipe.setCertificateType(patientDTO.getCertificateType());
                hisRecipe.setMpiId(patientDTO.getMpiId());
                hisRecipe.setPatientName(patientDTO.getPatientName());
                hisRecipe.setPatientAddress(patientDTO.getAddress());
                hisRecipe.setPatientNumber(queryHisRecipResTO.getPatientNumber());
                hisRecipe.setPatientTel(patientDTO.getMobile());
                hisRecipe.setRegisteredId(StringUtils.isNotEmpty(queryHisRecipResTO.getRegisteredId()) ? queryHisRecipResTO.getRegisteredId() : "");
                hisRecipe.setChronicDiseaseCode(StringUtils.isNotEmpty(queryHisRecipResTO.getChronicDiseaseCode()) ? queryHisRecipResTO.getChronicDiseaseCode() : "");
                hisRecipe.setChronicDiseaseName(StringUtils.isNotEmpty(queryHisRecipResTO.getChronicDiseaseName()) ? queryHisRecipResTO.getChronicDiseaseName() : "");
                hisRecipe.setRecipeCode(queryHisRecipResTO.getRecipeCode());
                hisRecipe.setDepartCode(queryHisRecipResTO.getDepartCode());
                hisRecipe.setDepartName(queryHisRecipResTO.getDepartName());
                hisRecipe.setDoctorName(queryHisRecipResTO.getDoctorName());
                hisRecipe.setCreateDate(queryHisRecipResTO.getCreateDate());
                if (queryHisRecipResTO.getTcmNum() != null) {
                    hisRecipe.setTcmNum(queryHisRecipResTO.getTcmNum().toString());
                }
                hisRecipe.setStatus(queryHisRecipResTO.getStatus());
                if (new Integer(2).equals(queryHisRecipResTO.getMedicalType())) {
                    hisRecipe.setMedicalType(queryHisRecipResTO.getMedicalType());//医保类型
                } else {
                    hisRecipe.setMedicalType(1);//默认自费
                }
                hisRecipe.setRecipeFee(queryHisRecipResTO.getRecipeFee());
                hisRecipe.setRecipeType(queryHisRecipResTO.getRecipeType());
                hisRecipe.setClinicOrgan(queryHisRecipResTO.getClinicOrgan());
                hisRecipe.setCreateTime(new Date());
                hisRecipe.setExtensionFlag(1);
                if (queryHisRecipResTO.getExtensionFlag() == null) {
                    hisRecipe.setRecipePayType(0); //设置外延处方的标志
                } else {
                    hisRecipe.setRecipePayType(queryHisRecipResTO.getExtensionFlag()); //设置外延处方的标志
                }

                if (!StringUtils.isEmpty(queryHisRecipResTO.getDiseaseName())) {
                    hisRecipe.setDiseaseName(queryHisRecipResTO.getDiseaseName());
                } else {
                    hisRecipe.setDiseaseName("无");
                }
                hisRecipe.setDisease(queryHisRecipResTO.getDisease());
                if (!StringUtils.isEmpty(queryHisRecipResTO.getDoctorCode())) {
                    hisRecipe.setDoctorCode(queryHisRecipResTO.getDoctorCode());
                }
                OrganService organService = BasicAPI.getService(OrganService.class);
                OrganDTO organDTO = organService.getByOrganId(queryHisRecipResTO.getClinicOrgan());
                if (null != organDTO) {
                    hisRecipe.setOrganName(organDTO.getName());
                }
                setMedicalInfo(queryHisRecipResTO, hisRecipe);
                hisRecipe.setGiveMode(queryHisRecipResTO.getGiveMode());
                hisRecipe.setDeliveryCode(queryHisRecipResTO.getDeliveryCode());
                hisRecipe.setDeliveryName(queryHisRecipResTO.getDeliveryName());
                hisRecipe.setSendAddr(queryHisRecipResTO.getSendAddr());
                hisRecipe.setRecipeSource(queryHisRecipResTO.getRecipeSource());
                hisRecipe.setReceiverName(queryHisRecipResTO.getReceiverName());
                hisRecipe.setReceiverTel(queryHisRecipResTO.getReceiverTel());

                //中药
                hisRecipe.setRecipeCostNumber(queryHisRecipResTO.getRecipeCostNumber());
                hisRecipe.setTcmFee(queryHisRecipResTO.getTcmFee());
                hisRecipe.setDecoctionFee(queryHisRecipResTO.getDecoctionFee());
                hisRecipe.setDecoctionCode(queryHisRecipResTO.getDecoctionCode());
                hisRecipe.setDecoctionText(queryHisRecipResTO.getDecoctionText());
                hisRecipe.setTcmNum(queryHisRecipResTO.getTcmNum() == null ? null : String.valueOf(queryHisRecipResTO.getTcmNum()));
                //中药医嘱跟着处方 西药医嘱跟着药品（见药品详情）
                hisRecipe.setRecipeMemo(queryHisRecipResTO.getRecipeMemo());
                //审核药师
                hisRecipe.setCheckerCode(queryHisRecipResTO.getCheckerCode());
                hisRecipe.setCheckerName(queryHisRecipResTO.getCheckerName());
                try {
                    hisRecipe = hisRecipeDAO.save(hisRecipe);
                    LOGGER.info("saveHisRecipeInfo hisRecipe:{} 当前时间：{}", hisRecipe, System.currentTimeMillis());
                    hisRecipes.add(hisRecipe);
                } catch (Exception e) {
                    LOGGER.error("hisRecipeDAO.save error ", e);
                    return hisRecipes;
                }

                //TODO 需要提到判断条件之外 保存前判断处方关联数据是否存在  存在先删除 然后新增 不存在新增
                if (null != queryHisRecipResTO.getExt()) {
                    for (ExtInfoTO extInfoTO : queryHisRecipResTO.getExt()) {
                        HisRecipeExt ext = ObjectCopyUtils.convert(extInfoTO, HisRecipeExt.class);
                        ext.setHisRecipeId(hisRecipe.getHisRecipeID());
                        hisRecipeExtDAO.save(ext);
                    }
                }
                //TODO 需要提到判断条件之外 保存前判断处方关联数据是否存在  存在先删除 然后新增 不存在新增
                if (null != queryHisRecipResTO.getDrugList()) {
                    for (RecipeDetailTO recipeDetailTO : queryHisRecipResTO.getDrugList()) {
                        HisRecipeDetail detail = ObjectCopyUtils.convert(recipeDetailTO, HisRecipeDetail.class);
                        detail.setHisRecipeId(hisRecipe.getHisRecipeID());
                        detail.setRecipeDeatilCode(recipeDetailTO.getRecipeDeatilCode());
                        detail.setDrugName(recipeDetailTO.getDrugName());
                        detail.setPrice(recipeDetailTO.getPrice());
                        detail.setTotalPrice(recipeDetailTO.getTotalPrice());
                        detail.setUsingRate(recipeDetailTO.getUsingRate());
                        detail.setUsePathways(recipeDetailTO.getUsePathWays());
                        detail.setDrugSpec(recipeDetailTO.getDrugSpec());
                        detail.setDrugUnit(recipeDetailTO.getDrugUnit());
                        //date 20200526
                        //修改线下处方同步用药天数，判断是否有小数类型的用药天数
                        //修改逻辑，UseDays这个字段为老字段只有整数
                        //UseDaysB这个字段为字符类型，可能小数，准备之后用药天数都用这个字段
                        //取对应没有的字段设置传过来的值
                        //这两个值只会有一个没有传
                        if (null != recipeDetailTO.getUseDays()) {
                            //设置字符类型的
                            detail.setUseDaysB(recipeDetailTO.getUseDays().toString());
                        }
                        if (null != recipeDetailTO.getUseDaysB()) {
                            //设置int类型的
                            //设置字符转向上取整
                            int useDays = new BigDecimal(recipeDetailTO.getUseDaysB()).setScale(0, BigDecimal.ROUND_UP).intValue();
                            detail.setUseDays(useDays);
                        }
//                        detail.setUseDays(recipeDetailTO.getUseDays());
//                        detail.setUseDaysB(recipeDetailTO.getUseDays().toString());
                        detail.setDrugCode(recipeDetailTO.getDrugCode());
                        detail.setUsingRateText(recipeDetailTO.getUsingRateText());
                        detail.setUsePathwaysText(recipeDetailTO.getUsePathwaysText());
                        //  线下特殊用法
                        detail.setUseDoseStr(recipeDetailTO.getUseDoseStr());
                        detail.setUseDose(recipeDetailTO.getUseDose());
                        detail.setUseDoseUnit(recipeDetailTO.getUseDoseUnit());
                        detail.setSaleName(recipeDetailTO.getSaleName());
                        detail.setPack(recipeDetailTO.getPack());
                        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
                        if (StringUtils.isNotEmpty(detail.getRecipeDeatilCode())) {
                            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(hisRecipe.getClinicOrgan(), Arrays.asList(detail.getDrugCode()));
                            if (CollectionUtils.isEmpty(organDrugLists)) {
                                LOGGER.info("saveHisRecipeInfo organDrugLists his传过来的药品编码没有在对应机构维护,organId:" + hisRecipe.getClinicOrgan() + ",organDrugCode:" + detail.getDrugCode());
                            }
                        }
                        detail.setStatus(1);
                        //西药医嘱
                        detail.setMemo(recipeDetailTO.getMemo());
                        //药房信息
                        detail.setPharmacyCode(recipeDetailTO.getPharmacyCode());
                        detail.setPharmacyName(recipeDetailTO.getPharmacyName());
                        hisRecipeDetailDAO.save(detail);
                    }
                }
            }
        }
        return hisRecipes;
    }

    private List<HisRecipeDetailVO> getHisRecipeDetailVOS(Recipe recipe) {
        List<HisRecipeDetailVO> recipeDetailVOS = new ArrayList<>();
        //药品名拼接配置
        Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(recipe.getClinicOrgan(), recipe.getRecipeType()));
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        for (Recipedetail recipedetail : recipedetails) {
            HisRecipeDetailVO hisRecipeDetailVO = new HisRecipeDetailVO();
            hisRecipeDetailVO.setDrugName(recipedetail.getDrugName());
            hisRecipeDetailVO.setDrugSpec(recipedetail.getDrugSpec());
            hisRecipeDetailVO.setDrugUnit(recipedetail.getDrugUnit());
            hisRecipeDetailVO.setPack(recipedetail.getPack());
            hisRecipeDetailVO.setDrugForm(recipedetail.getDrugForm());
            hisRecipeDetailVO.setUseTotalDose(new BigDecimal(recipedetail.getUseTotalDose()));
            hisRecipeDetailVO.setUseDose(recipedetail.getUseDose() == null ? "" : recipedetail.getUseDose().toString());
            hisRecipeDetailVO.setDrugUnit(recipedetail.getDrugUnit());
            DrugListBean drugList = new DrugListBean();
            drugList.setDrugName(hisRecipeDetailVO.getDrugName());
            drugList.setSaleName(hisRecipeDetailVO.getSaleName());
            drugList.setDrugSpec(hisRecipeDetailVO.getDrugSpec());
            drugList.setUnit(hisRecipeDetailVO.getDrugUnit());
            drugList.setDrugForm(hisRecipeDetailVO.getDrugForm());
            //前端展示的药品拼接名处理
            hisRecipeDetailVO.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(drugList, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(recipe.getRecipeType())));
            recipeDetailVOS.add(hisRecipeDetailVO);
        }
        return recipeDetailVOS;
    }


    /**
     * @author liumin
     * @Description 获取处方详情
     */
    /**
     * @param hisRecipeId
     * @param mpiId
     * @param recipeCode
     * @param organId
     * @param isCachePlatform 作废
     * @param cardId
     * @return
     * @author liumin
     * @Description 获取处方详情
     */
    @RpcService
    public Map<String, Object> getHisRecipeDetail(Integer hisRecipeId, String mpiId, String recipeCode, String organId, Integer isCachePlatform, String cardId) {
        LOGGER.info("HisRecipeService getHisRecipeDetail param:[{},{},{},{},{},{}]", hisRecipeId, mpiId, recipeCode, organId, isCachePlatform, cardId);
        HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(mpiId, Integer.parseInt(organId), recipeCode);
        if (hisRecipe == null) {
            //throw new DAOException(700, "该处方单信息已变更，请退出重新获取处方信息。");
        }
        LOGGER.info("getHisRecipeDetail hisRecipe:{}.", JSONUtils.toString(hisRecipe));
        //待处理
        Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode, Integer.parseInt(organId));
        Integer payFlag = 0;
        if (recipe != null && StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            if (new Integer(1).equals(recipeOrder.getPayFlag())) {
                payFlag = 1;
            }
        }
        //TODO liumin 这个条件应改为待缴费处方还是已缴费处方，已缴费处方不用走这个逻辑了  但是重复走这个逻辑也没关系，保存时会判断，存在数据也不会新增
        if (hisRecipe == null || hisRecipe.getStatus() != 2 || payFlag == 1) {
            LOGGER.info("getHisRecipeDetail 进入");
            try {
                PatientService patientService = BasicAPI.getService(PatientService.class);
                PatientDTO patientDTO = patientService.getPatientBeanByMpiId(mpiId);
                if (null == patientDTO) {
                    throw new DAOException(609, "患者信息不存在");
                }
                if (StringUtils.isNotEmpty(cardId)) {
                    patientDTO.setCardId(cardId);
                } else {
                    patientDTO.setCardId("");
                }
                recipeCodeThreadLocal.set(recipeCode);
                //线下处方处理(存储到cdr_his相关表)
                List<HisRecipe> hisRecipes = queryHisRecipeInfo(new Integer(organId), patientDTO, 180, 1);
                if (CollectionUtils.isEmpty(hisRecipes)) {
                    return initReturnMap();
                } else {
                    hisRecipeId = hisRecipes.get(0).getHisRecipeID();
                }
            } catch (Exception e) {
                LOGGER.error("getHisRecipeDetail error hisRecipeId:{}", hisRecipeId, e);
            } finally {
                recipeCodeThreadLocal.remove();
            }
        }
        if (hisRecipeId == null) {
            //点击卡片 历史处方his不会返回 故从表查  同时也兼容已处理状态的处方，前端漏传hisRecipeId的情况
            if (!StringUtils.isEmpty(recipeCode)) {
                hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(Integer.parseInt(organId), recipeCode);
            }
            if (hisRecipe != null) {
                hisRecipeId = hisRecipe.getHisRecipeID();
            }
        }

        //存储到recipe相关表
        if (hisRecipeId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "hisRecipeId不能为空！");
        }
        return getHisRecipeDetailByHisRecipeId(hisRecipeId);

    }

    private Map<String, Object> initReturnMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("hisRecipeDetails", null);
        map.put("hisRecipeExts", null);
        map.put("showText", null);
        return map;
    }

    /**
     * @param hisRecipeId
     * @return
     * @author liumin
     * @Description 转平台处方并根据hisRecipeId去表里查返回详情
     */
    private Map<String, Object> getHisRecipeDetailByHisRecipeId(Integer hisRecipeId) {
        if (hisRecipeId == null) {
            return null;
        }
        //将线下处方转化成线上处方
        HisRecipe hisRecipe = hisRecipeDAO.get(hisRecipeId);
        if (hisRecipe == null) {
            throw new DAOException(DAOException.DAO_NOT_FOUND, "没有查询到来自医院的处方单");
        }
        Recipe recipe = saveRecipeFromHisRecipe(hisRecipe);
        if (recipe != null) {
            // 线下转线上失效时间处理--仅平台线下转线上需处理（目前互联网环境没有线下转线上，不判断平台还是互联网）
            RecipeService.handleRecipeInvalidTime(recipe.getClinicOrgan(), recipe.getRecipeId(), recipe.getSignDate());
            saveRecipeExt(recipe, hisRecipe);
            //生成处方详情
            savaRecipeDetail(recipe.getRecipeId(), hisRecipe);
            // 线下转线上处理处方支持的购药按钮
//            Integer continueFlag = getContinueFlag(recipe);
            List<Integer> drugsEnterpriseContinue = drugsEnterpriseService.getDrugsEnterpriseContinue(recipe.getRecipeId(), recipe.getClinicOrgan());
            LOGGER.info("getHisRecipeDetailByHisRecipeId recipeId = {} drugsEnterpriseContinue = {}", recipe.getRecipeId(), JSONUtils.toString(drugsEnterpriseContinue));
            if (CollectionUtils.isNotEmpty(drugsEnterpriseContinue)) {
                Map<String, Object> attMap = new HashMap<>();
                String join = StringUtils.join(drugsEnterpriseContinue, ",");
                attMap.put("recipeSupportGiveMode", join);
                recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), attMap);
            }
        }
        Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(recipe.getClinicOrgan(), recipe.getRecipeType()));
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        Map<String, Object> map = recipeService.getPatientRecipeById(recipe.getRecipeId());
        //特殊处理线下药品名展示
        List<RecipeDetailBean> recipeDetailBeans = (List<RecipeDetailBean>) map.get("recipedetails");
        recipeDetailBeans.forEach(recipeDetailBean -> {
            recipeDetailBean.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(recipeDetailBean, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(recipe.getRecipeType())));
        });
        //if(recipeListService.isReturnRecipeDetail(recipe.getClinicOrgan(),recipe.getRecipeType(),recipe.getPayFlag())){
        List<HisRecipeDetail> hisRecipeDetails = hisRecipeDetailDAO.findByHisRecipeId(hisRecipeId);
        map.put("hisRecipeDetails", hisRecipeDetails);
        //}
        List<HisRecipeExt> hisRecipeExts = hisRecipeExtDAO.findByHisRecipeId(hisRecipeId);
        map.put("hisRecipeExts", hisRecipeExts);
        map.put("showText", hisRecipe.getShowText());
        LOGGER.info("getHisRecipeDetailByHisRecipeId response:{}", JSONUtils.toString(map));
        return map;
    }

    private void saveRecipeExt(Recipe recipe, HisRecipe hisRecipe) {
        Integer recipeId = recipe.getRecipeId();
        RecipeExtend haveRecipeExt = recipeExtendDAO.getByRecipeId(recipeId);
        if (haveRecipeExt != null) {
            return;
        }
        RecipeExtend recipeExtend = new RecipeExtend();
        recipeExtend.setRecipeId(recipeId);
        recipeExtend.setFromFlag(0);
        recipeExtend.setRegisterID(hisRecipe.getRegisteredId());
        recipeExtend.setChronicDiseaseCode(hisRecipe.getChronicDiseaseCode());
        recipeExtend.setChronicDiseaseName(hisRecipe.getChronicDiseaseName());
        //设置煎法
        if (StringUtils.isNotEmpty(hisRecipe.getDecoctionText())) {
            recipeExtend.setDecoctionText(hisRecipe.getDecoctionText());
        }
        try {
            IRevisitExService exService = RevisitAPI.getService(IRevisitExService.class);
            RevisitExDTO consultExDTO = exService.getByRegisterId(hisRecipe.getRegisteredId());
            if (consultExDTO != null) {
                recipeExtend.setCardNo(consultExDTO.getCardId());
            }
        } catch (Exception e) {
            LOGGER.error("线下处方转线上通过挂号序号关联复诊 error", e);
        }
        //中药
        recipeExtend.setRecipeCostNumber(hisRecipe.getRecipeCostNumber());
        RecipeBean recipeBean = new RecipeBean();
        BeanUtils.copy(recipe, recipeBean);
        emrRecipeManager.saveMedicalInfo(recipeBean, recipeExtend);
        recipeExtendDAO.save(recipeExtend);
    }

    private Recipe saveRecipeFromHisRecipe(HisRecipe hisRecipe) {
        LOGGER.info("saveRecipeFromHisRecipe hisRecipe:{}.", JSONUtils.toString(hisRecipe));
        Recipe haveRecipe = recipeDAO.getByHisRecipeCodeAndClinicOrgan(hisRecipe.getRecipeCode(), hisRecipe.getClinicOrgan());
        LOGGER.info("saveRecipeFromHisRecipe haveRecipe:{}.", JSONUtils.toString(haveRecipe));
        UserRoleToken userRoleToken = UserRoleToken.getCurrent();
        if (haveRecipe != null) {
            //TODO 在表存在 更新除recipeId外所有数据 这里删掉 因为在校验的时候会判断如果不是由本人生成的待缴费处方会更新全部信息
            //如果处方已经转到cdr_recipe表并且支付状态为待支付并且非本人转储到cdr_recipe，则替换用户信息
            if (new Integer(0).equals(haveRecipe.getPayFlag())
                    && !StringUtils.isEmpty(hisRecipe.getMpiId())
                    && !hisRecipe.getMpiId().equals(haveRecipe.getMpiid())) {
                //修改处方患者信息
                haveRecipe.setMpiid(hisRecipe.getMpiId());
                haveRecipe.setRequestMpiId(userRoleToken.getOwnMpiId());
                haveRecipe.setPatientName(hisRecipe.getPatientName());
                haveRecipe.setPatientID(hisRecipe.getPatientNumber());
                recipeDAO.update(haveRecipe);
            }
            return haveRecipe;
        }
        Recipe recipe = new Recipe();
        recipe.setBussSource(0);
        //通过挂号序号关联复诊
        try {
            IRevisitExService exService = RevisitAPI.getService(IRevisitExService.class);
            RevisitExDTO consultExDTO = exService.getByRegisterId(hisRecipe.getRegisteredId());
            if (consultExDTO != null) {
                recipe.setBussSource(2);
                recipe.setClinicId(consultExDTO.getConsultId());
            }
        } catch (Exception e) {
            LOGGER.error("线下处方转线上通过挂号序号关联复诊 error", e);
        }

        recipe.setClinicOrgan(hisRecipe.getClinicOrgan());
        recipe.setMpiid(hisRecipe.getMpiId());
        recipe.setPatientName(hisRecipe.getPatientName());
        recipe.setPatientID(hisRecipe.getPatientNumber());
        recipe.setPatientStatus(1);
        recipe.setOrganName(hisRecipe.getOrganName());
        recipe.setRecipeCode(hisRecipe.getRecipeCode());
        recipe.setRecipeType(hisRecipe.getRecipeType());
        //BUG#50592 【实施】【上海市奉贤区中心医院】【A】查询线下处方缴费提示系统繁忙
        AppointDepartService appointDepartService = ApplicationUtils.getBasicService(AppointDepartService.class);
        AppointDepartDTO appointDepartDTO = appointDepartService.getByOrganIDAndAppointDepartCode(hisRecipe.getClinicOrgan(), hisRecipe.getDepartCode());
        if (appointDepartDTO != null) {
            recipe.setDepart(appointDepartDTO.getDepartId());
        } else {
            LOGGER.info("HisRecipeService saveRecipeFromHisRecipe 无法查询到挂号科室:{}.", hisRecipe.getDepartCode());
        }
        EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
        if (StringUtils.isNotEmpty(hisRecipe.getDoctorCode())) {
            EmploymentDTO employmentDTO = employmentService.getByJobNumberAndOrganId(hisRecipe.getDoctorCode(), hisRecipe.getClinicOrgan());
            if (employmentDTO != null && employmentDTO.getDoctorId() != null) {
                recipe.setDoctor(employmentDTO.getDoctorId());
            } else {
                LOGGER.error("请确认医院的医生工号和纳里维护的是否一致:" + hisRecipe.getDoctorCode());
                throw new DAOException(ErrorCode.SERVICE_ERROR, "请将医院的医生工号和纳里维护的医生工号保持一致");
            }
        }

        if (StringUtils.isNotEmpty(hisRecipe.getCheckerCode())) {
            EmploymentDTO employmentDTO = employmentService.getByJobNumberAndOrganId(hisRecipe.getCheckerCode(), hisRecipe.getClinicOrgan());
            if (employmentDTO != null && employmentDTO.getDoctorId() != null) {
                recipe.setChecker(employmentDTO.getDoctorId());
                recipe.setCheckerText(hisRecipe.getCheckerName());
            } else {
                LOGGER.error("请确认医院的医生工号和纳里维护的是否一致:" + hisRecipe.getDoctorCode());
                throw new DAOException(ErrorCode.SERVICE_ERROR, "请将医院的医生工号和纳里维护的医生工号保持一致");
            }
        } else {
            IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            DoctorService doctorService = BasicAPI.getService(DoctorService.class);
            String doctorId = (String) configurationService.getConfiguration(recipe.getClinicOrgan(), "offlineDefaultRecipecheckDoctor");
            if (StringUtils.isNotEmpty(doctorId)) {
                DoctorDTO doctorDTO = doctorService.getByDoctorId(Integer.parseInt(doctorId));
                recipe.setChecker(Integer.parseInt(doctorId));
                recipe.setCheckerText(doctorDTO.getName());
            }
        }
        recipe.setDoctorName(hisRecipe.getDoctorName());
        recipe.setCreateDate(hisRecipe.getCreateDate());
        recipe.setSignDate(hisRecipe.getCreateDate());
        recipe.setOrganDiseaseName(hisRecipe.getDiseaseName());
        recipe.setOrganDiseaseId(hisRecipe.getDisease());
        recipe.setTotalMoney(hisRecipe.getRecipeFee());
        recipe.setActualPrice(hisRecipe.getRecipeFee());
        recipe.setMemo(hisRecipe.getMemo() == null ? "无" : hisRecipe.getMemo());
        recipe.setPayFlag(0);
        if (hisRecipe.getStatus() == 2) {
            recipe.setStatus(6);
        } else {
            recipe.setStatus(2);
        }

        recipe.setReviewType(0);
        recipe.setChooseFlag(0);
        recipe.setRemindFlag(0);
        recipe.setPushFlag(0);
        recipe.setTakeMedicine(0);
        recipe.setGiveFlag(0);
        recipe.setRecipeMode("ngarihealth");
        if (hisRecipe.getTcmNum() != null) {
            recipe.setCopyNum(Integer.parseInt(hisRecipe.getTcmNum()));
        } else {
            recipe.setCopyNum(1);
        }

        recipe.setValueDays(3);
        recipe.setFromflag(1);
        recipe.setRecipeSourceType(2);
        recipe.setRecipePayType(hisRecipe.getRecipePayType());
        recipe.setRequestMpiId(userRoleToken.getOwnMpiId());
        recipe.setRecipeSource(hisRecipe.getRecipeSource());
        recipe.setGiveMode(hisRecipe.getGiveMode());
        recipe.setLastModify(new Date());
        //中药
        recipe.setCopyNum(StringUtils.isEmpty(hisRecipe.getTcmNum()) == true ? null : Integer.parseInt(hisRecipe.getTcmNum()));
        //中药医嘱跟着处方 西药医嘱跟着药品（见药品详情）
        recipe.setRecipeMemo(hisRecipe.getRecipeMemo());
        return recipeDAO.saveRecipe(recipe);

    }

    private void savaRecipeDetail(Integer recipeId, HisRecipe hisRecipe) {
        List<HisRecipeDetail> hisRecipeDetails = hisRecipeDetailDAO.findByHisRecipeId(hisRecipe.getHisRecipeID());
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isNotEmpty(recipedetails)) {
            return;
        }
        for (HisRecipeDetail hisRecipeDetail : hisRecipeDetails) {
            LOGGER.info("hisRecipe.getClinicOrgan(): " + hisRecipe.getClinicOrgan() + "");
            LOGGER.info("Arrays.asList(hisRecipeDetail.getDrugCode()):" + hisRecipeDetail.getDrugCode());
            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(hisRecipe.getClinicOrgan(), Arrays.asList(hisRecipeDetail.getDrugCode()));
            if (CollectionUtils.isEmpty(organDrugLists)) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "请将医院的药品信息维护到纳里机构药品目录");
            }
            Recipedetail recipedetail = new Recipedetail();
            recipedetail.setRecipeId(recipeId);
            recipedetail.setUseDoseUnit(hisRecipeDetail.getUseDoseUnit());
            //用量纯数字的存useDose,非数字的存useDoseStr
            if (!StringUtils.isEmpty(hisRecipeDetail.getUseDose())) {
                try {
                    recipedetail.setUseDose(Double.valueOf(hisRecipeDetail.getUseDose()));//高优先级
                } catch (Exception e) {
                    recipedetail.setUseDoseStr(hisRecipeDetail.getUseDose() + hisRecipeDetail.getUseDoseUnit());
                }
            }
            //  线下特殊用法
            if (!StringUtils.isEmpty(hisRecipeDetail.getUseDoseStr())) {
                try {
                    if (recipedetail.getUseDose() == null) {
                        recipedetail.setUseDose(Double.valueOf(hisRecipeDetail.getUseDoseStr()));
                    }
                } catch (Exception e) {
                    recipedetail.setUseDoseStr(hisRecipeDetail.getUseDoseStr() + hisRecipeDetail.getUseDoseUnit());//高优先级
                }
            }

            if (StringUtils.isNotEmpty(hisRecipeDetail.getDrugSpec())) {
                recipedetail.setDrugSpec(hisRecipeDetail.getDrugSpec());
            } else {
                if (CollectionUtils.isNotEmpty(organDrugLists)) {
                    recipedetail.setDrugSpec(organDrugLists.get(0).getDrugSpec());
                }
            }
            if (StringUtils.isNotEmpty(hisRecipeDetail.getDrugName())) {
                recipedetail.setDrugName(hisRecipeDetail.getDrugName());
            } else {
                if (CollectionUtils.isNotEmpty(organDrugLists)) {
                    recipedetail.setDrugName(organDrugLists.get(0).getDrugName());
                }
            }
            if (StringUtils.isNotEmpty(hisRecipeDetail.getDrugUnit())) {
                recipedetail.setDrugUnit(hisRecipeDetail.getDrugUnit());
            } else {
                if (CollectionUtils.isNotEmpty(organDrugLists)) {
                    recipedetail.setDrugUnit(organDrugLists.get(0).getUnit());
                }
            }
            if (hisRecipeDetail.getPack() != null) {
                recipedetail.setPack(hisRecipeDetail.getPack());
            } else {
                if (CollectionUtils.isNotEmpty(organDrugLists)) {
                    recipedetail.setPack(organDrugLists.get(0).getPack());
                }
            }
            if (hisRecipeDetail.getPrice() != null) {
                recipedetail.setSalePrice(hisRecipeDetail.getPrice());
            }

            if (CollectionUtils.isNotEmpty(organDrugLists)) {
                recipedetail.setDrugId(organDrugLists.get(0).getDrugId());
                recipedetail.setOrganDrugCode(hisRecipeDetail.getDrugCode());
                //recipedetail.setUsingRate(organDrugLists.get(0).getUsingRate());
                //recipedetail.setUsePathways(organDrugLists.get(0).getUsePathways());
                if (StringUtils.isEmpty(recipedetail.getUseDoseUnit())) {
                    recipedetail.setUseDoseUnit(organDrugLists.get(0).getUseDoseUnit());
                }
            }
            recipedetail.setUsingRateTextFromHis(hisRecipeDetail.getUsingRateText());
            recipedetail.setUsePathwaysTextFromHis(hisRecipeDetail.getUsePathwaysText());
            if (hisRecipeDetail.getUseTotalDose() != null) {
                recipedetail.setUseTotalDose(hisRecipeDetail.getUseTotalDose().doubleValue());
            }
            recipedetail.setUseDays(hisRecipeDetail.getUseDays());
            //date 20200528
            //设置线上处方的信息
            recipedetail.setUseDaysB(hisRecipeDetail.getUseDaysB());
            recipedetail.setStatus(1);

            //单药品总价使用线下传过来的，传过来多少就是多少我们不计算
            if (hisRecipeDetail.getTotalPrice() != null) {
                recipedetail.setDrugCost(hisRecipeDetail.getTotalPrice());
            }
            recipedetail.setMemo(hisRecipeDetail.getMemo());
            //药房信息
            if (StringUtils.isNotEmpty(hisRecipeDetail.getPharmacyCode())) {
                PharmacyTcm pharmacy = pharmacyTcmDAO.getByPharmacyAndOrganId(hisRecipeDetail.getPharmacyCode(), hisRecipe.getClinicOrgan());
                if (pharmacy != null) {
                    recipedetail.setPharmacyId(pharmacy.getPharmacyId());
                    recipedetail.setPharmacyName(pharmacy.getPharmacyName());
                }
            }
            if (StringUtils.isNotEmpty(hisRecipeDetail.getPharmacyName())) {
                recipedetail.setPharmacyName(hisRecipeDetail.getPharmacyName());
            }

            recipeDetailDAO.save(recipedetail);
        }
    }

    private String getRecipeStatusTabText(int status) {
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
                msg = "未支付";
                break;
            case RecipeStatusConstant.NO_OPERATOR:
                msg = "未处理";
                break;
            //已撤销从已取消拆出来
            case RecipeStatusConstant.REVOKE:
                msg = "已撤销";
                break;
            //已撤销从已取消拆出来
            case RecipeStatusConstant.DELETE:
                msg = "已删除";
                break;
            //写入his失败从已取消拆出来
            case RecipeStatusConstant.HIS_FAIL:
                msg = "写入his失败";
                break;
            case RecipeStatusConstant.CHECK_NOT_PASS_YS:
                msg = "审核不通过";
                break;
            case RecipeStatusConstant.IN_SEND:
                msg = "配送中";
                break;
            case RecipeStatusConstant.WAIT_SEND:
                msg = "待配送";
                break;
            case RecipeStatusConstant.READY_CHECK_YS:
                msg = "待审核";
                break;
            case RecipeStatusConstant.CHECK_PASS_YS:
                msg = "审核通过";
                break;
            //这里患者取药失败和取药失败都判定为失败
            case RecipeStatusConstant.NO_DRUG:
            case RecipeStatusConstant.RECIPE_FAIL:
                msg = "失败";
                break;
            case RecipeStatusConstant.RECIPE_DOWNLOADED:
                msg = "待取药";
                break;
            case RecipeStatusConstant.USING:
                msg = "处理中";
                break;
            default:
                msg = "未知状态";
        }

        return msg;
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
        Integer orderStatus = order.getStatus();
        String tips = "";
        switch (RecipeStatusEnum.getRecipeStatusEnum(status)) {
            case RECIPE_STATUS_HIS_FAIL:
                tips = "已取消";
                break;
            case RECIPE_STATUS_FINISH:
                tips = "已完成";
                break;
            case RECIPE_STATUS_IN_SEND:
                tips = "配送中";
                break;
            case RECIPE_STATUS_CHECK_PASS:
                if (null == payMode || null == giveMode) {
                    tips = "待处理";
                } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode)) {
                    if (new Integer(1).equals(recipe.getRecipePayType()) && payFlag == 1) {
                        tips = "已支付";
                    } else if (payFlag == 0) {
                        tips = "待支付";
                    } else {
                        tips = "待取药";
                    }
                } else if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)) {
                    if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
                        if (payFlag == 0) {
                            tips = "待支付";
                        } else {
                            if (OrderStatusConstant.READY_SEND.equals(orderStatus)) {
                                tips = "待配送";
                            } else if (OrderStatusConstant.SENDING.equals(orderStatus)) {
                                tips = "配送中";
                            } else if (OrderStatusConstant.FINISH.equals(orderStatus)) {
                                tips = "已完成";
                            }
                        }
                    }

                } else if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode) && StringUtils.isNotEmpty(recipe.getOrderCode())) {
                    if (OrderStatusConstant.HAS_DRUG.equals(orderStatus)) {
                        if (payFlag == 0) {
                            tips = "待支付";
                        } else {
                            tips = "待取药";
                        }
                    }
                } else if (RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(giveMode)) {
                    tips = "已完成";
                } else if (RecipeOrderStatusEnum.ORDER_STATUS_DONE_DISPENSING.getType().equals(orderStatus)) {
                    tips = RecipeOrderStatusEnum.ORDER_STATUS_DONE_DISPENSING.getName();
                }
                break;
            case RECIPE_STATUS_REVOKE:
                if (RecipeOrderStatusEnum.ORDER_STATUS_DRUG_WITHDRAWAL.getType().equals(orderStatus)) {
                    tips = RecipeOrderStatusEnum.ORDER_STATUS_DRUG_WITHDRAWAL.getName();
                } else if (RecipeOrderStatusEnum.ORDER_STATUS_DECLINE.getType().equals(orderStatus)) {
                    tips = RecipeOrderStatusEnum.ORDER_STATUS_DECLINE.getName();
                } else {
                    tips = "已取消";
                }
                break;
            case RECIPE_STATUS_DONE_DISPENSING:
                tips = RecipeStatusEnum.RECIPE_STATUS_DONE_DISPENSING.getName();
                break;
            case RECIPE_STATUS_DECLINE:
                tips = RecipeStatusEnum.RECIPE_STATUS_DECLINE.getName();
                break;
            case RECIPE_STATUS_DRUG_WITHDRAWAL:
                tips = RecipeStatusEnum.RECIPE_STATUS_DRUG_WITHDRAWAL.getName();
                break;
            case REVIEW_DRUG_FAIL:
                tips = RecipeStatusEnum.REVIEW_DRUG_FAIL.getName();
                break;
            default:
                tips = "待取药";
        }
        return tips;
    }

    @RpcService
    public List<String> getCardType(Integer organId) {
        //卡类型 1 表示身份证  2 表示就诊卡  3 表示就诊卡
        IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        //根据运营平台配置  如果配置了就诊卡 医保卡（根据卡类型进行查询）； 如果都不配（默认使用身份证查询）
        String[] cardTypes = (String[]) configurationCenterUtilsService.getConfiguration(organId, "getCardTypeForHis");
        List<String> cardList = new ArrayList<>();
        if (cardTypes == null || cardTypes.length == 0) {
            cardList.add("1");
            return cardList;
        }
        return Arrays.asList(cardTypes);
    }

    /**
     * 校验 his线下处方是否有更改
     *
     * @param hisRecipeTO
     */
    private void hisRecipeInfoCheck(List<QueryHisRecipResTO> hisRecipeTO, PatientDTO patientDTO) {
        LOGGER.info("hisRecipeInfoCheck hisRecipeTO = {}.", JSONUtils.toString(hisRecipeTO));
        if (CollectionUtils.isEmpty(hisRecipeTO)) {
            return;
        }
        Integer clinicOrgan = hisRecipeTO.get(0).getClinicOrgan();
        if (null == clinicOrgan) {
            LOGGER.info("hisRecipeInfoCheck his data error clinicOrgan is null");
            return;
        }
        List<String> recipeCodeList = hisRecipeTO.stream().map(QueryHisRecipResTO::getRecipeCode).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(recipeCodeList)) {
            LOGGER.info("hisRecipeInfoCheck his data error recipeCodeList is null");
            return;
        }
        //2 判断Recipe 是否有订单
        List<Recipe> recipeList = recipeDAO.findByRecipeCodeAndClinicOrgan(recipeCodeList, clinicOrgan);
        LOGGER.info("hisRecipeInfoCheck recipeList = {}", JSONUtils.toString(recipeList));

        if (CollectionUtils.isNotEmpty(recipeList)) {
            List<String> orderCodeList = recipeList.stream().filter(a -> StringUtils.isNotEmpty(a.getOrderCode())).map(Recipe::getOrderCode).distinct().collect(Collectors.toList());
            //3 判断 订单 是否支付
            if (CollectionUtils.isNotEmpty(orderCodeList)) {
                List<RecipeOrder> recipeOrderList = recipeOrderDAO.findByOrderCode(orderCodeList);
                LOGGER.info("hisRecipeInfoCheck recipeOrderList = {}", JSONUtils.toString(recipeOrderList));
                List<String> recipeOrderCode = recipeOrderList.stream().filter(a -> a.getPayFlag().equals(PayConstant.PAY_FLAG_PAY_SUCCESS)).map(RecipeOrder::getOrderCode).collect(Collectors.toList());
                List<String> recipeCodeExclude = recipeList.stream().filter(a -> recipeOrderCode.contains(a.getOrderCode())).map(Recipe::getRecipeCode).distinct().collect(Collectors.toList());
                //排除支付订单处方（找到非已支付的recipeCode）
                recipeCodeList = recipeCodeList.stream().filter(a -> !recipeCodeExclude.contains(a)).collect(Collectors.toList());
                //question TODO 比如his 返回已缴费的处方 ordercode就是空 这样吧his返回的已缴费的处方也过滤出来了


                LOGGER.info("hisRecipeInfoCheck recipeCodeList = {}", JSONUtils.toString(recipeCodeList));
                if (CollectionUtils.isEmpty(recipeCodeList)) {
                    return;
                }
            }
        }

        List<HisRecipe> hisRecipeList = hisRecipeDAO.findHisRecipeByRecipeCodeAndClinicOrgan(clinicOrgan, recipeCodeList);
        LOGGER.info("hisRecipeInfoCheck hisRecipeList = {}", JSONUtils.toString(hisRecipeList));
        if (CollectionUtils.isEmpty(hisRecipeList)) {
            return;
        }
        //判断 hisRecipe 诊断不一致 更新
        Map<String, HisRecipe> hisRecipeMap = updateHisRecipe(hisRecipeTO, recipeList, hisRecipeList);

        /**判断处方是否删除*/
        List<Integer> hisRecipeIds = hisRecipeList.stream().map(HisRecipe::getHisRecipeID).distinct().collect(Collectors.toList());
        List<HisRecipeDetail> hisRecipeDetailList = hisRecipeDetailDAO.findByHisRecipeIds(hisRecipeIds);
        LOGGER.info("hisRecipeInfoCheck hisRecipeDetailList = {}", JSONUtils.toString(hisRecipeDetailList));
        if (CollectionUtils.isEmpty(hisRecipeDetailList)) {
            return;
        }

        //1 判断是否delete 处方相关表 / RecipeDetailTO 数量 ，药品，开药总数
        Set<String> deleteSetRecipeCode = new HashSet<>();
        Map<Integer, List<HisRecipeDetail>> hisRecipeIdDetailMap = hisRecipeDetailList.stream().collect(Collectors.groupingBy(HisRecipeDetail::getHisRecipeId));
        hisRecipeTO.forEach(a -> {
            String recipeCode = a.getRecipeCode();
            HisRecipe hisRecipe = hisRecipeMap.get(recipeCode);
            if (null == hisRecipe) {
                return;
            } else {
                if (!hisRecipe.getMpiId().equals(patientDTO.getMpiId())) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause mpiid recipeCode:{}", recipeCode);
                    return;
                }
            }
            //已处理处方不应删除(现在因为其他用户绑定了该就诊人也要查询到数据，所以mpiid不一致，数据需要删除)
            if (2 == hisRecipe.getStatus()) {
                return;
            }
            List<HisRecipeDetail> hisDetailList = hisRecipeIdDetailMap.get(hisRecipe.getHisRecipeID());
            if (CollectionUtils.isEmpty(a.getDrugList()) || CollectionUtils.isEmpty(hisDetailList)) {
                deleteSetRecipeCode.add(recipeCode);
                LOGGER.info("deleteSetRecipeCode cause drugList empty recipeCode:{}", recipeCode);

                return;
            }
            if (a.getDrugList().size() != hisDetailList.size()) {
                deleteSetRecipeCode.add(recipeCode);
                LOGGER.info("deleteSetRecipeCode cause drugList size no equal recipeCode:{}", recipeCode);
                return;
            }
            Map<String, HisRecipeDetail> recipeDetailMap = hisDetailList.stream().collect(Collectors.toMap(HisRecipeDetail::getDrugCode, b -> b, (k1, k2) -> k1));
            for (RecipeDetailTO recipeDetailTO : a.getDrugList()) {
                HisRecipeDetail hisRecipeDetail = recipeDetailMap.get(recipeDetailTO.getDrugCode());
                LOGGER.info("recipeDetailTO:{},hisRecipeDetail:{}.", JSONUtils.toString(recipeDetailTO), JSONUtils.toString(hisRecipeDetail));
                if (null == hisRecipeDetail) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause hisRecipeDetail is null recipeCode:{}", recipeCode);
                    continue;
                }
                BigDecimal useTotalDose = hisRecipeDetail.getUseTotalDose();
                if (null == useTotalDose || 0 != useTotalDose.compareTo(recipeDetailTO.getUseTotalDose())) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useTotalDose recipeCode:{}", recipeCode);
                    continue;
                }
                String useDose = hisRecipeDetail.getUseDose();
                if ((StringUtils.isEmpty(useDose) && StringUtils.isNotEmpty(recipeDetailTO.getUseDose())) || (StringUtils.isNotEmpty(useDose) && !useDose.equals(recipeDetailTO.getUseDose()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDose recipeCode:{}", recipeCode);
                    continue;
                }
                String useDoseStr = hisRecipeDetail.getUseDoseStr();
                if ((StringUtils.isEmpty(useDoseStr) && StringUtils.isNotEmpty(recipeDetailTO.getUseDoseStr())) || (StringUtils.isNotEmpty(useDoseStr) && !useDoseStr.equals(recipeDetailTO.getUseDoseStr()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDoseStr recipeCode:{}", recipeCode);
                    continue;
                }
                String useDaysB = hisRecipeDetail.getUseDaysB();
                if ((StringUtils.isEmpty(useDaysB) && recipeDetailTO.getUseDaysB() != null) || (StringUtils.isNotEmpty(useDaysB) && !useDaysB.equals(recipeDetailTO.getUseDaysB()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDays recipeCode:{}", recipeCode);
                    continue;
                }
                String usingRate = hisRecipeDetail.getUsingRate();
                if ((StringUtils.isEmpty(usingRate) && StringUtils.isNotEmpty(recipeDetailTO.getUsingRate())) || (StringUtils.isNotEmpty(usingRate) && !usingRate.equals(recipeDetailTO.getUsingRate()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause usingRate recipeCode:{}", recipeCode);
                    continue;
                }

                String usingRateText = hisRecipeDetail.getUsingRateText();
                if ((StringUtils.isEmpty(usingRateText) && StringUtils.isNotEmpty(recipeDetailTO.getUsingRateText())) || (StringUtils.isNotEmpty(usingRateText) && !usingRateText.equals(recipeDetailTO.getUsingRateText()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause usingRateText recipeCode:{}", recipeCode);
                    continue;
                }
                String usePathways = hisRecipeDetail.getUsePathways();
                if ((StringUtils.isEmpty(usePathways) && StringUtils.isNotEmpty(recipeDetailTO.getUsePathWays())) || (StringUtils.isNotEmpty(usePathways) && !usePathways.equals(recipeDetailTO.getUsePathWays()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause usePathWays recipeCode:{}", recipeCode);
                    continue;
                }
                String usePathwaysText = hisRecipeDetail.getUsePathwaysText();
                if ((StringUtils.isEmpty(usePathwaysText) && StringUtils.isNotEmpty(recipeDetailTO.getUsePathwaysText())) || (StringUtils.isNotEmpty(usePathwaysText) && !usePathwaysText.equals(recipeDetailTO.getUsePathwaysText()))) {
                    LOGGER.info("deleteSetRecipeCode cause usePathwaysText recipeCode:{}", recipeCode);
                    deleteSetRecipeCode.add(recipeCode);
                }
            }
            //中药判断tcmFee发生变化,删除数据
            BigDecimal tcmFee = a.getTcmFee();
            if ((tcmFee != null && tcmFee.compareTo(hisRecipe.getTcmFee()) != 0) || (tcmFee == null && hisRecipe.getTcmFee() != null)) {
                LOGGER.info("deleteSetRecipeCode cause tcmFee recipeCode:{}", recipeCode);
                deleteSetRecipeCode.add(hisRecipe.getRecipeCode());
            }

        });

        //删除
        deleteSetRecipeCode(clinicOrgan, deleteSetRecipeCode);
    }

    /**
     * 删除线下处方相关数据
     *
     * @param clinicOrgan         机构id
     * @param deleteSetRecipeCode 要删除的
     */
    void deleteSetRecipeCode(Integer clinicOrgan, Set<String> deleteSetRecipeCode) {
        LOGGER.info("deleteSetRecipeCode clinicOrgan = {},deleteSetRecipeCode = {}", clinicOrgan, JSONUtils.toString(deleteSetRecipeCode));
        if (CollectionUtils.isEmpty(deleteSetRecipeCode)) {
            return;
        }
        List<String> recipeCodeList = new ArrayList<>(deleteSetRecipeCode);
        List<HisRecipe> hisRecipeList = hisRecipeDAO.findHisRecipeByRecipeCodeAndClinicOrgan(clinicOrgan, recipeCodeList);
        List<Integer> hisRecipeIds = hisRecipeList.stream().map(HisRecipe::getHisRecipeID).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hisRecipeIds)) {
            LOGGER.info("deleteSetRecipeCode 查找无处方");
            return;
        }
        hisRecipeExtDAO.deleteByHisRecipeIds(hisRecipeIds);
        hisRecipeDetailDAO.deleteByHisRecipeIds(hisRecipeIds);
        hisRecipeDAO.deleteByHisRecipeIds(hisRecipeIds);
        List<Recipe> recipeList = recipeDAO.findByRecipeCodeAndClinicOrgan(recipeCodeList, clinicOrgan);
        if (CollectionUtils.isEmpty(recipeList)) {
            return;
        }
        List<Integer> recipeIds = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        List<String> orderCodeList = recipeList.stream().filter(a -> StringUtils.isNotEmpty(a.getOrderCode())).map(Recipe::getOrderCode).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(orderCodeList)) {
            recipeOrderDAO.deleteByRecipeIds(orderCodeList);
        }
        recipeExtendDAO.deleteByRecipeIds(recipeIds);
        recipeDetailDAO.deleteByRecipeIds(recipeIds);
        //question TODO recipe 主表不删 修改具体哪些字段（除id外所有字段）
        recipeDAO.deleteByRecipeIds(recipeIds);
        LOGGER.info("deleteSetRecipeCode is delete end ");
    }

    /**
     * 更新诊断字段
     *
     * @param hisRecipeTO
     * @param recipeList
     * @param hisRecipeList
     */
    private Map<String, HisRecipe> updateHisRecipe(List<QueryHisRecipResTO> hisRecipeTO, List<Recipe> recipeList, List<HisRecipe> hisRecipeList) {
        Map<String, Recipe> recipeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getRecipeCode, a -> a, (k1, k2) -> k1));
        Map<String, HisRecipe> hisRecipeMap = hisRecipeList.stream().collect(Collectors.toMap(HisRecipe::getRecipeCode, a -> a, (k1, k2) -> k1));
        hisRecipeTO.forEach(a -> {
            HisRecipe hisRecipe = hisRecipeMap.get(a.getRecipeCode());
            if (null == hisRecipe) {
                return;
            }
            String disease = null != a.getDisease() ? a.getDisease() : "";
            String diseaseName = null != a.getDiseaseName() ? a.getDiseaseName() : "";
            if (!disease.equals(hisRecipe.getDisease()) || !diseaseName.equals(hisRecipe.getDiseaseName())) {
                hisRecipe.setDisease(disease);
                hisRecipe.setDiseaseName(diseaseName);
                hisRecipeDAO.update(hisRecipe);
                LOGGER.info("updateHisRecipe hisRecipe = {}", JSONUtils.toString(hisRecipe));
                Recipe recipe = recipeMap.get(a.getRecipeCode());
                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                RecipeBean recipeBean = new RecipeBean();
                BeanUtils.copy(recipe, recipeBean);
                recipeBean.setOrganDiseaseName(diseaseName);
                recipeBean.setOrganDiseaseId(disease);
                emrRecipeManager.updateMedicalInfo(recipeBean, recipeExtend);
                recipeExtendDAO.saveOrUpdateRecipeExtend(recipeExtend);
                recipe.setOrganDiseaseId(disease);
                recipe.setOrganDiseaseName(diseaseName);
                recipeDAO.update(recipe);
            }
        });
        return hisRecipeMap;
    }
}
