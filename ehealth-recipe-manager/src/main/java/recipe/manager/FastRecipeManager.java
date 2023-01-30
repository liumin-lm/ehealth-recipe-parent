package recipe.manager;

import com.ngari.recipe.entity.*;
import ctd.persistence.exception.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.RecipeBussConstant;
import recipe.dao.FastRecipeDAO;
import recipe.dao.FastRecipeDetailDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.enumerate.type.FastRecipeFlagEnum;
import recipe.util.JsonUtil;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * @Description
 * @Author yzl
 * @Date 2023-01-04
 */
@Service
public class FastRecipeManager extends BaseManager {

    private static final Logger logger = LoggerFactory.getLogger(FastRecipeManager.class);

    @Resource
    FastRecipeDAO fastRecipeDAO;
    @Autowired
    private FastRecipeDetailDAO fastRecipeDetailDAO;
    @Autowired
    private OrganDrugListDAO organDrugListDAO;

    /**
     * 减少库存
     *
     * @param mouldId
     * @param buyNum
     * @param organId
     */
    public void decreaseStock(Integer mouldId, int buyNum, Integer organId) {
        logger.info("deductStock param: mouldId={}, buyNum={}, organId={}", mouldId, buyNum, organId);
        Boolean fastRecipeUsePlatStock = configurationClient.getValueBooleanCatch(organId, "fastRecipeUsePlatStock", false);
        if (!fastRecipeUsePlatStock) {
            return;
        }
        FastRecipe fastRecipe = fastRecipeDAO.get(mouldId);
        if (Objects.isNull(fastRecipe)) {
            return;
        }
        if (Objects.nonNull(fastRecipe.getStockNum())) {
            fastRecipeDAO.updateStockByMouldId(mouldId, buyNum);
        } else {
            logger.info("deductStock error! StockNum is null!");
        }
    }

    /**
     * 释放库存
     *
     * @param mouldId
     * @param buyNum
     * @param organId
     */
    public void addStock(Integer mouldId, int buyNum, Integer organId) {
        logger.info("addStock param: mouldId={}, buyNum={}, organId={}", mouldId, buyNum, organId);
        Boolean fastRecipeUsePlatStock = configurationClient.getValueBooleanCatch(organId, "fastRecipeUsePlatStock", false);
        if (!fastRecipeUsePlatStock) {
            return;
        }
        FastRecipe fastRecipe = fastRecipeDAO.get(mouldId);
        if (Objects.isNull(fastRecipe)) {
            return;
        }
        if (Objects.nonNull(fastRecipe.getStockNum())) {
            fastRecipeDAO.updateStockByMouldId(mouldId, -buyNum);
        } else {
            logger.info("addStock error! StockNum is null!");
        }
    }

    /**
     * 增加销量
     *
     * @param recipeId
     */
    public void addSaleNum(Integer recipeId) {
        logger.info("addSaleNum param: recipeId={}", recipeId);
        try {
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
            if (Objects.isNull(recipe) || Objects.isNull(recipeExtend) ||
                    !FastRecipeFlagEnum.FAST_RECIPE_FLAG_QUICK.getType().equals(recipe.getFastRecipeFlag())) {
                return;
            }
            FastRecipe fastRecipe = fastRecipeDAO.get(recipeExtend.getMouldId());
            if (Objects.isNull(fastRecipe)) {
                return;
            }
            if (Objects.nonNull(fastRecipe.getSaleNum())) {
                fastRecipeDAO.addSaleNumByMouldId(fastRecipe.getId(), recipeExtend.getFastRecipeNum());
            } else {
                logger.info("addStock error! StockNum is null!");
            }
        } catch (Exception e) {
            logger.error("addStock error! ", e);
        }
    }

    public void decreaseSaleNum(Integer recipeId) {
        logger.info("decreaseSaleNum param: recipeId={}", recipeId);
        try {
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
            if (Objects.isNull(recipe) || Objects.isNull(recipeExtend) ||
                    !FastRecipeFlagEnum.FAST_RECIPE_FLAG_QUICK.getType().equals(recipe.getFastRecipeFlag())) {
                return;
            }
            FastRecipe fastRecipe = fastRecipeDAO.get(recipeExtend.getMouldId());
            if (Objects.isNull(fastRecipe)) {
                return;
            }
            if (Objects.nonNull(fastRecipe.getSaleNum())) {
                fastRecipeDAO.addSaleNumByMouldId(fastRecipe.getId(), - recipeExtend.getFastRecipeNum());
            } else {
                logger.info("decreaseSaleNum error! StockNum is null!");
            }
        } catch (Exception e) {
            logger.error("decreaseSaleNum error! ", e);
        }
    }

    public void addStockByRecipeId(Integer recipeId) {
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (Objects.isNull(recipe) || Objects.isNull(recipeExtend) ||
                !FastRecipeFlagEnum.FAST_RECIPE_FLAG_QUICK.getType().equals(recipe.getFastRecipeFlag())) {
            return;
        }
        Boolean fastRecipeUsePlatStock = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "fastRecipeUsePlatStock", false);
        if (!fastRecipeUsePlatStock) {
            return;
        }

        FastRecipe fastRecipe = fastRecipeDAO.get(recipeExtend.getMouldId());
        if (Objects.isNull(fastRecipe)) {
            return;
        }
        if (Objects.nonNull(fastRecipe.getStockNum())) {
            fastRecipeDAO.updateStockByMouldId(recipeExtend.getMouldId(), -recipeExtend.getFastRecipeNum());
        } else {
            logger.info("addStock error! StockNum is null!");
        }
    }

    public Boolean updateFastRecipeSalePriceAndTotalMoney(List<Integer> fastRecipeIdList, Integer organId) {
        for (Integer fastRecipeId: fastRecipeIdList){
            FastRecipe fastRecipe = fastRecipeDAO.get(fastRecipeId);
            if(fastRecipe == null){
                return false;
            }
            logger.info("fastRecipeManager updateFastRecipeSalePriceAndTotalPrice fastRecipe={}", JsonUtil.toString(fastRecipe));
            List<FastRecipeDetail> fastRecipeDetailList = fastRecipeDetailDAO.findFastRecipeDetailsByFastRecipeId(fastRecipeId);
            if(fastRecipeDetailList.size() == 0){
                return false;
            }
            logger.info("fastRecipeManager updateFastRecipeSalePriceAndTotalPrice fastRecipeDetailList={}", JsonUtil.toString(fastRecipeDetailList));
            BigDecimal totalMoney = this.updateSalePriceAndTotalMoney(organId,fastRecipe.getRecipeType(),fastRecipeDetailList);
            fastRecipe.setTotalMoney(totalMoney);
            fastRecipe.setActualPrice(totalMoney);
            fastRecipeDAO.update(fastRecipe);
        }
        return true;
    }

    /**
     * 计算便捷处方总金额
     *
     * @param organId 机构ID
     * @param recipeType 便捷处方类型
     * @param detailList 便捷处方明细
     * @return 处方总金额
     */
    public BigDecimal updateSalePriceAndTotalMoney(Integer organId, Integer recipeType,List<FastRecipeDetail> detailList) {
        BigDecimal totalMoney = new BigDecimal(0d);
        if (org.apache.commons.collections.CollectionUtils.isEmpty(detailList)) {
            return totalMoney;
        }
        for (FastRecipeDetail fastRecipeDetail : detailList) {
            OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(organId, fastRecipeDetail.getOrganDrugCode(), fastRecipeDetail.getDrugId());
            if(organDrugList != null){
                logger.info("fastRecipeManager updateFastRecipeSalePriceAndTotalPrice organDrugList={}", JsonUtil.toString(organDrugList));
                fastRecipeDetail.setSalePrice(organDrugList.getSalePrice());
            }
            BigDecimal price = fastRecipeDetail.getSalePrice();
            BigDecimal drugCost;
            if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipeType)) {
                //保留4位小数
                drugCost = price.multiply(BigDecimal.valueOf(fastRecipeDetail.getUseTotalDose())).divide(BigDecimal.valueOf(fastRecipeDetail.getPack()), 4, RoundingMode.HALF_UP).setScale(4, RoundingMode.HALF_UP);
            } else {
                //保留4位小数
                drugCost = price.multiply(BigDecimal.valueOf(fastRecipeDetail.getUseTotalDose())).setScale(4, RoundingMode.HALF_UP);
            }
            fastRecipeDetail.setDrugCost(drugCost);
            fastRecipeDetailDAO.update(fastRecipeDetail);
            totalMoney = totalMoney.add(drugCost);
        }
        return totalMoney.setScale(4, RoundingMode.HALF_UP);
    }
}
