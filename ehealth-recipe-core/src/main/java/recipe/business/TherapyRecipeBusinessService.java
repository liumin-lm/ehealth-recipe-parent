package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.vo.ItemListVO;
import ctd.persistence.exception.DAOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.client.PatientClient;
import recipe.constant.ErrorCode;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.enumerate.status.TherapyStatusEnum;
import recipe.manager.*;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.doctor.RecipeTherapyVO;

import java.util.LinkedList;
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
    private PatientClient patientClient;
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
        RecipeTherapy recipeTherapy = new RecipeTherapy();
        recipeTherapy.setStatus(TherapyStatusEnum.READYSUBMIT.getType());
        recipeTherapyManager.saveRecipeTherapy(recipeTherapy, recipe);
        //更新处方
        recipe = recipeManager.saveRecipe(recipe);
        return recipe.getRecipeId();
    }

    @Override
    public Integer therapyRecipeTotal(RecipeTherapy recipeTherapy) {
        List<RecipeTherapy> recipeTherapyList = recipeTherapyManager.therapyRecipeList(recipeTherapy);
        if (CollectionUtils.isEmpty(recipeTherapyList)) {
            return 0;
        }
        return recipeTherapyList.size();
    }

    @Override
    public List<RecipeInfoDTO> therapyRecipeList(RecipeTherapy recipeTherapy, int start, int limit) {
        List<RecipeInfoDTO> list = new LinkedList<>();
        List<RecipeTherapy> recipeTherapyList = recipeTherapyManager.therapyRecipeList(recipeTherapy, start, limit);
        if (CollectionUtils.isEmpty(recipeTherapyList)) {
            return list;
        }
        List<Integer> recipeIds = recipeTherapyList.stream().map(RecipeTherapy::getRecipeId).collect(Collectors.toList());
        List<Recipe> recipeList = recipeManager.findByRecipeIds(recipeIds);
        Map<Integer, Recipe> recipeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getRecipeId, a -> a, (k1, k2) -> k1));
        Map<Integer, List<Recipedetail>> recipeDetailGroup = recipeDetailManager.findRecipeDetails(recipeIds);
        List<String> mpiIds = recipeTherapyList.stream().map(RecipeTherapy::getMpiId).distinct().collect(Collectors.toList());
        Map<String, PatientDTO> patientMap = patientClient.findPatientMap(mpiIds);
        recipeTherapyList.forEach(a -> {
            RecipeInfoDTO recipeInfoDTO = new RecipeInfoDTO();
            recipeInfoDTO.setRecipeTherapy(a);
            recipeInfoDTO.setRecipe(recipeMap.get(a.getRecipeId()));
            recipeInfoDTO.setRecipeDetails(recipeDetailGroup.get(a.getRecipeId()));
            recipeInfoDTO.setPatientBean(patientMap.get(a.getMpiId()));
            list.add(recipeInfoDTO);
        });
        return list;
    }

    @Override
    public RecipeInfoDTO therapyRecipeInfo(Integer recipeId) {
        RecipeInfoDTO recipePdfDTO = recipeManager.getRecipeInfoDTO(recipeId);
        RecipeTherapy recipeTherapy = recipeTherapyManager.getRecipeTherapyByRecipeId(recipeId);
        recipePdfDTO.setRecipeTherapy(recipeTherapy);
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
        List<ItemList> itemLists = itemListManager.findItemList(itemListVO.getOrganId(), itemListVO.getItemName(), itemListVO.getStart(), itemListVO.getLimit());
        return ObjectCopyUtils.convert(itemLists, ItemListVO.class);
    }

}
