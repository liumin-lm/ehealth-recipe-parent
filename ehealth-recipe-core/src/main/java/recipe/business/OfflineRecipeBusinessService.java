package recipe.business;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.his.recipe.mode.RecipeDetailTO;
import com.ngari.his.recipe.mode.RecipeInfoTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailReqVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailResVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeListVO;
import com.ngari.recipe.offlinetoonline.model.SettleForOfflineToOnlineVO;
import com.ngari.recipe.recipe.constant.RecipeTypeEnum;
import com.ngari.recipe.recipe.model.MergeRecipeVO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import com.ngari.recipe.vo.OffLineRecipeDetailVO;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.event.GlobalEventExecFactory;
import ngari.openapi.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.client.OfflineRecipeClient;
import recipe.common.CommonConstant;
import recipe.constant.ErrorCode;
import recipe.core.api.patient.IOfflineRecipeBusinessService;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeParameterDao;
import recipe.enumerate.status.OfflineToOnlineEnum;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.WriteHisEnum;
import recipe.factory.offlinetoonline.IOfflineToOnlineStrategy;
import recipe.factory.offlinetoonline.OfflineToOnlineFactory;
import recipe.manager.*;
import recipe.service.RecipeLogService;
import recipe.util.MapValueUtil;
import recipe.util.ObjectCopyUtils;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.patient.PatientRecipeListReqVO;
import recipe.vo.patient.RecipeGiveModeButtonRes;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

/**
 * 线下处方核心逻辑
 *
 * @Author liumin
 * @Date 2021/7/20 下午4:58
 * @Description
 */
@Service
public class OfflineRecipeBusinessService extends BaseService implements IOfflineRecipeBusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OfflineRecipeBusinessService.class);
    @Autowired
    private HisRecipeManager hisRecipeManager;
    @Autowired
    private OfflineToOnlineFactory offlineToOnlineFactory;
    @Autowired
    private IConfigurationCenterUtilsService configurationCenterUtilsService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private OfflineRecipeClient offlineRecipeClient;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private StateManager stateManager;
    @Autowired
    private RecipeTherapyManager recipeTherapyManager;
    @Autowired
    private PharmacyManager pharmacyManager;
    @Autowired
    private OrganDrugListManager organDrugListManager;
    @Autowired
    private RecipeParameterDao recipeParameterDao;

    @Override
    public List<MergeRecipeVO> findHisRecipeList(FindHisRecipeListVO request) {
        LOGGER.info("OfflineToOnlineService findHisRecipeList request:{}", JSONUtils.toString(request));
        try {
            // 1、公共参数获取
            PatientDTO patientDTO = obtainPatientInfo(request);
            // 2、获取his数据
            HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos = hisRecipeManager.queryHisRecipeData(request.getOrganId(), patientDTO, request.getTimeQuantum(), OfflineToOnlineEnum.getOfflineToOnlineType(request.getStatus()), null);
            // 3、待处理、进行中、已处理线下处方列表服务差异化实现
            IOfflineToOnlineStrategy offlineToOnlineStrategy = offlineToOnlineFactory.getFactoryService(request.getStatus());
            List<MergeRecipeVO> res = offlineToOnlineStrategy.findHisRecipeList(hisRecipeInfos, patientDTO, request);
            LOGGER.info("OfflineToOnlineService findHisRecipeList res:{}", JSONUtils.toString(res));
            return res;
        } catch (DAOException e) {
            logger.error("OfflineToOnlineService findHisRecipeList error", e);
            throw new DAOException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("OfflineToOnlineService findHisRecipeList error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    @Override
    public FindHisRecipeDetailResVO findHisRecipeDetail(FindHisRecipeDetailReqVO request) {
        logger.info("OfflineToOnlineService findHisRecipeDetail request:{}", JSONUtils.toString(request));
        try {
            request = obtainFindHisRecipeDetailParam(request);
            IOfflineToOnlineStrategy offlineToOnlineStrategy = offlineToOnlineFactory.getFactoryService(request.getStatus());
            return offlineToOnlineStrategy.findHisRecipeDetail(request);
        } catch (DAOException e) {
            logger.error("OfflineToOnlineService findHisRecipeDetail error", e);
            throw new DAOException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("OfflineToOnlineService findHisRecipeDetail error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }


    @Override
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request) {
        logger.info("OfflineToOnlineService settleForOfflineToOnline request:{}", JSONUtils.toString(request));
        IOfflineToOnlineStrategy offlineToOnlineStrategy = offlineToOnlineFactory.getFactoryService(OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getName());
        List<RecipeGiveModeButtonRes> result = offlineToOnlineStrategy.settleForOfflineToOnline(request);
        logger.info("OfflineToOnlineService settleForOfflineToOnline res:{}", JSONUtils.toString(result));
        return result;
    }

    @Override
    public List<String> getCardType(Integer organId) {
        //卡类型 1 表示身份证  2 表示就诊卡  3 表示就诊卡
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
     * 获取患者信息
     *
     * @param request
     * @return
     */
    private PatientDTO obtainPatientInfo(FindHisRecipeListVO request) {
        logger.info("OfflineToOnlineService obtainPatientInfo request:{}", JSONUtils.toString(request));
        PatientDTO patientDTO = hisRecipeManager.getPatientBeanByMpiId(request.getMpiId());
        if (null == patientDTO) {
            throw new DAOException(609, "患者信息不存在");
        }
        patientDTO.setCardId(StringUtils.isNotEmpty(request.getCardId()) ? request.getCardId() : patientDTO.getCardId());
        logger.info("OfflineToOnlineService obtainPatientInfo req patientDTO:{}", JSONUtils.toString(patientDTO));
        return patientDTO;
    }

    /**
     * 如果前端status没传【卡片消息详情获取】，需根据参数判断，获取状态
     *
     * @param request
     * @return
     */
    private FindHisRecipeDetailReqVO obtainFindHisRecipeDetailParam(FindHisRecipeDetailReqVO request) {
        logger.info("OfflineToOnlineService obtainFindHisRecipeDetailParam request:{}", JSONUtils.toString(request));
        //获取对应的status
        if (StringUtils.isEmpty(request.getStatus())) {
            String status = hisRecipeManager.attachHisRecipeStatus(request.getMpiId(), request.getOrganId(), request.getRecipeCode());
            request.setStatus(status);
        }
        //获取对应的hisRecipeId
        if (request.getHisRecipeId() == null) {
            //如果为已处理，需要获取hisRecipeId,再根据hisRecipeId获取详情（）
            if (OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ALREADY_PAY.getName().equals(request.getStatus())) {
                HisRecipe hisRecipe = hisRecipeManager.obatainHisRecipeByOrganIdAndMpiIdAndRecipeCode(request.getOrganId(), request.getMpiId(), request.getRecipeCode());
                if (hisRecipe != null) {
                    request.setHisRecipeId(hisRecipe.getHisRecipeID());
                }
            }
        }
        if (null != request.getTimeQuantum()) {
//            request.setTimeQuantum(request.getTimeQuantum());
        } else {
            request.setTimeQuantum(6);
        }
        logger.info("OfflineToOnlineService obtainFindHisRecipeDetailParam req:{}", JSONUtils.toString(request));
        return request;
    }

    /**
     * 获取线下处方详情
     *
     * @param mpiId       患者ID
     * @param clinicOrgan 机构ID
     * @param recipeCode  处方号码
     * @date 2021/8/06
     */
    @Override
    public OffLineRecipeDetailVO getHisRecipeDetail(String mpiId, Integer clinicOrgan, String recipeCode,String createDate) {
        logger.info("RecipeBusinessService getOffLineRecipeDetails mpiId={},clinicOrgan={},recipeCode={}", mpiId, clinicOrgan, recipeCode);
        PatientDTO patient = patientService.getPatientByMpiId(mpiId);
        if (ObjectUtils.isEmpty(patient)) {
            throw new DAOException(609, "患者信息不存在");
        }
//        //获取线下处方信息
        OffLineRecipeDetailDTO offLineRecipeDetailDTO = new OffLineRecipeDetailDTO();
        QueryHisRecipResTO queryHisRecipResTO = offlineRecipeClient.getHisRecipeDetail(offLineRecipeDetailDTO, clinicOrgan, patient, 6, 2, recipeCode,createDate);

        //判断是否为儿科 设置部门名称
        DepartmentDTO departmentDTO = departmentService.getByCodeAndOrgan(queryHisRecipResTO.getDepartCode(), queryHisRecipResTO.getClinicOrgan());
        try {
            RecipeDAO recipeDao = DAOFactory.getDAO(RecipeDAO.class);
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            try {
                if (StringUtils.isNotEmpty(mpiId) && clinicOrgan != null && StringUtils.isNotEmpty(recipeCode)) {
                    Recipe recipe = recipeDao.getByHisRecipeCodeAndClinicOrganAndMpiid(mpiId, recipeCode, clinicOrgan);
                    if (recipe != null) {
                        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                        if (recipeExtend != null && Integer.valueOf(1).equals(recipeExtend.getRecipeFlag())) {
                            //兼容老版本（此版本暂时不做删除）
                            offLineRecipeDetailDTO.setChildRecipeFlag(new Integer(0).equals(recipeExtend.getRecipeFlag()));
                            //新版本使用
                            offLineRecipeDetailDTO.setRecipeFlag(recipeExtend.getRecipeFlag());
                        }
                    } else {
                        //兼容老版本（此版本暂时不做删除）
                        offLineRecipeDetailDTO.setChildRecipeFlag(false);
                        //新版本使用
                        if (queryHisRecipResTO.getRecipeFlag() == null) {
                            offLineRecipeDetailDTO.setRecipeFlag(0);
                        } else {
                            offLineRecipeDetailDTO.setRecipeFlag(queryHisRecipResTO.getRecipeFlag());
                        }
                    }
                }
            } catch (Exception e) {
                logger.info("getOffLineRecipeDetails recipeFlag 设置错误");
            }

            //设置监护人字段
            if (!ObjectUtils.isEmpty(patient)) {
                offLineRecipeDetailDTO.setGuardianName(patient.getGuardianName());
                offLineRecipeDetailDTO.setGuardianAge(patient.getGuardianAge());
                offLineRecipeDetailDTO.setGuardianSex(patient.getGuardianSex());
            }

        } catch (Exception e) {
            logger.error("RecipeBusinessService getOffLineRecipeDetails", e);
        }
        //优先使用HIS返回的departName如果为空则查表
        if (!org.springframework.util.StringUtils.isEmpty(queryHisRecipResTO.getDepartName())) {
            offLineRecipeDetailDTO.setDepartName(queryHisRecipResTO.getDepartName());
        } else if (!org.springframework.util.StringUtils.isEmpty(departmentDTO.getName())) {
            offLineRecipeDetailDTO.setDepartName(departmentDTO.getName());
        }
        //处方药品信息
        List<RecipeDetailTO> drugLists = queryHisRecipResTO.getDrugList();
        List<RecipeDetailDTO> detailDTOS = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.valueOf(0);
        //计算药品价格
        Integer recipeType = queryHisRecipResTO.getRecipeType();
        Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(clinicOrgan, recipeType));
        boolean priceNotExits = false;
        if (!ObjectUtils.isEmpty(drugLists)) {
            for (RecipeDetailTO drugList : drugLists) {
                //如果his返回药品信息有totalPrice，优先使用 否则使用Price*UseTotalDose
                if (!ObjectUtils.isEmpty(drugList.getTotalPrice())) {
                    totalPrice = totalPrice.add(drugList.getTotalPrice());
                } else if (!ObjectUtils.isEmpty(drugList.getPrice()) && !ObjectUtils.isEmpty(drugList.getUseTotalDose())) {
                    totalPrice = totalPrice.add(drugList.getPrice().multiply(drugList.getUseTotalDose()));
                } else {
                    //如果二者都没有则不返回
                    priceNotExits = true;
                }
                RecipeDetailDTO recipeDetailDTO = new RecipeDetailDTO();
                BeanUtils.copy(drugList, recipeDetailDTO);
                recipeDetailDTO.setUsePathways(drugList.getUsePathwaysCode());
                recipeDetailDTO.setUsePathwaysText(drugList.getUsePathWays());
                recipeDetailDTO.setUsingRate(drugList.getUsingRateCode());
                recipeDetailDTO.setUsingRateText(drugList.getUsingRate());
                //拼接中药名称
                if (RecipeTypeEnum.RECIPETYPE_WM.getType().equals(recipeType)) {
                    recipeDetailDTO.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(recipeDetailDTO, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(recipeType)));
                }
                detailDTOS.add(recipeDetailDTO);
            }
            offLineRecipeDetailDTO.setRecipeDetails(detailDTOS);
            if (priceNotExits) {
                offLineRecipeDetailDTO.setTotalPrice(null);
            } else {
                offLineRecipeDetailDTO.setTotalPrice(totalPrice);
            }
        }
        //患者基本属性
        if (!ObjectUtils.isEmpty(patient)) {
            offLineRecipeDetailDTO.setPatientSex(patient.getPatientSex());
            offLineRecipeDetailDTO.setPatientBirthday(patient.getBirthday());
        }
        OffLineRecipeDetailVO offLineRecipeDetailVO = new OffLineRecipeDetailVO();
        BeanUtils.copy(offLineRecipeDetailDTO, offLineRecipeDetailVO);
        logger.info("RecipeBusinessService getOffLineRecipeDetails result={}", ctd.util.JSONUtils.toString(offLineRecipeDetailDTO));
        return offLineRecipeDetailVO;
    }

    @Override
    public RecipeInfoDTO pushRecipe(Integer recipeId, Integer pushType, Integer sysType, Integer expressFeePayType, Double expressFee, String giveModeKey, Integer pushDest) {
        logger.info("RecipeBusinessService pushRecipe recipeId={}", recipeId);
        RecipeInfoDTO recipePdfDTO = recipeTherapyManager.getRecipeTherapyDTO(recipeId);
        ChargeItemDTO chargeItemDTO = new ChargeItemDTO(expressFeePayType, expressFee);
        recipePdfDTO.setChargeItemDTO(chargeItemDTO);
        Recipe recipe = recipePdfDTO.getRecipe();
        String allowSecondWriteHisOrgan = recipeParameterDao.getByName("allowSecondWriteHisOrgan");
        if (RecipeStateEnum.PROCESS_STATE_CANCELLATION.getType().equals(recipe.getProcessState())) {
            logger.info("RecipeBusinessService pushRecipe 当前处方已撤销 recipeId:{}", recipeId);
            return recipePdfDTO;
        }
        if (StringUtils.isEmpty(allowSecondWriteHisOrgan) || !allowSecondWriteHisOrgan.contains(recipe.getClinicOrgan().toString())) {
            if (CommonConstant.RECIPE_PUSH_TYPE.equals(pushType) && WriteHisEnum.WRITE_HIS_STATE_ORDER.getType().equals(recipe.getWriteHisState())) {
                logger.info("RecipeBusinessService pushRecipe 当前处方已写入his成功 recipeId:{}", recipeId);
                return recipePdfDTO;
            }
        }
        //同时set最小售卖单位/单位HIS编码等
        organDrugListManager.setDrugItemCode(recipe.getClinicOrgan(), recipePdfDTO.getRecipeDetails());
        try {
            Map<Integer, PharmacyTcm> pharmacyIdMap = pharmacyManager.pharmacyIdMap(recipe.getClinicOrgan());
            RecipeInfoDTO result = hisRecipeManager.pushRecipe(recipePdfDTO, pushType, pharmacyIdMap, sysType, giveModeKey, pushDest);
            logger.info("RecipeBusinessService pushRecipe result={}", JSONUtils.toString(result));
            result.getRecipe().setBussSource(recipe.getBussSource());
            result.getRecipe().setClinicId(recipe.getClinicId());
            recipeManager.updatePushHisRecipe(result.getRecipe(), recipeId, pushType);
            recipeManager.updatePushHisRecipeExt(result.getRecipeExtend(), recipeId, pushType);
            if (CommonConstant.RECIPE_PUSH_TYPE.equals(pushType)) {
                stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_ORDER, RecipeStateEnum.SUB_ORDER_READY_SUBMIT_ORDER);
            } else {
                stateManager.updateWriteHisState(recipeId, WriteHisEnum.NONE);
            }
            logger.info("RecipeBusinessService pushRecipe end recipeId:{}", recipeId);
            return result;
        } catch (Exception e) {
            logger.error("RecipeBusinessService pushRecipe error,sysType={},recipeId:{}", sysType, recipeId, e);
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "当前处方推送his失败:" + e.getMessage());
            String msg = configurationClient.getValueCatch(recipe.getClinicOrgan(), "pushHisRecipeResultMsg", "当前处方推送his失败");
            stateManager.updateWriteHisState(recipeId, WriteHisEnum.WRITE_HIS_STATE_AUDIT);
            throw new DAOException(ErrorCode.SERVICE_ERROR, msg);
        }
    }

    @Override
    public void offlineToOnlineForRecipe(FindHisRecipeDetailReqVO request) {
        logger.info("OfflineToOnlineService findHisRecipeDetail request:{}", JSONUtils.toString(request));
        try {
            request = obtainFindHisRecipeDetailParam(request);
            IOfflineToOnlineStrategy offlineToOnlineStrategy = offlineToOnlineFactory.getFactoryService(request.getStatus());
            offlineToOnlineStrategy.offlineToOnlineForRecipe(request);
        } catch (DAOException e) {
            logger.error("OfflineToOnlineService findHisRecipeDetail error", e);
            throw new DAOException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("OfflineToOnlineService findHisRecipeDetail error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    @Override
    public HisResponseTO abolishOffLineRecipe(Integer organId, List<String> recipeCodes) {
        return hisRecipeManager.abolishOffLineRecipe(organId, recipeCodes);
    }

    @Override
    public List<RecipeInfoTO> patientOfflineRecipe(Integer organId, String patientId, String patientName, Date startTime, Date endTime) {
        return offlineRecipeClient.patientOfflineRecipe(organId, patientId, patientName, startTime, endTime);
    }

    @Override
    public HisRecipeDTO getOffLineRecipeDetailsV1(Integer organId, String recipeCode, String createDate) {
        return offlineRecipeClient.getOffLineRecipeDetailsV1(organId, recipeCode, createDate);
    }

    @Override
    public List<RecipeInfoVO> patientRecipeList(PatientRecipeListReqVO req) {
        PatientRecipeListReqDTO reqDTO = ObjectCopyUtils.convert(req, PatientRecipeListReqDTO.class);

        //线下异步任务
        List<Integer> hisTypes = PatientRecipeListReqDTO.hisState(reqDTO.getState());
        List<FutureTask<List<com.ngari.platform.recipe.mode.RecipeDTO>>> futureTasks = new LinkedList<>();
        //根据药企配置查询 库存
        for (Integer type : hisTypes) {
            FutureTask<List<com.ngari.platform.recipe.mode.RecipeDTO>> ft = new FutureTask<>(() -> hisRecipeManager.patientRecipeList(reqDTO, type));
            futureTasks.add(ft);
            GlobalEventExecFactory.instance().getExecutor().submit(ft);
        }
        //查询线上处方
        List<RecipeInfoDTO> recipeList = recipeManager.patientRecipeList(reqDTO);
        //查询线下处方
        List<List<com.ngari.platform.recipe.mode.RecipeDTO>> hisRecipeList = super.futureTaskCallbackBeanList(futureTasks, null);
        //组装线上 线下数据
        List<RecipeInfoVO> list = recipeList(recipeList, hisRecipeList);
        //去重返回
        return list.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() ->
                new TreeSet<>(Comparator.comparing(a -> a.getRecipeBean().getRecipeCode()))), ArrayList::new));
    }

    /**
     * 组装线上 线下数据
     *
     * @param recipeList    线上处方列表
     * @param hisRecipeList 线下处方列表
     * @return
     */
    private List<RecipeInfoVO> recipeList(List<RecipeInfoDTO> recipeList, List<List<com.ngari.platform.recipe.mode.RecipeDTO>> hisRecipeList) {
        List<RecipeInfoVO> list = new ArrayList<>();
        recipeList.forEach(a -> {
            if (null == a.getRecipe() || StringUtils.isEmpty(a.getRecipe().getRecipeCode())) {
                return;
            }
            RecipeInfoVO recipeInfo = new RecipeInfoVO();
            recipeInfo.setRecipeBean(ObjectCopyUtils.convert(a.getRecipe(), RecipeBean.class));
            recipeInfo.setRecipeExtendBean(ObjectCopyUtils.convert(a.getRecipeExtend(), RecipeExtendBean.class));
            recipeInfo.setRecipeDetails(ObjectCopyUtils.convert(a.getRecipeDetails(), RecipeDetailBean.class));
            list.add(recipeInfo);
        });
        hisRecipeList.forEach(a -> {
            if (CollectionUtils.isEmpty(a)) {
                return;
            }
            a.forEach(b -> {
                if (null == b.getRecipeBean() || StringUtils.isEmpty(b.getRecipeBean().getRecipeCode())) {
                    return;
                }
                RecipeInfoVO recipeInfo = new RecipeInfoVO();
                recipeInfo.setRecipeBean(ObjectCopyUtils.convert(b.getRecipeBean(), RecipeBean.class));
                recipeInfo.setRecipeExtendBean(ObjectCopyUtils.convert(b.getRecipeExtendBean(), RecipeExtendBean.class));
                recipeInfo.setRecipeDetails(ObjectCopyUtils.convert(b.getRecipeDetails(), RecipeDetailBean.class));
                list.add(recipeInfo);
            });
        });
        return list;
    }
}
