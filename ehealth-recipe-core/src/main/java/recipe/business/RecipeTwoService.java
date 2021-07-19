package recipe.business;

import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.api.open.IRecipeTwoService;
import recipe.dao.RecipeDAO;
import recipe.factory.status.constant.RecipeStatusEnum;

import java.util.Arrays;
import java.util.List;

/**
* 处方提供的二方服务
* @Date: 2021/7/19
* @Author: zhaoh
*/
@Service
public class RecipeTwoService extends BaseService implements IRecipeTwoService {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final List<Integer> UncheckedStatus = Arrays.asList(RecipeStatusEnum.RECIPE_STATUS_UNCHECK.getType(), RecipeStatusEnum.RECIPE_STATUS_READY_CHECK_YS.getType(),
            RecipeStatusEnum.RECIPE_STATUS_SIGN_ERROR_CODE_PHA.getType(), RecipeStatusEnum.RECIPE_STATUS_SIGN_ING_CODE_DOC.getType(), RecipeStatusEnum.RECIPE_STATUS_SIGN_ING_CODE_PHA.getType(),
            RecipeStatusEnum.RECIPE_STATUS_SIGN_NO_CODE_PHA.getType());

    @Autowired
    RecipeDAO recipeDAO;

    private Long getUncheckRecipeByClinicID(Integer bussSource, Integer clinicID, List<Integer> recipeStatus) {
        logger.info("getUncheckRecipeByClinicID bussSource={},clinicID={},recipeStatus={}", bussSource, clinicID, recipeStatus);
        Long recipeCount = recipeDAO.getRecipeCountByBussSourceAndClinicIdAndStatus(bussSource, clinicID, recipeStatus);
        logger.info("getUncheckRecipeByClinicID recipesCount={}", recipeCount);
        return recipeCount;
    }

    /**
     * @Description: 根据bussSource和clinicID查询是否存在药师审核未通过的处方
     * @Param: bussSource
     * @Param: clinicID
     * @return: true存在  false不存在
     * @Date: 2021/7/16
     */
    @Override
    public Boolean existUncheckRecipe(Integer bussSource, Integer clinicID) {
        //药师审核未通过的状态集合
        //获取处方状态为药师审核不通过的处方个数
        Long uncheckRecipeList = getUncheckRecipeByClinicID(bussSource, clinicID, UncheckedStatus);
        Integer uncheckCount = uncheckRecipeList.intValue();
        if (uncheckCount == 0) {
            return false;
        }
        return true;
    }
}
