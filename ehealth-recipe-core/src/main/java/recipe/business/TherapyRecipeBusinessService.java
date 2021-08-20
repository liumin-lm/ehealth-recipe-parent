package recipe.business;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.constant.TherapyStatusEnum;
import com.ngari.recipe.recipe.model.CancelRecipeReqVO;
import com.ngari.recipe.recipe.model.CancelRecipeResultVO;
import ctd.persistence.exception.DAOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.manager.OrganDrugListManager;
import recipe.manager.RecipeDetailManager;
import recipe.manager.RecipeManager;
import recipe.manager.RecipeTherapyManager;
import recipe.vo.doctor.RecipeInfoVO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Autowired
    private OrganDrugListManager organDrugListManager;

    @Override
    public Integer saveTherapyRecipe(RecipeInfoVO recipeInfoVO) {
        //保存处方
        Recipe recipe = ObjectCopyUtils.convert(recipeInfoVO.getRecipeBean(), Recipe.class);
        recipe = recipeManager.saveRecipe(recipe);
        //保存处方扩展
        RecipeExtend recipeExtend = ObjectCopyUtils.convert(recipeInfoVO.getRecipeExtendBean(), RecipeExtend.class);
        recipeManager.saveRecipeExtend(recipeExtend, recipe);
        //保存处方明细
        List<Recipedetail> details = ObjectCopyUtils.convert(recipeInfoVO.getRecipeDetails(), Recipedetail.class);
        List<Integer> drugIds = details.stream().map(Recipedetail::getDrugId).collect(Collectors.toList());
        Map<String, OrganDrugList> organDrugListMap = organDrugListManager.getOrganDrugByIdAndCode(recipe.getClinicOrgan(), drugIds);
        recipeDetailManager.saveRecipeDetails(recipe, details, organDrugListMap);
        //保存诊疗
        RecipeTherapy recipeTherapy = ObjectCopyUtils.convert(recipeInfoVO.getRecipeTherapyVO(), RecipeTherapy.class);
        recipeTherapyManager.saveRecipeTherapy(recipeTherapy, recipe);
        //更新处方
        recipe = recipeManager.saveRecipe(recipe);
        return recipe.getRecipeId();
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
