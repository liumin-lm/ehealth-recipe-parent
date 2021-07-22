package recipe.manager;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.DrugInfoResponseTO;
import com.ngari.platform.recipe.mode.RecipeResultBean;
import com.ngari.recipe.dto.DoSignRecipeDTO;
import com.ngari.recipe.entity.*;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DrugStockClient;
import recipe.client.IConfigurationClient;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.PharmacyTcmDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.util.ListValueUtil;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


/**
 * @description： 库存检查
 * @author： whf
 * @date： 2021-07-19 9:48
 */
@Service
public class DrugStockManager extends BaseManager {

    @Resource
    private IConfigurationClient configurationClient;

    @Resource
    private DrugStockClient drugStockClient;

    @Resource
    private PharmacyTcmDAO pharmacyTcmDAO;

    @Resource
    private OrganDrugListDAO organDrugListDAO;

    @Autowired
    private OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO;
    @Resource
    private SaleDrugListDAO saleDrugListDAO;

    /**
     * 校验医院库存
     *
     * @param recipe
     * @param detailList
     * @return
     */
    public RecipeResultBean scanDrugStockByRecipeId(Recipe recipe, List<Recipedetail> detailList) {
        logger.info("scanHisDrugStockByRecipeId req recipe={}  recipeDetails = {}", JSONArray.toJSONString(recipe), JSONArray.toJSONString(detailList));
        RecipeResultBean result = RecipeResultBean.getSuccess();
        if (Objects.isNull(recipe)) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("没有该处方");
            return result;
        }
        if (1 == recipe.getTakeMedicine()) {
            //外带药处方则不进行校验
            return RecipeResultBean.getSuccess();
        }
        if (CollectionUtils.isEmpty(detailList)) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("处方没有详情");
            return result;
        }

        // 判断是否需要对接HIS
        List<String> recipeTypes = configurationClient.getValueListCatch(recipe.getClinicOrgan(), "getRecipeTypeToHis", null);
        if (!recipeTypes.contains(Integer.toString(recipe.getRecipeType()))) {
            return result;
        }

        // 判断his 是否存在
        if (!configurationClient.isHisEnable(recipe.getClinicOrgan())) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("医院HIS未启用。");
            logger.info("scanDrugStock 医院HIS未启用 organId: {}", recipe.getClinicOrgan());
            return result;
        }
        Set<Integer> pharmaIds = new HashSet<>();
        AtomicReference<Boolean> organDrugCodeFlag = new AtomicReference<>(false);
        List<Integer> drugIdList = detailList.stream().map(detail -> {
            pharmaIds.add(detail.getPharmacyId());
            if (StringUtils.isEmpty(detail.getOrganDrugCode())) {
                organDrugCodeFlag.set(true);
            }
            return detail.getDrugId();
        }).collect(Collectors.toList());
        // 医院配置药品不能存在机构药品编号为空的情况
        if (organDrugCodeFlag.get()) {
            logger.warn("scanDrugStock 医院配置药品存在编号为空的数据.");
            result.setCode(RecipeResultBean.FAIL);
            result.setError("医院配置药品存在编号为空的数据");
            return result;
        }

        // 请求his
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(recipe.getClinicOrgan(), drugIdList);
        List<PharmacyTcm> pharmacyTcmByIds = pharmacyTcmDAO.getPharmacyTcmByIds(pharmaIds);
        DrugInfoResponseTO response = drugStockClient.scanDrugStock(detailList, recipe.getClinicOrgan(), organDrugList, pharmacyTcmByIds);
        if (null == response) {
            //his未配置该服务则还是可以通过
            result.setError("HIS返回为NULL");
            return result;
        }
        if (0 != response.getMsgCode()) {
            String organCodeStr = response.getMsg();
            List<String> nameList = new ArrayList<>();
            if (StringUtils.isNotEmpty(organCodeStr)) {
                List<String> organCodes = Arrays.asList(organCodeStr.split(","));
                nameList = organDrugListDAO.findNameByOrganIdAndDrugCodes(recipe.getClinicOrgan(), organCodes);
            }
            String showMsg = "由于" + Joiner.on(",").join(nameList) + "门诊药房库存不足，该处方仅支持配送，无法到院取药，是否继续？";
            result.setError(showMsg);
            result.setObject(nameList);
            result.setExtendValue("1");
            result.setCode(RecipeResultBean.FAIL);
            logger.warn("scanDrugStock 存在无库存药品. response={} ", JSONUtils.toString(response));
        }
        logger.info("scanDrugStock 结果={}", JSONObject.toJSONString(result));
        return result;
    }

    /**
     * 药企库存
     *
     * @param recipe
     * @param drugsEnterprise
     * @param recipeDetails
     * @return
     */
    public Integer scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails) {
        List<Integer> drugIds = recipeDetails.stream().map(Recipedetail::getDrugId).collect(Collectors.toList());
        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(drugsEnterprise.getId(), drugIds);
        HisResponseTO hisResponseTO = drugStockClient.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetails, saleDrugLists);
        if (hisResponseTO != null && hisResponseTO.isSuccess()) {
            return 1;
        } else {
            return 0;
        }
    }


    /**
     * 校验药企库存
     *
     * @param doSignRecipe
     * @param organId
     * @param object
     * @param recipeDetails
     * @return
     */
    public List<String> checkEnterprise(DoSignRecipeDTO doSignRecipe, Integer organId, List<Object> object, List<Recipedetail> recipeDetails) {
        //检查开处方是否需要进行药企库存校验
        boolean checkEnterprise = checkEnterprise(organId);
        if (!checkEnterprise) {
            return null;
        }
        Integer enterprisesDockType = configurationClient.getValueCatch(organId, "EnterprisesDockType", 0);
        if (0 != enterprisesDockType) {
            return null;
        }
        //验证能否药品配送以及能否开具到一张处方单上
        String msg = canOpenRecipeDrugs(organId, recipeDetails);
        if (StringUtils.isNotEmpty(msg)) {
            doSignRecipe(doSignRecipe, null, msg);
            return new LinkedList<>();
        }
        //药企库存实时查询判断药企库存
        if (CollectionUtils.isEmpty(object)) {
            return null;
        }
        //todo findUnSupportDepList 调整如下代码 需要确认
        List<List<String>> groupList = new ArrayList<>();
        object.forEach(a -> {
            List<String> list = (List<String>) a;
            if (CollectionUtils.isNotEmpty(list)) {
                groupList.add(list);
            }
        });
        List<String> resultBean = ListValueUtil.minIntersection(groupList);
        doSignRecipe(doSignRecipe, resultBean, "药品库存不足，请更换其他药品后再试");
        return resultBean;
    }

    /**
     * 检查开处方是否需要进行药企库存校验
     *
     * @param organId
     * @return true:需要校验  false:不需要校验
     */
    public boolean checkEnterprise(Integer organId) {
        Integer checkEnterprise = configurationClient.getCheckEnterpriseByOrganId(organId);
        if (ValidateUtil.integerIsEmpty(checkEnterprise)) {
            return false;
        }
        //获取机构配置的药企是否存在 如果有则需要校验 没有则不需要
        List<DrugsEnterprise> enterprise = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(organId, 1);
        return CollectionUtils.isNotEmpty(enterprise);
    }


    /**
     * 医院药企 库存都 较验
     *
     * @param doSignRecipe       返回结果
     * @param organId            机构id
     * @param enterpriseDrugName 药企无库存药品名称
     * @param scanResult         医院库存
     * @return
     */
    public RecipeResultBean checkEnterpriseAndHospital(DoSignRecipeDTO doSignRecipe, Integer organId, List<String> enterpriseDrugName, RecipeResultBean scanResult) {
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
        if (1000899 == organId) {
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
     * 组织返回结果msg
     *
     * @param doSignRecipe
     * @param object
     * @param msg
     * @return
     */
    public void doSignRecipe(DoSignRecipeDTO doSignRecipe, Object object, String msg) {
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


    /**
     * todo canOpenRecipeDrugs 调整如下代码 需要确认
     * 是否有一个药企存在药品
     *
     * @param clinicOrgan
     * @param recipeDetails
     * @return
     */
    private String canOpenRecipeDrugs(Integer clinicOrgan, List<Recipedetail> recipeDetails) {
        //找到每一个药能支持的药企关系
        List<DrugsEnterprise> enterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(clinicOrgan, 1);
        List<Integer> deps = enterprises.stream().map(DrugsEnterprise::getId).collect(Collectors.toList());
        List<Integer> drugIds = recipeDetails.stream().map(Recipedetail::getDrugId).distinct().collect(Collectors.toList());
        //获取药企与配送药品关系 （药企A:药品1,药品2）
        Map<Integer, List<Integer>> depDrugRel = saleDrugListDAO.findDepDrugRelation(drugIds, deps);

        List<Integer> depDrugIdList = null;
        Collections.sort(drugIds);
        for (List<Integer> depDrug : depDrugRel.values()) {
            Collections.sort(depDrug);
            if (drugIds.toString().equals(depDrug.toString())) {
                depDrugIdList = null;
                break;
            }
            depDrugIdList = depDrug;
        }
        if (CollectionUtils.isEmpty(depDrugIdList)) {
            return null;
        }
        List<Integer> finalDepDrugIdList = depDrugIdList;
        List<String> drugNames = recipeDetails.stream().filter(a -> !finalDepDrugIdList.contains(a.getDrugId())).map(Recipedetail::getDrugName).collect(Collectors.toList());
        return drugNames.toString() + "不支持同一家药企配送或不在该机构药企可配送的药品目录里面";
    }

}
