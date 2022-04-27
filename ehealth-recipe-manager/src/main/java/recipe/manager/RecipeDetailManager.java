package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.RecipeDetailDTO;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.revisit.common.model.RevisitExDTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.PharmacyTcmDAO;
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
    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;

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
    public Map<Integer, List<Recipedetail>> findRecipeDetailMap(List<Integer> recipeIds) {
        List<Recipedetail> recipeDetails = findRecipeDetails(recipeIds);
        return Optional.ofNullable(recipeDetails).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
    }

    public List<Recipedetail> findRecipeDetails(List<Integer> recipeIds) {
        if (CollectionUtils.isEmpty(recipeIds)) {
            return null;
        }
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeIdList(recipeIds);
        logger.info("RecipeDetailManager findRecipeDetails recipeDetails:{}", JSON.toJSONString(recipeDetails));
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
     *
     * @param recipeId 处方ID
     * @return 处方详情列表
     */
    public List<Recipedetail> findByRecipeId(Integer recipeId) {
        return recipeDetailDAO.findByRecipeId(recipeId);
    }

    /**
     * 校验his 药品规则，大病医保等
     *
     * @param recipe        处方信息
     * @param recipeDetails 药品信息
     * @return
     */
    public void validateHisDrugRule(Recipe recipe, List<RecipeDetailDTO> recipeDetails) {
        RevisitExDTO revisitExDTO = revisitClient.getByClinicId(recipe.getClinicId());
        if (StringUtils.isEmpty(revisitExDTO.getDbType())) {
            return;
        }
        Set<Integer> pharmaIds = new HashSet<>();
        List<Integer> drugIdList = recipeDetails.stream().map(a -> {
            pharmaIds.add(a.getPharmacyId());
            return a.getDrugId();
        }).collect(Collectors.toList());
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIds(recipe.getClinicOrgan(), drugIdList);
        List<PharmacyTcm> pharmacyTcmByIds = pharmacyTcmDAO.getPharmacyTcmByIds(pharmaIds);
        // 请求his
        drugClient.hisDrugRule(recipeDetails, recipe.getClinicOrgan(), organDrugList, pharmacyTcmByIds, revisitExDTO);
    }
}
