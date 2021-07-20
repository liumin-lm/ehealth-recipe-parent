package recipe.business;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.service.IRecipeEnterpriseService;
import com.ngari.platform.recipe.mode.ScanRequestBean;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.dto.DoSignRecipeDTO;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
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
import recipe.service.RecipeHisService;
import recipe.service.RecipePatientService;
import recipe.service.RecipeServiceSub;
import recipe.util.MapValueUtil;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description：
 * @author： whf
 * @date： 2021-07-19 15:41
 */
@Service
public class RecipeBusinessService extends BaseService {


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
        //添加按钮配置项key
        GiveModeShowButtonVO giveModeShowButtonVO = GiveModeFactory.getGiveModeBaseByRecipe(recipeDAO.getByRecipeId(recipeId)).getGiveModeSettingFromYypt(recipe.getClinicOrgan());
        List<GiveModeButtonBean> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        int checkFlag = 0;
        List<String> configurations;
        DoSignRecipeDTO doSignRecipe = new DoSignRecipeDTO(false, true, "-1", "抱歉，机构未配置购药方式，无法开处方", recipeId, null);
        if (null != giveModeButtonBeans) {
            configurations = giveModeButtonBeans.stream().map(e -> e.getShowButtonKey()).collect(Collectors.toList());
            //收集按钮信息用于判断校验哪边库存 0是什么都没有，1是指配置了到院取药，2是配置到药企相关，3是医院药企都配置了
            if (configurations == null || configurations.size() == 0) {
                return MapValueUtil.beanToMap(doSignRecipe);
            }
            for (String configuration : configurations) {
                switch (configuration) {
                    case "supportToHos":
                        if (checkFlag == 0 || checkFlag == 1) {
                            checkFlag = 1;
                        } else {
                            checkFlag = 3;
                        }
                        break;
                    case "showSendToHos":
                    case "showSendToEnterprises":
                    case "supportTFDS":
                        if (checkFlag == 0 || checkFlag == 2) {
                            checkFlag = 2;
                        } else {
                            checkFlag = 3;
                        }
                        break;
                }

            }
        } else {
            return MapValueUtil.beanToMap(doSignRecipe);
        }
        logger.info("doSignRecipeCheck recipeId={}, checkFlag={}", recipeId, checkFlag);
        com.ngari.platform.recipe.mode.RecipeResultBean scanResult = null;
        SupportDepListBean allSupportDepList = null;
        switch (checkFlag) {
            case 1:
                // 查询医院库存
                scanResult = drugStockManager.scanDrugStockByRecipeId(recipeId);
                break;
            case 2:
                // 查询药企库存
                allSupportDepList = findAllSupportDepList(recipeId, recipe.getClinicOrgan());
                break;
            case 3:
                // 医院药企 库存都查询
                scanResult = drugStockManager.scanDrugStockByRecipeId(recipeId);
                allSupportDepList = findAllSupportDepList(recipeId, recipe.getClinicOrgan());
                break;
            default:
                break;
        }

        // 保存药品购药方式
        List<DrugsEnterprise> supportDepList = allSupportDepList.getHaveList();
        List<Integer> recipeGiveMode = drugsEnterpriseService.getRecipeGiveMode(scanResult, supportDepList, checkFlag, recipeId, recipe.getClinicOrgan(), configurations);
        if (CollectionUtils.isNotEmpty(recipeGiveMode)) {
            Map<String, Object> attMap = new HashMap<>();
            String join = StringUtils.join(recipeGiveMode, ",");
            attMap.put("recipeSupportGiveMode", join);
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, attMap);
        }

        // 校验库存,看能否继续开方
        return MapValueUtil.beanToMap(getResMap(checkFlag, scanResult, recipe, allSupportDepList));
    }

    /**
     * 校验库存,看能否继续开方
     *
     * @param checkFlag
     * @param scanResult
     * @param recipe
     * @param allSupportDepList
     * @return
     */
    private DoSignRecipeDTO getResMap(int checkFlag, com.ngari.platform.recipe.mode.RecipeResultBean scanResult
            , RecipeBean recipe, SupportDepListBean allSupportDepList) {
        Integer recipeId = recipe.getRecipeId();
        DoSignRecipeDTO doSignRecipe = new DoSignRecipeDTO(false, true, "-1", "", recipeId, checkFlag);
        // 校验开方药品信息
        switch (checkFlag) {
            case 1:
                //只校验医院库存医院库存不校验药企，如无库存不允许开，直接弹出提示
                if (RecipeResultBean.FAIL.equals(scanResult.getCode())) {
                    return doSignRecipe(doSignRecipe, scanResult.getObject(), "药品门诊药房库存不足，请更换其他药品后再试");
                }
                break;
            case 2:
                //只校验处方药品药企配送以及库存信息，不校验医院库存
                boolean checkEnterprise = drugStockManager.checkEnterprise(recipe.getClinicOrgan());
                if (checkEnterprise) {
                    //验证能否药品配送以及能否开具到一张处方单上
                    RecipeResultBean recipeResult1 = RecipeServiceSub.validateRecipeSendDrugMsg(recipe);
                    if (RecipeResultBean.FAIL.equals(recipeResult1.getCode())) {
                        return doSignRecipe(doSignRecipe, null, recipeResult1.getMsg());
                    }
                    //药企库存实时查询判断药企库存
                    List<DrugEnterpriseResult> drugEnterpriseResults = allSupportDepList.getNoHaveList();
                    RecipeResultBean recipeResultBean = recipePatientService.findUnSupportDepList(recipeId, drugEnterpriseResults);
                    if (RecipeResultBean.FAIL.equals(recipeResultBean.getCode())) {
                        return doSignRecipe(doSignRecipe, recipeResultBean.getObject(), "药品库存不足，请更换其他药品后再试");
                    }
                }
                break;
            case 3:
                //药企无库存药品名称list
                Object enterpriseDrugName = null;
                int errFlag = 0;
                // 是否需要校验药企库存
                boolean checkEnterprise3 = drugsEnterpriseService.checkEnterprise(recipe.getClinicOrgan());
                if (checkEnterprise3) {
                    //his管理的药企不要验证库存和配送药品，有his【预校验】校验库存
                    Integer enterprisesDockType = configurationClient.getValueCatch(recipe.getClinicOrgan(), "EnterprisesDockType", 0);
                    if (0 == enterprisesDockType) {
                        //药品能否一起配送
                        RecipeResultBean recipeResult3 = RecipeServiceSub.validateRecipeSendDrugMsg(recipe);
                        if (RecipeResultBean.FAIL.equals(recipeResult3.getCode())) {
                            errFlag = 1;
                        } else {
                            //药企库存校验
                            List<DrugEnterpriseResult> drugEnterpriseResults = allSupportDepList.getNoHaveList();
                            RecipeResultBean recipeResultBean = recipePatientService.findUnSupportDepList(recipeId, drugEnterpriseResults);
                            if (RecipeResultBean.FAIL.equals(recipeResultBean.getCode())) {
                                enterpriseDrugName = recipeResultBean.getObject();
                                errFlag = 1;
                            }
                        }
                    }
                }
                if (RecipeResultBean.FAIL.equals(scanResult.getCode())) {
                    if (1000899 == recipe.getClinicOrgan()) {
                        return doSignRecipe(doSignRecipe, scanResult.getObject(), "药品门诊药房库存不足，请更换其他药品后再试");
                    }
                    if (0 == errFlag) {
                        //医院无库存药企有库存
                        doSignRecipe.setCanContinueFlag("1");
                        return doSignRecipe(doSignRecipe, scanResult.getObject(), "药品医院库存不足，该处方仅支持药企配送，无法到院取药，是否继续？");
                    } else {
                        //医院药企都无库存
                        List<String> hospitalDrugName = (List<String>) scanResult.getObject();
                        List<String> enterpriseDrugNameLiat = (List<String>) enterpriseDrugName;
                        if (CollectionUtils.isNotEmpty(hospitalDrugName) && CollectionUtils.isNotEmpty(enterpriseDrugNameLiat)) {
                            Boolean hospital = hospitalDrugName.containsAll(enterpriseDrugNameLiat);
                            Boolean enterprise = enterpriseDrugNameLiat.containsAll(hospitalDrugName);
                            if (hospital || enterprise) {
                                return doSignRecipe(doSignRecipe, scanResult.getObject(), "药品库存不足，请更换其他药品后再试");
                            }
                        }
                        return doSignRecipe;
                    }
                } else if (RecipeResultBean.SUCCESS.equals(scanResult.getCode()) && errFlag == 1) {
                    //医院有库存药企无库存
                    doSignRecipe.setCanContinueFlag("2");
                    return doSignRecipe(doSignRecipe, enterpriseDrugName, "药品配送药企库存不足，该处方仅支持到院取药，无法药企配送，是否继续？");
                }
                break;
            default:
                break;
        }
        // 校验开方是否可以继续
        doSignRecipe.setSignResult(true);
        doSignRecipe.setErrorFlag(false);
        doSignRecipe.setCanContinueFlag(null);
        return doSignRecipe;
    }

    /**
     * 查询药企无库存药品信息 DrugEnterpriseResult.Object=List<DrugName>
     *
     * @param recipeId
     * @param drugsEnterprise
     * @return
     */
    public DrugEnterpriseResult findUnSupportDrugEnterprise(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        logger.info("findUnSupportDrugEnterprise recipeId:{}, drugsEnterprise:{}", recipeId, JSONUtils.toString(drugsEnterprise));
        DrugEnterpriseResult result = DrugEnterpriseResult.getFail();
        if (drugsEnterprise != null && drugsEnterprise.getCheckInventoryFlag() != null && drugsEnterprise.getCheckInventoryFlag() == 0) {
            result.setCode(DrugEnterpriseResult.SUCCESS);
            return result;
        }
        //查询医院库存  药企配置：校验药品库存标志 0 不需要校验 1 校验药企库存 2 药店没库存时可以备货 3 校验医院库存
        //根据处方查询医院库存
        if (drugsEnterprise != null && drugsEnterprise.getCheckInventoryFlag() != null && drugsEnterprise.getCheckInventoryFlag() == 3) {
            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
            RecipeResultBean recipeResultBean = hisService.scanDrugStockByRecipeId(recipeId);
            logger.info("findUnSupportDrugEnterprise recipeId={},医院库存查询结果={}", recipeId, JSONObject.toJSONString(recipeResultBean));
            if (recipeResultBean.getCode() == RecipeResultBean.SUCCESS) {
                result.setCode(DrugEnterpriseResult.SUCCESS);
            } else {
                result.setCode(DrugEnterpriseResult.FAIL);
                result.setObject(recipeResultBean.getObject());
            }
            return result;
        }
        //通过前置机调用
        if (drugsEnterprise != null && 1 == drugsEnterprise.getOperationType()) {
            IRecipeEnterpriseService recipeEnterpriseService = AppContextHolder.getBean("his.iRecipeEnterpriseService", IRecipeEnterpriseService.class);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            ScanRequestBean scanRequestBean = RemoteDrugEnterpriseService.getScanRequestBean(recipe, drugsEnterprise);
            logger.info("findUnSupportDrugEnterprise-scanStock scanRequestBean:{}.", JSONUtils.toString(scanRequestBean));
            HisResponseTO responseTO = recipeEnterpriseService.scanStock(scanRequestBean);
            logger.info("findUnSupportDrugEnterprise recipeId={},前置机调用查询结果={}", recipeId, JSONObject.toJSONString(responseTO));
            if (responseTO != null && responseTO.isSuccess()) {
                result.setCode(DrugEnterpriseResult.SUCCESS);
            } else {
                result.setCode(DrugEnterpriseResult.FAIL);
            }
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
     * 获取所有 药企 查询库存信息
     *
     * @param recipeId
     * @param organId
     * @return
     */
    private SupportDepListBean findAllSupportDepList(Integer recipeId, int organId) {
        SupportDepListBean supportDepListBean = new SupportDepListBean();
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isEmpty(recipeDetails)) {
            return supportDepListBean;
        }
        List<Integer> drugIds = recipeDetails.stream().map(Recipedetail::getDrugId).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(drugIds)) {
            logger.warn("findUnSupportDepList 处方[{}]没有任何药品！", recipeId);
            return supportDepListBean;
        }
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findByOrganId(organId);

        List<DrugsEnterprise> haveList = new ArrayList<>();
        List<DrugEnterpriseResult> noHaveList = new ArrayList<>();
        //线上支付能力判断
        boolean onlinePay = iHisConfigService.isHisEnable(organId);
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
            Integer enterprisesDockType = configurationClient.getValueCatch(organId, "EnterprisesDockType", 0);
            if (1 == enterprisesDockType) {
                succFlag = true;
            } else {
                Long count = saleDrugListDAO.getCountByOrganIdAndDrugIds(depId, drugIds);
                if (null != count && count == drugIds.size()) {
                    succFlag = true;
                }
            }
            if (!succFlag) {
                logger.error("findUnSupportDepList 药企名称=[{}]存在不支持配送药品. 处方ID=[{}], 药企ID=[{}], drugIds={}", dep.getName(), recipeId, depId, JSONUtils.toString(drugIds));
                noHaveList.add(new DrugEnterpriseResult(RecipeResultBean.FAIL));
            } else {
                //通过查询该药企库存，最终确定能否配送
                DrugEnterpriseResult result = findUnSupportDrugEnterprise(recipeId, dep);
                if (DrugEnterpriseResult.SUCCESS.equals(result.getCode()) || 2 == dep.getCheckInventoryFlag()) {
                    haveList.add(dep);
                    logger.info("findUnSupportDepList 药企名称=[{}]支持配送该处方所有药品. 处方ID=[{}], 药企ID=[{}], drugIds={}", dep.getName(), recipeId, depId, JSONUtils.toString(drugIds));
                } else {
                    noHaveList.add(result);
                    logger.info("findUnSupportDepList  药企名称=[{}]药企库存查询返回药品无库存. 处方ID=[{}], 药企ID=[{}]", dep.getName(), recipeId, depId);
                }
            }
        }
        // 存在满足库存的药企
        if (CollectionUtils.isNotEmpty(noHaveList) && CollectionUtils.isNotEmpty(drugsEnterpriseList) && noHaveList.size() < drugsEnterpriseList.size()) {
            noHaveList.clear();
        }

        supportDepListBean.setHaveList(haveList);
        supportDepListBean.setNoHaveList(noHaveList);
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
    private DoSignRecipeDTO doSignRecipe(DoSignRecipeDTO doSignRecipe, Object object, String msg) {
        if (null != object) {
            List<String> nameList = (List<String>) object;
            if (CollectionUtils.isNotEmpty(nameList)) {
                String nameStr = "【" + Joiner.on("、").join(nameList) + "】";
                doSignRecipe.setMsg("由于该处方单上的" + nameStr + msg);
            }
        }
        doSignRecipe.setMsg(msg);
        return doSignRecipe;
    }

}
