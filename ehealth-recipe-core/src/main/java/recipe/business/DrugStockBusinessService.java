package recipe.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.dto.DoSignRecipeDTO;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bean.DrugEnterpriseResult;
import recipe.bean.SupportDepListBean;
import recipe.client.IConfigurationClient;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.enumerate.type.DrugStockCheckEnum;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.manager.ButtonManager;
import recipe.manager.DrugStockManager;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.MapValueUtil;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description： 药品库存业务 service
 * @author： whf
 * @date： 2021-07-19 15:41
 */
@Service
public class DrugStockBusinessService extends BaseService {
    @Resource
    private RecipeDAO recipeDAO;
    @Resource
    private DrugStockManager drugStockManager;
    @Resource
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Resource
    private RecipeDetailDAO recipeDetailDAO;
    @Resource
    private SaleDrugListDAO saleDrugListDAO;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private ButtonManager buttonManager;


    /**
     * 开方时对库存的操作
     *
     * @param recipe
     * @return
     */
    public Map<String, Object> doSignRecipeCheckAndGetGiveMode(RecipeBean recipe) {
        logger.info("进入 DrugStockBusinessService.doSignRecipeCheckAndGetGiveMode recipeId ={}", recipe.getRecipeId());
        Integer recipeId = recipe.getRecipeId();
        DoSignRecipeDTO doSignRecipe = new DoSignRecipeDTO(true, false, null, "", recipeId, null);

        // 校验数据
        Recipe recipeNew = recipeDAO.get(recipeId);
        if (Objects.isNull(recipeNew)) {
            drugStockManager.doSignRecipe(doSignRecipe, null, "没有该处方");
            return MapValueUtil.beanToMap(doSignRecipe);
        }
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isEmpty(recipeDetails)) {
            drugStockManager.doSignRecipe(doSignRecipe, null, "处方没有详情");
            return MapValueUtil.beanToMap(doSignRecipe);
        }

        //获取按钮
        List<String> configurations = buttonManager.getGiveMode(recipe.getClinicOrgan());
        if (CollectionUtils.isEmpty(configurations)) {
            drugStockManager.doSignRecipe(doSignRecipe, null, "抱歉，机构未配置购药方式，无法开处方");
            return MapValueUtil.beanToMap(doSignRecipe);
        }
        //获取校验何种类型库存
        Integer checkFlag = RecipeSupportGiveModeEnum.checkFlag(configurations);
        if (ValidateUtil.integerIsEmpty(checkFlag)) {
            return MapValueUtil.beanToMap(doSignRecipe);
        }
        logger.info("doSignRecipeCheckAndGetGiveMode recipeId={}, checkFlag={}", recipeId, checkFlag);
        //校验各种情况的库存
        com.ngari.platform.recipe.mode.RecipeResultBean scanResult = null;
        SupportDepListBean allSupportDepList = null;
        doSignRecipe.setCheckFlag(checkFlag);

        if (DrugStockCheckEnum.HOS_CHECK_STOCK.getType().equals(checkFlag)) {
            //校验医院库存
            scanResult = drugStockManager.scanDrugStockByRecipeId(recipeNew, recipeDetails);
            if (RecipeResultBean.FAIL.equals(scanResult.getCode())) {
                drugStockManager.doSignRecipe(doSignRecipe, scanResult.getObject(), "药品门诊药房库存不足，请更换其他药品后再试");
            }
        } else if (DrugStockCheckEnum.ENT_CHECK_STOCK.getType().equals(checkFlag)) {
            //查询药企库存
            drugStockManager.checkDrugEnterprise(doSignRecipe, recipe.getClinicOrgan(), recipeDetails);
            allSupportDepList = findAllSupportDepList(recipeNew, recipeDetails);
            List<DrugEnterpriseResult> drugEnterpriseResults = allSupportDepList.getNoHaveList();
            if (CollectionUtils.isNotEmpty(drugEnterpriseResults)) {
                List<Object> object = drugEnterpriseResults.stream().map(RecipeResultBean::getObject).collect(Collectors.toList());
                drugStockManager.checkEnterprise(doSignRecipe, object);
            }
        } else if (DrugStockCheckEnum.ALL_CHECK_STOCK.getType().equals(checkFlag)) {
            /**校验 医院+药企 库存*/
            //药企库存
            drugStockManager.checkDrugEnterprise(doSignRecipe, recipe.getClinicOrgan(), recipeDetails);
            allSupportDepList = findAllSupportDepList(recipeNew, recipeDetails);
            List<String> enterpriseDrugName = null;
            List<DrugEnterpriseResult> drugEnterpriseResults = allSupportDepList.getNoHaveList();
            if (CollectionUtils.isNotEmpty(drugEnterpriseResults)) {
                List<Object> object = drugEnterpriseResults.stream().map(RecipeResultBean::getObject).collect(Collectors.toList());
                enterpriseDrugName = drugStockManager.checkEnterprise(doSignRecipe, object);
            }
            //医院库存
            scanResult = drugStockManager.scanDrugStockByRecipeId(recipeNew, recipeDetails);
            //校验医院药企库存
            drugStockManager.checkEnterpriseAndHospital(doSignRecipe, recipe.getClinicOrgan(), enterpriseDrugName, scanResult, allSupportDepList.getHaveList());
        }
        //保存药品购药方式
        saveGiveMode(scanResult, allSupportDepList, checkFlag, recipeId, recipe.getClinicOrgan(), configurations);
        return MapValueUtil.beanToMap(doSignRecipe);
    }

    /**
     * 异步保存处方购药方式
     *
     * @param scanResult
     * @param allSupportDepList
     * @param checkFlag
     * @param recipeId
     * @param organId
     * @param configurations
     */
    private void saveGiveMode(com.ngari.platform.recipe.mode.RecipeResultBean scanResult, SupportDepListBean allSupportDepList, int checkFlag, Integer recipeId, int organId, List<String> configurations) {
        RecipeBusiThreadPool.execute(() -> {
            logger.info("saveGiveMode start");
            long start = System.currentTimeMillis();
            List<DrugsEnterprise> supportDepList = null;
            if (!Objects.isNull(allSupportDepList)) {
                supportDepList = allSupportDepList.getHaveList();
            }
            List<Integer> recipeGiveMode = buttonManager.getRecipeGiveMode(scanResult, supportDepList, checkFlag, recipeId, organId, configurations);
            if (CollectionUtils.isNotEmpty(recipeGiveMode)) {
                Map<String, Object> attMap = new HashMap<>();
                String join = StringUtils.join(recipeGiveMode, ",");
                attMap.put("recipeSupportGiveMode", join);
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, attMap);
            }
            long elapsedTime = System.currentTimeMillis() - start;
            logger.info("RecipeBusiThreadPool saveGiveMode 异步保存处方购药方式 执行时间:{}.", elapsedTime);
        });
    }

    /**
     * 获取所有 药企 查询库存信息
     *
     * @param recipe
     * @param recipeDetails
     * @return
     */
    private SupportDepListBean findAllSupportDepList(Recipe recipe, List<Recipedetail> recipeDetails) {
        logger.info("findAllSupportDepList req recipe={} recipeDetail={}", JSONArray.toJSONString(recipe), JSONArray.toJSONString(recipeDetails));
        SupportDepListBean supportDepListBean = new SupportDepListBean();

        List<Integer> drugIds = recipeDetails.stream().map(Recipedetail::getDrugId).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(drugIds)) {
            logger.warn("findAllSupportDepList 处方[{}]没有任何药品！", recipe.getRecipeId());
            return supportDepListBean;
        }
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findByOrganId(recipe.getClinicOrgan());

        List<DrugsEnterprise> haveList = new ArrayList<>();
        List<DrugEnterpriseResult> noHaveList = new ArrayList<>();
        //线上支付能力判断
        for (DrugsEnterprise dep : drugsEnterpriseList) {
            //不支持在线支付跳过该药企
            if (1 == dep.getPayModeSupport()) {
                noHaveList.add(new DrugEnterpriseResult(RecipeResultBean.FAIL));
                continue;
            }
            //药品匹配成功标识 his管理的药企】不用校验配送药品，由预校验结果
            boolean succFlag = false;
            Integer enterprisesDockType = configurationClient.getValueCatchReturnInteger(recipe.getClinicOrgan(), "EnterprisesDockType", 0);
            if (1 == enterprisesDockType) {
                succFlag = true;
            }
            if (!succFlag) {
                Long count = saleDrugListDAO.getCountByOrganIdAndDrugIds(dep.getId(), drugIds);
                if (null != count && count == drugIds.size()) {
                    succFlag = true;
                }
            }

            if (!succFlag) {
                logger.warn("findAllSupportDepList 药企名称=[{}]存在不支持配送药品. 处方ID=[{}], 药企ID=[{}], drugIds={}", dep.getName(), recipe.getRecipeId(), dep.getId(), JSONUtils.toString(drugIds));
                noHaveList.add(new DrugEnterpriseResult(RecipeResultBean.FAIL));
                continue;
            }
            //todo 通过查询该药企库存，最终确定能否配送
            DrugEnterpriseResult result = findUnSupportDrugEnterprise(recipe, dep, recipeDetails);
            if (DrugEnterpriseResult.SUCCESS.equals(result.getCode()) || 2 == dep.getCheckInventoryFlag()) {
                haveList.add(dep);
                logger.info("findAllSupportDepList 药企名称=[{}]支持配送该处方所有药品. 处方ID=[{}], 药企ID=[{}], drugIds={}", dep.getName(), recipe.getRecipeId(), dep.getId(), JSONUtils.toString(drugIds));
            } else {
                noHaveList.add(result);
                logger.info("findAllSupportDepList  药企名称=[{}]药企库存查询返回药品无库存. 处方ID=[{}], 药企ID=[{}]", dep.getName(), recipe.getRecipeId(), dep.getId());
            }
        }
        // 存在满足库存的药企
        supportDepListBean.setHaveList(haveList);
        if (CollectionUtils.isEmpty(haveList)) {
            supportDepListBean.setNoHaveList(noHaveList);
        }
        logger.info("findAllSupportDepList res supportDepListBean= {}", JSON.toJSONString(supportDepListBean));
        return supportDepListBean;
    }

    /**
     * 查询药企无库存药品信息 DrugEnterpriseResult.Object=List<DrugName>
     *
     * @param recipe
     * @param drugsEnterprise
     * @param recipeDetails
     * @return
     */
    private DrugEnterpriseResult findUnSupportDrugEnterprise(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails) {
        Integer recipeId = recipe.getRecipeId();
        logger.info("findUnSupportDrugEnterprise recipeId:{}, drugsEnterprise:{}", recipeId, JSONUtils.toString(drugsEnterprise));
        DrugEnterpriseResult result = DrugEnterpriseResult.getFail();
        if (drugsEnterprise != null && drugsEnterprise.getCheckInventoryFlag() != null && drugsEnterprise.getCheckInventoryFlag() == 0) {
            result.setCode(DrugEnterpriseResult.SUCCESS);
            return result;
        }
        //查询医院库存  药企配置：校验药品库存标志 0 不需要校验 1 校验药企库存 2 药店没库存时可以备货 3 校验医院库存
        // 有存在药企是医院的自建药企,配置了查医院库存
        if (drugsEnterprise != null && drugsEnterprise.getCheckInventoryFlag() != null && drugsEnterprise.getCheckInventoryFlag() == 3) {
            com.ngari.platform.recipe.mode.RecipeResultBean recipeResultBean = drugStockManager.scanDrugStockByRecipeId(recipe, recipeDetails);
            logger.info("findUnSupportDrugEnterprise recipeId={},医院库存查询结果={}", recipeId, JSONObject.toJSONString(recipeResultBean));
            if (RecipeResultBean.SUCCESS.equals(recipeResultBean.getCode())) {
                result.setCode(DrugEnterpriseResult.SUCCESS);
            } else {
                result.setCode(DrugEnterpriseResult.FAIL);
                result.setObject(recipeResultBean.getObject());
            }
            return result;
        }
        //通过前置机调用
        if (null != drugsEnterprise && 1 == drugsEnterprise.getOperationType()) {
            Integer code = drugStockManager.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetails);
            result.setCode(code);
            return result;
        }
        // 通过平台调用，获取调用实现
        AccessDrugEnterpriseService drugEnterpriseService = RemoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
        result = drugEnterpriseService.scanStock(recipeId, drugsEnterprise);

        logger.info("findUnSupportDrugEnterprise recipeId={},平台调用查询结果={}", recipeId, JSONObject.toJSONString(result));
        return result;
    }

}
