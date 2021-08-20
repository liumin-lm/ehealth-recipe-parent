package recipe.business;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeTherapy;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.constant.TherapyStatusEnum;
import com.ngari.recipe.recipe.model.CancelRecipeReqVO;
import com.ngari.recipe.recipe.model.CancelRecipeResultVO;
import ctd.persistence.exception.DAOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.manager.RecipeDetailManager;
import recipe.manager.RecipeManager;
import recipe.manager.RecipeTherapyManager;
import recipe.vo.doctor.RecipeInfoVO;

import java.util.List;

/**
 * 诊疗处方 核心处理类
 *
 * @author fuzi
 */
@Service
public class TherapyRecipeBusinessService extends BaseService implements ITherapyRecipeBusinessService {
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private RecipeTherapyManager recipeTherapyManager;
    @Autowired
    private RecipeDetailManager recipeDetailManager;

    @Override
    public RecipeInfoVO saveTherapyRecipe(RecipeInfoVO recipeInfoVO) {
        Recipe recipe = ObjectCopyUtils.convert(recipeInfoVO.getRecipeBean(), Recipe.class);
        recipe = recipeManager.saveRecipe(recipe);
        RecipeExtend recipeExtend = ObjectCopyUtils.convert(recipeInfoVO.getRecipeExtendBean(), RecipeExtend.class);
        recipeExtend = recipeManager.saveRecipeExtend(recipeExtend);
        List<Recipedetail> details = ObjectCopyUtils.convert(recipeInfoVO.getRecipeDetails(), Recipedetail.class);
        details = recipeDetailManager.saveRecipeDetails(details);
        RecipeTherapy recipeTherapy = ObjectCopyUtils.convert(recipeInfoVO.getRecipeTherapyVO(), RecipeTherapy.class);
        recipeTherapy = recipeTherapyManager.saveRecipeTherapy(recipeTherapy);
        return null;
    }

    @Override
    public CancelRecipeResultVO cancelRecipe(CancelRecipeReqVO cancelRecipeReqVO){
        RecipeTherapy recipeTherapy = recipeTherapyManager.getRecipeTherapyById(cancelRecipeReqVO.getBusId());
        if (null == recipeTherapy) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "数据不存在");
        }
        if (!TherapyStatusEnum.READYPAY.getStatus().equals(recipeTherapy.getStatus())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "当前状态无法撤销");
        }
        return null;
    }

    @Override
    public boolean abolishTherapyRecipe(Integer therapyId){
        RecipeTherapy recipeTherapy = recipeTherapyManager.getRecipeTherapyById(therapyId);
        if (null == recipeTherapy) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "数据不存在");
        }
        return true;
    }
}
