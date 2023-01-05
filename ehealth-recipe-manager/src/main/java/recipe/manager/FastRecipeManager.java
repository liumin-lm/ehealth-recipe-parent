package recipe.manager;

import com.ngari.recipe.entity.FastRecipe;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import ctd.persistence.exception.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import recipe.dao.FastRecipeDAO;
import recipe.enumerate.type.FastRecipeFlagEnum;

import javax.annotation.Resource;
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
}
