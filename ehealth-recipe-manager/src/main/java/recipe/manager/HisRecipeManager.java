package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.HisCheckRecipeReqTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.his.recipe.mode.RecipeDetailTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.RecipeBean;
import com.ngari.platform.recipe.mode.RecipeDTO;
import com.ngari.platform.recipe.mode.RecipeDetailBean;
import com.ngari.platform.recipe.mode.RecipeExtendBean;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.event.GlobalEventExecFactory;
import eh.cdr.constant.RecipeConstant;
import eh.entity.base.UsePathways;
import eh.entity.base.UsingRate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import recipe.aop.LogRecord;
import recipe.client.DocIndexClient;
import recipe.client.PayClient;
import recipe.common.CommonConstant;
import recipe.constant.ErrorCode;
import recipe.constant.HisRecipeConstant;
import recipe.constant.OrderStatusConstant;
import recipe.constant.PayConstant;
import recipe.dao.*;
import recipe.enumerate.status.*;
import recipe.enumerate.type.BussSourceTypeEnum;
import recipe.enumerate.type.OfflineRecipePayFlagEnum;
import recipe.enumerate.type.PayFlagEnum;
import recipe.enumerate.type.RecipeDrugFormTypeEnum;
import recipe.util.JsonUtil;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
    private HisRecipeExtDAO hisRecipeExtDAO;
    @Autowired
    private HisRecipeDetailDAO hisRecipeDetailDAO;
    @Autowired
    private HisRecipeDataDelDAO hisRecipeDataDelDAO;
    @Autowired
    private StateManager stateManager;
    @Autowired
    private RecipeParameterDao recipeParameterDao;
    @Autowired
    private PayClient payClient;
    @Autowired
    private RecipeBeforeOrderDAO recipeBeforeOrderDAO;
    @Autowired
    private RecipeManager recipeManager;

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
     * 统一查询线下处方数据
     *
     * @param organId
     * @param patientDTO
     * @param timeQuantum
     * @param flag
     * @param recipeCode
     * @return
     */
    public HisResponseTO<List<QueryHisRecipResTO>> queryData(Integer organId, PatientDTO patientDTO, Integer timeQuantum, Integer flag, String recipeCode,Date startDate,Date endDate) {
        LOGGER.info("HisRecipeManager queryData param organId:{},patientDTO:{},timeQuantum:{},flag:{},recipeCode:{}", organId, JSONUtils.toString(patientDTO), timeQuantum, flag, recipeCode);
        HisResponseTO<List<QueryHisRecipResTO>> responseTo = offlineRecipeClient.queryData(organId, patientDTO, timeQuantum, flag, recipeCode,startDate,endDate);
        //过滤数据
        HisResponseTO<List<QueryHisRecipResTO>> res = filterData(responseTo, recipeCode, flag);
        logger.info("HisRecipeManager res:{}.", JSONUtils.toString(res));
        return res;
    }

    /**
     * 列表查询--线下处方
     *
     * @param organId
     * @param patientDTO
     * @param timeQuantum
     * @param flag
     * @param recipeCode
     * @return
     */
    @LogRecord
    public HisResponseTO<List<QueryHisRecipResTO>> queryHisRecipeData(Integer organId, PatientDTO patientDTO, Integer timeQuantum, Integer flag, String recipeCode) {
        LOGGER.info("HisRecipeManager queryHisRecipeData param organId:{},patientDTO:{},timeQuantum:{},flag:{},recipeCode:{}", organId, JSONUtils.toString(patientDTO), timeQuantum, flag, recipeCode);
        HisResponseTO<List<QueryHisRecipResTO>> responseTo = new HisResponseTO<List<QueryHisRecipResTO>>();
        if (HisRecipeConstant.HISRECIPESTATUS_NOIDEAL.equals(flag)) {
            responseTo = offlineRecipeClient.queryData(organId, patientDTO, timeQuantum, flag, recipeCode,null,null);
            //过滤数据
            responseTo = filterData(responseTo, recipeCode, flag);
        } else if (HisRecipeConstant.HISRECIPESTATUS_ALREADYIDEAL.equals(flag)) {
            List<QueryHisRecipResTO> queryHisRecipResToList = new ArrayList<>();
            Future<HisResponseTO<List<QueryHisRecipResTO>>> hisTask1 = GlobalEventExecFactory.instance().getExecutor().submit(() -> {
                return offlineRecipeClient.queryData(organId, patientDTO, timeQuantum, HisRecipeConstant.HISRECIPESTATUS_ALREADYIDEAL, recipeCode,null,null);
            });
            Future<HisResponseTO<List<QueryHisRecipResTO>>> hisTask2 = GlobalEventExecFactory.instance().getExecutor().submit(() -> {
                return offlineRecipeClient.queryData(organId, patientDTO, timeQuantum, HisRecipeConstant.HISRECIPESTATUS_EXPIRED, recipeCode,null,null);
            });
            try {
                HisResponseTO<List<QueryHisRecipResTO>> hisResponseTO1 = hisTask1.get(60000, TimeUnit.MILLISECONDS);
                //过滤数据
                HisResponseTO<List<QueryHisRecipResTO>> res = filterData(hisResponseTO1, recipeCode, HisRecipeConstant.HISRECIPESTATUS_ALREADYIDEAL);
                queryHisRecipResToList.addAll(res.getData());
            } catch (Exception e) {
                logger.error("queryHisRecipeData hisTask1 error ", e);
                e.printStackTrace();
            }
            try {
                HisResponseTO<List<QueryHisRecipResTO>> hisResponseTO2 = hisTask2.get(60000, TimeUnit.MILLISECONDS);
                //过滤数据
                HisResponseTO<List<QueryHisRecipResTO>> res = filterData(hisResponseTO2, recipeCode, HisRecipeConstant.HISRECIPESTATUS_EXPIRED);
                queryHisRecipResToList.addAll(res.getData());
            } catch (Exception e) {
                logger.error("queryHisRecipeData hisTask2 error ", e);
                e.printStackTrace();
            }
            responseTo.setData(queryHisRecipResToList);

        }

        logger.info("HisRecipeManager res:{}.", JSONUtils.toString(responseTo));
        return responseTo;
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
            return null;
        }
        List<QueryHisRecipResTO> queryHisRecipeResTos = responseTo.getData();
        List<QueryHisRecipResTO> queryHisRecipeResToFilters = new ArrayList<>();
        //获取详情时防止前置机没过滤数据，做过滤处理
        if (StringUtils.isNotEmpty(recipeCode)) {
            //详情
            if (!CollectionUtils.isEmpty(queryHisRecipeResTos)) {
                for (QueryHisRecipResTO queryHisRecipResTo : queryHisRecipeResTos) {
                    if (recipeCode.equals(queryHisRecipResTo.getRecipeCode())) {
                        queryHisRecipeResToFilters.add(queryHisRecipResTo);
                        continue;
                    }
                }
            }
            responseTo.setData(queryHisRecipeResToFilters);
        } else {
            //对状态过滤(1、测试桩会返回所有数据，不好测试，对测试造成干扰 2、也可以做容错处理)
            if (!CollectionUtils.isEmpty(queryHisRecipeResTos)) {
                for (QueryHisRecipResTO queryHisRecipResTo : queryHisRecipeResTos) {
                    if (flag.equals(queryHisRecipResTo.getStatus())) {
                        queryHisRecipeResToFilters.add(queryHisRecipResTo);
                    }
                }
            }
            responseTo.setData(queryHisRecipeResToFilters);
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
        return hisRecipeDAO.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(mpiId, clinicOrgan, recipeCode);
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
                hisRecipeDAO.update(hisRecipe);

                LOGGER.info("updateHisRecipe hisRecipe = {}", JSONUtils.toString(hisRecipe));
                Recipe recipe = recipeMap.get(a.getRecipeCode());
                recipe.setOrganDiseaseId(disease);
                recipe.setOrganDiseaseName(diseaseName);
                recipeDAO.update(recipe);

                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
                recipeExtendDAO.saveOrUpdateRecipeExtend(recipeExtend);
                try {
                    if (null != recipeExtend.getDocIndexId()) {
                        return;
                    }
                    String doctorName = doctorClient.getDoctor(recipe.getDoctor()).getName();
                    docIndexClient.addMedicalInfo(recipe, recipeExtend, doctorName);
                } catch (Exception e) {
                    logger.error("HisRecipeManager updateHisRecipe 电子病历保存失败", e);
                }
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
    public void saveHisRecipeDataDel(List<Integer> hisRecipeIds, List<Recipe> recipeList) {
        try {
            HisRecipeDataDel hisRecipeDataDel = new HisRecipeDataDel();
            List<HisRecipe> hisRecipes = hisRecipeDAO.findHisRecipeByhisRecipeIds(hisRecipeIds);
            List<HisRecipeExt> hisRecipeExts = hisRecipeExtDAO.findHisRecipeByhisRecipeIds(hisRecipeIds);
            List<HisRecipeDetail> hisRecipeDetails = hisRecipeDetailDAO.findNoPayByHisRecipeIds(hisRecipeIds);
            Map<Integer, List<HisRecipeExt>> hisRecipeExtsMap = hisRecipeExts.stream().collect(Collectors.groupingBy(HisRecipeExt::getHisRecipeId));
            Map<Integer, List<HisRecipeDetail>> hisRecipeDetailsMap = hisRecipeDetails.stream().collect(Collectors.groupingBy(HisRecipeDetail::getHisRecipeId));
            Map<String, Recipe> recipeListMap = recipeList.stream().collect(Collectors.toMap(k -> k.getRecipeCode() + k.getClinicOrgan(), a -> a, (k1, k2) -> k1));

            for (HisRecipe hisRecipe : hisRecipes) {
                hisRecipeDataDel.setRecipeId(recipeListMap.get(hisRecipe.getRecipeCode() + hisRecipe.getClinicOrgan()).getRecipeId());
                hisRecipeDataDel.setHisRecipeId(hisRecipe.getHisRecipeID());
                hisRecipeDataDel.setRecipeCode(hisRecipe.getRecipeCode());
                hisRecipeDataDel.setData(JSON.toJSONString(hisRecipe));
                hisRecipeDataDel.setTableName("cdr_his_recipe");
                hisRecipeDataDel.setCreateTime(new Date());
                hisRecipeDataDelDAO.save(hisRecipeDataDel);

                hisRecipeDataDel.setRecipeId(recipeListMap.get(hisRecipe.getRecipeCode() + hisRecipe.getClinicOrgan()).getRecipeId());
                hisRecipeDataDel.setHisRecipeId(hisRecipe.getHisRecipeID());
                hisRecipeDataDel.setData(JSON.toJSONString(hisRecipeExtsMap.get(hisRecipe.getHisRecipeID())));
                hisRecipeDataDel.setTableName("cdr_his_recipe_ext");
                hisRecipeDataDel.setRecipeCode(hisRecipe.getRecipeCode());
                hisRecipeDataDel.setCreateTime(new Date());
                hisRecipeDataDelDAO.save(hisRecipeDataDel);

                hisRecipeDataDel.setRecipeId(recipeListMap.get(hisRecipe.getRecipeCode() + hisRecipe.getClinicOrgan()).getRecipeId());
                hisRecipeDataDel.setHisRecipeId(hisRecipe.getHisRecipeID());
                hisRecipeDataDel.setData(JSON.toJSONString(hisRecipeDetailsMap.get(hisRecipe.getHisRecipeID())));
                hisRecipeDataDel.setTableName("cdr_his_recipedetail");
                hisRecipeDataDel.setRecipeCode(hisRecipe.getRecipeCode());
                hisRecipeDataDel.setCreateTime(new Date());
                hisRecipeDataDelDAO.save(hisRecipeDataDel);
            }
        } catch (Exception e) {
            LOGGER.error("saveHisRecipeDataDel e={}", JsonUtil.toString(e));
        }
    }

    /**
     * 保存删除线上处方相关的数据
     */
    public void saveRecipeDataDel(List<Integer> recipeIds, List<String> orderCodeList) {
        try {
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

            for (Recipe recipe : recipes) {
                hisRecipeDataDel.setRecipeId(recipe.getRecipeId());
                hisRecipeDataDel.setData(JSON.toJSONString(recipe));
                hisRecipeDataDel.setTableName("cdr_recipe");
                hisRecipeDataDel.setRecipeCode(recipe.getRecipeCode());
                hisRecipeDataDel.setCreateTime(new Date());
                hisRecipeDataDelDAO.save(hisRecipeDataDel);

                hisRecipeDataDel.setRecipeId(recipe.getRecipeId());
                hisRecipeDataDel.setData(JSON.toJSONString(orderCodeMap.get(recipe.getOrderCode())));
                hisRecipeDataDel.setTableName("cdr_recipeorder");
                hisRecipeDataDel.setRecipeCode(recipe.getRecipeCode());
                hisRecipeDataDel.setCreateTime(new Date());
                hisRecipeDataDelDAO.save(hisRecipeDataDel);

                hisRecipeDataDel.setRecipeId(recipe.getRecipeId());
                hisRecipeDataDel.setData(JSON.toJSONString(recipeExtendsMap.get(recipe.getRecipeId())));
                hisRecipeDataDel.setTableName("cdr_recipe_ext");
                hisRecipeDataDel.setRecipeCode(recipe.getRecipeCode());
                hisRecipeDataDel.setCreateTime(new Date());
                hisRecipeDataDelDAO.save(hisRecipeDataDel);

                hisRecipeDataDel.setRecipeId(recipe.getRecipeId());
                hisRecipeDataDel.setData(JSON.toJSONString(recipeDetailsMap.get(recipe.getRecipeId())));
                hisRecipeDataDel.setTableName("cdr_recipedetail");
                hisRecipeDataDel.setRecipeCode(recipe.getRecipeCode());
                hisRecipeDataDel.setCreateTime(new Date());
                hisRecipeDataDelDAO.save(hisRecipeDataDel);
            }
        } catch (Exception e) {
            LOGGER.error("saveRecipeDataDel e={}", JSON.toJSONString(e));
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
        List<HisRecipe> hisRecipeList = hisRecipeDAO.findHisRecipeByRecipeCodeAndClinicOrgan(clinicOrgan, recipeCodeList);
        List<Integer> hisRecipeIds = hisRecipeList.stream().map(HisRecipe::getHisRecipeID).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hisRecipeIds)) {
            LOGGER.info("deleteSetRecipeCode 查找无处方");
            return;
        }
        List<Recipe> recipeList = recipeDAO.findByRecipeCodeAndClinicOrgan(recipeCodeList, clinicOrgan);
        saveHisRecipeDataDel(hisRecipeIds, recipeList);
        hisRecipeExtDAO.deleteByHisRecipeIds(hisRecipeIds);
        hisRecipeDetailDAO.deleteByHisRecipeIds(hisRecipeIds);
        hisRecipeDAO.deleteByHisRecipeIds(hisRecipeIds);
        if (CollectionUtils.isEmpty(recipeList)) {
            return;
        }
        List<Integer> recipeIds = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        LOGGER.info("deleteSetRecipeCode recipeIds:{}", JSONUtils.toString(recipeIds));
        List<String> orderCodeList = recipeList.stream().filter(a -> StringUtils.isNotEmpty(a.getOrderCode())).map(Recipe::getOrderCode).collect(Collectors.toList());
        saveRecipeDataDel(recipeIds, orderCodeList);
        if (CollectionUtils.isNotEmpty(orderCodeList)) {
            recipeOrderDAO.deleteByRecipeIds(orderCodeList);
        }
        recipeExtendDAO.deleteByRecipeIds(recipeIds);
        recipeDetailDAO.deleteByRecipeIds(recipeIds);
        recipeDAO.deleteByRecipeIds(recipeIds);
        if (CollectionUtils.isNotEmpty(recipeIds)) {
            recipeBeforeOrderDAO.updateDeleteFlagByRecipeId(recipeIds);
        }
        //日志记录
        Map<Integer, Recipe> recipeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getRecipeId, Function.identity(), (key1, key2) -> key2));
        recipeIds.forEach(a -> {
            RecipeLog recipeLog = new RecipeLog();
            recipeLog.setRecipeId(a);
            recipeLog.setBeforeStatus(recipeMap.get(a).getStatus());
            recipeLog.setAfterStatus(RecipeStatusEnum.RECIPE_STATUS_DELETE.getType());
            recipeLog.setMemo("线下转线上：修改处方状态为已删除,数据是：" + JSONUtils.toString(recipeMap.get(a)));
            recipeLogDAO.saveRecipeLog(recipeLog);
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
                hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(organId, recipeCode);
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
        HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(mpiId, organCode, recipeCode);
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
     * 根据处方号回查支付状态
     *
     * @param recipeCode
     * @param clinicOrgan
     * @return
     */
    public String obtainPayStatus(String recipeCode, Integer clinicOrgan) {
        String realPayFlag = "";
        Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode, clinicOrgan);
        logger.info("obtainPayStatus recipe:{}",JSONUtils.toString(recipe));
        if (recipe != null && StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            logger.info("obtainPayStatus recipeOrder:{}",JSONUtils.toString(recipeOrder));
            if (recipeOrder != null&&!Objects.isNull(recipeOrder.getWxPayWay())) {
                realPayFlag = payClient.orderQuery(recipeOrder);
            }
        }
        logger.info("obtainPayStatus payflag:{}",JSONUtils.toString(realPayFlag));
        return realPayFlag;

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
            }
            //场景：没付钱跑到线下去支付了
            //如果已缴费处方在数据库里已存在，且数据里的状态是未缴费，则处理数据
            if (a.getStatus() == 2) {
                if (1 == hisRecipe.getStatus()) {
                    //如果his给过来的处方状态是已经缴费的且在平台查询到订单 判断是否已支付
                    String payFlag = obtainPayStatus(hisRecipe.getRecipeCode(), hisRecipe.getClinicOrgan());
                    if (PayConstant.RESULT_SUCCESS.equals(payFlag) || PayConstant.RESULT_WAIT.equals(payFlag) || PayConstant.ERROR.equals(payFlag)) {
                        //已成功支付
                        return;
                    }else{
                        deleteSetRecipeCode.add(recipeCode);
                        LOGGER.info("deleteSetRecipeCode cause Status recipeCode:{}", recipeCode);
                        return;
                    }
                }
            }

            //已处理处方（平台已付费）
            if (2 == hisRecipe.getStatus()) {
                return;
            }
            if (!hisRecipe.getMpiId().equals(mpiId)) {
                //如果已经支付，则不允许删除（场景：a操作用户绑定就诊人支付，且支付成功，但是支付平台回调慢，导致处方支付状态未更改   所以需要回查，是否已支付 如果已经支付，则不允许更改数据）
                String payFlag = obtainPayStatus(recipeCode, hisRecipe.getClinicOrgan());
                if (PayConstant.RESULT_SUCCESS.equals(payFlag) || PayConstant.RESULT_WAIT.equals(payFlag) || PayConstant.ERROR.equals(payFlag)) {
                    return;
                }
                deleteSetRecipeCode.add(recipeCode);
                LOGGER.info("deleteSetRecipeCode cause mpiid recipeCode:{}", recipeCode);
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
                LOGGER.info("deleteSetRecipeCode cause useDose 数据库",MapValueUtil.covertString(hisRecipeDetail.getUseDose()));
                LOGGER.info("deleteSetRecipeCode cause useDose his",MapValueUtil.covertString(recipeDetailTO.getUseDose()));
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

                if ((!MapValueUtil.covertInteger(hisRecipeDetail.getUseDays()).equals(MapValueUtil.covertInteger(recipeDetailTO.getUseDays())))) {
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
        HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(mpiId, organId, recipeCode);
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
    public RecipeInfoDTO pushRecipe(RecipeInfoDTO recipePdfDTO, Integer pushType, Map<Integer, PharmacyTcm> pharmacyIdMap,
                                    Integer sysType, String giveModeKey, Integer pushDest) throws Exception {
        EmrDetailDTO emrDetail = emrDetail(recipePdfDTO);
        Integer clinicId = recipePdfDTO.getRecipe().getClinicId();
        RevisitExDTO revisitEx = revisitClient.getByClinicId(clinicId);

        String decoctionId = recipePdfDTO.getRecipeExtend().getDecoctionId();
        DecoctionWay decoctionWay = null;
        if (StringUtils.isNotBlank(decoctionId)) {
            decoctionWay = drugDecoctionWayDao.get(Integer.valueOf(decoctionId));
        }

        String makeMethodId = recipePdfDTO.getRecipeExtend().getMakeMethodId();
        DrugMakingMethod makingMethod = null;
        if (StringUtils.isNotBlank(makeMethodId)) {
            makingMethod = drugMakingMethodDao.get(Integer.valueOf(makeMethodId));
        }

        if (CommonConstant.RECIPE_DOCTOR_TYPE.equals(sysType)) {
            return offlineRecipeClient.pushRecipe(pushType, recipePdfDTO, emrDetail, pharmacyIdMap, giveModeKey, revisitEx);
        } else {
            return offlineRecipeClient.patientPushRecipe(pushType, recipePdfDTO, emrDetail, pharmacyIdMap,
                    giveModeKey, revisitEx, decoctionWay, makingMethod, pushDest);
        }
    }

    /**
     * 线下处方中药药品剂型校验
     * @param organDrugLists  organ drug list data
     * @param recipeType    recipe of type
     * @return recipeDrugFormType
     */
    public Integer validateDrugForm(List<OrganDrugList> organDrugLists, Integer recipeType){
        if (!RecipeConstant.RECIPETYPE_TCM.equals(recipeType)){
            return null;
        }
        Set<String> organDrugFormSet = organDrugLists.stream().filter(organDrugList -> StringUtils.isNotEmpty(organDrugList.getDrugForm())).map(OrganDrugList::getDrugForm).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(organDrugFormSet)) {
            return RecipeDrugFormTypeEnum.TCM_DECOCTION_PIECES.getType();
        }
        return RecipeDrugFormTypeEnum.getDrugFormType((String)organDrugFormSet.toArray()[0]);
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
        return docIndexClient.getEmrDetailsV1(recipeExtend.getDocIndexId());
    }

    /**
     * 用药提醒的线下处方 存在患者的数据 转换药品为线上数据
     */
    public List<RecipeInfoDTO> queryRemindRecipe(Integer organId, String dateTime) throws Exception {
        //先加个配置项，防止有问题，后续确认没有问题删除
        String remindRecipeFlag = recipeParameterDao.getByName("jjRemindRecipeFlag");
        List<RecipeInfoDTO> recipeInfoList = offlineRecipeClient.queryRemindRecipe(organId, remindRecipeFlag, dateTime);
        if (CollectionUtils.isEmpty(recipeInfoList)) {
            return null;
        }
        Map<String, UsingRate> usingRateMap = drugClient.usingRateMapCode(organId);
        Map<String, UsePathways> usePathwaysMap = drugClient.usePathwaysCodeMap(organId);
        List<RecipeInfoDTO> recipeList = new LinkedList<>();
        recipeInfoList.forEach(a -> {
            //排除患者
            List<PatientDTO> patientList = patientClient.patientByIdCard(a.getPatientBean());
            if (CollectionUtils.isEmpty(patientList)) {
                return;
            }
            //转换药品
            List<Recipedetail> recipeDetails = a.getRecipeDetails();
            if (CollectionUtils.isEmpty(recipeDetails)) {
                return;
            }
            for (Recipedetail recipedetail : recipeDetails) {
                UsingRate usingRate = usingRateMap.get(recipedetail.getOrganUsingRate());
                if (null != usingRate) {
                    recipedetail.setUsingRate(usingRate.getRelatedPlatformKey());
                }
                UsePathways usePathways = usePathwaysMap.get(recipedetail.getOrganUsePathways());
                if (null != usePathways) {
                    recipedetail.setUsePathways(usePathways.getRelatedPlatformKey());
                }
            }
            recipeList.add(a);
        });
        return recipeList;
    }

    /**
     * 撤销线下处方
     *
     * @param organId
     * @param recipeCodes
     * @return
     */
    @LogRecord
    public HisResponseTO abolishOffLineRecipe(Integer organId, List<String> recipeCodes) {
        HisResponseTO hisResponseTO = new HisResponseTO();
        List<String> errorMsg = new ArrayList<>();
        hisResponseTO.setMsgCode(ErrorCode.SERVICE_SUCCEED + "");
        if (CollectionUtils.isEmpty(recipeCodes)) {
            hisResponseTO.setMsgCode(ErrorCode.SERVICE_FAIL + "");
            hisResponseTO.setMsg("处方" + JsonUtil.toString(recipeCodes) + "撤销失败,原因是：处方号为空");
            logger.info("recipeCodes不存在:{}", JSONUtils.toString(recipeCodes));
            return hisResponseTO;
        }
        recipeCodes.forEach(recipeCode -> {
            try {
                List<Recipe> recipes = recipeDAO.findByRecipeCodeAndClinicOrgan(Lists.newArrayList(recipeCode), organId);
                if (CollectionUtils.isEmpty(recipes)) {
                    errorMsg.add("处方" + recipeCode + "撤销失败,原因是：该处方还未转到线上不允许撤销");
                    logger.info("该处方还未转到线上:{}", JSONUtils.toString(recipeCode));
                    return;
                }
                recipes.forEach(recipe -> {
                    logger.info("recipe:{}", JSONUtils.toString(recipe));
                    //修改处方状态
                    if (PayFlagEnum.PAYED.getType().equals(recipe.getPayFlag())) {
                        errorMsg.add("处方" + recipeCode + "撤销失败,原因是：处方已经支付，则不允许取消");
                        return;
                    } else {
                        RecipeOrder order = recipeOrderDAO.getOrderByRecipeId(recipe.getRecipeId());
                        if (order != null) {
                            List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
                            //合并处方订单取消
                            List<Recipe> mergrRecipes = recipeDAO.findByRecipeIds(recipeIdList);
                            mergrRecipes.forEach(mergeRecipe -> {
                                recipeDAO.updateOrderCodeToNullByOrderCodeAndClearChoose(order.getOrderCode(), mergeRecipe, 1,true);
                            });

                            //修改订单状态
                            Map<String, Object> orderAttrMap = Maps.newHashMap();
                            orderAttrMap.put("effective", 0);
                            orderAttrMap.put("status", OrderStatusConstant.CANCEL_MANUAL);
                            recipeOrderDAO.updateByOrdeCode(order.getOrderCode(), orderAttrMap);
                            stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_DOCTOR_REPEAL);
                        }

                        Map<String, Integer> recipeMap = Maps.newHashMap();
                        recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), RecipeStatusEnum.RECIPE_STATUS_REVOKE.getType(), recipeMap);
                        StateManager stateManager = AppContextHolder.getBean("stateManager", StateManager.class);
                        stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_DOCTOR);
                    }
                });

            } catch (Exception e) {
                errorMsg.add("处方" + recipeCode + "撤销失败,原因是：" + e.getMessage());
                e.printStackTrace();
                logger.error("abolishOffLineRecipe error", e);
            }
        });
        if (CollectionUtils.isNotEmpty(errorMsg)) {
            hisResponseTO.setMsgCode(ErrorCode.SERVICE_FAIL + "");
            hisResponseTO.setMsg(JSONUtils.toString(errorMsg));
        }
        return hisResponseTO;
    }

    /**
     * 查询线下处方
     *
     * @param req
     * @return
     */
    @LogRecord
    public List<RecipeDTO> patientRecipeList(PatientRecipeListReqDTO req, Integer type) {
        List<String> isHisRecipe = configurationClient.getPropertyByStringList("findRecipeListType");
        if (!isHisRecipe.contains("offLine")) {
            return Collections.emptyList();
        }
        PatientDTO patient = patientClient.getPatient(req.getMpiId());
        if (ObjectUtils.isEmpty(patient)) {
            logger.info("患者信息不存在");
            return Collections.emptyList();
        }
        patient.setCardId(StringUtils.isNotEmpty(req.getCardId()) ? req.getCardId() : patient.getCardId());
        HisResponseTO<List<QueryHisRecipResTO>> hisResponseTO =queryData(req.getOrganId(),patient,null,type,null,req.getStartTime(),req.getEndTime());
//        List<QueryHisRecipResTO> list = offlineRecipeClient.patientFeeRecipeList(req, patient, type);
        if (null == hisResponseTO || CollectionUtils.isEmpty(hisResponseTO.getData())) {
            return null;
        }
        List<QueryHisRecipResTO> list = hisResponseTO.getData();
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        List<RecipeDTO> res = covertRecipeDTOFromQueryHisRecipResTO(list, patient, type);
        logger.info("patientRecipeList res:{},{}", req.getUuid(), JSONUtils.toString(res));
        return res;
    }

    /**
     * his处方 预校验
     *
     * @param recipe
     * @param recipeExtend
     * @param details
     * @param pharmacyTcmMap
     * @param organDrugMap
     * @return
     */
    public DoSignRecipeDTO hisRecipeCheck(Recipe recipe, RecipeExtend recipeExtend, List<Recipedetail> details,
                                          Map<Integer, PharmacyTcm> pharmacyTcmMap, Map<String, OrganDrugList> organDrugMap) {
        HisCheckRecipeReqTO hisCheckRecipe = new HisCheckRecipeReqTO();
        //医生工号
        String jobNumber = doctorClient.jobNumber(recipe.getClinicOrgan(), recipe.getDoctor(), recipe.getDepart()).getJobNumber();
        hisCheckRecipe.setDoctorID(jobNumber);
        //科室代码---行政科室代码
        DepartmentDTO department = departClient.getDepartmentByDepart(recipe.getDepart());
        if (null != department) {
            hisCheckRecipe.setDeptCode(department.getCode());
            hisCheckRecipe.setDeptName(department.getName());
        }
        com.ngari.recipe.dto.PatientDTO patientDTO = patientClient.getPatientDTO(recipe.getMpiid());
        if (null != patientDTO) {
            //身份证
            hisCheckRecipe.setCertID(patientDTO.getIdcard());
            hisCheckRecipe.setCertificate(patientDTO.getCertificate());
            hisCheckRecipe.setCertificateType(patientDTO.getCertificateType());
            //患者名
            hisCheckRecipe.setPatientName(patientDTO.getPatientName());
            //患者性别
            hisCheckRecipe.setPatientSex(patientDTO.getPatientSex());
            //患者电话
            hisCheckRecipe.setPatientTel(patientDTO.getMobile());
            //病人类型
        }
        String organCode = organClient.getOrganizeCodeByOrganId(recipe.getClinicOrgan());
        hisCheckRecipe.setOrganID(organCode);
        try {
            Map<String, Object> map = offlineRecipeClient.hisRecipeCheck(recipe, recipeExtend, details, pharmacyTcmMap, organDrugMap, hisCheckRecipe);
            DoSignRecipeDTO doSignRecipeDTO = new DoSignRecipeDTO();
            doSignRecipeDTO.setMap(map);
            doSignRecipeDTO.setRecipeId(recipe.getRecipeId());
            doSignRecipeDTO.setSignResult(true);
            doSignRecipeDTO.setCanContinueFlag("0");
            return doSignRecipeDTO;
        } catch (DAOException e1) {
            logger.error("HisRecipeManager hisRecipeCheck e1 RecipeId ={} ", recipe.getRecipeId(), e1);
            return doSignRecipe(recipe.getRecipeId(), e1.getMessage(), true, recipe.getClinicOrgan());
        } catch (Exception e) {
            logger.error("HisRecipeManager hisRecipeCheck e RecipeId ={} ", recipe.getRecipeId(), e);
            return doSignRecipe(recipe.getRecipeId(), "his处方预检查异常", false, recipe.getClinicOrgan());
        }
    }

    /**
     * 查询his处方详情
     * @param patientRecipeDetailReq
     * @return
     */
    public RecipeInfoDTO getHisRecipeInfoDTO (PatientRecipeDetailReqDTO patientRecipeDetailReq){
        if (StringUtils.isEmpty(patientRecipeDetailReq.getMpiid()) || StringUtils.isEmpty(patientRecipeDetailReq.getRecipeCode())) {
            return null;
        }
        PatientDTO patient = patientClient.getPatient(patientRecipeDetailReq.getMpiid());
        if (Objects.isNull(patient)) {
            return null;
        }
        OfflineRecipePayFlagEnum offlineRecipePayFlagEnum = OfflineRecipePayFlagEnum.getByState(patientRecipeDetailReq.getProcessState());
        HisResponseTO<List<QueryHisRecipResTO>> hisResponseTO = queryData(patientRecipeDetailReq.getOrganId(), patient ,null, offlineRecipePayFlagEnum.getType(),null,patientRecipeDetailReq.getStartTime(),patientRecipeDetailReq.getEndTime());
        if (null == hisResponseTO || CollectionUtils.isEmpty(hisResponseTO.getData())) {
            return null;
        }

        RecipeInfoDTO recipeInfoDTO = new RecipeInfoDTO();
        List<QueryHisRecipResTO> hisRecipeResTOList = hisResponseTO.getData();
        QueryHisRecipResTO hisRecipeResTO = hisRecipeResTOList.get(0);
        RecipeExtend recipeExtend = new RecipeExtend();
        recipeExtend = getRecipeExtendFromHisRecipe(hisRecipeResTO, recipeExtend);
        Recipe recipe = getRecipeFromHisRecipe(hisRecipeResTO, patient, recipeExtend, offlineRecipePayFlagEnum);
        List<Recipedetail> recipeDetails = getRecipeDetailsFromHisRecipe(hisRecipeResTO);
        recipeInfoDTO.setRecipe(recipe);
        recipeInfoDTO.setRecipeExtend(recipeExtend);
        recipeInfoDTO.setRecipeDetails(recipeDetails);
        recipeInfoDTO.setPatientBean(ObjectCopyUtils.convert(patient, com.ngari.recipe.dto.PatientDTO.class));
        recipeInfoDTO.setShowText(hisRecipeResTO.getShowText());
        recipeInfoDTO.setGiveModeText("");
        return recipeInfoDTO;
    }

    /**
     * 将获取到的线下处方信息组装为线上处方对象
     * @param hisRecipeResTO
     * @param patient
     * @param recipeExtend
     * @param offlineRecipePayFlagEnum
     * @return
     */
    public Recipe getRecipeFromHisRecipe(QueryHisRecipResTO hisRecipeResTO, PatientDTO patient, RecipeExtend recipeExtend, OfflineRecipePayFlagEnum offlineRecipePayFlagEnum) {
        Recipe recipe = ObjectCopyUtils.convert(hisRecipeResTO, Recipe.class);
        recipe.setMpiid(patient.getMpiId());
        recipe.setOrganDiseaseName(hisRecipeResTO.getDiseaseName());
        recipe.setSignDate(hisRecipeResTO.getCreateDate());
        recipe.setRecipeSourceType(RecipeSourceTypeEnum.OFFLINE_RECIPE.getType());
        recipe.setAppointDepartName(hisRecipeResTO.getDepartName());
        //设置复诊单
        recipe.setBussSource(BussSourceTypeEnum.BUSSSOURCE_NO.getType());
        if (!BussSourceTypeEnum.BUSSSOURCE_NO.getType().equals(hisRecipeResTO.getRevisitType())) {
            RevisitExDTO revisitExDTO = revisitClient.getByRegisterId(hisRecipeResTO.getRegisteredId());
            if (Objects.nonNull(revisitExDTO)) {
                recipe.setBussSource(BussSourceTypeEnum.BUSSSOURCE_REVISIT.getType());
                recipe.setClinicId(revisitExDTO.getConsultId());
                if (Objects.isNull(hisRecipeResTO.getIllnessType())) {
                    recipeExtend.setIllnessType(revisitExDTO.getDbType());
                    recipeExtend.setIllnessName(revisitExDTO.getInsureTypeName());
                }
            } else {
                logger.error("无关联复诊:{},{}", hisRecipeResTO.getRecipeCode(), hisRecipeResTO.getRegisteredId());
            }
        } else {
            recipe.setBussSource(BussSourceTypeEnum.BUSSSOURCE_OUTPATIENT.getType());
        }
        //设置处方状态
        recipe.setProcessState(offlineRecipePayFlagEnum.getState());
        recipe.setPayFlag(offlineRecipePayFlagEnum.getPayFlag());
        //设置处方单靶向药
        List<RecipeDetailTO> recipeDetailTOList = hisRecipeResTO.getDrugList();
        List<String> drugCodeList = recipeDetailTOList.stream().filter(b -> StringUtils.isNotEmpty(b.getDrugCode())).map(RecipeDetailTO::getDrugCode).collect(Collectors.toList());
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(recipe.getClinicOrgan(), drugCodeList);
        boolean targetedDrug = organDrugList.stream().anyMatch(organDrug -> new Integer("1").equals(organDrug.getTargetedDrugType()));
        recipe.setTargetedDrugType(targetedDrug?1:0);
        return recipe;
    }

    /**
     * 将获取到的线下处方信息组装为线上处方扩展对象
     * @param hisRecipeResTO
     * @param recipeExtend
     * @return
     */
    public RecipeExtend getRecipeExtendFromHisRecipe(QueryHisRecipResTO hisRecipeResTO, RecipeExtend recipeExtend) {
        recipeExtend = ObjectCopyUtils.convert(hisRecipeResTO, RecipeExtend.class);
        recipeExtend.setRegisterID(hisRecipeResTO.getRegisteredId());
        return recipeExtend;
    }

    /**
     * 将获取到的线下处方药品信息组装为线上处方药品对象
     * @param hisRecipeResTO
     * @return
     */
    public List<Recipedetail> getRecipeDetailsFromHisRecipe(QueryHisRecipResTO hisRecipeResTO) {
        List<RecipeDetailTO> recipeDetailTOList = hisRecipeResTO.getDrugList();
        List<Recipedetail> recipeDetails = ObjectCopyUtils.convert(recipeDetailTOList, Recipedetail.class);
        return recipeDetails;
    }

    /**
     * @param queryHisRecipResTOs
     * @param patient
     * @param flag                1 代缴费，2，已缴费，3 已失效
     * @return
     */
    private List<RecipeDTO> covertRecipeDTOFromQueryHisRecipResTO(List<QueryHisRecipResTO> queryHisRecipResTOs, PatientDTO patient, Integer flag) {
        //PatientRecipeListResVo
        if (CollectionUtils.isEmpty(queryHisRecipResTOs)) {
            return Collections.emptyList();
        }
        List<RecipeDTO> recipeDTOS = new ArrayList<>();
        queryHisRecipResTOs.forEach(a -> {
            RecipeDTO recipeDTO = new RecipeDTO();
            RecipeBean recipe = ObjectCopyUtils.convert(a, RecipeBean.class);
            RecipeExtendBean recipeExt = ObjectCopyUtils.convert(a, RecipeExtendBean.class);
            List<RecipeDetailBean> recipeDetailBeans = ObjectCopyUtils.convert(a.getDrugList(), RecipeDetailBean.class);
            Recipe dbRecipe=recipeManager.getByRecipeCodeAndClinicOrganAndMpiid(a.getRecipeCode(),a.getClinicOrgan(),patient.getMpiId());
            if(dbRecipe!=null){
                //说明被当前人转过
                recipe.setRecipeId(dbRecipe.getRecipeId());
            }
            recipe.setBussSource(0);
            if (!new Integer(0).equals(a.getRevisitType())) {
                RevisitExDTO revisitExDTO = revisitClient.getByRegisterId(a.getRegisteredId());
                if (revisitExDTO != null) {
                    recipe.setBussSource(2);
                    recipe.setClinicId(revisitExDTO.getConsultId());
                    //优先级his->复诊
                    if (null == a.getIllnessType()) {
                        recipeExt.setIllnessType(revisitExDTO.getDbType());
                        recipeExt.setIllnessName(revisitExDTO.getInsureTypeName());
                    }
                }else{
                    logger.error("无关联复诊:{},{}", a.getRecipeCode(),a.getRegisteredId());
                }
            } else {
                recipe.setBussSource(5);
            }
            recipe.setMpiid(patient.getMpiId());
            recipe.setOrganDiseaseName(a.getDiseaseName());
            if (HisRecipeConstant.HISRECIPESTATUS_NOIDEAL.equals(flag)) {
                recipe.setProcessState(RecipeStateEnum.PROCESS_STATE_ORDER.getType());
                recipe.setPayFlag(0);
            } else if (HisRecipeConstant.HISRECIPESTATUS_ALREADYIDEAL.equals(flag)) {
                recipe.setProcessState(RecipeStateEnum.PROCESS_STATE_DONE.getType());
                recipe.setPayFlag(1);
            } else if (HisRecipeConstant.HISRECIPESTATUS_EXPIRED.equals(flag)) {
                recipe.setProcessState(RecipeStateEnum.PROCESS_STATE_CANCELLATION.getType());
                recipe.setPayFlag(0);
            }
            recipe.setSignDate(a.getCreateDate());
            recipe.setRecipeSourceType(2);
            //靶向药
            AtomicReference<Integer> targetedDrugType = new AtomicReference<>(0);
            List<String> drugCodeList = recipeDetailBeans.stream().filter(b -> StringUtils.isNotEmpty(b.getDrugCode())).map(RecipeDetailBean::getDrugCode).collect(Collectors.toList());
            List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(recipe.getClinicOrgan(), drugCodeList);
            Map<String, List<OrganDrugList>> organDrugListMap = organDrugList.stream().collect(Collectors.groupingBy(OrganDrugList::getOrganDrugCode));
            recipeDetailBeans.forEach(b->{
                List<OrganDrugList> organDrugLists = organDrugListMap.get(b.getDrugCode());
                if (CollectionUtils.isEmpty(organDrugLists)) {
                    logger.info("处方中的药品信息未维护到线上平台药品目录:{},{},{}", recipe.getRecipeCode(), b.getDrugCode(), recipe.getClinicOrgan());
                }
                if (new Integer("1").equals(organDrugLists.get(0).getTargetedDrugType())) {
                    targetedDrugType.set(1);
                    return;
                }
            });
            recipe.setTargetedDrugType(targetedDrugType.get());
            recipeExt.setRegisterID(a.getRegisteredId());
            //recipeType recipeCode illnessType illnessName departName doctorName
            //recipeBusType secrecyRecipe 这两组装数据的人自己搞
            recipeDTO.setPatientDTO(patient);
            recipeDTO.setRecipeBean(recipe);
            recipeDTO.setRecipeExtendBean(recipeExt);
            recipeDTO.setRecipeDetails(recipeDetailBeans);
            recipeDTOS.add(recipeDTO);
        });
        return recipeDTOS;
    }


    /**
     * 组装预校验返回结果
     *
     * @param recipeId
     * @param msg
     * @param flag
     * @param organId
     * @return
     */
    private DoSignRecipeDTO doSignRecipe(Integer recipeId, String msg, boolean flag, Integer organId) {
        DoSignRecipeDTO doSignRecipeDTO = new DoSignRecipeDTO();
        doSignRecipeDTO.setSignResult(false);
        doSignRecipeDTO.setErrorFlag(true);
        doSignRecipeDTO.setRecipeId(recipeId);
        doSignRecipeDTO.setMsg(msg);
        doSignRecipeDTO.setCanContinueFlag("-1");
        if (flag) {
            //允许继续处方:不进行校验/进行校验且校验通过0 ，进行校验校验不通过允许通过4，进行校验校验不通过不允许通过-1
            Boolean allowContinueMakeFlag = configurationClient.getValueBooleanCatch(organId, "allowContinueMakeRecipe", false);
            if (allowContinueMakeFlag) {
                doSignRecipeDTO.setCanContinueFlag("4");
            } else {
                doSignRecipeDTO.setCanContinueFlag("-1");
            }
        }
        return doSignRecipeDTO;
    }
}
