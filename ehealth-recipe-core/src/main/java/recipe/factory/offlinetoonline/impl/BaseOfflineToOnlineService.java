package recipe.factory.offlinetoonline.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.ExtInfoTO;
import com.ngari.his.recipe.mode.MedicalInfo;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.his.recipe.mode.RecipeDetailTO;
import com.ngari.patient.dto.*;
import com.ngari.patient.service.*;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.dto.GroupRecipeConf;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailReqVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailResVO;
import com.ngari.recipe.offlinetoonline.model.SettleForOfflineToOnlineVO;
import com.ngari.recipe.recipe.model.*;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import eh.base.constant.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.client.RevisitClient;
import recipe.dao.*;
import recipe.dao.bean.HisRecipeListBean;
import recipe.enumerate.status.OfflineToOnlineEnum;
import recipe.enumerate.status.RecipeSourceTypeEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.factory.offlinetoonline.IOfflineToOnlineStrategy;
import recipe.factory.offlinetoonline.OfflineToOnlineFactory;
import recipe.givemode.business.GiveModeFactory;
import recipe.givemode.business.IGiveModeBase;
import recipe.manager.EmrRecipeManager;
import recipe.manager.GroupRecipeManager;
import recipe.manager.HisRecipeManager;
import recipe.service.DrugsEnterpriseService;
import recipe.service.RecipeService;
import recipe.util.RecipeUtil;
import recipe.vo.patient.RecipeGiveModeButtonRes;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author 刘敏
 * @date 2021\6\30
 */
@Service
public class BaseOfflineToOnlineService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseOfflineToOnlineService.class);

    @Autowired
    private HisRecipeDAO hisRecipeDao;

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
    private GroupRecipeManager groupRecipeManager;

    @Autowired
    private DrugMakingMethodDao drugMakingMethodDao;

    @Autowired
    private SymptomDAO symptomDAO;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private HisRecipeManager hisRecipeManager;

    @Autowired
    private RevisitClient revisitClient;

    @Autowired
    private OfflineToOnlineFactory offlineToOnlineFactory;

    @Autowired
    private AppointDepartService appointDepartService;

    @Autowired
    private EmploymentService employmentService;

    final String BY_REGISTERID = "e.registerId";

    private static final Integer HISRECIPESTATUS_NOIDEAL = 1;

    private static final Integer HISRECIPESTATUS_ALREADYIDEAL = 2;


    /**
     * 获取购药按钮
     *
     * @param recipeIds
     * @return
     */
    public List<RecipeGiveModeButtonRes> getRecipeGiveModeButtonRes(List<Integer> recipeIds) {
        LOGGER.info("BaseOfflineToOnlineService getRecipeGiveModeButtonRes request = {}", JSONUtils.toString(recipeIds));
        List<RecipeGiveModeButtonRes> recipeGiveModeButtonRes = recipeService.getRecipeGiveModeButtonRes(recipeIds);
        if (CollectionUtils.isEmpty(recipeGiveModeButtonRes)) {
            throw new DAOException(609, "“抱歉，当前处方没有可支持的购药方式”");
        }
        LOGGER.info("BaseOfflineToOnlineService getRecipeGiveModeButtonRes response = {}", JSONUtils.toString(recipeGiveModeButtonRes));
        return recipeGiveModeButtonRes;
    }

    /**
     * @param request
     * @return
     * @Description 批量同步线下处方数据
     * @Author liumin
     */
    public List<Integer> batchSyncRecipeFromHis(SettleForOfflineToOnlineVO request) {
        LOGGER.info("BaseOfflineToOnlineService batchSyncRecipeFromHis request = {}", JSONUtils.toString(request));
        List<Integer> recipeIds = new ArrayList<>();
        // 1、删数据
        hisRecipeManager.deleteRecipeByRecipeCodes(request.getOrganId(), request.getRecipeCode());
        request.getRecipeCode().forEach(recipeCode -> {
            // 2、线下转线上
            FindHisRecipeDetailReqVO findHisRecipeDetailReqVO = getFindHisRecipeDetailParam(request.getMpiId(), recipeCode, request.getOrganId(), request.getCardId());
            IOfflineToOnlineStrategy iOfflineToOnlineStrategy = offlineToOnlineFactory.getFactoryService(OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getName());
            FindHisRecipeDetailResVO findHisRecipeDetailResVO = iOfflineToOnlineStrategy.findHisRecipeDetail(findHisRecipeDetailReqVO);
            if (findHisRecipeDetailResVO != null) {
                Map<String, Object> map = findHisRecipeDetailResVO.getPlatRecipeDetail();
                RecipeBean recipeBean = objectMapper.convertValue(map.get("recipe"), RecipeBean.class);
                if (null != recipeBean) {
                    recipeIds.add(recipeBean.getRecipeId());
                }
            }

        });
        LOGGER.info("batchSyncRecipeFromHis recipeIds:{}", JSONUtils.toString(recipeIds));
        //部分处方线下转线上成功
        if (recipeIds.size() != request.getRecipeCode().size()) {
            throw new DAOException(609, "抱歉，无法查找到对应的处方单数据");
        }
        //存在已失效处方
        List<Recipe> recipes = recipeDAO.findRecipeByRecipeIdAndClinicOrgan(Integer.parseInt(request.getOrganId()), recipeIds);
        if (CollectionUtils.isNotEmpty(recipes) && recipes.size() > 0) {
            LOGGER.info("batchSyncRecipeFromHis 存在已失效处方");
            throw new DAOException(600, "处方单过期已失效");
        }
        LOGGER.info("BaseOfflineToOnlineService batchSyncRecipeFromHis response = {}", JSONUtils.toString(recipeIds));
        return recipeIds;
    }

    /**
     * 获取一个获取详情的入参对象
     *
     * @param mpiId      就诊人mpiid
     * @param recipeCode 处方号
     * @param organId    机构id
     * @param cardId     卡号
     * @return
     */
    private FindHisRecipeDetailReqVO getFindHisRecipeDetailParam(String mpiId, String recipeCode, String organId, String cardId) {
        LOGGER.info("BaseOfflineToOnlineService getFindHisRecipeDetailParam mpiId:{},recipeCode:{},organId:{},cardId:{}", mpiId, recipeCode, organId, cardId);
        FindHisRecipeDetailReqVO findHisRecipeDetailReqVO;
        findHisRecipeDetailReqVO = FindHisRecipeDetailReqVO.builder()
                .mpiId(mpiId)
                .recipeCode(recipeCode)
                .organId(Integer.parseInt(organId))
                .cardId(cardId)
                .build();
        LOGGER.info("BaseOfflineToOnlineService getFindHisRecipeDetailParam res:{}", findHisRecipeDetailReqVO);
        return findHisRecipeDetailReqVO;
    }

    /**
     * 查询已处理处方列表
     *
     * @param organId            机构Id
     * @param mpiId              就诊人mpiid
     * @param giveModeButtonBean 按钮跳转位置
     * @param start              分页开始位置
     * @param limit              查询条数
     * @return
     */
    public List<MergeRecipeVO> findFinishHisRecipeList(Integer organId, String mpiId, GiveModeButtonBean giveModeButtonBean, Integer start, Integer limit) {
        LOGGER.info("BaseOfflineToOnlineService findFinishHisRecipeList mpiId:{} giveModeButtonBean : {} index:{} limit:{} ", mpiId, giveModeButtonBean, start, limit);
        List<MergeRecipeVO> result = new ArrayList<>();
        // 获取所有已处理的线下处方
        List<HisRecipeListBean> hisRecipeListBeans = hisRecipeDao.findHisRecipeListByMPIId(organId, mpiId, start, limit);
        if (CollectionUtils.isEmpty(hisRecipeListBeans)) {
            return result;
        }
        result = listShow(hisRecipeListBeans, organId, mpiId, giveModeButtonBean, start, limit);
        LOGGER.info("BaseOfflineToOnlineService findFinishHisRecipeList result:{} ", result);
        return result;
    }

    /**
     * 返回给前端所需数据
     *
     * @param hisRecipeListBeans
     * @param organId
     * @param mpiId
     * @param giveModeButtonBean
     * @param start
     * @param limit
     * @return
     */
    List<MergeRecipeVO> listShow(List<HisRecipeListBean> hisRecipeListBeans, Integer organId, String mpiId, GiveModeButtonBean giveModeButtonBean, Integer start, Integer limit) {
        LOGGER.info("BaseOfflineToOnlineService listShow hisRecipeListBeans:{},organId:{},mpiId:{},giveModeButtonBean:{}", JSONUtils.toString(hisRecipeListBeans), organId, mpiId, JSONUtils.toString(giveModeButtonBean));
        List<MergeRecipeVO> result = new ArrayList<>();
        Set<Integer> recipeIds = new HashSet<>();

        Map<String, List<HisRecipeListBean>> hisRecipeListBeanMap = hisRecipeListBeans.stream().filter(hisRecipeListBean -> hisRecipeListBean.getOrderCode() != null).collect(Collectors.groupingBy(HisRecipeListBean::getOrderCode));
        Map<Integer, List<Recipe>> recipeMap = getRecipeMap(hisRecipeListBeans);
        Map<String, List<RecipeOrder>> recipeOrderMap = getRecipeOrderMap(hisRecipeListBeanMap.keySet());

        // 获取合并处方显示配置项
        GroupRecipeConf groupRecipeConf = groupRecipeManager.getMergeRecipeSetting();
        String mergeRecipeWay = groupRecipeConf.getMergeRecipeWayAfter();

        hisRecipeListBeans.forEach(hisRecipeListBean -> {
            List<HisRecipeVO> hisRecipeVos = new ArrayList<>();
            if (!recipeIds.contains(hisRecipeListBean.getHisRecipeID())) {
                String orderCode = hisRecipeListBean.getOrderCode();
                String grpupField = "";
                if (BY_REGISTERID.equals(mergeRecipeWay)) {
                    // 挂号序号
                    grpupField = hisRecipeListBean.getRegisteredId();
                } else {
                    // 慢病名称
                    grpupField = hisRecipeListBean.getChronicDiseaseName();
                }

                if (StringUtils.isEmpty(orderCode)) {
                    HisRecipeVO hisRecipeVO = ObjectCopyUtils.convert(hisRecipeListBean, HisRecipeVO.class);
                    setOtherInfo(hisRecipeVO, mpiId, hisRecipeListBean.getRecipeCode(), organId);
                    recipeIds.add(hisRecipeVO.getHisRecipeID());
                    hisRecipeVos.add(hisRecipeVO);
                } else {
                    List<HisRecipeListBean> hisRecipeListBeansList = hisRecipeListBeanMap.get(orderCode);
                    List<RecipeOrder> recipeOrders = recipeOrderMap.get(orderCode);
                    RecipeOrder recipeOrder = null;
                    if (CollectionUtils.isNotEmpty(recipeOrders)) {
                        recipeOrder = recipeOrders.get(0);
                    }
                    hisRecipeVos = setPatientTabStatusMerge(recipeMap, recipeOrder, hisRecipeListBeansList, recipeIds);
                }
                covertMergeRecipeVO(grpupField, groupRecipeConf.getMergeRecipeFlag(), mergeRecipeWay, hisRecipeListBean.getHisRecipeID(), giveModeButtonBean.getButtonSkipType(), hisRecipeVos, result);
            }

        });
        LOGGER.info("BaseOfflineToOnlineService listShow result:{}", JSONUtils.toString(result));
        return result;
    }


    /**
     * 设置其它信息
     *
     * @param
     * @param recipeOrder
     * @param hisRecipeListBeans
     * @param recipeIds
     * @return
     */
    private List<HisRecipeVO> setPatientTabStatusMerge(Map<Integer, List<Recipe>> recipeMap, RecipeOrder recipeOrder, List<HisRecipeListBean> hisRecipeListBeans, Set<Integer> recipeIds) {
        LOGGER.info("BaseOfflineToOnlineService setPatientTabStatusMerge param recipeMap:{} ,recipeOrder:{} ,hisRecipeListBeans:{} ,recipeIds:{}", JSONUtils.toString(recipeMap), JSONUtils.toString(recipeOrder), JSONUtils.toString(hisRecipeListBeans), JSONUtils.toString(recipeIds));
        List<HisRecipeVO> hisRecipeVos = new ArrayList<>();
        hisRecipeListBeans.forEach(hisRecipeListBean -> {
            HisRecipeVO hisRecipeVO = ObjectCopyUtils.convert(hisRecipeListBean, HisRecipeVO.class);
            // 这个接口查询的所有处方都是线下处方 前端展示逻辑 0: 平台, 1: his
            hisRecipeVO.setFromFlag(1);
            // 有订单跳转订单
            hisRecipeVO.setJumpPageType(1);
            hisRecipeVO.setOrganDiseaseName(hisRecipeListBean.getDiseaseName());
            Recipe recipe = recipeMap.get(hisRecipeListBean.getRecipeId()).get(0);
            if (Objects.nonNull(recipeOrder)) {
                hisRecipeVO.setStatusText(RecipeUtil.getTipsByStatusForPatient(recipe, recipeOrder));
            }
            recipeIds.add(hisRecipeVO.getHisRecipeID());
            hisRecipeVos.add(hisRecipeVO);
        });
        LOGGER.info("BaseOfflineToOnlineService setPatientTabStatusMerge res:{}", JSONUtils.toString(hisRecipeVos));
        return hisRecipeVos;
    }


    /**
     * 转换成前端所需MergeRecipeVO对象
     *
     * @param grpupFiled
     * @param mergeRecipeFlag
     * @param mergeRecipeWay
     * @param firstRecipeId
     * @param listSkipType
     * @param recipes
     * @param result
     */
    protected void covertMergeRecipeVO(String grpupFiled, boolean mergeRecipeFlag, String mergeRecipeWay, Integer firstRecipeId, String listSkipType, List<HisRecipeVO> recipes, List<MergeRecipeVO> result) {
        LOGGER.info("BaseOfflineToOnlineService covertMergeRecipeVO param grpupFiled:{},mergeRecipeFlag:{},mergeRecipeWay:{},firstRecipeId:{},listSkipType:{},recipes:{},result:{}", grpupFiled, mergeRecipeFlag, mergeRecipeWay, firstRecipeId, listSkipType, JSONUtils.toString(recipes), JSONUtils.toString(result));
        if (mergeRecipeFlag) {
            MergeRecipeVO mergeRecipeVO = new MergeRecipeVO();
            mergeRecipeVO.setGroupField(grpupFiled);
            mergeRecipeVO.setMergeRecipeFlag(mergeRecipeFlag);
            mergeRecipeVO.setMergeRecipeWay(mergeRecipeWay);
            mergeRecipeVO.setRecipe(recipes);
            mergeRecipeVO.setFirstRecipeId(firstRecipeId);
            mergeRecipeVO.setListSkipType(listSkipType);
            result.add(mergeRecipeVO);
        } else {
            for (HisRecipeVO hisRecipeVO : recipes) {
                MergeRecipeVO mergeRecipeVO = new MergeRecipeVO();
                mergeRecipeVO.setMergeRecipeFlag(mergeRecipeFlag);
                mergeRecipeVO.setRecipe(Arrays.asList(hisRecipeVO));
                mergeRecipeVO.setListSkipType(listSkipType);
                result.add(mergeRecipeVO);
            }
        }
        LOGGER.info("BaseOfflineToOnlineService covertMergeRecipeVO response result:{}", JSONUtils.toString(result));
    }


    private Map<Integer, List<Recipe>> getRecipeMap(List<HisRecipeListBean> hisRecipeListByMpiIds) {
        Set<Integer> recipes = hisRecipeListByMpiIds.stream().filter(hisRecipeListBean -> hisRecipeListBean.getRecipeId() != null).collect(Collectors.groupingBy(HisRecipeListBean::getRecipeId)).keySet();
        List<Recipe> byRecipes = recipeDAO.findByRecipeIds(recipes);
        Map<Integer, List<Recipe>> collect = null;
        if (CollectionUtils.isNotEmpty(byRecipes)) {
            collect = byRecipes.stream().collect(Collectors.groupingBy(Recipe::getRecipeId));
        }
        return collect;
    }

    private Map<String, List<RecipeOrder>> getRecipeOrderMap(Set<String> orderCodes) {
        Map<String, List<RecipeOrder>> collect1 = null;
        if (CollectionUtils.isNotEmpty(orderCodes)) {
            List<RecipeOrder> byOrderCode = recipeOrderDAO.findByOrderCode(orderCodes);
            if (CollectionUtils.isNotEmpty(byOrderCode)) {
                collect1 = byOrderCode.stream().collect(Collectors.groupingBy(RecipeOrder::getOrderCode));
            }
        }
        return collect1;
    }

    /**
     * 设置文案显示、处方来源、跳转页面
     *
     * @param hisRecipeVO 返回对象
     * @param mpiId       mpiid
     * @param recipeCode  recipeCode
     * @param clinicOrgan clinicOrgan
     */
    void setOtherInfo(HisRecipeVO hisRecipeVO, String mpiId, String recipeCode, Integer clinicOrgan) {
        Recipe recipe = recipeDAO.getByHisRecipeCodeAndClinicOrganAndMpiid(mpiId, recipeCode, clinicOrgan);
        if (recipe == null) {
            hisRecipeVO.setStatusText("待处理");
            hisRecipeVO.setFromFlag(1);
            hisRecipeVO.setJumpPageType(0);
        } else {
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
            if (RecipeSourceTypeEnum.OFFLINE_RECIPE.getType().equals(recipe.getRecipeSourceType())) {
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
                        //跳转到订单详情页
                        hisRecipeVO.setJumpPageType(1);
                        hisRecipeVO.setStatusText(RecipeUtil.getTipsByStatusForPatient(recipe, recipeOrder));
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
            }
        }
        LOGGER.info("BaseOfflineToOnlineService  setOtherInfo hisRecipeVO:{}", JSONUtils.toString(hisRecipeVO));
    }

    /**
     * 获取机构配置够药方式
     *
     * @param organId 机构id
     * @return
     */
    public GiveModeButtonBean getGiveModeButtonBean(Integer organId) {
        LOGGER.info("BaseOfflineToOnlineService getGiveModeButtonBean param organId:{}", organId);
        IGiveModeBase giveModeBase = GiveModeFactory.getGiveModeBaseByRecipe(new Recipe());
        //获取机构配制的购药按钮
        GiveModeShowButtonVO giveModeShowButtons = giveModeBase.getGiveModeSettingFromYypt(organId);
        GiveModeButtonBean res = giveModeShowButtons.getListItem();
        LOGGER.info("BaseOfflineToOnlineService getGiveModeButtonBean res :{}", JSONUtils.toString(res));
        return res;
    }

    /**
     * @param hisRecipeId cdr_his_recipe表主键
     * @param recipeId    cdr_recipe表主键
     * @return
     * @author liumin
     * @Description 通过hisRecipeId和recipeId查询并返回前端所需数据
     */
    public FindHisRecipeDetailResVO getHisRecipeDetailByHisRecipeIdAndRecipeId(Integer hisRecipeId, Integer recipeId) {
        LOGGER.info("BaseOfflineToOnlineService getHisRecipeDetailByHisRecipeIdAndRecipeId param hisRecipeId:{},recipeId:{}", hisRecipeId, recipeId);
        if (hisRecipeId == null || recipeId == null) {
            throw new DAOException(DAOException.DAO_NOT_FOUND, "没有查询到来自医院的处方单,请刷新页面！");
        }
        FindHisRecipeDetailResVO findHisRecipeDetailResVO = new FindHisRecipeDetailResVO();
        Map<String, Object> recipeDetailMap;
        Recipe recipe = recipeDAO.get(recipeId);
        HisRecipe hisRecipe = hisRecipeDao.get(hisRecipeId);
        List<HisRecipeExt> hisRecipeExts = hisRecipeExtDAO.findByHisRecipeId(hisRecipeId);
        if (recipe == null) {
            throw new DAOException(DAOException.DAO_NOT_FOUND, "没有查询到来自医院的处方单,请刷新页面！");
        }
        if (HISRECIPESTATUS_ALREADYIDEAL.equals(hisRecipe.getStatus())) {
            recipeDetailMap = recipeService.getPatientRecipeByIdForOfflineRecipe(recipeId);
        } else {
            recipeDetailMap = recipeService.getPatientRecipeById(recipeId);
        }
        findHisRecipeDetailResVO.setPlatRecipeDetail(recipeDetailMap);
        List<com.ngari.recipe.offlinetoonline.model.HisRecipeExt> hisRecipeExtsTemp = new ArrayList<com.ngari.recipe.offlinetoonline.model.HisRecipeExt>();
        BeanUtils.copyProperties(hisRecipeExts, hisRecipeExtsTemp);
        findHisRecipeDetailResVO.setHisRecipeExts(hisRecipeExtsTemp);
        findHisRecipeDetailResVO.setShowText(hisRecipe.getShowText());
        LOGGER.info("BaseOfflineToOnlineService getHisRecipeDetailByHisRecipeId response:{}", JSONUtils.toString(recipeDetailMap));
        return findHisRecipeDetailResVO;
    }

    /**
     * 将线下处方转化成线上处方，保存线下处方信息到平台
     *
     * @param hisRecipeId cdr_his_recipe表主键
     * @return
     */
    public Integer saveRecipeInfo(Integer hisRecipeId) {
        LOGGER.info("BaseOfflineToOnlineService saveRecipeInfo param hisRecipeId:{}", hisRecipeId);
        if (hisRecipeId == null) {
            throw new DAOException(DAOException.DAO_NOT_FOUND, "没有查询到来自医院的处方单,请刷新页面！");
        }
        HisRecipe hisRecipe = hisRecipeDao.get(hisRecipeId);
        if (hisRecipe == null) {
            throw new DAOException(DAOException.DAO_NOT_FOUND, "没有查询到来自医院的处方单");
        }
        Recipe recipe = saveRecipeFromHisRecipe(hisRecipe);
        if (recipe != null) {
            // 线下转线上失效时间处理--仅平台线下转线上需处理（目前互联网环境没有线下转线上，不判断平台还是互联网）
            RecipeService.handleRecipeInvalidTime(recipe.getClinicOrgan(), recipe.getRecipeId(), recipe.getSignDate());
            saveRecipeExt(recipe, hisRecipe);
            savaRecipeDetail(recipe.getRecipeId(), hisRecipe);
            //购药按钮
            List<Integer> drugsEnterpriseContinue = drugsEnterpriseService.getDrugsEnterpriseContinue(recipe.getRecipeId(), recipe.getClinicOrgan());
            LOGGER.info("getHisRecipeDetailByHisRecipeId recipeId = {} drugsEnterpriseContinue = {}", recipe.getRecipeId(), JSONUtils.toString(drugsEnterpriseContinue));
            if (CollectionUtils.isNotEmpty(drugsEnterpriseContinue)) {
                String join = StringUtils.join(drugsEnterpriseContinue, ",");
                recipe.setRecipeSupportGiveMode(join);
            }
            recipeDAO.saveOrUpdate(recipe);
            LOGGER.info("BaseOfflineToOnlineService saveRecipeInfo res:{}", recipe.getRecipeId());
            return recipe.getRecipeId();
        }
        return null;
    }

    /**
     * 保存线下处方到Recipe表
     * 如果已经支付直接返回，如果未支付，存在该对象则修改，否则新增
     *
     * @param hisRecipe 线下处方对象
     * @return
     */
    private Recipe saveRecipeFromHisRecipe(HisRecipe hisRecipe) {
        LOGGER.info("BaseOfflineToOnlineService saveRecipeFromHisRecipe param hisRecipe:{}.", JSONUtils.toString(hisRecipe));
        Recipe recipeDb = recipeDAO.getByHisRecipeCodeAndClinicOrgan(hisRecipe.getRecipeCode(), hisRecipe.getClinicOrgan());
        LOGGER.info("saveRecipeFromHisRecipe recipeDb:{}.", JSONUtils.toString(recipeDb));
        UserRoleToken userRoleToken = UserRoleToken.getCurrent();
        if (recipeDb != null && !RecipeUtil.isAllowDeleteByPayFlag(recipeDb.getPayFlag())) {
            //已支付状态下的处方不允许修改
            return recipeDb;
        }
        Recipe recipe = new Recipe();
        if (recipeDb != null) {
            recipe = ObjectCopyUtils.convert(recipeDb, Recipe.class);
//            recipe.setRecipeId(recipeDb.getRecipeId());
//            recipe.setOrderCode(recipeDb.getOrderCode());
        }
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
        AppointDepartDTO appointDepartDTO = appointDepartService.getByOrganIDAndAppointDepartCode(hisRecipe.getClinicOrgan(), hisRecipe.getDepartCode());
        if (appointDepartDTO != null) {
            recipe.setDepart(appointDepartDTO.getDepartId());
        } else {
            LOGGER.info("BaseOfflineToOnlineService saveRecipeFromHisRecipe 无法查询到挂号科室:{}.", hisRecipe.getDepartCode());
            throw new DAOException(ErrorCode.SERVICE_ERROR, "挂号科室维护错误");
        }
        if (StringUtils.isNotEmpty(hisRecipe.getDoctorCode())) {
            EmploymentDTO employmentDTO = employmentService.getEmploymentByJobNumberAndOrganId(hisRecipe.getDoctorCode(), hisRecipe.getClinicOrgan());
            if (employmentDTO != null && employmentDTO.getDoctorId() != null) {
                recipe.setDoctor(employmentDTO.getDoctorId());
            } else {
                LOGGER.error("请确认医院的医生工号和纳里维护的是否一致:" + hisRecipe.getDoctorCode());
                throw new DAOException(ErrorCode.SERVICE_ERROR, "医生工号维护错误");
            }
        }

        if (StringUtils.isNotEmpty(hisRecipe.getCheckerCode())) {
            EmploymentDTO employmentDTO = employmentService.getEmploymentByJobNumberAndOrganId(hisRecipe.getCheckerCode(), hisRecipe.getClinicOrgan());
            if (employmentDTO != null && employmentDTO.getDoctorId() != null) {
                recipe.setChecker(employmentDTO.getDoctorId());
                recipe.setCheckerText(hisRecipe.getCheckerName());
            } else {
                LOGGER.error("请确认医院的药师工号和纳里维护的是否一致:" + hisRecipe.getDoctorCode());
                throw new DAOException(ErrorCode.SERVICE_ERROR, "药师工号维护错误");
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
        if (HISRECIPESTATUS_ALREADYIDEAL.equals(hisRecipe.getStatus())) {
            recipe.setPayFlag(1);
            //已完成
            recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_FINISH.getType());
        } else {
            recipe.setPayFlag(0);
            //待处理
            recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType());
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
        recipe = recipeDAO.saveOrUpdate(recipe);

        LOGGER.info("BaseOfflineToOnlineService saveRecipeFromHisRecipe res:{}", JSONUtils.toString(recipe));
        return recipe;
    }

    /**
     * 保存数据到平台处方详情表
     *
     * @param recipeId  处方号
     * @param hisRecipe 线下处方对象
     */
    private void savaRecipeDetail(Integer recipeId, HisRecipe hisRecipe) {
        LOGGER.info("BaseOfflineToOnlineService savaRecipeDetail param recipeId:{},hisRecipe:{}", recipeId, JSONUtils.toString(hisRecipe));
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
                    //高优先级
                    recipedetail.setUseDose(Double.valueOf(hisRecipeDetail.getUseDose()));
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
                    //高优先级
                    recipedetail.setUseDoseStr(hisRecipeDetail.getUseDoseStr() + hisRecipeDetail.getUseDoseUnit());
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
            if (StringUtils.isNotEmpty(hisRecipeDetail.getUseDaysB())) {
                recipedetail.setUseDaysB(new BigDecimal(hisRecipeDetail.getUseDaysB()).setScale(0, BigDecimal.ROUND_UP).toString());
            }
            recipedetail.setStatus(1);

            //单药品总价使用线下传过来的，传过来多少就是多少我们不计算
            if (hisRecipeDetail.getTotalPrice() != null) {
                recipedetail.setDrugCost(hisRecipeDetail.getTotalPrice());
            }
            //特殊煎法、备注
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
            recipedetail.setTcmContraindicationCause(hisRecipeDetail.getTcmContraindicationCause());
            recipedetail.setTcmContraindicationType(hisRecipeDetail.getTcmContraindicationType());
            recipeDetailDAO.save(recipedetail);
            LOGGER.info("BaseOfflineToOnlineService savaRecipeDetail 已经保存的recipeId:{},recipeDetail:{}", recipeId, JSONUtils.toString(recipedetail));
        }
    }

    /**
     * 保存线下处方到cdr_recipe_ext
     *
     * @param recipe    处方
     * @param hisRecipe 线下处方
     */
    private void saveRecipeExt(Recipe recipe, HisRecipe hisRecipe) {
        LOGGER.info("BaseOfflineToOnlineService saveRecipeExt recipe:{},hisRecipe:{}", JSONUtils.toString(recipe), JSONUtils.toString(hisRecipe));
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
        } else {
            if (StringUtils.isNotEmpty(hisRecipe.getDecoctionCode())) {
                DrugDecoctionWayDao drugDecoctionWayDao = DAOFactory.getDAO(DrugDecoctionWayDao.class);
                DecoctionWay decoctionWay = drugDecoctionWayDao.getDecoctionWayByOrganIdAndCode(recipe.getClinicOrgan(), hisRecipe.getDecoctionCode());
                if (decoctionWay != null) {
                    recipeExtend.setDecoctionText(decoctionWay.getDecoctionText());
                }
            }
        }

        DrugMakingMethod drugMakingMethod = drugMakingMethodDao.getDrugMakingMethodByOrganIdAndCode(recipe.getClinicOrgan(), hisRecipe.getMakeMethodCode());
        if (drugMakingMethod != null) {
            recipeExtend.setMakeMethodId(drugMakingMethod.getMethodId().toString());
        }
        if (StringUtils.isNotEmpty(hisRecipe.getMakeMethodText())) {
            recipeExtend.setMakeMethodText(hisRecipe.getMakeMethodText());
        } else {
            if (drugMakingMethod != null) {
                recipeExtend.setMakeMethodText(drugMakingMethod.getMethodText());
            }
        }
        recipeExtend.setJuice(hisRecipe.getJuice());
        recipeExtend.setJuiceUnit(hisRecipe.getJuiceUnit());
        recipeExtend.setMinor(hisRecipe.getMinor());
        recipeExtend.setMinorUnit(hisRecipe.getMinorUnit());
        Symptom symptom = symptomDAO.getByOrganIdAndSymptomCode(recipe.getClinicOrgan(), hisRecipe.getSymptomCode());
        if (symptom != null) {
            recipeExtend.setSymptomId(symptom.getSymptomId().toString());
        }
        if (StringUtils.isNotEmpty(hisRecipe.getSymptomName())) {
            recipeExtend.setSymptomName(hisRecipe.getSymptomName());
        } else {
            if (symptom != null) {
                recipeExtend.setSymptomName(recipeExtend.getSymptomName());
            }
        }

        RevisitExDTO consultExDTO = new RevisitExDTO();
        try {
            consultExDTO = revisitClient.getByRegisterId(hisRecipe.getRegisteredId());
        } catch (Exception e) {
            LOGGER.error("线下处方转线上通过挂号序号关联复诊 error", e);
        }
        if (StringUtils.isNotEmpty(hisRecipe.getCardNo())) {
            recipeExtend.setCardNo(hisRecipe.getCardNo());
        } else {
            if (consultExDTO != null) {
                recipeExtend.setCardNo(consultExDTO.getCardId());
            }
        }
        if (StringUtils.isNotEmpty(hisRecipe.getCardTypeName())) {
            recipeExtend.setCardTypeName(hisRecipe.getCardTypeName());
        }
        if (StringUtils.isNotEmpty(hisRecipe.getCardTypeName())) {
            recipeExtend.setCardType(hisRecipe.getCardTypeCode());
        } else {
            if (consultExDTO != null) {
                recipeExtend.setCardType(consultExDTO.getCardType());
            }
        }
        recipeExtend.setRecipeCostNumber(hisRecipe.getRecipeCostNumber());
        emrRecipeManager.saveMedicalInfo(recipe, recipeExtend);
        recipeExtendDAO.save(recipeExtend);
        LOGGER.info("BaseOfflineToOnlineService saveRecipeExt 拓展表数据已保存");
    }

    public List<HisRecipeListBean> findOngoingHisRecipeListByMpiId(Integer clinicOrgan, String mpiId, Integer start, Integer limit) {
        return hisRecipeDao.findOngoingHisRecipeListByMPIId(clinicOrgan, mpiId, start, limit);
    }

    /**
     * 保存线下处方数据到cdr_his_recipe、HisRecipeDetail、HisRecipeExt
     *
     * @param responseTo
     * @param patientDTO
     * @return
     */
    public List<HisRecipe> saveHisRecipeInfo(HisResponseTO<List<QueryHisRecipResTO>> responseTo, PatientDTO patientDTO, Integer flag) {
        LOGGER.info("BaseOfflineToOnlineService saveHisRecipeInfo param responseTO:{},patientDTO:{}", JSONUtils.toString(responseTo), JSONUtils.toString(patientDTO));
        List<HisRecipe> hisRecipes = new ArrayList<>();
        if (responseTo == null) {
            return hisRecipes;
        }
        List<QueryHisRecipResTO> queryHisRecipResToList = responseTo.getData();

        if (CollectionUtils.isEmpty(queryHisRecipResToList)) {
            return hisRecipes;
        }
        LOGGER.info("saveHisRecipeInfo queryHisRecipResTOList:" + JSONUtils.toString(queryHisRecipResToList));
        for (QueryHisRecipResTO queryHisRecipResTo : queryHisRecipResToList) {
            HisRecipe hisRecipe1 = hisRecipeDao.getHisRecipeByRecipeCodeAndClinicOrgan(queryHisRecipResTo.getClinicOrgan(), queryHisRecipResTo.getRecipeCode());
            //数据库不存在处方信息，则新增
            if (null == hisRecipe1) {
                HisRecipe hisRecipe = covertHisRecipeObject(patientDTO, queryHisRecipResTo);
                try {
                    hisRecipe = hisRecipeDao.save(hisRecipe);
                    LOGGER.info("saveHisRecipeInfo hisRecipe:{} 当前时间：{}", hisRecipe, System.currentTimeMillis());
                    hisRecipes.add(hisRecipe);
                } catch (Exception e) {
                    LOGGER.error("hisRecipeDAO.save error ", e);
                    return hisRecipes;
                }

                if (null != queryHisRecipResTo.getExt()) {
                    for (ExtInfoTO extInfoTo : queryHisRecipResTo.getExt()) {
                        HisRecipeExt ext = ObjectCopyUtils.convert(extInfoTo, HisRecipeExt.class);
                        ext.setHisRecipeId(hisRecipe.getHisRecipeID());
                        hisRecipeExtDAO.save(ext);
                    }
                }

                if (null != queryHisRecipResTo.getDrugList()) {
                    for (RecipeDetailTO recipeDetailTo : queryHisRecipResTo.getDrugList()) {
                        HisRecipeDetail detail = covertHisRecipeDetailObject(hisRecipe, recipeDetailTo);
                        hisRecipeDetailDAO.save(detail);
                    }
                }
            } else {
                hisRecipes.add(hisRecipe1);
            }
        }
        LOGGER.info("BaseOfflineToOnlineService saveHisRecipeInfo hisRecipes:{}", JSONUtils.toString(hisRecipes));
        return hisRecipes;
    }

    /**
     * 获取一个hisRecippeDetail对象
     *
     * @param hisRecipe
     * @param recipeDetailTo
     * @return
     */
    private HisRecipeDetail covertHisRecipeDetailObject(HisRecipe hisRecipe, RecipeDetailTO recipeDetailTo) {
        LOGGER.info("BaseOfflineToOnlineService covertHisRecipeDetailObject param hisRecipe:{},recipeDetailTO:{}", JSONUtils.toString(hisRecipe), JSONUtils.toString(recipeDetailTo));
        HisRecipeDetail detail = ObjectCopyUtils.convert(recipeDetailTo, HisRecipeDetail.class);
        detail.setHisRecipeId(hisRecipe.getHisRecipeID());
        detail.setRecipeDeatilCode(recipeDetailTo.getRecipeDeatilCode());
        detail.setDrugName(recipeDetailTo.getDrugName());
        detail.setPrice(recipeDetailTo.getPrice());
        detail.setTotalPrice(recipeDetailTo.getTotalPrice());
        detail.setUsingRate(recipeDetailTo.getUsingRate());
        detail.setUsePathways(recipeDetailTo.getUsePathWays());
        detail.setDrugSpec(recipeDetailTo.getDrugSpec());
        detail.setDrugUnit(recipeDetailTo.getDrugUnit());
        detail.setUseDays(recipeDetailTo.getUseDays());
        detail.setUseDaysB(recipeDetailTo.getUseDaysB());
        detail.setDrugCode(recipeDetailTo.getDrugCode());
        detail.setUsingRateText(recipeDetailTo.getUsingRateText());
        detail.setUsePathwaysText(recipeDetailTo.getUsePathwaysText());
        //  线下特殊用法
        detail.setUseDoseStr(recipeDetailTo.getUseDoseStr());
        detail.setUseDose(recipeDetailTo.getUseDose());
        detail.setUseDoseUnit(recipeDetailTo.getUseDoseUnit());
        detail.setSaleName(recipeDetailTo.getSaleName());
        detail.setPack(recipeDetailTo.getPack());
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        if (StringUtils.isNotEmpty(detail.getRecipeDeatilCode())) {
            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(hisRecipe.getClinicOrgan(), Arrays.asList(detail.getDrugCode()));
            if (CollectionUtils.isEmpty(organDrugLists)) {
                LOGGER.info("saveHisRecipeInfo organDrugLists his传过来的药品编码没有在对应机构维护,organId:" + hisRecipe.getClinicOrgan() + ",organDrugCode:" + detail.getDrugCode());
            }
        }
        detail.setStatus(1);
        //西药医嘱
        detail.setMemo(recipeDetailTo.getMemo());
        //药房信息
        detail.setPharmacyCode(recipeDetailTo.getPharmacyCode());
        detail.setPharmacyName(recipeDetailTo.getPharmacyName());
        detail.setPharmacyCategray(recipeDetailTo.getPharmacyCategray());
        detail.setTcmContraindicationCause(recipeDetailTo.getTcmContraindicationCause());
        detail.setTcmContraindicationType(recipeDetailTo.getTcmContraindicationType());
        LOGGER.info("BaseOfflineToOnlineService covertHisRecipeDetailObject res:{}", JSONUtils.toString(detail));
        return detail;
    }

    /**
     * 获取一个hisRecippe对象
     *
     * @param patientDTO
     * @param queryHisRecipResTo
     * @return
     */
    private HisRecipe covertHisRecipeObject(PatientDTO patientDTO, QueryHisRecipResTO queryHisRecipResTo) {
        LOGGER.info("BaseOfflineToOnlineService covertHisRecipeObject param patientDTO:{},queryHisRecipResTO:{}", JSONUtils.toString(patientDTO), JSONUtils.toString(queryHisRecipResTo));
        HisRecipe hisRecipe = new HisRecipe();
        hisRecipe.setCertificate(patientDTO.getCertificate());
        hisRecipe.setCertificateType(patientDTO.getCertificateType());
        hisRecipe.setMpiId(patientDTO.getMpiId());
        hisRecipe.setPatientName(patientDTO.getPatientName());
        hisRecipe.setPatientAddress(patientDTO.getAddress());
        hisRecipe.setPatientNumber(queryHisRecipResTo.getPatientNumber());
        hisRecipe.setPatientTel(patientDTO.getMobile());
        hisRecipe.setRegisteredId(StringUtils.isNotEmpty(queryHisRecipResTo.getRegisteredId()) ? queryHisRecipResTo.getRegisteredId() : "");
        hisRecipe.setChronicDiseaseCode(StringUtils.isNotEmpty(queryHisRecipResTo.getChronicDiseaseCode()) ? queryHisRecipResTo.getChronicDiseaseCode() : "");
        hisRecipe.setChronicDiseaseName(StringUtils.isNotEmpty(queryHisRecipResTo.getChronicDiseaseName()) ? queryHisRecipResTo.getChronicDiseaseName() : "");
        hisRecipe.setRecipeCode(queryHisRecipResTo.getRecipeCode());
        hisRecipe.setDepartCode(queryHisRecipResTo.getDepartCode());
        hisRecipe.setDepartName(queryHisRecipResTo.getDepartName());
        hisRecipe.setDoctorName(queryHisRecipResTo.getDoctorName());
        hisRecipe.setCreateDate(queryHisRecipResTo.getCreateDate());
        if (queryHisRecipResTo.getTcmNum() != null) {
            hisRecipe.setTcmNum(queryHisRecipResTo.getTcmNum().toString());
        }
        hisRecipe.setStatus(queryHisRecipResTo.getStatus());
        if (new Integer(2).equals(queryHisRecipResTo.getMedicalType())) {
            //医保类型
            hisRecipe.setMedicalType(queryHisRecipResTo.getMedicalType());
        } else {
            //默认自费
            hisRecipe.setMedicalType(1);
        }
        hisRecipe.setRecipeFee(queryHisRecipResTo.getRecipeFee());
        hisRecipe.setRecipeType(queryHisRecipResTo.getRecipeType());
        hisRecipe.setClinicOrgan(queryHisRecipResTo.getClinicOrgan());
        hisRecipe.setCreateTime(new Date());
        hisRecipe.setExtensionFlag(1);
        if (queryHisRecipResTo.getExtensionFlag() == null) {
            //设置外延处方的标志
            hisRecipe.setRecipePayType(0);
        } else {
            //设置外延处方的标志
            hisRecipe.setRecipePayType(queryHisRecipResTo.getExtensionFlag());
        }
        if (!StringUtils.isEmpty(queryHisRecipResTo.getDiseaseName())) {
            hisRecipe.setDiseaseName(queryHisRecipResTo.getDiseaseName());
        } else {
            hisRecipe.setDiseaseName("无");
        }
        hisRecipe.setDisease(queryHisRecipResTo.getDisease());
        if (!StringUtils.isEmpty(queryHisRecipResTo.getDoctorCode())) {
            hisRecipe.setDoctorCode(queryHisRecipResTo.getDoctorCode());
        }
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organDTO = organService.getByOrganId(queryHisRecipResTo.getClinicOrgan());
        if (null != organDTO) {
            hisRecipe.setOrganName(organDTO.getName());
        }
        setMedicalInfo(queryHisRecipResTo, hisRecipe);
        hisRecipe.setGiveMode(queryHisRecipResTo.getGiveMode());
        hisRecipe.setDeliveryCode(queryHisRecipResTo.getDeliveryCode());
        hisRecipe.setDeliveryName(queryHisRecipResTo.getDeliveryName());
        hisRecipe.setSendAddr(queryHisRecipResTo.getSendAddr());
        hisRecipe.setRecipeSource(queryHisRecipResTo.getRecipeSource());
        hisRecipe.setReceiverName(queryHisRecipResTo.getReceiverName());
        hisRecipe.setReceiverTel(queryHisRecipResTo.getReceiverTel());
        //中药
        hisRecipe.setRecipeCostNumber(queryHisRecipResTo.getRecipeCostNumber());
        hisRecipe.setTcmFee(queryHisRecipResTo.getTcmFee());
        hisRecipe.setDecoctionFee(queryHisRecipResTo.getDecoctionFee());
        hisRecipe.setDecoctionCode(queryHisRecipResTo.getDecoctionCode());
        hisRecipe.setDecoctionText(queryHisRecipResTo.getDecoctionText());
        hisRecipe.setDecoctionUnitFee(queryHisRecipResTo.getDecoctionUnitFee());
        hisRecipe.setTcmNum(queryHisRecipResTo.getTcmNum() == null ? null : String.valueOf(queryHisRecipResTo.getTcmNum()));
        //中药医嘱跟着处方 西药医嘱跟着药品（见药品详情）
        hisRecipe.setRecipeMemo(queryHisRecipResTo.getRecipeMemo());
        hisRecipe.setMakeMethodCode(queryHisRecipResTo.getMakeMethodCode());
        hisRecipe.setMakeMethodText(queryHisRecipResTo.getMakeMethodText());
        hisRecipe.setJuice(queryHisRecipResTo.getJuice());
        hisRecipe.setJuiceUnit(queryHisRecipResTo.getJuiceUnit());
        hisRecipe.setMinor(queryHisRecipResTo.getMinor());
        hisRecipe.setMinorUnit(queryHisRecipResTo.getMinorUnit());
        hisRecipe.setSymptomCode(queryHisRecipResTo.getSymptomCode());
        hisRecipe.setSymptomName(queryHisRecipResTo.getSymptomName());
        hisRecipe.setCardNo(queryHisRecipResTo.getCardNo());
        hisRecipe.setCardTypeCode(queryHisRecipResTo.getCardTypeCode());
        hisRecipe.setCardTypeName(queryHisRecipResTo.getCardTypeName());
        //审核药师
        hisRecipe.setCheckerCode(queryHisRecipResTo.getCheckerCode());
        hisRecipe.setCheckerName(queryHisRecipResTo.getCheckerName());
        LOGGER.info("BaseOfflineToOnlineService covertHisRecipeObject res hisRecipe:{}", JSONUtils.toString(hisRecipe));
        return hisRecipe;
    }

    /**
     * 设置医保信息
     *
     * @param queryHisRecipResTo his处方数据
     * @param hisRecipe          返回对象
     */
    private void setMedicalInfo(QueryHisRecipResTO queryHisRecipResTo, HisRecipe hisRecipe) {
        LOGGER.info("BaseOfflineToOnlineService setMedicalInfo param queryHisRecipResTO:{},hisRecipe:{}", JSONUtils.toString(queryHisRecipResTo), JSONUtils.toString(hisRecipe));
        if (null != queryHisRecipResTo.getMedicalInfo()) {
            MedicalInfo medicalInfo = queryHisRecipResTo.getMedicalInfo();
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
        LOGGER.info("BaseOfflineToOnlineService setMedicalInfo response hisRecipe:{} ", JSONUtils.toString(hisRecipe));
    }

    /**
     * 药品详情发生变化、数据不是由本人生成的未支付处方 数据处理
     *
     * @param hisRecipeTo         his处方数据
     * @param hisRecipeMap        key为未处理recipeCode,值为未处理HisRecipe的map对象
     * @param hisRecipeDetailList 未处理的线下处方详情
     * @param mpiId               查看详情处方的操作用户的mpiid
     */
    private void deleteRecipeData(List<QueryHisRecipResTO> hisRecipeTo, Map<String, HisRecipe> hisRecipeMap, List<HisRecipeDetail> hisRecipeDetailList, String mpiId) {
        if (CollectionUtils.isEmpty(hisRecipeDetailList)) {
            return;
        }
        Set<String> deleteSetRecipeCode = hisRecipeManager.obtainDeleteRecipeCodes(hisRecipeTo, hisRecipeMap, hisRecipeDetailList, mpiId);
        hisRecipeManager.deleteSetRecipeCode(hisRecipeTo.get(0).getClinicOrgan(), deleteSetRecipeCode);
    }

    /**
     * 校验his线下处方是否发生变更 如果变更则处理数据
     *
     * @param hisRecipeTo his处方数据
     * @param patientDTO  患者信息
     */
    public void hisRecipeInfoCheck(List<QueryHisRecipResTO> hisRecipeTo, PatientDTO patientDTO) {
        LOGGER.info("BaseOfflineToOnlineService hisRecipeInfoCheck param hisRecipeTO = {} , patientDTO={}", JSONUtils.toString(hisRecipeTo), JSONUtils.toString(patientDTO));
        if (CollectionUtils.isEmpty(hisRecipeTo)) {
            return;
        }
        Integer clinicOrgan = hisRecipeTo.get(0).getClinicOrgan();
        if (null == clinicOrgan) {
            LOGGER.info("hisRecipeInfoCheck his data error clinicOrgan is null");
            return;
        }
        List<String> recipeCodeList = hisRecipeTo.stream().map(QueryHisRecipResTO::getRecipeCode).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(recipeCodeList)) {
            LOGGER.info("hisRecipeInfoCheck his data error recipeCodeList is null");
            return;
        }
        //获取平台处方
        List<Recipe> recipeList = recipeDAO.findByRecipeCodeAndClinicOrgan(recipeCodeList, clinicOrgan);
        LOGGER.info("hisRecipeInfoCheck recipeList = {}", JSONUtils.toString(recipeList));

        //获取未处理的线下处方
        List<HisRecipe> hisRecipeList = hisRecipeDao.findNoDealHisRecipe(clinicOrgan, recipeCodeList);
        LOGGER.info("hisRecipeInfoCheck hisRecipeList = {}", JSONUtils.toString(hisRecipeList));
        if (CollectionUtils.isEmpty(hisRecipeList)) {
            return;
        }
        //获取一个key为未处理recipeCode,值为未处理HisRecipe的map对象
        Map<String, HisRecipe> hisRecipeMap = hisRecipeList.stream().collect(Collectors.toMap(HisRecipe::getRecipeCode, a -> a, (k1, k2) -> k1));
        //获取未处理的线下处方Ids，用来获取线下处方详情
        List<Integer> hisRecipeIds = hisRecipeList.stream().map(HisRecipe::getHisRecipeID).distinct().collect(Collectors.toList());
        //获取未处理的线下处方详情
        List<HisRecipeDetail> hisRecipeDetailList = hisRecipeDetailDAO.findByHisRecipeIds(hisRecipeIds);
        LOGGER.info("hisRecipeInfoCheck hisRecipeDetailList = {}", JSONUtils.toString(hisRecipeDetailList));
        //诊断变更，更新诊断
        hisRecipeManager.updateDisease(hisRecipeTo, recipeList, hisRecipeMap);
        //药品发生变更，删除关联信息
        deleteRecipeData(hisRecipeTo, hisRecipeMap, hisRecipeDetailList, patientDTO.getMpiId());
        LOGGER.info("BaseOfflineToOnlineService hisRecipeInfoCheck 方法结束");
    }

}
