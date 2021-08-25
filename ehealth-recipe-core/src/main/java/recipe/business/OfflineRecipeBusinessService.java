package recipe.business;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.his.recipe.mode.RecipeDetailTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.dto.OffLineRecipeDetailDTO;
import com.ngari.recipe.dto.RecipeDetailDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailReqVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailResVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeListVO;
import com.ngari.recipe.offlinetoonline.model.SettleForOfflineToOnlineVO;
import com.ngari.recipe.recipe.constant.RecipeTypeEnum;
import com.ngari.recipe.recipe.model.MergeRecipeVO;
import com.ngari.recipe.vo.OffLineRecipeDetailVO;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ngari.openapi.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.client.OfflineRecipeClient;
import recipe.constant.ErrorCode;
import recipe.core.api.patient.IOfflineRecipeBusinessService;
import recipe.enumerate.status.OfflineToOnlineEnum;
import recipe.factory.offlinetoonline.IOfflineToOnlineStrategy;
import recipe.factory.offlinetoonline.OfflineToOnlineFactory;
import recipe.manager.EmrRecipeManager;
import recipe.manager.HisRecipeManager;
import recipe.manager.RecipeManager;
import recipe.manager.RecipeTherapyManager;
import recipe.service.RecipeLogService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.MapValueUtil;
import recipe.vo.patient.RecipeGiveModeButtonRes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    protected RecipeManager recipeManager;
    @Autowired
    private RecipeTherapyManager recipeTherapyManager;
    @Autowired
    private EmrRecipeManager emrRecipeManager;

    @Override
    public List<MergeRecipeVO> findHisRecipeList(FindHisRecipeListVO request) {
        LOGGER.info("OfflineToOnlineService findHisRecipeList request:{}", JSONUtils.toString(request));
        try {
            // 1、公共参数获取
            PatientDTO patientDTO = obtainPatientInfo(request);
            // 2、获取his数据
            HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos = hisRecipeManager.queryData(request.getOrganId(), patientDTO, request.getTimeQuantum(), OfflineToOnlineEnum.getOfflineToOnlineType(request.getStatus()), null);
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
    public String getHandlerMode() {
        return null;
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
    public OffLineRecipeDetailVO getOffLineRecipeDetails(String mpiId, Integer clinicOrgan, String recipeCode) {
        logger.info("RecipeBusinessService getOffLineRecipeDetails mpiId={},clinicOrgan={},recipeCode={}", mpiId, clinicOrgan, recipeCode);
        PatientDTO patient = patientService.getPatientByMpiId(mpiId);
        if (ObjectUtils.isEmpty(patient)) {
            throw new DAOException(609, "患者信息不存在");
        }
//        //获取线下处方信息
        OffLineRecipeDetailDTO offLineRecipeDetailDTO = new OffLineRecipeDetailDTO();
        QueryHisRecipResTO queryHisRecipResTO = offlineRecipeClient.queryOffLineRecipeDetail(offLineRecipeDetailDTO, clinicOrgan, patient, 6, 2, recipeCode);

        //判断是否为儿科 设置部门名称
        DepartmentDTO departmentDTO = departmentService.getByCodeAndOrgan(queryHisRecipResTO.getDepartCode(), queryHisRecipResTO.getClinicOrgan());
        if (!ObjectUtils.isEmpty(departmentDTO)) {
            if (departmentDTO.getName().contains("儿科") || departmentDTO.getName().contains("新生儿科")
                    || departmentDTO.getName().contains("儿内科") || departmentDTO.getName().contains("儿外科")) {
                offLineRecipeDetailDTO.setChildRecipeFlag(true);
                //设置监护人字段
                if (!ObjectUtils.isEmpty(patient)) {
                    offLineRecipeDetailDTO.setGuardianName(patient.getGuardianName());
                    offLineRecipeDetailDTO.setGuardianAge(patient.getGuardianAge());
                    offLineRecipeDetailDTO.setGuardianSex(patient.getGuardianSex());
                }
            }
        }
        //优先使用HIS返回的departName如果为空则查表
        if (!org.springframework.util.StringUtils.isEmpty(queryHisRecipResTO.getDepartName())){
            offLineRecipeDetailDTO.setDepartName(queryHisRecipResTO.getDepartName());
        }else if (!org.springframework.util.StringUtils.isEmpty(departmentDTO.getName())){
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
                }else {
                    //如果二者都没有则不返回
                    priceNotExits=true;
                }
                RecipeDetailDTO recipeDetailDTO = new RecipeDetailDTO();
                BeanUtils.copy(drugList, recipeDetailDTO);
                //拼接中药名称
                if (RecipeTypeEnum.RECIPETYPE_WM.getType().equals(recipeType)) {
                    recipeDetailDTO.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(recipeDetailDTO, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(recipeType)));
                }
                detailDTOS.add(recipeDetailDTO);
            }
            offLineRecipeDetailDTO.setRecipeDetails(detailDTOS);
            if (priceNotExits){
                offLineRecipeDetailDTO.setTotalPrice(null);
            }else {
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
    public void pushRecipeExecute(Integer recipeId, Integer pushType) {
        RecipeBusiThreadPool.execute(() -> {
            logger.info("RecipeBusinessService pushTherapyRecipeExecute recipeId={}", recipeId);
            RecipeInfoDTO recipePdfDTO = recipeTherapyManager.getRecipeTherapyDTO(recipeId);
            try {
                RecipeInfoDTO result = hisRecipeManager.pushTherapyRecipe(recipePdfDTO, pushType);
                if (null == result) {
                    return;
                }
                recipeManager.updatePushHisRecipe(result.getRecipe(), recipeId, pushType);
                recipeManager.updatePushHisRecipeExt(result.getRecipeExtend(), recipeId, pushType);
                recipeTherapyManager.updatePushHisRecipeTherapy(result.getRecipeTherapy(), recipePdfDTO.getRecipeTherapy().getId(), pushType);
            } catch (Exception e) {
                Recipe recipe = recipePdfDTO.getRecipe();
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "当前处方推送his失败:" + e.getMessage());
            }
            emrRecipeManager.updateDisease(recipeId);
            logger.info("RecipeBusinessService pushTherapyRecipeExecute end recipeId:{}", recipeId);
        });
    }
}
