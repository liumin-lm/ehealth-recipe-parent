package recipe.atop.doctor;

import com.ngari.recipe.dto.PatientOptionalDrugDTO;
import com.ngari.recipe.vo.OffLineRecipeDetailVO;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IRecipeBusinessService;

import java.util.List;

/**
 * @description： 处方医生药品处理atop
 * @author： whf
 * @date： 2021-11-22 17:50
 */
@RpcBean("recipeDrugDoctorAtop")
public class RecipeDrugDoctorAtop extends BaseAtop {

    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    /**
     * 医生端 获取患者指定处方药品
     *
     * @param clinicId
     * @return
     */
    @RpcService
    public List<PatientOptionalDrugDTO> findPatientOptionalDrugDTO(Integer clinicId) {
        logger.info("OffLineRecipeAtop findPatientOptionalDrugDTO clinicId={}", clinicId);
        validateAtop(clinicId);
        List<PatientOptionalDrugDTO> result = recipeBusinessService.findPatientOptionalDrugDTO(clinicId);
        logger.info("OffLineRecipeAtop findPatientOptionalDrugDTO result = {}", JSONUtils.toString(result));
        return result;

    }
}
