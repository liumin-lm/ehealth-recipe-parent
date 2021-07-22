package recipe.core.api;

import com.ngari.recipe.dto.DiseaseInfoDTO;
import com.ngari.recipe.recipe.model.OutPatientRecipeVO;
import com.ngari.recipe.vo.OutPatientRecipeReqVO;
import com.ngari.recipe.vo.PatientInfoVO;

import java.util.List;

/**
 * @author yinsheng
 * @date 2021\7\16 0016 17:16
 */
public interface IRecipeBusinessService {

    /**
     * 获取线下门诊处方诊断信息
     * @param patientInfoVO 患者信息
     * @return  诊断列表
     */
    List<DiseaseInfoDTO> getOutRecipeDisease(PatientInfoVO patientInfoVO);

    /**
     * 查询门诊处方信息
     * @param outPatientRecipeReqVO 患者信息
     */
    List<OutPatientRecipeVO> queryOutPatientRecipe(OutPatientRecipeReqVO outPatientRecipeReqVO);

    /**
     * @Description: 根据处方来源，复诊id查询未审核处方个数
     * @Param: bussSource 处方来源
     * @Param: clinicId 复诊Id
     * @return: True存在 False不存在
     * @Date: 2021/7/20
     */
    Boolean existUncheckRecipe(Integer bussSource, Integer clinicId);
}
