package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.dto.OrganDTO;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeTherapyDTO;
import com.ngari.recipe.vo.ItemListVO;
import com.ngari.revisit.RevisitBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.client.OrganClient;
import recipe.client.PatientClient;
import recipe.client.RevisitClient;
import recipe.common.CommonConstant;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.dao.RecipeTherapyDAO;
import recipe.enumerate.status.TherapyStatusEnum;
import recipe.enumerate.type.TherapyCancellationTypeEnum;
import recipe.manager.*;
import recipe.service.RecipeServiceSub;
import recipe.util.DateConversion;
import recipe.vo.doctor.RecipeInfoVO;

import javax.annotation.Resource;
import java.util.ArrayList;
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
public class RecipeTherapyBusinessService extends BaseService implements ITherapyRecipeBusinessService {
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
    private OrganClient organClient;
    @Autowired
    private ItemListManager itemListManager;
    @Autowired
    private EmrRecipeManager emrRecipeManager;
    @Resource
    private RecipeTherapyDAO recipeTherapyDAO;
    @Resource
    private RevisitClient revisitClient;

    @Override
    public Integer saveTherapyRecipe(RecipeInfoVO recipeInfoVO) {
        //保存处方
        Recipe recipe = ObjectCopyUtils.convert(recipeInfoVO.getRecipeBean(), Recipe.class);
        recipe = recipeManager.saveRecipe(recipe);
        //保存处方扩展
        if (null != recipeInfoVO.getRecipeExtendBean()) {
            RecipeExtend recipeExtend = ObjectCopyUtils.convert(recipeInfoVO.getRecipeExtendBean(), RecipeExtend.class);
            recipeManager.saveRecipeExtend(recipeExtend, recipe);
        }
        //保存处方明细
        if (!CollectionUtils.isEmpty(recipeInfoVO.getRecipeDetails())) {
            List<Recipedetail> details = ObjectCopyUtils.convert(recipeInfoVO.getRecipeDetails(), Recipedetail.class);
            List<Integer> drugIds = details.stream().filter(a -> !a.getType().equals(2)).map(Recipedetail::getDrugId).collect(Collectors.toList());
            Map<String, OrganDrugList> organDrugListMap = organDrugListManager.getOrganDrugByIdAndCode(recipe.getClinicOrgan(), drugIds);
            recipeDetailManager.saveRecipeDetails(recipe, details, organDrugListMap);
        }
        //保存诊疗
        RecipeTherapy recipeTherapy = ObjectCopyUtils.convert(recipeInfoVO.getRecipeTherapyVO(), RecipeTherapy.class);
        if (null == recipeTherapy) {
            recipeTherapy = new RecipeTherapy();
        }
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
    public Integer therapyRecipeByMpiIdTotal(String mpiId) {
        //获取当前用户下所有就诊人
        List<String> allMpiIds = patientClient.getAllMemberPatientsByCurrentPatient(mpiId);
        if (CollectionUtils.isEmpty(allMpiIds)) {
            return 0;
        }
        List<RecipeTherapy> recipeTherapyList = recipeTherapyDAO.findTherapyByMpiIds(allMpiIds);
        if (CollectionUtils.isEmpty(recipeTherapyList)) {
            return 0;
        }
        return recipeTherapyList.size();
    }

    @Override
    public List<RecipeInfoDTO> therapyRecipeList(RecipeTherapy recipeTherapy, int start, int limit) {
        List<RecipeTherapy> recipeTherapyList = recipeTherapyManager.therapyRecipeList(recipeTherapy, start, limit);
        return paddingRecipeInfoDTO(recipeTherapyList);
    }

    @Override
    public RecipeInfoDTO therapyRecipeInfo(Integer recipeId) {
        RecipeInfoDTO recipePdfDTO = recipeManager.getRecipeInfoDTO(recipeId);
        RecipeTherapy recipeTherapy = recipeTherapyManager.getRecipeTherapyByRecipeId(recipeId);
        recipePdfDTO.setRecipeTherapy(recipeTherapy);
        OrganDTO organDTO = organClient.organDTO(recipePdfDTO.getRecipe().getClinicOrgan());
        recipePdfDTO.setOrgan(organDTO);
        return recipePdfDTO;
    }

    @Override
    public boolean abolishTherapyRecipe(Integer recipeId) {
        return recipeTherapyManager.abolishTherapyRecipe(recipeId);
    }

    @Override
    public boolean abolishTherapyRecipeForRevisitClose(Integer bussSource, Integer clinicId) {
        List<RecipeTherapy> recipeTherapies = recipeTherapyManager.findTherapyByClinicId(clinicId);
        recipeTherapies.forEach(recipeTherapy -> {
            if (TherapyStatusEnum.READYSUBMIT.getType().equals(recipeTherapy.getStatus())) {
                recipeTherapy.setStatus(TherapyStatusEnum.HADECANCEL.getType());
                recipeTherapy.setTherapyCancellation("超时未提交");
                recipeTherapy.setTherapyCancellationType(TherapyCancellationTypeEnum.SYSTEM_CANCEL.getType());
                recipeTherapyManager.updateRecipeTherapy(recipeTherapy);
            }
        });
        return true;
    }

    @Override
    public void updatePushTherapyRecipe(Integer recipeId, RecipeTherapy recipeTherapy, Integer pushType) {
        if (CommonConstant.RECIPE_PUSH_TYPE.equals(pushType)) {
            emrRecipeManager.updateDisease(recipeId);
            Recipe recipe = recipeManager.getRecipeById(recipeId);
            RecipeServiceSub.sendRecipeTagToPatient(recipe, null, null, true);
        }
        recipeTherapyManager.updatePushTherapyRecipe(recipeTherapy, pushType);
    }

    @Override
    public List<ItemListVO> searchItemListByKeyWord(ItemListVO itemListVO) {
        List<ItemList> itemLists = itemListManager.findItemList(itemListVO.getOrganId(), itemListVO.getStatus(), itemListVO.getItemName(), itemListVO.getStart(), itemListVO.getLimit(), itemListVO.getId(), itemListVO.getItemCode());
        return ObjectCopyUtils.convert(itemLists, ItemListVO.class);
    }

    @Override
    public boolean updateTherapyRecipe(Integer organId, String recipeCode, RecipeTherapyDTO recipeTherapyDTO) {
        Recipe recipe = recipeManager.getByRecipeCodeAndClinicOrgan(recipeCode, organId);
        RecipeTherapy recipeTherapy = recipeTherapyManager.getRecipeTherapyByRecipeId(recipe.getRecipeId());
        ObjectCopyUtils.copyPropertiesIgnoreNull(recipeTherapyDTO, recipeTherapy);
        return recipeTherapyManager.updateRecipeTherapy(recipeTherapy);
    }

    @Override
    public List<RecipeInfoDTO> therapyRecipeListForPatient(String mpiId, int start, int limit) {
        //获取当前用户下所有就诊人
        List<String> allMpiIds = patientClient.getAllMemberPatientsByCurrentPatient(mpiId);
        if (CollectionUtils.isEmpty(allMpiIds)) {
            return new ArrayList<>();
        }
        List<RecipeTherapy> recipeTherapyList = recipeTherapyDAO.findTherapyPageByMpiIds(allMpiIds, start, limit);
        return paddingRecipeInfoDTO(recipeTherapyList);
    }

    @Override
    public List<RecipeTherapy> findTherapyByClinicId(Integer clinicId) {
        return recipeTherapyManager.findTherapyByClinicId(clinicId);
    }


    /**
     * 包装数据 返回填充对象
     *
     * @param recipeTherapyList 诊疗列表
     * @return 填充后的诊疗信息
     */
    private List<RecipeInfoDTO> paddingRecipeInfoDTO(List<RecipeTherapy> recipeTherapyList) {
        List<RecipeInfoDTO> list = new LinkedList<>();
        logger.info("TherapyRecipeBusinessService paddingRecipeInfoDTO recipeTherapyList:{}", JSON.toJSONString(recipeTherapyList));
        if (CollectionUtils.isEmpty(recipeTherapyList)) {
            return list;
        }
        List<Integer> recipeIds = recipeTherapyList.stream().map(RecipeTherapy::getRecipeId).collect(Collectors.toList());
        List<Recipe> recipeList = recipeManager.findByRecipeIds(recipeIds);
        Map<Integer, Recipe> recipeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getRecipeId, a -> a, (k1, k2) -> k1));
        Map<Integer, List<Recipedetail>> recipeDetailGroup = recipeDetailManager.findRecipeDetailMap(recipeIds);
        List<String> mpiIds = recipeTherapyList.stream().map(RecipeTherapy::getMpiId).distinct().collect(Collectors.toList());
        List<Integer> clinicIds = recipeTherapyList.stream().map(RecipeTherapy::getClinicId).collect(Collectors.toList());
        List<RevisitBean> revisitBeans = revisitClient.findByConsultIds(clinicIds);
        Map<Integer, RevisitBean> revisitBeanMap = revisitBeans.stream().collect(Collectors.toMap(RevisitBean::getConsultId, a ->a, (k1, k2) -> k1));
        Map<String, PatientDTO> patientMap = patientClient.findPatientMap(mpiIds);
        recipeTherapyList.forEach(a -> {
            RecipeInfoDTO recipeInfoDTO = new RecipeInfoDTO();
            recipeInfoDTO.setRecipeTherapy(a);
            recipeInfoDTO.setRevisitTime(DateConversion.getDateFormatter(revisitBeanMap.get(a.getClinicId()).getRequestTime(), DateConversion.DEFAULT_DATE_TIME));
            recipeInfoDTO.setRecipe(recipeMap.get(a.getRecipeId()));
            recipeInfoDTO.setRecipeDetails(recipeDetailGroup.get(a.getRecipeId()));
            recipeInfoDTO.setPatientBean(patientMap.get(a.getMpiId()));
            list.add(recipeInfoDTO);
        });
        return list;
    }

}
