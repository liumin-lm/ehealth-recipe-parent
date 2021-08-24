package recipe.manager;

import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeTherapy;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.PatientClient;
import recipe.dao.RecipeTherapyDAO;
import recipe.util.ValidateUtil;

import java.util.List;

/**
 * 诊疗处方
 *
 * @author fuzi
 */
@Service
public class RecipeTherapyManager extends BaseManager {
    @Autowired
    private RecipeTherapyDAO recipeTherapyDAO;
    @Autowired
    private PatientClient patientClient;

    /**
     * 保存诊疗处方关联信息
     *
     * @param recipeTherapy
     * @param recipe
     * @return
     */
    public RecipeTherapy saveRecipeTherapy(RecipeTherapy recipeTherapy, Recipe recipe) {
        recipeTherapy.setDoctorId(recipe.getDoctor());
        recipeTherapy.setMpiId(recipe.getMpiid());
        recipeTherapy.setRecipeId(recipe.getRecipeId());
        recipeTherapy.setClinicId(recipe.getClinicId());
        recipeTherapy.setOrganId(recipe.getClinicOrgan());
        recipeTherapy.setTherapyExecuteDepart("");
        recipeTherapy.setTherapyNotice("");
        recipeTherapy.setTherapyCancellation("");
        recipeTherapy.setTherapyCancellationType(0);
        recipeTherapy.setTherapyTime("");
        if (ValidateUtil.integerIsEmpty(recipeTherapy.getId())) {
            recipeTherapy = recipeTherapyDAO.save(recipeTherapy);
        } else {
            recipeTherapy = recipeTherapyDAO.update(recipeTherapy);
        }
        return recipeTherapy;
    }

    /**
     * 获取诊疗处方相关信息
     *
     * @param recipeId 处方id
     * @return
     */
    public RecipeInfoDTO getRecipeTherapyDTO(Integer recipeId) {
        RecipeDTO recipeDTO = getRecipeDTO(recipeId);
        RecipeInfoDTO recipeInfoDTO = new RecipeInfoDTO();
        BeanUtils.copyProperties(recipeDTO, recipeInfoDTO);
        Recipe recipe = recipeInfoDTO.getRecipe();
        PatientDTO patientBean = patientClient.getPatientDTO(recipe.getMpiid());
        recipeInfoDTO.setPatientBean(patientBean);
        RecipeTherapy recipeTherapy = recipeTherapyDAO.getByRecipeId(recipeId);
        recipeInfoDTO.setRecipeTherapy(recipeTherapy);
        return recipeInfoDTO;
    }

    /**
     * 获取诊疗处方列表
     *
     * @param recipeTherapy 诊疗处方对象
     * @param start         页数
     * @param limit         每页条数
     * @return
     */
    public List<RecipeTherapy> therapyRecipeList(RecipeTherapy recipeTherapy, int start, int limit) {
        if (!ValidateUtil.validateObjects(recipeTherapy.getDoctorId()) && ValidateUtil.validateObjects(recipeTherapy.getClinicId())) {
            return recipeTherapyDAO.findTherapyByDoctorId(recipeTherapy.getOrganId(), recipeTherapy.getDoctorId(), start, limit);
        }
        if (!ValidateUtil.validateObjects(recipeTherapy.getDoctorId()) && !ValidateUtil.validateObjects(recipeTherapy.getClinicId())) {
            return recipeTherapyDAO.findTherapyByDoctorIdAndClinicId(recipeTherapy.getOrganId(), recipeTherapy.getDoctorId(), recipeTherapy.getClinicId(), start, limit);
        }
        if (!ValidateUtil.validateObjects(recipeTherapy.getMpiId())) {
            return recipeTherapyDAO.findTherapyByMpiId(recipeTherapy.getOrganId(), recipeTherapy.getMpiId(), start, limit);
        }
        return null;
    }

    public RecipeTherapy getRecipeTherapyById(Integer id) {
        return recipeTherapyDAO.getById(id);
    }

    public RecipeTherapy getRecipeTherapyByRecipeId(Integer recipeId) {
        return recipeTherapyDAO.getByRecipeId(recipeId);
    }

    public Boolean updateRecipeTherapy(RecipeTherapy recipeTherapy) {
        return recipeTherapyDAO.updateNonNullFieldByPrimaryKey(recipeTherapy);
    }

}
