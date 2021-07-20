package recipe.manager;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.DrugInfoResponseTO;
import com.ngari.patient.service.OrganConfigService;
import com.ngari.platform.recipe.mode.RecipeResultBean;
import com.ngari.recipe.entity.*;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DrugStockClient;
import recipe.client.IConfigurationClient;
import recipe.dao.*;
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

    /**
     * 检查开处方是否需要进行药企库存校验
     *
     * @param organId
     * @return true:需要校验  false:不需要校验
     */
    @Resource
    private SaleDrugListDAO saleDrugListDAO;


    /**
     * 药企库存
     * @param recipe
     * @param drugsEnterprise
     * @param recipeDetails
     * @return
     */
    public Integer scanEnterpriseDrugStock(Recipe recipe,DrugsEnterprise drugsEnterprise,List<Recipedetail> recipeDetails){
        List<Integer> drugIds = recipeDetails.stream().map(Recipedetail::getDrugId).collect(Collectors.toList());
        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds( drugsEnterprise.getId(),drugIds);
        HisResponseTO hisResponseTO = drugStockClient.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetails, saleDrugLists);
        if (hisResponseTO != null && hisResponseTO.isSuccess()) {
            return 1;
        } else {
            return 0;
        }
    }


    /**
     * 检查机构配置下的药企
     * @param organId
     * @return
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
     * 校验医院库存
     *
     * @param recipe
     * @param recipeDetails
     * @return
     */
    public RecipeResultBean scanDrugStockByRecipeId(Recipe recipe,List<Recipedetail> recipeDetails) {
        logger.info("scanHisDrugStockByRecipeId req recipe={}  recipeDetails = {}", JSONArray.toJSONString(recipe),JSONArray.toJSONString(recipeDetails));
        if (Integer.valueOf(1).equals(recipe.getTakeMedicine())) {
            //外带药处方则不进行校验
            return RecipeResultBean.getSuccess();
        }
        return scanDrugStock(recipe, recipeDetails);
    }



    /**
     * 医院库存查询
     *
     * @param recipe
     * @param detailList
     * @return
     */
    public RecipeResultBean scanDrugStock(Recipe recipe, List<Recipedetail> detailList) {
        logger.info("scanDrugStock 入参 recipe={},recipedetail={}", JSONObject.toJSONString(recipe), JSONObject.toJSONString(detailList));
        RecipeResultBean result = RecipeResultBean.getSuccess();

        if (Objects.isNull(recipe)) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("没有该处方");
            return result;
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

        if (!configurationClient.isHisEnable(recipe.getClinicOrgan())) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("医院HIS未启用。");
            logger.info("scanDrugStock 医院HIS未启用 organId: {}", recipe.getClinicOrgan());
            return result;
        }
        // 判断his 是否存在
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
            result.setObject(nameList);
            String showMsg = "由于" + Joiner.on(",").join(nameList) + "门诊药房库存不足，该处方仅支持配送，无法到院取药，是否继续？";
            result.setError(showMsg);
            result.setExtendValue("1");
            result.setCode(RecipeResultBean.FAIL);
            logger.warn("scanDrugStock 存在无库存药品. response={} ", JSONUtils.toString(response));
        }
        logger.info("scanDrugStock 结果={}", JSONObject.toJSONString(result));
        return result;
    }
}
