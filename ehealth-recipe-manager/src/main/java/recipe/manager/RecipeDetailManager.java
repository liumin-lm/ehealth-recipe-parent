package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.util.ValidateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 处方明细
 *
 * @author fuzi
 */
@Service
public class RecipeDetailManager extends BaseManager {

    /**
     * 保存处方明细
     *
     * @param recipe           处方信息
     * @param details          明细信息
     * @param organDrugListMap 机构药品
     * @return
     */
    public List<Recipedetail> saveRecipeDetails(Recipe recipe, List<Recipedetail> details, Map<String, OrganDrugList> organDrugListMap) {
        logger.info("RecipeDetailManager saveRecipeDetails  recipe = {},  details = {},  organDrugListMap = {}"
                , JSON.toJSONString(recipe), JSON.toJSONString(details), JSON.toJSONString(organDrugListMap));

        recipeDetailDAO.updateDetailInvalidByRecipeId(recipe.getRecipeId());
        BigDecimal totalMoney = new BigDecimal(0);
        for (Recipedetail detail : details) {
            BigDecimal drugCost = setRecipeDetail(detail, recipe.getRecipeId(), organDrugListMap);
            totalMoney = totalMoney.add(drugCost);
            if (ValidateUtil.integerIsEmpty(detail.getRecipeDetailId())) {
                recipeDetailDAO.save(detail);
            } else {
                recipeDetailDAO.update(detail);
            }
        }
        recipe.setTotalMoney(totalMoney);
        recipe.setActualPrice(totalMoney);
        logger.info("RecipeDetailManager saveRecipeDetails details:{}", JSON.toJSONString(details));
        return details;
    }

    /**
     * 批量查询处方明细
     *
     * @param recipeIds 处方id
     * @return 处方明细
     */
    public Map<Integer, List<Recipedetail>> findRecipeDetails(List<Integer> recipeIds) {
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeIdList(recipeIds);
        logger.info("RecipeDetailManager findRecipeDetails recipeDetails:{}", JSON.toJSONString(recipeDetails));
        return Optional.ofNullable(recipeDetails).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
    }

    /**
     * 根据复诊id获取处方明细，并排除 特定处方id
     *
     * @param clinicId 复诊id
     * @param recipeId 特定处方id
     * @return 处方明细
     */
    public List<Recipedetail> findRecipeDetailsByClinicId(Integer clinicId, Integer recipeId) {
        List<Recipe> recipeList = recipeDAO.findRecipeClinicIdAndStatus(clinicId, RecipeStatusEnum.RECIPE_REPEAT_COUNT);
        logger.info("RecipeDetailManager findRecipeDetailsByClinicId recipeList:{}", JSON.toJSONString(recipeList));
        if (CollectionUtils.isEmpty(recipeList)) {
            return null;
        }
        List<Integer> recipeIds;
        if (ValidateUtil.integerIsEmpty(recipeId)) {
            recipeIds = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        } else {
            recipeIds = recipeList.stream().filter(a -> !a.getRecipeId().equals(recipeId)).map(Recipe::getRecipeId).collect(Collectors.toList());
        }
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeIdList(recipeIds);
        logger.info("RecipeDetailManager findRecipeDetailsByClinicId recipeDetails:{}", JSON.toJSONString(recipeDetails));
        return recipeDetails;
    }

    /**
     * 写入明细字段
     *
     * @param detail           处方明细
     * @param recipeId         处方id
     * @param organDrugListMap 机构药品
     * @return
     */
    private BigDecimal setRecipeDetail(Recipedetail detail, Integer recipeId, Map<String, OrganDrugList> organDrugListMap) {
        Date nowDate = DateTime.now().toDate();
        detail.setRecipeId(recipeId);
        detail.setStatus(1);
        detail.setCreateDt(nowDate);
        detail.setLastModify(nowDate);
        if (2 == detail.getType()) {
            BigDecimal price = detail.getSalePrice();
            return price.multiply(BigDecimal.valueOf(detail.getUseTotalDose())).divide(BigDecimal.ONE, 2, RoundingMode.UP);
        }
        OrganDrugList organDrug = organDrugListMap.get(detail.getDrugId() + detail.getOrganDrugCode());
        if (null == organDrug) {
            return new BigDecimal(0);
        }
        detail.setProducer(organDrug.getProducer());
        detail.setProducerCode(organDrug.getProducerCode());
        detail.setLicenseNumber(organDrug.getLicenseNumber());
        detail.setOrganDrugCode(organDrug.getOrganDrugCode());
        detail.setDrugName(organDrug.getDrugName());
        detail.setDrugSpec(organDrug.getDrugSpec());
        detail.setDrugUnit(organDrug.getUnit());
        detail.setDefaultUseDose(organDrug.getUseDose());
        detail.setSaleName(organDrug.getSaleName());
        detail.setDosageUnit(organDrug.getUseDoseUnit());
        detail.setPack(organDrug.getPack());
        detail.setSalePrice(organDrug.getSalePrice());
        BigDecimal price = organDrug.getSalePrice();
        BigDecimal drugCost = price.multiply(BigDecimal.valueOf(detail.getUseTotalDose())).divide(BigDecimal.ONE, 2, RoundingMode.UP);
        detail.setDrugCost(drugCost);
        return drugCost;
    }

    /**
     * 获取项目数量
     * @param recipeId recipeId
     * @return 数量
     */
    public Long getCountByRecipeId(Integer recipeId){
        return recipeDetailDAO.getCountByRecipeId(recipeId);
    }

    /**
     * 获取处方详情列表
     * @param recipeId 处方ID
     * @return 处方详情列表
     */
    public List<Recipedetail> findByRecipeId(Integer recipeId){
        return recipeDetailDAO.findByRecipeId(recipeId);
    }
}
