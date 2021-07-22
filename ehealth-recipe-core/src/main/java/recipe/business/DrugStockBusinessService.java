package recipe.business;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.dto.DoSignRecipeDTO;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.constant.RecipeSupportGiveModeEnum;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
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
import recipe.givemode.business.GiveModeFactory;
import recipe.manager.DrugStockManager;
import recipe.service.DrugsEnterpriseService;
import recipe.service.RecipePatientService;
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
    private DrugsEnterpriseService drugsEnterpriseService;

    @Resource
    private DrugStockManager drugStockManager;

    @Resource
    private DrugsEnterpriseDAO drugsEnterpriseDAO;

    @Resource
    private IHisConfigService iHisConfigService;

    @Resource
    private RecipeDetailDAO recipeDetailDAO;

    @Resource
    private SaleDrugListDAO saleDrugListDAO;

    @Resource
    private RecipePatientService recipePatientService;

    @Autowired
    private IConfigurationClient configurationClient;

    /**
     * 开方时对库存的操作
     *
     * @param recipe
     * @return
     */
    public Map<String, Object> doSignRecipeCheckAndGetGiveMode(RecipeBean recipe) {
        Integer recipeId = recipe.getRecipeId();
        DoSignRecipeDTO doSignRecipe = new DoSignRecipeDTO(true, false, null, "", recipeId, null);
        //获取按钮
        List<String> configurations = configurations(recipe);
        //获取校验何种类型库存
        Integer checkFlag = RecipeSupportGiveModeEnum.checkFlag(configurations);
        if (ValidateUtil.integerIsEmpty(checkFlag)) {
            doSignRecipe(doSignRecipe, null, "抱歉，机构未配置购药方式，无法开处方");
            return MapValueUtil.beanToMap(doSignRecipe);
        }
        logger.info("doSignRecipeCheckAndGetGiveMode recipeId={}, checkFlag={}", recipeId, checkFlag);
        //校验各种情况的库存
        com.ngari.platform.recipe.mode.RecipeResultBean scanResult = null;
        SupportDepListBean allSupportDepList = null;
        doSignRecipe.setCheckFlag(checkFlag);
        Recipe recipeNew = recipeDAO.get(recipeId);
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        if (1 == checkFlag) {
            //只校验医院库存
            scanResult = drugStockManager.scanDrugStockByRecipeId(recipeNew,recipeDetails);
            if (RecipeResultBean.FAIL.equals(scanResult.getCode())) {
                doSignRecipe(doSignRecipe, scanResult.getObject(), "药品门诊药房库存不足，请更换其他药品后再试");
            }
        } else if (2 == checkFlag) {
            //查询药企库存
            allSupportDepList = findAllSupportDepList(recipeNew, recipeDetails);
            checkEnterprise(doSignRecipe, recipe, allSupportDepList, recipeDetails);
        } else if (3 == checkFlag) {
            //医院药企 库存都查询
            allSupportDepList = findAllSupportDepList(recipeNew,recipeDetails);
            scanResult = checkEnterpriseAndHospital(doSignRecipe, recipe, allSupportDepList,recipeNew,recipeDetails);
        }
        //保存药品购药方式
        saveGiveMode(scanResult, allSupportDepList, checkFlag, recipeId, recipe.getClinicOrgan(), configurations);
        return MapValueUtil.beanToMap(doSignRecipe);

    }

    /**
     * 查询药企无库存药品信息 DrugEnterpriseResult.Object=List<DrugName>
     *
     * @param recipe
     * @param drugsEnterprise
     * @param recipeDetails
     * @return
     */
    public DrugEnterpriseResult findUnSupportDrugEnterprise(Recipe recipe, DrugsEnterprise drugsEnterprise,List<Recipedetail> recipeDetails) {
        Integer recipeId = recipe.getRecipeId();
        logger.info("findUnSupportDrugEnterprise recipeId:{}, drugsEnterprise:{}", recipeId, JSONUtils.toString(drugsEnterprise));
        DrugEnterpriseResult result = DrugEnterpriseResult.getFail();
        if (drugsEnterprise != null && drugsEnterprise.getCheckInventoryFlag() != null && drugsEnterprise.getCheckInventoryFlag() == 0) {
            result.setCode(DrugEnterpriseResult.SUCCESS);
            return result;
        }
        //查询医院库存  药企配置：校验药品库存标志 0 不需要校验 1 校验药企库存 2 药店没库存时可以备货 3 校验医院库存
        //根据处方查询医院库存
        if (drugsEnterprise != null && drugsEnterprise.getCheckInventoryFlag() != null && drugsEnterprise.getCheckInventoryFlag() == 3) {
            com.ngari.platform.recipe.mode.RecipeResultBean recipeResultBean = drugStockManager.scanDrugStockByRecipeId(recipe,recipeDetails);
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
        if (drugsEnterprise != null && 1 == drugsEnterprise.getOperationType()) {
            Integer code = drugStockManager.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetails);
            result.setCode(code);
            return result;
        }
        // 通过平台调用，获取调用实现
        if (null == drugsEnterprise) {
            //药企对象为空，则通过处方id获取相应药企实现
            DrugEnterpriseResult result1 = RemoteDrugEnterpriseService.getServiceByRecipeId(recipeId);
            if (DrugEnterpriseResult.SUCCESS.equals(result1.getCode())) {
                AccessDrugEnterpriseService drugEnterpriseService = result1.getAccessDrugEnterpriseService();
                result = drugEnterpriseService.scanStock(recipeId, result1.getDrugsEnterprise());
            }
        } else {
            AccessDrugEnterpriseService drugEnterpriseService = RemoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
            result = drugEnterpriseService.scanStock(recipeId, drugsEnterprise);
        }
        logger.info("findUnSupportDrugEnterprise recipeId={},平台调用查询结果={}", recipeId, JSONObject.toJSONString(result));
        return result;
    }

    /**
     * 获取按钮
     *
     * @param recipe
     * @return
     */
    private List<String> configurations(RecipeBean recipe) {
        //添加按钮配置项key
        GiveModeShowButtonVO giveModeShowButtonVO = GiveModeFactory.getGiveModeBaseByRecipe(recipeDAO.getByRecipeId(recipe.getRecipeId())).getGiveModeSettingFromYypt(recipe.getClinicOrgan());
        List<GiveModeButtonBean> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        if (null == giveModeButtonBeans) {
            return null;
        }
        List<String> configurations = giveModeButtonBeans.stream().map(GiveModeButtonBean::getShowButtonKey).collect(Collectors.toList());
        //收集按钮信息用于判断校验哪边库存 0是什么都没有，1是指配置了到院取药，2是配置到药企相关，3是医院药企都配置了
        if (CollectionUtils.isEmpty(configurations)) {
            return null;
        }
        return configurations;
    }

    /**
     * 查询药企库存
     *
     * @param doSignRecipe 返回参数
     * @param recipe       处方对象
     * @return 药企库存
     */
    private List<String> checkEnterprise(DoSignRecipeDTO doSignRecipe, RecipeBean recipe, SupportDepListBean allSupportDepList, List<Recipedetail> recipeDetails) {
        //检查开处方是否需要进行药企库存校验
        boolean checkEnterprise = drugStockManager.checkEnterprise(recipe.getClinicOrgan());
        if (!checkEnterprise) {
            return null;
        }
        Integer enterprisesDockType = configurationClient.getValueCatch(recipe.getClinicOrgan(), "EnterprisesDockType", 0);
        if (0 != enterprisesDockType) {
            return null;
        }
        List<Integer> drugIds = recipeDetails.stream().map(Recipedetail::getDrugId).collect(Collectors.toList());
        //验证能否药品配送以及能否开具到一张处方单上
        String msg = drugStockManager.canOpenRecipeDrugs(recipe.getClinicOrgan(), recipe.getRecipeId(), drugIds);
        if (StringUtils.isNotEmpty(msg)) {
            doSignRecipe(doSignRecipe, null, msg);
            return new LinkedList<>();
        }
        //药企库存实时查询判断药企库存
        List<DrugEnterpriseResult> drugEnterpriseResults = allSupportDepList.getNoHaveList();
        List<Object> object = drugEnterpriseResults.stream().map(RecipeResultBean::getObject).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(object)) {
            return null;
        }
        List<String> resultBean = drugStockManager.findUnSupportDepList(object);
        doSignRecipe(doSignRecipe, resultBean, "药品库存不足，请更换其他药品后再试");
        return resultBean;

    }

    /**
     * 医院药企 库存都查询
     *
     * @param doSignRecipe      返回参数
     * @param recipe            处方对象
     * @param allSupportDepList 药企库存
     * @return 医院库存
     */
    private com.ngari.platform.recipe.mode.RecipeResultBean checkEnterpriseAndHospital(DoSignRecipeDTO doSignRecipe, RecipeBean recipe, SupportDepListBean allSupportDepList, Recipe recipeNew, List<Recipedetail> recipeDetails) {
        //查询药企库存
        List<String> enterpriseDrugName = checkEnterprise(doSignRecipe, recipe, allSupportDepList, recipeDetails);
        //查询医院库存
        com.ngari.platform.recipe.mode.RecipeResultBean scanResult = drugStockManager.scanDrugStockByRecipeId(recipeNew, recipeDetails);
        //医院有库存，药企有库存
        if (RecipeResultBean.SUCCESS.equals(scanResult.getCode()) && null == enterpriseDrugName) {
            return scanResult;
        }
        //医院有库存药企无库存
        if (RecipeResultBean.SUCCESS.equals(scanResult.getCode()) && null != enterpriseDrugName) {
            doSignRecipe(doSignRecipe, enterpriseDrugName, "药品配送药企库存不足，该处方仅支持到院取药，无法药企配送，是否继续？");
            doSignRecipe.setCanContinueFlag("2");
            return scanResult;
        }

        //医院无库存 特殊机构返回
        if (1000899 == recipe.getClinicOrgan()) {
            doSignRecipe(doSignRecipe, scanResult.getObject(), "药品门诊药房库存不足，请更换其他药品后再试");
            return scanResult;
        }
        //医院无库存，药企有库存
        if (null == enterpriseDrugName) {
            doSignRecipe(doSignRecipe, scanResult.getObject(), "药品医院库存不足，该处方仅支持药企配送，无法到院取药，是否继续？");
            doSignRecipe.setCanContinueFlag("1");
            return scanResult;
        }
        //医院无库存，药企无库存
        List<String> hospitalDrugName = (List<String>) scanResult.getObject();
        if (CollectionUtils.isNotEmpty(hospitalDrugName) && CollectionUtils.isNotEmpty(enterpriseDrugName)) {
            Boolean hospital = hospitalDrugName.containsAll(enterpriseDrugName);
            Boolean enterprise = enterpriseDrugName.containsAll(hospitalDrugName);
            if (hospital || enterprise) {
                doSignRecipe(doSignRecipe, scanResult.getObject(), "药品库存不足，请更换其他药品后再试");
            }
        }
        return scanResult;
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
        List<DrugsEnterprise> supportDepList = allSupportDepList.getHaveList();
        RecipeBusiThreadPool.execute(() -> {
            List<Integer> recipeGiveMode = drugsEnterpriseService.getRecipeGiveMode(scanResult, supportDepList, checkFlag, recipeId, organId, configurations);
            if (CollectionUtils.isNotEmpty(recipeGiveMode)) {
                Map<String, Object> attMap = new HashMap<>();
                String join = StringUtils.join(recipeGiveMode, ",");
                attMap.put("recipeSupportGiveMode", join);
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, attMap);
            }
        });
    }

    /**
     * 获取所有 药企 查询库存信息
     *
     * @param recipe
     * @param recipeDetails
     * @return
     */
    private SupportDepListBean findAllSupportDepList(Recipe recipe,List<Recipedetail> recipeDetails) {
        logger.info("findAllSupportDepList req recipe={} recipeDetail={}", JSONArray.toJSONString(recipe),JSONArray.toJSONString(recipeDetails));
        SupportDepListBean supportDepListBean = new SupportDepListBean();

        if (CollectionUtils.isEmpty(recipeDetails)) {
            return supportDepListBean;
        }
        List<Integer> drugIds = recipeDetails.stream().map(Recipedetail::getDrugId).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(drugIds)) {
            logger.warn("findAllSupportDepList 处方[{}]没有任何药品！", recipe.getRecipeId());
            return supportDepListBean;
        }
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findByOrganId(recipe.getClinicOrgan());

        List<DrugsEnterprise> haveList = new ArrayList<>();
        List<DrugEnterpriseResult> noHaveList = new ArrayList<>();
        //线上支付能力判断
        boolean onlinePay = iHisConfigService.isHisEnable(recipe.getClinicOrgan());
        for (DrugsEnterprise dep : drugsEnterpriseList) {
            //根据药企是否能满足所有配送的药品优先
            Integer depId = dep.getId();
            //不支持在线支付跳过该药企
            if (1 == dep.getPayModeSupport() && !onlinePay) {
                noHaveList.add(new DrugEnterpriseResult(RecipeResultBean.FAIL));
                continue;
            }
            //药品匹配成功标识 his管理的药企】不用校验配送药品，由预校验结果
            boolean succFlag = false;
            Integer enterprisesDockType = configurationClient.getValueCatchReturnInteger(recipe.getClinicOrgan(), "EnterprisesDockType", 0);
            if (1 == enterprisesDockType) {
                succFlag = true;
            } else {
                Long count = saleDrugListDAO.getCountByOrganIdAndDrugIds(depId, drugIds);
                if (null != count && count == drugIds.size()) {
                    succFlag = true;
                }
            }
            if (!succFlag) {
                logger.error("findAllSupportDepList 药企名称=[{}]存在不支持配送药品. 处方ID=[{}], 药企ID=[{}], drugIds={}", dep.getName(), recipe.getRecipeId(), depId, JSONUtils.toString(drugIds));
                noHaveList.add(new DrugEnterpriseResult(RecipeResultBean.FAIL));
            } else {
                //通过查询该药企库存，最终确定能否配送
                DrugEnterpriseResult result = findUnSupportDrugEnterprise(recipe,dep,recipeDetails);
                if (DrugEnterpriseResult.SUCCESS.equals(result.getCode()) || 2 == dep.getCheckInventoryFlag()) {
                    haveList.add(dep);
                    logger.info("findAllSupportDepList 药企名称=[{}]支持配送该处方所有药品. 处方ID=[{}], 药企ID=[{}], drugIds={}", dep.getName(), recipe.getRecipeId(), depId, JSONUtils.toString(drugIds));
                } else {
                    noHaveList.add(result);
                    logger.info("findAllSupportDepList  药企名称=[{}]药企库存查询返回药品无库存. 处方ID=[{}], 药企ID=[{}]", dep.getName(), recipe.getRecipeId(), depId);
                }
            }
        }
        // 存在满足库存的药企
        if (CollectionUtils.isNotEmpty(noHaveList) && CollectionUtils.isNotEmpty(drugsEnterpriseList) && noHaveList.size() < drugsEnterpriseList.size()) {
            noHaveList.clear();
        }

        supportDepListBean.setHaveList(haveList);
        supportDepListBean.setNoHaveList(noHaveList);

        logger.info("findAllSupportDepList res supportDepListBean={}",JSONArray.toJSONString(supportDepListBean));
        return supportDepListBean;
    }


    /**
     * 组织返回结果msg
     *
     * @param doSignRecipe
     * @param object
     * @param msg
     * @return
     */
    private void doSignRecipe(DoSignRecipeDTO doSignRecipe, Object object, String msg) {
        doSignRecipe.setSignResult(false);
        doSignRecipe.setErrorFlag(true);
        doSignRecipe.setCanContinueFlag("-1");
        if (null == object) {
            doSignRecipe.setMsg(msg);
            return;
        }
        List<String> nameList = (List<String>) object;
        if (CollectionUtils.isNotEmpty(nameList)) {
            String nameStr = "【" + Joiner.on("、").join(nameList) + "】";
            doSignRecipe.setMsg("由于该处方单上的" + nameStr + msg);
        }
    }
}
