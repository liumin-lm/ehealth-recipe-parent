package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.*;
import ctd.persistence.exception.DAOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.enumerate.status.TherapyStatusEnum;
import recipe.manager.*;
import recipe.vo.doctor.ItemListVO;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.doctor.RecipeTherapyVO;

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
    @Autowired
    private ItemListManager itemListManager;

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
        recipeTherapy.setStatus(TherapyStatusEnum.READYSUBMIT.getType());
        recipeTherapyManager.saveRecipeTherapy(recipeTherapy, recipe);
        //更新处方
        recipe = recipeManager.saveRecipe(recipe);
        return recipe.getRecipeId();
    }

    @Override
    public RecipeInfoDTO therapyRecipeInfo(Integer recipeId) {
        RecipeInfoDTO recipePdfDTO = recipeManager.getRecipeInfoDTO(recipeId);
        logger.info("TherapyRecipeBusinessService therapyRecipeInfo  recipePdfDTO = {}", JSON.toJSONString(recipePdfDTO));
        return recipePdfDTO;
    }

    @Override
    public boolean cancelRecipe(RecipeTherapyVO recipeTherapyVO) {
        Recipe recipe = recipeManager.getRecipeById(recipeTherapyVO.getRecipeId());
        if (null == recipe) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "处方数据不存在");
        }
        RecipeTherapy recipeTherapy = recipeTherapyManager.getRecipeTherapyByRecipeId(recipeTherapyVO.getRecipeId());
        if (null == recipeTherapy) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "诊疗数据不存在");
        }
        if (!TherapyStatusEnum.READYPAY.getType().equals(recipeTherapy.getStatus())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "当前状态无法撤销");
        }
        //调用HIS处方撤销接口
        //更新诊疗处方状态
        return true;
    }

    @Override
    public boolean abolishTherapyRecipe(Integer recipeId){
        RecipeTherapy recipeTherapy = recipeTherapyManager.getRecipeTherapyByRecipeId(recipeId);
        if (null == recipeTherapy) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "数据不存在");
        }

        if (!TherapyStatusEnum.READYSUBMIT.getType().equals(recipeTherapy.getStatus())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "当前状态无法作废");
        }
        recipeTherapy.setStatus(TherapyStatusEnum.HADECANCEL.getType());
        if (recipeTherapyManager.updateRecipeTherapy(recipeTherapy)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<ItemListVO> searchItemListByKeyWord(ItemListVO itemListVO) {
        List<ItemList> itemLists = itemListManager.findItemList(itemListVO.getItemName(), itemListVO.getStart(), itemListVO.getLimit());
        return ObjectCopyUtils.convert(itemLists, ItemListVO.class);
    }

    @Override
    public void deleteItemListById(Integer id) {
        itemListManager.deleteItemListById(id);
    }

    @Override
    public void updateStatusById(Integer id, Integer status) {
        itemListManager.updateItemListStatusById(id, status);
    }
}
