package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.ngari.his.recipe.mode.DrugInfoResponseTO;
import com.ngari.platform.recipe.mode.RecipeResultBean;
import com.ngari.recipe.dto.ValidateOrganDrugDTO;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DrugStockClient;
import recipe.dao.PharmacyTcmDAO;
import recipe.util.ValidateUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 机构药品处理
 *
 * @author fuzi
 */
@Service
public class OrganDrugListManager extends BaseManager {
    private static final Logger logger = LoggerFactory.getLogger(OrganDrugListManager.class);
    @Autowired
    private DrugStockClient drugStockClient;
    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;

    /**
     * 校验机构药品库存
     *
     * @param recipe
     * @param detailList
     * @return
     */
    public RecipeResultBean scanDrugStockByRecipeId(Recipe recipe, List<Recipedetail> detailList) {
        logger.info("OrganDrugListManager scanDrugStockByRecipeId recipe={}  recipeDetails = {}", JSONArray.toJSONString(recipe), JSONArray.toJSONString(detailList));
        RecipeResultBean result = RecipeResultBean.getSuccess();
        if (null != recipe.getTakeMedicine() && 1 == recipe.getTakeMedicine()) {
            //外带药处方则不进行校验
            return RecipeResultBean.getSuccess();
        }
        if (CollectionUtils.isEmpty(detailList)) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("处方没有详情");
            return result;
        }
        // 判断his 是否启用
        if (!configurationClient.isHisEnable(recipe.getClinicOrgan())) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("医院HIS未启用。");
            logger.info("OrganDrugListManager scanDrugStockByRecipeId 医院HIS未启用 organId: {}", recipe.getClinicOrgan());
            return result;
        }
        Set<Integer> pharmaIds = new HashSet<>();
        List<Integer> drugIdList = detailList.stream().map(a -> {
            pharmaIds.add(a.getPharmacyId());
            return a.getDrugId();
        }).collect(Collectors.toList());

        // 判断是否需要对接HIS
        List<String> recipeTypes = configurationClient.getValueListCatch(recipe.getClinicOrgan(), "getRecipeTypeToHis", null);
        if (!recipeTypes.contains(Integer.toString(recipe.getRecipeType()))) {
            return result;
        }
        // 请求his
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(recipe.getClinicOrgan(), drugIdList);
        List<PharmacyTcm> pharmacyTcmByIds = pharmacyTcmDAO.getPharmacyTcmByIds(pharmaIds);
        DrugInfoResponseTO response = drugStockClient.scanDrugStock(detailList, recipe.getClinicOrgan(), organDrugList, pharmacyTcmByIds);
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
        }
        logger.info("OrganDrugListManager scanDrugStockByRecipeId 结果={}", JSONObject.toJSONString(result));
        return result;
    }

    /**
     * 根据code获取机构药品 分组
     *
     * @param organId      机构id
     * @param drugCodeList 机构药品code
     * @return 机构code = key对象
     */
    public Map<String, List<OrganDrugList>> getOrganDrugCode(int organId, List<String> drugCodeList) {
        if (CollectionUtils.isEmpty(drugCodeList)) {
            return new HashMap<>();
        }
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(organId, drugCodeList);
        logger.info("OrganDrugListManager getOrganDrugCode organDrugList= {}", JSON.toJSONString(organDrugList));
        return Optional.ofNullable(organDrugList).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.groupingBy(OrganDrugList::getOrganDrugCode));
    }

    /**
     * 根据 drugId 查询药品，用drugId+organDrugCode为key
     *
     * @param organId 机构id
     * @param drugIds 药品id
     * @return drugId+organDrugCode为key返回药品Map
     */
    public Map<String, OrganDrugList> getOrganDrugByIdAndCode(int organId, List<Integer> drugIds) {
        if (CollectionUtils.isEmpty(drugIds)) {
            return new HashMap<>();
        }
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(organId, drugIds);
        logger.info("OrganDrugListManager getOrganDrugByIdAndCode organDrugList= {}", JSON.toJSONString(organDrugList));
        return Optional.ofNullable(organDrugList).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(k -> k.getDrugId() + k.getOrganDrugCode(), a -> a, (k1, k2) -> k1));
    }

    /**
     * 校验比对药品
     *
     * @param validateOrganDrugDTO 校验机构药品对象
     * @param organDrugGroup       机构药品组
     * @return 返回机构药品
     */
    public static OrganDrugList validateOrganDrug(ValidateOrganDrugDTO validateOrganDrugDTO, Map<String, List<OrganDrugList>> organDrugGroup) {
        validateOrganDrugDTO.setValidateStatus(true);
        //校验药品存在
        if (StringUtils.isEmpty(validateOrganDrugDTO.getOrganDrugCode())) {
            validateOrganDrugDTO.setValidateStatus(false);
            return null;
        }
        List<OrganDrugList> organDrugs = organDrugGroup.get(validateOrganDrugDTO.getOrganDrugCode());
        if (CollectionUtils.isEmpty(organDrugs)) {
            validateOrganDrugDTO.setValidateStatus(false);
            return null;
        }
        //校验比对药品
        OrganDrugList organDrug = null;
        if (ValidateUtil.integerIsEmpty(validateOrganDrugDTO.getDrugId()) && 1 == organDrugs.size()) {
            organDrug = organDrugs.get(0);
        }
        if (!ValidateUtil.integerIsEmpty(validateOrganDrugDTO.getDrugId())) {
            for (OrganDrugList drug : organDrugs) {
                if (drug.getDrugId().equals(validateOrganDrugDTO.getDrugId())) {
                    organDrug = drug;
                    break;
                }
            }
        }
        if (null == organDrug) {
            validateOrganDrugDTO.setValidateStatus(false);
            logger.info("RecipeDetailService validateDrug organDrug is null OrganDrugCode ：  {}", validateOrganDrugDTO.getOrganDrugCode());
            return null;
        }
        return organDrug;
    }

    /***
     * 比对获取药品单位
     * @param useDoseUnit 药品单位
     * @param organDrug 机构药品
     * @return 药品单位
     */
    public static String getUseDoseUnit(String useDoseUnit, OrganDrugList organDrug) {
        if (StringUtils.isEmpty(useDoseUnit)) {
            return null;
        }
        if (useDoseUnit.equals(organDrug.getUseDoseUnit())) {
            return organDrug.getUseDoseUnit();
        }
        if (useDoseUnit.equals(organDrug.getUseDoseSmallestUnit())) {
            return organDrug.getUseDoseSmallestUnit();
        }
        return null;
    }


}
