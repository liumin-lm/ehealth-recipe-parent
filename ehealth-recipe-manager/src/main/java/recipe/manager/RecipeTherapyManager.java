package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeTherapy;
import ctd.persistence.exception.DAOException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.PatientClient;
import recipe.common.CommonConstant;
import recipe.constant.ErrorCode;
import recipe.dao.RecipeTherapyDAO;
import recipe.enumerate.status.TherapyStatusEnum;
import recipe.enumerate.type.TherapyCancellationTypeEnum;
import recipe.util.ValidateUtil;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
        logger.info("RecipeTherapyManager saveRecipeTherapy recipeTherapy:{}.", JSON.toJSONString(recipeTherapy));
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
        RecipeDTO recipeDTO = super.getRecipeDTO(recipeId);
        RecipeInfoDTO recipeInfoDTO = new RecipeInfoDTO();
        BeanUtils.copyProperties(recipeDTO, recipeInfoDTO);
        Recipe recipe = recipeInfoDTO.getRecipe();
        PatientDTO patientBean = patientClient.getPatientDTO(recipe.getMpiid());
        recipeInfoDTO.setPatientBean(patientBean);
        RecipeTherapy recipeTherapy = recipeTherapyDAO.getByRecipeId(recipeId);
        recipeInfoDTO.setRecipeTherapy(recipeTherapy);
        logger.info("RecipeTherapyManager getRecipeTherapyDTO recipeInfoDTO:{}.", JSON.toJSONString(recipeInfoDTO));
        return recipeInfoDTO;
    }

    /**
     * 分页 获取诊疗处方列表
     *
     * @param recipeTherapy 诊疗处方对象
     * @param start         页数
     * @param limit         每页条数
     * @return 诊疗处方列表
     */
    public List<RecipeTherapy> therapyRecipeList(RecipeTherapy recipeTherapy, int start, int limit) {
        if (!ValidateUtil.validateObjects(recipeTherapy.getDoctorId()) && ValidateUtil.validateObjects(recipeTherapy.getClinicId())) {
            return recipeTherapyDAO.findTherapyPageByDoctorId(recipeTherapy.getOrganId(), recipeTherapy.getDoctorId(), start, limit);
        }
        if (!ValidateUtil.validateObjects(recipeTherapy.getDoctorId()) && !ValidateUtil.validateObjects(recipeTherapy.getClinicId())) {
            List<RecipeTherapy> list = recipeTherapyDAO.findTherapyByDoctorIdAndClinicId(recipeTherapy.getOrganId(), recipeTherapy.getDoctorId(), recipeTherapy.getClinicId());
            return list.stream().sorted(Comparator.comparing(RecipeTherapy::getStatus).thenComparing(RecipeTherapy::getId, Comparator.reverseOrder())).collect(Collectors.toList());
        }
        if (!ValidateUtil.validateObjects(recipeTherapy.getMpiId()) && ValidateUtil.validateObjects(recipeTherapy.getDoctorId())) {
            return recipeTherapyDAO.findTherapyPageByMpiIdAndClinicId(recipeTherapy.getOrganId(), recipeTherapy.getMpiId(), recipeTherapy.getClinicId(), start, limit);
        }
        return null;
    }

    /**
     * 获取诊疗处方列表
     *
     * @param recipeTherapy 诊疗处方对象
     * @return 诊疗处方列表
     */
    public List<RecipeTherapy> therapyRecipeList(RecipeTherapy recipeTherapy) {
        if (!ValidateUtil.validateObjects(recipeTherapy.getDoctorId()) && ValidateUtil.validateObjects(recipeTherapy.getClinicId())) {
            return recipeTherapyDAO.findTherapyByDoctorId(recipeTherapy.getOrganId(), recipeTherapy.getDoctorId());
        }
        if (!ValidateUtil.validateObjects(recipeTherapy.getDoctorId()) && !ValidateUtil.validateObjects(recipeTherapy.getClinicId())) {
            return recipeTherapyDAO.findTherapyByDoctorIdAndClinicId(recipeTherapy.getOrganId(), recipeTherapy.getDoctorId(), recipeTherapy.getClinicId());
        }
        if (!ValidateUtil.validateObjects(recipeTherapy.getMpiId()) && ValidateUtil.validateObjects(recipeTherapy.getDoctorId())) {
            return recipeTherapyDAO.findTherapyByMpiIdAndClinicId(recipeTherapy.getOrganId(), recipeTherapy.getMpiId(), recipeTherapy.getClinicId());
        }
        return null;
    }

    public RecipeTherapy getRecipeTherapyByRecipeId(Integer recipeId) {
        return recipeTherapyDAO.getByRecipeId(recipeId);
    }

    public Boolean updateRecipeTherapy(RecipeTherapy recipeTherapy) {
        logger.info("RecipeTherapyManager updateRecipeTherapy recipeTherapy:{}.", JSON.toJSONString(recipeTherapy));
        return recipeTherapyDAO.updateNonNullFieldByPrimaryKey(recipeTherapy);
    }

    public Boolean abolishTherapyRecipe(Integer recipeId){
        RecipeTherapy recipeTherapy = recipeTherapyDAO.getByRecipeId(recipeId);
        if (null == recipeTherapy) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "数据不存在");
        }

        if (!TherapyStatusEnum.READYSUBMIT.getType().equals(recipeTherapy.getStatus())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "当前状态无法作废");
        }
        recipeTherapy.setStatus(TherapyStatusEnum.HADECANCEL.getType());
        recipeTherapy.setTherapyCancellationType(TherapyCancellationTypeEnum.DOCTOR_ABOLISH.getType());
        return recipeTherapyDAO.updateNonNullFieldByPrimaryKey(recipeTherapy);
    }

    /**
     * 推送类型 更新诊疗信息
     *
     * @param recipeTherapy 诊疗处方
     * @param pushType      推送类型: 1：提交处方，2:撤销处方
     */
    public void updatePushTherapyRecipe(RecipeTherapy recipeTherapy, Integer pushType) {
        if (null == recipeTherapy) {
            return;
        }
        RecipeTherapy updateRecipeTherapy = new RecipeTherapy();
        updateRecipeTherapy.setId(recipeTherapy.getId());
        if (CommonConstant.RECIPE_PUSH_TYPE.equals(pushType)) {
            updateRecipeTherapy.setTherapyTime(recipeTherapy.getTherapyTime());
            updateRecipeTherapy.setTherapyExecuteDepart(recipeTherapy.getTherapyExecuteDepart());
            updateRecipeTherapy.setTherapyNotice(recipeTherapy.getTherapyNotice());
            updateRecipeTherapy.setStatus(TherapyStatusEnum.READYPAY.getType());
        } else {
            RecipeTherapy therapy = getRecipeTherapyByRecipeId(recipeTherapy.getRecipeId());
            updateRecipeTherapy.setId(therapy.getId());
            updateRecipeTherapy.setTherapyCancellationType(recipeTherapy.getTherapyCancellationType());
            updateRecipeTherapy.setTherapyCancellation(recipeTherapy.getTherapyCancellation());
            updateRecipeTherapy.setStatus(TherapyStatusEnum.HADECANCEL.getType());
        }
        updateRecipeTherapy(updateRecipeTherapy);
    }

    /**
     * 根据处方ids 获取诊疗信息
     *
     * @param recipeIds 处方ids
     * @return 诊疗信息
     */
    public List<RecipeTherapy> findTherapyByRecipeIds(List<Integer> recipeIds) {
        logger.info("RecipeTherapyManager findTherapyByRecipeIds recipeIds:{}.", JSON.toJSONString(recipeIds));
        List<RecipeTherapy> result = recipeTherapyDAO.findTherapyByRecipeIds(recipeIds);
        logger.info("RecipeTherapyManager findTherapyByRecipeIds result:{}.", JSON.toJSONString(result));
        return result;
    }
}
