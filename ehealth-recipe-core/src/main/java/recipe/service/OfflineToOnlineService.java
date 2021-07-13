package recipe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.ExtInfoTO;
import com.ngari.his.recipe.mode.MedicalInfo;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.his.recipe.mode.RecipeDetailTO;
import com.ngari.patient.dto.*;
import com.ngari.patient.service.*;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.grouprecipe.model.GroupRecipeConf;
import com.ngari.recipe.recipe.model.*;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.bean.RecipeGiveModeButtonRes;
import recipe.constant.PayConstant;
import recipe.constant.RecipeBussConstant;
import recipe.dao.*;
import recipe.dao.bean.HisRecipeListBean;
import recipe.factory.status.constant.RecipeOrderStatusEnum;
import recipe.factory.status.constant.RecipeStatusEnum;
import recipe.givemode.business.GiveModeFactory;
import recipe.givemode.business.IGiveModeBase;
import recipe.offlinetoonline.constant.OfflineToOnlineEnum;
import recipe.offlinetoonline.service.IOfflineToOnlineService;
import recipe.offlinetoonline.vo.FindHisRecipeDetailReqVO;
import recipe.offlinetoonline.vo.FindHisRecipeDetailResVO;
import recipe.offlinetoonline.vo.FindHisRecipeListVO;
import recipe.offlinetoonline.vo.SettleForOfflineToOnlineVO;
import recipe.service.manager.EmrRecipeManager;
import recipe.service.manager.GroupRecipeManager;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author 刘敏
 * @date 2021\6\30
 */
@RpcBean(value = "offlineToOnlineService", mvc_authentication = false)
public class OfflineToOnlineService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OfflineToOnlineService.class);

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
    private GroupRecipeManager groupRecipeManager;
    @Autowired
    private DrugMakingMethodDao drugMakingMethodDao;
    @Autowired
    private SymptomDAO symptomDAO;

    @Autowired
    private IRevisitExService exService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    @Qualifier("noPayServiceImpl")
    private IOfflineToOnlineService iOfflineToOnlineService;

    /**
     * 获取购药按钮
     *
     * @param recipeIds
     * @return
     */
    public List<RecipeGiveModeButtonRes> getRecipeGiveModeButtonRes(List<Integer> recipeIds) {
        LOGGER.info("OfflineToOnlineService getRecipeGiveModeButtonRes request = {}",  JSONUtils.toString(recipeIds));
        List<RecipeGiveModeButtonRes> recipeGiveModeButtonRes = recipeService.getRecipeGiveModeButtonRes(recipeIds);
        if (CollectionUtils.isEmpty(recipeGiveModeButtonRes)) {
            throw new DAOException(609, "“抱歉，当前处方没有可支持的购药方式”");
        }
        LOGGER.info("OfflineToOnlineService getRecipeGiveModeButtonRes response = {}",  JSONUtils.toString(recipeGiveModeButtonRes));
        return recipeGiveModeButtonRes;
    }

    /**
     * @param request
     * @return
     * @Description 批量同步线下处方数据
     * @Author liumin
     */
    public List<Integer> batchSyncRecipeFromHis(SettleForOfflineToOnlineVO request) {
        LOGGER.info("OfflineToOnlineService batchSyncRecipeFromHis request = {}", JSONUtils.toString(request));
        List<Integer> recipeIds = new ArrayList<>();
        // 1、删数据
        deleteRecipeByRecipeCodes(request.getOrganId(),request.getRecipeCode());

        request.getRecipeCode().forEach(recipeCode -> {
            // 2、线下转线上
            FindHisRecipeDetailReqVO findHisRecipeDetailReqVO=getFindHisRecipeDetailParam(request.getMpiId(),recipeCode, request.getOrganId(),request.getCardId());
            FindHisRecipeDetailResVO findHisRecipeDetailResVO=iOfflineToOnlineService.findHisRecipeDetail(findHisRecipeDetailReqVO);
            if(findHisRecipeDetailResVO!=null){
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
        List<Recipe> recipes=recipeDAO.findRecipeByRecipeIdAndClinicOrgan(Integer.parseInt(request.getOrganId()),recipeIds);
        if(CollectionUtils.isNotEmpty(recipes)&& recipes.size()>0){
            LOGGER.info("batchSyncRecipeFromHis 存在已失效处方");
            throw new DAOException(600, "处方单过期已失效");
        }
        LOGGER.info("OfflineToOnlineService batchSyncRecipeFromHis response = {}",  JSONUtils.toString(recipeIds));
        return recipeIds;
    }

    /**
     * 获取一个获取详情的入参对象
     * @param mpiId             就诊人mpiid
     * @param recipeCode        处方号
     * @param organId           机构id
     * @param cardId            卡号
     * @return
     */
    private FindHisRecipeDetailReqVO getFindHisRecipeDetailParam(String mpiId, String recipeCode, String organId, String cardId) {
        FindHisRecipeDetailReqVO findHisRecipeDetailReqVO;
        findHisRecipeDetailReqVO=FindHisRecipeDetailReqVO.builder()
                .mpiId(mpiId)
                .recipeCode(recipeCode)
                .organId(Integer.parseInt(organId))
                .cardId(cardId)
                .build();
        return findHisRecipeDetailReqVO;
    }

    public List<HisRecipe> findHisRecipes(FindHisRecipeListVO request) {
        return hisRecipeDAO.findHisRecipes(request.getOrganId(), request.getMpiId(), Integer.parseInt(request.getStatus()), request.getStart(), request.getLimit());
    }

//    /**
//     * organId 机构编码
//     * mpiId 用户mpiId
//     * timeQuantum 时间段  1 代表一个月  3 代表三个月 6 代表6个月
//     * status 1 未处理 2 已处理
//     *
//     * @param findHisRecipeListVO 入参
//     * @return 前端展示
//     * @Deprecated
//     */
//    @RpcService
//    public List<MergeRecipeVO> findHisRecipe(@Valid FindHisRecipeListVO findHisRecipeListVO) {
//        LOGGER.info("offlineToOnlineService findHisRecipe request:{}", JSONUtils.toString(findHisRecipeListVO));
//        if (null == findHisRecipeListVO
//                || findHisRecipeListVO.getOrganId() == null
//                || StringUtils.isEmpty(findHisRecipeListVO.getMpiId())
//                || StringUtils.isEmpty(findHisRecipeListVO.getStatus())) {
//            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
//        }
//        try {
//            List<MergeRecipeVO> result = findHisRecipeEnter(findHisRecipeListVO);
//            LOGGER.info("offlineToOnlineService findHisRecipe result:{}", JSONUtils.toString(result));
//            return result;
//        } catch (DAOException e1) {
//            LOGGER.error("offlineToOnlineService findHisRecipe error", e1);
//            throw new DAOException(e1.getCode(), e1.getMessage());
//        } catch (Exception e) {
//            LOGGER.error("offlineToOnlineService findHisRecipe error", e);
//            throw new DAOException(recipe.constant.ErrorCode.SERVICE_ERROR, e.getMessage());
//        }
//    }

//    /**
//     * 线下处方列表获取入口,根据前端传的状态onready（待处理）ongoing（进行中）isover（已完成）返回具体列表
//     *
//     * @param findHisRecipeListVO 请求入参
//     * @return 线下处方列表
//     */
//    private List<MergeRecipeVO> findHisRecipeEnter(FindHisRecipeListVO findHisRecipeListVO) {
//        LOGGER.info("offlineToOnlineService findHisRecipe findHisRecipeListVO:{}", JSONUtils.toString(findHisRecipeListVO));
//        String status = findHisRecipeListVO.getStatus();
//        String mpiId = findHisRecipeListVO.getMpiId();
//        String carId = findHisRecipeListVO.getCardId();
//        Integer organId = findHisRecipeListVO.getOrganId();
//        Integer timeQuantum = findHisRecipeListVO.getTimeQuantum();
//        Integer start = findHisRecipeListVO.getStart();
//        Integer limit = findHisRecipeListVO.getLimit();
//
//        PatientService patientService = BasicAPI.getService(PatientService.class);
//        PatientDTO patientDTO = patientService.getPatientBeanByMpiId(mpiId);
//        patientDTO.setCardId(StringUtils.isNotEmpty(carId) ? carId : "");
//
//        IGiveModeBase giveModeBase = GiveModeFactory.getGiveModeBaseByRecipe(new Recipe());
//        //获取机构配制的购药按钮
//        GiveModeShowButtonVO giveModeShowButtons = giveModeBase.getGiveModeSettingFromYypt(organId);
//        GiveModeButtonBean giveModeButtonBean = giveModeShowButtons.getListItem();
//
//        //表示获取待缴费或者已处理的处方,此时需要查询HIS
//        HisResponseTO<List<QueryHisRecipResTO>> hisResponseTO = queryData(organId, patientDTO, timeQuantum, OfflineToOnlineEnum.getOfflineToOnlineType(status), null);
//        if (null == hisResponseTO) {
//            return new ArrayList<>();
//        }
//        if ("ongoing".equals(status)) {
//            //表示为进行中的处方
//            return findOngoingHisRecipeList(hisResponseTO.getData(), patientDTO, giveModeButtonBean, start, limit);
//        } else {
//            if ("onready".equals(findHisRecipeListVO.getStatus())) {
//                List<HisRecipeVO> noPayFeeHisRecipeVO = covertToHisRecipeVoObject(hisResponseTO, patientDTO);
//                return findOnReadyHisRecipeList(noPayFeeHisRecipeVO, giveModeButtonBean);
//            } else {
//                checkHisRecipeAndSave(status, patientDTO, hisResponseTO);
//                return findFinishHisRecipeList(organId,mpiId, giveModeButtonBean, start, limit);
//            }
//        }
//    }

    /**
     * 获取待处理的线下的处方单
     *
     * @param request his的处方单集合
     * @return 前端需要的处方单集合
     */
    public List<MergeRecipeVO> findOnReadyHisRecipeList(List<HisRecipeVO> request, GiveModeButtonBean giveModeButtonBean) {
        LOGGER.info("offlineToOnlineService findOnReadyHisRecipe request:{}", JSONUtils.toString(request));
        //查询线下待缴费处方
        List<MergeRecipeVO> result = new ArrayList<>();
        GroupRecipeConf groupRecipeConf = groupRecipeManager.getMergeRecipeSetting();
        Boolean mergeRecipeFlag = groupRecipeConf.getMergeRecipeFlag();
        String mergeRecipeWayAfter = groupRecipeConf.getMergeRecipeWayAfter();
        if (mergeRecipeFlag) {
            //开启合并支付开关
            if ("e.registerId".equals(mergeRecipeWayAfter)) {
                //表示根据挂号序号分组
                Map<String, List<HisRecipeVO>> registerIdRelation = request.stream().collect(Collectors.groupingBy(HisRecipeVO::getRegisteredId));
                for (Map.Entry<String, List<HisRecipeVO>> entry : registerIdRelation.entrySet()) {
                    List<HisRecipeVO> recipes = entry.getValue();
                    if (StringUtils.isEmpty(entry.getKey())) {
                        //表示挂号序号为空,不能进行处方合并
                        covertMergeRecipeVO(null,false,null,null,giveModeButtonBean.getButtonSkipType(),recipes,result);
                    } else {
                        //可以进行合并支付
                        covertMergeRecipeVO(recipes.get(0).getRegisteredId(),true,mergeRecipeWayAfter,recipes.get(0).getHisRecipeID(),giveModeButtonBean.getButtonSkipType(),recipes,result);
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
                            covertMergeRecipeVO(null,false,null,null,giveModeButtonBean.getButtonSkipType(),recipes,result);
                        }
                    } else {
                        //表示挂号序号不为空,需要根据当前病种
                        Map<String, List<HisRecipeVO>> recipeMap = entry.getValue();
                        for (Map.Entry<String, List<HisRecipeVO>> recipeEntry : recipeMap.entrySet()) {
                            //如果病种为空不能进行合并
                            List<HisRecipeVO> recipes = recipeEntry.getValue();
                            if (StringUtils.isEmpty(recipeEntry.getKey())) {
                                covertMergeRecipeVO(null,false,null,null,giveModeButtonBean.getButtonSkipType(),recipes,result);
                            } else {
                                //可以进行合并支付
                                covertMergeRecipeVO(recipes.get(0).getChronicDiseaseName(),true,mergeRecipeWayAfter,recipes.get(0).getHisRecipeID(),giveModeButtonBean.getButtonSkipType(),recipes,result);
                            }
                        }
                    }
                }
            }
        } else {
            //不开启合并支付开关
            covertMergeRecipeVO(null,false,null,null,giveModeButtonBean.getButtonSkipType(),request,result);
        }
        LOGGER.info("offlineToOnlineService findOnReadyHisRecipe result:{}", JSONUtils.toString(result));
        return result;
    }

    /**
     * @param data 当前获取HIS的处方单集合
     * @return 前端需要展示的进行中的处方单集合, 先获取进行中的处方返回给前端展示, 然后对处方数据进行校验, 处方发生
     * 变更需要删除处方,当患者点击处方列表时如果订单已删除,会弹框提示"该处方单信息已变更，请退出重新获取处方信息"
     */
    public List<MergeRecipeVO> findOngoingHisRecipeList(List<QueryHisRecipResTO> data, PatientDTO patientDTO, GiveModeButtonBean giveModeButtonBean, Integer start, Integer limit) {
        LOGGER.info("offlineToOnlineService findOngoingHisRecipe request:{}", JSONUtils.toString(data));
        List<MergeRecipeVO> result = Lists.newArrayList();
        //查询所有进行中的线下处方
        List<HisRecipeListBean> hisRecipeListBeans = hisRecipeDAO.findOngoingHisRecipeListByMPIId(data.get(0).getClinicOrgan(),patientDTO.getMpiId(), start, limit);
        if (CollectionUtils.isEmpty(hisRecipeListBeans)) {
            return result;
        }
        //返回前端所需数据
        result = listShow(hisRecipeListBeans,hisRecipeListBeans.get(0).getClinicOrgan(),patientDTO.getMpiId(),giveModeButtonBean,start,limit);
        try {
            //更新数据校验
            hisRecipeInfoCheck(data, patientDTO);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo hisRecipeInfoCheck error ", e);
        }
        LOGGER.info("offlineToOnlineService findOngoingHisRecipe result:{}", JSONUtils.toString(result));
        return result;
    }


    /**
     * 查询已处理处方列表
     *
     * @param organId   机构Id
     * @param mpiId     就诊人mpiid
     * @param giveModeButtonBean    按钮跳转位置
     * @param start     分页开始位置
     * @param limit     查询条数
     * @return
     */
    public List<MergeRecipeVO> findFinishHisRecipeList(Integer organId,String mpiId, GiveModeButtonBean giveModeButtonBean, Integer start, Integer limit) {
        LOGGER.info("findFinishHisRecipes mpiId:{} giveModeButtonBean : {} index:{} limit:{} ", mpiId, giveModeButtonBean, start, limit);
        List<MergeRecipeVO> result = new ArrayList<>();
        // 获取所有已处理的线下处方
        List<HisRecipeListBean> hisRecipeListBeans = hisRecipeDAO.findHisRecipeListByMPIId(organId,mpiId, start, limit);
        if (CollectionUtils.isEmpty(hisRecipeListBeans)) {
            return result;
        }
        result = listShow(hisRecipeListBeans,organId,mpiId,giveModeButtonBean,start,limit);
        LOGGER.info("findFinishHisRecipes result:{} ", result);
        return result;
    }

    /**
     * 返回给前端所需数据
     * @param hisRecipeListBeans
     * @param organId
     * @param mpiId
     * @param giveModeButtonBean
     * @param start
     * @param limit
     * @return
     */
    private List<MergeRecipeVO> listShow(List<HisRecipeListBean> hisRecipeListBeans, Integer organId, String mpiId, GiveModeButtonBean giveModeButtonBean, Integer start, Integer limit) {
        List<MergeRecipeVO> result=new ArrayList<>();
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
                String grpupField="";
                if ("e.registerId".equals(mergeRecipeWay)) {
                    // 挂号序号
                    grpupField=hisRecipeListBean.getRegisteredId();
                } else {
                    // 慢病名称
                    grpupField=hisRecipeListBean.getChronicDiseaseName();
                }

                if (StringUtils.isEmpty(orderCode)) {
                    HisRecipeVO hisRecipeVO = ObjectCopyUtils.convert(hisRecipeListBean, HisRecipeVO.class);
                    setOtherInfo(hisRecipeVO,mpiId,hisRecipeListBean.getRecipeCode(),organId);
                    recipeIds.add(hisRecipeVO.getHisRecipeID());
                    hisRecipeVOS.add(hisRecipeVO);
                } else {
                    List<HisRecipeListBean> hisRecipeListBeansList = hisRecipeListBeanMap.get(orderCode);
                    List<RecipeOrder> recipeOrders = recipeOrderMap.get(orderCode);
                    RecipeOrder recipeOrder = null;
                    if(CollectionUtils.isNotEmpty(recipeOrders)) {
                        recipeOrder = recipeOrders.get(0);
                    }
                    hisRecipeVOS = setPatientTabStatusMerge(recipeMap,  recipeOrder, hisRecipeListBeansList,recipeIds);
                }
                covertMergeRecipeVO(grpupField,groupRecipeConf.getMergeRecipeFlag(),mergeRecipeWay,hisRecipeListBean.getHisRecipeID(),giveModeButtonBean.getButtonSkipType(),hisRecipeVOS,result);
            }

        });
        return result;
    }



    /**
     * 设置其它信息
     * @param collect
     * @param recipeOrder
     * @param hisRecipeListBeans
     * @param recipeIds
     * @return
     */
    private List<HisRecipeVO> setPatientTabStatusMerge(Map<Integer, List<Recipe>> collect, RecipeOrder recipeOrder, List<HisRecipeListBean> hisRecipeListBeans, Set<Integer> recipeIds) {
        List<HisRecipeVO> hisRecipeVOS =new ArrayList<>();
        hisRecipeListBeans.forEach(hisRecipeListBean -> {
            HisRecipeVO hisRecipeVO = ObjectCopyUtils.convert(hisRecipeListBean, HisRecipeVO.class);
            // 这个接口查询的所有处方都是线下处方 前端展示逻辑 0: 平台, 1: his
            hisRecipeVO.setFromFlag(1);
            // 有订单跳转订单
            hisRecipeVO.setJumpPageType(1);
            hisRecipeVO.setOrganDiseaseName(hisRecipeListBean.getDiseaseName());
            Recipe recipe = collect.get(hisRecipeListBean.getRecipeId()).get(0);
            if(Objects.nonNull(recipeOrder)) {
                hisRecipeVO.setStatusText(getTipsByStatusForPatient(recipe, recipeOrder));
            }
            recipeIds.add(hisRecipeVO.getHisRecipeID());
            hisRecipeVOS.add(hisRecipeVO);
        });
        return hisRecipeVOS;
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
     *转换成前端所需MergeRecipeVO对象
     * @param grpupFiled
     * @param mergeRecipeFlag
     * @param mergeRecipeWay
     * @param firstRecipeId
     * @param listSkipType
     * @param recipes
     * @param result
     */
    private void covertMergeRecipeVO(String grpupFiled, boolean mergeRecipeFlag, String mergeRecipeWay, Integer firstRecipeId, String  listSkipType, List<HisRecipeVO> recipes, List<MergeRecipeVO> result) {
        LOGGER.info("setMergeRecipeVO param grpupFiled:{},mergeRecipeFlag:{},mergeRecipeWay:{},firstRecipeId:{},listSkipType:{},recipes:{},result:{}",grpupFiled,mergeRecipeFlag,mergeRecipeWay,firstRecipeId,listSkipType,JSONUtils.toString(recipes),JSONUtils.toString(result));
        if(mergeRecipeFlag){
            MergeRecipeVO mergeRecipeVO = new MergeRecipeVO();
            mergeRecipeVO.setGroupField(grpupFiled);
            mergeRecipeVO.setMergeRecipeFlag(mergeRecipeFlag);
            mergeRecipeVO.setMergeRecipeWay(mergeRecipeWay);
            mergeRecipeVO.setRecipe(recipes);
            mergeRecipeVO.setFirstRecipeId(firstRecipeId);
            mergeRecipeVO.setListSkipType(listSkipType);
            result.add(mergeRecipeVO);
        }else{
            for (HisRecipeVO hisRecipeVO : recipes) {
                MergeRecipeVO mergeRecipeVO = new MergeRecipeVO();
                mergeRecipeVO.setMergeRecipeFlag(mergeRecipeFlag);
                mergeRecipeVO.setRecipe(Arrays.asList(hisRecipeVO));
                mergeRecipeVO.setListSkipType(listSkipType);
                result.add(mergeRecipeVO);
            }
        }
        LOGGER.info("setMergeRecipeVO response result:{}",JSONUtils.toString(result));
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
     * 线下待处理处方转换成前端列表所需对象
     *
     * @param responseTO    his返回线下处方
     * @param patientDTO    患者信息
     * @return
     */
    public List<HisRecipeVO> covertToHisRecipeVoObject(HisResponseTO<List<QueryHisRecipResTO>> responseTO, PatientDTO patientDTO) {
        LOGGER.info("covertHisRecipeObject param responseTO:{},patientDTO:{}" + JSONUtils.toString(responseTO),JSONUtils.toString(patientDTO));
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
            HisRecipe hisRecipeDb = hisRecipeDAO.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(
                    patientDTO.getMpiId(), queryHisRecipResTO.getClinicOrgan(), queryHisRecipResTO.getRecipeCode());
            //移除已在平台处理的处方单
            if (null != hisRecipeDb && new Integer("2").equals(hisRecipeDb.getStatus())) {
                continue;
            }
            //移除正在进行中的处方单
            Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(queryHisRecipResTO.getRecipeCode(), queryHisRecipResTO.getClinicOrgan());
            if (null != recipe && StringUtils.isNotEmpty(recipe.getOrderCode())) {
                continue;
            }

            HisRecipeVO hisRecipeVO =new HisRecipeVO();
            //详情需要
            hisRecipeVO.setMpiId(patientDTO.getMpiId());
            hisRecipeVO.setClinicOrgan(queryHisRecipResTO.getClinicOrgan());
            //列表显示需要
            hisRecipeVO.setPatientName(patientDTO.getPatientName());
            hisRecipeVO.setCreateDate(queryHisRecipResTO.getCreateDate());
            hisRecipeVO.setRecipeCode(queryHisRecipResTO.getRecipeCode());
            if (!StringUtils.isEmpty(queryHisRecipResTO.getDiseaseName())) {
                hisRecipeVO.setDiseaseName(queryHisRecipResTO.getDiseaseName());
            } else {
                hisRecipeVO.setDiseaseName("无");
            }
            hisRecipeVO.setDisease(queryHisRecipResTO.getDisease());
            hisRecipeVO.setDoctorName(queryHisRecipResTO.getDoctorName());
            hisRecipeVO.setDepartName(queryHisRecipResTO.getDepartName());
            setOtherInfo(hisRecipeVO, patientDTO.getMpiId(), queryHisRecipResTO.getRecipeCode(), queryHisRecipResTO.getClinicOrgan());
            //其它需要
            hisRecipeVO.setStatus(queryHisRecipResTO.getStatus());
            hisRecipeVO.setRecipeMode("ngarihealth");
            hisRecipeVOs.add(hisRecipeVO);
        }
        LOGGER.info("covertHisRecipeObject response hisRecipeVOs:{}" , JSONUtils.toString(hisRecipeVOs));
        return hisRecipeVOs;
    }

    /**
     * 设置医保信息
     * @param queryHisRecipResTO    his处方数据
     * @param hisRecipe             返回对象
     */
    private void setMedicalInfo(QueryHisRecipResTO queryHisRecipResTO, HisRecipe hisRecipe) {
        LOGGER.info("setMedicalInfo param queryHisRecipResTO:{},hisRecipe:{}",JSONUtils.toString(queryHisRecipResTO),JSONUtils.toString(hisRecipe));
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
        LOGGER.info("setMedicalInfo response hisRecipe:{} ",JSONUtils.toString(hisRecipe));
    }

    /**
     * 设置文案显示、处方来源、跳转页面
     * @param hisRecipeVO   返回对象
     * @param mpiId         mpiid
     * @param recipeCode    recipeCode
     * @param clinicOrgan   clinicOrgan
     */
    private void setOtherInfo(HisRecipeVO hisRecipeVO, String mpiId, String recipeCode, Integer clinicOrgan) {
        Recipe recipe = recipeDAO.getByHisRecipeCodeAndClinicOrganAndMpiid(mpiId, recipeCode, clinicOrgan);
        if (recipe == null) {
            hisRecipeVO.setStatusText("待处理");
            hisRecipeVO.setFromFlag(1);
            hisRecipeVO.setJumpPageType(0);
        } else {
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
            if (recipe.getRecipeSourceType() == 2 ) {
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
     * 保存线下处方数据到cdr_his_recipe、HisRecipeDetail、HisRecipeExt
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
                    //默认自费
                    hisRecipe.setMedicalType(1);
                }
                hisRecipe.setRecipeFee(queryHisRecipResTO.getRecipeFee());
                hisRecipe.setRecipeType(queryHisRecipResTO.getRecipeType());
                hisRecipe.setClinicOrgan(queryHisRecipResTO.getClinicOrgan());
                hisRecipe.setCreateTime(new Date());
                hisRecipe.setExtensionFlag(1);
                if (queryHisRecipResTO.getExtensionFlag() == null) {
                    //设置外延处方的标志
                    hisRecipe.setRecipePayType(0);
                } else {
                    //设置外延处方的标志
                    hisRecipe.setRecipePayType(queryHisRecipResTO.getExtensionFlag());
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
//                hisRecipe.setMakeMethodCode(queryHisRecipResTO.getMakeMethodCode());
//                hisRecipe.setMakeMethodText(queryHisRecipResTO.getMakeMethodText());
//                hisRecipe.setJuice(queryHisRecipResTO.getJuice());
//                hisRecipe.setJuiceUnit(queryHisRecipResTO.getJuiceUnit());
//                hisRecipe.setMinor(queryHisRecipResTO.getMinor());
//                hisRecipe.setMinorUnit(queryHisRecipResTO.getMinorUnit());
//                hisRecipe.setSymptomCode(queryHisRecipResTO.getSymptomCode());
//                hisRecipe.setSymptomName(queryHisRecipResTO.getSysmptomName());
//                hisRecipe.setSpecialDecoctionCode(queryHisRecipResTO.getSpecialDecoctiionCode());
//                hisRecipe.setCardNo(queryHisRecipResTO.getCardNo());
//                hisRecipe.setCardTypeCode(queryHisRecipResTO.getCardTypeCode());
//                hisRecipe.setCardTypeName(queryHisRecipResTO.getCardTypeName());

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

                if (null != queryHisRecipResTO.getExt()) {
                    for (ExtInfoTO extInfoTO : queryHisRecipResTO.getExt()) {
                        HisRecipeExt ext = ObjectCopyUtils.convert(extInfoTO, HisRecipeExt.class);
                        ext.setHisRecipeId(hisRecipe.getHisRecipeID());
                        hisRecipeExtDAO.save(ext);
                    }
                }

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
                        detail.setUseDays(recipeDetailTO.getUseDays());
                        detail.setUseDaysB(recipeDetailTO.getUseDays().toString());
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
//                        detail.setPharmacyCategray(recipeDetailTO.getPharmacyCategray());
//                        detail.setTcmContraindicationCause(recipeDetailTO.getTcmContraindicationCause());
//                        detail.setTcmContraindicationType(recipeDetailTO.getTcmContraindicationType());
                        hisRecipeDetailDAO.save(detail);
                    }
                }
            }else {
                hisRecipes.add(hisRecipe1);
            }
        }
        return hisRecipes;
    }

//    /**
//     * 历史老接口
//     * @param hisRecipeId
//     * @param mpiId
//     * @param recipeCode
//     * @param organId
//     * @param isCachePlatform 作废
//     * @param cardId
//     * @return
//     * @author liumin
//     * @Description 获取处方详情
//     */
//    @RpcService
//    public Map<String, Object> getHisRecipeDetail(Integer hisRecipeId, String mpiId, String recipeCode, String organId, Integer isCachePlatform, String cardId) {
//        LOGGER.info("offlineToOnlineService getHisRecipeDetail param:[{},{},{},{},{},{}]", hisRecipeId, mpiId, recipeCode, organId, isCachePlatform, cardId);
//        HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(mpiId, Integer.parseInt(organId), recipeCode);
//        if (hisRecipe == null) {
//            //throw new DAOException(700, "该处方单信息已变更，请退出重新获取处方信息。");
//        }
//        LOGGER.info("getHisRecipeDetail hisRecipe:{}.", JSONUtils.toString(hisRecipe));
//        //待处理
//        Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode, Integer.parseInt(organId));
//        Integer payFlag = 0;
//        if (recipe != null && StringUtils.isNotEmpty(recipe.getOrderCode())) {
//            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
//            if (new Integer(1).equals(recipeOrder.getPayFlag())) {
//                payFlag = 1;
//            }
//        }
//        if (hisRecipe.getStatus() != 2 ) {
//            LOGGER.info("getHisRecipeDetail 进入");
//            try {
//                PatientService patientService = BasicAPI.getService(PatientService.class);
//                PatientDTO patientDTO = patientService.getPatientBeanByMpiId(mpiId);
//                if (null == patientDTO) {
//                    throw new DAOException(609, "患者信息不存在");
//                }
//                if (StringUtils.isNotEmpty(cardId)) {
//                    patientDTO.setCardId(cardId);
//                } else {
//                    patientDTO.setCardId("");
//                }
//                recipeCodeThreadLocal.set(recipeCode);
//                //线下处方处理(存储到cdr_his相关表)
//                List<HisRecipe> hisRecipes = queryHisRecipeInfo(new Integer(organId), patientDTO, 180, 1);
//                if (CollectionUtils.isEmpty(hisRecipes)) {
//                    return initReturnMap();
//                } else {
//                    hisRecipeId = hisRecipes.get(0).getHisRecipeID();
//                }
//            } catch (Exception e) {
//                LOGGER.error("getHisRecipeDetail error hisRecipeId:{}", hisRecipeId, e);
//            } finally {
//                recipeCodeThreadLocal.remove();
//            }
//        }
//        if (hisRecipeId == null) {
//            //点击卡片 历史处方his不会返回 故从表查  同时也兼容已处理状态的处方，前端漏传hisRecipeId的情况
//            if (!StringUtils.isEmpty(recipeCode)) {
//                hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(Integer.parseInt(organId), recipeCode);
//            }
//            if (hisRecipe != null) {
//                hisRecipeId = hisRecipe.getHisRecipeID();
//            }
//        }
//
//        //存储到recipe相关表
//        if (hisRecipeId == null) {
//            throw new DAOException(DAOException.VALUE_NEEDED, "hisRecipeId不能为空！");
//        }
//        return getHisRecipeDetailByHisRecipeId(hisRecipeId);
//
//    }

    /**
     * 初始化一个返回对象
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

    /**
     * 保存线下处方到cdr_recipe_ext
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
        }else{
            if(StringUtils.isNotEmpty(hisRecipe.getDecoctionCode())){
                DrugDecoctionWayDao drugDecoctionWayDao = DAOFactory.getDAO(DrugDecoctionWayDao.class);
                DecoctionWay decoctionWay=drugDecoctionWayDao.getDecoctionWayByOrganIdAndCode(recipe.getClinicOrgan(),hisRecipe.getDecoctionCode());
                if(decoctionWay!=null){
                    recipeExtend.setDecoctionText(decoctionWay.getDecoctionText());
                }
            }
        }

        DrugMakingMethod drugMakingMethod=drugMakingMethodDao.getDrugMakingMethodByOrganIdAndCode(recipe.getClinicOrgan(),hisRecipe.getMakeMethodCode());
        if(drugMakingMethod!=null){
            recipeExtend.setMakeMethodId(drugMakingMethod.getMethodId().toString());
        }
        if(StringUtils.isNotEmpty(hisRecipe.getMakeMethodText())){
            recipeExtend.setMakeMethodText(hisRecipe.getMakeMethodText());
        }else{
            if(drugMakingMethod!=null){
                recipeExtend.setMakeMethodText(drugMakingMethod.getMethodText());
            }
        }
        recipeExtend.setJuice(hisRecipe.getJuice());
        recipeExtend.setJuiceUnit(hisRecipe.getJuiceUnit());
        recipeExtend.setMinor(hisRecipe.getMinor());
        recipeExtend.setMinorUnit(hisRecipe.getMinorUnit());
        Symptom symptom=symptomDAO.getByOrganIdAndSymptomCode(recipe.getClinicOrgan(),hisRecipe.getSymptomCode());
        if(symptom!=null){
            recipeExtend.setSymptomId(symptom.getSymptomId().toString());
        }
        if(StringUtils.isNotEmpty(hisRecipe.getSymptomName())){
            recipeExtend.setSymptomName(hisRecipe.getSymptomName());
        }else{
            if(symptom!=null){
                recipeExtend.setSymptomName(recipeExtend.getSymptomName());
            }
        }
        //列表详情都没用到这个特殊煎法 app说只有搜索用到了 那这个字段还有什么意义
        //recipeExtend.setSpecialDecoctionCode(hisRecipe.getSpecialDecoctiionCode());

        RevisitExDTO consultExDTO = new RevisitExDTO();
        try {
            consultExDTO = exService.getByRegisterId(hisRecipe.getRegisteredId());
        } catch (Exception e) {
            LOGGER.error("线下处方转线上通过挂号序号关联复诊 error", e);
        }
        if(StringUtils.isNotEmpty(hisRecipe.getCardNo())){
            recipeExtend.setCardNo(hisRecipe.getCardNo());
        }else{
            if (consultExDTO != null) {
                recipeExtend.setCardNo(consultExDTO.getCardId());
            }
        }
        if(StringUtils.isNotEmpty(hisRecipe.getCardTypeName())){
            recipeExtend.setCardTypeName(hisRecipe.getCardTypeName());
        }
        if(StringUtils.isNotEmpty(hisRecipe.getCardTypeName())){
            recipeExtend.setCardType(hisRecipe.getCardTypeCode());
        }else{
            if (consultExDTO != null) {
                recipeExtend.setCardType(consultExDTO.getCardType());
            }
        }
        recipeExtend.setRecipeCostNumber(hisRecipe.getRecipeCostNumber());
        RecipeBean recipeBean = new RecipeBean();
        BeanUtils.copy(recipe, recipeBean);
        emrRecipeManager.saveMedicalInfo(recipeBean, recipeExtend);
        recipeExtendDAO.save(recipeExtend);
    }

    /**
     * 状态文字提示（患者端）
     * @param recipe    处方
     * @param order     订单
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
     * 校验his线下处方是否发生变更 如果变更则处理数据
     * @param hisRecipeTO his处方数据
     * @param patientDTO  患者信息
     */
    public void hisRecipeInfoCheck(List<QueryHisRecipResTO> hisRecipeTO, PatientDTO patientDTO) {
        LOGGER.info("hisRecipeInfoCheck param hisRecipeTO = {} , patientDTO={}", JSONUtils.toString(hisRecipeTO),JSONUtils.toString(patientDTO));
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
        //获取平台处方
        List<Recipe> recipeList = recipeDAO.findByRecipeCodeAndClinicOrgan(recipeCodeList, clinicOrgan);
        LOGGER.info("hisRecipeInfoCheck recipeList = {}", JSONUtils.toString(recipeList));

        //获取未处理的线下处方
        List<HisRecipe> hisRecipeList = hisRecipeDAO.findNoDealHisRecipe(clinicOrgan, recipeCodeList);
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
        updateDisease(hisRecipeTO, recipeList, hisRecipeMap);
        //药品发生变更，删除关联信息
        deleteRecipeData(hisRecipeTO,hisRecipeMap,hisRecipeDetailList,patientDTO.getMpiId());
    }

    /**
     * 药品详情发生变化、数据不是由本人生成的未支付处方 数据处理
     * @param hisRecipeTO           his处方数据
     * @param hisRecipeMap          key为未处理recipeCode,值为未处理HisRecipe的map对象
     * @param hisRecipeDetailList   未处理的线下处方详情
     * @param mpiId                 查看详情处方的操作用户的mpiid
     */
    private void deleteRecipeData(List<QueryHisRecipResTO> hisRecipeTO, Map<String, HisRecipe> hisRecipeMap, List<HisRecipeDetail> hisRecipeDetailList, String mpiId) {
        if (CollectionUtils.isEmpty(hisRecipeDetailList)) {
            return;
        }
        Set<String> deleteSetRecipeCode=attachDeleteRecipeCodes(hisRecipeTO,hisRecipeMap,hisRecipeDetailList,mpiId);
        deleteSetRecipeCode(hisRecipeTO.get(0).getClinicOrgan(), deleteSetRecipeCode);
    }

    /**
     * 获取需要删除的recipeCodes
     * 判断药品详情发生变化、数据不是由本人生成的未支付处方、中药tcmFee变更
     * @param hisRecipeTO           his处方数据
     * @param hisRecipeMap          key为未处理recipeCode,值为未处理HisRecipe的map对象
     * @param hisRecipeDetailList   未处理的线下处方详情
     * @param mpiId                 查看详情处方的操作用户的mpiid
     * @return
     */
    public Set<String> attachDeleteRecipeCodes(List<QueryHisRecipResTO> hisRecipeTO, Map<String, HisRecipe> hisRecipeMap, List<HisRecipeDetail> hisRecipeDetailList, String mpiId) {
        Set<String> deleteSetRecipeCode = new HashSet<>();
        Map<Integer, List<HisRecipeDetail>> hisRecipeIdDetailMap = hisRecipeDetailList.stream().collect(Collectors.groupingBy(HisRecipeDetail::getHisRecipeId));
        hisRecipeTO.forEach(a -> {
            String recipeCode = a.getRecipeCode();
            HisRecipe hisRecipe = hisRecipeMap.get(recipeCode);
            if (null == hisRecipe) {
                return;
            } else {
                if (!hisRecipe.getMpiId().equals(mpiId)) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause mpiid recipeCode:{}", recipeCode);
                    return;
                }
            }
            //场景：没付钱跑到线下去支付了
            //如果已缴费处方在数据库里已存在，且数据里的状态是未缴费，则处理数据
            if (a.getStatus()==2) {
                if (1 == hisRecipe.getStatus()) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause Status recipeCode:{}", recipeCode);
                }
            }

            //已处理处方(现在因为其他用户绑定了该就诊人也要查询到数据，所以mpiid不一致，数据需要删除)
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
                if ( 0 != covertBigdecimal(hisRecipeDetail.getUseTotalDose()).compareTo(covertBigdecimal(recipeDetailTO.getUseTotalDose()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useTotalDose recipeCode:{}", recipeCode);
                    continue;
                }
                if ( !covertObject(hisRecipeDetail.getUseDose()).equals(covertObject(recipeDetailTO.getUseDose())) ){
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDose recipeCode:{}", recipeCode);
                    continue;
                }
                if ((!covertObject(hisRecipeDetail.getUseDoseStr()).equals(covertObject(recipeDetailTO.getUseDoseStr())))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDoseStr recipeCode:{}", recipeCode);
                    continue;
                }
                if ((!covertObject(hisRecipeDetail.getUseDaysB()).equals(covertObject(recipeDetailTO.getUseDaysB())))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDaysB recipeCode:{}", recipeCode);
                    continue;
                }

                if ((!covertObject( hisRecipeDetail.getUseDays()).equals(covertObject(recipeDetailTO.getUseDays())))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDays recipeCode:{}",recipeCode);
                    continue;
                }

                if ( !covertObject(hisRecipeDetail.getUsingRate()).equals(covertObject(recipeDetailTO.getUsingRate()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause usingRate recipeCode:{}", recipeCode);
                    continue;
                }

                if (!covertObject(hisRecipeDetail.getUsingRateText()).equals(covertObject(recipeDetailTO.getUsingRateText()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause usingRateText recipeCode:{}", recipeCode);
                    continue;
                }
                if ( !covertObject(hisRecipeDetail.getUsePathways()).equals(covertObject(recipeDetailTO.getUsePathWays()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause usePathWays recipeCode:{}", recipeCode);
                    continue;
                }
                if ( !covertObject(hisRecipeDetail.getUsePathwaysText()).equals(covertObject(recipeDetailTO.getUsePathwaysText()))) {
                    LOGGER.info("deleteSetRecipeCode cause usePathwaysText recipeCode:{}", recipeCode);
                    deleteSetRecipeCode.add(recipeCode);
                }
            }
            //中药判断tcmFee发生变化,删除数据
            BigDecimal tcmFee = a.getTcmFee();
            if ( covertBigdecimal(tcmFee).compareTo(covertBigdecimal(hisRecipe.getTcmFee())) != 0) {
                LOGGER.info("deleteSetRecipeCode cause tcmFee recipeCode:{}", recipeCode);
                deleteSetRecipeCode.add(hisRecipe.getRecipeCode());
            }
        });
        return deleteSetRecipeCode;
    }

    /**
     * 数据转换
     * @param obj
     * @return
     */
    private Object covertObject(Object obj) {
        if(obj instanceof BigDecimal){
            if(obj==null){
                return BigDecimal.ZERO;
            }
        }else if(obj instanceof String){
            if(obj==null){
                return "";
            }
        }else if(obj instanceof Integer){
            if(obj==null){
                return 0;
            }
        }
        return obj;
    }



    private BigDecimal covertBigdecimal(BigDecimal obj) {
        if(obj==null){
            return BigDecimal.ZERO;
        }
        return obj;
    }

    /**
     * 删除线下处方相关数据
     *
     * @param clinicOrgan         机构id
     * @param deleteSetRecipeCode 要删除的recipeCodes
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
        //recipe表不删 添加的时候修改（除id外所有字段）
        recipeDAO.updateRecipeStatusByRecipeIds(recipeIds);
        //日志记录
        Map<Integer, Recipe> recipeMap =recipeList.stream().collect(Collectors.toMap(Recipe::getRecipeId,Function.identity(),(key1, key2) -> key2));
        recipeIds.forEach(a -> {
            RecipeLogService.saveRecipeLog(a, recipeMap.get(a).getStatus(), RecipeStatusEnum.RECIPE_STATUS_DELETE.getType(),
                    "线下转线上：修改处方状态为已删除");
        });
        LOGGER.info("deleteSetRecipeCode is delete end ");
    }

    /**
     * 更新诊断字段
     * @param hisRecipeTO     his处方数据
     * @param hisRecipeMap    key为未处理recipeCode,值为未处理HisRecipe的map对象
     * @param recipeList      平台处方
     */
    private Map<String, HisRecipe> updateDisease(List<QueryHisRecipResTO> hisRecipeTO, List<Recipe> recipeList, Map<String, HisRecipe> hisRecipeMap) {
        LOGGER.info("updateHisRecipe param hisRecipeTO:{},recipeList:{},hisRecipeMap:{}",JSONUtils.toString(hisRecipeTO),JSONUtils.toString(recipeList),JSONUtils.toString(hisRecipeMap));
        Map<String, Recipe> recipeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getRecipeCode, a -> a, (k1, k2) -> k1));
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
                recipe.setOrganDiseaseId(disease);
                recipe.setOrganDiseaseName(diseaseName);
                recipeDAO.update(recipe);

                RecipeBean recipeBean = new RecipeBean();
                BeanUtils.copy(recipe, recipeBean);
                recipeBean.setOrganDiseaseName(diseaseName);
                recipeBean.setOrganDiseaseId(disease);
                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                emrRecipeManager.updateMedicalInfo(recipeBean, recipeExtend);
                recipeExtendDAO.saveOrUpdateRecipeExtend(recipeExtend);
            }
        });
        LOGGER.info("updateHisRecipe response hisRecipeMap:{}",JSONUtils.toString(hisRecipeMap));
        return hisRecipeMap;
    }

    /**
     * 删除未支付处方，同时判断是否存在已缴费处方 若存在 返回true
     * @param organId       机构id
     * @param recipeCodes   需删除处方号
     * @return
     */
    public void deleteRecipeByRecipeCodes(String organId, List<String> recipeCodes) {
        //默认不存在
        boolean isExistPayRecipe=false;
        List<Recipe> recipes=recipeDAO.findRecipeByRecipeCodeAndClinicOrgan(Integer.parseInt(organId),recipeCodes);
        if(CollectionUtils.isNotEmpty(recipes)&&recipes.size()>0){
            //存在已支付处方出现在（待处理列表） 提示用户刷新列表
            isExistPayRecipe=true;
        }
        if(isExistPayRecipe){
            throw new DAOException(609, "处方单已经缴费，请刷新重试");
        }
        //2 删除数据
        deleteSetRecipeCode(Integer.parseInt(organId), new HashSet<>(recipeCodes));
    }

    /**
     * 获取机构配置够药方式
     * @param organId 机构id
     * @return
     */
    public GiveModeButtonBean getGiveModeButtonBean(Integer organId){
        IGiveModeBase giveModeBase = GiveModeFactory.getGiveModeBaseByRecipe(new Recipe());
        //获取机构配制的购药按钮
        GiveModeShowButtonVO giveModeShowButtons = giveModeBase.getGiveModeSettingFromYypt(organId);
        GiveModeButtonBean giveModeButtonBean = giveModeShowButtons.getListItem();
        return giveModeButtonBean;
    }

    /**
     * 将线下处方转化成线上处方，保存线下处方信息到平台
     * @param hisRecipeId cdr_his_recipe表主键
     * @return
     */
    public Integer saveRecipeInfo(Integer hisRecipeId) {
        LOGGER.info("saveRecipeInfo param hisRecipeId:{}",hisRecipeId);
        if (hisRecipeId == null) {
            throw new DAOException(DAOException.DAO_NOT_FOUND, "没有查询到来自医院的处方单,请刷新页面！");
        }
        HisRecipe hisRecipe = hisRecipeDAO.get(hisRecipeId);
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
     * @param hisRecipe 线下处方对象
     * @return
     */
    private Recipe saveRecipeFromHisRecipe(HisRecipe hisRecipe) {
        LOGGER.info("saveRecipeFromHisRecipe param hisRecipe:{}.", JSONUtils.toString(hisRecipe));
        Recipe recipeDb = recipeDAO.getByHisRecipeCodeAndClinicOrgan(hisRecipe.getRecipeCode(), hisRecipe.getClinicOrgan());
        LOGGER.info("saveRecipeFromHisRecipe recipeDb:{}.", JSONUtils.toString(recipeDb));
        UserRoleToken userRoleToken = UserRoleToken.getCurrent();
        if(recipeDb!=null && !isAllowDeleteByPayFlag(recipeDb.getPayFlag())){
            //已支付状态下的处方不允许修改
            return recipeDb;
        }
        Recipe recipe=new Recipe();
        if(recipeDb!=null){
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
        recipe=recipeDAO.saveOrUpdate(recipe);
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
     * @param recipeId 处方号
     * @param hisRecipe 线下处方对象
     */
    private void savaRecipeDetail(Integer recipeId, HisRecipe hisRecipe) {
        LOGGER.info("savaRecipeDetail param recipeId:{},hisRecipe:{}",recipeId,JSONUtils.toString(hisRecipe));
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
            if(StringUtils.isNotEmpty(hisRecipeDetail.getUseDaysB())){
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
            LOGGER.info("savaRecipeDetail 已经保存的recipeId:{},recipeDetail:{}",recipeId,JSONUtils.toString(recipedetail));
        }
    }

    /**
     * @param hisRecipeId cdr_his_recipe表主键
     * @param recipeId cdr_recipe表主键
     * @author liumin
     * @Description 通过hisRecipeId和recipeId查询并返回前端所需数据
     * @return
     */
    public FindHisRecipeDetailResVO getHisRecipeDetailByHisRecipeIdAndRecipeId(Integer hisRecipeId, Integer recipeId) {
        if(hisRecipeId==null || recipeId==null){
            throw new DAOException(DAOException.DAO_NOT_FOUND, "没有查询到来自医院的处方单,请刷新页面！");
        }
        FindHisRecipeDetailResVO findHisRecipeDetailResVO=new FindHisRecipeDetailResVO();
        Map<String, Object> recipeDetailMap=new HashMap<String, Object>();
        Recipe recipe=recipeDAO.get(recipeId);
        HisRecipe hisRecipe= hisRecipeDAO.get(hisRecipeId);
        List<HisRecipeExt> hisRecipeExts = hisRecipeExtDAO.findByHisRecipeId(hisRecipeId);
        if(recipe==null){
            throw new DAOException(DAOException.DAO_NOT_FOUND, "没有查询到来自医院的处方单,请刷新页面！");
        }
        if("2".equals(hisRecipe.getStatus())){
            recipeDetailMap= recipeService.getPatientRecipeByIdForOfflineRecipe(recipeId);
        }else {
            recipeDetailMap = recipeService.getPatientRecipeById(recipeId);
        }
        findHisRecipeDetailResVO.setPlatRecipeDetail(recipeDetailMap);
        findHisRecipeDetailResVO.setHisRecipeExts(hisRecipeExts);
        findHisRecipeDetailResVO.setShowText(hisRecipe.getShowText());
        LOGGER.info("getHisRecipeDetailByHisRecipeId response:{}", JSONUtils.toString(recipeDetailMap));
        return findHisRecipeDetailResVO;
    }

    /**
     * 是否允许删除 默认不允许
     * @param payFlag 支付状态
     * @return
     */
    boolean isAllowDeleteByPayFlag(Integer payFlag){
        if(PayConstant.PAY_FLAG_NOT_PAY==payFlag ||PayConstant.PAY_FLAG_REFUND_FAIL==payFlag ){
            return false;
        }
        return true;
    }

    /**
     * 获取处方id
     * @param organId       机构
     * @param recipeCode    处方号
     * @param hisRecipes    线下处方
     * @return
     */
    public Integer attachRecipeId(Integer organId, String recipeCode, List<HisRecipe> hisRecipes) {
        HisRecipe hisRecipe=new HisRecipe();
        if (CollectionUtils.isEmpty(hisRecipes)) {
            //点击卡片 历史处方his不会返回 故从表查  同时也兼容已处理状态的处方，前端漏传hisRecipeId的情况
            if (!StringUtils.isEmpty(recipeCode)) {
                hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(organId, recipeCode);
            }
            if (hisRecipe != null) {
                return hisRecipe.getHisRecipeID();
            }else{
                return null;
            }
        }
        return hisRecipes.get(0).getHisRecipeID();
    }

    /**
     * TODO 这个接口还在调用么
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


}
