package recipe.business.offlinetoonline.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.AppointDepartService;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.dto.GroupRecipeConf;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailReqVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailResVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeListVO;
import com.ngari.recipe.offlinetoonline.model.SettleForOfflineToOnlineVO;
import com.ngari.recipe.recipe.model.*;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcService;
import eh.base.constant.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.business.offlinetoonline.IOfflineToOnlineStrategy;
import recipe.business.offlinetoonline.OfflineToOnlineFactory;
import recipe.client.RevisitClient;
import recipe.constant.PayConstant;
import recipe.constant.RecipeBussConstant;
import recipe.dao.*;
import recipe.dao.bean.HisRecipeListBean;
import recipe.enumerate.status.OfflineToOnlineEnum;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.givemode.business.GiveModeFactory;
import recipe.givemode.business.IGiveModeBase;
import recipe.manager.EmrRecipeManager;
import recipe.manager.GroupRecipeManager;
import recipe.manager.HisRecipeManager;
import recipe.manager.RecipeManager;
import recipe.service.DrugsEnterpriseService;
import recipe.service.RecipeService;
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
    HisRecipeManager hisRecipeManager;

    @Autowired
    RecipeManager recipeManager;

    @Autowired
    RevisitClient revisitClient;

    @Autowired
    OfflineToOnlineFactory offlineToOnlineFactory;

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
        LOGGER.info("getFindHisRecipeDetailParam mpiId:{},recipeCode:{},organId:{},cardId:{}",mpiId,recipeCode,organId,cardId);
        FindHisRecipeDetailReqVO findHisRecipeDetailReqVO;
        findHisRecipeDetailReqVO = FindHisRecipeDetailReqVO.builder()
                .mpiId(mpiId)
                .recipeCode(recipeCode)
                .organId(Integer.parseInt(organId))
                .cardId(cardId)
                .build();
        LOGGER.info("getFindHisRecipeDetailParam res:{}",findHisRecipeDetailReqVO);
        return findHisRecipeDetailReqVO;
    }

    public List<HisRecipe> findHisRecipes(FindHisRecipeListVO request) {
        return hisRecipeDao.findHisRecipes(request.getOrganId(), request.getMpiId(), OfflineToOnlineEnum.getOfflineToOnlineType(request.getStatus()), request.getStart(), request.getLimit());
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
        LOGGER.info("findFinishHisRecipeList mpiId:{} giveModeButtonBean : {} index:{} limit:{} ", mpiId, giveModeButtonBean, start, limit);
        List<MergeRecipeVO> result = new ArrayList<>();
        // 获取所有已处理的线下处方
        List<HisRecipeListBean> hisRecipeListBeans = hisRecipeDao.findHisRecipeListByMPIId(organId, mpiId, start, limit);
        if (CollectionUtils.isEmpty(hisRecipeListBeans)) {
            return result;
        }
        result = listShow(hisRecipeListBeans, organId, mpiId, giveModeButtonBean, start, limit);
        LOGGER.info("findFinishHisRecipeList result:{} ", result);
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
        LOGGER.info("listShow hisRecipeListBeans:{},organId:{},mpiId:{},giveModeButtonBean:{}",JSONUtils.toString(hisRecipeListBeans),organId,mpiId,JSONUtils.toString(giveModeButtonBean));
        List<MergeRecipeVO> result = new ArrayList<>();
        Set<Integer> recipeIds = new HashSet<>();

        Map<String, List<HisRecipeListBean>> hisRecipeListBeanMap = hisRecipeListBeans.stream().filter(hisRecipeListBean -> hisRecipeListBean.getOrderCode() != null).collect(Collectors.groupingBy(HisRecipeListBean::getOrderCode));
        Map<Integer, List<Recipe>> recipeMap = getRecipeMap(hisRecipeListBeans);
        Map<String, List<RecipeOrder>> recipeOrderMap = getRecipeOrderMap(hisRecipeListBeanMap.keySet());

        // 获取合并处方显示配置项
        GroupRecipeConf groupRecipeConf = groupRecipeManager.getMergeRecipeSetting();
        String mergeRecipeWay = groupRecipeConf.getMergeRecipeWayAfter();

        hisRecipeListBeans.forEach(hisRecipeListBean -> {
            List<HisRecipeVO> hisRecipeVOS = new ArrayList<>();
            if (!recipeIds.contains(hisRecipeListBean.getHisRecipeID())) {
                String orderCode = hisRecipeListBean.getOrderCode();
                String grpupField = "";
                if ("e.registerId".equals(mergeRecipeWay)) {
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
                    hisRecipeVOS.add(hisRecipeVO);
                } else {
                    List<HisRecipeListBean> hisRecipeListBeansList = hisRecipeListBeanMap.get(orderCode);
                    List<RecipeOrder> recipeOrders = recipeOrderMap.get(orderCode);
                    RecipeOrder recipeOrder = null;
                    if (CollectionUtils.isNotEmpty(recipeOrders)) {
                        recipeOrder = recipeOrders.get(0);
                    }
                    hisRecipeVOS = setPatientTabStatusMerge(recipeMap, recipeOrder, hisRecipeListBeansList, recipeIds);
                }
                covertMergeRecipeVO(grpupField, groupRecipeConf.getMergeRecipeFlag(), mergeRecipeWay, hisRecipeListBean.getHisRecipeID(), giveModeButtonBean.getButtonSkipType(), hisRecipeVOS, result);
            }

        });
        LOGGER.info("listShow result:{}",JSONUtils.toString(result));
        return result;
    }


    /**
     * 设置其它信息
     *
     * @param collect
     * @param recipeOrder
     * @param hisRecipeListBeans
     * @param recipeIds
     * @return
     */
    private List<HisRecipeVO> setPatientTabStatusMerge(Map<Integer, List<Recipe>> recipeMap, RecipeOrder recipeOrder, List<HisRecipeListBean> hisRecipeListBeans, Set<Integer> recipeIds) {
        LOGGER.info("setPatientTabStatusMerge param recipeMap:{} ,recipeOrder:{} ,hisRecipeListBeans:{} ,recipeIds:{}",JSONUtils.toString(recipeMap) ,JSONUtils.toString(recipeOrder),JSONUtils.toString(hisRecipeListBeans),JSONUtils.toString(recipeIds));
        List<HisRecipeVO> hisRecipeVOS = new ArrayList<>();
        hisRecipeListBeans.forEach(hisRecipeListBean -> {
            HisRecipeVO hisRecipeVO = ObjectCopyUtils.convert(hisRecipeListBean, HisRecipeVO.class);
            // 这个接口查询的所有处方都是线下处方 前端展示逻辑 0: 平台, 1: his
            hisRecipeVO.setFromFlag(1);
            // 有订单跳转订单
            hisRecipeVO.setJumpPageType(1);
            hisRecipeVO.setOrganDiseaseName(hisRecipeListBean.getDiseaseName());
            Recipe recipe = recipeMap.get(hisRecipeListBean.getRecipeId()).get(0);
            if (Objects.nonNull(recipeOrder)) {
                hisRecipeVO.setStatusText(getTipsByStatusForPatient(recipe, recipeOrder));
            }
            recipeIds.add(hisRecipeVO.getHisRecipeID());
            hisRecipeVOS.add(hisRecipeVO);
        });
        LOGGER.info("setPatientTabStatusMerge res:{}",JSONUtils.toString(hisRecipeVOS));
        return hisRecipeVOS;
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
        LOGGER.info("covertMergeRecipeVO param grpupFiled:{},mergeRecipeFlag:{},mergeRecipeWay:{},firstRecipeId:{},listSkipType:{},recipes:{},result:{}", grpupFiled, mergeRecipeFlag, mergeRecipeWay, firstRecipeId, listSkipType, JSONUtils.toString(recipes), JSONUtils.toString(result));
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
        LOGGER.info("covertMergeRecipeVO response result:{}", JSONUtils.toString(result));
    }


    private Map<Integer, List<Recipe>> getRecipeMap(List<HisRecipeListBean> hisRecipeListByMPIIds) {
        Set<Integer> recipes = hisRecipeListByMPIIds.stream().filter(hisRecipeListBean -> hisRecipeListBean.getRecipeId() != null).collect(Collectors.groupingBy(HisRecipeListBean::getRecipeId)).keySet();
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
            if (recipe.getRecipeSourceType() == 2) {
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
            }
        }
    }



    /**
     * 初始化一个返回对象
     *
     * @return
     */
    private Map<String, Object> initReturnMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("hisRecipeDetails", null);
        map.put("hisRecipeExts", null);
        map.put("showText", null);
        return map;
    }




    /**
     * 状态文字提示（患者端）
     *
     * @param recipe 处方
     * @param order  订单
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
                            if (RecipeOrderStatusEnum.ORDER_STATUS_AWAIT_SHIPPING.getType().equals(orderStatus)) {
                                tips = "待配送";
                            } else if (RecipeOrderStatusEnum.ORDER_STATUS_PROCEED_SHIPPING.equals(orderStatus)) {
                                tips = "配送中";
                            } else if (RecipeOrderStatusEnum.ORDER_STATUS_DONE.equals(orderStatus)) {
                                tips = "已完成";
                            }
                        }
                    }

                } else if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode) && StringUtils.isNotEmpty(recipe.getOrderCode())) {
                    if (RecipeOrderStatusEnum.ORDER_STATUS_HAS_DRUG.equals(orderStatus)) {
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


    /**
     * 获取机构配置够药方式
     *
     * @param organId 机构id
     * @return
     */
    public GiveModeButtonBean getGiveModeButtonBean(Integer organId) {
        LOGGER.info("BaseOfflineToOnlineService getGiveModeButtonBean param organId:{}",organId);
        IGiveModeBase giveModeBase = GiveModeFactory.getGiveModeBaseByRecipe(new Recipe());
        //获取机构配制的购药按钮
        GiveModeShowButtonVO giveModeShowButtons = giveModeBase.getGiveModeSettingFromYypt(organId);
        GiveModeButtonBean res = giveModeShowButtons.getListItem();
        LOGGER.info("BaseOfflineToOnlineService getGiveModeButtonBean res :{}",JSONUtils.toString(res));
        return res;
    }


    /**
     * TODO 这个接口还在调用么
     *
     * @param organId
     * @return
     */
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
     * @param hisRecipeId cdr_his_recipe表主键
     * @param recipeId    cdr_recipe表主键
     * @return
     * @author liumin
     * @Description 通过hisRecipeId和recipeId查询并返回前端所需数据
     */
    public FindHisRecipeDetailResVO getHisRecipeDetailByHisRecipeIdAndRecipeId(Integer hisRecipeId, Integer recipeId) {
        if (hisRecipeId == null || recipeId == null) {
            throw new DAOException(DAOException.DAO_NOT_FOUND, "没有查询到来自医院的处方单,请刷新页面！");
        }
        FindHisRecipeDetailResVO findHisRecipeDetailResVO = new FindHisRecipeDetailResVO();
        Map<String, Object> recipeDetailMap = new HashMap<String, Object>();
        Recipe recipe = recipeDAO.get(recipeId);
        HisRecipe hisRecipe = hisRecipeDao.get(hisRecipeId);
        List<HisRecipeExt> hisRecipeExts = hisRecipeExtDAO.findByHisRecipeId(hisRecipeId);
        if (recipe == null) {
            throw new DAOException(DAOException.DAO_NOT_FOUND, "没有查询到来自医院的处方单,请刷新页面！");
        }
        if ("2".equals(hisRecipe.getStatus())) {
            recipeDetailMap = recipeService.getPatientRecipeByIdForOfflineRecipe(recipeId);
        } else {
            recipeDetailMap = recipeService.getPatientRecipeById(recipeId);
        }
        findHisRecipeDetailResVO.setPlatRecipeDetail(recipeDetailMap);
        List<com.ngari.recipe.offlinetoonline.model.HisRecipeExt> hisRecipeExtsTemp = new ArrayList<com.ngari.recipe.offlinetoonline.model.HisRecipeExt>();
        org.springframework.beans.BeanUtils.copyProperties(hisRecipeExts, hisRecipeExtsTemp);
        findHisRecipeDetailResVO.setHisRecipeExts(hisRecipeExtsTemp);
        findHisRecipeDetailResVO.setShowText(hisRecipe.getShowText());
        LOGGER.info("getHisRecipeDetailByHisRecipeId response:{}", JSONUtils.toString(recipeDetailMap));
        return findHisRecipeDetailResVO;
    }

    /**
     * 将线下处方转化成线上处方，保存线下处方信息到平台
     *
     * @param hisRecipeId cdr_his_recipe表主键
     * @return
     */
    public Integer saveRecipeInfo(Integer hisRecipeId) {
        LOGGER.info("saveRecipeInfo param hisRecipeId:{}", hisRecipeId);
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
        LOGGER.info("saveRecipeFromHisRecipe param hisRecipe:{}.", JSONUtils.toString(hisRecipe));
        Recipe recipeDb = recipeDAO.getByHisRecipeCodeAndClinicOrgan(hisRecipe.getRecipeCode(), hisRecipe.getClinicOrgan());
        LOGGER.info("saveRecipeFromHisRecipe recipeDb:{}.", JSONUtils.toString(recipeDb));
        UserRoleToken userRoleToken = UserRoleToken.getCurrent();
        if (recipeDb != null && !isAllowDeleteByPayFlag(recipeDb.getPayFlag())) {
            //已支付状态下的处方不允许修改
            return recipeDb;
        }
        Recipe recipe = new Recipe();
        if (recipeDb != null) {
            recipe.setRecipeId(recipeDb.getRecipeId());
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
        //BUG#50592 【实施】【上海市奉贤区中心医院】【A】查询线下处方缴费提示系统繁忙
        AppointDepartService appointDepartService = ApplicationUtils.getBasicService(AppointDepartService.class);
        AppointDepartDTO appointDepartDTO = appointDepartService.getByOrganIDAndAppointDepartCode(hisRecipe.getClinicOrgan(), hisRecipe.getDepartCode());
        if (appointDepartDTO != null) {
            recipe.setDepart(appointDepartDTO.getDepartId());
        } else {
            LOGGER.info("offlineToOnlineService saveRecipeFromHisRecipe 无法查询到挂号科室:{}.", hisRecipe.getDepartCode());
            throw new DAOException(ErrorCode.SERVICE_ERROR, "挂号科室维护错误");
        }
        EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
        if (StringUtils.isNotEmpty(hisRecipe.getDoctorCode())) {
            EmploymentDTO employmentDTO = employmentService.getByJobNumberAndOrganId(hisRecipe.getDoctorCode(), hisRecipe.getClinicOrgan());
            if (employmentDTO != null && employmentDTO.getDoctorId() != null) {
                recipe.setDoctor(employmentDTO.getDoctorId());
            } else {
                LOGGER.error("请确认医院的医生工号和纳里维护的是否一致:" + hisRecipe.getDoctorCode());
                throw new DAOException(ErrorCode.SERVICE_ERROR, "医生工号维护错误");
            }
        }

        if (StringUtils.isNotEmpty(hisRecipe.getCheckerCode())) {
            EmploymentDTO employmentDTO = employmentService.getByJobNumberAndOrganId(hisRecipe.getCheckerCode(), hisRecipe.getClinicOrgan());
            if (employmentDTO != null && employmentDTO.getDoctorId() != null) {
                recipe.setChecker(employmentDTO.getDoctorId());
                recipe.setCheckerText(hisRecipe.getCheckerName());
            } else {
                LOGGER.error("请确认医院的医生工号和纳里维护的是否一致:" + hisRecipe.getDoctorCode());
                throw new DAOException(ErrorCode.SERVICE_ERROR, "医生工号维护错误");
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
        if (hisRecipe.getStatus() == 2) {
            recipe.setPayFlag(1);
            recipe.setStatus(6);
        } else {
            recipe.setPayFlag(0);
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
        //购药按钮
        List<Integer> drugsEnterpriseContinue = drugsEnterpriseService.getDrugsEnterpriseContinue(recipe.getRecipeId(), recipe.getClinicOrgan());
        LOGGER.info("getHisRecipeDetailByHisRecipeId recipeId = {} drugsEnterpriseContinue = {}", recipe.getRecipeId(), JSONUtils.toString(drugsEnterpriseContinue));
        if (CollectionUtils.isNotEmpty(drugsEnterpriseContinue)) {
            String join = StringUtils.join(drugsEnterpriseContinue, ",");
            recipe.setRecipeSupportGiveMode(join);
        }
        return recipeDAO.saveOrUpdate(recipe);

    }

    /**
     * 保存数据到平台处方详情表
     *
     * @param recipeId  处方号
     * @param hisRecipe 线下处方对象
     */
    private void savaRecipeDetail(Integer recipeId, HisRecipe hisRecipe) {
        LOGGER.info("savaRecipeDetail param recipeId:{},hisRecipe:{}", recipeId, JSONUtils.toString(hisRecipe));
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
            if (StringUtils.isNotEmpty(hisRecipeDetail.getUseDaysB())) {
                recipedetail.setUseDaysB(new BigDecimal(hisRecipeDetail.getUseDaysB()).setScale(0, BigDecimal.ROUND_UP).toString());
            }
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
            recipedetail.setTcmContraindicationCause(hisRecipeDetail.getTcmContraindicationCause());
            recipedetail.setTcmContraindicationType(hisRecipeDetail.getTcmContraindicationType());
            recipeDetailDAO.save(recipedetail);
            LOGGER.info("savaRecipeDetail 已经保存的recipeId:{},recipeDetail:{}", recipeId, JSONUtils.toString(recipedetail));
        }
    }


    /**
     * 是否允许删除 默认不允许
     *
     * @param payFlag 支付状态
     * @return
     */
    boolean isAllowDeleteByPayFlag(Integer payFlag) {
        if (PayConstant.PAY_FLAG_NOT_PAY == payFlag || PayConstant.PAY_FLAG_REFUND_FAIL == payFlag) {
            return false;
        }
        return true;
    }

    /**
     * 保存线下处方到cdr_recipe_ext
     *
     * @param recipe    处方
     * @param hisRecipe 线下处方
     */
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
        //列表详情都没用到这个特殊煎法 app说只有搜索用到了 那这个字段还有什么意义
        //recipeExtend.setSpecialDecoctionCode(hisRecipe.getSpecialDecoctiionCode());

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
    }

    public List<HisRecipeListBean> findOngoingHisRecipeListByMPIId(Integer clinicOrgan, String mpiId, Integer start, Integer limit) {
        return hisRecipeDao.findOngoingHisRecipeListByMPIId(clinicOrgan, mpiId, start, limit);
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
            hisRecipeManager.hisRecipeInfoCheck(hisResponseTO.getData(), patientDTO);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo hisRecipeInfoCheck error ", e);
        }
        try {
            //数据入库
            hisRecipeManager.saveHisRecipeInfo(hisResponseTO, patientDTO, OfflineToOnlineEnum.getOfflineToOnlineType(status));
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo saveHisRecipeInfo error ", e);
        }
    }

    /**
     * 查询线下处方 入库操作
     *
     * @param organId
     * @param patientDTO
     * @param timeQuantum
     * @param flag
     */
//    @RpcService
//    public List<HisRecipe> queryHisRecipeInfo(Integer organId, PatientDTO patientDTO, Integer timeQuantum, Integer flag) {
//        List<HisRecipe> recipes = new ArrayList<>();
//        //查询数据
//        HisResponseTO<List<QueryHisRecipResTO>> responseTO = queryData(organId, patientDTO, timeQuantum, flag, recipeCodeThreadLocal.get());
//        if (null == responseTO || CollectionUtils.isEmpty(responseTO.getData())) {
//            return null;
//        }
//        try {
//            //更新数据校验
//            hisRecipeInfoCheck(responseTO.getData(), patientDTO);
//        } catch (Exception e) {
//            LOGGER.error("queryHisRecipeInfo hisRecipeInfoCheck error ", e);
//        }
//        try {
//            //数据入库
//            recipes = saveHisRecipeInfo(responseTO, patientDTO, flag);
//        } catch (Exception e) {
//            LOGGER.error("queryHisRecipeInfo saveHisRecipeInfo error ", e);
//        }
//        return recipes;
//    }

    /**
     * @param hisRecipeId
     * @return
     * @author liumin
     * @Description 转平台处方并根据hisRecipeId去表里查返回详情
     */
//    private Map<String, Object> getHisRecipeDetailByHisRecipeId(Integer hisRecipeId) {
//        //1、保存
//        Integer recipeId=saveRecipeInfo(hisRecipeId);
//        if(recipeId==null){
//            return null;
//        }else {
//            //2、查询
//            return getHisRecipeDetailByHisRecipeIdAndRecipeId(hisRecipeId,recipeId);
//        }
//    }
}
