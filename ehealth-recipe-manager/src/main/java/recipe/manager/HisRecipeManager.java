package recipe.manager;

import com.alibaba.druid.sql.visitor.functions.Now;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.his.recipe.mode.RecipeDetailTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.dto.EmrDetailDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.*;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DocIndexClient;
import recipe.client.OfflineRecipeClient;
import recipe.client.PatientClient;
import recipe.client.RevisitClient;
import recipe.common.CommonConstant;
import recipe.dao.HisRecipeDAO;
import recipe.dao.HisRecipeDataDelDAO;
import recipe.dao.HisRecipeDetailDAO;
import recipe.dao.HisRecipeExtDAO;
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
    private DocIndexClient docIndexClient;
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private OfflineRecipeClient offlineRecipeClient;
    @Autowired
    private HisRecipeDAO hisRecipeDao;
    @Autowired
    private HisRecipeExtDAO hisRecipeExtDAO;
    @Autowired
    private HisRecipeDetailDAO hisRecipeDetailDAO;
    @Autowired
    private EmrRecipeManager emrRecipeManager;
    @Autowired
    private RevisitClient revisitClient;
    @Autowired
    private HisRecipeDataDelDAO hisRecipeDataDelDAO;


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
        HisResponseTO<List<QueryHisRecipResTO>> res = filterData(responseTo, recipeCode, flag);
        logger.info("HisRecipeManager res:{}.", JSONUtils.toString(res));
        return res;
    }

    /**
     * @param responseTo
     * @param flag
     * @return
     * @author liumin
     * @Description 数据过滤
     */
    private HisResponseTO<List<QueryHisRecipResTO>> filterData(HisResponseTO<List<QueryHisRecipResTO>> responseTo, String recipeCode, Integer flag) {
        logger.info("HisRecipeManager filterData responseTo:{},recipeCode:{}", JSONUtils.toString(responseTo), recipeCode);
        if (responseTo == null) {
            return responseTo;
        }
        List<QueryHisRecipResTO> queryHisRecipResTos = responseTo.getData();
        List<QueryHisRecipResTO> queryHisRecipResToFilters = new ArrayList<>();
        //获取详情时防止前置机没过滤数据，做过滤处理
        if (StringUtils.isNotEmpty(recipeCode)) {
            logger.info("HisRecipeManager queryHisRecipeInfo recipeCode:{}", recipeCode);
            //详情
            if (!CollectionUtils.isEmpty(queryHisRecipResTos)) {
                for (QueryHisRecipResTO queryHisRecipResTo : queryHisRecipResTos) {
                    if (recipeCode.equals(queryHisRecipResTo.getRecipeCode())) {
                        queryHisRecipResToFilters.add(queryHisRecipResTo);
                        continue;
                    }
                }
            }
            responseTo.setData(queryHisRecipResToFilters);
        }
        //列表
        if (StringUtils.isNotEmpty(recipeCode)) {
            //对状态过滤(1、测试桩会返回所有数据，不好测试，对测试造成干扰 2、也可以做容错处理)
            if (!CollectionUtils.isEmpty(queryHisRecipResTos)) {
                for (QueryHisRecipResTO queryHisRecipResTo : queryHisRecipResTos) {
                    if (flag.equals(queryHisRecipResTo.getStatus())) {
                        queryHisRecipResToFilters.add(queryHisRecipResTo);
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
     * 保存删除线下处方相关的数据
     */
    public void saveHisRecipeDataDel(List<Integer> hisRecipeIds,List<Recipe> recipeList){
        try{
            HisRecipeDataDel hisRecipeDataDel = new HisRecipeDataDel();
            List<HisRecipe> hisRecipes = hisRecipeDao.findHisRecipeByhisRecipeIds(hisRecipeIds);
            List<HisRecipeExt> hisRecipeExts = hisRecipeExtDAO.findHisRecipeByhisRecipeIds(hisRecipeIds);
            List<HisRecipeDetail> hisRecipeDetails = hisRecipeDetailDAO.findByHisRecipeIds(hisRecipeIds);
            Map<Integer, List<HisRecipeExt>> hisRecipeExtsMap = hisRecipeExts.stream().collect(Collectors.groupingBy(HisRecipeExt::getHisRecipeId));
            Map<Integer, List<HisRecipeDetail>> hisRecipeDetailsMap = hisRecipeDetails.stream().collect(Collectors.groupingBy(HisRecipeDetail::getHisRecipeId));
            Map<String, Recipe> recipeListMap = recipeList.stream().collect(Collectors.toMap(k -> k.getRecipeCode() + k.getClinicOrgan(), a -> a, (k1, k2) -> k1));

            for(HisRecipe hisRecipe : hisRecipes){
                hisRecipeDataDel.setRecipeId(recipeListMap.get(hisRecipe.getRecipeCode() + hisRecipe.getClinicOrgan()).getRecipeId());
                hisRecipeDataDel.setHisRecipeId(hisRecipe.getHisRecipeID());
                hisRecipeDataDel.setRecipeCode(hisRecipe.getRecipeCode());
                hisRecipeDataDel.setData(JSONUtils.toString(hisRecipe));
                hisRecipeDataDel.setTableName("cdr_his_recipe");
                hisRecipeDataDel.setCreateTime(new Date());
                hisRecipeDataDelDAO.save(hisRecipeDataDel);

                hisRecipeDataDel.setRecipeId(recipeListMap.get(hisRecipe.getRecipeCode() + hisRecipe.getClinicOrgan()).getRecipeId());
                hisRecipeDataDel.setHisRecipeId(hisRecipe.getHisRecipeID());
                hisRecipeDataDel.setData(JSONUtils.toString(hisRecipeExtsMap.get(hisRecipe.getHisRecipeID())));
                hisRecipeDataDel.setTableName("cdr_his_recipe_ext");
                hisRecipeDataDel.setRecipeCode(hisRecipe.getRecipeCode());
                hisRecipeDataDel.setCreateTime(new Date());
                hisRecipeDataDelDAO.save(hisRecipeDataDel);

                hisRecipeDataDel.setRecipeId(recipeListMap.get(hisRecipe.getRecipeCode() + hisRecipe.getClinicOrgan()).getRecipeId());
                hisRecipeDataDel.setHisRecipeId(hisRecipe.getHisRecipeID());
                hisRecipeDataDel.setData(JSONUtils.toString(hisRecipeDetailsMap.get(hisRecipe.getHisRecipeID())));
                hisRecipeDataDel.setTableName("cdr_his_recipedetail");
                hisRecipeDataDel.setRecipeCode(hisRecipe.getRecipeCode());
                hisRecipeDataDel.setCreateTime(new Date());
                hisRecipeDataDelDAO.save(hisRecipeDataDel);
            }
        }catch (Exception e){
            LOGGER.error("saveHisRecipeDataDel e={}",JSONUtils.toString(e));
        }
    }

    /**
     * 保存删除线上处方相关的数据
     */
    public void saveRecipeDataDel(List<Integer> recipeIds, List<String> orderCodeList ){
        try{
            HisRecipeDataDel hisRecipeDataDel = new HisRecipeDataDel();
            List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIds);
            Map<String, List<RecipeOrder>> orderCodeMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(orderCodeList)) {
                List<RecipeOrder> orderCodes = recipeOrderDAO.findByOrderCode(orderCodeList);
                orderCodeMap = orderCodes.stream().collect(Collectors.groupingBy(RecipeOrder::getOrderCode));
            }
            List<RecipeExtend> recipeExtends = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIds);
            Map<Integer, List<RecipeExtend>> recipeExtendsMap = recipeExtends.stream().collect(Collectors.groupingBy(RecipeExtend::getRecipeId));
            List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeIds(recipeIds);
            Map<Integer, List<Recipedetail>> recipeDetailsMap = recipeDetails.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));

            for(Recipe recipe : recipes){
                hisRecipeDataDel.setRecipeId(recipe.getRecipeId());
                hisRecipeDataDel.setData(JSONUtils.toString(recipe));
                hisRecipeDataDel.setTableName("cdr_recipe");
                hisRecipeDataDel.setRecipeCode(recipe.getRecipeCode());
                hisRecipeDataDel.setCreateTime(new Date());
                hisRecipeDataDelDAO.save(hisRecipeDataDel);

                hisRecipeDataDel.setRecipeId(recipe.getRecipeId());
                hisRecipeDataDel.setData(JSONUtils.toString(orderCodeMap.get(recipe.getOrderCode())));
                hisRecipeDataDel.setTableName("cdr_recipeorder");
                hisRecipeDataDel.setRecipeCode(recipe.getRecipeCode());
                hisRecipeDataDel.setCreateTime(new Date());
                hisRecipeDataDelDAO.save(hisRecipeDataDel);

                hisRecipeDataDel.setRecipeId(recipe.getRecipeId());
                hisRecipeDataDel.setData(JSONUtils.toString(recipeExtendsMap.get(recipe.getRecipeId())));
                hisRecipeDataDel.setTableName("cdr_recipe_ext");
                hisRecipeDataDel.setRecipeCode(recipe.getRecipeCode());
                hisRecipeDataDel.setCreateTime(new Date());
                hisRecipeDataDelDAO.save(hisRecipeDataDel);

                hisRecipeDataDel.setRecipeId(recipe.getRecipeId());
                hisRecipeDataDel.setData(JSONUtils.toString(recipeDetailsMap.get(recipe.getRecipeId())));
                hisRecipeDataDel.setTableName("cdr_recipedetail");
                hisRecipeDataDel.setRecipeCode(recipe.getRecipeCode());
                hisRecipeDataDel.setCreateTime(new Date());
                hisRecipeDataDelDAO.save(hisRecipeDataDel);
            }
        }catch (Exception e){
            LOGGER.error("saveRecipeDataDel e={}",JSONUtils.toString(e));
        }
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
        List<Recipe> recipeList = recipeDAO.findByRecipeCodeAndClinicOrgan(recipeCodeList, clinicOrgan);
        saveHisRecipeDataDel(hisRecipeIds,recipeList);
        hisRecipeExtDAO.deleteByHisRecipeIds(hisRecipeIds);
        hisRecipeDetailDAO.deleteByHisRecipeIds(hisRecipeIds);
        hisRecipeDao.deleteByHisRecipeIds(hisRecipeIds);
        if (CollectionUtils.isEmpty(recipeList)) {
            return;
        }
        List<Integer> recipeIds = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        LOGGER.info("deleteSetRecipeCode recipeIds:{}", JSONUtils.toString(recipeIds));
        List<String> orderCodeList = recipeList.stream().filter(a -> StringUtils.isNotEmpty(a.getOrderCode())).map(Recipe::getOrderCode).collect(Collectors.toList());
        saveRecipeDataDel(recipeIds,orderCodeList);
        if (CollectionUtils.isNotEmpty(orderCodeList)) {
            recipeOrderDAO.deleteByRecipeIds(orderCodeList);
        }
        recipeExtendDAO.deleteByRecipeIds(recipeIds);
        recipeDetailDAO.deleteByRecipeIds(recipeIds);
        recipeDAO.deleteByRecipeIds(recipeIds);
        //日志记录
        Map<Integer, Recipe> recipeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getRecipeId, Function.identity(), (key1, key2) -> key2));
        recipeIds.forEach(a -> {
            RecipeLog recipeLog = new RecipeLog();
            recipeLog.setRecipeId(a);
            recipeLog.setBeforeStatus(recipeMap.get(a).getStatus());
            recipeLog.setAfterStatus(RecipeStatusEnum.RECIPE_STATUS_DELETE.getType());
            recipeLog.setMemo("线下转线上：修改处方状态为已删除,数据是：" + JSONUtils.toString(recipeMap.get(a)));
            recipeLogDao.saveRecipeLog(recipeLog);
            revisitClient.deleteByBusIdAndBusNumOrder(a);
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

    /**
     * 根据机构+mpiId+recipeCode获取HisRecipe
     *
     * @param organId
     * @param mpiId
     * @param recipeCode
     * @return
     */
    public HisRecipe obatainHisRecipeByOrganIdAndMpiIdAndRecipeCode(Integer organId, String mpiId, String recipeCode) {
        LOGGER.info("HisRecipeManager obatainHisRecipeByOrganIdAndMpiIdAndRecipeCode organId:{},mpiId:{},recipeCode:{}", organId, mpiId, recipeCode);
        HisRecipe hisRecipe = hisRecipeDao.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(mpiId, organId, recipeCode);
        LOGGER.info("HisRecipeManager obatainHisRecipeByOrganIdAndMpiIdAndRecipeCode hisRecipe:{}", JSONUtils.toString(hisRecipe));
        return hisRecipe;
    }


    /**
     * 推送处方给his
     *
     * @param recipePdfDTO  处方信息
     * @param pushType      推送类型: 1：提交处方，2:撤销处方
     * @param pharmacyIdMap 药房
     * @return
     * @throws Exception
     */
    public RecipeInfoDTO pushRecipe(RecipeInfoDTO recipePdfDTO, Integer pushType, Map<Integer, PharmacyTcm> pharmacyIdMap, Integer sysType) throws Exception {
        EmrDetailDTO emrDetail = emrDetail(recipePdfDTO);
        if (CommonConstant.RECIPE_DOCTOR_TYPE.equals(sysType)) {
            return offlineRecipeClient.pushRecipe(pushType, recipePdfDTO, emrDetail, pharmacyIdMap);
        } else {
            return offlineRecipeClient.patientPushRecipe(pushType, recipePdfDTO, emrDetail, pharmacyIdMap);
        }
    }


    /**
     * 获取 电子病历信息诊断等
     *
     * @param recipePdfDTO 处方信息
     * @return 电子病历信息
     */
    private EmrDetailDTO emrDetail(RecipeInfoDTO recipePdfDTO) {
        RecipeExtend recipeExtend = recipePdfDTO.getRecipeExtend();
        if (null == recipeExtend) {
            return null;
        }
        return docIndexClient.getEmrDetails(recipeExtend.getDocIndexId());
    }
}
