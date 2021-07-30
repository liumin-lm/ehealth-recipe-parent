package recipe.manager;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.his.recipe.mode.RecipeDetailTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.entity.*;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.OfflineRecipeClient;
import recipe.client.PatientClient;
import recipe.dao.*;
import recipe.enumerate.status.OfflineToOnlineEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author liumin
 * @Date 2021/7/20 下午5:30
 * @Description
 */
@Service
public class HisRecipeManager extends BaseManager {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    PatientClient patientClient;

    @Autowired
    OfflineRecipeClient offlineRecipeClient;

    @Autowired
    HisRecipeDAO hisRecipeDao;

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
    private RecipeLogDAO recipeLogDao;

    @Autowired
    private EmrRecipeManager emrRecipeManager;


    /**
     * 获取患者信息
     *
     * @param mpiId
     * @return
     */
    public PatientDTO getPatientBeanByMpiId(String mpiId) {
        return patientClient.getPatientBeanByMpiId(mpiId);
    }

    /**
     * 查询线下处方数据
     *
     * @param organId
     * @param patientDTO
     * @param timeQuantum
     * @param flag
     * @param recipeCode
     * @return
     */
    public HisResponseTO<List<QueryHisRecipResTO>> queryData(Integer organId, PatientDTO patientDTO, Integer timeQuantum, Integer flag, String recipeCode) {
        LOGGER.info("HisRecipeManager queryData param organId:{},patientDTO:{},timeQuantum:{},flag:{},recipeCode:{}", organId, JSONUtils.toString(patientDTO), timeQuantum, flag, recipeCode);
        HisResponseTO<List<QueryHisRecipResTO>> responseTo = offlineRecipeClient.queryData(organId, patientDTO, timeQuantum, flag, recipeCode);
        //过滤数据
        HisResponseTO<List<QueryHisRecipResTO>> res = filterData(responseTo, recipeCode);
        logger.info("HisRecipeManager res:{}.", JSONUtils.toString(res));
        return res;
    }

    /**
     * @param responseTo
     * @return
     * @author liumin
     * @Description 数据过滤
     */
    private HisResponseTO<List<QueryHisRecipResTO>> filterData(HisResponseTO<List<QueryHisRecipResTO>> responseTo, String recipeCode) {
        logger.info("HisRecipeManager filterData responseTo:{},recipeCode:{}", JSONUtils.toString(responseTo), recipeCode);
        //获取详情时防止前置机没过滤数据，做过滤处理
        if (responseTo != null && recipeCode != null) {
            logger.info("HisRecipeManager queryHisRecipeInfo recipeCode:{}", recipeCode);
            List<QueryHisRecipResTO> queryHisRecipResTos = responseTo.getData();
            List<QueryHisRecipResTO> queryHisRecipResToFilters = new ArrayList<>();
            if (!CollectionUtils.isEmpty(queryHisRecipResTos) && queryHisRecipResTos.size() > 1) {
                for (QueryHisRecipResTO queryHisRecipResTo : queryHisRecipResTos) {
                    if (recipeCode.equals(queryHisRecipResTo.getRecipeCode())) {
                        queryHisRecipResToFilters.add(queryHisRecipResTo);
                        continue;
                    }
                }
            }
            responseTo.setData(queryHisRecipResToFilters);
        }
        logger.info("HisRecipeManager filterData:{}.", JSONUtils.toString(responseTo));
        return responseTo;
    }

    /**
     * 获取线下处方
     *
     * @param mpiId
     * @param clinicOrgan
     * @param recipeCode
     * @return
     */
    public HisRecipe getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(String mpiId, Integer clinicOrgan, String recipeCode) {
        return hisRecipeDao.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(mpiId, clinicOrgan, recipeCode);
    }

    /**
     * 获取未处理的线下处方
     *
     * @param clinicOrgan
     * @param recipeCodeList
     * @return
     */
    public List<HisRecipe> findNoDealHisRecipe(Integer clinicOrgan, List<String> recipeCodeList) {
        LOGGER.info("HisRecipeManager findNoDealHisRecipe param clinicOrgan:{},recipeCodeList:{}", clinicOrgan, JSONUtils.toString(recipeCodeList));
        List<HisRecipe> hisRecipes = hisRecipeDao.findNoDealHisRecipe(clinicOrgan, recipeCodeList);
        LOGGER.info("HisRecipeManager findNoDealHisRecipe res hisRecipes:{}", JSONUtils.toString(hisRecipes));
        return hisRecipes;
    }

    /**
     * 更新诊断字段
     *
     * @param hisRecipeTO  his处方数据
     * @param hisRecipeMap key为未处理recipeCode,值为未处理HisRecipe的map对象
     * @param recipeList   平台处方
     */
    public Map<String, HisRecipe> updateDisease(List<QueryHisRecipResTO> hisRecipeTO, List<Recipe> recipeList, Map<String, HisRecipe> hisRecipeMap) {
        LOGGER.info("HisRecipeManager updateHisRecipe param hisRecipeTO:{},recipeList:{},hisRecipeMap:{}", JSONUtils.toString(hisRecipeTO), JSONUtils.toString(recipeList), JSONUtils.toString(hisRecipeMap));
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
                hisRecipeDao.update(hisRecipe);

                LOGGER.info("updateHisRecipe hisRecipe = {}", JSONUtils.toString(hisRecipe));
                Recipe recipe = recipeMap.get(a.getRecipeCode());
                recipe.setOrganDiseaseId(disease);
                recipe.setOrganDiseaseName(diseaseName);
                recipeDAO.update(recipe);

                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
                recipeExtendDAO.saveOrUpdateRecipeExtend(recipeExtend);

                emrRecipeManager.saveMedicalInfo(recipe, recipeExtend);
            }
        });
        LOGGER.info("HisRecipeManager updateHisRecipe response hisRecipeMap:{}", JSONUtils.toString(hisRecipeMap));
        return hisRecipeMap;
    }

    /**
     * 删除未支付处方，同时判断是否存在已缴费处方 若存在 返回true
     *
     * @param organId     机构id
     * @param recipeCodes 需删除处方号
     * @return
     */
    public void deleteRecipeByRecipeCodes(String organId, List<String> recipeCodes) {
        logger.info("HisRecipeManager deleteRecipeByRecipeCodes param organId:{},recipeCodes:{}", organId, JSONUtils.toString(recipeCodes));
        //默认不存在
        boolean isExistPayRecipe = false;
        List<Recipe> recipes = recipeDAO.findRecipeByRecipeCodeAndClinicOrgan(Integer.parseInt(organId), recipeCodes);
        if (CollectionUtils.isNotEmpty(recipes) && recipes.size() > 0) {
            //存在已支付处方出现在（待处理列表） 提示用户刷新列表
            isExistPayRecipe = true;
        }
        if (isExistPayRecipe) {
            throw new DAOException(609, "处方单已经缴费，请刷新重试");
        }
        //2 删除数据
        deleteSetRecipeCode(Integer.parseInt(organId), new HashSet<>(recipeCodes));
        logger.info("HisRecipeManager deleteRecipeByRecipeCodes 方法结束");
    }

    /**
     * 删除线下处方相关数据
     *
     * @param clinicOrgan         机构id
     * @param deleteSetRecipeCode 要删除的recipeCodes
     */
    public void deleteSetRecipeCode(Integer clinicOrgan, Set<String> deleteSetRecipeCode) {
        LOGGER.info("HisRecipeManager deleteSetRecipeCode clinicOrgan = {},deleteSetRecipeCode = {}", clinicOrgan, JSONUtils.toString(deleteSetRecipeCode));
        if (CollectionUtils.isEmpty(deleteSetRecipeCode)) {
            return;
        }
        List<String> recipeCodeList = new ArrayList<>(deleteSetRecipeCode);
        List<HisRecipe> hisRecipeList = hisRecipeDao.findHisRecipeByRecipeCodeAndClinicOrgan(clinicOrgan, recipeCodeList);
        List<Integer> hisRecipeIds = hisRecipeList.stream().map(HisRecipe::getHisRecipeID).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hisRecipeIds)) {
            LOGGER.info("deleteSetRecipeCode 查找无处方");
            return;
        }
        hisRecipeExtDAO.deleteByHisRecipeIds(hisRecipeIds);
        hisRecipeDetailDAO.deleteByHisRecipeIds(hisRecipeIds);
        hisRecipeDao.deleteByHisRecipeIds(hisRecipeIds);
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
        Map<Integer, Recipe> recipeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getRecipeId, Function.identity(), (key1, key2) -> key2));
        recipeIds.forEach(a -> {
            RecipeLog recipeLog = new RecipeLog();
            recipeLog.setRecipeId(a);
            recipeLog.setBeforeStatus(recipeMap.get(a).getStatus());
            recipeLog.setAfterStatus(RecipeStatusEnum.RECIPE_STATUS_DELETE.getType());
            recipeLog.setMemo("线下转线上：修改处方状态为已删除");
            recipeLogDao.saveRecipeLog(recipeLog);

        });
        LOGGER.info("HisRecipeManager deleteSetRecipeCode is delete end ");
    }

    /**
     * 获取处方id
     *
     * @param organId    机构
     * @param recipeCode 处方号
     * @param hisRecipes 线下处方
     * @return
     */
    public Integer attachRecipeId(Integer organId, String recipeCode, List<HisRecipe> hisRecipes) {
        LOGGER.info("HisRecipeManager attachRecipeId organId:{},recipeCode:{},hisRecipes:{}", organId, recipeCode, JSONUtils.toString(hisRecipes));
        Integer hisRecipeId = null;
        HisRecipe hisRecipe = new HisRecipe();
        if (CollectionUtils.isEmpty(hisRecipes)) {
            //点击卡片 历史处方his不会返回 故从表查  同时也兼容已处理状态的处方，前端漏传hisRecipeId的情况
            if (!StringUtils.isEmpty(recipeCode)) {
                hisRecipe = hisRecipeDao.getHisRecipeByRecipeCodeAndClinicOrgan(organId, recipeCode);
            }
            if (hisRecipe != null) {
                hisRecipeId = hisRecipe.getHisRecipeID();
            }
        } else {
            hisRecipeId = hisRecipes.get(0).getHisRecipeID();
        }
        LOGGER.info("HisRecipeManager attachRecipeId hisRecipeId:{}", hisRecipeId);
        return hisRecipeId;
    }

    /**
     * 根据处方号获取状态
     *
     * @param mpiId      患者mpiid
     * @param organCode  查询机构
     * @param recipeCode 查询处方号
     * @return
     */
    public String attachHisRecipeStatus(String mpiId, Integer organCode, String recipeCode) {
        LOGGER.info("HisRecipeManager attachHisRecipeStatus param mpiId:{},organCode:{},recipeCode:{}", mpiId, organCode, recipeCode);
        String status = "";
        HisRecipe hisRecipe = hisRecipeDao.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(mpiId, organCode, recipeCode);
        if (hisRecipe != null) {
            //已处理
            if (OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ALREADY_PAY.getType().equals(hisRecipe.getStatus())) {
                status = OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ALREADY_PAY.getName();
            }
            //待处理或进行中
            if (OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getType().equals(hisRecipe.getStatus())) {
                Recipe recipe = recipeDAO.getByHisRecipeCodeAndClinicOrganAndMpiid(mpiId, recipeCode, organCode);
                if (recipe != null && !StringUtils.isEmpty(recipe.getOrderCode())) {
                    status = OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ONGOING.getName();
                } else {
                    status = OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getName();
                }
            }
        } else {
            status = OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getName();
        }
        if (StringUtils.isEmpty(status)) {
            LOGGER.info("attachHisRecipeStatus 根据处方单号获取不到状态");
            throw new DAOException(recipe.constant.ErrorCode.SERVICE_ERROR, "参数异常，请刷新页面后重试");
        }
        LOGGER.info("HisRecipeManager attachHisRecipeStatus res status:{}", status);
        return status;
    }

    /**
     * 获取需要删除的recipeCodes
     * 判断药品详情发生变化、数据不是由本人生成的未支付处方、中药tcmFee变更
     *
     * @param hisRecipeTO         his处方数据
     * @param hisRecipeMap        key为未处理recipeCode,值为未处理HisRecipe的map对象
     * @param hisRecipeDetailList 未处理的线下处方详情
     * @param mpiId               查看详情处方的操作用户的mpiid
     * @return
     */
    public Set<String> obtainDeleteRecipeCodes(List<QueryHisRecipResTO> hisRecipeTO, Map<String, HisRecipe> hisRecipeMap, List<HisRecipeDetail> hisRecipeDetailList, String mpiId) {
        LOGGER.info("HisRecipeManager deleteSetRecipeCode hisRecipeTO:{},hisRecipeMap:{},hisRecipeDetailList:{},mpiId:{}", JSONUtils.toString(hisRecipeTO), JSONUtils.toString(hisRecipeMap), JSONUtils.toString(hisRecipeDetailList), mpiId);
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
            if (a.getStatus() == 2) {
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
                if (0 != MapValueUtil.covertBigdecimal(hisRecipeDetail.getUseTotalDose()).compareTo(MapValueUtil.covertBigdecimal(recipeDetailTO.getUseTotalDose()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useTotalDose recipeCode:{}", recipeCode);
                    continue;
                }
                if (!MapValueUtil.covertString(hisRecipeDetail.getUseDose()).equals(MapValueUtil.covertString(recipeDetailTO.getUseDose()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDose recipeCode:{}", recipeCode);
                    continue;
                }
                if ((!MapValueUtil.covertString(hisRecipeDetail.getUseDoseStr()).equals(MapValueUtil.covertString(recipeDetailTO.getUseDoseStr())))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDoseStr recipeCode:{}", recipeCode);
                    continue;
                }
                if ((!MapValueUtil.covertString(hisRecipeDetail.getUseDaysB()).equals(MapValueUtil.covertString(recipeDetailTO.getUseDaysB())))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDaysB recipeCode:{}", recipeCode);
                    continue;
                }

                if ((MapValueUtil.covertInteger(hisRecipeDetail.getUseDays()) != MapValueUtil.covertInteger(recipeDetailTO.getUseDays()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDays recipeCode:{}", recipeCode);
                    continue;
                }

                if (!MapValueUtil.covertString(hisRecipeDetail.getUsingRate()).equals(MapValueUtil.covertString(recipeDetailTO.getUsingRate()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause usingRate recipeCode:{}", recipeCode);
                    continue;
                }

                if (!MapValueUtil.covertString(hisRecipeDetail.getUsingRateText()).equals(MapValueUtil.covertString(recipeDetailTO.getUsingRateText()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause usingRateText recipeCode:{}", recipeCode);
                    continue;
                }
                if (!MapValueUtil.covertString(hisRecipeDetail.getUsePathways()).equals(MapValueUtil.covertString(recipeDetailTO.getUsePathWays()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause usePathWays recipeCode:{}", recipeCode);
                    continue;
                }
                if (!MapValueUtil.covertString(hisRecipeDetail.getUsePathwaysText()).equals(MapValueUtil.covertString(recipeDetailTO.getUsePathwaysText()))) {
                    LOGGER.info("deleteSetRecipeCode cause usePathwaysText recipeCode:{}", recipeCode);
                    deleteSetRecipeCode.add(recipeCode);
                }
            }
            //中药判断tcmFee发生变化,删除数据
            BigDecimal tcmFee = a.getTcmFee();
            if (MapValueUtil.covertBigdecimal(tcmFee).compareTo(MapValueUtil.covertBigdecimal(hisRecipe.getTcmFee())) != 0) {
                LOGGER.info("deleteSetRecipeCode cause tcmFee recipeCode:{}", recipeCode);
                deleteSetRecipeCode.add(hisRecipe.getRecipeCode());
            }
        });
        LOGGER.info("HisRecipeManager deleteSetRecipeCode res:{}", JSONUtils.toString(deleteSetRecipeCode));
        return deleteSetRecipeCode;
    }

}
