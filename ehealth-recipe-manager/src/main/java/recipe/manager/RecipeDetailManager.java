package recipe.manager;

import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import recipe.util.ValidateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
        return details;
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
        if (3 == detail.getType()) {
            return detail.getDrugCost();
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
}
